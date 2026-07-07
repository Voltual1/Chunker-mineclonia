package me.voltual.vb.ui.stitch

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.anggrayudi.storage.callback.SingleFolderConflictCallback
import com.anggrayudi.storage.file.copyFolderTo
import com.anggrayudi.storage.result.SingleFolderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.FileRepairUtil
import java.io.File
import java.util.UUID

class StitchViewModel(private val context: Context) : ViewModel() {

    var sourceFolder by mutableStateOf<DocumentFile?>(null)
    var destFolder by mutableStateOf<DocumentFile?>(null)

    var dimension by mutableStateOf("minecraft:overworld")
    var minX by mutableStateOf("0")
    var minZ by mutableStateOf("0")
    var maxX by mutableStateOf("31")
    var maxZ by mutableStateOf("31")

    var isPreparing by mutableStateOf(false)
    var prepareStatus by mutableStateOf("")
    var prepareProgress by mutableStateOf(0f)

    var stitchWorkId by mutableStateOf<UUID?>(null)
    var stitchProgress by mutableStateOf(0f)
    var isStitching by mutableStateOf(false)
    var stitchSuccess by mutableStateOf(false)
    var stitchError by mutableStateOf<String?>(null)

    private fun getInternalDir(folderName: String): File {
        val externalDir = context.getExternalFilesDir(null)
        val dir = if (externalDir != null) File(externalDir, folderName) else File(context.filesDir, folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun startStitch() {
        val sourceDoc = sourceFolder ?: return
        val destDoc = destFolder ?: return
        
        val minXVal = minX.toIntOrNull() ?: 0
        val minZVal = minZ.toIntOrNull() ?: 0
        val maxXVal = maxX.toIntOrNull() ?: 31
        val maxZVal = maxZ.toIntOrNull() ?: 31

        isPreparing = true
        stitchSuccess = false
        stitchError = null

        viewModelScope.launch(Dispatchers.IO) {
            val sourceInternal = getInternalDir("stitch_source")
            val destInternal = getInternalDir("stitch_dest")

            sourceInternal.deleteRecursively()
            destInternal.deleteRecursively()
            sourceInternal.mkdirs()
            destInternal.mkdirs()

            // 拷贝源文件
            withContext(Dispatchers.Main) { prepareStatus = "正在拷贝源世界至沙盒..." }
            val sourceCopied = copyToInternal(sourceDoc, sourceInternal)
            if (!sourceCopied) {
                withContext(Dispatchers.Main) { 
                    isPreparing = false
                    stitchError = "源世界读取失败"
                }
                return@launch
            }
            FileRepairUtil.repairCopiedDatabaseFiles(sourceInternal)

            // 拷贝目标文件
            withContext(Dispatchers.Main) { prepareStatus = "正在拷贝目标世界至沙盒..." }
            val destCopied = copyToInternal(destDoc, destInternal)
            if (!destCopied) {
                withContext(Dispatchers.Main) { 
                    isPreparing = false
                    stitchError = "目标世界读取失败"
                }
                return@launch
            }
            FileRepairUtil.repairCopiedDatabaseFiles(destInternal)

            withContext(Dispatchers.Main) {
                isPreparing = false
                isStitching = true
                launchStitchWorker(sourceInternal.absolutePath, destInternal.absolutePath, minXVal, minZVal, maxXVal, maxZVal)
            }
        }
    }

    private suspend fun copyToInternal(doc: DocumentFile, internalDir: File): Boolean {
        var success = false
        val countDownLatch = java.util.concurrent.CountDownLatch(1)
        val targetParentDoc = DocumentFile.fromFile(internalDir.parentFile!!)
        
        doc.copyFolderTo(
            context = context,
            targetParentFolder = targetParentDoc,
            newFolderNameInTargetPath = internalDir.name,
            skipEmptyFiles = false,
            onConflict = object : SingleFolderConflictCallback(viewModelScope) {
                override fun onParentConflict(destinationFolder: DocumentFile, action: ParentFolderConflictAction, canMerge: Boolean) {
                    action.confirmResolution(ConflictResolution.REPLACE)
                }
            }
        ).collect { result ->
            when (result) {
                is SingleFolderResult.InProgress -> {
                    withContext(Dispatchers.Main) { prepareProgress = result.progress / 100f }
                }
                is SingleFolderResult.Completed -> {
                    success = true
                    countDownLatch.countDown()
                }
                is SingleFolderResult.Error -> {
                    countDownLatch.countDown()
                }
                else -> {}
            }
        }
        countDownLatch.await()
        return success
    }

    private fun launchStitchWorker(sourcePath: String, destPath: String, xMin: Int, zMin: Int, xMax: Int, zMax: Int) {
        val inputData = workDataOf(
            "sourcePath" to sourcePath,
            "destPath" to destPath,
            "dimension" to dimension,
            "minX" to xMin,
            "minZ" to zMin,
            "maxX" to xMax,
            "maxZ" to zMax
        )

        val workRequest = OneTimeWorkRequestBuilder<StitchWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        stitchWorkId = workRequest.id
        WorkManager.getInstance(context).enqueue(workRequest)

        viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    stitchProgress = workInfo.progress.getFloat("progress", 0f)
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        isStitching = false
                        exportStitchedWorld()
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        isStitching = false
                        stitchError = workInfo.outputData.getString("error") ?: "未知错误"
                    }
                }
            }
        }
    }

    private fun exportStitchedWorld() {
        isPreparing = true
        prepareStatus = "打包导出中，请进入FTP或世界导出查看..."
        viewModelScope.launch(Dispatchers.IO) {
            val destInternal = getInternalDir("stitch_dest")
            val outputDir = getInternalDir("world_output")
            outputDir.deleteRecursively()
            destInternal.renameTo(outputDir)
            
            withContext(Dispatchers.Main) {
                isPreparing = false
                stitchSuccess = true
            }
        }
    }
}