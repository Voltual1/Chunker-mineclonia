package me.voltual.vb.ui.map

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import me.voltual.vb.core.ui.theme.BBQOutlinedButton
import me.voltual.vb.core.utils.WorldExporter
import me.voltual.vb.ui.LocalTopAppBarController
import me.voltual.vb.ui.LocalNavigator
import me.voltual.vb.ui.TopAppBarAction
import me.voltual.vb.ui.Export
import me.voltual.vb.ui.FtpSettings
import org.koin.compose.viewmodel.koinViewModel
import java.io.File
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

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

    // 弹窗状态
    var showDestinationSelectDialog by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.worldDirUri = folder.uri.toString()
        composeBitmaps.clear()
        selectedChunk = null
        showActionMenu = false
        viewModel.loadAndRenderWorld(context, folder)
    }

    // 独立的外部粘贴底图拷贝
    val targetStitchPicker = rememberLauncherForFolderPicker { targetFolder ->
        composeBitmaps.clear()
        viewModel.copyExternalTargetToTemp(context, targetFolder)
    }

    LaunchedEffect(initialFolderUri) {
        viewModel.checkExistingFtpInput(context)
        if (initialFolderUri.isNotEmpty() && viewModel.worldDirUri.isEmpty()) {
            val doc = com.anggrayudi.storage.file.DocumentFileCompat.fromFullPath(context, initialFolderUri)
            if (doc != null) viewModel.loadAndRenderWorld(context, doc)
        }
    }

    LaunchedEffect(viewModel.showGrid, viewModel.worldDirUri, viewModel.previewState) {
        if (viewModel.previewState == PreviewState.IDLE || viewModel.previewState == PreviewState.SOURCE_SELECT) {
            topAppBarController.updateActions(
                listOf(
                    TopAppBarAction(
                        icon = { tint -> Icon(Icons.Default.Crop, contentDescription = "裁切复制", tint = if (viewModel.previewState == PreviewState.SOURCE_SELECT) MaterialTheme.colorScheme.primary else tint) },
                        description = "裁切复制",
                        onClick = { viewModel.toggleSourceSelectionMode() }
                    ),
                    TopAppBarAction(
                        icon = { tint -> Icon(Icons.Default.GridOn, contentDescription = "网格线", tint = if (viewModel.showGrid) MaterialTheme.colorScheme.primary else tint) },
                        description = "网格线",
                        onClick = { viewModel.showGrid = !viewModel.showGrid }
                    ),
                    TopAppBarAction(
                        icon = { tint -> Icon(Icons.Default.FolderOpen, contentDescription = "打开世界", tint = tint) },
                        description = "打开世界",
                        onClick = { folderPicker.launch() }
                    )
                )
            )
        } else if (viewModel.previewState == PreviewState.DEST_PASTE) {
            topAppBarController.updateActions(
                listOf(
                    TopAppBarAction(
                        icon = { tint -> Icon(Icons.Default.Close, contentDescription = "取消粘贴", tint = MaterialTheme.colorScheme.error) },
                        description = "取消粘贴",
                        onClick = { viewModel.abortPasting() }
                    )
                )
            )
        }
    }

    LaunchedEffect(viewModel.regionBitmaps.size) {
        viewModel.regionBitmaps.forEach { (region, bmp) ->
            if (!composeBitmaps.containsKey(region)) composeBitmaps[region] = bmp.asImageBitmap()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold { padding ->
            Box(modifier = modifier.fillMaxSize().padding(padding).background(Color(0xFF090A0E))) {
                if (viewModel.worldDirUri.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text("NO_WORLD_MOUNTED // 未读取世界介质", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BBQButton(onClick = { folderPicker.launch() }, text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("SELECT_DIR", fontWeight = FontWeight.Bold) } })
                            if (viewModel.hasExistingFtpInput) {
                                BBQOutlinedButton(onClick = { viewModel.loadAndRenderWorld(context, null, useFtpInput = true) }, text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wifi, null); Spacer(Modifier.width(8.dp)); Text("PREVIEW_FTP", fontWeight = FontWeight.Bold) } })
                            }
                        }
                    }
                } else {
                    if (viewModel.isLoading) {
                        Surface(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).border(1.dp, MaterialTheme.colorScheme.primary, AppShapes.small), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), shape = AppShapes.small) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("SCANNING_CHUNKS: ${viewModel.statusMessage}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    val filteredBitmaps = composeBitmaps.filterKeys { it.first == viewModel.selectedDimension }.mapKeys { it.key.second }
                    if (filteredBitmaps.isNotEmpty()) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            InteractiveMapCanvas(
                                regionBitmaps = filteredBitmaps,
                                viewModel = viewModel,
                                onChunkTap = { chunkPair ->
                                    if (viewModel.previewState == PreviewState.IDLE) {
                                        selectedChunk = chunkPair
                                        showActionMenu = true
                                    }
                                }
                            )

                            if (viewModel.availableDimensions.size > 1) {
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clickable {
                                        val cI = viewModel.availableDimensions.indexOf(viewModel.selectedDimension)
                                        viewModel.selectedDimension = viewModel.availableDimensions[(cI + 1) % viewModel.availableDimensions.size]
                                        viewModel.isMapCentered = false
                                    }.border(1.5.dp, MaterialTheme.colorScheme.primary, AppShapes.small),
                                    shape = AppShapes.small, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, shape = AppShapes.small))
                                        Spacer(Modifier.width(8.dp))
                                        Text(viewModel.selectedDimension.getIdentifier().uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // 底部操作面板
                            AnimatedVisibility(
                                visible = viewModel.previewState == PreviewState.SOURCE_SELECT && viewModel.sourceSelectionStart != null && viewModel.sourceSelectionEnd != null,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                Surface(modifier = Modifier.fillMaxWidth(0.9f).border(1.5.dp, MaterialTheme.colorScheme.primary, AppShapes.medium), shape = AppShapes.medium, color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        val s = viewModel.sourceSelectionStart!!
                                        val e = viewModel.sourceSelectionEnd!!
                                        val w = abs(e.first - s.first)
                                        val h = abs(e.second - s.second)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ContentCut, null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("SOURCE SELECTION // 源选区", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("尺寸: $w x $h (方块)", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.height(16.dp))
                                        BBQButton(
                                            onClick = {
                                                viewModel.scanLocalTargetWorlds(context)
                                                showDestinationSelectDialog = true
                                            }, 
                                            modifier = Modifier.fillMaxWidth(),
                                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ContentPaste, null); Spacer(Modifier.width(8.dp)); Text("准备选择缝合覆盖的目标世界...", fontWeight = FontWeight.Bold) } }
                                        )
                                    }
                                }
                            }

                            // 粘贴确认面板
                            AnimatedVisibility(
                                visible = viewModel.previewState == PreviewState.DEST_PASTE && viewModel.pasteTargetPoint != null,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                Surface(modifier = Modifier.fillMaxWidth(0.9f).border(1.5.dp, MaterialTheme.colorScheme.tertiary, AppShapes.medium), shape = AppShapes.medium, color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        val t = viewModel.pasteTargetPoint!!
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.tertiary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("CONFIRM PASTE // 确认覆盖位置", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("左上角落点: (${t.first}, ${t.second})", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.height(16.dp))
                                        BBQButton(
                                            onClick = { viewModel.confirmStitch(context) }, modifier = Modifier.fillMaxWidth(),
                                            text = { Text("CONFIRM OVERWRITE // 确认覆盖并缝合", fontWeight = FontWeight.Black) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ChunkActionMenu(visible = showActionMenu, chunk = selectedChunk, onDismiss = { showActionMenu = false }, onAction = { action, chunkPair ->
            showActionMenu = false
            when (action) {
                "entities" -> viewModel.openChunkNbt(chunkPair, true, navigator)
                "block_entities" -> viewModel.openChunkNbt(chunkPair, false, navigator)
                "delete_chunk" -> coroutineScope.launch { val success = viewModel.deleteChunk(chunkPair, viewModel.selectedDimension); snackbarHostState.showSnackbar(if(success) "已彻底抹除" else "抹除失败") }
            }
        })

        // 缝合状态覆盖层
        if (viewModel.previewState == PreviewState.STITCHING || viewModel.stitchSuccess || viewModel.stitchError != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (viewModel.stitchSuccess) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("缝合圆满完成！", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        BBQButton(onClick = { navigator.navigate(Export) }, text = { Text("前往导出") })
                    } else if (viewModel.stitchError != null) {
                        Text("ERROR: ${viewModel.stitchError}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.stitchError = null; viewModel.abortPasting() }) { Text("返回") }
                    } else {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("STITCHING IN PROGRESS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(24.dp))
                        LinearProgressIndicator(progress = { viewModel.stitchProgress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("${(viewModel.stitchProgress * 10).toInt()}%", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 新增：目标选择弹窗 (覆盖已有存档、外部SAF、或快捷导航到 FTP)
        if (showDestinationSelectDialog) {
            AlertDialog(
                onDismissRequest = { showDestinationSelectDialog = false },
                shape = AppShapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("TARGET WORLD SELECT // 选择目标存档", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("请选择要覆盖缝合的已有世界目标，或从系统外载入：", style = MaterialTheme.typography.bodyMedium)

                        // 本地已有存档列表
                        if (viewModel.localTargetWorlds.isNotEmpty()) {
                            Text("已检出中转站存档列表：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), AppShapes.small)
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(viewModel.localTargetWorlds) { folder ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                composeBitmaps.clear()
                                                viewModel.selectExistingLocalTarget(context, folder)
                                                showDestinationSelectDialog = false
                                            },
                                        shape = AppShapes.small
                                    ) {
                                        Text(
                                            text = folder.name,
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("（当前中转站没有其他可用存档）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // 从系统内以 SAF 复制
                        Button(
                            onClick = {
                                showDestinationSelectDialog = false
                                targetStitchPicker.launch()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = AppShapes.small
                        ) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SAF // 从系统加载全新目标", fontWeight = FontWeight.Bold)
                        }

                        // 导航至 FTP
                        OutlinedButton(
                            onClick = {
                                showDestinationSelectDialog = false
                                navigator.navigate(FtpSettings)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.Wifi, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ROUTE // 前往 FTP 导入存档", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showDestinationSelectDialog = false }) {
                        Text("ABORT")
                    }
                }
            )
        }
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

                viewModel.mapOffset = Offset(vWidth / 2f - boundsCenterX * viewModel.mapScale, vHeight / 2f - boundsCenterZ * viewModel.mapScale)
                viewModel.isMapCentered = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(viewModel.previewState) {
                        if (viewModel.previewState == PreviewState.SOURCE_SELECT) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val mapX = (offset.x - viewModel.mapOffset.x) / viewModel.mapScale
                                    val mapZ = (offset.y - viewModel.mapOffset.y) / viewModel.mapScale
                                    viewModel.sourceSelectionStart = Pair(mapX.toInt(), mapZ.toInt())
                                    viewModel.sourceSelectionEnd = Pair(mapX.toInt(), mapZ.toInt())
                                },
                                onDrag = { change, _ ->
                                    val offset = change.position
                                    val mapX = (offset.x - viewModel.mapOffset.x) / viewModel.mapScale
                                    val mapZ = (offset.y - viewModel.mapOffset.y) / viewModel.mapScale
                                    viewModel.sourceSelectionEnd = Pair(mapX.toInt(), mapZ.toInt())
                                }
                            )
                        } else if (viewModel.previewState == PreviewState.DEST_PASTE) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val mapX = (tapOffset.x - viewModel.mapOffset.x) / viewModel.mapScale
                                    val mapZ = (tapOffset.y - viewModel.mapOffset.y) / viewModel.mapScale
                                    viewModel.pasteTargetPoint = Pair(mapX.toInt(), mapZ.toInt())
                                }
                            )
                        } else {
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
                    }
                    .pointerInput(viewModel.previewState) {
                        if (viewModel.previewState != PreviewState.SOURCE_SELECT) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val oldScale = viewModel.mapScale
                                viewModel.mapScale = (viewModel.mapScale * zoom).coerceIn(0.01f, 50f)
                                viewModel.mapOffset = (viewModel.mapOffset + pan - centroid) * (viewModel.mapScale / oldScale) + centroid
                            }
                        }
                    }
            ) {
                withTransform({
                    translate(viewModel.mapOffset.x, viewModel.mapOffset.y)
                    scale(viewModel.mapScale, viewModel.mapScale, pivot = Offset.Zero)
                }) {
                    regionBitmaps.forEach { (region, bitmap) ->
                        drawImage(image = bitmap, topLeft = Offset(region.regionX() * 512f, region.regionZ() * 512f))
                        if (viewModel.showGrid) {
                            drawRect(color = Color(0xFF3B82F6).copy(alpha = 0.3f), topLeft = Offset(region.regionX() * 512f, region.regionZ() * 512f), size = Size(512f, 512f), style = Stroke(width = 1.5f))
                        }
                    }

                    if (viewModel.previewState == PreviewState.SOURCE_SELECT && viewModel.sourceSelectionStart != null && viewModel.sourceSelectionEnd != null) {
                        val s = viewModel.sourceSelectionStart!!
                        val e = viewModel.sourceSelectionEnd!!
                        val minX = min(s.first, e.first).toFloat()
                        val minZ = min(s.second, e.second).toFloat()
                        val maxX = max(s.first, e.first).toFloat()
                        val maxZ = max(s.second, e.second).toFloat()

                        drawRect(color = Color(0xFF3B82F6).copy(alpha = 0.4f), topLeft = Offset(minX, minZ), size = Size(maxX - minX, maxZ - minZ))
                        drawRect(color = Color(0xFF3B82F6), topLeft = Offset(minX, minZ), size = Size(maxX - minX, maxZ - minZ), style = Stroke(width = 2f / viewModel.mapScale))
                    }

                    if (viewModel.previewState == PreviewState.DEST_PASTE && viewModel.sourceSelectionStart != null && viewModel.sourceSelectionEnd != null && viewModel.pasteTargetPoint != null) {
                        val s = viewModel.sourceSelectionStart!!
                        val e = viewModel.sourceSelectionEnd!!
                        val w = kotlin.math.abs(e.first - s.first).toFloat()
                        val h = kotlin.math.abs(e.second - s.second).toFloat()
                        val target = viewModel.pasteTargetPoint!!

                        drawRect(color = Color(0xFFFACC15).copy(alpha = 0.4f), topLeft = Offset(target.first.toFloat(), target.second.toFloat()), size = Size(w, h))
                        drawRect(color = Color(0xFFFACC15), topLeft = Offset(target.first.toFloat(), target.second.toFloat()), size = Size(w, h), style = Stroke(width = 3f / viewModel.mapScale))
                    }
                }
            }

            FloatingActionButton(
                onClick = { viewModel.isMapCentered = false },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = AppShapes.small
            ) { Icon(Icons.Default.CenterFocusStrong, contentDescription = "扫描对准") }
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