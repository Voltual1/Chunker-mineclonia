// [file name]: com.hivemc.chunker.conversion.encoding.java.base.reader.JavaWorldReader.java
package com.hivemc.chunker.conversion.encoding.java.base.reader;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.reader.WorldReader;
import com.hivemc.chunker.conversion.encoding.java.base.reader.util.MCAReader;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A reader for Java dimensions.
 */
public class JavaWorldReader implements WorldReader {
    protected final Converter converter;
    protected final JavaResolvers resolvers;
    protected final File dimensionFolder;
    protected final Dimension dimension;

    // The maximum number of columns to process concurrently before chaining the next batch
    private static final int MAX_CONCURRENT_COLUMNS = 32;

    public JavaWorldReader(Converter converter, JavaResolvers resolvers, File dimensionFolder, Dimension dimension) {
        this.converter = converter;
        this.resolvers = resolvers;
        this.dimensionFolder = dimensionFolder;
        this.dimension = dimension;
    }

    @Override
    public void readWorld(WorldConversionHandler worldConversionHandler) {
        Set<RegionCoordPair> regions = new ObjectOpenHashSet<>();
        ChunkerWorld chunkerWorld = new ChunkerWorld(
                dimension,
                regions
        );

        File[] folders = getMCAFolders();
        Set<String> knownRegionFiles = new ObjectOpenHashSet<>();

        for (File folder : folders) {
            File[] regionFiles = folder.listFiles((parent, fileName) -> fileName.endsWith(".mca"));
            if (regionFiles != null) {
                for (File regionFile : regionFiles) {
                    String[] parts = regionFile.getName().split("\\.");
                    if (parts.length != 4 || regionFile.length() < 4096) continue;

                    knownRegionFiles.add(regionFile.getAbsolutePath());

                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        regions.add(new RegionCoordPair(x, z));
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        }

        Task<ColumnConversionHandler> convertWorld = worldConversionHandler.convertWorld(chunkerWorld);

        Task<Void> regionProcessing = convertWorld.thenUnwrap("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return Task.async("Skip", TaskWeight.LOW, () -> {});
            
            // Kick off the fully asynchronous recursive promise chain
            return processRegionsAsync(new ArrayList<>(regions), 0, knownRegionFiles, columnConversionHandler);
        });

        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    /**
     * Asynchronously process regions one by one using a promise chain.
     */
    protected Task<Void> processRegionsAsync(List<RegionCoordPair> regions, int index, Set<String> knownFiles, ColumnConversionHandler handler) {
        if (index >= regions.size() || converter.isCancelled()) {
            handler.flushColumns();
            return Task.async("Regions Done", TaskWeight.LOW, () -> {});
        }

        RegionCoordPair region = regions.get(index);
        if (!converter.shouldProcessRegion(dimension, region)) {
            return processRegionsAsync(regions, index + 1, knownFiles, handler);
        }

        return readRegionAsync(region, knownFiles, handler).thenUnwrap("Next Region", TaskWeight.HIGHER, (ignore) -> {
            handler.flushRegion(region); // Safe to flush, the previous region tasks are completely finished
            System.gc(); // Explicit hint to JVM for memory reclaim
            return processRegionsAsync(regions, index + 1, knownFiles, handler);
        });
    }

    /**
     * Asynchronously read a region's columns via batching.
     */
    protected Task<Void> readRegionAsync(RegionCoordPair region, Set<String> knownFiles, ColumnConversionHandler handler) {
        File[] regionFiles = getRegionFiles(region, knownFiles);
        int regionFilesCount = regionFiles.length;
        MCAReader[] mcaReaders = new MCAReader[regionFilesCount];

        boolean foundValidFile = false;
        for (int i = 0; i < regionFilesCount; i++) {
            File file = regionFiles[i];
            if (file == null) continue;
            try {
                mcaReaders[i] = new MCAReader(converter, file);
                foundValidFile = true;
            } catch (FileNotFoundException e) {
                // Ignore
            }
        }

        if (!foundValidFile) {
            return Task.async("Skip Region", TaskWeight.LOW, () -> {});
        }

        try {
            Int2ObjectMap<int[]> positionsToOffsets = new Int2ObjectOpenHashMap<>();
            for (int regionFileIndex = 0; regionFileIndex < regionFilesCount; regionFileIndex++) {
                MCAReader mcaReader = mcaReaders[regionFileIndex];
                if (mcaReader == null) continue;

                int[] offsets = mcaReader.readOffsetTable();
                for (int i = 0; i < offsets.length; i++) {
                    int offset = offsets[i];
                    if (offset > 0) {
                        int[] mcaOffsets = positionsToOffsets.computeIfAbsent(i, (ignored) -> new int[regionFilesCount]);
                        mcaOffsets[regionFileIndex] = offset;
                    }
                }
            }

            List<Int2ObjectMap.Entry<int[]>> entries = new ArrayList<>(positionsToOffsets.int2ObjectEntrySet());
            
            // Start processing batches asynchronously
            return processBatchAsync(entries, 0, region, mcaReaders, handler)
                    .thenConsume("Close Readers", TaskWeight.LOW, (ignore) -> closeReaders(mcaReaders));

        } catch (Exception e) {
            converter.logNonFatalException(e);
            closeReaders(mcaReaders);
            return Task.async("Region Error", TaskWeight.LOW, () -> {});
        }
    }

    /**
     * Micro-batching using Promise chains. Processes 32 columns, then yields and chains the next 32.
     */
    protected Task<Void> processBatchAsync(List<Int2ObjectMap.Entry<int[]>> entries, int startIndex, RegionCoordPair region, MCAReader[] mcaReaders, ColumnConversionHandler handler) {
        if (startIndex >= entries.size() || converter.isCancelled()) {
            return Task.async("Batch Done", TaskWeight.LOW, () -> {});
        }

        int endIndex = Math.min(startIndex + MAX_CONCURRENT_COLUMNS, entries.size());
        List<Task<Void>> activeBatches = new ArrayList<>(endIndex - startIndex);

        for (int i = startIndex; i < endIndex; i++) {
            Int2ObjectMap.Entry<int[]> entry = entries.get(i);
            ChunkCoordPair localCoords = new ChunkCoordPair(
                    entry.getIntKey() & 31,
                    entry.getIntKey() >> 5
            );
            ChunkCoordPair columnsCoords = region.getChunk(localCoords.chunkX(), localCoords.chunkZ());
            int[] columnFileOffsets = entry.getValue();

            if (!converter.shouldProcessColumn(dimension, columnsCoords)) continue;

            try {
                // Sync read bytes
                CompoundTag[] compoundTags = new CompoundTag[mcaReaders.length];
                for (int regionFileIndex = 0; regionFileIndex < mcaReaders.length; regionFileIndex++) {
                    MCAReader mcaReader = mcaReaders[regionFileIndex];
                    if (mcaReader == null) continue;

                    int offset = columnFileOffsets[regionFileIndex];
                    if (offset <= 0) continue;

                    compoundTags[regionFileIndex] = mcaReader.readColumnSync(columnsCoords, offset);
                }

                CompoundTag combinedNBT = combineColumnCompounds(compoundTags);
                JavaColumnReader columnReader = createColumnReader(columnsCoords, combinedNBT);

                // Push inner conversion task
                Task<Void> columnTask = Task.asyncConsume("Reading Column", TaskWeight.HIGHER, columnReader::readColumn, handler);
                activeBatches.add(columnTask);
            } catch (Exception e) {
                converter.logNonFatalException(e);
            }
        }

        if (activeBatches.isEmpty()) {
            return processBatchAsync(entries, endIndex, region, mcaReaders, handler);
        }

        // Wait for all 32 to finish NON-BLOCKING, then chain the next batch
        return Task.join(activeBatches).thenUnwrap("Next Batch", TaskWeight.HIGHER, (ignore) -> {
            return processBatchAsync(entries, endIndex, region, mcaReaders, handler);
        });
    }

    protected CompoundTag combineColumnCompounds(CompoundTag[] compoundTags) {
        if (compoundTags.length > 1)
            throw new IllegalArgumentException("Combining compounds is unsupported at this version");
        return compoundTags[0];
    }

    protected void closeReaders(MCAReader[] mcaReaders) {
        for (MCAReader mcaReader : mcaReaders) {
            if (mcaReader == null) continue;
            try {
                mcaReader.close();
            } catch (IOException e) {
                converter.logNonFatalException(e);
            }
        }
    }

    protected File[] getMCAFolders() {
        return new File[]{resolvers.javaLevelDirectoryResolver().getDimensionRegionDirectory(dimensionFolder)};
    }

    protected @Nullable File[] getRegionFiles(RegionCoordPair region, Set<String> knownFiles) {
        File[] folders = getMCAFolders();
        File[] files = new File[folders.length];

        File[] mcaFolders = getMCAFolders();
        for (int i = 0; i < mcaFolders.length; i++) {
            File folder = mcaFolders[i];
            File temp = new File(folder, "r." + region.regionX() + "." + region.regionZ() + ".mca");

            if (knownFiles.contains(temp.getAbsolutePath())) {
                files[i] = temp;
            }
        }

        return files;
    }

    public JavaColumnReader createColumnReader(ChunkCoordPair worldChunkCoords, CompoundTag columnNBT) {
        return new JavaColumnReader(converter, resolvers, dimension, worldChunkCoords, columnNBT);
    }
}