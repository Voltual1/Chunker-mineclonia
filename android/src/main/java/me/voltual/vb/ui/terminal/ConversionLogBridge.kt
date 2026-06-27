// [file name]: me.voltual.vb.ui.terminal.ConversionLogBridge.kt
package me.voltual.vb.ui.terminal

import java.util.concurrent.atomic.AtomicReference

/**
 * A thread-safe bridge to route stdout/stderr logs from background CoroutineWorker
 * to the terminal screen session.
 */
object ConversionLogBridge {
    private val logListener = AtomicReference<((String) -> Unit)?>()

    fun setListener(listener: ((String) -> Unit)?) {
        logListener.set(listener)
    }

    fun print(text: String) {
        logListener.get()?.invoke(text)
    }

    fun println(text: String) {
        logListener.get()?.invoke(text + "\n")
    }
}