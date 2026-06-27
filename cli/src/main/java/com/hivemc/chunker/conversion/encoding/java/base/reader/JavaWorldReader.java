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

    // The maximum number of columns to process concurrently before forcing a pipeline flush
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

        HashSet<RegionCoordPair> regionsCopy = new HashSet<>(regions);
        Task<ColumnConversionHandler> convertWorld = worldConversionHandler.convertWorld(chunkerWorld);

        Task<Void> regionProcessing = convertWorld.thenConsume("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return;

            try {
                readRegionsSync(regionsCopy, knownRegionFiles, columnConversionHandler);
            } catch (Throwable t) {
                converter.logNonFatalException(t);
            }

            columnConversionHandler.flushColumns();
        });

        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    protected void readRegionsSync(Set<RegionCoordPair> regions, Set<String> knownRegionFiles, ColumnConversionHandler columnConversionHandler) {
        for (RegionCoordPair region : regions) {
            if (converter.isCancelled()) break;
            if (converter.shouldProcessRegion(dimension, region)) {
                readRegionSync(region, knownRegionFiles, columnConversionHandler);
                
                // Block until ALL columns in the region have been written out to prevent runaway memory
                columnConversionHandler.flushRegion(region); 
                
                // Force JVM GC to reclaim NBT objects immediately after a region is done
                System.gc(); 
            }
        }
    }

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

            List<Task<Void>> activeBatches = new ArrayList<>(MAX_CONCURRENT_COLUMNS);

            for (Int2ObjectMap.Entry<int[]> entry : positionsToOffsets.int2ObjectEntrySet()) {
                if (converter.isCancelled()) break;

                ChunkCoordPair localCoords = new ChunkCoordPair(
                        entry.getIntKey() & 31,
                        entry.getIntKey() >> 5
                );
                ChunkCoordPair columnsCoords = region.getChunk(localCoords.chunkX(), localCoords.chunkZ());
                int[] columnFileOffsets = entry.getValue();

                if (!converter.shouldProcessColumn(dimension, columnsCoords)) continue;

                // Micro-batching: Block completely if we hit our column limit
                if (activeBatches.size() >= MAX_CONCURRENT_COLUMNS) {
                    try {
                        Task.join(activeBatches).future().get(); // Wait for all columns in this batch to finish completely
                    } catch (Exception e) {
                        converter.logNonFatalException(e);
                    }
                    activeBatches.clear();
                }

                // Read bytes synchronously to avoid OOM from queued readers
                CompoundTag[] compoundTags = new CompoundTag[regionFilesCount];
                for (int regionFileIndex = 0; regionFileIndex < regionFilesCount; regionFileIndex++) {
                    MCAReader mcaReader = mcaReaders[regionFileIndex];
                    if (mcaReader == null) continue;

                    int offset = columnFileOffsets[regionFileIndex];
                    if (offset <= 0) continue;

                    // SYNC READ AND DECOMPRESS: Do not push this to queue. Memory is allocated strictly per-batch.
                    compoundTags[regionFileIndex] = mcaReader.readColumnSync(columnsCoords, offset);
                }

                CompoundTag combinedNBT = combineColumnCompounds(compoundTags);
                JavaColumnReader columnReader = createColumnReader(columnsCoords, combinedNBT);
                
                // Process in Task Executor and track it
                Task<Void> columnTask = Task.asyncConsume("Reading Column", TaskWeight.HIGHER, columnReader::readColumn, columnConversionHandler);
                activeBatches.add(columnTask);
            }

            // Await remainder of the batch
            if (!activeBatches.isEmpty()) {
                try {
                    Task.join(activeBatches).future().get();
                } catch (Exception e) {
                    converter.logNonFatalException(e);
                }
                activeBatches.clear();
            }

        } catch (Exception e) {
            converter.logNonFatalException(e);
        } finally {
            closeReaders(mcaReaders);
        }
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