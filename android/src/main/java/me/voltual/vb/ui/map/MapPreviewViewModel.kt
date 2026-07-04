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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MapPreviewViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    // 存储 Region 坐标和生成的 Android Bitmap 的映射
    val regionBitmaps = mutableStateMapOf<RegionCoordPair, Bitmap>()

    var statusMessage by mutableStateOf("")
        private set

    /**
     * 异步分析指定存档的区域并渲染生成所有的 Region 预览图
     * @param chunksData 模拟 Chunker 加载进来的内存列集
     */
    fun loadAndRenderPreview(columns: List<ChunkerColumn>) {
        viewModelScope.launch {
            isLoading = true
            statusMessage = "正在解析世界预览数据..."
            
            withContext(Dispatchers.Default) {
                // 模拟 Chunker 列分类组合
                val groupedRGBA = ConcurrentHashMap<RegionCoordPair, ConcurrentHashMap<ChunkCoordPair, IntArray>>()

                columns.forEach { col ->
                    val argb = IntArray(256)
                    var hasContent = false
                    for (x in 0 until 16) {
                        for (z in 0 until 16) {
                            val block = col.getHighestBlock(x, z) { it.hasRGBColor() }
                            if (block != null) {
                                hasContent = true
                                val rgb = block.value().rgbColor
                                argb[(z shl 4) or x] = if (rgb == 0) 0 else (0xFF000000.toInt() or rgb)
                            }
                        }
                    }

                    if (hasContent) {
                        val regionPos = col.position.region
                        val regionMap = groupedRGBA.computeIfAbsent(regionPos) { ConcurrentHashMap() }
                        regionMap[col.position] = argb
                    }
                }

                // 在主线程更新生成的 Bitmaps
                groupedRGBA.forEach { (regionCoord, chunkMap) ->
                    val bitmap = PreviewMapGenerator.generateRegionBitmap(chunkMap)
                    withContext(Dispatchers.Main) {
                        regionBitmaps[regionCoord] = bitmap
                    }
                }
            }

            statusMessage = "生成了 ${regionBitmaps.size} 个区域的预览图"
            isLoading = false
        }
    }
}