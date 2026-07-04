// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.map

import android.graphics.Bitmap
import com.hivemc.chunker.conversion.encoding.preview.PreviewColumnWriter
import com.hivemc.chunker.conversion.encoding.preview.PreviewWorldWriter
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 预览图渲染器：负责把 Chunker 提取出的 Region RGBA 缓冲区，映射为 Android 平台的 Bitmap
 */
object PreviewMapGenerator {

    /**
     * 将 Chunker 的区域像素表转换为 Android 原生 Bitmap
     * @param regionRGBA 某个 Region 坐标下包含的全部已加载 Chunker 像素数据
     */
    fun generateRegionBitmap(regionRGBA: Map<ChunkCoordPair, IntArray>): Bitmap {
        // 每个 Region 由 32x32 个 Chunk 组成，每个 Chunk 为 16x16 像素，共 512x512
        val width = 512
        val height = 512
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        // 将每个区块的像素复制到 BufferedImage 中
        for ((chunkCoord, argbArray) in regionRGBA) {
            if (argbArray.isEmpty()) continue
            val startX = (chunkCoord.chunkX() and 31) shl 4
            val startY = (chunkCoord.chunkZ() and 31) shl 4
            
            bufferedImage.setRGB(
                startX,
                startY,
                16,
                16,
                argbArray,
                0,
                16
            )
        }

        // 从 BufferedImage 中提取合并后的 ARGB 像素数组
        val mergedPixels = IntArray(width * height)
        bufferedImage.getRGB(0, 0, width, height, mergedPixels, 0, width)

        // 在 Android 侧直接创建原生 Bitmap
        return Bitmap.createBitmap(mergedPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}