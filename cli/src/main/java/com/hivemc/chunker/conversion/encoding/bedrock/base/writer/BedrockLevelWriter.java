// [file name]: com.hivemc.chunker.conversion.encoding.bedrock.base.writer.BedrockLevelWriter.java
package com.hivemc.chunker.conversion.encoding.bedrock.base.writer;

import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.Version;
import com.hivemc.chunker.conversion.encoding.base.writer.LevelWriter;
import com.hivemc.chunker.conversion.encoding.base.writer.WorldWriter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.BedrockReaderWriter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.reader.BedrockLevelReader;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey;
import com.hivemc.chunker.conversion.handlers.pretransform.manager.PreTransformManager;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerLodestoneData;
import com.hivemc.chunker.conversion.intermediate.level.*;
import com.hivemc.chunker.conversion.intermediate.level.map.ChunkerMap;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.IntTag;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.iq80.leveldb.table.BloomFilterPolicy;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * A writer for Bedrock levels.
 */
public class BedrockLevelWriter implements LevelWriter, BedrockReaderWriter {
    public static final String VOID_WORD_STRING = "{ " +
            "\"biome_id\" : 1, " +
            "\"block_layers\" : [{" +
            "\"block_data\" : 0, " +
            "\"block_name\" : \"minecraft:air\", " +
            "\"count\" : 1 " +
            "}], " +
            "\"encoding_version\" : 4,  " +
            "\"structure_options\" : null" +
            "}";

    protected final File outputFolder;
    protected final Version version;
    protected final Converter converter;
    protected final BedrockResolvers resolvers;
    protected DB database;

    public BedrockLevelWriter(File outputFolder, Version version, Converter converter) {
        this.outputFolder = outputFolder;
        this.version = version;
        this.converter = converter;
        resolvers = buildResolvers(converter).build();
    }

    @Override
    public Set<ChunkerBiome.ChunkerVanillaBiome> getSupportedBiomes() {
        return resolvers.biomeIdResolver().getSupportedBiomes();
    }

    protected void openDatabase() throws IOException {
        File databaseDirectory = new File(outputFolder, "db");
        databaseDirectory.mkdirs();

        new File(databaseDirectory, "LOCK").delete();

        Options options = new Options();
        options.compressionType(CompressionType.ZLIB_RAW);
        // FIX MEMORY BOMB: Lower LevelDB memory usage for mobile environments
        options.blockSize(4 * 1024); // 4KB block size (Standard)
        options.filterPolicy(new BloomFilterPolicy(10));
        options.writeBufferSize(8 * 1024 * 1024); // 8MB write buffer (down from 400MB)
        options.createIfMissing(true);

        DBFactory factory = new Iq80DBFactory();
        database = factory.open(databaseDirectory, options);

        if (converter.shouldAllowNBTCopying()) {
            remapExistingDB();
        }
    }

