package me.voltual.vb.ui

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

@Composable
fun TerminalViewAndroidView(
    modifier: Modifier = Modifier,
    onSessionCreated: (TerminalSession) -> Unit = {}
) {
    AndroidView(
        factory = { ctx ->
            TerminalView(ctx, null).apply {
                // 记录当前字体大小（单位：sp）
                var currentTextSize = 48

                // 设置初始字体大小
                setTextSize(currentTextSize)

                setTerminalViewClient(object : TerminalViewClient {
                    override fun onScale(scale: Float): Float {
                        // 计算新字体大小，限制在 8-48 sp 之间
                        val newSize = (currentTextSize * scale).toInt().coerceIn(8, 48)
                        if (newSize != currentTextSize) {
                            currentTextSize = newSize
                            setTextSize(currentTextSize)
                        }
                        return scale
                    }

                    override fun onSingleTapUp(e: MotionEvent) {
                        // 单点触控：如果处于文本选择模式，则退出选择模式
                        if (isSelectingText) {
                            stopTextSelectionMode()
                        }
                    }

                    override fun shouldBackButtonBeMappedToEscape() = false
                    override fun shouldEnforceCharBasedInput() = false
                    override fun shouldUseCtrlSpaceWorkaround() = false
                    override fun isTerminalViewSelected() = true
                    override fun copyModeChanged(copyMode: Boolean) = Unit

                    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
                        // 支持 Ctrl+Shift+C 复制，Ctrl+Shift+V 粘贴
                        if (e.isCtrlPressed && e.isShiftPressed) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_C -> {
                                    getSelectedText()?.let { copyTextToClipboard(it) }
                                    return true
                                }
                                KeyEvent.KEYCODE_V -> {
                                    pasteTextFromClipboard()
                                    return true
                                }
                            }
                        }
                        return false
                    }

                    override fun onKeyUp(keyCode: Int, e: KeyEvent) = false

                    override fun onLongPress(event: MotionEvent): Boolean {
                        // 长按启动文本选择模式
                        startTextSelectionMode(event)
                        return true
                    }

                    override fun readControlKey() = false
                    override fun readAltKey() = false
                    override fun readShiftKey() = false
                    override fun readFnKey() = false
                    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession) = false
                    override fun onEmulatorSet() = Unit

                    override fun logError(tag: String, message: String) = Unit
                    override fun logWarn(tag: String, message: String) = Unit
                    override fun logInfo(tag: String, message: String) = Unit
                    override fun logDebug(tag: String, message: String) = Unit
                    override fun logVerbose(tag: String, message: String) = Unit
                    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) = Unit
                    override fun logStackTrace(tag: String, e: Exception) = Unit
                })

                val session = TerminalSession(
                    "/system/bin/sh",
                    "/data/local/tmp",
                    arrayOf("sh"),
                    arrayOf("TERM=xterm-256color"),
                    1000,
                    object : TerminalSessionClient {
                        override fun onTextChanged(session: TerminalSession) = Unit
                        override fun onTitleChanged(session: TerminalSession) = Unit
                        override fun onSessionFinished(session: TerminalSession) = Unit
                        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
                            copyTextToClipboard(text)
                        }
                        override fun onPasteTextFromClipboard(session: TerminalSession?) {
                            pasteTextFromClipboard()
                        }
                        override fun onBell(session: TerminalSession) = Unit
                        override fun onColorsChanged(session: TerminalSession) = Unit
                        override fun onTerminalCursorStateChange(state: Boolean) = Unit
                        override fun setTerminalShellPid(session: TerminalSession, pid: Int) = Unit
                        override fun getTerminalCursorStyle(): Int? = null
                        override fun logError(tag: String, message: String) = Unit
                        override fun logWarn(tag: String, message: String) = Unit
                        override fun logInfo(tag: String, message: String) = Unit
                        override fun logDebug(tag: String, message: String) = Unit
                        override fun logVerbose(tag: String, message: String) = Unit
                        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) = Unit
                        override fun logStackTrace(tag: String, e: Exception) = Unit
                    }
                )
                attachSession(session)
                onSessionCreated(session)
            }
        },
        modifier = modifier
    )
}

// 复制文本到剪贴板
private fun TerminalView.copyTextToClipboard(text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Terminal Selection", text)
    clipboard.setPrimaryClip(clip)
}

// 从剪贴板粘贴文本
private fun TerminalView.pasteTextFromClipboard() {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = clipboard.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val text = clip.getItemAt(0).coerceToText(context).toString()
        if (text.isNotEmpty()) {
            mTermSession?.write(text)
        }
    }
}