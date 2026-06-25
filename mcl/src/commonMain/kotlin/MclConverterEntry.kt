package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import java.io.File

object MclConverterEntry {
    fun runConversion(columns: Iterable<ChunkerColumn>, outputFolder: String) {
        val manager = MclConverterManager(File(outputFolder))
        
        var count = 0
        for (column in columns) {
            manager.convertColumn(column)
            count++
            
            // 每 10 个 Column 提交一次，防止内存占用过高或事务过大
            if (count % 10 == 0) {
                manager.flush()
            }
        }
        
        manager.flush()
        manager.close()
    }
}