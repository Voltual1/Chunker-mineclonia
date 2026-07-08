package me.voltual.vb.ui.stitch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
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

    /**
     * 实现前台服务信息，确保子进程任务在长时间执行或大内存占用时不会被系统回收
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "stitch_worker_channel"
        val notificationId = 2002 // 专用通知ID

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 兼容 Android 8.0+ 创建通知频道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "世界存档缝合服务",
                NotificationManager.IMPORTANCE_LOW // 使用低重要度，避免弹出干扰
            ).apply {
                description = "正在执行跨世界区块缝合与物理坐标系平移任务"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 构建前台通知，显示任务状态
        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_upload) // 使用系统上传图标
            .setContentTitle("存档重构中")
            .setContentText("正在执行物理级区块移植...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 设置为不可滑动删除
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

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

        val exceptionHandler = java.util.function.Consumer<Throwable> { error -> 
            log("异常: ${error.message}")
            error.printStackTrace() 
        }
        
        val signalConsumer = java.util.function.BiConsumer<String, Any> { n, v -> 
            if (n == WorldConverter.SIGNAL_COMPACTION && (v as Boolean)) {
                log("正在进行目标 LevelDB 数据库合并与压缩...")
            }
        }

        val stitcher = WorldStitcher(
            sessionId, 
            File(sourcePath), 
            File(destPath), 
            maxOf(1, Runtime.getRuntime().availableProcessors() - 1), 
            offsetX, 
            offsetZ, 
            exceptionHandler, 
            signalConsumer
        )

        val database = AppDatabase.getDatabase(context)

        try {
            val env = stitcher.stitch(dimension, pruningConfig)
            
            // 独立的进度上报协程
            val progressJob = CoroutineScope(Dispatchers.Default).launch {
                var lastP = -1.0
                while (!env.future().isDone) {
                    val p = env.progress
                    if (kotlin.math.abs(p - lastP) > 0.01) {
                        lastP = p
                        // 倍率校准：将环境进度缩放至 0-100 范围传递给 UI
                        setProgress(workDataOf("progress" to (p * 10).toFloat()))
                    }
                    kotlinx.coroutines.delay(200)
                }
            }

            env.future().get()
            progressJob.join()
            setProgress(workDataOf("progress" to 100f))

            val totalProcessed = stitcher.processedChunks.get()
            log("STITCH_SUCCESS // 完成：共平移并缝合了 $totalProcessed 个区块")

            database.logDao().insert(LogEntry(
                type = "STITCH", 
                requestBody = "Source: $sourcePath\nDest: $destPath\nOffset: ($offsetX, $offsetZ)", 
                responseBody = logBuilder.toString(), 
                status = "SUCCESS"
            ))
            Result.success()
        } catch (e: Exception) {
            log("STITCH_CRITICAL // 缝合过程发生灾难性故障：${e.message}")
            database.logDao().insert(LogEntry(
                type = "STITCH", 
                requestBody = "FAILED_STITCH", 
                responseBody = logBuilder.toString() + "\n" + (e.message ?: "Unknown"), 
                status = "FAILURE"
            ))
            Result.failure(workDataOf("error" to (e.message ?: "Stitch Error")))
        }
    }
}