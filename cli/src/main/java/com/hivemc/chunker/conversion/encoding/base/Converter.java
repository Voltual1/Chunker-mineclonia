// [file name]: com.hivemc.chunker.conversion.encoding.base.Converter.java
package com.hivemc.chunker.conversion.encoding.base;

import com.google.common.base.CaseFormat;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry;
import com.hivemc.chunker.mapping.resolver.MappingsFileResolvers;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A converter is an interface for converter settings / feeding information back to the user.
 */
public interface Converter {
    boolean shouldLevelDBCompaction();
    boolean shouldProcessMaps();
    boolean shouldProcessItems();
    boolean shouldProcessEntities();
    boolean shouldProcessBlockEntities();
    boolean shouldProcessLootTables();
    boolean shouldProcessBiomes();
    boolean shouldProcessHeightMap();
    boolean shouldProcessColumnPreTransform();
    boolean shouldProcessLighting();
    boolean shouldProcessDimension(Dimension dimension);
    boolean shouldProcessRegion(Dimension dimension, RegionCoordPair regionPair);
    boolean shouldProcessColumn(Dimension dimension, ChunkCoordPair columnPair);
    boolean shouldAllowNBTCopying();
    boolean shouldAllowCustomIdentifiers();

    @Nullable
    MappingsFileResolvers getBlockMappings();
    DimensionRegistry getDimensionRegistry();

    default void logNonFatalException(Throwable throwable) {
        throwable.printStackTrace();
    }

    default void logMissingMapping(MissingMappingType type, String identifier) {
        System.err.println("Missing " + type.getName().replace('_', ' ') + " mapping for " + identifier);
    }

    boolean shouldDiscardEmptyChunks();
    boolean shouldPreventYBiomeBlending();
    Optional<Dimension> getNewDimension(Dimension dimension);
    ChunkerBiome getNewBiome(ChunkerBiome biome);
    Optional<ChunkerLevel> level();

    default void incrementActiveColumns() {}
    default void decrementActiveColumns() {}
    default void awaitFreeColumnSlot() {}

    /**
     * Block the current thread if the memory monitor has signaled a pause.
     */
    default void awaitMemoryPause() {}

    default boolean isCancelled() {
        return false;
    }

    enum MissingMappingType {
        BLOCK, ITEM, POTION, EFFECT, BIOME, ENTITY, ENTITY_TYPE, BLOCK_ENTITY, HORN, PAINTING, ENCHANTMENT, TRIM_MATERIAL, TRIM_PATTERN;

        private final String name;

        MissingMappingType() {
            name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, name());
        }

        public String getName() {
            return name;
        }
    }
}