package me.voltual.vb.ui.decoder

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

                // 1. 自动寻找包含 CURRENT 和 MANIFEST 的 db 目录
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

                // 2. 采用原生原始算法推导解密 Key
                val decryptKey = try {
                    FileInputStream(currentFile).use { currentStream ->
                        NetEaseDecryptor.deriveKey(currentStream, manifestFile.name)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext Result.failure()
                }

                // 3. 递归解密并转换整个存档目录结构
                val success = decryptDirectory(inputDir, outputDir, decryptKey)
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
                    val fis = FileInputStream(file)
                    val fos = FileOutputStream(destFile)
                    
                    // 利用原始 NetEaseDecryptor 的 decryptFile 方法完成转换
                    val ok = NetEaseDecryptor.decryptFile(fis, fos, key)
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