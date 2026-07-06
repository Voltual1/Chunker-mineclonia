// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.map

import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Version
import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter
import com.hivemc.chunker.conversion.encoding.base.writer.LevelWriter
import com.hivemc.chunker.conversion.encoding.base.writer.WorldWriter
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import com.hivemc.chunker.scheduling.task.FutureTask
import com.hivemc.chunker.scheduling.task.Task
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate

/**
 * 拦截 Chunker 转换器流式读取到的区块，提取最高方块像素色值并传回给 UI。
 * 修复：携带 Dimension 维度上下文。
 */
class ComposeMapPreviewWriter(
    private val onColumnRendered: (Dimension, RegionCoordPair, ChunkCoordPair, IntArray) -> Unit,
    private val onFlushRegion: (Dimension, RegionCoordPair) -> Unit
) : LevelWriter {

    override fun writeLevel(chunkerLevel: ChunkerLevel?): WorldWriter {
        return object : WorldWriter {
            override fun writeWorld(chunkerWorld: ChunkerWorld?): WorldWriter {
                // 无法在 WorldWriter 级别直接转换，但我们需要捕获维度以供 ColumnWriter 使用
                val currentDimension = chunkerWorld?.dimension ?: Dimension.OVERWORLD

                return object : ColumnWriter {
                    override fun writeColumn(chunkerColumn: ChunkerColumn): Task<Void> {
                        val argb = IntArray(256)
                        var hasContent = false

                        for (x in 0 until 16) {
                            for (z in 0 until 16) {
                                val block = chunkerColumn.getHighestBlock(x, z, Predicate { identifier ->
                                    identifier.hasRGBColor()
                                })
                                if (block != null) {
                                    hasContent = true
                                    val rgb = block.value().rgbColor
                                    argb[(z shl 4) or x] = if (rgb == 0) 0 else (0xFF000000.toInt() or rgb)
                                }
                            }
                        }

                        if (hasContent) {
                            onColumnRendered(
                                currentDimension,
                                chunkerColumn.position.region,
                                chunkerColumn.position,
                                argb
                            )
                        }

                        return FutureTask(CompletableFuture.completedFuture(null))
                    }

                    override fun flushRegion(regionCoordPair: RegionCoordPair) {
                        onFlushRegion(currentDimension, regionCoordPair)
                    }
                }
            }
        }
    }

    override fun getEncodingType(): EncodingType = EncodingType.PREVIEW
    override fun getVersion(): Version = Version(1, 0, 0)
    override fun getSupportedBiomes(): Set<ChunkerBiome.ChunkerVanillaBiome> = emptySet()
}