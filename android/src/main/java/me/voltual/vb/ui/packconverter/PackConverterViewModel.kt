package me.voltual.vb.ui.packconverter

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import androidx.work.multiprocess.RemoteWorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _logs = MutableStateFlow("")
    val logs: StateFlow<String> = _logs.asStateFlow()

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
        _logs.value = "准备开启多进程材质包转换任务...\n"

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

        startTailLog()

        // 监听 WorkManager 的完成状态
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(context)
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    _isRunning.value = false
                    tailJob?.cancel()
                    // 确保日志读完最后一部分
                    readLogFileTail()
                }
            }
        }
    }

    private fun startTailLog() {
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
                                _logs.value += text
                                filePointer = length
                            }
                        }
                    } catch (ignored: Exception) {}
                }
                delay(200)
            }
        }
    }

    private suspend fun readLogFileTail() = withContext(Dispatchers.IO) {
        val logFile = File(context.cacheDir, "pack_conversion_log.txt")
        if (logFile.exists()) {
            try {
                val fullText = logFile.readText(StandardCharsets.UTF_8)
                _logs.value = fullText
            } catch (e: Exception) {
                _logs.value += "\n读取最终日志失败: ${e.message}"
            }
        }
    }
}