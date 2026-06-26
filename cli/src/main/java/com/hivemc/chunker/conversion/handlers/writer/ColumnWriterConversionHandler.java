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

    /**
     * Create a new column writer conversion handler.
     *
     * @param writer    the writer to delegate methods to.
     * @param converter the converter instance.
     */
    public ColumnWriterConversionHandler(ColumnWriter writer, Converter converter) {
        this.writer = writer;
        this.converter = converter;
    }

    @Override
    public void convertColumn(ChunkerColumn column) {
        Task.asyncConsume("Writing Column", TaskWeight.NORMAL, (col) -> {
            try {
                writer.writeColumn(col);
            } finally {
                converter.decrementActiveColumns();
            }
        }, column);
    }

    @Override
    public void flushRegion(RegionCoordPair regionCoordPair) {
        Task.asyncConsume("Flushing Region", TaskWeight.NORMAL, writer::flushRegion, regionCoordPair);
    }

    @Override
    public void flushColumns() {
        Task.async("Flushing Writer Regions", TaskWeight.NORMAL, writer::flushColumns);
    }
}