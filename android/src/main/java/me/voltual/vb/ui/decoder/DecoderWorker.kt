package me.voltual.vb.ui.decoder

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.mineclonia.engine.buffer.LayerV2StreamCodec
import java.io.File
import java.io.FileOutputStream

class DecoderWorker(
    val context: Context,
    val params: WorkerParameters
) : RemoteCoroutineWorker(context, params) {

    override suspend fun doRemoteWork(): Result {
        val inputUriStr = inputData.getString("inputUri") ?: return Result.failure()
        val outputPath = inputData.getString("outputPath") ?: return Result.failure()

        return withContext(Dispatchers.IO) {
            try {
                val inputUri = Uri.parse(inputUriStr)
                val worldFolderDoc = DocumentFile.fromTreeUri(context, inputUri) 
                    ?: return@withContext Result.failure()
                
                val targetExportDir = File(outputPath)
                targetExportDir.deleteRecursively()
                targetExportDir.mkdirs()

                val dbFolder = worldFolderDoc.findFile("db")
                if (dbFolder == null || !dbFolder.isDirectory) {
                    System.err.println("系统错误：未能在导入的存档中找到 db 目录")
                    return@withContext Result.failure()
                }

                val dbFiles = dbFolder.listFiles()
                var currentDoc: DocumentFile? = null
                var manifestDoc: DocumentFile? = null

                for (file in dbFiles) {
                    if (file.isFile) {
                        val name = file.name ?: continue
                        if (name == "CURRENT") currentDoc = file
                        if (name.startsWith("MANIFEST")) manifestDoc = file
                    }
                }

                if (currentDoc == null || manifestDoc == null) {
                    System.err.println("系统错误：缺少 CURRENT 或 MANIFEST 引导文件，还原终止")
                    return@withContext Result.failure()
                }

                val currentStream = context.contentResolver.openInputStream(currentDoc.uri)
                    ?: return@withContext Result.failure()
                val metaSource = currentStream.asSource().buffered()
                
                val decryptKey = try {
                    LayerV2StreamCodec.deriveTransformKey(metaSource, manifestDoc.name!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                val targetDbDir = File(targetExportDir, "db")
                targetDbDir.mkdirs()

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

                for (file in dbFiles) {
                    if (file.isFile) {
                        val fileName = file.name ?: continue
                        val inputStream = context.contentResolver.openInputStream(file.uri) ?: continue
                        val outFile = File(targetDbDir, fileName)
                        val outputStream = FileOutputStream(outFile)

                        if (decryptKey != null) {
                            val source = inputStream.asSource().buffered()
                            val sink = outputStream.asSink().buffered()
                            val ok = LayerV2StreamCodec.transformStream(source, sink, decryptKey)
                            if (!ok) {
                                context.contentResolver.openInputStream(file.uri)?.use { input ->
                                    FileOutputStream(outFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } else {
                            inputStream.use { input ->
                                outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }
}