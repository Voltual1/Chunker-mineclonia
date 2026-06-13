// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.core.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.BuildConfig
import me.voltual.vb.KtorClient
import me.voltual.vb.R
import me.voltual.vb.data.UpdateInfo

/** 定义一个密封类来封装检查更新的结果 */
sealed class UpdateCheckResult {
  data class Success(val updateInfo: UpdateInfo) : UpdateCheckResult()

  data class NoUpdate(val message: String) : UpdateCheckResult()

  data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {

  /** 检查更新函数 针对标签格式如 "A321-1.0.1-release" */
  fun checkForUpdates(context: Context, onUpdateResult: (UpdateCheckResult) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val result =
          KtorClient.ApiServiceImpl.getLatestRelease(
            "https://gitee.com/api/v5/repos/Voltula/VB/releases/latest"
          )

        if (result.isSuccess) {
          val update = result.getOrNull()
          if (update != null) {
            val currentVersion = BuildConfig.VERSION_NAME // 假设为 "1.0.0"
            val rawTagName = update.tag_name // 例如 "VB-1.1.0-release"

            // 使用正则精准提取 x.y.z 格式的数字部分
            // \d+(\.\d+)+ 匹配数字开始，后面跟着至少一个“.数字”的组合
            val versionRegex = Regex("""\d+(\.\d+)+""")
            val newVersionMatch = versionRegex.find(rawTagName)?.value

            if (newVersionMatch != null && isNewerVersion(currentVersion, newVersionMatch)) {
              // 发现新版本
              withContext(Dispatchers.Main) { onUpdateResult(UpdateCheckResult.Success(update)) }
            } else {
              // 当前已是最新版本或解析失败
              withContext(Dispatchers.Main) {
                onUpdateResult(
                  UpdateCheckResult.NoUpdate(context.getString(R.string.already_latest_version))
                )
              }
            }
          } else {
            sendError(context, R.string.failed_to_get_update_info, onUpdateResult)
          }
        } else {
          val errorMsg =
            context.getString(R.string.check_update_failed) +
              ": ${result.exceptionOrNull()?.message}"
          withContext(Dispatchers.Main) { onUpdateResult(UpdateCheckResult.Error(errorMsg)) }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        val errorMsg = context.getString(R.string.check_update_error) + ": ${e.message}"
        withContext(Dispatchers.Main) { onUpdateResult(UpdateCheckResult.Error(errorMsg)) }
      }
    }
  }

  /** 语义化版本号比较 解决 "1.10" > "1.2" 的逻辑问题，而不仅仅是字符串对比 */
  private fun isNewerVersion(current: String, latest: String): Boolean {
    if (current == latest) return false

    val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
    val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

    val maxLength = maxOf(currentParts.size, latestParts.size)

    for (i in 0 until maxLength) {
      val v1 = currentParts.getOrElse(i) { 0 }
      val v2 = latestParts.getOrElse(i) { 0 }
      if (v1 < v2) return true
      if (v1 > v2) return false
    }
    return false
  }

  private suspend fun sendError(
    context: Context,
    resId: Int,
    onUpdateResult: (UpdateCheckResult) -> Unit,
  ) {
    withContext(Dispatchers.Main) {
      onUpdateResult(UpdateCheckResult.Error(context.getString(resId)))
    }
  }
}
