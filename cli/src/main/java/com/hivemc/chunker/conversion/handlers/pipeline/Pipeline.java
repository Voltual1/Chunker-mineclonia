// [file name]: com.hivemc.chunker.conversion.handlers.pipeline.Pipeline.java
package com.hivemc.chunker.conversion.handlers.pipeline;

import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.handlers.LevelConversionHandler;
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A pipeline with handlers to allow different handlers to be used when level, world and column data is processed.
 */
public class Pipeline {
    private final LevelConversionHandler base;
    private Function<LevelConversionHandler, LevelConversionHandler> levelConversionTransformer;
    private BiFunction<WorldConversionHandler, ChunkerLevel, WorldConversionHandler> worldConversionTransformer;
    private BiFunction<ColumnConversionHandler, ChunkerWorld, ColumnConversionHandler> columnConversionTransformer;

    public Pipeline(LevelConversionHandler base) {
        this.base = base;
    }

    @SafeVarargs
    @Nullable
    protected static <T> Function<T, T> compose(Function<T, T>... handlers) {
        if (handlers.length == 0) return null;
        Function<T, T> function = handlers[0];
        for (int i = 1; i < handlers.length; i++) {
            function = function.andThen(handlers[i]);
        }
        return function;
    }

    @SafeVarargs
    @Nullable
    protected static <T, U> BiFunction<T, U, T> compose(BiFunction<T, U, T>... handlers) {
        if (handlers.length == 0) return null;
        BiFunction<T, U, T> function = handlers[0];
        for (int i = 1; i < handlers.length; i++) {
            final BiFunction<T, U, T> previous = function;
            final BiFunction<T, U, T> current = handlers[i];

            function = (t, u) -> current.apply(previous.apply(t, u), u);
        }
        return function;
    }

    @SafeVarargs
    public final void levelHandlers(Function<LevelConversionHandler, LevelConversionHandler>... handlers) {
        levelConversionTransformer = compose(handlers);
    }

    @SafeVarargs
    public final void worldHandlers(BiFunction<WorldConversionHandler, ChunkerLevel, WorldConversionHandler>... handlers) {
        worldConversionTransformer = compose(handlers);
    }

    @SafeVarargs
    public final void columnHandlers(BiFunction<ColumnConversionHandler, ChunkerWorld, ColumnConversionHandler>... handlers) {
        columnConversionTransformer = compose(handlers);
    }

    @Nullable
    public LevelConversionHandler build() {
        if (base == null) return null;
        LevelConversionHandler delegate = levelConversionTransformer != null ? levelConversionTransformer.apply(base) : base;
        return new PipelineLevelConversionHandler(delegate);
    }

    static class PipelineColumnConversionHandler implements ColumnConversionHandler {
        protected final ColumnConversionHandler delegate;

        public PipelineColumnConversionHandler(ColumnConversionHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public Task<Void> convertColumn(ChunkerColumn column) {
            return Task.asyncUnwrap("Running column middleware", TaskWeight.NORMAL, delegate::convertColumn, column);
        }

        @Override
        public Task<Void> flushRegion(RegionCoordPair regionCoordPair) {
            return Task.asyncUnwrap("Running column middleware", TaskWeight.NORMAL, delegate::flushRegion, regionCoordPair);
        }

        @Override
        public Task<Void> flushColumns() {
            return Task.asyncUnwrap("Running column middleware", TaskWeight.NORMAL, delegate::flushColumns);
        }
    }

    class PipelineLevelConversionHandler implements LevelConversionHandler {
        protected final LevelConversionHandler delegate;

        public PipelineLevelConversionHandler(LevelConversionHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public Task<WorldConversionHandler> convertLevel(ChunkerLevel level) {
            return Task.asyncUnwrap("Running level middleware", TaskWeight.NORMAL, () -> {
                return delegate.convertLevel(level).then("Applying level pipeline", TaskWeight.NONE, (base) -> {
                    if (base == null) return null;
                    WorldConversionHandler delegate = worldConversionTransformer != null ? worldConversionTransformer.apply(base, level) : base;
                    return new PipelineWorldConversionHandler(delegate);
                });
            });
        }

        @Override
        public void flushLevel() {
            Task.async("Running level middleware", TaskWeight.NORMAL, delegate::flushLevel);
        }
    }

    class PipelineWorldConversionHandler implements WorldConversionHandler {
        protected final WorldConversionHandler delegate;

        public PipelineWorldConversionHandler(WorldConversionHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public Task<ColumnConversionHandler> convertWorld(ChunkerWorld world) {
            return Task.asyncUnwrap("Running world middleware", TaskWeight.NORMAL, () -> {
                return delegate.convertWorld(world).then("Applying world pipeline", TaskWeight.NONE, (base) -> {
                    if (base == null) return null;
                    ColumnConversionHandler delegate = columnConversionTransformer != null ? columnConversionTransformer.apply(base, world) : base;
                    return new PipelineColumnConversionHandler(delegate);
                });
            });
        }

        @Override
        public void flushWorld(ChunkerWorld chunkerWorld) {
            Task.asyncConsume("Running world middleware", TaskWeight.NORMAL, delegate::flushWorld, chunkerWorld);
        }

        @Override
        public void flushWorlds() {
            Task.async("Running world middleware", TaskWeight.NORMAL, delegate::flushWorlds);
        }
    }
}