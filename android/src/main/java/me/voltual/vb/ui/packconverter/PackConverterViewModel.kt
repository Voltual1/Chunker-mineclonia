package me.voltual.vb.ui.packconverter

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.extension.openOutputStream
import com.anggrayudi.storage.extension.fromTreeUri
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.geysermc.pack.converter.PackConverter
import org.geysermc.pack.converter.pipeline.AssetConverters
import org.geysermc.pack.converter.util.LogListener
import org.geysermc.pack.converter.util.IccProfileStore
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path

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

        // 初始化本地终端 Session，用于直接接收实时输出
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

        writeToTerminal("\u001B[1;32m[System] 准备在主进程中执行材质包转换任务...\u001B[0m\r\n")

        val logFile = File(context.cacheDir, "pack_conversion_log.txt")
        if (logFile.exists()) logFile.delete()
        logFile.createNewFile()

        val logger = object : LogListener {
            private fun appendLog(text: String) {
                try {
                    logFile.appendText("$text\n")
                    viewModelScope.launch(Dispatchers.Main) {
                        writeFormattedLog(text + "\n")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun isDebugEnabled(): Boolean {
                return _debugMode.value
            }

            override fun debug(message: String) {
                if (_debugMode.value) appendLog("DEBUG: $message")
            }

            override fun debugUnchecked(message: String) {
                if (_debugMode.value) appendLog("DEBUG_UNCHECKED: $message")
            }

            override fun info(message: String) {
                appendLog("INFO: $message")
            }

            override fun warn(message: String) {
                appendLog("WARN: $message")
            }

            override fun error(message: String) {
                appendLog("ERROR: $message")
            }

            override fun error(message: String, error: Throwable?) {
                if (error != null) {
                    appendLog("ERROR: $message\n${error.stackTraceToString()}")
                } else {
                    appendLog("ERROR: $message")
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val tempInputDir = File(context.cacheDir, "pack_converter_input")
            tempInputDir.mkdirs()
            val tempInputZip = File(tempInputDir, "input_pack.zip")
            
            val tempOutputDir = File(context.cacheDir, "pack_converter_output")
            tempOutputDir.mkdirs()
            val tempOutputMcpack = File(tempOutputDir, "${_packName.value}.mcpack")

            val vanillaPackZip = File(context.cacheDir, "Vanilla-Assets.zip")

            var success = false
            try {
                // 诊断 AWT Native JNI 库是否能被当前 CPU 架构正常加载
                try {
                    logger.info("系统：正在诊断 AWT Native Library...")
                    val hello = ro.andob.awtcompat.nativec.AwtCompatNativeComponents.getHelloWorldMesssage()
                    logger.info("系统：AWT JNI 测试消息: $hello")
                } catch (t: Throwable) {
                    logger.error("系统：AWT JNI 加载失败，这会导致 AWT/ImageIO 相关的材质转换不可用。请检查 APK 的 ABI 兼容性。", t)
                }

                // 初始化释放本地 Base64 ICC 颜色配置档
                logger.info("系统：释放并配置 AWT 颜色映射参数...")
                IccProfileStore.install(context.cacheDir)

                logger.info("系统：正在从 SAF 拷贝输入材质包...")
                context.contentResolver.openInputStream(currentInput)?.use { inputStream ->
                    FileOutputStream(tempInputZip).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                logger.info("系统：输入文件拷贝完毕，开始转换过程。")
                val converter = PackConverter()
                    .enforcePackCheck(true)
                    .input(Path.of(tempInputZip.absolutePath))
                    .output(Path.of(tempOutputMcpack.absolutePath))
                    .packName(_packName.value)
                    .vanillaPackPath(Path.of(vanillaPackZip.absolutePath))
                    .converters(AssetConverters.converters(_debugMode.value))
                    .logListener(logger)

                converter.convert().pack()

                logger.info("系统：转换处理完成，正在将结果写入用户选择的输出目录...")
                val outputDirDoc = context.fromTreeUri(currentOutput)
                if (outputDirDoc == null || !outputDirDoc.canWrite()) {
                    logger.error("系统：输出目录无法访问或没有写入权限。")
                } else {
                    val finalFileName = "${_packName.value}.mcpack"
                    val targetDoc = outputDirDoc.makeFile(context, finalFileName)
                    if (targetDoc == null) {
                        logger.error("系统：无法在输出目录创建文件 $finalFileName。")
                    } else {
                        targetDoc.openOutputStream(context)?.use { targetOutputStream ->
                            tempOutputMcpack.inputStream().use { input ->
                                input.copyTo(targetOutputStream)
                            }
                        }
                        success = true
                        logger.info("系统：导出成功！转换已彻底完成。")
                    }
                }
            } catch (e: Throwable) {
                logger.error("系统：转换期间发生严重错误", e)
                e.printStackTrace()
            } finally {
                try {
                    if (tempInputDir.exists()) tempInputDir.deleteRecursively()
                    if (tempOutputDir.exists()) tempOutputDir.deleteRecursively()
                } catch (ignored: Exception) {}

                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                    if (success) {
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

    override fun onCleared() {
        super.onCleared()
        _session.value?.finishIfRunning()
    }
}