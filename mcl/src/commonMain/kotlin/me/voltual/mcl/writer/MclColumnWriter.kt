//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.mcl.writer

import me.voltual.mcl.core.MclConverterManager
import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import com.hivemc.chunker.scheduling.task.Task
import com.hivemc.chunker.scheduling.task.TaskWeight
import java.lang.Void

class MclColumnWriter(val manager: MclConverterManager, val dimension: Dimension) : ColumnWriter {
    override fun writeColumn(chunkerColumn: ChunkerColumn): Task<Void> {
        return Task.async("Mcl Write Column", TaskWeight.LOW) {
            // 将维度信息传递给管理器
            manager.convertColumn(chunkerColumn, dimension)
        }
    }

    override fun flushColumns() {
        manager.flush()
    }
}