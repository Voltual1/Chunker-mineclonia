package com.hivemc.chunker.conversion.stitch;

import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.encoding.EncodingType;
import com.hivemc.chunker.conversion.encoding.base.Version;
import com.hivemc.chunker.conversion.encoding.base.reader.LevelReader;
import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter;
import com.hivemc.chunker.conversion.encoding.base.writer.WorldWriter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.BedrockReaderWriter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.writer.BedrockWorldWriter;
import com.hivemc.chunker.conversion.encoding.java.base.JavaReaderWriter;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.encoding.java.base.writer.JavaWorldWriter;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.handlers.LevelConversionHandler;
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler;
import com.hivemc.chunker.conversion.handlers.pretransform.ColumnPreTransformConversionHandler;
import com.hivemc.chunker.conversion.handlers.pretransform.ColumnPreTransformWriterConversionHandler;
import com.hivemc.chunker.conversion.handlers.writer.ColumnWriterConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelSettings;
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.pruning.PruningConfig;
import com.hivemc.chunker.scheduling.task.Environment;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import com.hivemc.chunker.scheduling.task.TrackedTask;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.iq80.leveldb.table.BloomFilterPolicy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * WorldStitcher 引擎 - 修复了接口实现与方法引用问题。
 */
public class WorldStitcher {
    private final UUID sessionID;
    private final File sourceDir;
    private final File destDir;
    private final int threadCount;
    private final Consumer<Throwable> exceptionHandler;
    private final BiConsumer<String, Object> signalConsumer;

    private Environment environment = null;
    private StitchConverter converter = null;
    private DB destBedrockDb = null;

    public WorldStitcher(UUID sessionID, File sourceDir, File destDir, int threadCount, Consumer<Throwable> exceptionHandler, BiConsumer<String, Object> signalConsumer) {
        this.sessionID = sessionID;
        this.sourceDir = sourceDir;
        this.destDir = destDir;
        this.threadCount = threadCount;
        this.exceptionHandler = exceptionHandler;
        this.signalConsumer = signalConsumer;
    }

    public void cancel() {
        if (converter != null) {
            converter.cancel();
        }
        if (environment != null) {
            environment.cancel(null);
        }
    }

