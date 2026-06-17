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
            // 在主线程创建完整的 Session 客户端
            val sessionClient = object : TerminalSessionClient {
                override fun onTextChanged(changedSession: TerminalSession) {
                    // 当终端屏幕字符发生变化时触发更新
                    _session.value = changedSession
                }

                override fun onTitleChanged(changedSession: TerminalSession) {}

                override fun onSessionFinished(finishedSession: TerminalSession) {
                    isRunning = false
                }

                override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
                    // 支持终端内部通过控制序列触发剪贴板复制
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Terminal Copy", text)
                    if (clipboard != null && clip != null) {
                        clipboard.setPrimaryClip(clip)
                    }
                }

                override fun onPasteTextFromClipboard(session: TerminalSession?) {
                    // 支持终端内部通过控制序列触发剪贴板粘贴
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = clipboard?.primaryClip
                    if (clip != null && clip.itemCount > 0 && session != null) {
                        val text = clip.getItemAt(0).coerceToText(context).toString()
                        session.emulator.paste(text)
                    }
                }

                override fun onBell(session: TerminalSession) {
                    // 触发蜂鸣器或震动（可选）
                }

                override fun onColorsChanged(session: TerminalSession) {
                    // 颜色发生变化时刷新状态
                    _session.value = session
                }

                override fun onTerminalCursorStateChange(state: Boolean) {}

                override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

                override fun getTerminalCursorStyle(): Int = 0 // 使用默认 BLOCK 样式

                override fun logError(tag: String, message: String) {}
                override fun logWarn(tag: String, message: String) {}
                override fun logInfo(tag: String, message: String) {}
                override fun logDebug(tag: String, message: String) {}
                override fun logVerbose(tag: String, message: String) {}
                override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                override fun logStackTrace(tag: String, e: Exception) {}
            }

            val newSession = TerminalSession(
                "/system/bin/true", // 虚拟 Shell 路径
                context.filesDir.absolutePath,
                arrayOf("chunker-exec"),
                emptyArray(),
                5000, // 增加回滚行数限制以保存更多 Chunker 日志
                sessionClient
            )
            
            _session.value = newSession

            // 切换到 IO 线程执行 Chunker 任务
            withContext(Dispatchers.IO) {
                runChunkerTask(newSession, args)
            }
        }
    }

    private fun runChunkerTask(session: TerminalSession, args: TerminalExec) {
        val outBridge = TerminalPrintStream(session)
        val oldOut = System.`out`
        val oldErr = System.err

        System.setOut(outBridge)
        System.setErr(outBridge)

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

            // 启动 Chunker 转换器
            val cli = CLI()
            CommandLine(cli).execute(*cliArgs)

            outBridge.println("\n\u001B[1;32m[SUCCESS] Conversion completed successfully!\u001B[0m")
        } catch (e: Exception) {
            outBridge.println("\n\u001B[1;31m[FATAL ERROR] Conversion failed!\u001B[0m")
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