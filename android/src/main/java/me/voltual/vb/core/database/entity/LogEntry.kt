//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @OptIn(kotlin.time.ExperimentalTime::class) 
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val type: String, // e.g., "LLM_REQUEST", "POST_COMMENT"
    val requestBody: String,
    val responseBody: String,
    val status: String // e.g., "SUCCESS", "FAILURE"
) {
    /**
     * 格式化时间字符串 (yyyy-MM-dd HH:mm:ss)
     * 遵循 Kotlin 2.1 最新属性命名规范 (day, month 代替已弃用的 dayOfMonth, monthNumber)
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun formattedTime(): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        
        return with(localDateTime) {
            val yearStr = year.toString()
            val monthStr = month.toString().padStart(2, '0')
            val dayStr = day.toString().padStart(2, '0')
            val hourStr = hour.toString().padStart(2, '0')
            val minuteStr = minute.toString().padStart(2, '0')
            val secondStr = second.toString().padStart(2, '0')
            
            "$yearStr-$monthStr-$dayStr $hourStr:$minuteStr:$secondStr"
        }
    }
}