    public TrackedTask<Void> stitch(Dimension dimension, PruningConfig pruningConfig) throws Exception {
        converter = new StitchConverter(sessionID, dimension, pruningConfig);
        converter.setCompactionSignal(signal -> {
            if (signalConsumer != null) {
                signalConsumer.accept(WorldConverter.SIGNAL_COMPACTION, signal);
            }
        });

        Optional<? extends LevelReader> sourceReaderOpt = EncodingType.findReader(sourceDir, converter);
        if (sourceReaderOpt.isEmpty()) {
            throw new Exception("Unable to detect source world format.");
        }
        LevelReader sourceReader = sourceReaderOpt.get();

        Optional<? extends LevelReader> destReaderDetectorOpt = EncodingType.findReader(destDir, converter);
        if (destReaderDetectorOpt.isEmpty()) {
            throw new Exception("Unable to detect destination world format.");
        }
        LevelReader destReaderDetector = destReaderDetectorOpt.get();

        if (sourceReader.getEncodingType() != destReaderDetector.getEncodingType()) {
            throw new Exception("Source and Destination worlds must be of the same platform.");
        }

        boolean isBedrock = sourceReader.getEncodingType() == EncodingType.BEDROCK;
        Version destVersion = destReaderDetector.getVersion();
        try { destReaderDetector.free(); } catch (Exception ignored) {}

        environment = Task.environment("Archive Stitching", threadCount, exceptionHandler, signalConsumer);

        try {
            WorldWriter targetWorldWriter;

            if (isBedrock) {
                File databaseDirectory = new File(destDir, "db");
                new File(databaseDirectory, "LOCK").delete();
                Options options = new Options();
                options.compressionType(CompressionType.ZLIB_RAW);
                options.blockSize(160 * 1024);
                options.filterPolicy(new BloomFilterPolicy(10));
                options.createIfMissing(true);
                destBedrockDb = new Iq80DBFactory().open(databaseDirectory, options);

                BedrockResolvers destResolvers = new BedrockReaderWriter() {
                    @Override public Version getVersion() { return destVersion; }
                    @Override public boolean isReader() { return false; }
                    @Override public Set<ChunkerBiome.ChunkerVanillaBiome> getSupportedBiomes() { return Collections.emptySet(); }
                    @Override public int getOrCreateLodestoneData(com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerLodestoneData data) { return -1; }
                    @Override public com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerLodestoneData getLodestoneData(int index) { return null; }
                }.buildResolvers(converter).build();

                targetWorldWriter = new BedrockWorldWriter(destDir, converter, destResolvers, destBedrockDb);
            } else {
                JavaResolvers destResolvers = new JavaReaderWriter() {
                    @Override public Version getVersion() { return destVersion; }
                    @Override public boolean isReader() { return false; }
                    @Override public File getLevelDirectory() { return destDir; }
                    @Override public Set<ChunkerBiome.ChunkerVanillaBiome> getSupportedBiomes() { return Collections.emptySet(); }
                }.buildResolvers(converter).build();

                targetWorldWriter = new JavaWorldWriter(destDir, converter, destResolvers);
            }

            LevelConversionHandler bridgeHandler = new LevelConversionHandler() {
                @Override
                public Task<WorldConversionHandler> convertLevel(ChunkerLevel level) {
                    return Task.async("Setup World Handler", TaskWeight.NONE, () -> new WorldConversionHandler() {
                        @Override
                        public Task<ColumnConversionHandler> convertWorld(ChunkerWorld world) {
                            if (world.getDimension() != dimension) return Task.async("Skip", TaskWeight.NONE, () -> null);

                            return Task.asyncUnwrap("Initializing Writer", TaskWeight.LOW, () -> {
                                try {
                                    ColumnWriter columnWriter = targetWorldWriter.writeWorld(world);

                                    // 修复：通过获取到的 columnWriter 实例来引用 getPreTransformManager
                                    ColumnConversionHandler baseHandler = new ColumnWriterConversionHandler(columnWriter, converter);
                                    ColumnConversionHandler preTransformWriter = new ColumnPreTransformWriterConversionHandler(
                                            columnWriter::getPreTransformManager, 
                                            baseHandler, 
                                            true
                                    );
                                    ColumnConversionHandler finalHandler = new ColumnPreTransformConversionHandler(preTransformWriter, world);
                                    
                                    return Task.async("Handler Ready", TaskWeight.NONE, () -> finalHandler);
                                } catch (Exception e) {
                                    converter.logNonFatalException(e);
                                    return Task.async("Error", TaskWeight.NONE, () -> null);
                                }
                            });
                        }

                        @Override public void flushWorld(ChunkerWorld world) { 
                            try { targetWorldWriter.flushWorld(world); } catch (Exception e) { converter.logNonFatalException(e); } 
                        }
                        @Override public void flushWorlds() { 
                            try { targetWorldWriter.flushWorlds(); } catch (Exception e) { converter.logNonFatalException(e); } 
                        }
                    });
                }

                @Override
                public void flushLevel() {
                    if (isBedrock && converter.shouldLevelDBCompaction() && destBedrockDb != null) {
                        Task.signal(WorldConverter.SIGNAL_COMPACTION, true);
                        destBedrockDb.compactRange(null, null);
                        Task.signal(WorldConverter.SIGNAL_COMPACTION, false);
                    }
                }
            };

            Task.asyncConsume("Reading Source Level for Stitching", TaskWeight.NORMAL, sourceReader::readLevel, bridgeHandler);
            return environment;
        } catch (Exception e) {
            environment.close();
            closeDatabaseQuietly();
            try { sourceReader.free(); } catch (Exception ex) {}
            throw e;
        } finally {
            environment.close();
            environment.setFreeCallback(() -> {
                closeDatabaseQuietly();
                try { sourceReader.free(); } catch (Exception ex) {}
            });
        }
    }

    private void closeDatabaseQuietly() {
        if (destBedrockDb != null) {
            try { destBedrockDb.close(); } catch (Exception ignored) {} finally { destBedrockDb = null; }
        }
    }
}