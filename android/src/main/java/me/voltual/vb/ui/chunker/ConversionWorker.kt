package me.voltual.vb.ui.chunker

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Version
import me.voltual.vb.data.ConversionProgressDataStore
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import me.voltual.mc2mt.MC2MTLib // 引入 Rust 极速转换通道

class ConversionWorker(
    val context: Context,
    val params: WorkerParameters
) : RemoteCoroutineWorker(context, params) {

    private val fs = FileSystem.SYSTEM
    private val factory = Iq80DBFactory.factory

    private var currentConverter: WorldConverter? = null
    private var srcDb: org.iq80.leveldb.DB? = null
    private var targetDb: org.iq80.leveldb.DB? = null
    private var sliceDb: org.iq80.leveldb.DB? = null

    @Volatile
    private var isMerging = false

    @Volatile
    private var isSelfKilling = false

    override suspend fun doRemoteWork(): Result {
        val inputPath = inputData.getString("inputPath") ?: return Result.failure()
        val outputPath = inputData.getString("outputPath") ?: return Result.failure()
        val format = inputData.getString("format") ?: return Result.failure()
        val threadCount = inputData.getInt("threadCount", 8)
        val processMaps = inputData.getBoolean("processMaps", true)

        val inputPathFile = File(inputPath)
        val outputPathFile = File(outputPath)

        val oldOut = System.`out`
        val oldErr = System.err

        // 创建临时重定向文件日志，终端视图会通过 TailJob 读取并实时高亮输出到屏幕上
        val logFile = File(context.cacheDir, "slice_log.txt")
        logFile.parentFile?.mkdirs()
        val fileOutputStream = FileOutputStream(logFile, true)
        val slicePrintStream = PrintStream(fileOutputStream, true)

        System.setOut(slicePrintStream)
        System.setErr(slicePrintStream)

        // 内存看门狗守护线程：防止 JVM 堆内存暴涨导致 OOM 崩溃
        val memoryMonitorThread = Thread {
            val runtime = Runtime.getRuntime()
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(100)
                    val usedMem = runtime.totalMemory() - runtime.freeMemory()
                    val maxMem = runtime.maxMemory()
                    val ratio = usedMem.toDouble() / maxMem.toDouble()

                    if (ratio > 0.80) {
                        if (isMerging) {
                            var waitCount = 0
                            while (isMerging && waitCount < 15) {
                                Thread.sleep(20)
                                waitCount++
                            }
                        }

                        System.out.println("\u001B[31m[Memory Monitor] JVM Heap critically high (${usedMem / 1024 / 1024}MB / ${maxMem / 1024 / 1024}MB). Killing process immediately to prevent JVM OOM...\u001B[0m")
                        
                        isSelfKilling = true
                        closeDatabases()
                        slicePrintStream.close()
                        System.setOut(oldOut)
                        System.setErr(oldErr)

                        android.os.Process.killProcess(android.os.Process.myPid())
                        break
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        memoryMonitorThread.isDaemon = true
        memoryMonitorThread.start()

        val targetTypeName = format.substringBefore("_")
        val targetVersionString = format.substringAfter("_").replace("_", ".")
        val encodingType = EncodingType.getTypes().find { it.name.equals(targetTypeName, ignoreCase = true) }
        val outputVersion = Version.fromString(targetVersionString)

        val worldId = calculateWorldIdentity(inputPathFile)
        val lastSavedProgressIndex = ConversionProgressDataStore.getProgress(context, worldId)

        // 检测输入存档格式
        val tempDetectConverter = WorldConverter(UUID.randomUUID())
        val readerOptional = EncodingType.findReader(inputPathFile, tempDetectConverter)
        if (!readerOptional.isPresent) {
            memoryMonitorThread.interrupt()
            slicePrintStream.close()
            System.setOut(oldOut)
            System.setErr(oldErr)
            return Result.failure()
        }
        val reader = readerOptional.get()
        val srcFormat = reader.encodingType.name

        // =========================================================================
        // 核心特判：如果目标格式为 MINECLONIA，则直接切入极速物理 Rust 管道！
        // =========================================================================
        if (targetTypeName.contains("MINECLONIA", ignoreCase = true) || format.contains("MINECLONIA", ignoreCase = true)) {
            System.out.println("\u001B[1;36m[System] Target format 'Mineclonia' detected. Redirecting to high-performance Rust Rayon engine...\u001B[0m")
            
            var success = false
            try {
                // 调用原生 JNI，传入 Rust 转换引擎
                success = MC2MTLib.convertMap(inputPath, outputPath, object : MC2MTLib.ConversionCallback {
                    override fun onProgress(groupsDone: Long, totalGroups: Long, blocksDone: Long) {
                        // 在虚拟终端上打印战术风的进度指示条
                        System.out.print("\r\u001B[1;32m[Rust Pipeline]\u001B[0m Progress: [$groupsDone/$totalGroups] regions converted | Saved \u001B[1;33m$blocksDone\u001B[0m blocks to map.sqlite")
                    }
                })
            } catch (e: Exception) {
                System.err.println("[Rust Bridge Exception] " + e.message)
                e.printStackTrace()
            }

            memoryMonitorThread.interrupt()
            slicePrintStream.close()
            System.setOut(oldOut)
            System.setErr(oldErr)

            return if (success) {
                System.out.println("\n\u001B[1;32m[System] Rust engine completed conversion successfully!\u001B[0m")
                Result.success()
            } else {
                System.err.println("\n[System] Rust engine conversion failed.")
                Result.failure()
            }
        }

        // =========================================================================
        // 降级回退：非 Mineclonia 的通用转换，继续走 Chunker 分片 Java 转换流
        // =========================================================================
        val workerId = id.toString()
        val sliceInputDir = File(context.cacheDir, "slice_input_$workerId")
        val sliceOutputDir = File(context.cacheDir, "slice_output_$workerId")

        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && (file.name.startsWith("slice_input_") || file.name.startsWith("slice_output_"))) {
                if (file.name != "slice_input_$workerId" && file.name != "slice_output_$workerId") {
                    deleteDirectory(file)
                }
            }
        }

        if (lastSavedProgressIndex == 0) {
            deleteDirectory(outputPathFile)
        }
        deleteDirectory(sliceInputDir)
        deleteDirectory(sliceOutputDir)

        try {
            if (srcFormat.contains("JAVA", ignoreCase = true)) {
                processJavaWorld(
                    inputPathFile = inputPathFile,
                    outputPathFile = outputPathFile,
                    sliceInputDir = sliceInputDir,
                    sliceOutputDir = sliceOutputDir,
                    lastSavedProgressIndex = lastSavedProgressIndex,
                    threadCount = threadCount,
                    processMaps = processMaps,
                    encodingType = encodingType,
                    outputVersion = outputVersion,
                    targetTypeName = targetTypeName,
                    worldId = worldId
                )
            } else if (srcFormat.contains("BEDROCK", ignoreCase = true)) {
                processBedrockWorld(
                    inputPathFile = inputPathFile,
                    outputPathFile = outputPathFile,
                    sliceInputDir = sliceInputDir,
                    sliceOutputDir = sliceOutputDir,
                    lastSavedProgressIndex = lastSavedProgressIndex,
                    threadCount = threadCount,
                    processMaps = processMaps,
                    encodingType = encodingType,
                    outputVersion = outputVersion,
                    targetTypeName = targetTypeName,
                    worldId = worldId
                )
            }

            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)
            ConversionProgressDataStore.clearProgress(context, worldId)
            
            memoryMonitorThread.interrupt()
            return Result.success()

        } catch (e: CancellationException) {
            memoryMonitorThread.interrupt()
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            memoryMonitorThread.interrupt()
            if (isSelfKilling) {
                return Result.retry()
            }
            return Result.failure()
        } finally {
            closeDatabases()
            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)
            slicePrintStream.close()
            System.setOut(oldOut)
            System.setErr(oldErr)
        }
    }

    private suspend fun processJavaWorld(
        inputPathFile: File,
        outputPathFile: File,
        sliceInputDir: File,
        sliceOutputDir: File,
        lastSavedProgressIndex: Int,
        threadCount: Int,
        processMaps: Boolean,
        encodingType: EncodingType?,
        outputVersion: Version,
        targetTypeName: String,
        worldId: String
    ) {
        val regionDir = File(inputPathFile, "region")
        val mcaFiles = regionDir.listFiles { _, name -> name.endsWith(".mca") } ?: emptyArray()

        for ((index, mcaFile) in mcaFiles.withIndex()) {
            if (isStopped) break
            if (index < lastSavedProgressIndex) continue

            val runtime = Runtime.getRuntime()
            val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            
            ConversionProgressDataStore.saveProgress(context, worldId, index)
            System.out.println("\n[Slicing] Processing Region file ${index + 1}/${mcaFiles.size}: ${mcaFile.name} | Heap: ${usedMem}MB")

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
            currentConverter = sliceConverter
            sliceConverter.setProcessItems(true)
            sliceConverter.setProcessEntities(true)
            sliceConverter.setProcessBlockEntities(true)
            sliceConverter.setProcessBiomes(true)
            sliceConverter.setProcessLighting(false)
            sliceConverter.setProcessColumnPreTransform(false)
            sliceConverter.setThreadCount(threadCount)
            sliceConverter.setProcessMaps(processMaps)

            val sliceReaderOpt = EncodingType.findReader(sliceInputDir, sliceConverter)
            if (!sliceReaderOpt.isPresent) throw IllegalStateException("Reader not found for slice.")
            val sliceReader = sliceReaderOpt.get()
            
            val sliceWriterOpt = encodingType!!.createWriter(sliceOutputDir, outputVersion, sliceConverter)
            if (!sliceWriterOpt.isPresent) {
                throw IllegalStateException("Failed to create writer.")
            }
            val sliceWriter = sliceWriterOpt.get()

            val future = sliceConverter.convert(sliceReader, sliceWriter).future()
            while (!future.isDone) {
                delay(250)
            }
            future.get()

            try { sliceReader.free() } catch (ignored: Exception) {}
            try { sliceWriter.free() } catch (ignored: Exception) {}
            
            delay(50)

            mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, factory)
            ConversionProgressDataStore.saveProgress(context, worldId, index + 1)

            System.gc()
            System.runFinalization()
        }
    }

    private suspend fun processBedrockWorld(
        inputPathFile: File,
        outputPathFile: File,
        sliceInputDir: File,
        sliceOutputDir: File,
        lastSavedProgressIndex: Int, 
        threadCount: Int,
        processMaps: Boolean,
        encodingType: EncodingType?,
        outputVersion: Version,
        targetTypeName: String,
        worldId: String
    ) {
        val srcDbDir = File(inputPathFile, "db")
        File(srcDbDir, "LOCK").delete()

        val dbOptions = Options().createIfMissing(false)
        dbOptions.writeBufferSize(4 * 1024 * 1024) 
        dbOptions.blockSize(4 * 1024)

        srcDb = factory.open(srcDbDir, dbOptions)

        var currentSliceIndex = 0
        var lastProcessedKey: ByteArray? = null
        var hasMoreData = true
        
        val CHUNK_LIMIT_PER_SLICE = 256

        while (hasMoreData) {
            if (isStopped || isSelfKilling) break

            if (currentSliceIndex < lastSavedProgressIndex) {
                srcDb!!.iterator().use { skipIterator ->
                    if (lastProcessedKey != null) {
                        skipIterator.seek(lastProcessedKey)
                        if (skipIterator.hasNext()) skipIterator.next()
                    } else {
                        skipIterator.seekToFirst()
                    }
                    var skipCount = 0
                    while (skipIterator.hasNext() && skipCount < CHUNK_LIMIT_PER_SLICE) {
                        val entry = skipIterator.next()
                        if (isBedrockChunkKey(entry.key)) {
                            skipCount++
                        }
                        lastProcessedKey = entry.key
                    }
                    if (!skipIterator.hasNext()) {
                        hasMoreData = false
                    }
                }
                currentSliceIndex++
                continue
            }

            val runtime = Runtime.getRuntime()
            val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            System.out.println("\n[Slicing] Slice #$currentSliceIndex | Heap: ${usedMem}MB")

            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)
            sliceInputDir.mkdirs()
            sliceOutputDir.mkdirs()

            val levelDat = File(inputPathFile, "level.dat")
            if (levelDat.exists()) {
                copyFile(levelDat, File(sliceInputDir, "level.dat"))
            }

            val sliceDbDir = File(sliceInputDir, "db")
            sliceDbDir.mkdirs()
            File(sliceDbDir, "LOCK").delete()

            val tempDbOptions = Options().createIfMissing(true)
            tempDbOptions.writeBufferSize(2 * 1024 * 1024)
            val tempDb = factory.open(sliceDbDir, tempDbOptions)

            var loadedChunkCount = 0
            var nextBoundaryKey: ByteArray? = null

            srcDb!!.iterator().use { readIterator ->
                if (lastProcessedKey != null) {
                    readIterator.seek(lastProcessedKey)
                    if (readIterator.hasNext()) readIterator.next()
                } else {
                    readIterator.seekToFirst()
                }

                while (readIterator.hasNext() && loadedChunkCount < CHUNK_LIMIT_PER_SLICE) {
                    if (isStopped || isSelfKilling) break
                    val entry = readIterator.next()
                    val key = entry.key
                    
                    if (isBedrockChunkKey(key)) {
                        tempDb.put(key, entry.value)
                        loadedChunkCount++
                    } else {
                        tempDb.put(key, entry.value)
                    }
                    nextBoundaryKey = key
                }
                
                if (!readIterator.hasNext()) {
                    hasMoreData = false
                }
            }
            tempDb.close()

            if (loadedChunkCount == 0) {
                break
            }

            lastProcessedKey = nextBoundaryKey
            ConversionProgressDataStore.saveProgress(context, worldId, currentSliceIndex)

            val sliceConverter = WorldConverter(UUID.randomUUID())
            currentConverter = sliceConverter
            sliceConverter.setProcessItems(true)
            sliceConverter.setProcessEntities(true)
            sliceConverter.setProcessBlockEntities(true)
            sliceConverter.setProcessBiomes(true)
            sliceConverter.setProcessLighting(false)
            sliceConverter.setProcessColumnPreTransform(false)
            sliceConverter.setThreadCount(threadCount)
            sliceConverter.setProcessMaps(processMaps)

            val sliceReaderOpt = EncodingType.findReader(sliceInputDir, sliceConverter)
            if (!sliceReaderOpt.isPresent) throw IllegalStateException("Reader not found for slice.")
            val sliceReader = sliceReaderOpt.get()

            val sliceWriterOpt = encodingType!!.createWriter(sliceOutputDir, outputVersion, sliceConverter)
            if (!sliceWriterOpt.isPresent) throw IllegalStateException("Failed to create writer.")
            val sliceWriter = sliceWriterOpt.get()

            val future = sliceConverter.convert(sliceReader, sliceWriter).future()
            while (!future.isDone) {
                delay(250)
            }
            future.get()

            try { sliceReader.free() } catch (_: Exception) {}
            try { sliceWriter.free() } catch (_: Exception) {}
            
            delay(50)

            mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, factory)
            
            currentSliceIndex++
            ConversionProgressDataStore.saveProgress(context, worldId, currentSliceIndex)

            System.gc()
            System.runFinalization()
        }
    }

    private fun closeDatabases() {
        currentConverter?.cancel(null)
        try {
            srcDb?.close()
        } catch (ignored: Exception) {}
        finally {
            srcDb = null
        }
        try {
            targetDb?.close()
        } catch (ignored: Exception) {}
        finally {
            targetDb = null
        }
        try {
            sliceDb?.close()
        } catch (ignored: Exception) {}
        finally {
            sliceDb = null
        }
    }

    private fun calculateWorldIdentity(inputDir: File): String {
        val iconPng = File(inputDir, "icon.png")
        val iconJpeg = File(inputDir, "world_icon.jpeg")
        val targetFile = when {
            iconPng.exists() -> iconPng
            iconJpeg.exists() -> iconJpeg
            else -> null
        }
        return if (targetFile != null) {
            try {
                val bytes = targetFile.readBytes()
                val md = MessageDigest.getInstance("MD5")
                val digest = md.digest(bytes)
                digest.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                getFallbackIdentity(inputDir)
            }
        } else {
            getFallbackIdentity(inputDir)
        }
    }

    private fun getFallbackIdentity(inputDir: File): String {
        val levelDat = File(inputDir, "level.dat")
        val baseString = inputDir.absolutePath + "_" + (if (levelDat.exists()) levelDat.lastModified() else 0L)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(baseString.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun mergeOutputSlice(sliceOutputDir: File, finalOutputDir: File, targetFormat: String, factory: Iq80DBFactory) {
        if (targetFormat.contains("JAVA", ignoreCase = true)) {
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
            val finalDbDir = File(finalOutputDir, "db")
            if (sliceDbDir.exists()) {
                isMerging = true
                try {
                    File(finalDbDir, "LOCK").delete()
                    
                    val writeOptions = Options().createIfMissing(true)
                    writeOptions.writeBufferSize(2 * 1024 * 1024)
                    writeOptions.blockSize(4 * 1024)
                    
                    targetDb = factory.open(finalDbDir, writeOptions)
                    sliceDb = factory.open(sliceDbDir, writeOptions)
                    
                    val iterator = sliceDb!!.iterator()
                    iterator.seekToFirst()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        targetDb!!.put(entry.key, entry.value)
                    }
                    iterator.close()
                } finally {
                    try { sliceDb?.close() } catch (ignored: Exception) {}
                    sliceDb = null
                    try { targetDb?.close() } catch (ignored: Exception) {}
                    targetDb = null
                    isMerging = false
                }
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

    companion object {
        init {
            System.setProperty("leveldb.mmap", "false")
        }
    }
}