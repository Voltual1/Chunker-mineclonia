// [file name]: com.hivemc.chunker.conversion.handlers.ColumnConversionHandler.java
package com.hivemc.chunker.conversion.handlers;

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.scheduling.task.Task;

/**
 * Provides methods to submit intermediate column data to the writer.
 * The data goes through a pipeline and may not immediately go to the writer.
 */
public interface ColumnConversionHandler {
    /**
     * Called when a column has been read.
     *
     * @param column the column that has been read.
     * @return a task that is completed when the column has been fully converted and written.
     */
    Task<Void> convertColumn(ChunkerColumn column);

    /**
     * Called when a region (32 x 32 columns) has completed reading.
     *
     * @param regionCoordPair the co-ordinates of the region.
     * @return a task that completes when the flush is done.
     */
    Task<Void> flushRegion(RegionCoordPair regionCoordPair);

    /**
     * Called when all the column reading has completed.
     * @return a task that completes when the final flush is done.
     */
    Task<Void> flushColumns();
}