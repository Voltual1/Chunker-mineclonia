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

import java.util.Iterator;
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

    /**
     * Create a new Bedrock world reader.
     *
     * @param resolvers      the resolvers to be used.
     * @param converter      the converter instance.
     * @param database       the LevelDB database.
     * @param presentRegions the regions present in the world.
     * @param dimension      the dimension being converted.
     */
    public BedrockWorldReader(BedrockResolvers resolvers, Converter converter, DB database, Map<RegionCoordPair, Set<ChunkCoordPair>> presentRegions, Dimension dimension) {
        this.database = database;
        this.resolvers = resolvers;
        this.converter = converter;
        this.presentRegions = presentRegions;
        this.dimension = dimension;
    }

    @Override
    public void readWorld(WorldConversionHandler worldConversionHandler) {
        // Use a copy for the present region hashset in the case of something modifying it
        ChunkerWorld chunkerWorld = new ChunkerWorld(
                dimension,
                new ObjectOpenHashSet<>(presentRegions.keySet())
        );

        // Submit world info (done before column reading)
        Task<ColumnConversionHandler> convertWorld = worldConversionHandler.convertWorld(chunkerWorld);

        // Handle the reading of the regions
        Task<Void> regionProcessing = convertWorld.thenUnwrap("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return null;

            // Read the regions sequentially to prevent OOM
            Task<Void> readingRegionFiles = readRegionsSequentially(presentRegions, columnConversionHandler);

            // Call the flush task after all the region files have been read
            return readingRegionFiles.then("Flushing columns", TaskWeight.MEDIUM, columnConversionHandler::flushColumns);
        });

        // When the region processing is done flush the world
        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    /**
     * Read all the regions in the world sequentially.
     *
     * @param regions                 the regions to read.
     * @param columnConversionHandler the handler to submit the read columns to.
     * @return a task that finishes when all regions have been processed.
     */
    public Task<Void> readRegionsSequentially(Map<RegionCoordPair, Set<ChunkCoordPair>> regions, ColumnConversionHandler columnConversionHandler) {
        return readRegionsSequentially(regions.entrySet().iterator(), columnConversionHandler);
    }

    private Task<Void> readRegionsSequentially(Iterator<Map.Entry<RegionCoordPair, Set<ChunkCoordPair>>> iterator, ColumnConversionHandler columnConversionHandler) {
        if (!iterator.hasNext()) {
            return Task.async("Finished regions", TaskWeight.LOW, () -> {});
        }
        Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region = iterator.next();
        if (converter.shouldProcessRegion(dimension, region.getKey())) {
            return Task.async("Reading region " + region.getKey(), TaskWeight.NORMAL, () -> readRegion(region, columnConversionHandler))
                    .then("Region - Flushing " + region.getKey(), TaskWeight.MEDIUM, () -> columnConversionHandler.flushRegion(region.getKey()))
                    .then("GC", TaskWeight.LOW, System::gc)
                    .thenUnwrap("Next region", TaskWeight.LOW, (ignored) -> readRegionsSequentially(iterator, columnConversionHandler));
        } else {
            return readRegionsSequentially(iterator, columnConversionHandler);
        }
    }

    /**
     * Read all the columns in a region.
     *
     * @param region                  the region to read with columns.
     * @param columnConversionHandler the handler to submit the columns to.
     */
    public void readRegion(Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region, ColumnConversionHandler columnConversionHandler) {
        for (ChunkCoordPair chunkCoordPair : region.getValue()) {
            if (!converter.shouldProcessColumn(dimension, chunkCoordPair)) continue;
            Task.async("Creating Column Reader", TaskWeight.LOW, () -> createColumnReader(chunkCoordPair))
                    .thenConsume("Reading Column", TaskWeight.HIGHER, (columnReader) -> columnReader.readColumn(columnConversionHandler));
        }
    }

    /**
     * Create the column reader used for reading a column.
     *
     * @param worldChunkCoords the column co-ordinates being read.
     * @return the new column reader.
     */
    public BedrockColumnReader createColumnReader(ChunkCoordPair worldChunkCoords) {
        return new BedrockColumnReader(resolvers, converter, database, dimension, worldChunkCoords);
    }
}