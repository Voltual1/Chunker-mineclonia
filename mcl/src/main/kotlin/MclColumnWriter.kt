package me.voltual.mcl

import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn

class MclColumnWriter(val manager: MclConverterManager) : ColumnWriter {

    override fun writeColumn(chunkerColumn: ChunkerColumn) {
        // 调用我们之前在 MclConverterManager 中写好的转换逻辑
        manager.convertColumn(chunkerColumn)
    }

    override fun flushColumns() {
        manager.flush()
    }
}