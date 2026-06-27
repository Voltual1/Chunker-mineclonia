// [file name]: com.hivemc.chunker.conversion.encoding.bedrock.base.reader.BedrockColumnReader.java
package com.hivemc.chunker.conversion.encoding.bedrock.base.reader;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.reader.ColumnReader;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBChunkType;
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey;
import com.hivemc.chunker.conversion.encoding.bedrock.util.PaletteUtil;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.column.biome.layout.ChunkerColumnBasedBiomes;
import com.hivemc.chunker.conversion.intermediate.column.biome.layout.ChunkerPaletteBasedBiomes;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkerChunk;
import com.hivemc.chunker.conversion.intermediate.column.chunk.palette.Palette;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.column.heightmap.BedrockHeightMap;
import com.hivemc.chunker.conversion.intermediate.column.heightmap.HeightMap;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.io.Reader;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.TagWithName;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import org.iq80.leveldb.DB;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BedrockColumnReader implements ColumnReader {
    protected final BedrockResolvers resolvers;
    protected final Converter converter;
    protected final Dimension dimension;
    protected final DB database;
    protected final ChunkCoordPair columnCoords;

    public BedrockColumnReader(BedrockResolvers resolvers, Converter converter, DB database, Dimension dimension, ChunkCoordPair columnCoords) {
        this.resolvers = resolvers;
        this.converter = converter;
        this.database = database;
        this.dimension = dimension;
        this.columnCoords = columnCoords;
    }

    @Override
    public Task<Void> readColumn(ColumnConversionHandler columnConversionHandler) {
        ChunkerColumn column = new ChunkerColumn(columnCoords);

        ArrayList<Task<Void>> processing = new ArrayList<>(4);
        if (converter.shouldProcessHeightMap() || converter.shouldProcessBiomes()) processing.add(Task.asyncConsume("Reading Biome/HeightMap", TaskWeight.NORMAL, this::readBiomeHeightMap, column));
        if (converter.shouldProcessEntities()) processing.add(Task.asyncConsume("Reading Entities", TaskWeight.HIGH, this::readEntities, column));
        if (converter.shouldProcessBlockEntities()) processing.add(Task.asyncConsume("Reading Block Entities", TaskWeight.HIGH, this::readBlockEntities, column));
        processing.add(Task.asyncConsume("Reading Chunks", TaskWeight.HIGHER, this::readChunks, column));

        return Task.join(processing)
                .then("Post-processing column", TaskWeight.HIGH, this::postProcess, column)
                // NOW we unwrap the returned task! This waits for the writer to actually finish writing to disk!
                .thenUnwrap("Submitting column", TaskWeight.LOW, columnConversionHandler::convertColumn);
    }

    protected void readBiomeHeightMap(ChunkerColumn column) {
        try {
            byte[] value = database.get(LevelDBKey.key(dimension, column.getPosition(), LevelDBChunkType.DATA_2D));
            if (value != null) {
                ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
                if (converter.shouldProcessHeightMap()) column.setHeightMap(readHeightMap(buffer));
                else buffer.position(buffer.position() + 256);
                if (converter.shouldProcessBiomes()) column.setBiomes(readBiomesColumn(buffer));
            }
        } catch (Exception e) { converter.logNonFatalException(e); }
    }

    protected HeightMap readHeightMap(ByteBuffer buffer) {
        short[][] heightMap = new short[16][16];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                heightMap[x][z] = buffer.getShort();
            }
        }
        return new BedrockHeightMap(heightMap);
    }

    protected ChunkerPaletteBasedBiomes readBiomesExtended(ByteBuffer buffer) throws Exception {
        List<Palette<ChunkerBiome>> palettes = new ArrayList<>();
        while (buffer.hasRemaining()) {
            Palette<ChunkerBiome> palette = PaletteUtil.readChunkPalette(buffer, (ignored) -> resolvers.readBiome(buffer.getInt(), dimension));
            palettes.add(palette);
        }
        for (int i = palettes.size() - 1; i >= 0; i--) {
            if (palettes.get(i).isEmpty()) palettes.remove(i); else break;
        }
        return new ChunkerPaletteBasedBiomes(palettes);
    }

    protected ChunkerColumnBasedBiomes readBiomesColumn(ByteBuffer buffer) {
        ChunkerBiome[] biomes = new ChunkerBiome[256];
        for (int i = 0; i < 256; i++) biomes[i] = resolvers.readBiome(buffer.get() & 0xFF, dimension);
        return new ChunkerColumnBasedBiomes(biomes);
    }

    protected void readEntities(ChunkerColumn column) throws Exception {
        byte[] value = database.get(LevelDBKey.key(dimension, column.getPosition(), LevelDBChunkType.ENTITY));
        if (value == null) return;
        try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(value);
             DataInputStream readerStream = new DataInputStream(fileInputStream)) {
            Reader reader = Reader.toBedrockReader(readerStream);
            while (fileInputStream.available() > 0) {
                TagWithName<CompoundTag> pair = Tag.decodeNamed(reader, CompoundTag.class);
                if (pair == null) break;
                try { readEntity(column, pair.tag()); } catch (Exception e) { converter.logNonFatalException(new Exception("Failed to process Entity", e)); }
            }
        }
    }

    protected void readEntity(ChunkerColumn chunkerColumn, CompoundTag compoundTag) {
        Optional<Entity> entity = resolvers.entityResolver().to(compoundTag);
        if (entity.isPresent()) chunkerColumn.getEntities().add(entity.get());
        else converter.logMissingMapping(Converter.MissingMappingType.ENTITY, resolvers.entityResolver().getKey(compoundTag).map(Object::toString).orElseGet(compoundTag::toString));
    }

    protected void readBlockEntities(ChunkerColumn column) throws Exception {
        byte[] value = database.get(LevelDBKey.key(dimension, column.getPosition(), LevelDBChunkType.BLOCK_ENTITY));
        if (value == null) return;
        try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(value);
             DataInputStream readerStream = new DataInputStream(fileInputStream)) {
            Reader reader = Reader.toBedrockReader(readerStream);
            while (fileInputStream.available() > 0) {
                TagWithName<CompoundTag> pair = Tag.decodeNamed(reader, CompoundTag.class);
                if (pair == null) break;
                try { readBlockEntity(column, pair.tag()); } catch (Exception e) { converter.logNonFatalException(new Exception("Failed to process BlockEntity", e)); }
            }
        }
    }

    protected void readBlockEntity(ChunkerColumn chunkerColumn, CompoundTag compoundTag) {
        Optional<BlockEntity> blockEntity = resolvers.blockEntityResolver().to(compoundTag);
        if (blockEntity.isPresent()) chunkerColumn.getBlockEntities().add(blockEntity.get());
        else converter.logMissingMapping(Converter.MissingMappingType.BLOCK_ENTITY, resolvers.blockEntityResolver().getKey(compoundTag).orElseGet(compoundTag::toString));
    }

    protected void readChunks(ChunkerColumn column) {
        List<Task<ChunkerChunk>> tasks = new ArrayList<>();
        byte[] key = LevelDBKey.key(dimension, column.getPosition(), (byte) 0, LevelDBChunkType.SUB_CHUNK_PREFIX);
        for (byte y = -64; y < 64; y++) {
            key[key.length - 1] = y;
            byte[] value = database.get(key);
            if (value == null) continue;
            ChunkerChunk chunk = new ChunkerChunk(y);
            tasks.add(Task.async("Creating Chunk Reader", TaskWeight.LOW, () -> createChunkReader(chunk))
                    .thenConsume("Reading Chunk", TaskWeight.HIGHER, (chunkReader) -> chunkReader.readChunk(value))
                    .then(chunk));
        }
        Task.join(tasks).thenConsume("Adding chunks to column", TaskWeight.LOW, (chunks) -> {
            for (ChunkerChunk chunk : chunks) column.getChunks().put(chunk.getY(), chunk);
        });
    }

    protected ChunkerColumn postProcess(ChunkerColumn column) {
        for (ChunkerChunk chunk : column.getChunks().values()) {
            resolvers.blockEntityResolver().generateBeforeProcessBlockEntities(column, chunk);
            resolvers.entityResolver().generateBeforeProcessEntities(column, chunk);
        }

        List<BlockEntity> blockEntities = column.getBlockEntities();
        for (int i = 0; i < blockEntities.size(); i++) {
            BlockEntity blockEntity = blockEntities.get(i);
            BlockEntity replacement = resolvers.blockEntityResolver().updateBeforeProcess(column, blockEntity.getX(), blockEntity.getY(), blockEntity.getZ(), blockEntity);
            if (replacement != blockEntity) blockEntities.set(i, replacement);
        }

        List<Entity> entities = column.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            Entity replacement = resolvers.entityResolver().updateBeforeProcess(column, entity);
            if (replacement != entity) entities.set(i, replacement);
        }

        column.getBlockEntities().removeIf(blockEntity -> resolvers.blockEntityResolver().shouldRemoveBeforeProcess(column, blockEntity.getX(), blockEntity.getY(), blockEntity.getZ(), blockEntity));
        column.getEntities().removeIf(entity -> resolvers.entityResolver().shouldRemoveBeforeProcess(column, entity));
        resolvers.preTransformManager().solve(column, converter.shouldProcessColumnPreTransform());

        return column;
    }

    public BedrockChunkReader createChunkReader(ChunkerChunk chunk) {
        return new BedrockChunkReader(resolvers, converter, dimension, chunk);
    }
}