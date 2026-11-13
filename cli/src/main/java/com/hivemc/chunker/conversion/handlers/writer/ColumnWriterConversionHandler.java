// [file name]: com.hivemc.chunker.conversion.handlers.writer.ColumnWriterConversionHandler.java
package com.hivemc.chunker.conversion.handlers.writer;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;

/**
 * ColumnConversionHandler which delegates the methods asynchronously to the writer.
 */
public class ColumnWriterConversionHandler implements ColumnConversionHandler {
    protected final ColumnWriter writer;
    protected final Converter converter;

    public ColumnWriterConversionHandler(ColumnWriter writer, Converter converter) {
        this.writer = writer;
        this.converter = converter;
    }

    @Override
    public Task<Void> convertColumn(ChunkerColumn column) {
        return Task.asyncUnwrap("Writing Column Dispatch", TaskWeight.NORMAL, () -> {
            try {
                // Return the task from the writer so the reader can actually await disk completion
                return writer.writeColumn(column);
            } catch (Throwable t) {
                converter.logNonFatalException(t);
                return Task.async("Failed Write Column", TaskWeight.LOW, () -> {});
            }
        });
    }

    @Override
    public Task<Void> flushRegion(RegionCoordPair regionCoordPair) {
        return Task.async("Flushing Region", TaskWeight.LOW, () -> {
            try {
                writer.flushRegion(regionCoordPair);
            } catch (Exception e) {
                converter.logNonFatalException(e);
            }
        });
    }

    @Override
    public Task<Void> flushColumns() {
        return Task.async("Flushing Columns", TaskWeight.LOW, () -> {
            try {
                writer.flushColumns();
            } catch (Exception e) {
                converter.logNonFatalException(e);
            }
        });
    }
}