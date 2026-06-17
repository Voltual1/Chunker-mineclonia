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
    
    // 维持当前的字体大小状态
    var currentTextSize by remember { mutableStateOf(initialTextSize) }
    
    // 临时持有 TerminalView 引用，以便生命周期观察者控制光标闪烁
    var terminalViewRef by remember { mutableStateOf<TerminalView?>(null) }

    // 监听生命周期以控制光标闪烁器的启动与停止
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val view = terminalViewRef ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // 启动光标闪烁，闪烁间隔设为 500 毫秒
                    view.setTerminalCursorBlinkerRate(500)
                    view.setTerminalCursorBlinkerState(true, true)
                }
                Lifecycle.Event.ON_STOP -> {
                    // 停止光标闪烁以节省资源
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
                // 初始化配置
                setTextSize(currentTextSize)
                setTypeface(Typeface.MONOSPACE)
                
                // 允许获取焦点并处理输入
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()

                setTerminalViewClient(object : TerminalViewClient {
                    override fun onScale(scale: Float): Float {
                        // 缩放手势处理：限制字体大小在 24 到 72 之间
                        val newSize = (currentTextSize * scale).toInt().coerceIn(24, 72)
                        if (newSize != currentTextSize) {
                            currentTextSize = newSize
                            setTextSize(currentTextSize)
                            onFontSizeChanged(newSize)
                        }
                        return 1.0f // 消费缩放事件，重置缩放因子为 1
                    }

                    override fun onSingleTapUp(e: MotionEvent) {
                        // 单击：如果处于选择模式则退出
                        if (isSelectingText) {
                            stopTextSelectionMode()
                            return
                        }
                        // 唤起软键盘
                        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.showSoftInput(this@apply, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }

                    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                    override fun shouldEnforceCharBasedInput(): Boolean = false
                    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                    override fun isTerminalViewSelected(): Boolean = hasFocus()
                    override fun copyModeChanged(copyMode: Boolean) = Unit

                    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
                        // 支持物理键盘的快捷键
                        if (e.isCtrlPressed) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_C -> {
                                    // 复制选中文本
                                    getSelectedText()?.let { copyTextToClipboard(ctx, it) }
                                    stopTextSelectionMode()
                                    return true
                                }
                                KeyEvent.KEYCODE_V -> {
                                    // 粘贴文本
                                    pasteTextFromClipboard(ctx, session)
                                    return true
                                }
                            }
                        }
                        return false
                    }

                    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

                    override fun onLongPress(event: MotionEvent): Boolean {
                        // 长按开启文本选择模式
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
                        // 拦截 Ctrl+J 动作
                        if (ctrlDown && codePoint == 106 && !session.isRunning) {
                            return true
                        }
                        return false
                    }

                    override fun onEmulatorSet() {
                        // 模拟器就绪后，启动光标闪烁
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

                // 绑定 Session
                attachSession(session)
                terminalViewRef = this
            }
        },
        update = { view ->
            // 运行时如果 Session 发生变化，重新绑定
            if (view.mTermSession != session) {
                view.attachSession(session)
                // 更新背景色以匹配终端配色
                val bgColor = session.emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]
                view.setBackgroundColor(bgColor)
            }
        },
        modifier = modifier
    )
}

// 复制到系统剪贴板
private fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = android.content.ClipData.newPlainText("Terminal Selection", text)
    if (clipboard != null && clip != null) {
        clipboard.setPrimaryClip(clip)
    }
}

// 从系统剪贴板粘贴
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