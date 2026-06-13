package me.voltual.mcl

import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn

class MclColumnWriter(val manager: MclConverterManager) : ColumnWriter {
    override fun writeColumn(chunkerColumn: ChunkerColumn) {
        manager.convertColumn(chunkerColumn)
    }

    override fun flushColumns() {
        manager.flush()
    }
}