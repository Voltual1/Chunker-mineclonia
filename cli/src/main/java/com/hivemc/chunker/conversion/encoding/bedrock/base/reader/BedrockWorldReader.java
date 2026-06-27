// [file name]: com.hivemc.chunker.conversion.encoding.bedrock.base.reader.BedrockWorldReader.java
package com.hivemc.chunker.conversion.encoding.bedrock.base.reader;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.reader.WorldReader;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.scheduling.task.ProgressiveTask;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.iq80.leveldb.DB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A reader for Bedrock dimensions.
 */
public class BedrockWorldReader implements WorldReader {
    protected final BedrockResolvers resolvers;
    protected final Converter converter;
    protected final Map<RegionCoordPair, Set<ChunkCoordPair>> presentRegions;
    protected final Dimension dimension;
    protected final DB database;

    private static final int MAX_CONCURRENT_COLUMNS = 32;

    public BedrockWorldReader(BedrockResolvers resolvers, Converter converter, DB database, Map<RegionCoordPair, Set<ChunkCoordPair>> presentRegions, Dimension dimension) {
        this.database = database;
        this.resolvers = resolvers;
        this.converter = converter;
        this.presentRegions = presentRegions;
        this.dimension = dimension;
    }

    @Override
    public void readWorld(WorldConversionHandler worldConversionHandler) {
        ChunkerWorld chunkerWorld = new ChunkerWorld(
                dimension,
                new ObjectOpenHashSet<>(presentRegions.keySet())
        );

        Task<ColumnConversionHandler> convertWorld = worldConversionHandler.convertWorld(chunkerWorld);

        Task<Void> regionProcessing = convertWorld.thenConsume("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return;

            try {
                readRegionsSync(presentRegions, columnConversionHandler);
            } catch (Throwable t) {
                converter.logNonFatalException(t);
            }

            columnConversionHandler.flushColumns();
        });

        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    public void readRegionsSync(Map<RegionCoordPair, Set<ChunkCoordPair>> regions, ColumnConversionHandler columnConversionHandler) {
        for (Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region : regions.entrySet()) {
            if (converter.isCancelled()) break;
            if (converter.shouldProcessRegion(dimension, region.getKey())) {
                readRegionSync(region, columnConversionHandler);
                columnConversionHandler.flushRegion(region.getKey()); 
                System.gc(); 
            }
        }
    }

    public void readRegionSync(Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region, ColumnConversionHandler columnConversionHandler) {
        List<Task<Void>> activeBatches = new ArrayList<>(MAX_CONCURRENT_COLUMNS);

        for (ChunkCoordPair chunkCoordPair : region.getValue()) {
            if (converter.isCancelled()) break;
            if (!converter.shouldProcessColumn(dimension, chunkCoordPair)) continue;

            if (activeBatches.size() >= MAX_CONCURRENT_COLUMNS) {
                try {
                    Task.join(activeBatches).future().get();
                } catch (Exception e) {
                    converter.logNonFatalException(e);
                }
                activeBatches.clear();
            }

            BedrockColumnReader columnReader = createColumnReader(chunkCoordPair);
            Task<Void> columnTask = Task.asyncConsume("Reading Column", TaskWeight.HIGHER, columnReader::readColumn, columnConversionHandler);
            activeBatches.add(columnTask);
        }

        if (!activeBatches.isEmpty()) {
            try {
                Task.join(activeBatches).future().get();
            } catch (Exception e) {
                converter.logNonFatalException(e);
            }
            activeBatches.clear();
        }
    }

    public BedrockColumnReader createColumnReader(ChunkCoordPair worldChunkCoords) {
        return new BedrockColumnReader(resolvers, converter, database, dimension, worldChunkCoords);
    }
}