// [file name]: me.voltual.vb.ui.terminal.TerminalViewModel.kt
package me.voltual.vb.ui.terminal

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import androidx.work.multiprocess.RemoteWorkManager
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import me.voltual.vb.ui.TerminalExec
import me.voltual.vb.ui.Export
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import me.voltual.vb.core.database.repository.LogRepository
import me.voltual.vb.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import me.voltual.vb.data.ConversionSettingsDataStore
import me.voltual.vb.data.ConversionProgressDataStore
import java.util.concurrent.TimeUnit

class TerminalViewModel(
    private val context: Context,
    private val conversionSettingsDataStore: ConversionSettingsDataStore
) : ViewModel(), KoinComponent {

    private val logRepository: LogRepository by inject()

    private val _session = MutableStateFlow<TerminalSession?>(null)
    val session = _session.asStateFlow()

    private var isRunning = false

    fun startExecution(args: TerminalExec, navigator: Navigator) {
        if (isRunning) return
        isRunning = true

        viewModelScope.launch {
            val sessionClient = object : TerminalSessionClient {
                override fun onTextChanged(changedSession: TerminalSession) {
                    _session.value = changedSession
                }
                override fun onTitleChanged(changedSession: TerminalSession) {}
                override fun onSessionFinished(finishedSession: TerminalSession) {
                    isRunning = false
                }
                override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Terminal Copy", text)
                    clipboard?.setPrimaryClip(clip)
                }
                override fun onPasteTextFromClipboard(session: TerminalSession?) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = clipboard?.primaryClip
                    if (clip != null && clip.itemCount > 0 && session != null) {
                        val text = clip.getItemAt(0).coerceToText(context).toString()
                        session.emulator.paste(text)
                    }
                }
                override fun onBell(session: TerminalSession) {}
                override fun onColorsChanged(session: TerminalSession) {
                    _session.value = session
                }
                override fun onTerminalCursorStateChange(state: Boolean) {}
                override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
                override fun getTerminalCursorStyle(): Int = 0
                override fun logError(tag: String, message: String) {}
                override fun logWarn(tag: String, message: String) {}
                override fun logInfo(tag: String, message: String) {}
                override fun logDebug(tag: String, message: String) {}
                override fun logVerbose(tag: String, message: String) {}
                override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                override fun logStackTrace(tag: String, e: Exception) {}
            }

            val newSession = TerminalSession(
                "/system/bin/sh",
                context.filesDir.absolutePath,
                arrayOf("sh", "-c", "stty -echo && cat"),
                emptyArray(),
                5000,
                sessionClient
            )
            
            _session.value = newSession

            withContext(Dispatchers.IO) {
                runChunkerTask(newSession, args, navigator)
            }
        }
    }

    private suspend fun runChunkerTask(session: TerminalSession, args: TerminalExec, navigator: Navigator) {
        val crashLogFile = File(context.filesDir, "terminal_crash.log")
        val outBridge = TerminalPrintStream(session, crashLogFile)
        val oldOut = System.`out`
        val oldErr = System.err

        System.setOut(outBridge)
        System.setErr(outBridge)

        val userThreadCount = conversionSettingsDataStore.threadCount.first()
        val userProcessMaps = conversionSettingsDataStore.processMaps.first()

        val logFile = File(context.cacheDir, "slice_log.txt")
        if (logFile.exists()) {
            logFile.delete()
        }

        val tailJob = viewModelScope.launch(Dispatchers.IO) {
            val delayTime = 100L
            var filePointer = 0L
            while (isActive) {
                if (logFile.exists()) {
                    try {
                        RandomAccessFile(logFile, "r").use { raf ->
                            val length = raf.length()
                            if (length > filePointer) {
                                raf.seek(filePointer)
                                val buffer = ByteArray((length - filePointer).toInt())
                                raf.readFully(buffer)
                                val text = String(buffer, StandardCharsets.UTF_8)
                                outBridge.print(text)
                                filePointer = length
                            }
                        }
                    } catch (ignored: Exception) {}
                }
                delay(delayTime)
            }
        }

        var isSuccess = false
        var attempt = 0
        val maxAttempts = 15

        // 在任务开始时记录当前的转换配置，以便意外被强杀后能自动恢复
        ConversionProgressDataStore.saveActiveConversion(context, args.inputPath, args.outputPath, args.format)

        while (attempt < maxAttempts && !isSuccess) {
            attempt++
            if (attempt > 1) {
                outBridge.println("\n\u001B[1;33m[System] Connection lost. Resuming conversion (Attempt $attempt/$maxAttempts)...\u001B[0m")
                delay(1500) // 等待 1.5 秒以便操作系统彻底清理并释放文件锁
            }

            try {
                val workData = workDataOf(
                    "inputPath" to args.inputPath,
                    "outputPath" to args.outputPath,
                    "format" to args.format,
                    "threadCount" to userThreadCount,
                    "processMaps" to userProcessMaps,
                    "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME" to context.packageName,
                    "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME" to "androidx.work.multiprocess.RemoteWorkerService"
                )

                val workRequest = OneTimeWorkRequestBuilder<ConversionWorker>()
                    .setInputData(workData)
                    .build()

                val workManager = RemoteWorkManager.getInstance(context)
                workManager.enqueueUniqueWork(
                    "world_conversion_work",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                val finalWorkInfo = WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id)
                    .first { it?.state?.isFinished == true }

                if (finalWorkInfo?.state == WorkInfo.State.SUCCEEDED) {
                    isSuccess = true
                } else {
                    // 确认有无产生切片保存进度。如有，我们将进入下一轮循环重新拉起 WorkRequest 自动续传
                    val worldId = calculateWorldIdentity(File(args.inputPath))
                    val progress = ConversionProgressDataStore.getProgress(context, worldId)
                    if (progress == 0) {
                        outBridge.println("\n\u001B[1;31m[FATAL ERROR] Background worker failed at startup without progress.\u001B[0m")
                        break
                    }
                }
            } catch (e: Exception) {
                outBridge.println("\n\u001B[1;33m[System] Process bridge exception: ${e.message}. Retrying...\u001B[0m")
            }
        }

        if (!isSuccess) {
            outBridge.println("\n\u001B[1;31m[FATAL ERROR] Sliced conversion failed after maximum retries.\u001B[0m")
        }

        tailJob.cancel() 
        System.setOut(oldOut)
        System.setErr(oldErr)
        isRunning = false
        session.finishIfRunning()

        val inputDir = File(context.filesDir, "world_input")
        if (inputDir.exists()) {
            inputDir.deleteRecursively()
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (crashLogFile.exists()) {
                    val logContent = crashLogFile.readText()
                    logRepository.insertLog(
                        type = "MINECLONIA_CONVERSION",
                        requestBody = "Input: ${args.inputPath}\nOutput: ${args.outputPath}\nFormat: ${args.format}",
                        responseBody = logContent,
                        status = if (isSuccess) "SUCCESS" else "FAILURE"
                    )
                    crashLogFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (isSuccess) {
                // 转换圆满完成，清空活跃转换缓存
                ConversionProgressDataStore.clearActiveConversion(context)
                withContext(Dispatchers.Main) {
                    navigator.navigate(Export)
                }
            } else {
                // 如果用户手动中途退出或者重试次数完全耗尽，也要清空状态避免下次打开自动跳转
                ConversionProgressDataStore.clearActiveConversion(context)
            }
        }
    }

    private fun calculateWorldIdentity(inputDir: File): String {
        val iconPng = File(inputDir, "icon.png")
        val iconJpeg = File(inputDir, "world_icon.jpeg")
        val targetFile = when {
            iconPng.exists() -> iconPng
            iconJpeg.exists() -> iconJpeg
            else -> null
        }
        return if (targetFile != null) {
            try {
                val bytes = targetFile.readBytes()
                val md = MessageDigest.getInstance("MD5")
                val digest = md.digest(bytes)
                digest.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                getFallbackIdentity(inputDir)
            }
        } else {
            getFallbackIdentity(inputDir)
        }
    }

    private fun getFallbackIdentity(inputDir: File): String {
        val levelDat = File(inputDir, "level.dat")
        val baseString = inputDir.absolutePath + "_" + (if (levelDat.exists()) levelDat.lastModified() else 0L)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(baseString.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private inner class TerminalPrintStream(val session: TerminalSession, val file: File) :
        PrintStream(ByteArrayOutputStream(), true) {

        init {
            try {
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun writeToCrashLog(text: String) {
            try {
                val cleanText = text.replace("\\u001B\\[[;\\d]*[ -/]*[@-~]".toRegex(), "")
                file.appendText(cleanText)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @Synchronized
        override fun println(x: String?) {
            val line = (x ?: "null") + "\n"
            val bytes = line.replace("\n", "\r\n").toByteArray(StandardCharsets.UTF_8)
            session.write(bytes, 0, bytes.size)
            writeToCrashLog(line)
        }

        @Synchronized
        override fun print(x: String?) {
            val text = x ?: "null"
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            session.write(bytes, 0, bytes.size)
            writeToCrashLog(text)
        }

        @Synchronized
        override fun write(buf: ByteArray, off: Int, len: Int) {
            session.write(buf, off, len)
            val text = String(buf, off, len, StandardCharsets.UTF_8)
            writeToCrashLog(text)
        }
    }

    companion object {
        init {
            // 最优先级静态初始化：强行关闭 mmap 以保证多进程自杀时 LevelDB 事务绝对落盘与安全性
            System.setProperty("leveldb.mmap", "false")
        }
    }
}