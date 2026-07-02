package me.voltual.vb.ui.decoder

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.anggrayudi.storage.callback.SingleFolderConflictCallback
import com.anggrayudi.storage.file.copyFolderTo
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.openOutputStream
import com.anggrayudi.storage.result.SingleFolderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.extension.text.formatSize
import java.io.File

class DecoderViewModel(private val context: Context) : ViewModel() {

    var selectedInputFolder by mutableStateOf<DocumentFile?>(null)
    var selectedOutputFolder by mutableStateOf<DocumentFile?>(null)
    var selectedMetaFile by mutableStateOf<DocumentFile?>(null)
    var identifier by mutableStateOf("")

    var isProcessing by mutableStateOf(false)
    var progressText by mutableStateOf("")
    var progressVal by mutableStateOf(0f)

    fun startDecoding(onFinished: (Boolean, String) -> Unit) {
        val inputFolder = selectedInputFolder ?: return
        val outputFolder = selectedOutputFolder ?: return
        val metaFile = selectedMetaFile ?: return
        val idVal = identifier

        if (idVal.isBlank()) {
            onFinished(false, "标识符不能为空")
            return
        }

        isProcessing = true
        progressVal = 0f
        progressText = "正在准备导入存档..."

        viewModelScope.launch(Dispatchers.IO) {
            val cacheInputDir = File(context.cacheDir, "decoder_input")
            val cacheOutputDir = File(context.cacheDir, "decoder_output")
            val cacheMetaFile = File(context.cacheDir, "decoder_meta_file")

            cacheInputDir.deleteRecursively()
            cacheOutputDir.deleteRecursively()
            if (cacheMetaFile.exists()) cacheMetaFile.delete()

            cacheInputDir.mkdirs()
            cacheOutputDir.mkdirs()

            // 1. 拷贝 Meta 文件到内部存储以避开 SAF Binder 限制
            try {
                context.contentResolver.openInputStream(metaFile.uri)?.use { input ->
                    cacheMetaFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    onFinished(false, "读取密钥文件失败: ${e.message}")
                }
                return@launch
            }

            // 2. 拷贝输入文件夹至内部存储
            val targetParentDoc = DocumentFile.fromFile(context.cacheDir)
            val conflictCallback = object : SingleFolderConflictCallback(viewModelScope) {
                override fun onParentConflict(
                    destinationFolder: DocumentFile,
                    action: ParentFolderConflictAction,
                    canMerge: Boolean
                ) {
                    action.confirmResolution(ConflictResolution.REPLACE)
                }
            }

            inputFolder.copyFolderTo(
                context = context,
                targetParentFolder = targetParentDoc,
                skipEmptyFiles = false,
                newFolderNameInTargetPath = "decoder_input",
                onConflict = conflictCallback
            ).collect { result ->
                when (result) {
                    is SingleFolderResult.InProgress -> {
                        withContext(Dispatchers.Main) {
                            progressVal = (result.progress / 100f) * 0.4f
                            progressText = "正在导入存档: ${(result.progress).toInt()}% (${result.bytesMoved.formatSize()})"
                        }
                    }
                    is SingleFolderResult.Completed -> {
                        runWorker(cacheInputDir, cacheOutputDir, cacheMetaFile, idVal, outputFolder, onFinished)
                    }
                    is SingleFolderResult.Error -> {
                        withContext(Dispatchers.Main) {
                            isProcessing = false
                            onFinished(false, "导入存档失败，错误码: ${result.errorCode}")
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun runWorker(
        inputDir: File,
        outputDir: File,
        metaFile: File,
        idVal: String,
        safOutputDir: DocumentFile,
        onFinished: (Boolean, String) -> Unit
    ) {
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putString("inputPath", inputDir.absolutePath)
            .putString("outputPath", outputDir.absolutePath)
            .putString("metaFilePath", metaFile.absolutePath)
            .putString("identifier", idVal)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DecoderWorker>()
            .setInputData(inputData)
            .build()

        viewModelScope.launch(Dispatchers.Main) {
            progressText = "正在解析并解码存档数据..."
            progressVal = 0.5f
        }

        workManager.enqueue(workRequest)
        workManager.getWorkInfoByIdLiveData(workRequest.id).observeForever { workInfo ->
            if (workInfo != null) {
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        exportDecodedFolder(outputDir, safOutputDir, onFinished)
                    }
                    WorkInfo.State.FAILED -> {
                        isProcessing = false
                        onFinished(false, "解码任务失败")
                    }
                    WorkInfo.State.CANCELLED -> {
                        isProcessing = false
                        onFinished(false, "解码任务被取消")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun exportDecodedFolder(
        localOutputDir: File,
        safOutputDir: DocumentFile,
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                progressText = "正在导出已解码的存档..."
                progressVal = 0.7f
            }

            try {
                writeLocalDirToSaf(localOutputDir, safOutputDir)
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    progressVal = 1f
                    onFinished(true, "存档还原已完全成功并成功导出！")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    onFinished(false, "导出已解码存档失败: ${e.message}")
                }
            } finally {
                localOutputDir.deleteRecursively()
                File(context.cacheDir, "decoder_input").deleteRecursively()
                val meta = File(context.cacheDir, "decoder_meta_file")
                if (meta.exists()) meta.delete()
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