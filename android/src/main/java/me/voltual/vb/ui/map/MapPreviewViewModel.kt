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
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Converter
import com.hivemc.chunker.conversion.encoding.base.Version
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

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
        private set        
        
    /**
     * 响应用户点击，通过 Navigator 跳转到区块 NBT 编辑界面
     */
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

    fun loadAndRenderWorld(context: Context, docFolder: DocumentFile) {
        viewModelScope.launch {
            isLoading = true
            regionBitmaps.clear()
            regionRGBAData.clear()
            statusMessage = "正在迁移世界存档至高速缓存以防读写受阻..."

            withContext(Dispatchers.Default) {
                // 1. 本地内部存储 worlds 路径准备
                val externalDir = context.getExternalFilesDir(null)
                val worldsDir = if (externalDir != null) {
                    File(externalDir, "worlds")
                } else {
                    File(context.filesDir, "worlds")
                }
                val localInputPath = File(worldsDir, "world_input")
                
                // 清理旧缓存并使用 SimpleStorage 复制
                if (localInputPath.exists()) {
                    localInputPath.deleteRecursively()
                }
                localInputPath.mkdirs()
                
                // 执行内部零 SAF 限制的高速拷贝
                val targetParentDoc = DocumentFile.fromFile(worldsDir)
                val countDownLatch = java.util.concurrent.CountDownLatch(1)
                var copyError = false

                viewModelScope.launch(Dispatchers.Main) {
                    docFolder.copyFolderTo(
                        context = context,
                        targetParentFolder = targetParentDoc,
                        skipEmptyFiles = false,
                        newFolderNameInTargetPath = "world_input",
                        onConflict = object : com.anggrayudi.storage.callback.SingleFolderConflictCallback(viewModelScope) {
                            override fun onParentConflict(
                                destinationFolder: DocumentFile,
                                action: ParentFolderConflictAction,
                                canMerge: Boolean
                            ) {
                                action.confirmResolution(ConflictResolution.REPLACE)
                            }
                        }
                    ).collect { result: SingleFolderResult ->
                        when (result) {
                            is SingleFolderResult.Completed -> {
                                countDownLatch.countDown()
                            }
                            is SingleFolderResult.Error -> {
                                copyError = true
                                countDownLatch.countDown()
                            }
                            else -> {}
                        }
                    }
                }

                // 阻断等待复制任务结束
                countDownLatch.await()

                if (copyError) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "存档移动至高速缓存失败"
                        isLoading = false
                    }
                    return@withContext
                }

                // 2. 转换为内部物理绝对路径，彻底解脱 SAF
                val worldDirectory = localInputPath
                worldDirUri = localInputPath.absolutePath

                if (!worldDirectory.exists()) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "高速缓存目录异常！"
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

                val readerOpt = EncodingType.findReader(worldDirectory, converterStub)
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
            statusMessage = "预览加载完成！(共 ${regionBitmaps.size} 个区域)"
        }
    }
}