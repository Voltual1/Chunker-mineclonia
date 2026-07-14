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
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.FileRepairUtil
import me.voltual.vb.ui.stitch.StitchWorker
import com.hivemc.chunker.conversion.encoding.EncodingType
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.floor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import me.voltual.vb.data.ChunkerSettingsDataStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class PreviewState {
    IDLE,
    SOURCE_SELECT,
    DEST_PASTE,
    STITCHING
}

class MapPreviewViewModel : ViewModel(), KoinComponent {

    private val chunkerSettingsDataStore: ChunkerSettingsDataStore by inject()

    var previewState by mutableStateOf(PreviewState.IDLE)
        private set

    var isLoading by mutableStateOf(false)
        private set

    val availableDimensions = mutableStateListOf<Dimension>()
    var selectedDimension by mutableStateOf(Dimension.OVERWORLD)

    val regionBitmaps = mutableStateMapOf<Pair<Dimension, RegionCoordPair>, Bitmap>()
    
    val litRegions = mutableStateListOf<Pair<Dimension, RegionCoordPair>>()

    var mapUpdateTrigger by mutableStateOf(0)
        private set

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

    // 存储块坐标 (Block Coordinates)
    var sourceSelectionStart by mutableStateOf<Pair<Int, Int>?>(null)
    var sourceSelectionEnd by mutableStateOf<Pair<Int, Int>?>(null)
    var pasteTargetPoint by mutableStateOf<Pair<Int, Int>?>(null)

    var isStitching by mutableStateOf(false)
    var stitchProgress by mutableStateOf(0f)
    var stitchSuccess by mutableStateOf(false)
    var stitchError by mutableStateOf<String?>(null)

    val localTargetWorlds = mutableStateListOf<File>()
    var currentDestPath by mutableStateOf("")
    
    var stitchSourceUri by mutableStateOf("")
        private set

