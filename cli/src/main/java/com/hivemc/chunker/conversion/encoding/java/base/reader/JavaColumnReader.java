package com.hivemc.chunker.conversion.encoding.java.base.reader;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.reader.ColumnReader;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.column.biome.layout.ChunkerColumnBasedBiomes;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkerChunk;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.column.heightmap.JavaLegacyHeightMap;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.array.ByteArrayTag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.ByteTag;
import com.hivemc.chunker.nbt.tags.primitive.IntTag;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;

import java.util.*;

/**
 * A reader for Java columns.
 */
public class JavaColumnReader implements ColumnReader {
    private static final Set<String> UNFINISHED_STATUSES = Set.of(
            "empty",
            "structure_starts",
            "structure_references",
            "biomes",
            "noise",
            "surface",
            "carvers",
            "liquid_carvers",
            "minecraft:empty",
            "minecraft:structure_starts",
            "minecraft:structure_references",
            "minecraft:biomes",
            "minecraft:noise",
            "minecraft:surface",
            "minecraft:carvers",
            "minecraft:liquid_carvers"
    );

    protected final Converter converter;
    protected final JavaResolvers resolvers;
    protected final Dimension dimension;
    protected final ChunkCoordPair columnCoords;
    protected CompoundTag columnNBT;

    /**
     * Create a new java column reader.
     *
     * @param converter    the converter instance.
     * @param resolvers    the resolvers to use.
     * @param dimension    the dimension this column is inside.
     * @param columnCoords the co-ordinates of the column.
     * @param columnNBT    the NBT of the column.
     */
    public JavaColumnReader(Converter converter, JavaResolvers resolvers, Dimension dimension, ChunkCoordPair columnCoords, CompoundTag columnNBT) {
        this.converter = converter;
        this.resolvers = resolvers;
        this.dimension = dimension;
        this.columnCoords = columnCoords;
        this.columnNBT = columnNBT;
    }

    @Override
    public Task<Void> readColumn(ColumnConversionHandler columnConversionHandler) {
        if (columnNBT == null) return Task.async("Skipped Column", TaskWeight.LOW, () -> {});

        columnNBT = columnNBT.getCompound("Level", columnNBT);

        if (!columnNBT.contains("xPos") || !columnNBT.contains("zPos")) {
            return Task.async("Skipped Column", TaskWeight.LOW, () -> {});
        }

        int xPos = columnNBT.getInt("xPos");
        int zPos = columnNBT.getInt("zPos");
        if (xPos != columnCoords.chunkX() || zPos != columnCoords.chunkZ()) {
            converter.logNonFatalException(new Exception("Mislocated chunk, chunk states " + xPos + ", " + zPos + " but actually at " + columnCoords));
        }

        if (columnNBT.contains("Status") && UNFINISHED_STATUSES.contains(columnNBT.getString("Status"))) {
            return Task.async("Skipped Column", TaskWeight.LOW, () -> {});
        }

        ChunkerColumn column = new ChunkerColumn(columnCoords);
        readLightPopulated(column);

        ArrayList<Task<Void>> processing = new ArrayList<>(5);
        if (converter.shouldProcessHeightMap()) {
            processing.add(Task.asyncConsume("Reading HeightMap", TaskWeight.NORMAL, this::readHeightMap, column));
        }
        if (converter.shouldProcessBiomes()) {
            processing.add(Task.asyncConsume("Reading Biomes", TaskWeight.NORMAL, this::readBiomes, column));
        }
        if (converter.shouldProcessEntities()) {
            processing.add(Task.asyncConsume("Reading Entities", TaskWeight.HIGH, this::readEntities, column));
        }
        if (converter.shouldProcessBlockEntities()) {
            processing.add(Task.asyncConsume("Reading Block Entities", TaskWeight.HIGH, this::readBlockEntities, column));
        }
        processing.add(Task.asyncConsume("Reading Chunks", TaskWeight.HIGHER, this::readChunks, column));

        return Task.join(processing)
                .then("Post-processing column", TaskWeight.HIGH, this::postProcess, column)
                .thenConsume("Submitting column", TaskWeight.LOW, columnConversionHandler::convertColumn);
    }

    protected void readLightPopulated(ChunkerColumn column) {
        if (columnNBT.contains("LightPopulated")) {
            column.setLightPopulated(columnNBT.getByte("LightPopulated") != (byte) 0);
        }
    }

    protected void readHeightMap(ChunkerColumn column) {
        if (!columnNBT.contains("HeightMap")) return;

        short[][] heightMapOutput = new short[16][16];
        int[] heightMap = columnNBT.getIntArray("HeightMap");
        for (int i = 0; i < heightMap.length; i++) {
            heightMapOutput[i & 0xF][(i >> 4) & 0xF] = (short) heightMap[i];
        }
        column.setHeightMap(new JavaLegacyHeightMap(heightMapOutput));
    }

