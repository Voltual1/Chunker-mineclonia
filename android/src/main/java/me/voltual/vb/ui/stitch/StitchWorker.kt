package me.voltual.vb.ui.stitch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.UUID

class StitchWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val database: AppDatabase by inject()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("stitch_channel", "Stitching", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, "stitch_channel")
            .setContentTitle("存档缝合进行中")
            .setContentText("正在重写区块...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .build()
        return ForegroundInfo(2002, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sourcePath = inputData.getString("sourcePath") ?: return@withContext Result.failure()
        val destPath = inputData.getString("destPath") ?: return@withContext Result.failure()
        val dimensionName = inputData.getString("dimension") ?: "minecraft:overworld"
        
        val minX = inputData.getInt("minX", 0)
        val minZ = inputData.getInt("minZ", 0)
        val maxX = inputData.getInt("maxX", 0)
        val maxZ = inputData.getInt("maxZ", 0)
        
       val dimension = when (dimensionName) {
            "minecraft:the_nether" -> Dimension.NETHER
            "minecraft:the_end" -> Dimension.THE_END
            else -> Dimension.OVERWORLD
        }

        val offsetX = inputData.getInt("offsetX", 0)
        val offsetZ = inputData.getInt("offsetZ", 0)

        val pruningConfig = PruningConfig(
            true, 
            listOf(PruningRegion(minX, minZ, maxX, maxZ))
        )

        val sessionId = UUID.randomUUID()
        val logBuilder = StringBuilder()
        fun log(m: String) { logBuilder.appendLine(m); ConversionLogBridge.println(m) }

        val exceptionHandler = java.util.function.Consumer<Throwable> { error -> log("异常: ${error.message}"); error.printStackTrace() }
        val signalConsumer = java.util.function.BiConsumer<String, Any> { n, v -> if (n == WorldConverter.SIGNAL_COMPACTION && (v as Boolean)) log("正在进行 LevelDB 压缩...") }

        log("STITCH_INIT // 启动可视化平移缝合引擎...")
        log("源目录: $sourcePath")
        log("目标目录: $destPath")
        log("作用维度: $dimensionName")
        log("源边界(区块系): ($minX, $minZ) ~ ($maxX, $maxZ)")
        log("坐标偏移量(区块系): X_Offset=$offsetX, Z_Offset=$offsetZ")

        val threadCount = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
        val stitcher = WorldStitcher(sessionId, File(sourcePath), File(destPath), threadCount, offsetX, offsetZ, exceptionHandler, signalConsumer)

        try {
            val env = stitcher.stitch(dimension, pruningConfig)
            
            val progressJob = launch {
                var lastP = -1.0
                while (!env.future().isDone) {
                    val p = env.progress
                    if (kotlin.math.abs(p - lastP) > 0.01) {
                        lastP = p
                        setProgress(workDataOf("progress" to (p * 100).toFloat()))
                    }
                    kotlinx.coroutines.delay(200)
                }
            }

            env.future().get()
            progressJob.join()
            setProgress(workDataOf("progress" to 100f))

            // 核心提示：打印出确切被影响的区块数量！
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