package me.voltual.vb.ui

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

@Composable
fun TerminalViewAndroidView(
    session: TerminalSession,
    modifier: Modifier = Modifier,
    initialTextSize: Int = 36,
    onFontSizeChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var currentTextSize by remember { mutableStateOf(initialTextSize) }
    var terminalViewRef by remember { mutableStateOf<TerminalView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val view = terminalViewRef ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    view.setTerminalCursorBlinkerRate(500)
                    view.setTerminalCursorBlinkerState(true, true)
                }
                Lifecycle.Event.ON_STOP -> {
                    view.setTerminalCursorBlinkerState(false, true)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            TerminalView(ctx, null).apply {
                setTextSize(currentTextSize)
                setTypeface(Typeface.MONOSPACE)
                
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()

                // 绑定并重写 Session 的 Client 回调，确保 onTextChanged 触发重绘
                val viewClient = object : TerminalSessionClient {
                    override fun onTextChanged(changedSession: TerminalSession) {
                        onScreenUpdated() // 关键：字符发生改变时，强制 TerminalView 重绘
                    }
                    override fun onTitleChanged(changedSession: TerminalSession) {}
                    override fun onSessionFinished(finishedSession: TerminalSession) {}
                    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Terminal Copy", text)
                        clipboard?.setPrimaryClip(clip)
                    }
                    override fun onPasteTextFromClipboard(session: TerminalSession?) {
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = clipboard?.primaryClip
                        if (clip != null && clip.itemCount > 0 && session != null) {
                            val text = clip.getItemAt(0).coerceToText(ctx).toString()
                            session.emulator.paste(text)
                        }
                    }
                    override fun onBell(session: TerminalSession) {}
                    override fun onColorsChanged(session: TerminalSession) {
                        onScreenUpdated()
                    }
                    override fun onTerminalCursorStateChange(enabled: Boolean) {
                        setTerminalCursorBlinkerState(enabled, false)
                    }
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
                session.updateTerminalSessionClient(viewClient)

                setTerminalViewClient(object : TerminalViewClient {
                    override fun onScale(scale: Float): Float {
                        val newSize = (currentTextSize * scale).toInt().coerceIn(24, 72)
                        if (newSize != currentTextSize) {
                            currentTextSize = newSize
                            setTextSize(currentTextSize)
                            onFontSizeChanged(newSize)
                        }
                        return 1.0f
                    }

                    override fun onSingleTapUp(e: MotionEvent) {
                        if (isSelectingText) {
                            stopTextSelectionMode()
                            return
                        }
                        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.showSoftInput(this@apply, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }

                    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                    override fun shouldEnforceCharBasedInput(): Boolean = false
                    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                    override fun isTerminalViewSelected(): Boolean = hasFocus()
                    override fun copyModeChanged(copyMode: Boolean) = Unit

                    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
                        if (e.isCtrlPressed) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_C -> {
                                    getSelectedText()?.let { copyTextToClipboard(ctx, it) }
                                    stopTextSelectionMode()
                                    return true
                                }
                                KeyEvent.KEYCODE_V -> {
                                    pasteTextFromClipboard(ctx, session)
                                    return true
                                }
                            }
                        }
                        return false
                    }

                    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

                    override fun onLongPress(event: MotionEvent): Boolean {
                        if (!isSelectingText) {
                            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            startTextSelectionMode(event)
                        }
                        return true
                    }

                    override fun readControlKey(): Boolean = false
                    override fun readAltKey(): Boolean = false
                    override fun readShiftKey(): Boolean = false
                    override fun readFnKey(): Boolean = false

                    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean {
                        if (ctrlDown && codePoint == 106 && !session.isRunning) {
                            return true
                        }
                        return false
                    }

                    override fun onEmulatorSet() {
                        setTerminalCursorBlinkerRate(500)
                        setTerminalCursorBlinkerState(true, true)
                    }

                    override fun logError(tag: String, message: String) {}
                    override fun logWarn(tag: String, message: String) {}
                    override fun logInfo(tag: String, message: String) {}
                    override fun logDebug(tag: String, message: String) {}
                    override fun logVerbose(tag: String, message: String) {}
                    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                    override fun logStackTrace(tag: String, e: Exception) {}
                })

                applyDarkTheme(session)
                attachSession(session)
                terminalViewRef = this
            }
        },
        update = { view ->
            if (view.mTermSession != session) {
                applyDarkTheme(session)
                view.attachSession(session)
                val bgColor = session.emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]
                view.setBackgroundColor(bgColor)
            }
        },
        modifier = modifier
    )
}

private fun applyDarkTheme(session: TerminalSession) {
    val colors = session.emulator.mColors.mCurrentColors
    if (colors.size > TextStyle.COLOR_INDEX_CURSOR) {
        // 设置深色背景、浅色前景和白色光标
        colors[TextStyle.COLOR_INDEX_BACKGROUND] = 0xFF121212.toInt() // 深灰背景
        colors[TextStyle.COLOR_INDEX_FOREGROUND] = 0xFFE0E0E0.toInt() // 浅白文字
        colors[TextStyle.COLOR_INDEX_CURSOR] = 0xFFFFFFFF.toInt()     // 白色光标
        
        // 基础 16 色适配暗色主题
        colors[0] = 0xFF121212.toInt() // Black
        colors[1] = 0xFFCF6679.toInt() // Red
        colors[2] = 0xFF03DAC6.toInt() // Green
        colors[3] = 0xFFF2C94C.toInt() // Yellow
        colors[4] = 0xFF3700B3.toInt() // Blue
        colors[5] = 0xFFBB86FC.toInt() // Magenta
        colors[6] = 0xFF03DAC6.toInt() // Cyan
        colors[7] = 0xFFE0E0E0.toInt() // White
        
        colors[8] = 0xFF555555.toInt() // Bright Black
        colors[9] = 0xFFFF8A80.toInt() // Bright Red
        colors[10] = 0xFFB9F6CA.toInt() // Bright Green
        colors[11] = 0xFFFFE57F.toInt() // Bright Yellow
        colors[12] = 0xFF82B1FF.toInt() // Bright Blue
        colors[13] = 0xFFFF80AB.toInt() // Bright Magenta
        colors[14] = 0xFF84FFFF.toInt() // Bright Cyan
        colors[15] = 0xFFFFFFFF.toInt() // Bright White
    }
}

private fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = android.content.ClipData.newPlainText("Terminal Selection", text)
    clipboard?.setPrimaryClip(clip)
}

private fun pasteTextFromClipboard(context: Context, session: TerminalSession) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = clipboard?.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val text = clip.getItemAt(0).coerceToText(context).toString()
        if (text.isNotEmpty()) {
            session.emulator.paste(text)
        }
    }
}