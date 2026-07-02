package me.voltual.vb.ui.decoder

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidWorldDecryptor(private val context: Context) {

    suspend fun decryptWorld(
        worldFolderDoc: DocumentFile,
        targetExportDir: File,
        listener: DecryptListener
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dbFolder = worldFolderDoc.findFile("db")
            if (dbFolder == null || !dbFolder.isDirectory) {
                listener.onError("未找到有效的 db 数据库文件夹，请确保选择正确的网易存档根目录")
                return@withContext null
            }

            val allFiles = dbFolder.listFiles()
            var currentFileDoc: DocumentFile? = null
            var manifestFileDoc: DocumentFile? = null

            for (file in allFiles) {
                if (file.isFile) {
                    val name = file.name ?: continue
                    if (name == "CURRENT") currentFileDoc = file
                    if (name.startsWith("MANIFEST")) manifestFileDoc = file
                }
            }

            if (currentFileDoc == null || manifestFileDoc == null) {
                listener.onError("存档 db 目录下缺失 CURRENT 或 MANIFEST 核心数据库指针文件")
                return@withContext null
            }

            listener.onLog("开始读取 CURRENT 指针文件，计算解密密钥...")
            val currentStream = context.contentResolver.openInputStream(currentFileDoc.uri)
                ?: throw IllegalStateException("无法打开 CURRENT 文件的输入流")

            val decryptKey = try {
                NetEaseDecryptor.deriveKey(currentStream, manifestFileDoc.name!!)
            } catch (e: Exception) {
                listener.onLog("密钥提取失败或未加密：${e.message}。如果未加密，将视为原样复制。")
                null
            }

            val targetDbDir = File(targetExportDir, "db")
            if (!targetDbDir.exists()) {
                targetDbDir.mkdirs()
            }

            listener.onLog("正在复制外部基本元数据文件...")
            worldFolderDoc.listFiles().forEach { file ->
                if (file.isFile) {
                    context.contentResolver.openInputStream(file.uri)?.use { input ->
                        val outFile = File(targetExportDir, file.name!!)
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            listener.onLog("开始逐个解密并导出核心 LevelDB 数据库...")
            val totalFiles = allFiles.size
            var processed = 0

            for (file in allFiles) {
                if (file.isFile) {
                    val fileName = file.name ?: continue
                    val inputStream = context.contentResolver.openInputStream(file.uri)
                    if (inputStream != null) {
                        val outFile = File(targetDbDir, fileName)
                        val outputStream = FileOutputStream(outFile)

                        if (decryptKey != null) {
                            NetEaseDecryptor.decryptFile(inputStream, outputStream, decryptKey)
                        } else {
                            inputStream.use { input ->
                                outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
                processed++
                withContext(Dispatchers.Main) {
                    listener.onProgress((processed * 100) / totalFiles)
                }
            }

            listener.onSuccess(targetExportDir.absolutePath)
            return@withContext targetExportDir.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            listener.onError("解密发生未知异常: ${e.message}")
            return@withContext null
        }
    }

    interface DecryptListener {
        fun onProgress(progress: Int)
        fun onLog(message: String)
        fun onSuccess(exportPath: String)
        fun onError(error: String)
    }
}