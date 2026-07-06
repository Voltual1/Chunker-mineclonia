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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import kotlinx.coroutines.launch
import me.voltual.vb.core.utils.WorldExporter
import me.voltual.vb.ui.LocalTopAppBarController
import me.voltual.vb.ui.LocalNavigator
import me.voltual.vb.ui.TopAppBarAction
import org.koin.compose.viewmodel.koinViewModel
import java.io.File
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
    val coroutineScope = rememberCoroutineScope()

    // 修复：使用正确的 mutableStateMapOf 泛型映射
    val composeBitmaps = remember { mutableStateMapOf<Pair<Dimension, RegionCoordPair>, ImageBitmap>() }
    var selectedChunk by remember { mutableStateOf<ChunkCoordPair?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.worldDirUri = folder.uri.toString()
        composeBitmaps.clear()
        selectedChunk = null
        showActionMenu = false
        viewModel.loadAndRenderWorld(context, folder)
    }

    // 初始化载入
    LaunchedEffect(initialFolderUri) {
        viewModel.checkExistingFtpInput(context)
        if (initialFolderUri.isNotEmpty() && viewModel.worldDirUri.isEmpty()) {
            val doc = com.anggrayudi.storage.file.DocumentFileCompat.fromFullPath(context, initialFolderUri)
            if (doc != null) viewModel.loadAndRenderWorld(context, doc)
        }
    }

    LaunchedEffect(viewModel.showGrid, viewModel.worldDirUri) {
        topAppBarController.updateActions(
            listOf(
                TopAppBarAction(
                    icon = { tint -> Icon(Icons.Default.GridOn, contentDescription = "网格线", tint = if (viewModel.showGrid) MaterialTheme.colorScheme.primary else tint) },
                    description = "网格线",
                    onClick = { viewModel.showGrid = !viewModel.showGrid }
                ),
                TopAppBarAction(
                    icon = { tint -> Icon(Icons.Default.IosShare, contentDescription = "导出世界", tint = tint) },
                    description = "导出世界",
                    onClick = {
                        coroutineScope.launch {
                            val uri = viewModel.worldDirUri
                            if (uri.isNotEmpty()) {
                                snackbarHostState.showSnackbar("正在打包导出当前世界...")
                                val file = File(uri)
                                if (file.exists()) {
                                    val success = WorldExporter.exportWorld(context, file)
                                    snackbarHostState.showSnackbar(if (success) "导出成功！已保存至 Downloads 文件夹" else "打包压缩失败")
                                } else {
                                    snackbarHostState.showSnackbar("物理文件不可访问")
                                }
                            } else {
                                snackbarHostState.showSnackbar("请先选取一个存档世界")
                            }
                        }
                    }
                ),
                TopAppBarAction(
                    icon = { tint -> Icon(Icons.Default.FolderOpen, contentDescription = "打开存档", tint = tint) },
                    description = "打开存档",
                    onClick = { folderPicker.launch() }
                )
            )
        )
    }

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
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { folderPicker.launch() }) {
                                Icon(Icons.Default.FolderOpen, null)
                                Spacer(Modifier.width(8.dp))
                                Text("选取存档目录")
                            }

                            if (viewModel.hasExistingFtpInput) {
                                FilledTonalButton(onClick = {
                                    viewModel.loadAndRenderWorld(context, null, useFtpInput = true)
                                }) {
                                    Icon(Icons.Default.Wifi, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("直接预览中转站")
                                }
                            }
                        }
                    }
                } else {
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

                    // 修复：正确解构过滤当前选择维度的地图瓦片数据
                    val filteredBitmaps = composeBitmaps
                        .filterKeys { it.first == viewModel.selectedDimension }
                        .mapKeys { it.key.second }

                    if (filteredBitmaps.isNotEmpty()) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            InteractiveMapCanvas(
                                regionBitmaps = filteredBitmaps,
                                viewModel = viewModel,
                                onChunkTap = { chunkPair ->
                                    selectedChunk = chunkPair
                                    showActionMenu = true
                                }
                            )

                            // 悬浮维度切换面板
                            if (viewModel.availableDimensions.size > 1) {
                                FloatingActionButton(
                                    onClick = {
                                        val currentIndex = viewModel.availableDimensions.indexOf(viewModel.selectedDimension)
                                        val nextIndex = (currentIndex + 1) % viewModel.availableDimensions.size
                                        viewModel.selectedDimension = viewModel.availableDimensions[nextIndex]
                                        viewModel.isMapCentered = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(24.dp),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    val dimLabel = when (viewModel.selectedDimension) {
                                        Dimension.OVERWORLD -> "主世界"
                                        Dimension.NETHER -> "下界"
                                        Dimension.THE_END -> "末地"
                                        else -> viewModel.selectedDimension.getIdentifier()
                                    }
                                    Text(
                                        text = dimLabel,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ChunkActionMenu(
            visible = showActionMenu,
            chunk = selectedChunk,
            onDismiss = { showActionMenu = false },
            onAction = { action, chunkPair ->
                showActionMenu = false
                when (action) {
                    "entities" -> viewModel.openChunkNbt(chunkPair, isEntity = true, navigator = navigator)
                    "block_entities" -> viewModel.openChunkNbt(chunkPair, isEntity = false, navigator = navigator)
                    "delete_chunk" -> {
                        coroutineScope.launch {
                            val success = viewModel.deleteChunk(chunkPair, viewModel.selectedDimension)
                            if (success) {
                                snackbarHostState.showSnackbar("区块 (${chunkPair.chunkX()}, ${chunkPair.chunkZ()}) 已彻底抹除")
                            } else {
                                snackbarHostState.showSnackbar("区块删除失败，可能它本身就是空的")
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun InteractiveMapCanvas(
    regionBitmaps: Map<RegionCoordPair, ImageBitmap>,
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
                        
                        if (viewModel.showGrid) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.4f),
                                topLeft = Offset(region.regionX() * 512f, region.regionZ() * 512f),
                                size = Size(512f, 512f),
                                style = Stroke(width = 2.0f)
                            )
                            
                            val startX = region.regionX() * 512f
                            val startZ = region.regionZ() * 512f
                            for (c in 1 until 32) {
                                val offsetDist = c * 16f
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(startX + offsetDist, startZ),
                                    end = Offset(startX + offsetDist, startZ + 512f),
                                    strokeWidth = 0.8f
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(startX, startZ + offsetDist),
                                    end = Offset(startX + 512f, startZ + offsetDist),
                                    strokeWidth = 0.8f
                                )
                            }
                        }
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

@Composable
fun ChunkActionMenu(
    visible: Boolean,
    chunk: ChunkCoordPair?,
    onDismiss: () -> Unit,
    onAction: (String, ChunkCoordPair) -> Unit
) {
    AnimatedVisibility(
        visible = visible && chunk != null,
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

                    // 危险操作分隔符
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    )

                    ActionMenuItem(
                        icon = Icons.Default.DeleteForever,
                        label = "删除此区块 (不可逆)",
                        textColor = MaterialTheme.colorScheme.error
                    ) {
                        chunk?.let { onAction("delete_chunk", it) }
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
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                tint = textColor,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.bodyLarge, 
                color = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}