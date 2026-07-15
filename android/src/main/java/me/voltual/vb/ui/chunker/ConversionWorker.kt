package me.voltual.vb.ui.chunker

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Version
import me.voltual.vb.core.database.AppDatabase
import me.voltual.vb.core.database.repository.ConversionTaskRepository
import me.voltual.vb.data.model.ConversionManifest
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

    private lateinit var conversionTaskRepository: ConversionTaskRepository

    override suspend fun doRemoteWork(): Result {
        // Initialize Room DB directly to avoid cross-process DI issues
        val db = AppDatabase.getDatabase(context)
        conversionTaskRepository = ConversionTaskRepository(db.conversionTaskDao())

        val inputPath = inputData.getString("inputPath") ?: return Result.failure()
        val outputPath = inputData.getString("outputPath") ?: return Result.failure()
        val format = inputData.getString("format") ?: return Result.failure()
        val threadCount = inputData.getInt("threadCount", 8)
        val processMaps = inputData.getBoolean("processMaps", true)

        val inputPathFile = File(inputPath)
        val outputPathFile = File(outputPath)

        val oldOut = System.`out`
        val oldErr = System.err

        val logFile = File(context.cacheDir, "slice_log.txt")
        logFile.parentFile?.mkdirs()
        val fileOutputStream = FileOutputStream(logFile, true)
        val slicePrintStream = PrintStream(fileOutputStream, true)

        System.setOut(slicePrintStream)
        System.setErr(slicePrintStream)

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

                        System.out.println("\u001B[31m[Memory Monitor] JVM Heap critically high. Killing process immediately to prevent JVM OOM...\u001B[0m")
                        
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
        
        // Load Manifest
        var manifest = conversionTaskRepository.getManifest(worldId) ?: ConversionManifest(
            worldId = worldId, inputPath = inputPath, outputPath = outputPath, format = format, progressIndex = 0
        )
        val lastSavedProgressIndex = manifest.progressIndex

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
                    manifest = manifest,
                    threadCount = threadCount,
                    processMaps = processMaps,
                    encodingType = encodingType,
                    outputVersion = outputVersion,
                    targetTypeName = targetTypeName
                )
            } else if (srcFormat.contains("BEDROCK", ignoreCase = true)) {
                processBedrockWorld(
                    inputPathFile = inputPathFile,
                    outputPathFile = outputPathFile,
                    sliceInputDir = sliceInputDir,
                    sliceOutputDir = sliceOutputDir,
                    manifest = manifest,
                    threadCount = threadCount,
                    processMaps = processMaps,
                    encodingType = encodingType,
                    outputVersion = outputVersion,
                    targetTypeName = targetTypeName
                )
            }

            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)
            
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
        inputPathFile: File, outputPathFile: File, sliceInputDir: File, sliceOutputDir: File,
        manifest: ConversionManifest, threadCount: Int, processMaps: Boolean, encodingType: EncodingType?,
        outputVersion: Version, targetTypeName: String
    ) {
        var currentManifest = manifest
        val regionDir = File(inputPathFile, "region")
        val mcaFiles = regionDir.listFiles { _, name -> name.endsWith(".mca") } ?: emptyArray()

        for ((index, mcaFile) in mcaFiles.withIndex()) {
            if (isStopped) break
            if (index < currentManifest.progressIndex) continue

            val runtime = Runtime.getRuntime()
            val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            
            // Save state
            currentManifest = currentManifest.copy(progressIndex = index)
            conversionTaskRepository.saveManifest(currentManifest)
            
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
            
            val sliceWriter = if (targetTypeName.equals("MINECLONIA", ignoreCase = true)) {
                me.voltual.mcl.writer.MclLevelWriter(outputPathFile)
            } else {
                val sliceWriterOpt = encodingType?.createWriter(sliceOutputDir, outputVersion, sliceConverter)
                    ?: throw IllegalStateException("Failed to create writer.")
                if (!sliceWriterOpt.isPresent) throw IllegalStateException("Failed to create writer.")
                sliceWriterOpt.get()
            }

            val future = sliceConverter.convert(sliceReader, sliceWriter).future()
            while (!future.isDone) {
                delay(250)
            }
            future.get()

            try { sliceReader.free() } catch (ignored: Exception) {}
            try { sliceWriter.free() } catch (ignored: Exception) {}
            
            delay(50)

            mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, factory)
            
            currentManifest = currentManifest.copy(progressIndex = index + 1)
            conversionTaskRepository.saveManifest(currentManifest)

            System.gc()
            System.runFinalization()
        }
    }

    private suspend fun processBedrockWorld(
        inputPathFile: File, outputPathFile: File, sliceInputDir: File, sliceOutputDir: File,
        manifest: ConversionManifest, threadCount: Int, processMaps: Boolean, encodingType: EncodingType?,
        outputVersion: Version, targetTypeName: String
    ) {
        var currentManifest = manifest
        val srcDbDir = File(inputPathFile, "db")
        File(srcDbDir, "LOCK").delete()

        val dbOptions = Options().createIfMissing(false).writeBufferSize(4 * 1024 * 1024).blockSize(4 * 1024)
        srcDb = factory.open(srcDbDir, dbOptions)

        System.out.println("\n[Slicing] Scanning Bedrock database to partition regions...")
        val regionCoords = mutableSetOf<Pair<Int, Int>>()
        val readIterator = srcDb!!.iterator()
        readIterator.seekToFirst()
        while (readIterator.hasNext() && !isStopped && !isSelfKilling) {
            val entry = readIterator.next()
            if (isBedrockChunkKey(entry.key)) {
                val (cx, cz) = getBedrockChunkCoords(entry.key)
                regionCoords.add(Pair(cx shr 5, cz shr 5)) // 转换为 Region 坐标
            }
        }
        readIterator.close()

        val regionList = regionCoords.toList()
        System.out.println("[Slicing] Found ${regionList.size} regions to process safely.")

        for ((index, region) in regionList.withIndex()) {
            if (isStopped || isSelfKilling) break
            if (index < currentManifest.progressIndex) continue

            val runtime = Runtime.getRuntime()
            val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            System.out.println("\n[Slicing] Processing Bedrock Region ${index + 1}/${regionList.size}: (${region.first}, ${region.second}) | Heap: ${usedMem}MB")

            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)
            sliceInputDir.mkdirs()
            sliceOutputDir.mkdirs()

            val levelDat = File(inputPathFile, "level.dat")
            if (levelDat.exists()) copyFile(levelDat, File(sliceInputDir, "level.dat"))

            val sliceDbDir = File(sliceInputDir, "db")
            sliceDbDir.mkdirs()
            File(sliceDbDir, "LOCK").delete()

            val tempDbOptions = Options().createIfMissing(true).writeBufferSize(2 * 1024 * 1024)
            val tempDb = factory.open(sliceDbDir, tempDbOptions)

            val sliceIterator = srcDb!!.iterator()
            sliceIterator.seekToFirst()
            var chunkCount = 0
            while (sliceIterator.hasNext() && !isStopped && !isSelfKilling) {
                val entry = sliceIterator.next()
                if (isBedrockChunkKey(entry.key)) {
                    val (cx, cz) = getBedrockChunkCoords(entry.key)
                    if ((cx shr 5) == region.first && (cz shr 5) == region.second) {
                        tempDb.put(entry.key, entry.value)
                        chunkCount++
                    }
                } else {
                    // 全局元数据必须随带写入
                    tempDb.put(entry.key, entry.value)
                }
            }
            sliceIterator.close()
            tempDb.close()

            if (chunkCount == 0) continue

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

            val sliceWriter = if (targetTypeName.equals("MINECLONIA", ignoreCase = true)) {
                me.voltual.mcl.writer.MclLevelWriter(outputPathFile)
            } else {
                val sliceWriterOpt = encodingType?.createWriter(sliceOutputDir, outputVersion, sliceConverter)
                    ?: throw IllegalStateException("Failed to create writer.")
                if (!sliceWriterOpt.isPresent) throw IllegalStateException("Failed to create writer.")
                sliceWriterOpt.get()
            }

            val future = sliceConverter.convert(sliceReader, sliceWriter).future()
            while (!future.isDone) {
                delay(250)
            }
            future.get()

            try { sliceReader.free() } catch (_: Exception) {}
            try { sliceWriter.free() } catch (_: Exception) {}
            
            delay(50)

            mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, factory)
            
            // 安全保存持久化索引以供恢复
            currentManifest = currentManifest.copy(progressIndex = index + 1)
            conversionTaskRepository.saveManifest(currentManifest)

            System.gc()
            System.runFinalization()
        }
    }

    private fun getBedrockChunkCoords(key: ByteArray): Pair<Int, Int> {
        val buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN)
        val x = buffer.int
        val z = buffer.int
        return Pair(x, z)
    }

    private fun closeDatabases() {
        currentConverter?.cancel(null)
        try { srcDb?.close() } catch (ignored: Exception) {} finally { srcDb = null }
        try { targetDb?.close() } catch (ignored: Exception) {} finally { targetDb = null }
        try { sliceDb?.close() } catch (ignored: Exception) {} finally { sliceDb = null }
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
        if (targetFormat.equals("MINECLONIA", ignoreCase = true)) {
            return
        }

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
        
        // 确保不会因为目标存在而引发未捕获的 OKIO 覆写安全隐患
        if (fs.exists(destPath)) {
            fs.delete(destPath)
        }
        
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