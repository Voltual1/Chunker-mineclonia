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
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
import okio.FileSystem
import okio.Path.Companion.toPath
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory

class TerminalViewModel(
    private val context: Context,
    private val conversionSettingsDataStore: ConversionSettingsDataStore
) : ViewModel(), KoinComponent {

    private val logRepository: LogRepository by inject()

    private val _session = MutableStateFlow<TerminalSession?>(null)
    val session = _session.asStateFlow()

    private var isRunning = false
    private val fs = FileSystem.SYSTEM

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

        var srcDb: org.iq80.leveldb.DB? = null
        var destDb: org.iq80.leveldb.DB? = null

        try {
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

            val tempDetectConverter = WorldConverter(UUID.randomUUID())
            val readerOptional = EncodingType.findReader(inputPathFile, tempDetectConverter)
            if (!readerOptional.isPresent) {
                throw IllegalStateException("Failed to detect input world format!")
            }
            val reader = readerOptional.get()
            val srcFormat = reader.encodingType.name
            outBridge.println("Detected format: \u001B[32m$srcFormat\u001B[0m Version: \u001B[32m${reader.version}\u001B[0m")

            val targetTypeName = if (isMineclonia) "MINECLONIA" else args.format.substringBefore("_")
            val targetVersionString = if (isMineclonia) "1.12.2" else args.format.substringAfter("_").replace("_", ".")
            val encodingType = if (isMineclonia) null else EncodingType.getTypes().find { it.name.equals(targetTypeName, ignoreCase = true) }
            val outputVersion = if (isMineclonia) Version.fromString("1.12.2") else Version.fromString(targetVersionString)

            val sliceInputDir = File(context.cacheDir, "slice_input")
            val sliceOutputDir = File(context.cacheDir, "slice_output")
            
            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)
            deleteDirectory(outputPathFile)

            val isTargetBedrock = targetTypeName.contains("BEDROCK", ignoreCase = true)
            val factory = Iq80DBFactory.factory

            // 在最外层打开目标数据库连接以重用并减小开销
            if (isTargetBedrock) {
                val finalDbDir = File(outputPathFile, "db")
                val writeOptions = Options().createIfMissing(true)
                writeOptions.writeBufferSize(8 * 1024 * 1024) // 限制为 8MB 缓存
                writeOptions.blockSize(4 * 1024)
                destDb = factory.open(finalDbDir, writeOptions)
            }

            if (srcFormat.contains("JAVA", ignoreCase = true)) {
                val regionDir = File(inputPathFile, "region")
                val mcaFiles = regionDir.listFiles { _, name -> name.endsWith(".mca") } ?: emptyArray()
                
                outBridge.println("Java Save detected. Slicing world into ${mcaFiles.size} region files...")

                for ((index, mcaFile) in mcaFiles.withIndex()) {
                    if (!isRunning) break
                    
                    val runtime = Runtime.getRuntime()
                    val preMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    outBridge.println("\n[Slicing] Processing Region file ${index + 1}/${mcaFiles.size}: ${mcaFile.name} | Heap: ${preMem}MB")

                    deleteDirectory(sliceInputDir)
                    deleteDirectory(sliceOutputDir)
                    sliceInputDir.mkdirs()
                    sliceOutputDir.mkdirs()

                    val levelDat = File(inputPathFile, "level.dat")
                    if (levelDat.exists()) {
                        copyFile(levelDat, File(sliceInputDir, "level.dat"))
                    }

                    copyFile(mcaFile, File(sliceInputDir, "region/${mcaFile.name}"))
                    
                    val entitiesFile = File(inputPathFile, "entities/${mcaFile.name}")
                    if (entitiesFile.exists()) {
                        copyFile(entitiesFile, File(sliceInputDir, "entities/${mcaFile.name}"))
                    }
                    val poiFile = File(inputPathFile, "poi/${mcaFile.name}")
                    if (poiFile.exists()) {
                        copyFile(poiFile, File(sliceInputDir, "poi/${mcaFile.name}"))
                    }

                    val sliceConverter = WorldConverter(UUID.randomUUID())
                    sliceConverter.setProcessItems(true)
                    sliceConverter.setProcessEntities(true)
                    sliceConverter.setProcessBlockEntities(true)
                    sliceConverter.setProcessBiomes(true)
                    sliceConverter.setProcessLighting(true)
                    sliceConverter.setProcessColumnPreTransform(false)
                    sliceConverter.setThreadCount(userThreadCount)
                    sliceConverter.setProcessMaps(userProcessMaps)

                    val sliceReader = EncodingType.findReader(sliceInputDir, sliceConverter).get()
                    val sliceWriter = if (isMineclonia) {
                        MclLevelWriter(sliceOutputDir)
                    } else {
                        encodingType!!.createWriter(sliceOutputDir, outputVersion, sliceConverter).get()
                    }

                    val trackedTask = sliceConverter.convert(sliceReader, sliceWriter)
                    trackedTask.future().get()

                    mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, destDb)
                    
                    System.gc()
                    System.runFinalization()
                    val postMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    outBridge.println("[Memory] GC completed. Heap: ${postMem}MB")
                }
                isSuccess = true
            } else if (srcFormat.contains("BEDROCK", ignoreCase = true)) {
                val srcDbDir = File(inputPathFile, "db")
                val dbOptions = Options().createIfMissing(false)
                dbOptions.writeBufferSize(8 * 1024 * 1024)
                dbOptions.blockSize(4 * 1024)
                
                outBridge.println("Bedrock Save detected. Analysing database keys...")
                srcDb = factory.open(srcDbDir, dbOptions)
                
                val regionCoords = mutableSetOf<Pair<Int, Int>>()
                val iterator = srcDb.iterator()
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    if (isBedrockChunkKey(key)) {
                        val (cx, cz) = getBedrockChunkCoords(key)
                        regionCoords.add(Pair(cx shr 5, cz shr 5))
                    }
                }
                iterator.close()

                outBridge.println("Slicing Bedrock database into ${regionCoords.size} region segments...")

                for ((index, region) in regionCoords.withIndex()) {
                    if (!isRunning) break
                    
                    val runtime = Runtime.getRuntime()
                    val preMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    outBridge.println("\n[Slicing] Processing Bedrock Region ${index + 1}/${regionCoords.size}: (${region.first}, ${region.second}) | Heap: ${preMem}MB")

                    deleteDirectory(sliceInputDir)
                    deleteDirectory(sliceOutputDir)
                    sliceInputDir.mkdirs()
                    sliceOutputDir.mkdirs()

                    val levelDat = File(inputPathFile, "level.dat")
                    if (levelDat.exists()) {
                        copyFile(levelDat, File(sliceInputDir, "level.dat"))
                    }

                    // 仅向临时数据库写入匹配当前坐标的块数据
                    val sliceDbDir = File(sliceInputDir, "db")
                    sliceDbDir.mkdirs()
                    val tempDbOptions = Options().createIfMissing(true)
                    tempDbOptions.writeBufferSize(2 * 1024 * 1024)
                    tempDbOptions.blockSize(4 * 1024)
                    val tempDb = factory.open(sliceDbDir, tempDbOptions)
                    
                    val readIterator = srcDb.iterator()
                    readIterator.seekToFirst()
                    while (readIterator.hasNext()) {
                        val entry = readIterator.next()
                        val key = entry.key
                        if (isBedrockChunkKey(key)) {
                            val (cx, cz) = getBedrockChunkCoords(key)
                            if ((cx shr 5) == region.first && (cz shr 5) == region.second) {
                                tempDb.put(key, entry.value)
                            }
                        } else {
                            tempDb.put(key, entry.value)
                        }
                    }
                    readIterator.close()
                    tempDb.close()

                    val sliceConverter = WorldConverter(UUID.randomUUID())
                    sliceConverter.setProcessItems(true)
                    sliceConverter.setProcessEntities(true)
                    sliceConverter.setProcessBlockEntities(true)
                    sliceConverter.setProcessBiomes(true)
                    sliceConverter.setProcessLighting(true)
                    sliceConverter.setProcessColumnPreTransform(false)
                    sliceConverter.setThreadCount(userThreadCount)
                    sliceConverter.setProcessMaps(userProcessMaps)

                    val sliceReader = EncodingType.findReader(sliceInputDir, sliceConverter).get()
                    val sliceWriter = if (isMineclonia) {
                        MclLevelWriter(sliceOutputDir)
                    } else {
                        encodingType!!.createWriter(sliceOutputDir, outputVersion, sliceConverter).get()
                    }

                    val trackedTask = sliceConverter.convert(sliceReader, sliceWriter)
                    trackedTask.future().get()

                    mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, destDb)
                    
                    System.gc()
                    System.runFinalization()
                    val postMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    outBridge.println("[Memory] GC completed. Heap: ${postMem}MB")
                }
                isSuccess = true
            }

            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)

            if (isSuccess) {
                outBridge.println("\n\u001B[1;32m[SUCCESS] Sliced conversion completed successfully!\u001B[0m")
            }

        } catch (e: Exception) {
            outBridge.println("\n\u001B[1;31m[FATAL ERROR] Sliced conversion failed!\u001B[0m")
            e.printStackTrace(outBridge)
        } finally {
            // 确保最外层打开的数据库被完全关闭
            try { srcDb?.close() } catch (ignored: Exception) {}
            try { destDb?.close() } catch (ignored: Exception) {}

            System.setOut(oldOut)
            System.setErr(oldErr)
            isRunning = false
            session.finishIfRunning()

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

    private fun mergeOutputSlice(sliceOutputDir: File, finalOutputDir: File, targetFormat: String, destDb: org.iq80.leveldb.DB?) {
        if (targetFormat.contains("JAVA", ignoreCase = true) || targetFormat.equals("MINECLONIA", ignoreCase = true)) {
            val subFolders = listOf("region", "poi", "entities")
            for (folderName in subFolders) {
                val srcFolder = File(sliceOutputDir, folderName)
                if (srcFolder.exists()) {
                    val destFolder = File(finalOutputDir, folderName)
                    destFolder.mkdirs()
                    srcFolder.listFiles()?.forEach { file ->
                        copyFile(file, File(destFolder, file.name))
                    }
                }
            }
            val levelDat = File(sliceOutputDir, "level.dat")
            if (levelDat.exists()) {
                copyFile(levelDat, File(finalOutputDir, "level.dat"))
            }
        } else if (targetFormat.contains("BEDROCK", ignoreCase = true)) {
            val sliceDbDir = File(sliceOutputDir, "db")
            if (sliceDbDir.exists() && destDb != null) {
                val writeOptions = Options().createIfMissing(true)
                writeOptions.writeBufferSize(2 * 1024 * 1024)
                writeOptions.blockSize(4 * 1024)
                
                val srcDb = factory.open(sliceDbDir, writeOptions)
                val iterator = srcDb.iterator()
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    destDb.put(entry.key, entry.value)
                }
                iterator.close()
                srcDb.close()
            }
            val levelDat = File(sliceOutputDir, "level.dat")
            if (levelDat.exists()) {
                copyFile(levelDat, File(finalOutputDir, "level.dat"))
            }
        }
    }

    private fun isBedrockChunkKey(key: ByteArray): Boolean {
        val len = key.size
        if (len != 9 && len != 10 && len != 13 && len != 14) return false
        val keyStr = String(key, StandardCharsets.UTF_8)
        if (keyStr.startsWith("map_")) return false
        if (keyStr == "~local_player") return false
        if (keyStr == "portals") return false
        return true
    }

    private fun getBedrockChunkCoords(key: ByteArray): Pair<Int, Int> {
        val buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN)
        val x = buffer.int
        val z = buffer.int
        return Pair(x, z)
    }

    private fun copyFile(src: File, dest: File) {
        val srcPath = src.absolutePath.toPath()
        val destPath = dest.absolutePath.toPath()
        fs.createDirectories(destPath.parent!!)
        fs.copy(srcPath, destPath)
    }

    private fun deleteDirectory(dir: File) {
        val path = dir.absolutePath.toPath()
        if (fs.exists(path)) {
            fs.deleteRecursively(path)
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