    protected void readBiomes(ChunkerColumn column) {
        Tag<?> biomes = columnNBT.get("Biomes", Tag.class);
        if (biomes == null) return;

        if (biomes instanceof ByteArrayTag byteArrayTag && byteArrayTag.getValue() != null) {
            ChunkerBiome[] chunkerBiomeArray = new ChunkerBiome[256];
            byte[] value = byteArrayTag.getValue();

            for (int i = 0; i < chunkerBiomeArray.length; i++) {
                chunkerBiomeArray[i] = resolvers.readBiome(value[i] & 0xFF, dimension);
            }
            column.setBiomes(new ChunkerColumnBasedBiomes(chunkerBiomeArray));
        }
    }

    protected void readEntities(ChunkerColumn column) {
        ListTag<CompoundTag, Map<String, Tag<?>>> entities = columnNBT.getList("Entities", CompoundTag.class, null);
        if (entities != null) {
            for (CompoundTag entityTag : entities) {
                try {
                    readEntity(column, entityTag);
                } catch (Exception e) {
                    converter.logNonFatalException(new Exception("Failed to process Entity " + entityTag, e));
                }
            }
        }
    }

    protected void readEntity(ChunkerColumn chunkerColumn, CompoundTag compoundTag) {
        Optional<Entity> entity = resolvers.entityResolver().to(compoundTag);
        if (entity.isPresent()) {
            chunkerColumn.getEntities().add(entity.get());
        } else {
            String identifier = resolvers.entityResolver().getKey(compoundTag).map(Object::toString).orElseGet(compoundTag::toString);
            converter.logMissingMapping(Converter.MissingMappingType.ENTITY, identifier);
        }
    }

    protected void readBlockEntities(ChunkerColumn column) {
        ListTag<CompoundTag, Map<String, Tag<?>>> blockEntities = columnNBT.getList("TileEntities", CompoundTag.class, null);
        if (blockEntities != null) {
            for (CompoundTag blockEntityTag : blockEntities) {
                try {
                    readBlockEntity(column, blockEntityTag);
                } catch (Exception e) {
                    converter.logNonFatalException(new Exception("Failed to process BlockEntity " + blockEntityTag, e));
                }
            }
        }
    }

    protected void readBlockEntity(ChunkerColumn chunkerColumn, CompoundTag compoundTag) {
        Optional<BlockEntity> blockEntity = resolvers.blockEntityResolver().to(compoundTag);
        if (blockEntity.isPresent()) {
            chunkerColumn.getBlockEntities().add(blockEntity.get());
        } else {
            String identifier = resolvers.blockEntityResolver().getKey(compoundTag).orElseGet(compoundTag::toString);
            converter.logMissingMapping(Converter.MissingMappingType.BLOCK_ENTITY, identifier);
        }
    }

    protected void readChunks(ChunkerColumn column) {
        ListTag<CompoundTag, Map<String, Tag<?>>> sections = columnNBT.getList("Sections", CompoundTag.class, null);
        if (sections == null) {
            sections = columnNBT.getList("sections", CompoundTag.class, null);
            if (sections == null) {
                return;
            }
        }

        List<Task<ChunkerChunk>> tasks = new ArrayList<>(sections.size());

        for (CompoundTag section : sections) {
            Tag<?> tag = section.get("Y");
            if (tag == null || section.size() <= 1) continue;

            byte y;
            if (tag instanceof ByteTag byteYTag) {
                y = byteYTag.getValue();
            } else if (tag instanceof IntTag intYTag) {
                y = intYTag.getBoxedValue().byteValue();
            } else {
                throw new IllegalArgumentException("Invalid Section Y NBT Tag: " + tag);
            }

            ChunkerChunk chunk = new ChunkerChunk(y);

            tasks.add(Task.async("Creating Chunk Reader", TaskWeight.LOW, () -> createChunkReader(column, chunk))
                    .thenConsume("Reading Chunk", TaskWeight.HIGHER, (chunkReader) -> chunkReader.readChunk(section))
                    .then(chunk));
        }

        Task.join(tasks).thenConsume("Adding chunks to column", TaskWeight.LOW, (chunks) -> {
            for (ChunkerChunk chunk : chunks) {
                column.getChunks().put(chunk.getY(), chunk);
            }
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
            BlockEntity replacement = resolvers.blockEntityResolver().updateBeforeProcess(
                    column,
                    blockEntity.getX(),
                    blockEntity.getY(),
                    blockEntity.getZ(),
                    blockEntity
            );

            if (replacement != blockEntity) {
                blockEntities.set(i, replacement);
            }
        }

        List<Entity> entities = column.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            Entity replacement = resolvers.entityResolver().updateBeforeProcess(column, entity);

            if (replacement != entity) {
                entities.set(i, replacement);
            }
        }

        column.getBlockEntities().removeIf(blockEntity -> resolvers.blockEntityResolver().shouldRemoveBeforeProcess(
                column,
                blockEntity.getX(),
                blockEntity.getY(),
                blockEntity.getZ(),
                blockEntity
        ));

        column.getEntities().removeIf(entity -> resolvers.entityResolver().shouldRemoveBeforeProcess(
                column,
                entity
        ));

        resolvers.preTransformManager().solve(column, converter.shouldProcessColumnPreTransform());

        return column;
    }

    public JavaChunkReader createChunkReader(ChunkerColumn column, ChunkerChunk chunk) {
        return new JavaChunkReader(converter, resolvers, column, chunk);
    }
}