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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.FileRepairUtil
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class StitchViewModel(private val context: Context) : ViewModel() {

    var sourceFolder by mutableStateOf<DocumentFile?>(null)
    var destFolder by mutableStateOf<DocumentFile?>(null)

    var dimension by mutableStateOf("minecraft:overworld")
    var minX by mutableStateOf("0")
    var minZ by mutableStateOf("0")
    var maxX by mutableStateOf("100")
    var maxZ by mutableStateOf("100")

    var isPreparing by mutableStateOf(false)
    var prepareStatus by mutableStateOf("")
    var prepareProgress by mutableStateOf(0f)

    var stitchWorkId by mutableStateOf<UUID?>(null)
    var stitchProgress by mutableStateOf(0f)
    var isStitching by mutableStateOf(false)
    var stitchSuccess by mutableStateOf(false)
    var stitchError by mutableStateOf<String?>(null)

    private fun getWorldsDir(): File {
        val externalDir = context.getExternalFilesDir(null)
        val worldsDir = if (externalDir != null) File(externalDir, "worlds") else File(context.filesDir, "worlds")
        if (!worldsDir.exists()) worldsDir.mkdirs()
        return worldsDir
    }

    fun startStitch() {
        val sourceDoc = sourceFolder ?: return
        val destDoc = destFolder ?: return
        
        // 核心修复：接收方块坐标，自动转换为区块坐标 (shr 4) 并处理大小容错
        val x1 = minX.toIntOrNull() ?: 0
        val z1 = minZ.toIntOrNull() ?: 0
        val x2 = maxX.toIntOrNull() ?: 0
        val z2 = maxZ.toIntOrNull() ?: 0

        val chunkMinX = min(x1, x2) shr 4
        val chunkMinZ = min(z1, z2) shr 4
        val chunkMaxX = max(x1, x2) shr 4
        val chunkMaxZ = max(z1, z2) shr 4

        isPreparing = true
        stitchSuccess = false
        stitchError = null

        viewModelScope.launch(Dispatchers.IO) {
            val rootDir = getWorldsDir()
            val sourceInternal = File(rootDir, "stitch_source")
            val outputInternal = File(rootDir, "world_output")

            sourceInternal.deleteRecursively()
            outputInternal.deleteRecursively()
            sourceInternal.mkdirs()
            outputInternal.mkdirs()

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

            withContext(Dispatchers.Main) { prepareStatus = "正在拷贝目标世界至沙盒..." }
            val destCopied = copyToInternal(destDoc, outputInternal)
            if (!destCopied) {
                withContext(Dispatchers.Main) { 
                    isPreparing = false
                    stitchError = "目标世界读取失败"
                }
                return@launch
            }
            FileRepairUtil.repairCopiedDatabaseFiles(outputInternal)

            withContext(Dispatchers.Main) {
                isPreparing = false
                isStitching = true
                launchStitchWorker(sourceInternal.absolutePath, outputInternal.absolutePath, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)
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
        val inputData = Data.Builder()
            .putString("sourcePath", sourcePath)
            .putString("destPath", destPath)
            .putString("dimension", dimension)
            .putInt("minX", xMin)
            .putInt("minZ", zMin)
            .putInt("maxX", xMax)
            .putInt("maxZ", zMax)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME", context.packageName)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME", "androidx.work.multiprocess.RemoteWorkerService")
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StitchWorker>()
            .setInputData(inputData)
            .build()

        stitchWorkId = workRequest.id
        androidx.work.multiprocess.RemoteWorkManager.getInstance(context).enqueueUniqueWork("stitch_work", ExistingWorkPolicy.REPLACE, workRequest)

        viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    stitchProgress = workInfo.progress.getFloat("progress", 0f)
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        isStitching = false
                        stitchSuccess = true
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        isStitching = false
                        stitchError = workInfo.outputData.getString("error") ?: "核心缝合引擎异常"
                    }
                }
            }
        }
    }
}