//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.vb.ui

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.isSystemInDarkTheme
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
import me.voltual.vb.core.ui.theme.ThemeManager // 导入你的主题管理器

@Composable
fun TerminalViewAndroidView(
    session: TerminalSession,
    modifier: Modifier = Modifier,
    initialTextSize: Int = 36,
    onFontSizeChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 1. 联动你的 ThemeManager 自动计算出当前到底是 Dark 还是 Light
    val systemIsDark = isSystemInDarkTheme()
    val isDarkTheme = remember(systemIsDark, ThemeManager.themeMode) {
        ThemeManager.calculateIsDark(systemIsDark)
    }
    
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

                val viewClient = object : TerminalSessionClient {
                    override fun onTextChanged(changedSession: TerminalSession) {
                        onScreenUpdated() 
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

                attachSession(session)
                terminalViewRef = this
            }
        },
        update = { view ->
            if (view.mTermSession != session) {
                view.attachSession(session)
            }

            // 2. 核心：根据 Compose 的明暗主题动态更新终端颜色
            val colors = session.emulator.mColors
            if (isDarkTheme) {
                // 暗色模式：经典 Termux 黑底白字
                colors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND] = Color.BLACK
                colors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND] = Color.WHITE
                view.setBackgroundColor(Color.BLACK)
            } else {
                // 浅色模式：白底黑字（如果你希望无论如何都是黑底，把这里也改成 Color.BLACK/WHITE 即可）
                colors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND] = Color.WHITE
                colors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND] = Color.BLACK
                view.setBackgroundColor(Color.WHITE)
            }
            
            // 通知终端刷新颜色
            view.onScreenUpdated()
        },
        modifier = modifier
    )
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