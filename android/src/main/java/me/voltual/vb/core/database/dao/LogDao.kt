//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    /**
     * 插入一条新的日志记录。
     * 如果发生冲突，则替换旧记录（虽然在这里因为主键自增，基本不会发生）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntry)

    /**
     * 获取所有的日志记录，并按时间戳降序排列（最新的在最前面）。
     * 返回一个 Flow，当数据库内容变化时，它会自动发射新的列表。
     */
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    /**
     * 清空所有的日志记录。
     */
     // 新增：按 ID 列表删除日志
    @Query("DELETE FROM logs WHERE id IN (:logIds)")
    suspend fun deleteLogsByIds(logIds: List<Int>)
    @Query("DELETE FROM logs")
    suspend fun clearAll()
    @Query("SELECT * FROM logs WHERE requestBody LIKE :query OR responseBody LIKE :query ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<LogEntry>>
}