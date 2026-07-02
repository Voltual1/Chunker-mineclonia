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

        return withContext(Dispatchers.IO) {
            try {
                val inputDir = File(inputPath)
                val outputDir = File(outputPath)

                if (!inputDir.exists()) {
                    return@withContext Result.failure()
                }

                outputDir.deleteRecursively()
                outputDir.mkdirs()

                val dbDir = findDbDir(inputDir)
                if (dbDir == null) {
                    System.err.println("系统错误：未能在导入的存档中找到有效的 LevelDB 数据库目录(未找到 CURRENT 或 MANIFEST)")
                    return@withContext Result.failure()
                }

                val currentFile = File(dbDir, "CURRENT")
                val manifestFile = dbDir.listFiles()?.find { it.name.startsWith("MANIFEST") }
                if (manifestFile == null) {
                    System.err.println("系统错误：未能在 db 目录下找到 MANIFEST 文件")
                    return@withContext Result.failure()
                }

                // 2. 自动推导解密 Key
                val metaSource = FileInputStream(currentFile).asSource().buffered()
                val transformKey = try {
                    LayerV2StreamCodec.deriveTransformKey(metaSource, manifestFile.name)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext Result.failure()
                }

                // 3. 递归解密并转换整个存档目录结构
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

    private fun findDbDir(dir: File): File? {
        if (File(dir, "CURRENT").exists() && dir.listFiles()?.any { it.name.startsWith("MANIFEST") } == true) {
            return dir
        }
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findDbDir(file)
                if (found != null) return found
            }
        }
        return null
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
                    val source = FileInputStream(file).asSource().buffered()
                    val sink = FileOutputStream(destFile).asSink().buffered()
                    
                    val ok = LayerV2StreamCodec.transformStream(source, sink, key)
                    if (!ok) {
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