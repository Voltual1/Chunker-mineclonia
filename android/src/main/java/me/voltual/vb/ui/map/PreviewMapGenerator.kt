package me.voltual.vb.ui.map

import android.graphics.Bitmap
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair

/**
 * 预览图渲染器：负责把 Chunker 提取出的 Region RGBA 缓冲区，映射为 Android 平台的 Bitmap
 */
object PreviewMapGenerator {

    /**
     * 将 Chunker 的区域像素表转换为 Android 原生 Bitmap (移除了 AWT 依赖)
     * @param regionRGBA 某个 Region 坐标下包含的全部已加载 Chunker 像素数据
     */
    fun generateRegionBitmap(regionRGBA: Map<ChunkCoordPair, IntArray>): Bitmap {
        val width = 512
        val height = 512
        val mergedPixels = IntArray(width * height)

        for ((chunkCoord, argbArray) in regionRGBA) {
            if (argbArray.isEmpty()) continue
            val startX = (chunkCoord.chunkX() and 31) shl 4
            val startY = (chunkCoord.chunkZ() and 31) shl 4
            
            for (cz in 0 until 16) {
                for (cx in 0 until 16) {
                    // 直接将子区块的像素填入 512x512 的大数组中
                    mergedPixels[(startY + cz) * width + (startX + cx)] = argbArray[cz * 16 + cx]
                }
            }
        }

        // 使用 Android 原生接口创建 Bitmap
        return Bitmap.createBitmap(mergedPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}