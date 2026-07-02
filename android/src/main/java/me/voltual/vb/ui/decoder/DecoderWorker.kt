package me.voltual.vb.ui.decoder

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.mineclonia.engine.buffer.LayerV2StreamCodec
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DecoderWorker(
    val context: Context,
    val params: WorkerParameters
) : RemoteCoroutineWorker(context, params) {

    override suspend fun doRemoteWork(): Result {
        val inputPath = inputData.getString("inputPath") ?: return Result.failure()
        val outputPath = inputData.getString("outputPath") ?: return Result.failure()
        val metaFilePath = inputData.getString("metaFilePath") ?: return Result.failure()
        val identifier = inputData.getString("identifier") ?: return Result.failure()

        return withContext(Dispatchers.IO) {
            try {
                val inputDir = File(inputPath)
                val outputDir = File(outputPath)
                val metaFile = File(metaFilePath)

                if (!inputDir.exists() || !metaFile.exists()) {
                    return@withContext Result.failure()
                }

                outputDir.deleteRecursively()
                outputDir.mkdirs()

                // Derive key - 使用 .buffered() 将 RawSource 转换为 Source
                val metaSource = FileInputStream(metaFile).asSource().buffered()
                val transformKey = try {
                    LayerV2StreamCodec.deriveTransformKey(metaSource, identifier)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext Result.failure()
                }

                // 递归转换流目录
                val success = decryptDirectory(inputDir, outputDir, transformKey)
                if (success) {
                    Result.success()
                } else {
                    Result.failure()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun decryptDirectory(srcDir: File, destDir: File, key: ByteArray): Boolean {
        val files = srcDir.listFiles() ?: return true
        for (file in files) {
            val destFile = File(destDir, file.name)
            if (file.isDirectory) {
                destFile.mkdirs()
                if (!decryptDirectory(file, destFile, key)) {
                    return false
                }
            } else {
                try {
                    // 使用 .buffered() 将 RawSource/RawSink 转换为 Source/Sink
                    val source = FileInputStream(file).asSource().buffered()
                    val sink = FileOutputStream(destFile).asSink().buffered()
                    
                    val ok = LayerV2StreamCodec.transformStream(source, sink, key)
                    if (!ok) {
                        // 解码失败时作为兜底方案执行常规拷贝
                        file.copyTo(destFile, overwrite = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }
        }
        return true
    }
}