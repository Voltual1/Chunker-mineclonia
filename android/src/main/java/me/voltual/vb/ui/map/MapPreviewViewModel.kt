// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it。
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
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld
import com.hivemc.chunker.scheduling.task.Task
import com.anggrayudi.storage.file.toRawFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MapPreviewViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    val regionBitmaps = mutableStateMapOf<RegionCoordPair, Bitmap>()
    private val regionRGBAData = ConcurrentHashMap<RegionCoordPair, ConcurrentHashMap<ChunkCoordPair, IntArray>>()

    var statusMessage by mutableStateOf("")
        private set

    /**
     * 打开本地存档并解析生成渲染图
     */
    fun loadAndRenderWorld(context: Context, docFolder: DocumentFile) {
        viewModelScope.launch {
            isLoading = true
            regionBitmaps.clear()
            regionRGBAData.clear()
            statusMessage = "正在检测存档类型..."

            withContext(Dispatchers.Default) {
                val worldDirectory = docFolder.toRawFile(context)
                if (worldDirectory == null || !worldDirectory.exists()) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "无法访问存档路径！"
                        isLoading = false
                    }
                    return@withContext
                }

                // 创建 Chunker 转换器存根 Stub
                val converterStub = object : Converter {
                    override fun shouldProcessIncompleteChunks(): Boolean = false
                    override fun getUnmappedBlockHandler(): Any? = null
                    override fun getUnmappedItemHandler(): Any? = null
                    override fun logMissingMapping(p0: String?, p1: String?) {}
                }

                // 探测 LevelReader
                val readerOpt = EncodingType.findReader(worldDirectory, converterStub)
                if (!readerOpt.isPresent) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "未检测到支持的 Java 或 Bedrock 存档！"
                        isLoading = false
                    }
                    return@withContext
                }

                val levelReader = readerOpt.get()
                withContext(Dispatchers.Main) {
                    statusMessage = "检测到 ${levelReader.encodingType.name} 格式 (版本: ${levelReader.version})"
                }

                // 自定义预览写入管线，将生成的每一列像素注入我们 ViewModel 维护的 regionRGBAData 中
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

                // 启动 Chunker 数据转换传输管线
                try {
                    levelReader.readLevel(object : LevelConversionHandler {
                        override fun readLevel(chunkerLevel: ChunkerLevel): WorldConversionHandler {
                            val worldWriter = previewWriter.writeLevel(chunkerLevel)
                            return object : WorldConversionHandler {
                                override fun readWorld(chunkerWorld: ChunkerWorld): ColumnConversionHandler {
                                    val columnWriter = worldWriter.writeWorld(chunkerWorld)
                                    return object : ColumnConversionHandler {
                                        override fun readColumn(chunkerColumn: ChunkerColumn) {
                                            columnWriter.writeColumn(chunkerColumn)
                                        }

                                        override fun flushColumns() {
                                            columnWriter.flushColumns()
                                        }
                                    }
                                }

                                override fun flushWorld(p0: ChunkerWorld?) {
                                    worldWriter.flushWorld(p0)
                                }

                                override fun flushWorlds() {
                                    worldWriter.flushWorlds()
                                }
                            }
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
                }
            }

            isLoading = false
        }
    }
}