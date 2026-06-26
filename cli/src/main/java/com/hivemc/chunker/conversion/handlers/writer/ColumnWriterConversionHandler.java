package com.hivemc.chunker.conversion.handlers.writer;

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
    private final AtomicInteger activeWrites = new AtomicInteger(0);

    /**
     * Create a new column writer conversion handler.
     *
     * @param writer the writer to delegate methods to.
     */
    public ColumnWriterConversionHandler(ColumnWriter writer) {
        this.writer = writer;
    }

    @Override
    public void convertColumn(ChunkerColumn column) {
        activeWrites.incrementAndGet();
        Task.asyncConsume("Writing Column", TaskWeight.NORMAL, (col) -> {
            try {
                writer.writeColumn(col);
            } finally {
                synchronized (activeWrites) {
                    if (activeWrites.decrementAndGet() == 0) {
                        activeWrites.notifyAll();
                    }
                }
            }
        }, column);
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