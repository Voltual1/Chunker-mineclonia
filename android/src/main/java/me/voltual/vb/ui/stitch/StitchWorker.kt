package me.voltual.vb.ui.stitch

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import androidx.work.workDataOf
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import com.hivemc.chunker.conversion.stitch.WorldStitcher
import com.hivemc.chunker.pruning.PruningConfig
import com.hivemc.chunker.pruning.PruningRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import me.voltual.vb.core.database.AppDatabase
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.ui.chunker.ConversionLogBridge
import java.io.File
import java.util.UUID

class StitchWorker(
    private val context: Context,
    private val params: WorkerParameters
) : RemoteCoroutineWorker(context, params) {

    override suspend fun doRemoteWork(): Result = withContext(Dispatchers.IO) {
        val sourcePath = inputData.getString("sourcePath") ?: return@withContext Result.failure()
        val destPath = inputData.getString("destPath") ?: return@withContext Result.failure()
        val dimensionName = inputData.getString("dimension") ?: "minecraft:overworld"
        
        val minX = inputData.getInt("minX", 0)
        val minZ = inputData.getInt("minZ", 0)
        val maxX = inputData.getInt("maxX", 0)
        val maxZ = inputData.getInt("maxZ", 0)
        val offsetX = inputData.getInt("offsetX", 0)
        val offsetZ = inputData.getInt("offsetZ", 0)

        val dimension = when (dimensionName) {
            "minecraft:the_nether" -> Dimension.NETHER
            "minecraft:the_end" -> Dimension.THE_END
            else -> Dimension.OVERWORLD
        }

        val pruningConfig = PruningConfig(true, listOf(PruningRegion(minX, minZ, maxX, maxZ)))

        val sessionId = UUID.randomUUID()
        val logBuilder = StringBuilder()
        fun log(m: String) { logBuilder.appendLine(m); ConversionLogBridge.println(m) }

        val exceptionHandler = java.util.function.Consumer<Throwable> { error -> log("异常: ${error.message}"); error.printStackTrace() }
        val signalConsumer = java.util.function.BiConsumer<String, Any> { n, v -> if (n == WorldConverter.SIGNAL_COMPACTION && (v as Boolean)) log("正在进行 LevelDB 压缩...") }

        val stitcher = WorldStitcher(sessionId, File(sourcePath), File(destPath), maxOf(1, Runtime.getRuntime().availableProcessors() - 1), offsetX, offsetZ, exceptionHandler, signalConsumer)

        val database = AppDatabase.getDatabase(context)

        try {
            val env = stitcher.stitch(dimension, pruningConfig)
            
            val progressJob = CoroutineScope(Dispatchers.Default).launch {
                var lastP = -1.0
                while (!env.future().isDone) {
                    val p = env.progress
                    if (kotlin.math.abs(p - lastP) > 0.01) {
                        lastP = p
                        setProgress(workDataOf("progress" to (p * 10).toFloat()))
                    }
                    kotlinx.coroutines.delay(200)
                }
            }

            env.future().get()
            progressJob.join()
            setProgress(workDataOf("progress" to 100f))

            val totalProcessed = stitcher.processedChunks.get()
            log("STITCH_SUCCESS // 操作完成，共成功覆盖了 $totalProcessed 个物理区块！")

            database.logDao().insert(LogEntry(
                type = "STITCH", requestBody = "Source: $sourcePath", 
                responseBody = logBuilder.toString(), status = "SUCCESS"
            ))
            Result.success()
        } catch (e: Exception) {
            database.logDao().insert(LogEntry(
                type = "STITCH", requestBody = "FAILED", 
                responseBody = e.message ?: "Unknown", status = "FAILURE"
            ))
            Result.failure(workDataOf("error" to (e.message ?: "Stitch Error")))
        }
    }
}