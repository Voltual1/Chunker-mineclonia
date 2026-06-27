// [file name]: me.voltual.vb.ui.terminal.TerminalViewModel.kt
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import me.voltual.vb.ui.TerminalExec
import me.voltual.vb.ui.Export
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import me.voltual.vb.core.database.repository.LogRepository
import me.voltual.vb.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Version
import me.voltual.vb.data.ConversionSettingsDataStore
import me.voltual.mcl.MclLevelWriter
import java.util.UUID

class TerminalViewModel(
    private val context: Context,
    private val conversionSettingsDataStore: ConversionSettingsDataStore
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
                "/system/bin/sh",
                context.filesDir.absolutePath,
                arrayOf("sh", "-c", "stty -echo && cat"),
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

    private suspend fun runChunkerTask(session: TerminalSession, args: TerminalExec, navigator: Navigator) {
        val crashLogFile = File(context.filesDir, "terminal_crash.log")
        val outBridge = TerminalPrintStream(session, crashLogFile)
        val oldOut = System.`out`
        val oldErr = System.err

        System.setOut(outBridge)
        System.setErr(outBridge)

        var isSuccess = false

        val userThreadCount = conversionSettingsDataStore.threadCount.first()
        val userProcessMaps = conversionSettingsDataStore.processMaps.first()

        try {
            val converter = WorldConverter(UUID.randomUUID())
            converter.setProcessItems(true)
            converter.setProcessEntities(true)
            converter.setProcessBlockEntities(true)
            converter.setProcessBiomes(true)
            converter.setProcessLighting(true)
            converter.setProcessColumnPreTransform(false)
            converter.setThreadCount(userThreadCount)
            converter.setProcessMaps(userProcessMaps)

            val isMineclonia = args.format == "MINECLONIA"
            val targetEngine = if (isMineclonia) "Mineclonia" else "Chunker"

            outBridge.println("\u001B[1;36m[$targetEngine Engine] Starting World Conversion Task...\u001B[0m")
            outBridge.println("Source Path : \u001B[33m${args.inputPath}\u001B[0m")
            outBridge.println("Target Path : \u001B[33m${args.outputPath}\u001B[0m")
            outBridge.println("Target Format: \u001B[32m${args.format}\u001B[0m")
            outBridge.println("Concurrency : \u001B[35m$userThreadCount Thread(s)\u001B[0m")
            outBridge.println("Process Maps: \u001B[35m$userProcessMaps\u001B[0m")
            outBridge.println("================================================")

            val inputPathFile = File(args.inputPath)
            val outputPathFile = File(args.outputPath)

            outBridge.println("Detecting input world format...")
            val readerOptional = EncodingType.findReader(inputPathFile, converter)
            if (!readerOptional.isPresent) {
                throw IllegalStateException("Failed to detect input world format!")
            }
            val reader = readerOptional.get()
            outBridge.println("Detected format: \u001B[32m${reader.encodingType.name}\u001B[0m Version: \u001B[32m${reader.version}\u001B[0m")

            val writer = if (isMineclonia) {
                MclLevelWriter(outputPathFile)
            } else {
                val targetTypeName = args.format.substringBefore("_")
                val targetVersionString = args.format.substringAfter("_").replace("_", ".")
                val encodingType = EncodingType.getTypes().find { it.name.equals(targetTypeName, ignoreCase = true) }
                    ?: throw IllegalArgumentException("Unsupported output format target: $targetTypeName")
                val outputVersion = Version.fromString(targetVersionString)
                outBridge.println("Creating Level Writer for target \u001B[32m${encodingType.name}\u001B[0m Version: \u001B[32m$outputVersion\u001B[0m...")
                
                val writerOpt = encodingType.createWriter(outputPathFile, outputVersion, converter)
                if (!writerOpt.isPresent) {
                    throw IllegalStateException("Failed to create writer for format ${encodingType.name} at version $outputVersion")
                }
                writerOpt.get()
            }

            outBridge.println("Initializing conversion pipeline...")
            val trackedTask = converter.convert(reader, writer)
            val future = trackedTask.future()

            // Launch aggressive Memory Monitor Coroutine
            val memMonitorJob = viewModelScope.launch(Dispatchers.IO) {
                val runtime = Runtime.getRuntime()
                var gcAttemptCount = 0

                while (isActive && !future.isDone) {
                    val maxMemory = runtime.maxMemory()
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    val ratio = usedMemory.toDouble() / maxMemory.toDouble()

                    if (ratio > 0.80) { // 80% threshold (Danger, start throttling)
                        if (!converter.isMemoryPaused) {
                            outBridge.println("\u001B[31m[System] Heavy memory load (${usedMemory/1024/1024}MB / ${maxMemory/1024/1024}MB). Pausing pipeline to allow GC...\u001B[0m")
                            converter.isMemoryPaused = true
                            gcAttemptCount = 0
                        }

                        // Aggressive GC polling while paused
                        System.gc()
                        System.runFinalization()
                        gcAttemptCount++
                        
                        if (gcAttemptCount % 5 == 0) {
                             outBridge.println("\u001B[33m[System] Still trying to free memory... Attempt $gcAttemptCount (${usedMemory/1024/1024}MB used)\u001B[0m")
                        }

                    } else if (ratio < 0.70) { // 70% threshold (Safe, resume)
                        if (converter.isMemoryPaused) {
                            outBridge.println("\u001B[32m[System] Memory recovered (${usedMemory/1024/1024}MB). Resuming pipeline...\u001B[0m")
                            converter.isMemoryPaused = false
                        }
                    }
                    delay(500) // Polling interval
                }
            }

            var lastProgress = -1.0
            while (!future.isDone) {
                val progress = trackedTask.progress
                if (progress != lastProgress) {
                    val percentage = (progress * 100).toInt()
                    outBridge.println("Conversion Progress: \u001B[33m$percentage%\u001B[0m")
                    lastProgress = progress
                }
                Thread.sleep(100)
            }

            future.get()
            memMonitorJob.cancel()

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