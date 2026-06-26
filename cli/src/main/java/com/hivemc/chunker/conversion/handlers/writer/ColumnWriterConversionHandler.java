package com.hivemc.chunker.conversion.handlers.writer;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ColumnConversionHandler which delegates the methods asynchronously to the writer.
 */
public class ColumnWriterConversionHandler implements ColumnConversionHandler {
    protected final ColumnWriter writer;
    protected final Converter converter;
    private final AtomicInteger activeWrites = new AtomicInteger(0);

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
        activeWrites.incrementAndGet();
        Task.asyncUnwrap("Writing Column Dispatch", TaskWeight.NORMAL, () -> {
            try {
                return writer.writeColumn(column).then("Writing Column Done", TaskWeight.LOW, () -> {
                    converter.decrementActiveColumns(); // Release the global throttle slot
                    synchronized (activeWrites) {
                        if (activeWrites.decrementAndGet() == 0) {
                            activeWrites.notifyAll(); // Notify region flush
                        }
                    }
                });
            } catch (Throwable t) {
                converter.logNonFatalException(t);
                converter.decrementActiveColumns();
                synchronized (activeWrites) {
                    if (activeWrites.decrementAndGet() == 0) {
                        activeWrites.notifyAll();
                    }
                }
                return Task.async("Failed Write Column", TaskWeight.LOW, () -> {});
            }
        });
    }

    @Override
    public void flushRegion(RegionCoordPair regionCoordPair) {
        // Block the calling thread (our dedicated reader thread) until all columns of this region are written
        synchronized (activeWrites) {
            while (activeWrites.get() > 0) {
                try {
                    activeWrites.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        try {
            writer.flushRegion(regionCoordPair);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flushColumns() {
        // Block the calling thread until all remaining columns are written
        synchronized (activeWrites) {
            while (activeWrites.get() > 0) {
                try {
                    activeWrites.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        try {
            writer.flushColumns();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}