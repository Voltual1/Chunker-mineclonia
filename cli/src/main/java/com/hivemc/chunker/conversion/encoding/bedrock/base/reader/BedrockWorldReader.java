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

    // EXTREME MICRO-BATCHING: Process only 8 columns at a time to minimize memory footprint
    private static final int MAX_CONCURRENT_COLUMNS = 8;

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

        Task<Void> regionProcessing = convertWorld.thenUnwrap("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return Task.async("Skip", TaskWeight.LOW, () -> {});

            // Kick off the fully asynchronous recursive promise chain
            List<Map.Entry<RegionCoordPair, Set<ChunkCoordPair>>> regionList = new ArrayList<>(presentRegions.entrySet());
            return processRegionsAsync(regionList, 0, columnConversionHandler);
        });

        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    /**
     * Asynchronously process regions one by one using a promise chain.
     */
    protected Task<Void> processRegionsAsync(List<Map.Entry<RegionCoordPair, Set<ChunkCoordPair>>> regions, int index, ColumnConversionHandler handler) {
        if (index >= regions.size() || converter.isCancelled()) {
            handler.flushColumns();
            return Task.async("Regions Done", TaskWeight.LOW, () -> {});
        }

        Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region = regions.get(index);
        if (!converter.shouldProcessRegion(dimension, region.getKey())) {
            return processRegionsAsync(regions, index + 1, handler);
        }

        List<ChunkCoordPair> chunks = new ArrayList<>(region.getValue());
        return processBatchAsync(chunks, 0, handler).thenUnwrap("Next Region", TaskWeight.HIGHER, (ignore) -> {
            handler.flushRegion(region.getKey());
            System.gc(); // Explicit hint to JVM for memory reclaim
            
            // Force sleep to give the Android GC thread breathing room
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return processRegionsAsync(regions, index + 1, handler);
        });
    }

    /**
     * Micro-batching using Promise chains. Processes limited columns, yields, and chains the next batch.
     */
    protected Task<Void> processBatchAsync(List<ChunkCoordPair> chunks, int startIndex, ColumnConversionHandler handler) {
        if (startIndex >= chunks.size() || converter.isCancelled()) {
            return Task.async("Batch Done", TaskWeight.LOW, () -> {});
        }

        int endIndex = Math.min(startIndex + MAX_CONCURRENT_COLUMNS, chunks.size());
        List<Task<Void>> activeBatches = new ArrayList<>(endIndex - startIndex);

        for (int i = startIndex; i < endIndex; i++) {
            ChunkCoordPair chunkCoordPair = chunks.get(i);
            if (!converter.shouldProcessColumn(dimension, chunkCoordPair)) continue;

            BedrockColumnReader columnReader = createColumnReader(chunkCoordPair);
            Task<Void> columnTask = columnReader.readColumn(handler);
            activeBatches.add(columnTask);
        }

        if (activeBatches.isEmpty()) {
            return processBatchAsync(chunks, endIndex, handler);
        }

        // Wait for all columns in this batch to fully complete processing
        return Task.join(activeBatches).thenUnwrap("Next Batch", TaskWeight.HIGHER, (ignore) -> {
            
            // YIELD TO GC: Suspend the worker thread briefly to guarantee GC gets CPU time
            System.gc();
            try {
                Thread.sleep(25); // 25ms rest period for GC thread
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return processBatchAsync(chunks, endIndex, handler);
        });
    }

    public BedrockColumnReader createColumnReader(ChunkCoordPair worldChunkCoords) {
        return new BedrockColumnReader(resolvers, converter, database, dimension, worldChunkCoords);
    }
}