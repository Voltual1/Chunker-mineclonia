package com.hivemc.chunker.conversion.stitch;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry;
import com.hivemc.chunker.mapping.resolver.MappingsFileResolvers;
import com.hivemc.chunker.pruning.PruningConfig;
import com.hivemc.chunker.pruning.PruningRegion;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 专为区块缝合设计的轻量级 Converter 上下文，提供边界裁剪 (Pruning) 支持
 */
public class StitchConverter implements Converter {
    private final UUID sessionID;
    private final Dimension targetDimension;
    private final PruningConfig pruningConfig;
    private final DimensionRegistry dimensionRegistry = new DimensionRegistry();
    protected Multimap<MissingMappingType, String> missingIdentifiers = Multimaps.synchronizedSetMultimap(
            MultimapBuilder.enumKeys(MissingMappingType.class).hashSetValues().build()
    );
    private boolean cancelled = false;
    private volatile boolean memoryPaused = false;
    @Nullable
    protected Consumer<Boolean> compactionSignalConsumer;

    public StitchConverter(UUID sessionID, Dimension targetDimension, PruningConfig pruningConfig) {
        this.sessionID = sessionID;
        this.targetDimension = targetDimension;
        this.pruningConfig = pruningConfig;
    }

    public void setCompactionSignal(@Nullable Consumer<Boolean> compactionSignalConsumer) {
        this.compactionSignalConsumer = compactionSignalConsumer;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void awaitMemoryPause() {
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
    public boolean shouldLevelDBCompaction() { return true; }
    @Override
    public boolean shouldProcessMaps() { return false; } 
    @Override
    public boolean shouldProcessItems() { return true; }
    @Override
    public boolean shouldProcessEntities() { return true; }
    @Override
    public boolean shouldProcessBlockEntities() { return true; }
    @Override
    public boolean shouldProcessLootTables() { return false; }
    @Override
    public boolean shouldProcessBiomes() { return true; }
    @Override
    public boolean shouldProcessHeightMap() { return true; }

    // 极其重要：必须关闭跨区块预处理，否则被裁剪掉边界外的区块会导致边界内的区块发生互相等待的死锁！
    @Override
    public boolean shouldProcessColumnPreTransform() { return false; } 

    @Override
    public boolean shouldProcessLighting() { return true; }
    @Override
    public boolean shouldPreventYBiomeBlending() { return false; }
    @Override
    public boolean shouldAllowNBTCopying() { return true; } 
    @Override
    public boolean shouldAllowCustomIdentifiers() { return true; }
    @Override
    public boolean shouldDiscardEmptyChunks() { return true; }

    @Override
    public boolean shouldProcessDimension(Dimension dimension) {
        return dimension == targetDimension;
    }

    @Override
    public boolean shouldProcessRegion(Dimension dimension, RegionCoordPair regionPair) {
        if (dimension != targetDimension) return false;
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
        if (dimension != targetDimension) return false;
        if (pruningConfig == null || pruningConfig.getRegions() == null || pruningConfig.getRegions().isEmpty()) return true;

        for (PruningRegion region : pruningConfig.getRegions()) {
            if (columnPair.chunkX() >= region.getMinChunkX() && columnPair.chunkX() <= region.getMaxChunkX() &&
                    columnPair.chunkZ() >= region.getMinChunkZ() && columnPair.chunkZ() <= region.getMaxChunkZ()) {
                return pruningConfig.isInclude();
            }
        }
        return !pruningConfig.isInclude();
    }

    @Nullable
    @Override
    public MappingsFileResolvers getBlockMappings() { return null; }

    @Override
    public DimensionRegistry getDimensionRegistry() { return dimensionRegistry; }

    @Override
    public Optional<Dimension> getNewDimension(Dimension dimension) { return Optional.of(dimension); }

    @Override
    public ChunkerBiome getNewBiome(ChunkerBiome biome) { return biome; }

    @Override
    public Optional<ChunkerLevel> level() { return Optional.empty(); }

    @Override
    public void logMissingMapping(MissingMappingType type, String identifier) {
        if (missingIdentifiers.put(type, identifier)) {
            Converter.super.logMissingMapping(type, identifier);
        }
    }
}