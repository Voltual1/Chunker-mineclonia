//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.core.database.repository

import org.koin.core.annotation.Single

/**
 * 日志仓库，作为日志数据源的唯一入口。
 * 它封装了对 LogDao 的直接访问。
 */
@Single
class LogRepository(

    private val logDao: LogDao
) {

    // 提供一个 Flow 来观察所有的日志
    val allLogs = logDao.getAllLogs()

    /**
     * 插入一条日志记录。
     * 这是一个 suspend 函数，因为它执行的是数据库 I/O 操作。
     * @param type 日志类型 (e.g., "LLM_REQUEST")
     * @param requestBody 请求体
     * @param responseBody 响应体
     * @param status 状态 (e.g., "SUCCESS")
     */
    suspend fun insertLog(
        type: String,
        requestBody: String,
        responseBody: String,
        status: String
    ) {
        val logEntry = LogEntry(
            type = type,
            requestBody = requestBody,
            responseBody = responseBody,
            status = status
        )
        logDao.insert(logEntry)
    }
    
    // 提供一个公共方法来访问 DAO 的删除功能
    suspend fun deleteLogsByIds(logIds: List<Int>) {
        logDao.deleteLogsByIds(logIds)
    }

    /**
     * 清空所有日志。
     */
    suspend fun clearAllLogs() {
        logDao.clearAll()
    }
}