package me.voltual.mcl

import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.scheduling.task.Task
import com.hivemc.chunker.scheduling.task.TaskWeight
import java.lang.Void

class MclColumnWriter(val manager: MclConverterManager) : ColumnWriter {
    override fun writeColumn(chunkerColumn: ChunkerColumn): Task<Void> {
        return Task.async("Mcl Write Column", TaskWeight.LOW) {
            manager.convertColumn(chunkerColumn)
        }
    }

    override fun flushColumns() {
        manager.flush()
    }
}