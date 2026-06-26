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
import java.util.Iterator;
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
        ChunkerWorld chunkerWorld = new ChunkerWorld(
                dimension,
                new ObjectOpenHashSet<>(presentRegions.keySet())
        );

        Task<ColumnConversionHandler> convertWorld = worldConversionHandler.convertWorld(chunkerWorld);

        Task<Void> regionProcessing = convertWorld.thenUnwrap("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return null;

            Task<Void> readingRegionFiles = readRegionsSequentially(presentRegions, columnConversionHandler);
            return readingRegionFiles.then("Flushing columns", TaskWeight.MEDIUM, columnConversionHandler::flushColumns);
        });

        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    public Task<Void> readRegionsSequentially(Map<RegionCoordPair, Set<ChunkCoordPair>> regions, ColumnConversionHandler columnConversionHandler) {
        return readRegionsSequentially(regions.entrySet().iterator(), columnConversionHandler);
    }

    private Task<Void> readRegionsSequentially(Iterator<Map.Entry<RegionCoordPair, Set<ChunkCoordPair>>> iterator, ColumnConversionHandler columnConversionHandler) {
        if (!iterator.hasNext()) {
            return Task.async("Finished regions", TaskWeight.LOW, () -> {});
        }
        Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region = iterator.next();
        if (converter.shouldProcessRegion(dimension, region.getKey())) {
            return readRegion(region, columnConversionHandler)
                    .then("Region - Flushing " + region.getKey(), TaskWeight.MEDIUM, () -> columnConversionHandler.flushRegion(region.getKey()))
                    .then("GC", TaskWeight.LOW, System::gc)
                    .thenUnwrap("Next region", TaskWeight.LOW, (ignored) -> readRegionsSequentially(iterator, columnConversionHandler));
        } else {
            return readRegionsSequentially(iterator, columnConversionHandler);
        }
    }

    public Task<Void> readRegion(Map.Entry<RegionCoordPair, Set<ChunkCoordPair>> region, ColumnConversionHandler columnConversionHandler) {
        List<ChunkCoordPair> chunks = new ArrayList<>(region.getValue());
        return readColumnsSequentially(chunks.iterator(), columnConversionHandler);
    }

    private Task<Void> readColumnsSequentially(Iterator<ChunkCoordPair> iterator, ColumnConversionHandler columnConversionHandler) {
        if (!iterator.hasNext()) {
            return Task.async("Finished columns for region", TaskWeight.LOW, () -> {});
        }
        ChunkCoordPair chunkCoordPair = iterator.next();
        if (!converter.shouldProcessColumn(dimension, chunkCoordPair)) {
            return readColumnsSequentially(iterator, columnConversionHandler);
        }

        return Task.asyncUnwrap("Processing column " + chunkCoordPair, TaskWeight.NORMAL, () -> {
            converter.awaitFreeColumnSlot();
            converter.incrementActiveColumns();

            return Task.async("Creating Column Reader", TaskWeight.LOW, () -> createColumnReader(chunkCoordPair))
                    .thenUnwrap("Reading Column", TaskWeight.HIGHER, (columnReader) -> columnReader.readColumn(columnConversionHandler))
                    .thenUnwrap("Next Column", TaskWeight.LOW, (ignored) -> readColumnsSequentially(iterator, columnConversionHandler));
        });
    }

    public BedrockColumnReader createColumnReader(ChunkCoordPair worldChunkCoords) {
        return new BedrockColumnReader(resolvers, converter, database, dimension, worldChunkCoords);
    }
}