    protected void remapExistingDB() throws IOException {
        List<byte[]> removals = new ArrayList<>();
        try (DBIterator iterator = database.iterator()) {
            DimensionRegistry dimensionRegistry = converter.getDimensionRegistry();
            while (iterator.hasNext()) {
                Map.Entry<byte[], byte[]> entry = iterator.next();
                byte[] key = entry.getKey();
                int keyLength = key.length;

                boolean containsSubChunk = keyLength == 14 || keyLength == 10;
                boolean containsDimension = keyLength == 14 || keyLength == 13;

                if (keyLength != 9 && !containsSubChunk && !containsDimension) continue;

                if (Arrays.equals(key, LevelDBKey.LOCAL_PLAYER)) {
                    continue;
                }

                ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);

                int x = buffer.getInt();
                int z = buffer.getInt();

                Dimension dimension = Dimension.OVERWORLD;
                if (containsDimension) {
                    int dimensionID = buffer.getInt();
                    dimension = dimensionRegistry.fromBedrock(dimensionID, null);

                    if (dimension == null) {
                        converter.logNonFatalException(new Exception("Unknown dimension key " + dimensionID));
                        removals.add(key);
                        continue;
                    }
                }
                byte subChunkY = 0;
                if (containsSubChunk) {
                    subChunkY = buffer.get();
                }
                byte type = buffer.get();

                Optional<Dimension> newDimension = converter.getNewDimension(dimension);
                ChunkCoordPair chunkCoordPair = new ChunkCoordPair(x, z);
                if (newDimension.isPresent() && converter.shouldProcessColumn(dimension, chunkCoordPair)) {
                    if (newDimension.get() != dimension) {
                        byte[] value = entry.getValue();
                        removals.add(key);

                        if (containsSubChunk) {
                            database.put(LevelDBKey.key(newDimension.get(), chunkCoordPair, subChunkY, type), value);
                        } else {
                            database.put(LevelDBKey.key(newDimension.get(), chunkCoordPair, type), value);
                        }
                    }
                } else {
                    removals.add(key);
                }
            }
        }

