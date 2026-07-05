// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPreviewScreen(
    initialFolderUri: String,
    modifier: Modifier = Modifier,
    viewModel: MapPreviewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var currentUri by remember { mutableStateOf(initialFolderUri) }

    // 将 Android Bitmap 转换为 Compose 适用的 ImageBitmap 以利用硬件加速
    val composeBitmaps = remember { mutableStateMapOf<RegionCoordPair, ImageBitmap>() }

    // 监听 ViewModel 中生成的 Bitmap 并缓存为 Compose ImageBitmap
    LaunchedEffect(viewModel.regionBitmaps.size) {
        viewModel.regionBitmaps.forEach { (region, bmp) ->
            if (!composeBitmaps.containsKey(region)) {
                composeBitmaps[region] = bmp.asImageBitmap()
            }
        }
    }

    val folderPicker = rememberLauncherForFolderPicker { folder ->
        currentUri = folder.uri.toString()
        composeBitmaps.clear()
        viewModel.loadAndRenderWorld(context, folder)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地图预览 (世界视图)") },
                actions = {
                    IconButton(onClick = { folderPicker.launch() }) {
                        Icon(Icons.Default.FolderOpen, "打开存档")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF202020)) // 类似 Blocktopograph 的深色底板
        ) {
            if (currentUri.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { folderPicker.launch() }) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("选择 Minecraft 存档文件夹")
                    }
                }
            } else {
                // UI 顶层加载指示器
                if (viewModel.isLoading) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), shape = MaterialTheme.shapes.medium)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(viewModel.statusMessage, fontSize = 12.sp)
                    }
                }

                // 交互式 2D 地图画布
                if (composeBitmaps.isNotEmpty()) {
                    InteractiveMapCanvas(
                        regionBitmaps = composeBitmaps,
                        viewportWidth = constraints.maxWidth.toFloat(),
                        viewportHeight = constraints.maxHeight.toFloat()
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveMapCanvas(
    regionBitmaps: Map<RegionCoordPair, ImageBitmap>,
    viewportWidth: Float,
    viewportHeight: Float
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isCentered by remember { mutableStateOf(false) }

    // 当有新区块加载进来、或是强制请求居中时，重新计算最佳视图与居中位置
    LaunchedEffect(regionBitmaps, isCentered) {
        if (regionBitmaps.isNotEmpty() && !isCentered) {
            val minX = regionBitmaps.keys.minOf { it.regionX() }
            val maxX = regionBitmaps.keys.maxOf { it.regionX() }
            val minZ = regionBitmaps.keys.minOf { it.regionZ() }
            val maxZ = regionBitmaps.keys.maxOf { it.regionZ() }

            val mapWidth = (maxX - minX + 1) * 512f
            val mapHeight = (maxZ - minZ + 1) * 512f

            // 计算适合屏幕的初始缩放比例
            val scaleX = viewportWidth / mapWidth
            val scaleY = viewportHeight / mapHeight
            scale = minOf(scaleX, scaleY).coerceIn(0.05f, 2f)

            // 计算地图坐标系中的绝对中心点
            val boundsCenterX = minX * 512f + mapWidth / 2f
            val boundsCenterZ = minZ * 512f + mapHeight / 2f

            // 将其平移至屏幕中心
            offset = Offset(
                viewportWidth / 2f - boundsCenterX * scale,
                viewportHeight / 2f - boundsCenterZ * scale
            )
            isCentered = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        // 允许最大放大 50 倍，最小缩小到 1%
                        scale = (scale * zoom).coerceIn(0.01f, 50f)
                        
                        // 围绕手势捏合中心进行平滑缩放与平移
                        offset = (offset + pan - centroid) * (scale / oldScale) + centroid
                    }
                }
        ) {
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // 遍历渲染所有的 Region 块到地图二维世界中！
                regionBitmaps.forEach { (region, bitmap) ->
                    drawImage(
                        image = bitmap,
                        topLeft = Offset(region.regionX() * 512f, region.regionZ() * 512f)
                    )
                }
            }
        }

        // 复位居中按钮
        FloatingActionButton(
            onClick = { isCentered = false },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = "居中地图")
        }
    }
}