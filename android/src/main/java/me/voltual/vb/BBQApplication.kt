// [file name]: me.voltual.vb.BBQApplication.kt
@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package me.voltual.vb

import android.app.Application
import android.app.ActivityManager
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

        // 强行对所有进程禁用 LevelDB mmap 以免写入意外中断引起文件物理损坏
        System.setProperty("leveldb.mmap", "false")

        val processName = getProcessName(this)
        
        // 遇到远程多进程服务时，直接退避，防止数据库争锁以及 Koin 载入异常
        if (processName != null && processName.endsWith(":conversion")) {
            return
        }

        database = AppDatabase.getDatabase(this)
        runBlocking {
            ThemeManager.updateCustomColors(themeStore.colorsFlow.first())
        }

        val crashLogFile = File(filesDir, "terminal_crash.log")

        try {
            com.termux.terminal.JNI.setupNativeCrashHandler(crashLogFile.absolutePath)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        checkAndRecoverCrashLog(crashLogFile)
    }

    private fun getProcessName(context: Context): String? {
        val pid = android.os.Process.myPid()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
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