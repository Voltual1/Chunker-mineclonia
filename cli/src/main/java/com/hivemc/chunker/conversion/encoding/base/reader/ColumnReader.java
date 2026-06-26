package com.hivemc.chunker.conversion.encoding.base.reader;

import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.scheduling.task.Task;

/**
 * A reader which reads a column from a format.
 */
public interface ColumnReader {
    /**
     * Invoked when the column that this handler was constructed with should be read.
     *
     * @param columnConversionHandler the output handler to call the relevant methods of.
     * @return a task that is completed when the column has been read and submitted.
     */
    Task<Void> readColumn(ColumnConversionHandler columnConversionHandler);
}