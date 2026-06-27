// [file name]: com.hivemc.chunker.conversion.WorldConverter.java
package com.hivemc.chunker.conversion;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonObject;
import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.reader.LevelReader;
import com.hivemc.chunker.conversion.encoding.base.writer.LevelWriter;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.handlers.LevelConversionHandler;
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler;
import com.hivemc.chunker.conversion.handlers.pipeline.Pipeline;
import com.hivemc.chunker.conversion.handlers.pretransform.ColumnPreTransformConversionHandler;
import com.hivemc.chunker.conversion.handlers.pretransform.ColumnPreTransformWriterConversionHandler;
import com.hivemc.chunker.conversion.handlers.writer.LevelWriterConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelSettings;
import com.hivemc.chunker.conversion.intermediate.level.map.ChunkerMap;
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry;
import com.hivemc.chunker.mapping.resolver.MappingsFileResolvers;
import com.hivemc.chunker.pruning.PruningConfig;
import com.hivemc.chunker.pruning.PruningRegion;
import com.hivemc.chunker.scheduling.task.Environment;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import com.hivemc.chunker.scheduling.task.TrackedTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WorldConverter implements Converter {
    public static final String SIGNAL_COMPACTION = "signal_compaction";

    private final UUID sessionID;
    @Nullable
    protected Consumer<Boolean> compactionSignalConsumer;
    @Nullable
    protected ChunkerLevel level;
    @Nullable
    protected LevelReader reader = null;
    @Nullable
    protected LevelWriter writer = null;
    @Nullable
    protected Environment environment = null;
    protected Multimap<Converter.MissingMappingType, String> missingIdentifiers = Multimaps.synchronizedSetMultimap(
            MultimapBuilder.enumKeys(Converter.MissingMappingType.class).hashSetValues().build()
    );

    @Nullable
    private Map<Dimension, PruningConfig> pruningConfigs;
    @Nullable
    private Map<Dimension, Dimension> dimensionMapping;
    private DimensionRegistry dimensionRegistry = new DimensionRegistry();
    @Nullable
    private Map<ChunkerBiome, ChunkerBiome> biomeMapping;
    @Nullable
    private JsonObject changedSettings;
    @Nullable
    private List<ChunkerMap> maps;
    @Nullable
    private MappingsFileResolvers blockMappings;
    private boolean levelDBCompaction = true;
    private boolean processMaps = true;
    private boolean processItems = true;
    private boolean processEntities = true;
    private boolean processBlockEntities = true;
    private boolean processLootTables = true;
    private boolean processBiomes = true;
    private boolean processHeightMap = true;
    private boolean processLighting = true;
    private boolean processColumnPreTransform = true;
    private boolean allowNBTCopying = false;
    private boolean discardEmptyChunks = false;
    private boolean preventYBiomeBlending = false;
    private boolean customIdentifiers = true;
    private boolean exceptions = false;
    private boolean cancelled = false;

    // Memory control flag managed by Android frontend
    private volatile boolean memoryPaused = false;
    private int threadCount = 8; // Default

    public WorldConverter(UUID sessionID) {
        this.sessionID = sessionID;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public boolean isMemoryPaused() {
        return memoryPaused;
    }

    public void setMemoryPaused(boolean paused) {
        this.memoryPaused = paused;
    }

    @Override
    public void awaitMemoryPause() {
        // Block the reader thread until Android UI signals memory has recovered
        while (memoryPaused && !cancelled) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void incrementActiveColumns() {}

    @Override
    public void decrementActiveColumns() {}

    @Override
    public void awaitFreeColumnSlot() {}

    public void setCompactionSignal(@Nullable Consumer<Boolean> compactionSignalConsumer) { this.compactionSignalConsumer = compactionSignalConsumer; }
    public void setPruningConfigs(@Nullable Map<Dimension, PruningConfig> pruningConfigs) { this.pruningConfigs = pruningConfigs; }
    public void setDimensionMapping(@Nullable Map<Dimension, Dimension> dimensionMapping) { this.dimensionMapping = dimensionMapping; }
    public void setDimensionRegistry(@Nullable DimensionRegistry dimensionRegistry) { this.dimensionRegistry = dimensionRegistry; }
    public void setBiomeMapping(@Nullable Map<ChunkerBiome, ChunkerBiome> biomeMapping) { this.biomeMapping = biomeMapping; }
    public void setLevelDBCompaction(boolean levelDBCompaction) { this.levelDBCompaction = levelDBCompaction; }
    public void setProcessMaps(boolean processMaps) { this.processMaps = processMaps; }
    public void setPreventYBiomeBlending(boolean preventYBiomeBlending) { this.preventYBiomeBlending = preventYBiomeBlending; }
    public void setProcessItems(boolean processItems) { this.processItems = processItems; }
    public void setProcessEntities(boolean processEntities) { this.processEntities = processEntities; }
    public void setProcessBlockEntities(boolean processBlockEntities) { this.processBlockEntities = processBlockEntities; }
    public void setProcessLootTables(boolean processLootTables) { this.processLootTables = processLootTables; }
    public void setProcessBiomes(boolean processBiomes) { this.processBiomes = processBiomes; }
    public void setProcessHeightMap(boolean processHeightMap) { this.processHeightMap = processHeightMap; }
    public void setProcessLighting(boolean processLighting) { this.processLighting = processLighting; }
    public void setAllowNBTCopying(boolean allowNBTCopying) { this.allowNBTCopying = allowNBTCopying; }
    public void setDiscardEmptyChunks(boolean discardEmptyChunks) { this.discardEmptyChunks = discardEmptyChunks; }
    public void setProcessColumnPreTransform(boolean processColumnPreTransform) { this.processColumnPreTransform = processColumnPreTransform; }
    public void setCustomIdentifiers(boolean customIdentifiers) { this.customIdentifiers = customIdentifiers; }

    @Override public boolean shouldLevelDBCompaction() { return levelDBCompaction; }
    @Override public boolean shouldProcessMaps() { return processMaps; }
    @Override public boolean shouldProcessItems() { return processItems; }
    @Override public boolean shouldProcessEntities() { return processEntities; }
    @Override public boolean shouldProcessBlockEntities() { return processBlockEntities; }
    @Override public boolean shouldProcessLootTables() { return processLootTables; }
    @Override public boolean shouldProcessBiomes() { return processBiomes; }
    @Override public boolean shouldProcessHeightMap() { return processHeightMap; }
    @Override public boolean shouldProcessColumnPreTransform() { return processColumnPreTransform; }
    @Override public boolean shouldProcessLighting() { return processLighting; }
    @Override public boolean shouldPreventYBiomeBlending() { return preventYBiomeBlending; }
    @Override public boolean shouldProcessDimension(Dimension dimension) { return dimensionMapping == null || dimensionMapping.containsKey(dimension); }

    public boolean isCancelled() { return cancelled; }
    public boolean isExceptions() { return exceptions; }
    public Multimap<MissingMappingType, String> getMissingIdentifiers() { return missingIdentifiers; }

    @Override
    public boolean shouldProcessRegion(Dimension dimension, RegionCoordPair regionPair) {
        if (pruningConfigs == null || pruningConfigs.isEmpty()) return true;
        PruningConfig pruningConfig = pruningConfigs.get(dimension);
        if (pruningConfig == null || pruningConfig.getRegions() == null || pruningConfig.getRegions().isEmpty()) return true;

        ChunkCoordPair minRegionChunk = regionPair.getChunk(0, 0);
        ChunkCoordPair maxRegionChunk = regionPair.getChunk(31, 31);
        for (PruningRegion region : pruningConfig.getRegions()) {
            if (pruningConfig.isInclude()) {
                boolean overlap = maxRegionChunk.chunkX() >= region.getMinChunkX() && minRegionChunk.chunkX() <= region.getMaxChunkX() &&
                        maxRegionChunk.chunkZ() >= region.getMinChunkZ() && minRegionChunk.chunkZ() <= region.getMaxChunkZ();
                if (overlap) return true;
            } else {
                boolean fullyContained = minRegionChunk.chunkX() >= region.getMinChunkX() && maxRegionChunk.chunkX() <= region.getMaxChunkX() &&
                        minRegionChunk.chunkZ() >= region.getMinChunkZ() && maxRegionChunk.chunkZ() <= maxRegionChunk.chunkZ();
                if (fullyContained) return false;
            }
        }
        return !pruningConfig.isInclude();
    }

    @Override
    public boolean shouldProcessColumn(Dimension dimension, ChunkCoordPair columnPair) {
        if (pruningConfigs == null || pruningConfigs.isEmpty()) return true;
        PruningConfig pruningConfig = pruningConfigs.get(dimension);
        if (pruningConfig == null || pruningConfig.getRegions() == null || pruningConfig.getRegions().isEmpty()) return true;

        for (PruningRegion region : pruningConfig.getRegions()) {
            if (columnPair.chunkX() >= region.getMinChunkX() && columnPair.chunkX() <= region.getMaxChunkX() &&
                    columnPair.chunkZ() >= region.getMinChunkZ() && columnPair.chunkZ() <= region.getMaxChunkZ()) {
                return pruningConfig.isInclude();
            }
        }
        return !pruningConfig.isInclude();
    }

    @Override public boolean shouldAllowNBTCopying() { return allowNBTCopying; }
    @Override public boolean shouldAllowCustomIdentifiers() { return customIdentifiers; }
    @Override @Nullable public MappingsFileResolvers getBlockMappings() { return blockMappings; }
    @Override public DimensionRegistry getDimensionRegistry() { return dimensionRegistry; }
    public void setBlockMappings(@Nullable MappingsFileResolvers blockMappings) { this.blockMappings = blockMappings; }
    @Override public boolean shouldDiscardEmptyChunks() { return discardEmptyChunks; }

    @Override
    public Optional<Dimension> getNewDimension(Dimension dimension) {
        return dimensionMapping == null ? Optional.of(dimension) : Optional.ofNullable(dimensionMapping.get(dimension));
    }

    @Override
    public ChunkerBiome getNewBiome(ChunkerBiome biome) {
        return biomeMapping == null ? biome : biomeMapping.getOrDefault(biome, biome);
    }

    @Override
    public void logNonFatalException(Throwable throwable) {
        exceptions = true;
        Converter.super.logNonFatalException(throwable);
    }

    public void logFatalException(Throwable throwable) {
        cancel(throwable);
        logNonFatalException(throwable);
    }

    public void handleSignal(String signalName, Object signalValue) {
        if (signalName.equals(SIGNAL_COMPACTION)) {
            if (compactionSignalConsumer == null) return;
            compactionSignalConsumer.accept((Boolean) signalValue);
        }
    }

    @Override
    public void logMissingMapping(MissingMappingType type, String identifier) {
        if (missingIdentifiers.put(type, identifier)) {
            Converter.super.logMissingMapping(type, identifier);
        }
    }

    @Override
    public Optional<ChunkerLevel> level() { return Optional.ofNullable(level); }
    @Nullable public JsonObject getChangedSettings() { return changedSettings; }
    public void setChangedSettings(@Nullable JsonObject changedSettings) { this.changedSettings = changedSettings; }
    public void setMaps(@Nullable List<ChunkerMap> maps) { this.maps = maps; }

    public TrackedTask<Void> convert(@NotNull LevelReader reader, @NotNull LevelWriter writer) {
        this.reader = reader;
        this.writer = writer;
        cancelled = false;
        exceptions = false;
        memoryPaused = false;
        missingIdentifiers.clear();
        environment = Task.environment("World Conversion", threadCount, this::logFatalException, this::handleSignal);

        try {
            LevelWriterConversionHandler writerHandler = new LevelWriterConversionHandler(writer, this);
            Pipeline pipeline = new Pipeline(writerHandler);
            level = null;
            pipeline.levelHandlers((delegate) -> new LevelHandler(this, delegate));
            pipeline.worldHandlers((delegate, level) -> new WorldHandler(this, delegate));

            if (shouldProcessColumnPreTransform()) {
                // If pre-transform is enabled, keep the heavy pending/caching logic
                pipeline.columnHandlers(
                        (delegate, world) -> new ColumnPreTransformWriterConversionHandler(writer::getPreTransformManager, delegate, true),
                        ColumnPreTransformConversionHandler::new
                );
            } else {
                // FIX MEMORY LEAK: Completely omit the memory-hogging ColumnPreTransformConversionHandler caching layer
                pipeline.columnHandlers(
                        (delegate, world) -> new ColumnPreTransformWriterConversionHandler(writer::getPreTransformManager, delegate, false)
                );
            }

            LevelConversionHandler handler = pipeline.build();
            Task.asyncConsume("Reading Level", TaskWeight.NORMAL, reader::readLevel, handler);
            return environment;
        } finally {
            environment.close();
            environment.setFreeCallback(() -> {
                try { reader.free(); } catch (Throwable e) {}
                try { writer.free(); } catch (Throwable e) {}
            });
        }
    }

    public CompletableFuture<Void> cancel(@Nullable Throwable fatalException) {
        cancelled = true;
        if (environment != null) {
            environment.cancel(fatalException);
            return environment.future();
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    static class WorldHandler implements WorldConversionHandler {
        private final WorldConverter worldConverter;
        private final WorldConversionHandler delegate;

        public WorldHandler(WorldConverter worldConverter, WorldConversionHandler delegate) {
            this.worldConverter = worldConverter;
            this.delegate = delegate;
        }

        @Override
        public Task<ColumnConversionHandler> convertWorld(ChunkerWorld world) {
            Optional<Dimension> newDimension = worldConverter.getNewDimension(world.getDimension());
            if (newDimension.isPresent()) {
                world.setDimension(newDimension.get());
                return delegate.convertWorld(world);
            }
            return Task.asyncUnwrap("Empty Dimension", TaskWeight.LOW, () -> null);
        }

        @Override public void flushWorld(ChunkerWorld world) { delegate.flushWorld(world); }
        @Override public void flushWorlds() { delegate.flushWorlds(); }
    }

    static class LevelHandler implements LevelConversionHandler {
        private final WorldConverter worldConverter;
        private final LevelConversionHandler delegate;

        public LevelHandler(WorldConverter worldConverter, LevelConversionHandler delegate) {
            this.worldConverter = worldConverter;
            this.delegate = delegate;
        }

        @Override
        public Task<WorldConversionHandler> convertLevel(ChunkerLevel level) {
            if (worldConverter.maps != null) level.setMaps(worldConverter.maps);

            level.getMaps().removeIf(map -> {
                Optional<Dimension> newDimension = worldConverter.getNewDimension(map.getDimension());
                if (newDimension.isPresent()) {
                    map.setDimension(newDimension.get());
                    return false;
                } else return true;
            });

            level.getPortals().removeIf(portal -> {
                Optional<Dimension> newDimension = worldConverter.getNewDimension(portal.getDimension());
                if (newDimension.isPresent()) {
                    portal.setDimension(newDimension.get());
                    return false;
                } else return true;
            });

            JsonObject baseSettings = level.getSettings().toJSON();
            if (worldConverter.getChangedSettings() != null) {
                baseSettings.asMap().putAll(worldConverter.getChangedSettings().asMap());
            }

            level.setSettings(ChunkerLevelSettings.fromJSON(baseSettings));
            worldConverter.level = level;
            return delegate.convertLevel(level);
        }

        @Override public void flushLevel() { delegate.flushLevel(); }
    }
}