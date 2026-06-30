package me.voltual.vb.ui.packconverter

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import androidx.work.multiprocess.RemoteWorkManager
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

class PackConverterViewModel(private val context: Context) : ViewModel() {

    private val _packName = MutableStateFlow("")
    val packName: StateFlow<String> = _packName.asStateFlow()

    private val _inputUri = MutableStateFlow<Uri?>(null)
    val inputUri: StateFlow<Uri?> = _inputUri.asStateFlow()

    private val _outputTreeUri = MutableStateFlow<Uri?>(null)
    val outputTreeUri: StateFlow<Uri?> = _outputTreeUri.asStateFlow()

    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _session = MutableStateFlow<TerminalSession?>(null)
    val session: StateFlow<TerminalSession?> = _session.asStateFlow()

    private var tailJob: Job? = null

    fun setPackName(name: String) {
        _packName.value = name
    }

    fun setInputUri(uri: Uri, fileName: String) {
        _inputUri.value = uri
        if (_packName.value.isBlank()) {
            _packName.value = fileName.replace(Regex("\\.[^.]+$"), "")
        }
    }

    fun setOutputTreeUri(uri: Uri) {
        _outputTreeUri.value = uri
    }

    fun setDebugMode(debug: Boolean) {
        _debugMode.value = debug
    }

    fun startConversion() {
        if (_isRunning.value) return
        val currentInput = _inputUri.value ?: return
        val currentOutput = _outputTreeUri.value ?: return

        _isRunning.value = true

        // 初始化终端 Session
        val sessionClient = object : TerminalSessionClient {
            override fun onTextChanged(changedSession: TerminalSession) {
                _session.value = changedSession
            }
            override fun onTitleChanged(changedSession: TerminalSession) {}
            override fun onSessionFinished(finishedSession: TerminalSession) {
                _isRunning.value = false
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

        writeToTerminal("\u001B[1;32m[System] 准备开启多进程材质包转换任务...\u001B[0m\r\n")

        val workData = workDataOf(
            "inputUri" to currentInput.toString(),
            "outputTreeUri" to currentOutput.toString(),
            "packName" to _packName.value,
            "debugMode" to _debugMode.value,
            "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME" to context.packageName,
            "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME" to "androidx.work.multiprocess.RemoteWorkerService"
        )

        val workRequest = OneTimeWorkRequestBuilder<PackConversionWorker>()
            .setInputData(workData)
            .build()

        val remoteWorkManager = RemoteWorkManager.getInstance(context)
        remoteWorkManager.enqueueUniqueWork(
            "pack_conversion_work",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        startTailLog(newSession)

        // 监听 WorkManager 的完成状态
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(context)
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    _isRunning.value = false
                    tailJob?.cancel()
                    // 确保日志读完最后一部分
                    readLogFileTail(newSession)
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        writeToTerminal("\r\n\u001B[1;32m[System] 材质包转换成功完成！\u001B[0m\r\n")
                    } else {
                        writeToTerminal("\r\n\u001B[1;31m[System] 材质包转换失败，请检查上方错误日志。\u001B[0m\r\n")
                    }
                }
            }
        }
    }

    private fun writeToTerminal(text: String) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        _session.value?.write(bytes, 0, bytes.size)
    }

    private fun colorizeLogLine(line: String): String {
        return when {
            line.startsWith("INFO: ") -> "\u001B[32m[INFO]\u001B[0m " + line.substring(6)
            line.startsWith("WARN: ") -> "\u001B[33m[WARN]\u001B[0m " + line.substring(6)
            line.startsWith("ERROR: ") -> "\u001B[31m[ERROR]\u001B[0m " + line.substring(7)
            line.startsWith("DEBUG: ") -> "\u001B[36m[DEBUG]\u001B[0m " + line.substring(7)
            line.startsWith("DEBUG_UNCHECKED: ") -> "\u001B[35m[DEBUG_UNCHECKED]\u001B[0m " + line.substring(17)
            else -> line
        }
    }

    private fun writeFormattedLog(text: String) {
        val lines = text.split("\n")
        val colorizedText = lines.joinToString("\r\n") { line ->
            colorizeLogLine(line)
        }
        writeToTerminal(colorizedText)
    }

    private fun startTailLog(activeSession: TerminalSession) {
        tailJob?.cancel()
        val logFile = File(context.cacheDir, "pack_conversion_log.txt")
        if (logFile.exists()) logFile.delete()

        tailJob = viewModelScope.launch(Dispatchers.IO) {
            var filePointer = 0L
            while (_isRunning.value) {
                if (logFile.exists()) {
                    try {
                        RandomAccessFile(logFile, "r").use { raf ->
                            val length = raf.length()
                            if (length > filePointer) {
                                raf.seek(filePointer)
                                val buffer = ByteArray((length - filePointer).toInt())
                                raf.readFully(buffer)
                                val text = String(buffer, StandardCharsets.UTF_8)
                                withContext(Dispatchers.Main) {
                                    writeFormattedLog(text)
                                }
                                filePointer = length
                            }
                        }
                    } catch (ignored: Exception) {}
                }
                delay(200)
            }
        }
    }

    private suspend fun readLogFileTail(activeSession: TerminalSession) = withContext(Dispatchers.IO) {
        val logFile = File(context.cacheDir, "pack_conversion_log.txt")
        if (logFile.exists()) {
            try {
                val fullText = logFile.readText(StandardCharsets.UTF_8)
                withContext(Dispatchers.Main) {
                    // 清屏并重新绘制完整着色日志
                    writeToTerminal("\u001B[2J\u001B[H")
                    writeFormattedLog(fullText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    writeToTerminal("\r\n\u001B[1;31m[System] 读取最终日志失败: ${e.message}\u001B[0m\r\n")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tailJob?.cancel()
        _session.value?.finishIfRunning()
    }
}