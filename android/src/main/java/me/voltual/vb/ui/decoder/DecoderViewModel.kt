package me.voltual.vb.ui.decoder

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import androidx.work.multiprocess.RemoteWorkManager
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DecoderViewModel(private val context: Context) : ViewModel() {

    var selectedInputFolder by mutableStateOf<DocumentFile?>(null)
    var selectedOutputFolder by mutableStateOf<DocumentFile?>(null)

    var isProcessing by mutableStateOf(false)
    var progressText by mutableStateOf("")
    var progressVal by mutableStateOf(0f)

    fun startDecoding(onFinished: (Boolean, String) -> Unit) {
        val inputFolder = selectedInputFolder ?: return
        val outputFolder = selectedOutputFolder ?: return

        isProcessing = true
        progressVal = 0.1f
        progressText = "正在启动还原任务..."

        val cacheOutputDir = File(context.cacheDir, "decoder_final_output")
        cacheOutputDir.deleteRecursively()
        cacheOutputDir.mkdirs()

        val remoteWorkManager = RemoteWorkManager.getInstance(context)
        val workManager = WorkManager.getInstance(context)

        val inputData = Data.Builder()
            .putString("inputUri", inputFolder.uri.toString())
            .putString("outputPath", cacheOutputDir.absolutePath)
            .putString(
                "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME",
                context.packageName
            )
            .putString(
                "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME",
                "androidx.work.multiprocess.RemoteWorkerService"
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DecoderWorker>()
            .setInputData(inputData)
            .build()

        remoteWorkManager.enqueueUniqueWork(
            "decoder_work",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        viewModelScope.launch(Dispatchers.Main) {
            val liveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
            
            val observer = object : Observer<WorkInfo?> {
                override fun onChanged(value: WorkInfo?) {
                    if (value != null) {
                        when (value.state) {
                            WorkInfo.State.RUNNING -> {
                                progressText = "正在扫描并还原世界存档流..."
                                progressVal = 0.5f
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                liveData.removeObserver(this)
                                exportDecodedFolder(cacheOutputDir, outputFolder, onFinished)
                            }
                            WorkInfo.State.FAILED -> {
                                liveData.removeObserver(this)
                                isProcessing = false
                                onFinished(false, "还原失败，请确保选择的是有效的存档目录")
                            }
                            WorkInfo.State.CANCELLED -> {
                                liveData.removeObserver(this)
                                isProcessing = false
                                onFinished(false, "还原任务被用户或系统取消")
                            }
                            else -> {}
                        }
                    }
                }
            }
            liveData.observeForever(observer)
        }
    }

    private fun exportDecodedFolder(
        localOutputDir: File,
        safOutputDir: DocumentFile,
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                progressText = "还原完成，正在输出到目标文件夹..."
                progressVal = 0.8f
            }

            try {
                writeLocalDirToSaf(localOutputDir, safOutputDir)
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    progressVal = 1f
                    onFinished(true, "存档还原已完全成功！")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    onFinished(false, "还原后写入目标文件夹失败: ${e.message}")
                }
            } finally {
                localOutputDir.deleteRecursively()
            }
        }
    }

    private fun writeLocalDirToSaf(localDir: File, safDir: DocumentFile) {
        val files = localDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                var subSafDir = safDir.findFile(file.name)
                if (subSafDir == null || !subSafDir.isDirectory) {
                    subSafDir = safDir.createDirectory(file.name)
                }
                if (subSafDir != null) {
                    writeLocalDirToSaf(file, subSafDir)
                }
            } else {
                var targetDoc = safDir.findFile(file.name)
                if (targetDoc != null && targetDoc.isFile) {
                    targetDoc.delete()
                }
                targetDoc = safDir.makeFile(context, file.name)
                if (targetDoc != null) {
                    targetDoc.openOutputStream(context)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}