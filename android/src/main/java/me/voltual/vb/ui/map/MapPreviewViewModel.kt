package me.voltual.vb.ui.map

import android.content.Context
import android.graphics.Bitmap
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
import com.anggrayudi.storage.callback.SingleFolderConflictCallback
import com.anggrayudi.storage.file.copyFolderTo
import com.anggrayudi.storage.result.SingleFolderResult
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Converter
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBChunkType
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey
import com.hivemc.chunker.conversion.handlers.LevelConversionHandler
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelSettings
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome
import com.hivemc.chunker.mapping.resolver.MappingsFileResolvers
import com.hivemc.chunker.scheduling.task.FutureTask
import com.hivemc.chunker.scheduling.task.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.FileRepairUtil
import me.voltual.vb.ui.stitch.StitchWorker
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
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

    // 选取坐标（方块系）
    var sourceSelectionStart by mutableStateOf<Pair<Int, Int>?>(null)
    var sourceSelectionEnd by mutableStateOf<Pair<Int, Int>?>(null)

    // 粘贴左上角坐标（方块系）
    var pasteTargetPoint by mutableStateOf<Pair<Int, Int>?>(null)

    var stitchProgress by mutableStateOf(0f)
    var stitchSuccess by mutableStateOf(false)
    var stitchError by mutableStateOf<String?>(null)

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

    // --- 补全：删除单个区块方法 ---
    suspend fun deleteChunk(chunk: ChunkCoordPair, dimension: Dimension): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isBedrock) {
                    val dbDir = File(worldDirUri, "db")
                    if (!dbDir.exists()) return@withContext false
                    
                    File(dbDir, "LOCK").delete()
                    val options = Options().createIfMissing(false)
                    Iq80DBFactory.factory.open(dbDir, options).use { db ->
                        val batch = db.createWriteBatch()
                        for (type in LevelDBChunkType.values()) {
                            if (type == LevelDBChunkType.SUB_CHUNK_PREFIX) {
                                for (y in -64..64) {
                                    batch.delete(LevelDBKey.key(dimension, chunk, y.toByte(), type))
                                }
                            } else {
                                batch.delete(LevelDBKey.key(dimension, chunk, type))
                            }
                        }
                        db.write(batch)
                    }
                } else {
                    val regionX = chunk.chunkX() shr 5
                    val regionZ = chunk.chunkZ() shr 5
                    val dimFolder = when (dimension) {
                        Dimension.NETHER -> "DIM-1"
                        Dimension.THE_END -> "DIM1"
                        else -> ""
                    }
                    val dirs = listOf("region", "entities", "poi")
                    var deletedAny = false
                    
                    dirs.forEach { dirName ->
                        val targetPath = if (dimFolder.isEmpty()) {
                            File(worldDirUri, "$dirName/r.$regionX.$regionZ.mca")
                        } else {
                            File(worldDirUri, "$dimFolder/$dirName/r.$regionX.$regionZ.mca")
                        }
                        
                        if (targetPath.exists()) {
                            RandomAccessFile(targetPath, "rw").use { raf ->
                                val index = (chunk.chunkX() and 31) + (chunk.chunkZ() and 31) * 32
                                raf.seek(index * 4L)
                                raf.writeInt(0)
                                deletedAny = true
                            }
                        }
                    }
                    if (!deletedAny) return@withContext false
                }

                val dimRegion = Pair(dimension, chunk.region)
                val chunkMap = regionRGBAData[dimRegion]
                if (chunkMap != null) {
                    chunkMap.remove(chunk)
                    val newBitmap = PreviewMapGenerator.generateRegionBitmap(chunkMap)
                    withContext(Dispatchers.Main) {
                        regionBitmaps[dimRegion] = newBitmap
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun prepareDestinationAndLoad(context: Context, destDoc: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootDir = getWorldsDir(context)
            val outputInternal = File(rootDir, "world_output")
            outputInternal.deleteRecursively()
            outputInternal.mkdirs()

            withContext(Dispatchers.Main) { 
                previewState = PreviewState.DEST_PASTE 
                isLoading = true
                statusMessage = "正在迁移目标存档作为粘贴底图..."
            }

            val countDownLatch = java.util.concurrent.CountDownLatch(1)
            destDoc.copyFolderTo(
                context = context,
                targetParentFolder = DocumentFile.fromFile(rootDir),
                newFolderNameInTargetPath = outputInternal.name,
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

            FileRepairUtil.repairCopiedDatabaseFiles(outputInternal)
            
            withContext(Dispatchers.Main) {
                internalLoadAndRender(context, outputInternal.absolutePath)
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
        stitchSuccess = false
        stitchError = null

        val rootDir = getWorldsDir(context)
        val sourceInternalPath = File(rootDir, "stitch_source").absolutePath
        val destInternalPath = File(rootDir, "world_output").absolutePath

        val inputData = workDataOf(
            "sourcePath" to sourceInternalPath,
            "destPath" to destInternalPath,
            "dimension" to selectedDimension.getIdentifier(),
            "minX" to chunkMinX,
            "minZ" to chunkMinZ,
            "maxX" to chunkMaxX,
            "maxZ" to chunkMaxZ,
            "offsetX" to offsetX,
            "offsetZ" to offsetZ
        )

        val workRequest = OneTimeWorkRequestBuilder<StitchWorker>().setInputData(inputData).build()
        WorkManager.getInstance(context).enqueue(workRequest)

        viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    stitchProgress = workInfo.progress.getFloat("progress", 0f)
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        stitchSuccess = true
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
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
                statusMessage = "正在迁移源存档作为裁剪底图..."
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

        withContext(Dispatchers.Default) {
            val finalWorldDirectory = File(path)
            val converterStub = object : Converter {
                override fun shouldLevelDBCompaction(): Boolean = false
                override fun shouldProcessMaps(): Boolean = false
                override fun shouldProcessItems(): Boolean = false
                override fun shouldProcessEntities(): Boolean = false
                override fun shouldProcessBlockEntities(): Boolean = false
                override fun shouldProcessLootTables(): Boolean = false
                override fun shouldProcessBiomes(): Boolean = false
                override fun shouldProcessHeightMap(): Boolean = false
                override fun shouldProcessColumnPreTransform(): Boolean = false
                override fun shouldProcessLighting(): Boolean = false
                override fun shouldProcessDimension(dimension: Dimension?): Boolean = true
                override fun shouldProcessRegion(dimension: Dimension?, regionPair: RegionCoordPair?): Boolean = true
                override fun shouldProcessColumn(dimension: Dimension?, columnPair: ChunkCoordPair?): Boolean = true
                override fun shouldAllowNBTCopying(): Boolean = false
                override fun shouldAllowCustomIdentifiers(): Boolean = false
                override fun getBlockMappings(): MappingsFileResolvers? = null
                override fun getDimensionRegistry(): DimensionRegistry = DimensionRegistry()
                override fun shouldDiscardEmptyChunks(): Boolean = true
                override fun shouldPreventYBiomeBlending(): Boolean = false
                override fun getNewDimension(dimension: Dimension?): Optional<Dimension> = Optional.ofNullable(dimension)
                override fun getNewBiome(biome: ChunkerBiome?): ChunkerBiome? = biome
                override fun level(): Optional<ChunkerLevel> = Optional.empty()
            }

            val readerOpt = EncodingType.findReader(finalWorldDirectory, converterStub)
            if (!readerOpt.isPresent) {
                withContext(Dispatchers.Main) { statusMessage = "未检测到支持的格式！"; isLoading = false }
                return@withContext
            }

            val levelReader = readerOpt.get()
            isBedrock = levelReader.encodingType == EncodingType.BEDROCK
            
            val previewWriter = ComposeMapPreviewWriter(
                onColumnRendered = { dimension, region, chunk, argb ->
                    viewModelScope.launch(Dispatchers.Main) {
                        if (availableDimensions.none { it.identifier == dimension.identifier }) {
                            availableDimensions.add(dimension)
                            if (availableDimensions.size == 1) selectedDimension = dimension
                        }
                    }
                    val dimRegion = Pair(dimension, region)
                    val chunksInRegion = regionRGBAData.computeIfAbsent(dimRegion) { ConcurrentHashMap() }
                    chunksInRegion[chunk] = argb
                },
                onFlushRegion = { dimension, region ->
                    val dimRegion = Pair(dimension, region)
                    val chunkMap = regionRGBAData[dimRegion]
                    if (chunkMap != null) {
                        val bitmap = PreviewMapGenerator.generateRegionBitmap(chunkMap)
                        viewModelScope.launch(Dispatchers.Main) { regionBitmaps[dimRegion] = bitmap }
                    }
                }
            )

            val environment = Task.environment("Map Preview Generation", maxOf(1, Runtime.getRuntime().availableProcessors() - 1), { it.printStackTrace() }, null)
            try {
                levelReader.readLevel(object : LevelConversionHandler {
                    override fun convertLevel(level: ChunkerLevel?): Task<WorldConversionHandler> {
                        val safeLevel = level ?: ChunkerLevel(ChunkerLevelSettings(), null, emptyList(), null, emptyList())
                        val worldWriter = previewWriter.writeLevel(safeLevel)
                        return FutureTask(CompletableFuture.completedFuture(object : WorldConversionHandler {
                            override fun convertWorld(world: ChunkerWorld?): Task<ColumnConversionHandler> {
                                val columnWriter = worldWriter.writeWorld(world ?: throw NullPointerException())
                                return FutureTask(CompletableFuture.completedFuture(object : ColumnConversionHandler {
                                    override fun convertColumn(column: ChunkerColumn?): Task<Void> = if (column != null) columnWriter.writeColumn(column) else FutureTask(CompletableFuture.completedFuture(null))
                                    override fun flushRegion(regionCoordPair: RegionCoordPair?): Task<Void> { if (regionCoordPair != null) columnWriter.flushRegion(regionCoordPair); return FutureTask(CompletableFuture.completedFuture(null)) }
                                    override fun flushColumns(): Task<Void> { columnWriter.flushColumns(); return FutureTask(CompletableFuture.completedFuture(null)) }
                                }))
                            }
                            override fun flushWorld(world: ChunkerWorld?) { worldWriter.flushWorld(world) }
                            override fun flushWorlds() { worldWriter.flushWorlds() }
                        }))
                    }
                    override fun flushLevel() { previewWriter.flushLevel() }
                })
                environment.future().get()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { statusMessage = "解析出错: ${e.localizedMessage}" }
            } finally { environment.close() }
        }

        isLoading = false
        isLoaded = true
        statusMessage = "预览加载完成！"
    }

    fun checkExistingFtpInput(context: Context) {
        val inputDir = File(getWorldsDir(context), "world_input")
        hasExistingFtpInput = inputDir.exists() && (inputDir.listFiles()?.isNotEmpty() == true)
    }

    fun openChunkNbt(chunk: ChunkCoordPair, isEntity: Boolean, navigator: me.voltual.vb.ui.Navigator) {
        navigator.navigate(
            me.voltual.vb.ui.ChunkNbtEditorDest(worldDirUri, chunk.chunkX(), chunk.chunkZ(), selectedDimension.getIdentifier(), isEntity, isBedrock)
        )
    }
}