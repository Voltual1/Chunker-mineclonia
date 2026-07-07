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
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.database.repository.LogRepository
import me.voltual.vb.ui.chunker.ConversionLogBridge
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.UUID

class StitchWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val logRepository: LogRepository by inject()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "stitch_channel",
                "World Stitching",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(context, "stitch_channel")
            .setContentTitle("存档缝合进行中")
            .setContentText("正在转移区块数据...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
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

        val pruningConfig = PruningConfig(
            true, 
            listOf(PruningRegion(minX, minZ, maxX, maxZ))
        )

        val sessionId = UUID.randomWork()
        val logBuilder = StringBuilder()
        
        fun log(msg: String, isError: Boolean = false) {
            logBuilder.appendLine(msg)
            ConversionLogBridge.sendLog(msg)
        }

        val exceptionHandler = java.util.function.Consumer<Throwable> { error ->
            val msg = "异常: ${error.message}"
            log(msg, true)
            error.printStackTrace()
        }

        val signalConsumer = java.util.function.BiConsumer<String, Any> { signalName, signalValue ->
            if (signalName == WorldConverter.SIGNAL_COMPACTION) {
                val isCompacting = signalValue as Boolean
                if (isCompacting) {
                    log("正在进行目标 LevelDB 数据库碎片整理与压缩...")
                } else {
                    log("LevelDB 压缩完成。")
                }
            }
        }

        log("STITCH_INIT // 启动存档缝合引擎...")
        log("源目录: $sourcePath")
        log("目标目录: $destPath")
        log("作用维度: $dimensionName")
        log("裁剪边界: 最小(X:$minX, Z:$minZ) 最大(X:$maxX, Z:$maxZ)")

        val threadCount = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
        val stitcher = WorldStitcher(sessionId, File(sourcePath), File(destPath), threadCount, exceptionHandler, signalConsumer)

        try {
            val environment = stitcher.stitch(dimension, pruningConfig)
            
            var progressLoopRunning = true
            val progressJob = kotlinx.coroutines.GlobalScope.launch {
                var lastProgress = -1.0
                while (progressLoopRunning && !environment.future().isDone) {
                    val p = environment.progress
                    if (kotlin.math.abs(p - lastProgress) > 0.001) {
                        lastProgress = p
                        setProgress(workDataOf("progress" to (p * 100).toFloat()))
                    }
                    kotlinx.coroutines.delay(100)
                }
            }

            environment.future().get()
            progressLoopRunning = false
            progressJob.join()

            log("STITCH_SUCCESS // 区块缝合操作彻底完成。")
            setProgress(workDataOf("progress" to 100f))

            logRepository.insertLog(
                LogEntry(
                    id = 0,
                    timestamp = System.currentTimeMillis(),
                    taskType = "Archive Stitching",
                    details = "Source: $sourcePath\nDest: $destPath\nBounds: ($minX, $minZ) to ($maxX, $maxZ)",
                    fullLog = logBuilder.toString()
                )
            )

            Result.success()
        } catch (e: Exception) {
            log("STITCH_FAILED // 缝合失败: ${e.message}", true)
            e.printStackTrace()
            
            logRepository.insertLog(
                LogEntry(
                    id = 0,
                    timestamp = System.currentTimeMillis(),
                    taskType = "Archive Stitching",
                    details = "FAILED: Source: $sourcePath | Dest: $destPath",
                    fullLog = logBuilder.toString()
                )
            )
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }
}