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
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

        Task<Void> regionProcessing = convertWorld.thenUnwrap("Reading regions", TaskWeight.HIGHER, (columnConversionHandler) -> {
            if (columnConversionHandler == null) return null;

            Task<Void> readingRegionFiles = readRegionsSequentially(regionsCopy, knownRegionFiles, columnConversionHandler);
            return readingRegionFiles.then("Flushing columns", TaskWeight.MEDIUM, columnConversionHandler::flushColumns);
        });

        regionProcessing.then("Flushing world", TaskWeight.MEDIUM, () -> worldConversionHandler.flushWorld(chunkerWorld));
    }

    public Task<Void> readRegionsSequentially(Set<RegionCoordPair> regions, Set<String> knownRegionFiles, ColumnConversionHandler columnConversionHandler) {
        return readRegionsSequentially(regions.iterator(), knownRegionFiles, columnConversionHandler);
    }

    private Task<Void> readRegionsSequentially(Iterator<RegionCoordPair> iterator, Set<String> knownRegionFiles, ColumnConversionHandler columnConversionHandler) {
        if (!iterator.hasNext()) {
            return Task.async("Finished regions", TaskWeight.LOW, () -> {});
        }
        RegionCoordPair region = iterator.next();
        if (converter.shouldProcessRegion(dimension, region)) {
            File[] regionFiles = getRegionFiles(region, knownRegionFiles);
            return Task.asyncUnwrap("Reading region " + region, TaskWeight.NORMAL, () -> readRegion(regionFiles, region, columnConversionHandler))
                    .thenConsume("Closing region readers", TaskWeight.NONE, this::closeReaders)
                    .then("Region - Flushing " + region, TaskWeight.MEDIUM, () -> columnConversionHandler.flushRegion(region))
                    .then("GC", TaskWeight.LOW, System::gc)
                    .thenUnwrap("Next region", TaskWeight.LOW, (ignored) -> readRegionsSequentially(iterator, knownRegionFiles, columnConversionHandler));
        } else {
            return readRegionsSequentially(iterator, knownRegionFiles, columnConversionHandler);
        }
    }

    protected Task<Void> readRegion(@Nullable File[] regionFiles, RegionCoordPair region, ColumnConversionHandler columnConversionHandler) {
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
            converter.logNonFatalException(new Exception("Misnamed region file for " + dimension + ", " + region));
            return Task.async("Failed region", TaskWeight.LOW, () -> {});
        }

        Int2ObjectMap<int[]> positionsToOffsets = new Int2ObjectOpenHashMap<>();
        for (int regionFileIndex = 0; regionFileIndex < regionFilesCount; regionFileIndex++) {
            MCAReader mcaReader = mcaReaders[regionFileIndex];
            if (mcaReader == null) continue;

            try {
                int[] offsets = mcaReader.readOffsetTable();
                for (int i = 0; i < offsets.length; i++) {
                    int offset = offsets[i];
                    if (offset > 0) {
                        int[] mcaOffsets = positionsToOffsets.computeIfAbsent(i, (ignored) -> new int[regionFilesCount]);
                        mcaOffsets[regionFileIndex] = offset;
                    }
                }
            } catch (Exception e) {
                converter.logNonFatalException(e);
            }
        }

        List<Map.Entry<Integer, int[]>> entries = new ArrayList<>(positionsToOffsets.int2ObjectEntrySet());
        return readColumnsSequentially(entries.iterator(), region, mcaReaders, columnConversionHandler);
    }

    private Task<Void> readColumnsSequentially(Iterator<Map.Entry<Integer, int[]>> iterator, RegionCoordPair region, MCAReader[] mcaReaders, ColumnConversionHandler columnConversionHandler) {
        if (!iterator.hasNext()) {
            return Task.async("Finished columns for region", TaskWeight.LOW, () -> {});
        }
        Map.Entry<Integer, int[]> entry = iterator.next();
        ChunkCoordPair localCoords = new ChunkCoordPair(
                entry.getKey() & 31,
                entry.getKey() >> 5
        );
        ChunkCoordPair columnsCoords = region.getChunk(localCoords.chunkX(), localCoords.chunkZ());
        int[] columnFileOffsets = entry.getValue();

        if (!converter.shouldProcessColumn(dimension, columnsCoords)) {
            return readColumnsSequentially(iterator, region, mcaReaders, columnConversionHandler);
        }

        return Task.asyncUnwrap("Processing column " + columnsCoords, TaskWeight.NORMAL, () -> {
            converter.awaitFreeColumnSlot();
            converter.incrementActiveColumns();

            List<Task<CompoundTag>> decompressingTasks = new ArrayList<>(mcaReaders.length);
            List<Integer> decompressingTasksIndexes = new ArrayList<>(mcaReaders.length);

            for (int regionFileIndex = 0; regionFileIndex < mcaReaders.length; regionFileIndex++) {
                MCAReader mcaReader = mcaReaders[regionFileIndex];
                if (mcaReader == null) continue;

                int offset = columnFileOffsets[regionFileIndex];
                if (offset <= 0) continue;

                synchronized (mcaReader) {
                    decompressingTasks.add(mcaReader.readColumn(columnsCoords, offset));
                }
                decompressingTasksIndexes.add(regionFileIndex);
            }

            return Task.join(decompressingTasks)
                    .then("Combining NBT " + columnsCoords, TaskWeight.LOW, (results) -> {
                        CompoundTag[] compoundTags = new CompoundTag[mcaReaders.length];
                        for (int i = 0; i < decompressingTasksIndexes.size(); i++) {
                            compoundTags[decompressingTasksIndexes.get(i)] = results.get(i);
                        }
                        return combineColumnCompounds(compoundTags);
                    })
                    .then("Creating Reader " + columnsCoords, TaskWeight.LOW, (columnNbt) -> createColumnReader(columnsCoords, columnNbt))
                    .thenUnwrap("Reading Column " + columnsCoords, TaskWeight.HIGHER, (columnReader) -> columnReader.readColumn(columnConversionHandler))
                    .thenUnwrap("Next Column", TaskWeight.LOW, (ignored) -> readColumnsSequentially(iterator, region, mcaReaders, columnConversionHandler));
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