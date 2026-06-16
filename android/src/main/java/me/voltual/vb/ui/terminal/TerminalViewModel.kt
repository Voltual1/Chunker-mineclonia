package me.voltual.vb.ui.terminal

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
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import com.hivemc.chunker.cli.CLI
import picocli.CommandLine

class TerminalViewModel(
    private val context: Context
) : ViewModel() {

    private val _session = MutableStateFlow<TerminalSession?>(null)
    val session = _session.asStateFlow()

    private var isRunning = false

    fun startExecution(args: TerminalExec) {
        if (isRunning) return
        isRunning = true

        viewModelScope.launch {
            // 1. 在主线程创建 Session
            val sessionClient = createSessionClient()
            val newSession = TerminalSession(
                "/system/bin/true",
                context.filesDir.absolutePath,
                arrayOf("chunker-exec"),
                emptyArray(),
                2000,
                sessionClient
            )
            _session.value = newSession

            // 2. 切换到 IO 线程执行 Chunker 逻辑
            withContext(Dispatchers.IO) {
                runChunkerTask(newSession, args)
            }
        }
    }

    private fun createSessionClient() = object : TerminalSessionClient {
        override fun onTextChanged(session: TerminalSession) {}
        override fun onTitleChanged(session: TerminalSession) {}
        override fun onSessionFinished(session: TerminalSession) {}
        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}
        override fun onPasteTextFromClipboard(session: TerminalSession?) {}
        override fun onBell(session: TerminalSession) {}
        override fun onColorsChanged(session: TerminalSession) {}
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

    private fun runChunkerTask(session: TerminalSession, args: TerminalExec) {
        val outBridge = TerminalPrintStream(session)
        val oldOut = System.`out`
        val oldErr = System.err

        System.setOut(outBridge)
        System.setErr(outBridge)

        try {
            outBridge.println("\u001B[32m[Chunker Android MVP] Starting conversion...\u001B[0m")
            outBridge.println("Input: ${args.inputPath}")
            outBridge.println("Output: ${args.outputPath}")
            outBridge.println("Format: ${args.format}")
            outBridge.println("------------------------------------------------")

            val cliArgs = arrayOf(
                "--inputDirectory", args.inputPath,
                "--outputFormat", args.format,
                "--outputDirectory", args.outputPath
            )

            val cli = CLI()
            CommandLine(cli).execute(*cliArgs)

            outBridge.println("\n\u001B[32m[SUCCESS] Execution finished.\u001B[0m")
        } catch (e: Exception) {
            outBridge.println("\n\u001B[31m[ERROR] ${e.message}\u001B[0m")
            e.printStackTrace(outBridge)
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
            isRunning = false
        }
    }

    private inner class TerminalPrintStream(val session: TerminalSession) :
        PrintStream(ByteArrayOutputStream(), true) {

        override fun println(x: String?) {
            val bytes = ((x ?: "null") + "\r\n").toByteArray(StandardCharsets.UTF_8)
            session.write(bytes, 0, bytes.size)
        }

        override fun print(x: String?) {
            val bytes = (x ?: "null").toByteArray(StandardCharsets.UTF_8)
            session.write(bytes, 0, bytes.size)
        }

        override fun write(buf: ByteArray, off: Int, len: Int) {
            session.write(buf, off, len)
        }
    }
}