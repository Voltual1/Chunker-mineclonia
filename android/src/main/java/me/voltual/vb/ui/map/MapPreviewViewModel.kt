package me.voltual.vb.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import androidx.work.multiprocess.RemoteWorkManager
import com.anggrayudi.storage.callback.SingleFolderConflictCallback
import com.anggrayudi.storage.file.copyFolderTo
import com.anggrayudi.storage.result.SingleFolderResult
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry // 核心修复：补全导入
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.FileRepairUtil
import me.voltual.vb.ui.stitch.StitchWorker
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Converter
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBChunkType
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey
import com.hivemc.chunker.conversion.handlers.LevelConversionHandler
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelSettings
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld
import com.hivemc.chunker.mapping.resolver.MappingsFileResolvers
import com.hivemc.chunker.scheduling.task.FutureTask
import com.hivemc.chunker.scheduling.task.Task
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import kotlin.math.max
import kotlin.math.min

enum class PreviewState {
    IDLE,
    SOURCE_SELECT,
    DEST_PASTE,
    STITCHING
}

class MapPreviewViewModel : ViewModel() {

    var previewState by mutableStateOf(PreviewState.IDLE)
        private set

    var isLoading by mutableStateOf(false)
        private set

    val availableDimensions = mutableStateListOf<Dimension>()
    var selectedDimension by mutableStateOf(Dimension.OVERWORLD)

    val regionBitmaps = mutableStateMapOf<Pair<Dimension, RegionCoordPair>, Bitmap>()
    private val regionRGBAData = ConcurrentHashMap<Pair<Dimension, RegionCoordPair>, ConcurrentHashMap<ChunkCoordPair, IntArray>>()

    var statusMessage by mutableStateOf("")
        private set
        
    var isBedrock by mutableStateOf(false)
        private set

    var worldDirUri by mutableStateOf("")
    
    var isLoaded by mutableStateOf(false)
        private set

    var mapScale by mutableStateOf(1f)
    var mapOffset by mutableStateOf(Offset.Zero)
    var isMapCentered by mutableStateOf(false)

    var showGrid by mutableStateOf(false)

    var hasExistingFtpInput by mutableStateOf(false)
        private set

    var sourceSelectionStart by mutableStateOf<Pair<Int, Int>?>(null)
    var sourceSelectionEnd by mutableStateOf<Pair<Int, Int>?>(null)
    var pasteTargetPoint by mutableStateOf<Pair<Int, Int>?>(null)

    var isStitching by mutableStateOf(false)
    var stitchProgress by mutableStateOf(0f)
    var stitchSuccess by mutableStateOf(false)
    var stitchError by mutableStateOf<String?>(null)

    val localTargetWorlds = mutableStateListOf<File>()
    var currentDestPath by mutableStateOf("")

    fun clearSelection() {
        sourceSelectionStart = null
        sourceSelectionEnd = null
        pasteTargetPoint = null
        previewState = PreviewState.IDLE
    }

    fun toggleSourceSelectionMode() {
        if (previewState == PreviewState.IDLE) {
            previewState = PreviewState.SOURCE_SELECT
            sourceSelectionStart = null
            sourceSelectionEnd = null
        } else {
            previewState = PreviewState.IDLE
        }
    }

    fun abortPasting() {
        previewState = PreviewState.IDLE
        pasteTargetPoint = null
    }

    private fun getWorldsDir(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        val worldsDir = if (externalDir != null) File(externalDir, "worlds") else File(context.filesDir, "worlds")
        if (!worldsDir.exists()) worldsDir.mkdirs()
        return worldsDir
    }

    fun scanLocalTargetWorlds(context: Context) {
        val rootDir = getWorldsDir(context)
        val files = rootDir.listFiles() ?: emptyArray()
        localTargetWorlds.clear()
        localTargetWorlds.addAll(
            files.filter { 
                it.isDirectory && 
                it.name != "stitch_source" && 
                it.name != "world_preview" && 
                it.name != "stitch_dest_temp"
            }
        )
    }

