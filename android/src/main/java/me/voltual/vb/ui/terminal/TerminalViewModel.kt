package me.voltual.vb.ui.terminal

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.ui.TerminalExec
import me.voltual.vb.ui.Export
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import com.hivemc.chunker.cli.CLI
import picocli.CommandLine
import me.voltual.vb.core.database.repository.LogRepository
import me.voltual.vb.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import me.voltual.mcl.MclLevelWriter
import java.util.UUID

class TerminalViewModel(
    private val context: Context
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
                "/system/bin/cat",
                context.filesDir.absolutePath,
                arrayOf("cat"),
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

    private fun runChunkerTask(session: TerminalSession, args: TerminalExec, navigator: Navigator) {
        val crashLogFile = File(context.filesDir, "terminal_crash.log")
        val outBridge = TerminalPrintStream(session, crashLogFile)
        val oldOut = System.`out`
        val oldErr = System.err

        System.setOut(outBridge)
        System.setErr(outBridge)

        var isSuccess = false

        try {
            if (args.format == "MINECLONIA") {
                // 执行 Mineclonia 自定义转换管线
                outBridge.println("\u001B[1;36m[Mineclonia Engine] Starting Minecraft to Mineclonia Conversion...\u001B[0m")
                outBridge.println("Source Path : \u001B[33m${args.inputPath}\u001B[0m")
                outBridge.println("Target Path : \u001B[33m${args.outputPath}\u001B[0m")
                outBridge.println("================================================")

                val inputPathFile = File(args.inputPath)
                val outputPathFile = File(args.outputPath)

                // 1. 创建 WorldConverter 并配置参数
                val mclConverter = WorldConverter(UUID.randomUUID())
                mclConverter.setProcessItems(true)
                mclConverter.setProcessEntities(true)
                mclConverter.setProcessBlockEntities(true)
                mclConverter.setProcessBiomes(true)
                mclConverter.setProcessLighting(true)
                mclConverter.setProcessColumnPreTransform(true)

                // 2. 自动探测输入格式
                outBridge.println("Detecting input world format...")
                val readerOptional = EncodingType.findReader(inputPathFile, mclConverter)
                if (!readerOptional.isPresent) {
                    throw IllegalStateException("Failed to detect input world format!")
                }
                val reader = readerOptional.get()
                outBridge.println("Detected format: \u001B[32m${reader.encodingType.name}\u001B[0m Version: \u001B[32m${reader.version}\u001B[0m")

                // 3. 创建 Mineclonia 专属 Writer
                val writer = MclLevelWriter(outputPathFile)

                // 4. 启动转换任务
                outBridge.println("Initializing Mineclonia conversion pipeline...")
                val trackedTask = mclConverter.convert(reader, writer)

                // 5. 循环监听进度直到任务完成
                val future = trackedTask.future()
                var lastProgress = -1.0
                while (!future.isDone) {
                    val progress = trackedTask.progress
                    if (progress != lastProgress) {
                        val percentage = (progress * 100).toInt()
                        outBridge.println("Conversion Progress: \u001B[33m$percentage%\u001B[0m")
                        lastProgress = progress
                    }
                    Thread.sleep(200)
                }

                // 获取结果以抛出任何执行中产生的异常
                future.get()

                outBridge.println("\n\u001B[1;32m[SUCCESS] Mineclonia conversion completed successfully!\u001B[0m")
                isSuccess = true
            } else {
                // 执行标准 Chunker 命令行任务
                outBridge.println("\u001B[1;36m[Chunker Engine] Starting World Conversion Task...\u001B[0m")
                outBridge.println("Source Path : \u001B[33m${args.inputPath}\u001B[0m")
                outBridge.println("Target Path : \u001B[33m${args.outputPath}\u001B[0m")
                outBridge.println("Target Format: \u001B[32m${args.format}\u001B[0m")
                outBridge.println("================================================")

                val cliArgs = arrayOf(
                    "--inputDirectory", args.inputPath,
                    "--outputFormat", args.format,
                    "--outputDirectory", args.outputPath
                )

                val cli = CLI()
                CommandLine(cli).execute(*cliArgs)

                outBridge.println("\n\u001B[1;32m[SUCCESS] Conversion completed successfully!\u001B[0m")
                isSuccess = true
            }
        } catch (e: Exception) {
            outBridge.println("\n\u001B[1;31m[FATAL ERROR] Conversion failed!\u001B[0m")
            e.printStackTrace(outBridge)
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
            isRunning = false
            session.finishIfRunning()

            // 自动销毁复制过来的输入存档缓存（world_input），避免占用空间
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
                    withContext(Dispatchers.Main) {
                        navigator.navigate(Export)
                    }
                }
            }
        }
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
}