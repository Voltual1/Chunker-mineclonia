package me.voltual.vb

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity 
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.voltual.vb.data.UserAgreementDataStore
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.database.dao.LogDao
import me.voltual.vb.ui.*
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val agreementDataStore: UserAgreementDataStore by inject()    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_BBQ_Main)
        super.onCreate(savedInstanceState)

        setContent {
            PyrolysisApp(
                agreementDataStore = agreementDataStore,
                platformEntryProvider = { _, _ -> null }
            )
        }
    }

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val crashReport = getCrashReport(throwable)
            val logDao: LogDao by inject()
            CoroutineScope(Dispatchers.IO).launch {
                val logEntry = LogEntry(
                    type = "CRASH",
                    requestBody = "MainActivity 崩溃",
                    responseBody = crashReport,
                    status = "FAILURE"
                )
                logDao.insert(logEntry)
            }.invokeOnCompletion {
                CrashLogActivity.start(BBQApplication.instance, crashReport)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
    
    private fun getCrashReport(throwable: Throwable): String {
        val stackTrace = throwable.stackTraceToString()
        val deviceInfo = """
            设备型号: ${android.os.Build.MODEL}
            Android 版本: ${android.os.Build.VERSION.RELEASE}
            App 版本: ${BuildConfig.VERSION_NAME}
        """.trimIndent()
        return """
            崩溃信息: ${throwable.message}
            
            设备信息:
            $deviceInfo
            
            堆栈跟踪:
            $stackTrace
        """.trimIndent()
    }
}