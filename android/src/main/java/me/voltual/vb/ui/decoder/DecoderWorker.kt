package me.voltual.vb.ui.decoder

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

                val decryptor = AndroidWorldDecryptor(context)
                val resultPath = decryptor.decryptWorld(
                    worldFolderDoc = worldFolderDoc,
                    targetExportDir = targetExportDir,
                    listener = object : AndroidWorldDecryptor.DecryptListener {
                        override fun onProgress(progress: Int) {
                            // WorkManager 进度可以通过 setProgress 更新，但这里主要靠 ViewModel 观察状态
                        }
                        override fun onLog(message: String) {
                            println("DecoderLog: $message")
                        }
                        override fun onSuccess(exportPath: String) {
                            println("DecoderSuccess: Exported to $exportPath")
                        }
                        override fun onError(error: String) {
                            System.err.println("DecoderError: $error")
                        }
                    }
                )

                if (resultPath != null) {
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
}