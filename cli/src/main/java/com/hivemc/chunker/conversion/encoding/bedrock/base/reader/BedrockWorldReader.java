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
        Task<Void> regionProcessing = convertWorld.thenConsume("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return;

            try {
                // Synchronously loop over regions (blocks the single dimension reader task safely)
                readRegionsSync(presentRegions, columnConversionHandler);
            } catch (Throwable t) {
                converter.logNonFatalException(t);
            }

            // Call the flush task after all the region files have been read
            columnConversionHandler.flushColumns();
        });

        // When the region processing is done flush the world
        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    /**
     * Synchronously read regions one by one.
     */
    public void readRegionsSync(Map<RegionCoordPair, Set<ChunkCoordPair>> regions, ColumnConversionHandler columnConversionHandler) {
        for (Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region : regions.entrySet()) {
            if (converter.isCancelled()) break;
            if (converter.shouldProcessRegion(dimension, region.getKey())) {
                readRegionSync(region, columnConversionHandler);
                columnConversionHandler.flushRegion(region.getKey()); // Blocks until all active writes of this region are finished
                System.gc(); // Suggest Garbage Collection after each region
            }
        }
    }

    /**
     * Synchronously read a region's columns.
     */
    public void readRegionSync(Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region, ColumnConversionHandler columnConversionHandler) {
        for (ChunkCoordPair chunkCoordPair : region.getValue()) {
            if (converter.isCancelled()) break;
            if (!converter.shouldProcessColumn(dimension, chunkCoordPair)) continue;

            // Throttle: wait if there are too many active writes to avoid memory bloat
            converter.awaitFreeColumnSlot();
            converter.incrementActiveColumns();

            // Process column asynchronously
            BedrockColumnReader columnReader = createColumnReader(chunkCoordPair);
            Task.asyncConsume("Reading Column " + chunkCoordPair, TaskWeight.HIGHER,
                    columnReader::readColumn, columnConversionHandler);
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