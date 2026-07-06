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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQButton
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.core.ui.theme.BBQOutlinedButton
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
                                snackbarHostState.showSnackbar("PACKING // 正在打包物理存档文件...")
                                val file = File(uri)
                                if (file.exists()) {
                                    val success = WorldExporter.exportWorld(context, file)
                                    snackbarHostState.showSnackbar(if (success) "EXPORT_OK // 存档已妥善保存至 Downloads 目录" else "PACK_ERR // 压缩封装异常")
                                } else {
                                    snackbarHostState.showSnackbar("FS_ERR // 无法访问物理介质")
                                }
                            } else {
                                snackbarHostState.showSnackbar("MOUNT_ERR // 尚未挂载任何有效存档")
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
                    .background(Color(0xFF090A0E)) 
            ) {
                if (viewModel.worldDirUri.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO_WORLD_MOUNTED // 未读取世界介质",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BBQButton(
                                onClick = { folderPicker.launch() },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FolderOpen, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("SELECT_DIR", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )

                            if (viewModel.hasExistingFtpInput) {
                                BBQOutlinedButton(
                                    onClick = {
                                        viewModel.loadAndRenderWorld(context, null, useFtpInput = true)
                                    },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Wifi, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("PREVIEW_FTP", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (viewModel.isLoading) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary, AppShapes.small),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = AppShapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "SCANNING_CHUNKS: ${viewModel.statusMessage}", 
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

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

                            if (viewModel.availableDimensions.size > 1) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .clickable {
                                            val currentIndex = viewModel.availableDimensions.indexOf(viewModel.selectedDimension)
                                            val nextIndex = (currentIndex + 1) % viewModel.availableDimensions.size
                                            viewModel.selectedDimension = viewModel.availableDimensions[nextIndex]
                                            viewModel.isMapCentered = false
                                        }
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, AppShapes.small),
                                    shape = AppShapes.small,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ) {
                                    val dimLabel = when (viewModel.selectedDimension.getIdentifier()) {
                                        "minecraft:overworld" -> "主世界 OVERWORLD"
                                        "minecraft:the_nether" -> "下界 NETHER"
                                        "minecraft:the_end" -> "末地 THE_END"
                                        else -> viewModel.selectedDimension.getIdentifier().uppercase()
                                    }
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(MaterialTheme.colorScheme.primary, shape = AppShapes.small)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dimLabel,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
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
                                snackbarHostState.showSnackbar("SECTOR_PURGE // 区块 (${chunkPair.chunkX()}, ${chunkPair.chunkZ()}) 已彻底抹除")
                            } else {
                                snackbarHostState.showSnackbar("PURGE_FAILED // 目标区块可能本来就是空的")
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
                                color = Color(0xFF3B82F6).copy(alpha = 0.3f),
                                topLeft = Offset(region.regionX() * 512f, region.regionZ() * 512f),
                                size = Size(512f, 512f),
                                style = Stroke(width = 1.5f)
                            )
                            
                            val startX = region.regionX() * 512f
                            val startZ = region.regionZ() * 512f
                            for (c in 1 until 32) {
                                val offsetDist = c * 16f
                                drawLine(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                    start = Offset(startX + offsetDist, startZ),
                                    end = Offset(startX + offsetDist, startZ + 512f),
                                    strokeWidth = 0.5f
                                )
                                drawLine(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                    start = Offset(startX, startZ + offsetDist),
                                    end = Offset(startX + 512f, startZ + offsetDist),
                                    strokeWidth = 0.5f
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
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = AppShapes.small
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "扫描对准")
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
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center),
        exit = fadeOut(animationSpec = tween(120)) + scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center),
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
                modifier = Modifier
                    .width(280.dp)
                    .padding(16.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, AppShapes.small),
                shape = AppShapes.small,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "SECTOR_LOCK // 区块 (${chunk?.chunkX()}, ${chunk?.chunkZ()})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )

                    ActionMenuItem(
                        icon = Icons.Default.Pets,
                        label = "DECRYPT_ENTITIES // 实体 NBT"
                    ) {
                        chunk?.let { onAction("entities", it) }
                    }

                    ActionMenuItem(
                        icon = Icons.Default.Widgets,
                        label = "DECRYPT_TILE // 方块实体 NBT"
                    ) {
                        chunk?.let { onAction("block_entities", it) }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    )

                    ActionMenuItem(
                        icon = Icons.Default.DeleteForever,
                        label = "SECTOR_PURGE // 物理销毁区块",
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
                modifier = Modifier.size(20.dp),
                tint = textColor,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                color = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}