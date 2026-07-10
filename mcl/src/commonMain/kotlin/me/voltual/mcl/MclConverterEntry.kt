//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.mcl

import me.voltual.mcl.core.MclConverterManager


import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import java.io.File

object MclConverterEntry {
    /**
     * 运行转换任务，增加了对出生点坐标的支持
     */
    fun runConversion(
        columns: Iterable<ChunkerColumn>, 
        outputFolder: String,
        spawnX: Int = 0,
        spawnY: Int = 64,
        spawnZ: Int = 0
    ) {
        // 传入 4 个参数以匹配 MclConverterManager 的新构造函数
        val manager = MclConverterManager(File(outputFolder), spawnX, spawnY, spawnZ)
        
        var count = 0
        for (column in columns) {
            manager.convertColumn(column)
            count++
            
            // 每 100 个 Column 提交一次，防止内存占用过高或事务过大
            if (count % 100 == 0) {
                manager.flush()
            }
        }
        
        manager.flush()
        manager.close()
    }
}