        try (WriteBatch writeBatch = database.createWriteBatch()) {
            for (byte[] key : removals) {
                writeBatch.delete(key);
            }
            database.write(writeBatch);
            removals.clear();
        }
    }

    @Override
    public void free() throws Exception {
        if (database != null) {
            try {
                database.close();
            } finally {
                database = null;
            }
        }
    }

    @Override
    public void flushLevel() {
        try {
            writeBiomeList();
        } catch (Exception e) {
            converter.logNonFatalException(e);
        }

        if (converter.shouldLevelDBCompaction()) {
            Task.signal(WorldConverter.SIGNAL_COMPACTION, true);
            database.compactRange(null, null);
            Task.signal(WorldConverter.SIGNAL_COMPACTION, false);
        }
    }

    @Override
    public WorldWriter writeLevel(ChunkerLevel chunkerLevel) throws Exception {
        openDatabase();

        Task.asyncConsume("Writing Level Data", TaskWeight.NORMAL, this::writeLevelData, chunkerLevel);

        if (version.isGreaterThan(1, 26, 10)) {
            CompoundTag dimensionTable = new CompoundTag(1);
            CompoundTag entries = new CompoundTag(4);

            for (Dimension dimension : converter.getDimensionRegistry().getDimensions()) {
                if (dimension.getBedrockID() < 1000) continue;
                entries.put(dimension.getIdentifier(), dimension.getBedrockID());
            }

            if (entries.size() > 0) {
                dimensionTable.put("entries", entries);
                byte[] value = Tag.writeBedrockNBT(dimensionTable);
                database.put(LevelDBKey.DIMENSION_NAME_ID_TABLE, value);
            }
        }

        return createWorldWriter();
    }

    protected void writeLevelData(ChunkerLevel chunkerLevel) {
        Task.asyncConsume("Writing Level Settings", TaskWeight.NORMAL, this::writeLevelSettings, chunkerLevel);
        Task.asyncConsume("Writing Local Player", TaskWeight.NORMAL, this::writeLocalPlayer, chunkerLevel);
        Task.asyncConsume("Writing Saved Maps", TaskWeight.NORMAL, this::writeMaps, chunkerLevel);
        Task.asyncConsume("Writing Portals", TaskWeight.NORMAL, this::writePortals, chunkerLevel);
    }

    protected void writeMaps(ChunkerLevel chunkerLevel) {
        if (chunkerLevel.getMaps().isEmpty()) {
            return;
        }
        Task.asyncConsumeForEach("Writing Saved Map", TaskWeight.NORMAL, this::writeMap, chunkerLevel.getMaps());
    }

    protected CompoundTag prepareMap(ChunkerMap chunkerMap) throws Exception {
        CompoundTag mapData = chunkerMap.getOriginalNBT() != null ? chunkerMap.getOriginalNBT() : new CompoundTag(12);
        mapData.put("mapId", chunkerMap.getId());

        if (!mapData.contains("parentMapId")) {
            mapData.put("parentMapId", -1L);
        }

        if (!mapData.contains("decorations")) {
            mapData.put("decorations", new ListTag<>(TagType.COMPOUND));
        }

        mapData.put("scale", mapData.getLong("parentMapId", -1L) == -1L ? (byte) 4 : chunkerMap.getScale());

        boolean dimensionShouldUseByte = getVersion().isLessThanOrEqual(1, 26, 10);
        if (dimensionShouldUseByte) {
            mapData.put("dimension", (byte) chunkerMap.getDimension().getBedrockID());
        }
        else {
            mapData.put("dimension", chunkerMap.getDimension().getBedrockID());
        }
        mapData.put("width", (short) chunkerMap.getWidth());
        mapData.put("height", (short) chunkerMap.getHeight());
        mapData.put("xCenter", chunkerMap.getXCenter());
        mapData.put("zCenter", chunkerMap.getZCenter());
        mapData.put("unlimitedTracking", chunkerMap.isUnlimitedTracking() ? (byte) 1 : (byte) 0);
        mapData.put("mapLocked", chunkerMap.isLocked() ? (byte) 1 : (byte) 0);
        if (chunkerMap.getBytes() != null) {
            mapData.put("colors", chunkerMap.getBytes());
        }
        return mapData;
    }

    protected void writeMap(ChunkerMap chunkerMap) throws Exception {
        CompoundTag mapData = prepareMap(chunkerMap);
        byte[] value = Tag.writeBedrockNBT(mapData);
        database.put(("map_" + chunkerMap.getId()).getBytes(StandardCharsets.UTF_8), value);
    }

    protected void writePortals(ChunkerLevel chunkerLevel) throws Exception {
        if (chunkerLevel.getPortals().isEmpty()) {
            return;
        }

        CompoundTag entry = new CompoundTag(1);
        ListTag<CompoundTag, Map<String, Tag<?>>> portalRecords = new ListTag<>(TagType.COMPOUND, chunkerLevel.getPortals().size());
        for (ChunkerPortal portal : chunkerLevel.getPortals()) {
            CompoundTag record = new CompoundTag(7);
            record.put("DimId", portal.getDimension().getBedrockID());
            record.put("Span", portal.getWidth());
            record.put("TpX", portal.getX());
            record.put("TpY", portal.getY());
            record.put("TpZ", portal.getZ());
            record.put("Xa", portal.getXa());
            record.put("Za", portal.getZa());
            portalRecords.add(record);
        }
        entry.put("PortalRecords", portalRecords);

        CompoundTag data = new CompoundTag(1);
        data.put("data", entry);

        byte[] value = Tag.writeBedrockNBT(data);
        database.put(LevelDBKey.PORTALS, value);
    }

    @Override
    public void writeCustomLevelSetting(ChunkerLevelSettings chunkerLevelSettings, CompoundTag output, String targetName, Object value) {
        if (targetName.equals("SummerDrop2026")) return;
        if (targetName.equals("AutumnDrop2025")) return;
        if (targetName.equals("SummerDrop2025")) return;
        if (targetName.equals("WinterDrop2024")) return;
        if (targetName.equals("R21Support")) return;
        if (targetName.equals("R20Support")) return;
        if (targetName.equals("CavesAndCliffs")) return;

        if (targetName.equals("FlatWorldVersion")) {
            output.put("WorldVersion", (int) value);
            return;
        }

        if (targetName.equals("RandomSeed")) {
            if (!output.contains("RandomSeed") || (int) output.getLong("RandomSeed") != (int) Long.parseLong((String) value)) {
                output.put("RandomSeed", Long.parseLong((String) value));
            }
            return;
        }

        if (value instanceof ChunkerGeneratorType type) {
            if (!converter.shouldAllowNBTCopying() && type == ChunkerGeneratorType.CUSTOM)
                type = ChunkerGeneratorType.VOID;

            switch (type) {
                case NORMAL:
                    output.put("Generator", 1);
                    return;
                case FLAT:
                    output.put("Generator", 2);
                    return;
                case VOID:
                    output.put("Generator", 2);
                    output.put("FlatWorldLayers", VOID_WORD_STRING);
                    return;
                case CUSTOM:
                    return;
            }
        }

        throw new IllegalArgumentException("Writing of " + targetName + " is not implemented.");
    }

    protected void enableExperiments(CompoundTag output, String... experiments) {
        CompoundTag experimentsTag = output.getOrCreateCompound("experiments");
        for (String experiment : experiments) {
            experimentsTag.put(experiment, (byte) 1);
        }
        experimentsTag.put("experiments_ever_used", (byte) 1);
        experimentsTag.put("saved_with_toggled_experiments", (byte) 1);
    }

    protected void writeLevelSettings(ChunkerLevel chunkerLevel) throws Exception {
        CompoundTag data = chunkerLevel.getOriginalLevelData() == null || !converter.shouldAllowNBTCopying() ? new CompoundTag(100) : chunkerLevel.getOriginalLevelData();
        chunkerLevel.getSettings().worldStartCount = 0xFFFFFFFFL - 1L;
        chunkerLevel.getSettings().toNBT(data, this, converter);

        if (!data.contains("Generator")) {
            data.put("Generator", 2);
        }

        if (!data.contains("StorageVersion")) {
            data.put("StorageVersion", resolvers.dataVersion().getStorageVersion());
        }

        if (!data.contains("NetworkVersion")) {
            data.put("NetworkVersion", resolvers.dataVersion().getProtocolVersion());
        }

        if (data.contains("SpawnY")) {
            int y = data.getInt("SpawnY");
            if (y == -1) {
                data.put("SpawnY", 32767);
            }
        }

        if (data.contains("GameType")) {
            int type = data.getInt("GameType");
            if (type == 3 || type == 4 || type == 6) {
                data.put("GameType", getVersion().isGreaterThanOrEqual(1, 18, 30) && getVersion().isLessThan(1, 19, 50) ? 6 : 2);
            }
        }

        data.put("LastPlayed", Instant.now().getEpochSecond());

        Version version = resolvers.dataVersion().getVersion();
        ListTag<IntTag, Integer> minimumVersion = new ListTag<>(TagType.INT, 5);
        minimumVersion.add(new IntTag(version.getMajor()));
        minimumVersion.add(new IntTag(version.getMinor()));
        minimumVersion.add(new IntTag(version.getPatch()));
        minimumVersion.add(new IntTag(0));
        minimumVersion.add(new IntTag(0));

        if (!data.contains("MinimumCompatibleClientVersion")) {
            data.put("MinimumCompatibleClientVersion", minimumVersion);
        }

        if (!data.contains("lastOpenedWithVersion")) {
            data.put("lastOpenedWithVersion", minimumVersion);
        }

        Tag.writeBedrockNBT(new File(outputFolder, "level.dat"), resolvers.dataVersion().getStorageVersion(), data);
    }

    protected void writeLocalPlayer(ChunkerLevel output) throws Exception {
        if (output.getPlayer() == null || converter.shouldAllowNBTCopying()) return;
        ChunkerLevelPlayer player = output.getPlayer();

        CompoundTag playerTag = new CompoundTag(9);

        playerTag.put("Pos", ListTag.fromValues(TagType.FLOAT, List.of(
                (float) player.getPositionX(),
                (float) player.getPositionY() + BedrockLevelReader.PLAYER_HEIGHT,
                (float) player.getPositionZ()
        )));
        playerTag.put("Motion", ListTag.fromValues(TagType.FLOAT, List.of(
                (float) player.getMotionX(),
                (float) player.getMotionY(),
                (float) player.getMotionZ()
        )));
        playerTag.put("Rotation", ListTag.fromValues(TagType.FLOAT, List.of(
                player.getYaw(),
                player.getPitch()
        )));

        ListTag<CompoundTag, Map<String, Tag<?>>> items = new ListTag<>(TagType.COMPOUND, player.getInventory().size());
        for (Byte2ObjectMap.Entry<ChunkerItemStack> tag : player.getInventory().byte2ObjectEntrySet()) {
            if ((tag.getByteKey() & 0xFF) >= 100) continue;
            if (tag.getValue().getIdentifier().isAir()) continue;

            Optional<CompoundTag> item = resolvers.writeItem(tag.getValue());
            if (item.isEmpty()) continue;

            item.get().put("Slot", tag.getByteKey());
            items.add(item.get());
        }
        playerTag.put("Inventory", items);

        ListTag<CompoundTag, Map<String, Tag<?>>> armor = new ListTag<>(TagType.COMPOUND, 4);
        for (int i = 3; i >= 0; i--) {
            ChunkerItemStack chunkerItemStack = player.getInventory().get((byte) (100 + i));
            if (chunkerItemStack == null) {
                chunkerItemStack = new ChunkerItemStack(ChunkerBlockIdentifier.AIR);
            }
            Optional<CompoundTag> item = resolvers.writeItem(chunkerItemStack);
            if (item.isEmpty()) continue;
            armor.add(item.get());
        }
        playerTag.put("Armor", armor);

        ListTag<CompoundTag, Map<String, Tag<?>>> offhand = new ListTag<>(TagType.COMPOUND, 1);
        for (int i = 0; i < 1; i++) {
            ChunkerItemStack chunkerItemStack = player.getInventory().get((byte) (150 + i));
            if (chunkerItemStack == null) {
                chunkerItemStack = new ChunkerItemStack(ChunkerBlockIdentifier.AIR);
            }
            Optional<CompoundTag> item = resolvers.writeItem(chunkerItemStack);
            if (item.isEmpty()) continue;
            offhand.add(item.get());
        }
        playerTag.put("Offhand", offhand);

        playerTag.put("DimensionId", player.getDimension().getBedrockID());

        if (player.getGameType() == 3 || player.getGameType() == 4 || player.getGameType() == 6) {
            playerTag.put("PlayerGameMode", getVersion().isGreaterThanOrEqual(1, 18, 30) && getVersion().isLessThan(1, 19, 50) ? 6 : 2);
        } else {
            playerTag.put("PlayerGameMode", player.getGameType());
        }

        CompoundTag movementAttribute = new CompoundTag(7);
        movementAttribute.put("Base", 0.1F);
        movementAttribute.put("Current", 0.1F);
        movementAttribute.put("DefaultMax", Float.MAX_VALUE);
        movementAttribute.put("DefaultMin", 0F);
        movementAttribute.put("Max", Float.MAX_VALUE);
        movementAttribute.put("Min", 0F);
        movementAttribute.put("Name", "minecraft:movement");
        playerTag.put("Attributes", new ListTag<>(TagType.COMPOUND, List.of(movementAttribute)));

        byte[] value = Tag.writeBedrockNBT(playerTag);
        database.put(LevelDBKey.LOCAL_PLAYER, value);
    }

    protected void writeBiomeList() throws Exception {}

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public @Nullable PreTransformManager getPreTransformManager() {
        return resolvers.preTransformManager();
    }

    @Override
    public int getOrCreateLodestoneData(ChunkerLodestoneData lodestoneData) {
        return -1;
    }

    @Override
    public @Nullable ChunkerLodestoneData getLodestoneData(int index) {
        return null;
    }

    public BedrockWorldWriter createWorldWriter() {
        return new BedrockWorldWriter(outputFolder, converter, resolvers, database);
    }
}