    fun clearSelection() {
        sourceSelectionStart = null
        sourceSelectionEnd = null
        pasteTargetPoint = null
        stitchSourceUri = ""
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

    fun abortPasting(context: Context) {
        previewState = PreviewState.IDLE
        pasteTargetPoint = null
        if (stitchSourceUri.isNotEmpty() && stitchSourceUri != worldDirUri) {
            viewModelScope.launch(Dispatchers.Main) {
                internalLoadAndRender(context, stitchSourceUri)
            }
        }
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
                it.name != "world_preview" && 
                it.name != "world_output" && 
                it.name != "world_input" 
            }
        )
    }

    fun selectExistingLocalTarget(context: Context, targetFolder: File) {
        stitchSourceUri = worldDirUri
        currentDestPath = targetFolder.absolutePath
        previewState = PreviewState.DEST_PASTE
        viewModelScope.launch(Dispatchers.Main) {
            internalLoadAndRender(context, currentDestPath)
        }
    }

    fun copyExternalTargetToTemp(context: Context, destDoc: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootDir = getWorldsDir(context)
            val outputDir = File(rootDir, "world_output")
            outputDir.deleteRecursively()
            outputDir.mkdirs()

            withContext(Dispatchers.Main) {
                stitchSourceUri = worldDirUri
                previewState = PreviewState.DEST_PASTE
                isLoading = true
                statusMessage = "正在迁移并处理外部底层世界..."
            }

            val countDownLatch = java.util.concurrent.CountDownLatch(1)
            destDoc.copyFolderTo(
                context = context,
                targetParentFolder = DocumentFile.fromFile(rootDir),
                newFolderNameInTargetPath = outputDir.name,
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

            FileRepairUtil.repairCopiedDatabaseFiles(outputDir)
            currentDestPath = outputDir.absolutePath

            withContext(Dispatchers.Main) {
                internalLoadAndRender(context, currentDestPath)
            }
        }
    }

    fun confirmStitch(context: Context) {
        val start = sourceSelectionStart ?: return
        val end = sourceSelectionEnd ?: return
        val target = pasteTargetPoint ?: return

        // 细化到 Chunk 坐标
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

        val sourceInternalPath = stitchSourceUri.ifEmpty { worldDirUri }

        val inputData = workDataOf(
            "sourcePath" to sourceInternalPath,
            "destPath" to currentDestPath,
            "dimension" to selectedDimension.getIdentifier(),
            "minX" to chunkMinX,
            "minZ" to chunkMinZ,
            "maxX" to chunkMaxX,
            "maxZ" to chunkMaxZ,
            "offsetX" to offsetX,
            "offsetZ" to offsetZ
        )

        val workRequest = OneTimeWorkRequestBuilder<StitchWorker>().setInputData(inputData).build()
        WorkManager.getInstance(context).enqueueUniqueWork("stitch_work", ExistingWorkPolicy.REPLACE, workRequest)

        viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    val rawProgress = workInfo.progress.getFloat("progress", 0f)
                    stitchProgress = rawProgress / 100f
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

    fun loadAndRenderWorld(context: Context, docFolder: DocumentFile?, useFtpInput: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootDir = getWorldsDir(context)
            
            if (useFtpInput) {
                withContext(Dispatchers.Main) {
                    previewState = PreviewState.IDLE
                    isLoading = true
                    statusMessage = "直接挂载中转站 (FTP) 数据流..."
                }

                val ftpDir = File(rootDir, "world_input")
                if (ftpDir.exists() && ftpDir.listFiles()?.isNotEmpty() == true) {
                    withContext(Dispatchers.Main) {
                        internalLoadAndRender(context, ftpDir.absolutePath)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        statusMessage = "FTP 存档内容未就绪！"
                        isLoading = false
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    previewState = PreviewState.IDLE
                    isLoading = true
                    statusMessage = "正在迁移外部源世界作为裁剪底图..."
                }

                if (docFolder != null) {
                    val previewDir = File(rootDir, "world_preview")
                    previewDir.deleteRecursively()
                    previewDir.mkdirs()
                    val countDownLatch = java.util.concurrent.CountDownLatch(1)
                    docFolder.copyFolderTo(
                        context = context, targetParentFolder = DocumentFile.fromFile(rootDir),
                        newFolderNameInTargetPath = previewDir.name, skipEmptyFiles = false,
                        onConflict = object : SingleFolderConflictCallback(viewModelScope) {
                            override fun onParentConflict(destinationFolder: DocumentFile, action: ParentFolderConflictAction, canMerge: Boolean) {
                                action.confirmResolution(ConflictResolution.REPLACE)
                            }
                        }
                    ).collect { if (it is SingleFolderResult.Completed || it is SingleFolderResult.Error) countDownLatch.countDown() }
                    countDownLatch.await()
                    FileRepairUtil.repairCopiedDatabaseFiles(previewDir)
                    
                    withContext(Dispatchers.Main) {
                        internalLoadAndRender(context, previewDir.absolutePath)
                    }
                }
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
        litRegions.clear()
        availableDimensions.clear()
        worldDirUri = path

        val cacheMapDir = File(context.cacheDir, "map_regions")
        cacheMapDir.deleteRecursively()
        cacheMapDir.mkdirs()

        val modeEnergySaving = chunkerSettingsDataStore.energySavingMode.first()

        if (modeEnergySaving) {
            isLoaded = true
            isLoading = false
            statusMessage = "【节能模式开启】点哪里，亮哪里！"
            
            val tempDetectConverter = com.hivemc.chunker.conversion.WorldConverter(java.util.UUID.randomUUID())
            val readerOptional = EncodingType.findReader(File(path), tempDetectConverter)
            if (readerOptional.isPresent) {
                isBedrock = readerOptional.get().encodingType == EncodingType.BEDROCK
            }

            availableDimensions.addAll(listOf(Dimension.OVERWORLD, Dimension.NETHER, Dimension.THE_END))
            isMapCentered = false
            return
        }

        val pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isLoading) {
                delay(400)
                loadBitmapsFromDir(cacheMapDir)
            }
            loadBitmapsFromDir(cacheMapDir)
        }

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
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        isBedrock = workInfo.outputData.getBoolean("isBedrock", false)
                        isLoading = false
                        isLoaded = true
                        statusMessage = "预览图已构建完毕！"
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        isLoading = false
                        statusMessage = "解析出错：" + (workInfo.outputData.getString("error") ?: "未知")
                    }
                }
            }
        }
    }

    fun lightUpRegionOnDemand(context: Context, dimension: Dimension, region: RegionCoordPair) {
        val dimRegion = Pair(dimension, region)
        if (litRegions.contains(dimRegion) || isLoading) return

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLoading = true
                statusMessage = "正在局部按需提取 Region (${region.regionX()}, ${region.regionZ()}) ..."
            }

            val cacheMapDir = File(context.cacheDir, "map_regions")
            val inputData = Data.Builder()
                .putString("worldDirUri", worldDirUri)
                .putString("outputPath", cacheMapDir.absolutePath)
                .putString("targetDimension", dimension.getIdentifier())
                .putInt("targetRegionX", region.regionX())
                .putInt("targetRegionZ", region.regionZ())
                .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME", context.packageName)
                .putString("androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME", "androidx.work.multiprocess.RemoteWorkerService")
                .build()

            val workRequest = OneTimeWorkRequestBuilder<MapPreviewWorker>().setInputData(inputData).build()
            RemoteWorkManager.getInstance(context).enqueueUniqueWork("region_lightup_${region.regionX()}_${region.regionZ()}", ExistingWorkPolicy.REPLACE, workRequest)

            val workManager = WorkManager.getInstance(context)
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    loadBitmapsFromDir(cacheMapDir)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        litRegions.add(dimRegion)
                        statusMessage = "点亮完成！"
                    }
                }
            }
        }
    }

    private suspend fun loadBitmapsFromDir(cacheDir: File) = withContext(Dispatchers.IO) {
        val files = cacheDir.listFiles { _, name -> name.endsWith(".bin") } ?: return@withContext
        for (file in files) {
            val parts = file.nameWithoutExtension.split("_")
            if (parts.size < 3) continue
            val rx = parts[parts.size - 2].toInt()
            val rz = parts[parts.size - 1].toInt()
            val dimId = parts.dropLast(2).joinToString("_").replaceFirst("_", ":")

            val dimension = DimensionRegistry().getDimensions().find { it.getIdentifier() == dimId } ?: Dimension.OVERWORLD
            val dimRegion = Pair(dimension, RegionCoordPair(rx, rz))
            
            if (regionBitmaps.containsKey(dimRegion)) continue

            val pixels = IntArray(512 * 512)
            try {
                DataInputStream(FileInputStream(file).buffered()).use { dis ->
                    for (i in pixels.indices) pixels[i] = dis.readInt()
                }
                val bitmap = Bitmap.createBitmap(pixels, 512, 512, Bitmap.Config.ARGB_8888)
                
                withContext(Dispatchers.Main) {
                    regionBitmaps[dimRegion] = bitmap
                    if (!availableDimensions.contains(dimension)) availableDimensions.add(dimension)
                    if (!litRegions.contains(dimRegion)) litRegions.add(dimRegion)
                    mapUpdateTrigger++
                }
            } catch (e: Exception) { 
                e.printStackTrace() 
            }
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

    fun deleteSelectedChunksOptimistic(context: Context, startBlock: Pair<Int, Int>, endBlock: Pair<Int, Int>, onStitchScheduled: () -> Unit) {
        val chunkMinX = min(startBlock.first, endBlock.first) shr 4
        val chunkMinZ = min(startBlock.second, endBlock.second) shr 4
        val chunkMaxX = max(startBlock.first, endBlock.first) shr 4
        val chunkMaxZ = max(startBlock.second, endBlock.second) shr 4

        viewModelScope.launch(Dispatchers.Main) {
            eraseChunksFromBitmaps(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)
            onStitchScheduled()

            withContext(Dispatchers.IO) {
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
            }
        }
    }

    suspend fun deleteChunk(context: Context, chunk: ChunkCoordPair, dimension: Dimension): Boolean {
        var success = false
        val countDownLatch = java.util.concurrent.CountDownLatch(1)
        deleteSelectedChunksOptimistic(context, Pair(chunk.chunkX() shl 4, chunk.chunkZ() shl 4), Pair(chunk.chunkX() shl 4, chunk.chunkZ() shl 4)) {
            success = true
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return success
    }

    private suspend fun eraseChunksFromBitmaps(minX: Int, minZ: Int, maxX: Int, maxZ: Int) {
        withContext(Dispatchers.Main) {
            val paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
            for (cx in minX..maxX) {
                for (cz in minZ..maxZ) {
                    val chunkCoord = ChunkCoordPair(cx, cz)
                    val dimRegion = Pair(selectedDimension, chunkCoord.region)
                    val originalBitmap = regionBitmaps[dimRegion]
                    if (originalBitmap != null) {
                        val mutableBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        val canvas = Canvas(mutableBmp)
                        // 计算在该 Region (512x512) 内部的局部坐标 (0-511)
                        val startXInRegion = (cx and 31) shl 4
                        val startZInRegion = (cz and 31) shl 4
                        canvas.drawRect(startXInRegion.toFloat(), startZInRegion.toFloat(), startXInRegion + 16f, startZInRegion + 16f, paint)
                        regionBitmaps[dimRegion] = mutableBmp
                    }
                }
            }
            mapUpdateTrigger++
        }
    }
}