// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.map

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Converter
import com.hivemc.chunker.conversion.encoding.base.Version
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
import com.anggrayudi.storage.file.toRawFile
import com.anggrayudi.storage.file.copyFolderTo
import com.anggrayudi.storage.result.SingleFolderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.io.RandomAccessFile
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class MapPreviewViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    val regionBitmaps = mutableStateMapOf<RegionCoordPair, Bitmap>()
    private val regionRGBAData = ConcurrentHashMap<RegionCoordPair, ConcurrentHashMap<ChunkCoordPair, IntArray>>()

    var statusMessage by mutableStateOf("")
        private set
        
    var isBedrock by mutableStateOf(false)
        private set

    var worldDirUri by mutableStateOf("")
    
    var isLoaded by mutableStateOf(false)
        private set

    // 视口状态
    var mapScale by mutableStateOf(1f)
    var mapOffset by mutableStateOf(Offset.Zero)
    var isMapCentered by mutableStateOf(false)

    // 控制网格线绘制
    var showGrid by mutableStateOf(false)

    var hasExistingFtpInput by mutableStateOf(false)
        private set

    fun checkExistingFtpInput(context: Context) {
        val externalDir = context.getExternalFilesDir(null)
        val worldsDir = if (externalDir != null) File(externalDir, "worlds") else File(context.filesDir, "worlds")
        val inputDir = File(worldsDir, "world_input")
        hasExistingFtpInput = inputDir.exists() && (inputDir.listFiles()?.isNotEmpty() == true)
    }

    fun openChunkNbt(chunk: ChunkCoordPair, isEntity: Boolean, navigator: me.voltual.vb.ui.Navigator) {
        navigator.navigate(
            me.voltual.vb.ui.ChunkNbtEditorDest(
                worldDirUri = worldDirUri,
                chunkX = chunk.chunkX(),
                chunkZ = chunk.chunkZ(),
                isEntity = isEntity,
                isBedrock = isBedrock
            )
        )
    }

    /**
     * 在物理底层彻底抹除一个区块的所有数据，使 Minecraft 在下次进入时自动重生成
     */
    suspend fun deleteChunk(chunk: ChunkCoordPair): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isBedrock) {
                    val dbDir = File(worldDirUri, "db")
                    if (!dbDir.exists()) return@withContext false
                    
                    File(dbDir, "LOCK").delete()
                    val options = Options().createIfMissing(false)
                    Iq80DBFactory.factory.open(dbDir, options).use { db ->
                        val batch = db.createWriteBatch()
                        
                        // 遍历抹除所有基岩版区块的各种 Tag 层级数据
                        for (type in LevelDBChunkType.values()) {
                            if (type == LevelDBChunkType.SUB_CHUNK_PREFIX) {
                                // 垂直清空所有的地形 SubChunk (Y:-64..320)
                                for (y in -64..64) {
                                    batch.delete(LevelDBKey.key(Dimension.OVERWORLD, chunk, y.toByte(), type))
                                }
                            } else {
                                batch.delete(LevelDBKey.key(Dimension.OVERWORLD, chunk, type))
                            }
                        }
                        db.write(batch)
                    }
                } else {
                    // Java MCA 删除逻辑: 清零 FAT 表偏移
                    val regionX = chunk.chunkX() shr 5
                    val regionZ = chunk.chunkZ() shr 5
                    val dirs = listOf("region", "entities", "poi")
                    var deletedAny = false
                    
                    dirs.forEach { dirName ->
                        val mcaFile = File(worldDirUri, "$dirName/r.$regionX.$regionZ.mca")
                        if (mcaFile.exists()) {
                            RandomAccessFile(mcaFile, "rw").use { raf ->
                                val index = (chunk.chunkX() and 31) + (chunk.chunkZ() and 31) * 32
                                raf.seek(index * 4L)
                                raf.writeInt(0) // 将 Offset 和 Size 彻底归零
                                deletedAny = true
                            }
                        }
                    }
                    if (!deletedAny) return@withContext false
                }

                // 更新内存中的 UI 地图缓存，让被删除的区块立刻变成深渊色
                val regionPos = chunk.region
                val regionMap = regionRGBAData[regionPos]
                if (regionMap != null) {
                    regionMap.remove(chunk)
                    val newBitmap = PreviewMapGenerator.generateRegionBitmap(regionMap)
                    withContext(Dispatchers.Main) {
                        regionBitmaps[regionPos] = newBitmap
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun loadAndRenderWorld(context: Context, docFolder: DocumentFile?, useFtpInput: Boolean = false) {
        viewModelScope.launch {
            isLoading = true
            isLoaded = false
            isMapCentered = false
            mapScale = 1f
            mapOffset = Offset.Zero
            regionBitmaps.clear()
            regionRGBAData.clear()

            withContext(Dispatchers.Default) {
                val externalDir = context.getExternalFilesDir(null)
                val worldsDir = if (externalDir != null) File(externalDir, "worlds") else File(context.filesDir, "worlds")
                
                val finalWorldDirectory = if (useFtpInput) {
                    statusMessage = "正在直接读取中转站 (FTP) 存档数据..."
                    val ftpInputDir = File(worldsDir, "world_input")
                    worldDirUri = ftpInputDir.absolutePath
                    ftpInputDir
                } else {
                    if (docFolder == null) {
                        isLoading = false
                        return@withContext
                    }
                    statusMessage = "正在迁移世界存档至高速预览缓存以防读写受阻..."
                    val localPreviewPath = File(worldsDir, "world_preview")
                    if (localPreviewPath.exists()) {
                        localPreviewPath.deleteRecursively()
                    }
                    localPreviewPath.mkdirs()

                    val targetParentDoc = DocumentFile.fromFile(worldsDir)
                    val countDownLatch = java.util.concurrent.CountDownLatch(1)
                    var copyError = false

                    viewModelScope.launch(Dispatchers.IO) {
                        docFolder.copyFolderTo(
                            context = context,
                            targetParentFolder = targetParentDoc,
                            skipEmptyFiles = false,
                            newFolderNameInTargetPath = "world_preview",
                            onConflict = object : com.anggrayudi.storage.callback.SingleFolderConflictCallback(viewModelScope) {
                                override fun onParentConflict(
                                    destinationFolder: DocumentFile,
                                    action: ParentFolderConflictAction,
                                    canMerge: Boolean
                                ) {
                                    action.confirmResolution(ConflictResolution.REPLACE)
                                }
                            }
                        ).flowOn(Dispatchers.IO).collect { result: SingleFolderResult ->
                            when (result) {
                                is SingleFolderResult.Completed -> countDownLatch.countDown()
                                is SingleFolderResult.Error -> {
                                    copyError = true
                                    countDownLatch.countDown()
                                }
                                else -> {}
                            }
                        }
                    }

                    countDownLatch.await()

                    if (copyError) {
                        withContext(Dispatchers.Main) {
                            statusMessage = "存档移动至缓存失败"
                            isLoading = false
                        }
                        return@withContext
                    }

                    worldDirUri = localPreviewPath.absolutePath
                    localPreviewPath
                }

                if (!finalWorldDirectory.exists()) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "世界物理路径不存在！"
                        isLoading = false
                    }
                    return@withContext
                }

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
                    withContext(Dispatchers.Main) {
                        statusMessage = "未检测到支持的 Java 或 Bedrock 格式！"
                        isLoading = false
                    }
                    return@withContext
                }

                val levelReader = readerOpt.get()
                isBedrock = levelReader.encodingType == EncodingType.BEDROCK
                
                withContext(Dispatchers.Main) {
                    statusMessage = "检测到 ${levelReader.encodingType.name} 格式 (版本: ${levelReader.version})"
                }

                val previewWriter = ComposeMapPreviewWriter(
                    onColumnRendered = { region, chunk, argb ->
                        val chunksInRegion = regionRGBAData.computeIfAbsent(region) { ConcurrentHashMap() }
                        chunksInRegion[chunk] = argb
                    },
                    onFlushRegion = { region ->
                        val chunkMap = regionRGBAData[region]
                        if (chunkMap != null) {
                            val bitmap = PreviewMapGenerator.generateRegionBitmap(chunkMap)
                            viewModelScope.launch(Dispatchers.Main) {
                                regionBitmaps[region] = bitmap
                            }
                        }
                    }
                )

                val exceptionHandler = java.util.function.Consumer<Throwable> { it.printStackTrace() }
                val environment = Task.environment(
                    "Map Preview Generation",
                    maxOf(1, Runtime.getRuntime().availableProcessors() - 1),
                    exceptionHandler,
                    null
                )

                try {
                    levelReader.readLevel(object : LevelConversionHandler {
                        override fun convertLevel(level: ChunkerLevel?): Task<WorldConversionHandler> {
                            val safeLevel = level ?: ChunkerLevel(
                                ChunkerLevelSettings(),
                                null,
                                emptyList(),
                                null,
                                emptyList()
                            )
                            val worldWriter = previewWriter.writeLevel(safeLevel)
                            val worldHandler = object : WorldConversionHandler {
                                override fun convertWorld(world: ChunkerWorld?): Task<ColumnConversionHandler> {
                                    val safeWorld = world ?: throw NullPointerException("ChunkerWorld cannot be null")
                                    val columnWriter = worldWriter.writeWorld(safeWorld)
                                    val columnHandler = object : ColumnConversionHandler {
                                        override fun convertColumn(column: ChunkerColumn?): Task<Void> {
                                            if (column != null) {
                                                return columnWriter.writeColumn(column)
                                            }
                                            return FutureTask(CompletableFuture.completedFuture(null))
                                        }

                                        override fun flushRegion(regionCoordPair: RegionCoordPair?): Task<Void> {
                                            if (regionCoordPair != null) {
                                                columnWriter.flushRegion(regionCoordPair)
                                            }
                                            return FutureTask(CompletableFuture.completedFuture(null))
                                        }

                                        override fun flushColumns(): Task<Void> {
                                            columnWriter.flushColumns()
                                            return FutureTask(CompletableFuture.completedFuture(null))
                                        }
                                    }
                                    return FutureTask(CompletableFuture.completedFuture(columnHandler))
                                }

                                override fun flushWorld(world: ChunkerWorld?) {
                                    worldWriter.flushWorld(world)
                                }

                                override fun flushWorlds() {
                                    worldWriter.flushWorlds()
                                }
                            }
                            return FutureTask(CompletableFuture.completedFuture(worldHandler))
                        }

                        override fun flushLevel() {
                            previewWriter.flushLevel()
                        }
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        statusMessage = "解析出错: ${e.localizedMessage}"
                    }
                } finally {
                    environment.close()
                }

                try {
                    environment.future().get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            isLoading = false
            isLoaded = true
            statusMessage = "预览加载完成！(共 ${regionBitmaps.size} 个区域)"
        }
    }
}