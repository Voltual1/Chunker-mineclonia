//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.core.utils

import me.voltual.vb.BuildConfig
import me.voltual.vb.KtorClient
import me.voltual.vb.data.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 定义一个密封类来封装检查更新的结果
sealed class UpdateCheckResult {
    data class Success(val updateInfo: UpdateInfo) : UpdateCheckResult()
    data class NoUpdate(val message: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {
    // 修改函数签名，使用回调传递结果
    fun checkForUpdates(onUpdateResult: (UpdateCheckResult) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = KtorClient.ApiServiceImpl.getLatestRelease()
                if (result.isSuccess) {
                    val update = result.getOrNull()
                    if (update != null) {
                        val currentVersion = BuildConfig.VERSION_NAME
                        val newVersion = update.tag_name.replace(Regex("[^\\d.]"), "")
                        if (newVersion > currentVersion) {
                            // 有新版本，传递 UpdateInfo
                            withContext(Dispatchers.Main) {
                                onUpdateResult(UpdateCheckResult.Success(update))
                            }
                        } else {
                            // 当前已是最新版本
                            withContext(Dispatchers.Main) {
                                onUpdateResult(UpdateCheckResult.NoUpdate("当前已是最新版本"))
                            }
                        }
                    } else {
                        // 获取更新信息失败
                        withContext(Dispatchers.Main) {
                            onUpdateResult(UpdateCheckResult.Error("获取更新信息失败"))
                        }
                    }
                } else {
                    // 检查更新失败
                    val errorMsg = "检查更新失败" + ": ${result.exceptionOrNull()?.message}"
                    withContext(Dispatchers.Main) {
                        onUpdateResult(UpdateCheckResult.Error(errorMsg))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 检查更新出错
                val errorMsg = "检查更新出错" + ": ${e.message}"
                withContext(Dispatchers.Main) {
                    onUpdateResult(UpdateCheckResult.Error(errorMsg))
                }
            }
        }
    }
}