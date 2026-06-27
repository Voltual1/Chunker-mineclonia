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
import com.hivemc.chunker.scheduling.task.FutureTask;
import com.hivemc.chunker.scheduling.task.ProgressiveTask;
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

    /**
     * Create a new world reader.
     *
     * @param converter       the converter instance.
     * @param resolvers       the resolvers to use.
     * @param dimensionFolder the dimension folder to read data from.
     * @param dimension       the dimension being read.
     */
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

        // Get each region file and loop through
        File[] folders = getMCAFolders();
        Set<String> knownRegionFiles = new ObjectOpenHashSet<>();

        for (File folder : folders) {
            File[] regionFiles = folder.listFiles((parent, fileName) -> fileName.endsWith(".mca"));
            if (regionFiles != null) {
                for (File regionFile : regionFiles) {
                    String[] parts = regionFile.getName().split("\\.");
                    if (parts.length != 4 || regionFile.length() < 4096) continue;

                    // Add to the known files
                    knownRegionFiles.add(regionFile.getAbsolutePath());

                    // Parse and add the region
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);

                        // Add to the list of regions
                        regions.add(new RegionCoordPair(x, z));
                    } catch (NumberFormatException e) {
                        // Ignore the region file
                    }
                }
            }
        }

        // Copy the regions for reading of the world
        HashSet<RegionCoordPair> regionsCopy = new HashSet<>(regions);

        // Submit world info (done before column reading)
        Task<ColumnConversionHandler> convertWorld = worldConversionHandler.convertWorld(chunkerWorld);

        // Handle the reading of the regions
        Task<Void> regionProcessing = convertWorld.thenConsume("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return;

            try {
                // Synchronously loop over regions (blocks the single dimension reader task safely)
                readRegionsSync(regionsCopy, knownRegionFiles, columnConversionHandler);
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
     * Synchronously read regions one by one to ensure memory containment.
     */
    protected void readRegionsSync(Set<RegionCoordPair> regions, Set<String> knownRegionFiles, ColumnConversionHandler columnConversionHandler) {
        for (RegionCoordPair region : regions) {
            if (converter.isCancelled()) break;
            if (converter.shouldProcessRegion(dimension, region)) {
                readRegionSync(region, knownRegionFiles, columnConversionHandler);
                columnConversionHandler.flushRegion(region); // Blocks until all active writes of this region are finished
                System.gc(); // Explicit hint to JVM for memory reclaim
            }
        }
    }

    /**
     * Synchronously read a region's columns.
     */
    protected void readRegionSync(RegionCoordPair region, Set<String> knownRegionFiles, ColumnConversionHandler columnConversionHandler) {
        File[] regionFiles = getRegionFiles(region, knownRegionFiles);
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
            return;
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

            for (Int2ObjectMap.Entry<int[]> entry : positionsToOffsets.int2ObjectEntrySet()) {
                if (converter.isCancelled()) break;

                ChunkCoordPair localCoords = new ChunkCoordPair(
                        entry.getIntKey() & 31,
                        entry.getIntKey() >> 5
                );
                ChunkCoordPair columnsCoords = region.getChunk(localCoords.chunkX(), localCoords.chunkZ());
                int[] columnFileOffsets = entry.getValue();

                if (!converter.shouldProcessColumn(dimension, columnsCoords)) continue;

                // Throttle: Limit TaskExecutor Queue Size to prevent memory bloat
                converter.awaitFreeColumnSlot();

                // Read column compressed bytes synchronously, but push NBT decompression to TaskExecutor
                List<Task<CompoundTag>> compoundTagTasks = new ArrayList<>(regionFilesCount);
                for (int regionFileIndex = 0; regionFileIndex < regionFilesCount; regionFileIndex++) {
                    MCAReader mcaReader = mcaReaders[regionFileIndex];
                    if (mcaReader == null) {
                        compoundTagTasks.add(new FutureTask<>(java.util.concurrent.CompletableFuture.completedFuture(null)));
                        continue;
                    }

                    int offset = columnFileOffsets[regionFileIndex];
                    if (offset <= 0) {
                        compoundTagTasks.add(new FutureTask<>(java.util.concurrent.CompletableFuture.completedFuture(null)));
                        continue;
                    }

                    // `readColumn` synchronously reads bytes from disk but returns an async task for CPU decompression
                    compoundTagTasks.add(mcaReader.readColumn(columnsCoords, offset));
                }

                // Wait for decompression asynchronously, then submit to the pipeline
                Task.join(compoundTagTasks).thenConsume("Combine and Read Column", TaskWeight.HIGHER, (tags) -> {
                    try {
                        CompoundTag[] compoundTags = tags.toArray(new CompoundTag[0]);
                        CompoundTag combinedNBT = combineColumnCompounds(compoundTags);
                        JavaColumnReader columnReader = createColumnReader(columnsCoords, combinedNBT);
                        
                        // Execute internal processing of the column which will automatically push smaller sub-tasks
                        columnReader.readColumn(columnConversionHandler);
                    } catch (Exception e) {
                        converter.logNonFatalException(e);
                    }
                });
            }
        } catch (Exception e) {
            converter.logNonFatalException(e);
        } finally {
            closeReaders(mcaReaders);
        }
    }

    /**
     * Combine compoundTags fetched from multiple files.
     *
     * @param compoundTags the compound tags in the same order as the folders array with null when a tag is missing.
     * @return the combined compound tag.
     */
    protected CompoundTag combineColumnCompounds(CompoundTag[] compoundTags) {
        // By default, no combining is supported in older versions
        if (compoundTags.length > 1)
            throw new IllegalArgumentException("Combining compounds is unsupported at this version");
        return compoundTags[0];
    }

    /**
     * Close all the MCAReaders in an array.
     *
     * @param mcaReaders the readers.
     */
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

    /**
     * Get all the folders which contain MCA files for this dimension.
     *
     * @return an array of all the folders (to be combined).
     */
    protected File[] getMCAFolders() {
        // Base version only uses region folder
        return new File[]{resolvers.javaLevelDirectoryResolver().getDimensionRegionDirectory(dimensionFolder)};
    }

    /**
     * Get a list of all the valid MCA files for a region.
     *
     * @param region     the region co-ordinates.
     * @param knownFiles a hashset of absolute paths to known files within the MCAFolders.
     * @return an array of all the matching .mca files, null entries are used when the file isn't present.
     */
    protected @Nullable File[] getRegionFiles(RegionCoordPair region, Set<String> knownFiles) {
        File[] folders = getMCAFolders();
        File[] files = new File[folders.length];

        // Create a path for each MCA folder
        File[] mcaFolders = getMCAFolders();
        for (int i = 0; i < mcaFolders.length; i++) {
            File folder = mcaFolders[i];
            File temp = new File(folder, "r." + region.regionX() + "." + region.regionZ() + ".mca");

            // Only add the file if it's known to exist
            if (knownFiles.contains(temp.getAbsolutePath())) {
                files[i] = temp;
            }
        }

        // Return the files
        return files;
    }

    /**
     * Create a new column reader.
     *
     * @param worldChunkCoords the co-ordinates of the column.
     * @param columnNBT        the NBT compound which was read for the column.
     * @return a newly created column reader.
     */
    public JavaColumnReader createColumnReader(ChunkCoordPair worldChunkCoords, CompoundTag columnNBT) {
        return new JavaColumnReader(converter, resolvers, dimension, worldChunkCoords, columnNBT);
    }
}