    fun selectExistingLocalTarget(context: Context, targetFolder: File) {
        currentDestPath = targetFolder.absolutePath
        previewState = PreviewState.DEST_PASTE
        viewModelScope.launch(Dispatchers.Main) {
            internalLoadAndRender(context, currentDestPath)
        }
    }

    fun copyExternalTargetToTemp(context: Context, destDoc: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootDir = getWorldsDir(context)
            val tempDestDir = File(rootDir, "stitch_dest_temp")
            tempDestDir.deleteRecursively()
            tempDestDir.mkdirs()

            withContext(Dispatchers.Main) {
                previewState = PreviewState.DEST_PASTE
                isLoading = true
                statusMessage = "正在复制目标至临时缝合区..."
            }

            val countDownLatch = java.util.concurrent.CountDownLatch(1)
            destDoc.copyFolderTo(
                context = context,
                targetParentFolder = DocumentFile.fromFile(rootDir),
                newFolderNameInTargetPath = tempDestDir.name,
                skipEmptyFiles = false,
                onConflict = object : SingleFolderConflictCallback(viewModelScope) {
                    override fun onParentConflict(destinationFolder: DocumentFile, action: ParentFolderConflictAction, canMerge: Boolean) {
                        action.confirmResolution(ConflictResolution.REPLACE)
                    }
                }
            ).collect { result ->
                if (result is SingleFolderResult.Completed || result is SingleFolderResult.Error) {
                    countDownLatch.countDown()
                }
            }
            countDownLatch.await()

            FileRepairUtil.repairCopiedDatabaseFiles(tempDestDir)
            currentDestPath = tempDestDir.absolutePath

            withContext(Dispatchers.Main) {
                internalLoadAndRender(context, currentDestPath)
            }
        }
    }

    fun confirmStitch(context: Context) {
        val start = sourceSelectionStart ?: return
        val end = sourceSelectionEnd ?: return
        val target = pasteTargetPoint ?: return

        val chunkMinX = min(start.first, end.first) shr 4
        val chunkMinZ = min(start.second, end.second) shr 4
        val chunkMaxX = max(start.first, end.first) shr 4
        val chunkMaxZ = max(start.second, end.second) shr 4

        val targetChunkX = target.first shr 4
        val targetChunkZ = target.second shr 4

        val offsetX = targetChunkX - chunkMinX
        val offsetZ = targetChunkZ - chunkMinZ

        previewState = PreviewState.STITCHING
        isStitching = true
        stitchSuccess = false
        stitchError = null

        val rootDir = getWorldsDir(context)
        val sourceInternalPath = File(rootDir, "stitch_source").absolutePath

        val inputData = Data.Builder()
            .putString("sourcePath", sourceInternalPath)
            .putString("destPath", currentDestPath)
            .putString("dimension", selectedDimension.getIdentifier())
            .putInt("minX", chunkMinX)
            .putInt("minZ", chunkMinZ)
            .putInt("maxX", chunkMaxX)
            .putInt("maxZ", chunkMaxZ)
            .putInt("offsetX", offsetX)
            .putInt("offsetZ", offsetZ)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME", context.packageName)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME", "androidx.work.multiprocess.RemoteWorkerService")
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StitchWorker>().setInputData(inputData).build()
        RemoteWorkManager.getInstance(context).enqueueUniqueWork("stitch_work", ExistingWorkPolicy.REPLACE, workRequest)

        viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    val rawProgress = workInfo.progress.getFloat("progress", 0f)
                    stitchProgress = rawProgress / 10f
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        isStitching = false
                        if (currentDestPath.endsWith("stitch_dest_temp")) {
                            val outputDir = File(rootDir, "world_output")
                            outputDir.deleteRecursively()
                            File(currentDestPath).renameTo(outputDir)
                        }
                        stitchSuccess = true
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        isStitching = false
                        stitchError = workInfo.outputData.getString("error") ?: "核心缝合引擎异常"
                    }
                }
            }
        }
    }

    fun loadAndRenderWorld(context: Context, docFolder: DocumentFile?, useFtpInput: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootDir = getWorldsDir(context)
            val sourceInternal = File(rootDir, "stitch_source")
            
            withContext(Dispatchers.Main) {
                previewState = PreviewState.IDLE
                isLoading = true
                statusMessage = "正在迁移源世界作为裁剪底图..."
            }

            if (!useFtpInput && docFolder != null) {
                sourceInternal.deleteRecursively()
                sourceInternal.mkdirs()
                val countDownLatch = java.util.concurrent.CountDownLatch(1)
                docFolder.copyFolderTo(
                    context = context, targetParentFolder = DocumentFile.fromFile(rootDir),
                    newFolderNameInTargetPath = sourceInternal.name, skipEmptyFiles = false,
                    onConflict = object : SingleFolderConflictCallback(viewModelScope) {
                        override fun onParentConflict(d: DocumentFile, a: ParentFolderConflictAction, m: Boolean) {
                            a.confirmResolution(ConflictResolution.REPLACE)
                        }
                    }
                ).collect { if (it is SingleFolderResult.Completed || it is SingleFolderResult.Error) countDownLatch.countDown() }
                countDownLatch.await()
                FileRepairUtil.repairCopiedDatabaseFiles(sourceInternal)
            } else if (useFtpInput) {
                val ftpDir = File(rootDir, "world_input")
                sourceInternal.deleteRecursively()
                ftpDir.renameTo(sourceInternal)
            }

            withContext(Dispatchers.Main) {
                internalLoadAndRender(context, sourceInternal.absolutePath)
            }
        }
    }

    private suspend fun internalLoadAndRender(context: Context, path: String) {
        isLoading = true
        isLoaded = false
        isMapCentered = false
        mapScale = 1f
        mapOffset = Offset.Zero
        regionBitmaps.clear()
        regionRGBAData.clear()
        availableDimensions.clear()
        worldDirUri = path

        val cacheMapDir = File(context.cacheDir, "map_regions")
        cacheMapDir.deleteRecursively()
        cacheMapDir.mkdirs()

        val inputData = Data.Builder()
            .putString("worldDirUri", path)
            .putString("outputPath", cacheMapDir.absolutePath)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME", context.packageName)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME", "androidx.work.multiprocess.RemoteWorkerService")
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MapPreviewWorker>().setInputData(inputData).build()
        RemoteWorkManager.getInstance(context).enqueueUniqueWork("map_preview", ExistingWorkPolicy.REPLACE, workRequest)

        viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    if (workInfo.state == WorkInfo.State.RUNNING) {
                        val progStatus = workInfo.progress.getString("status")
                        if (progStatus != null && progStatus == "FLUSHED") {
                            statusMessage = "正在读取区块并构建图像数据..."
                            loadBitmapsFromDir(cacheMapDir)
                        }
                    } else if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        isBedrock = workInfo.outputData.getBoolean("isBedrock", false)
                        loadBitmapsFromDir(cacheMapDir)
                        isLoading = false
                        isLoaded = true
                        statusMessage = "预览加载完成！"
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        isLoading = false
                        statusMessage = "解析出错：" + (workInfo.outputData.getString("error") ?: "未知")
                    }
                }
            }
        }
    }

    private fun loadBitmapsFromDir(cacheDir: File) {
        val files = cacheDir.listFiles { _, name -> name.endsWith(".bin") } ?: return
        for (file in files) {
            val parts = file.nameWithoutExtension.split("_")
            if (parts.size < 3) continue
            val rx = parts[parts.size - 2].toInt()
            val rz = parts[parts.size - 1].toInt()
            val dimId = parts.dropLast(2).joinToString("_").replaceFirst("_", ":")

            // 核心修复：使用 getDimensions()
            val dimension = DimensionRegistry().getDimensions().find { it.getIdentifier() == dimId } ?: Dimension.OVERWORLD

            val dimRegion = Pair(dimension, RegionCoordPair(rx, rz))
            if (regionBitmaps.containsKey(dimRegion)) continue

            val pixels = IntArray(512 * 512)
            try {
                DataInputStream(FileInputStream(file).buffered()).use { dis ->
                    for (i in pixels.indices) pixels[i] = dis.readInt()
                }
                val bitmap = Bitmap.createBitmap(pixels, 512, 512, Bitmap.Config.ARGB_8888)
                regionBitmaps[dimRegion] = bitmap
                if (!availableDimensions.contains(dimension)) availableDimensions.add(dimension)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun checkExistingFtpInput(context: Context) {
        val rootDir = getWorldsDir(context)
        val inputDir = File(rootDir, "world_input")
        hasExistingFtpInput = inputDir.exists() && (inputDir.listFiles()?.isNotEmpty() == true)
    }

    fun openChunkNbt(chunk: ChunkCoordPair, isEntity: Boolean, navigator: me.voltual.vb.ui.Navigator) {
        navigator.navigate(
            me.voltual.vb.ui.ChunkNbtEditorDest(worldDirUri, chunk.chunkX(), chunk.chunkZ(), selectedDimension.getIdentifier(), isEntity, isBedrock)
        )
    }

    suspend fun deleteSelectedChunks(context: Context, startBlock: Pair<Int, Int>, endBlock: Pair<Int, Int>): Int = withContext(Dispatchers.IO) {
        val chunkMinX = min(startBlock.first, endBlock.first) shr 4
        val chunkMinZ = min(startBlock.second, endBlock.second) shr 4
        val chunkMaxX = max(startBlock.first, endBlock.first) shr 4
        val chunkMaxZ = max(startBlock.second, endBlock.second) shr 4

        val inputData = Data.Builder()
            .putString("worldDirUri", worldDirUri)
            .putBoolean("isBedrock", isBedrock)
            .putString("dimension", selectedDimension.getIdentifier())
            .putInt("minX", chunkMinX)
            .putInt("minZ", chunkMinZ)
            .putInt("maxX", chunkMaxX)
            .putInt("maxZ", chunkMaxZ)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME", context.packageName)
            .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME", "androidx.work.multiprocess.RemoteWorkerService")
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MapDeleteWorker>().setInputData(inputData).build()
        RemoteWorkManager.getInstance(context).enqueueUniqueWork("map_delete", ExistingWorkPolicy.REPLACE, workRequest)

        var deleted = 0
        WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                deleted = workInfo.outputData.getInt("deletedCount", 0)
                if (deleted > 0) eraseChunksFromBitmaps(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)
                return@collect
            }
        }
        return@withContext deleted
    }

    suspend fun deleteChunk(context: Context, chunk: ChunkCoordPair, dimension: Dimension): Boolean {
        val count = deleteSelectedChunks(context, Pair(chunk.chunkX() shl 4, chunk.chunkZ() shl 4), Pair(chunk.chunkX() shl 4, chunk.chunkZ() shl 4))
        return count > 0
    }

    private suspend fun eraseChunksFromBitmaps(minX: Int, minZ: Int, maxX: Int, maxZ: Int) {
        withContext(Dispatchers.Main) {
            val paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
            for (cx in minX..maxX) {
                for (cz in minZ..maxZ) {
                    val dimRegion = Pair(selectedDimension, ChunkCoordPair(cx, cz).region)
                    val originalBitmap = regionBitmaps[dimRegion]
                    if (originalBitmap != null) {
                        val mutableBmp = if (originalBitmap.isMutable) originalBitmap else originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        val canvas = Canvas(mutableBmp)
                        val startX = (cx and 31) * 16f
                        val startZ = (cz and 31) * 16f
                        canvas.drawRect(startX, startZ, startX + 16f, startZ + 16f, paint)
                        regionBitmaps.remove(dimRegion)
                        regionBitmaps[dimRegion] = mutableBmp
                    }
                }
            }
        }
    }
}