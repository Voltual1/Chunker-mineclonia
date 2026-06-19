// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
// 你应该已经收到了一份 GNU 通用公共许可证 of the GPL.
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package me.voltual.vb

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import me.voltual.vb.core.database.*
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.ui.theme.*
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.android.ext.android.inject
import org.koin.dsl.koinConfiguration
import java.io.File

class BBQApplication : Application(), KoinStartup {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  val themeStore: ThemeColorDataStore by inject()

  lateinit var database: AppDatabase
    private set

  override fun onCreate() {
    super.onCreate()
    instance = this

    // 初始化数据库
    database = AppDatabase.getDatabase(this)
    runBlocking {
      ThemeManager.updateCustomColors(themeStore.colorsFlow.first())
    }

    val crashLogFile = File(filesDir, "terminal_crash.log")

    // 注册 Native 崩溃信号处理器
    try {
      com.termux.terminal.JNI.setupNativeCrashHandler(crashLogFile.absolutePath)
    } catch (e: Throwable) {
      e.printStackTrace()
    }

    // 检查并恢复上次崩溃的日志
    checkAndRecoverCrashLog(crashLogFile)
  }

  private fun checkAndRecoverCrashLog(crashLogFile: File) {
    applicationScope.launch(Dispatchers.IO) {
      if (crashLogFile.exists()) {
        try {
          val logContent = crashLogFile.readText()
          if (logContent.isNotBlank()) {
            val logDao = database.logDao()
            val logEntry = LogEntry(
              type = "CRASH_RECOVERY",
              requestBody = "检测到上次运行未正常退出的终端日志",
              responseBody = logContent,
              status = "FAILURE"
            )
            logDao.insert(logEntry)
          }
          crashLogFile.delete()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  override fun onKoinStartup() = koinConfiguration {
    androidContext(this@BBQApplication)
    modules(appModule)
  }

  companion object {
    lateinit var instance: BBQApplication
      private set

    val context: Context
      get() = instance
  }
}