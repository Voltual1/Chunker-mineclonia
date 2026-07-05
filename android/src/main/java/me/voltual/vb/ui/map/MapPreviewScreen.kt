// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.map

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import kotlinx.coroutines.launch
import me.voltual.vb.ui.LocalTopAppBarController
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPreviewScreen(
    initialFolderUri: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MapPreviewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val topAppBarController = LocalTopAppBarController.current

    val composeBitmaps = remember { mutableStateMapOf<RegionCoordPair, ImageBitmap>() }
    var selectedChunk by remember { mutableStateOf<ChunkCoordPair?>(null) }

    // 统一的文件选择器逻辑
    val folderPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.worldDirUri = folder.uri.toString()
        composeBitmaps.clear()
        selectedChunk = null
        viewModel.loadAndRenderWorld(context, folder)
    }

    // 初始化载入
    LaunchedEffect(initialFolderUri) {
        if (initialFolderUri.isNotEmpty() && viewModel.worldDirUri.isEmpty()) {
            viewModel.worldDirUri = initialFolderUri
            val doc = com.anggrayudi.storage.file.DocumentFileCompat.fromFullPath(context, initialFolderUri)
            if (doc != null) viewModel.loadAndRenderWorld(context, doc)
        }
    }

    // 同步渲染 ImageBitmap
    LaunchedEffect(viewModel.regionBitmaps.size) {
        viewModel.regionBitmaps.forEach { (region, bmp) ->
            if (!composeBitmaps.containsKey(region)) {
                composeBitmaps[region] = bmp.asImageBitmap()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold { padding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF202020))
            ) {
                if (viewModel.worldDirUri.isEmpty()) {
                    // 简约的空状态：中心提示并放置统一按钮
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "尚未选取存档世界",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { folderPicker.launch() },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("选取存档目录")
                        }
                    }
                } else {
                    // 已选存档后的 UI 展示
                    if (viewModel.isLoading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), shape = MaterialTheme.shapes.medium)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(viewModel.statusMessage, fontSize = 12.sp)
                        }
                    }

                    if (composeBitmaps.isNotEmpty()) {
                        InteractiveMapCanvas(
                            regionBitmaps = composeBitmaps,
                            viewportWidth = 2000f, // 占位，会在计算时自动适配父容器
                            viewportHeight = 2000f,
                            viewModel = viewModel,
                            onChunkTap = { chunkPair ->
                                selectedChunk = chunkPair
                            }
                        )
                    }
                }
            }
        }

        // 居中操作菜单
        ChunkActionMenu(
            chunk = selectedChunk,
            onDismiss = { selectedChunk = null },
            onAction = { action, chunkPair ->
                selectedChunk = null
                when (action) {
                    "entities" -> viewModel.openChunkNbt(chunkPair, isEntity = true, navigator = navigator)
                    "block_entities" -> viewModel.openChunkNbt(chunkPair, isEntity = false, navigator = navigator)
                }
            }
        )
    }
}

// 这里的 InteractiveMapCanvas 需要获取真实的 Constraints
@Composable
fun InteractiveMapCanvas(
    regionBitmaps: Map<RegionCoordPair, ImageBitmap>,
    viewportWidth: Float, // 这里的参数现在由 BoxWithConstraints 提供更佳
    viewportHeight: Float,
    viewModel: MapPreviewViewModel,
    onChunkTap: (ChunkCoordPair) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val vWidth = constraints.maxWidth.toFloat()
        val vHeight = constraints.maxHeight.toFloat()

        LaunchedEffect(regionBitmaps, viewModel.isMapCentered) {
            if (regionBitmaps.isNotEmpty() && !viewModel.isMapCentered) {
                val minX = regionBitmaps.keys.minOf { it.regionX() }
                val maxX = regionBitmaps.keys.maxOf { it.regionX() }
                val minZ = regionBitmaps.keys.minOf { it.regionZ() }
                val maxZ = regionBitmaps.keys.maxOf { it.regionZ() }

                val mapWidth = (maxX - minX + 1) * 512f
                val mapHeight = (maxZ - minZ + 1) * 512f

                val scaleX = vWidth / mapWidth
                val scaleY = vHeight / mapHeight
                viewModel.mapScale = minOf(scaleX, scaleY).coerceIn(0.05f, 2f)

                val boundsCenterX = minX * 512f + mapWidth / 2f
                val boundsCenterZ = minZ * 512f + mapHeight / 2f

                viewModel.mapOffset = Offset(
                    vWidth / 2f - boundsCenterX * viewModel.mapScale,
                    vHeight / 2f - boundsCenterZ * viewModel.mapScale
                )
                viewModel.isMapCentered = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val worldX = (tapOffset.x - viewModel.mapOffset.x) / viewModel.mapScale
                                val worldZ = (tapOffset.y - viewModel.mapOffset.y) / viewModel.mapScale
                                val chunkX = floor(worldX / 16f).toInt()
                                val chunkZ = floor(worldZ / 16f).toInt()
                                onChunkTap(ChunkCoordPair(chunkX, chunkZ))
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = viewModel.mapScale
                            viewModel.mapScale = (viewModel.mapScale * zoom).coerceIn(0.01f, 50f)
                            viewModel.mapOffset = (viewModel.mapOffset + pan - centroid) * (viewModel.mapScale / oldScale) + centroid
                        }
                    }
            ) {
                withTransform({
                    translate(viewModel.mapOffset.x, viewModel.mapOffset.y)
                    scale(viewModel.mapScale, viewModel.mapScale, pivot = Offset.Zero)
                }) {
                    regionBitmaps.forEach { (region, bitmap) ->
                        drawImage(
                            image = bitmap,
                            topLeft = Offset(region.regionX() * 512f, region.regionZ() * 512f)
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { viewModel.isMapCentered = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "居中")
            }
        }
    }
}

// ChunkActionMenu 和 ActionMenuItem 逻辑保持不变...
@Composable
fun ChunkActionMenu(
    chunk: ChunkCoordPair?,
    onDismiss: () -> Unit,
    onAction: (String, ChunkCoordPair) -> Unit
) {
    AnimatedVisibility(
        visible = chunk != null,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.width(280.dp).padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "区块 (${chunk?.chunkX()}, ${chunk?.chunkZ()})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    ActionMenuItem(
                        icon = Icons.Default.Pets,
                        label = "查看区块实体 NBT"
                    ) {
                        chunk?.let { onAction("entities", it) }
                    }

                    ActionMenuItem(
                        icon = Icons.Default.Widgets,
                        label = "查看方块实体 NBT"
                    ) {
                        chunk?.let { onAction("block_entities", it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, color = Color.Transparent) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}