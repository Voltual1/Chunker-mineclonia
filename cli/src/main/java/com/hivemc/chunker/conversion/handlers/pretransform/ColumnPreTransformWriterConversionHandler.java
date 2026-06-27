// [file name]: com.hivemc.chunker.conversion.handlers.pretransform.ColumnPreTransformWriterConversionHandler.java
package com.hivemc.chunker.conversion.handlers.pretransform;

import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.handlers.pretransform.manager.PreTransformManager;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.scheduling.task.Task;

import java.util.function.Supplier;

/**
 * A Column handler which calls the writer pre-transform method to ensure that the column knows which edges need
 * solving before the actual pre-transform handler is called.
 */
public class ColumnPreTransformWriterConversionHandler implements ColumnConversionHandler {
    private final Supplier<PreTransformManager> preTransformManagerGetter;
    private final ColumnConversionHandler delegate;
    private final boolean preTransformAllowed;

    public ColumnPreTransformWriterConversionHandler(Supplier<PreTransformManager> preTransformManagerGetter, ColumnConversionHandler delegate, boolean preTransformAllowed) {
        this.preTransformManagerGetter = preTransformManagerGetter;
        this.delegate = delegate;
        this.preTransformAllowed = preTransformAllowed;
    }

    @Override
    public Task<Void> convertColumn(ChunkerColumn column) {
        // Call pre-transform solver
        PreTransformManager preTransformManager = preTransformManagerGetter.get();
        if (preTransformManager != null) {
            preTransformManager.solve(column, preTransformAllowed);
        }

        // Call delegate
        return delegate.convertColumn(column);
    }

    @Override
    public Task<Void> flushRegion(RegionCoordPair regionCoordPair) {
        // Call delegate
        return delegate.flushRegion(regionCoordPair);
    }

    @Override
    public Task<Void> flushColumns() {
        // Call delegate
        return delegate.flushColumns();
    }
}