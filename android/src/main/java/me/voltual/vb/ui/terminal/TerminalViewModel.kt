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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import com.hivemc.chunker.cli.CLI
import picocli.CommandLine
import me.voltual.vb.core.database.repository.LogRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TerminalViewModel(
    private val context: Context
) : ViewModel(), KoinComponent {

    private val logRepository: LogRepository by inject()

    private val _session = MutableStateFlow<TerminalSession?>(null)
    val session = _session.asStateFlow()

    private var isRunning = false

    fun startExecution(args: TerminalExec) {
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
                runChunkerTask(newSession, args)
            }
        }
    }

    private fun runChunkerTask(session: TerminalSession, args: TerminalExec) {
        val crashLogFile = File(context.filesDir, "terminal_crash.log")
        val outBridge = TerminalPrintStream(session, crashLogFile)
        val oldOut = System.`out`
        val oldErr = System.err

        System.setOut(outBridge)
        System.setErr(outBridge)

        var isSuccess = false

        try {
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
        } catch (e: Exception) {
            outBridge.println("\n\u001B[1;31m[FATAL ERROR] Conversion failed!\u001B[0m")
            e.printStackTrace(outBridge)
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
            isRunning = false
            session.finishIfRunning()

            // 正常结束时，读取实时写入的文件内容，保存到 Room 数据库，并清理临时文件
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (crashLogFile.exists()) {
                        val logContent = crashLogFile.readText()
                        logRepository.insertLog(
                            type = "CHUNKER_CONVERSION",
                            requestBody = "Input: ${args.inputPath}\nOutput: ${args.outputPath}\nFormat: ${args.format}",
                            responseBody = logContent,
                            status = if (isSuccess) "SUCCESS" else "FAILURE"
                        )
                        crashLogFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                // 剔除 ANSI 颜色转义码
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