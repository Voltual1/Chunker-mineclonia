package me.voltual.vb.ui.chunker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Version
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

class DirectConversionWorker(
    val context: Context,
    val params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var currentConverter: WorldConverter? = null

    override suspend fun doWork(): Result {
        val inputPath = inputData.getString("inputPath") ?: return Result.failure()
        val outputPath = inputData.getString("outputPath") ?: return Result.failure()
        val format = inputData.getString("format") ?: return Result.failure()
        val threadCount = inputData.getInt("threadCount", 8)
        val processMaps = inputData.getBoolean("processMaps", true)

        val inputPathFile = File(inputPath)
        val outputPathFile = File(outputPath)

        val targetTypeName = format.substringBefore("_")
        val targetVersionString = format.substringAfter("_").replace("_", ".")
        val encodingType = EncodingType.getTypes().find { it.name.equals(targetTypeName, ignoreCase = true) }
        val outputVersion = Version.fromString(targetVersionString)

        System.out.println("\u001B[1;36m[Direct Engine] Starting Full World Conversion...\u001B[0m")
        System.out.println("Source Path : \u001B[33m$inputPath\u001B[0m")
        System.out.println("Target Path : \u001B[33m$outputPath\u001B[0m")
        System.out.println("Target Format: \u001B[32m$format\u001B[0m")
        System.out.println("Threads: $threadCount | Process Maps: $processMaps")
        System.out.println("================================================")

        try {
            val converter = WorldConverter(UUID.randomUUID())
            currentConverter = converter
            converter.setProcessItems(true)
            converter.setProcessEntities(true)
            converter.setProcessBlockEntities(true)
            converter.setProcessBiomes(true)
            converter.setProcessLighting(false)
            converter.setProcessColumnPreTransform(false)
            converter.setThreadCount(threadCount)
            converter.setProcessMaps(processMaps)

            val readerOptional = EncodingType.findReader(inputPathFile, converter)
            if (!readerOptional.isPresent) {
                System.err.println("\u001B[1;31m[FATAL] Failed to find suitable reader for the world.\u001B[0m")
                return Result.failure()
            }
            val reader = readerOptional.get()

            outputPathFile.deleteRecursively()
            outputPathFile.mkdirs()
            
            val writer = if (targetTypeName.equals("MINECLONIA", ignoreCase = true)) {
                me.voltual.mcl.writer.MclLevelWriter(outputPathFile)
            } else {
                val writerOpt = encodingType?.createWriter(outputPathFile, outputVersion, converter)
                    ?: throw IllegalStateException("Failed to create writer.")
                if (!writerOpt.isPresent) throw IllegalStateException("Failed to create writer.")
                writerOpt.get()
            }

            System.out.println("\u001B[33m[Engine] Compacting world and mapping blocks, please wait...\u001B[0m")
            val task = converter.convert(reader, writer)
            val future = task.future()

            var value = -1.0
            while (!future.isDone && !isStopped) {
                val polled = task.progress
                if (polled != value) {
                    System.out.printf("%.2f%%%n", (polled * 100.0))
                    value = polled
                }
                delay(100)
            }
            
            if (isStopped) {
                converter.cancel(null)
                System.out.println("\n\u001B[1;31m[Engine] Direct conversion aborted by system or user.\u001B[0m")
                return Result.failure()
            }
            
            future.get()

            try { reader.free() } catch (ignored: Exception) {}
            try { writer.free() } catch (ignored: Exception) {}
            
            System.out.println("\n\u001B[1;32m[SUCCESS] Conversion completed successfully in Direct Mode!\u001B[0m")
            return Result.success()

        } catch (e: CancellationException) {
            currentConverter?.cancel(null)
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}