// [file name]: me.voltual.vb.ui.terminal.ConversionWorker.kt
package me.voltual.vb.ui.terminal

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Version
import me.voltual.mcl.MclLevelWriter
import me.voltual.vb.data.ConversionProgressDataStore
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlinx.coroutines.delay

class ConversionWorker(
    val context: Context,
    val params: WorkerParameters
) : RemoteCoroutineWorker(context, params) {

    private val fs = FileSystem.SYSTEM
    private val factory = Iq80DBFactory.factory

    private var currentConverter: WorldConverter? = null
    private var srcDb: org.iq80.leveldb.DB? = null
    private var destDb: org.iq80.leveldb.DB? = null

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

        // 创建共享日志流文件，采用追加写入模式（Append = true）以兼容子进程自杀重建
        val logFile = File(context.filesDir, "conversion_stream.log")
        val fileOutputStream = FileOutputStream(logFile, true)
        val filePrintStream = PrintStream(fileOutputStream, true)

        System.setOut(filePrintStream)
        System.setErr(filePrintStream)

        val isMineclonia = format == "MINECLONIA"
        val targetTypeName = if (isMineclonia) "MINECLONIA" else format.substringBefore("_")
        val targetVersionString = if (isMineclonia) "1.12.2" else format.substringAfter("_").replace("_", ".")
        val encodingType = if (isMineclonia) null else EncodingType.getTypes().find { it.name.equals(targetTypeName, ignoreCase = true) }
        val outputVersion = Version.fromString(targetVersionString)

        val worldId = calculateWorldIdentity(inputPathFile)
        val lastSavedProgressIndex = ConversionProgressDataStore.getProgress(context, worldId)

        val tempDetectConverter = WorldConverter(UUID.randomUUID())
        val readerOptional = EncodingType.findReader(inputPathFile, tempDetectConverter)
        if (!readerOptional.isPresent) {
            System.setOut(oldOut)
            System.setErr(oldErr)
            filePrintStream.close()
            return Result.failure()
        }
        val reader = readerOptional.get()
        val srcFormat = reader.encodingType.name

        val sliceInputDir = File(context.cacheDir, "slice_input")
        val sliceOutputDir = File(context.cacheDir, "slice_output")

        if (lastSavedProgressIndex == 0) {
            deleteDirectory(outputPathFile)
        }
        deleteDirectory(sliceInputDir)
        deleteDirectory(sliceOutputDir)

        val isTargetBedrock = targetTypeName.contains("BEDROCK", ignoreCase = true)

        try {
            if (isTargetBedrock) {
                val finalDbDir = File(outputPathFile, "db")
                finalDbDir.mkdirs()
                
                File(finalDbDir, "LOCK").delete()

                val writeOptions = Options().createIfMissing(true)
                writeOptions.writeBufferSize(8 * 1024 * 1024)
                writeOptions.blockSize(4 * 1024)
                destDb = factory.open(finalDbDir, writeOptions)
            }

            if (srcFormat.contains("JAVA", ignoreCase = true)) {
                val regionDir = File(inputPathFile, "region")
                val mcaFiles = regionDir.listFiles { _, name -> name.endsWith(".mca") } ?: emptyArray()

                for ((index, mcaFile) in mcaFiles.withIndex()) {
                    if (isStopped) break
                    if (index < lastSavedProgressIndex) continue

                    val runtime = Runtime.getRuntime()
                    val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    val maxMem = runtime.maxMemory() / (1024 * 1024)
                    
                    if (usedMem.toDouble() / maxMem.toDouble() > 0.82) {
                        println("\u001B[31m[Worker] Sub-process Heap limit reached (${usedMem}MB/${maxMem}MB). Committing suicide to cleanse memory and trigger Auto-Resume...\u001B[0m")
                        conversionProgressDataStore.saveProgress(context, worldId, index)
                        
                        closeDatabases()
                        System.setOut(oldOut)
                        System.setErr(oldErr)
                        filePrintStream.close()
                        
                        android.os.Process.killProcess(android.os.Process.myPid())
                        return Result.retry()
                    }

                    println("\n[Slicing] Processing Region file ${index + 1}/${mcaFiles.size}: ${mcaFile.name} | Heap: ${usedMem}MB")

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
                    sliceConverter.setProcessLighting(true)
                    sliceConverter.setProcessColumnPreTransform(false)
                    sliceConverter.setThreadCount(threadCount)
                    sliceConverter.setProcessMaps(processMaps)

                    val sliceReader = EncodingType.findReader(sliceInputDir, sliceConverter).get()
                    val sliceWriter = if (isMineclonia) {
                        MclLevelWriter(sliceOutputDir)
                    } else {
                        encodingType!!.createWriter(sliceOutputDir, outputVersion, sliceConverter).get()
                    }

                    sliceConverter.convert(sliceReader, sliceWriter).future().get()

                    try { sliceReader.free() } catch (ignored: Exception) {}
                    try { sliceWriter.free() } catch (ignored: Exception) {}
                    
                    delay(50)

                    mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, destDb, factory)
                    conversionProgressDataStore.saveProgress(context, worldId, index + 1)

                    System.gc()
                    System.runFinalization()
                }
            } else if (srcFormat.contains("BEDROCK", ignoreCase = true)) {
                val srcDbDir = File(inputPathFile, "db")
                File(srcDbDir, "LOCK").delete()

                val dbOptions = Options().createIfMissing(false)
                dbOptions.writeBufferSize(8 * 1024 * 1024)
                dbOptions.blockSize(4 * 1024)

                srcDb = factory.open(srcDbDir, dbOptions)
                
                val regionCoords = mutableListOf<Pair<Int, Int>>()
                val iterator = srcDb!!.iterator()
                iterator.seekToFirst()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    if (isBedrockChunkKey(key)) {
                        val (cx, cz) = getBedrockChunkCoords(key)
                        val pair = Pair(cx shr 5, cz shr 5)
                        if (!regionCoords.contains(pair)) {
                            regionCoords.add(pair)
                        }
                    }
                }
                iterator.close()

                for ((index, region) in regionCoords.withIndex()) {
                    if (isStopped) break
                    if (index < lastSavedProgressIndex) continue

                    val runtime = Runtime.getRuntime()
                    val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    val maxMem = runtime.maxMemory() / (1024 * 1024)
                    
                    if (usedMem.toDouble() / maxMem.toDouble() > 0.82) {
                        println("\u001B[31m[Worker] Sub-process Heap limit reached (${usedMem}MB/${maxMem}MB). Committing suicide to cleanse memory and trigger Auto-Resume...\u001B[0m")
                        conversionProgressDataStore.saveProgress(context, worldId, index)
                        
                        closeDatabases()
                        System.setOut(oldOut)
                        System.setErr(oldErr)
                        filePrintStream.close()
                        
                        android.os.Process.killProcess(android.os.Process.myPid())
                        return Result.retry()
                    }

                    println("\n[Slicing] Processing Bedrock Region ${index + 1}/${regionCoords.size}: (${region.first}, ${region.second}) | Heap: ${usedMem}MB")

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
                    tempDbOptions.blockSize(4 * 1024)
                    val tempDb = factory.open(sliceDbDir, tempDbOptions)
                    
                    val readIterator = srcDb!!.iterator()
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
                    currentConverter = sliceConverter
                    sliceConverter.setProcessItems(true)
                    sliceConverter.setProcessEntities(true)
                    sliceConverter.setProcessBlockEntities(true)
                    sliceConverter.setProcessBiomes(true)
                    sliceConverter.setProcessLighting(true)
                    sliceConverter.setProcessColumnPreTransform(false)
                    sliceConverter.setThreadCount(threadCount)
                    sliceConverter.setProcessMaps(processMaps)

                    val sliceReader = EncodingType.findReader(sliceInputDir, sliceConverter).get()
                    val sliceWriter = if (isMineclonia) {
                        MclLevelWriter(sliceOutputDir)
                    } else {
                        encodingType!!.createWriter(sliceOutputDir, outputVersion, sliceConverter).get()
                    }

                    sliceConverter.convert(sliceReader, sliceWriter).future().get()

                    try { sliceReader.free() } catch (ignored: Exception) {}
                    try { sliceWriter.free() } catch (ignored: Exception) {}
                    
                    delay(50)

                    mergeOutputSlice(sliceOutputDir, outputPathFile, targetTypeName, destDb, factory)
                    conversionProgressDataStore.saveProgress(context, worldId, index + 1)

                    System.gc()
                    System.runFinalization()
                }
            }

            deleteDirectory(sliceInputDir)
            deleteDirectory(sliceOutputDir)

            conversionProgressDataStore.clearProgress(context, worldId)
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        } finally {
            closeDatabases()
            System.setOut(oldOut)
            System.setErr(oldErr)
            filePrintStream.close()
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
            destDb?.close()
        } catch (ignored: Exception) {}
        finally {
            destDb = null
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

    private fun mergeOutputSlice(sliceOutputDir: File, finalOutputDir: File, targetFormat: String, destDb: org.iq80.leveldb.DB?, factory: Iq80DBFactory) {
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
}