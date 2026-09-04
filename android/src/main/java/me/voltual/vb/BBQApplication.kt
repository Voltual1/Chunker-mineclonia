@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package me.voltual.vb

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
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

class BBQApplication : Application(), KoinStartup, Configuration.Provider {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    val themeStore: ThemeColorDataStore by inject()

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val processName = getProcessName(this)
        
        // 针对子进程强制手动初始化 WorkManager
        if (processName != null && processName.endsWith(":conversion")) {
            try {
                WorkManager.initialize(this, workManagerConfiguration)
            } catch (e: Exception) {
                // 已经初始化过则忽略
            }
            return
        }

        database = AppDatabase.getDatabase(this)
        runBlocking {
            ThemeManager.updateCustomColors(themeStore.colorsFlow.first())
        }

        WorkManager.getInstance(this).pruneWork()

        val crashLogFile = File(filesDir, "terminal_crash.log")
        try {
            com.termux.terminal.JNI.setupNativeCrashHandler(crashLogFile.absolutePath)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        checkAndRecoverCrashLog(crashLogFile)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    private fun getProcessName(context: Context): String? {
        val pid = android.os.Process.myPid()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
    }

/*    private fun checkAndRecoverCrashLog(crashLogFile: File) {
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
    }*/

    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@BBQApplication)
        modules(appModule)
    }

    companion object {
        init {
            System.setProperty("leveldb.mmap", "false")
        }

        lateinit var instance: BBQApplication
            private set

        val context: Context
            get() = instance
    }
}
