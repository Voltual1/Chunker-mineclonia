package me.voltual.vb.ui.settings.chunker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anggrayudi.storage.compose.rememberLauncherForFilePicker
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChunkerSettingsScreen(
    snackbarHostState: SnackbarHostState, 
    modifier: Modifier = Modifier,
    viewModel: ChunkerSettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val threadCount by viewModel.threadCount.collectAsState()
    val processMaps by viewModel.processMaps.collectAsState()
    val energySavingMode by viewModel.energySavingMode.collectAsState()
    val enableSlicing by viewModel.enableSlicing.collectAsState()
    
    val scrollState = rememberScrollState()
    var showClearDialog by remember { mutableStateOf(false) }

    // 挂置 simple-storage 文件夹选择器：用于断点导出
    val folderPickerLauncher = rememberLauncherForFolderPicker { folder ->
        viewModel.exportBreakpoints(
            folder = folder,
            context = context,
            onSuccess = {
                scope.launch {
                    snackbarHostState.showSnackbar("EXPORT_OK // 断点已安全导出至 breakpoints_backup.json")
                }
            },
            onError = { err ->
                scope.launch {
                    snackbarHostState.showSnackbar("EXPORT_FAIL // 备份导出失败: $err")
                }
            }
        )
    }

    // 挂置 simple-storage 文件选择器：用于断点导入
    val filePickerLauncher = rememberLauncherForFilePicker(
        filterMimeTypes = setOf("application/json")
    ) { files ->
        val file = files.firstOrNull() ?: return@rememberLauncherForFilePicker
        viewModel.importBreakpoints(
            file = file,
            context = context,
            onSuccess = {
                scope.launch {
                    snackbarHostState.showSnackbar("IMPORT_OK // 断点物理备份已成功合入覆盖")
                }
            },
            onError = { err ->
                scope.launch {
                    snackbarHostState.showSnackbar("IMPORT_FAIL // 备份恢复失败: $err")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "ENGINE_CALIBRATION // 转换核心微调",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = "REGULATION // 调节底层多线程并发与模块屏蔽开关，以在吞吐能效与硬件过载保护之间达成平衡。",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 核心线程滑块
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CORES_ALLOCATION // 核心分配",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "分配并行硬件线程数量",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "$threadCount THREADS",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Slider(
                    value = threadCount.toFloat(),
                    onValueChange = { viewModel.updateThreadCount(it.toInt()) },
                    valueRange = 1f..viewModel.maxCores.toFloat(),
                    steps = if (viewModel.maxCores > 1) viewModel.maxCores - 2 else 0,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "■ 当前平台物理核心限界: ${viewModel.maxCores}。线程设置过高会导致温控降频与系统卡顿。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }
        }

        // 防 OOM 内存安全切片模式
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SAFE_SLICING_MODE // 内存安全切片",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "将世界数据分割成小型阵列流，通过额外挂载独立的系统子进程运算并逐一合并。这是处理巨型存档防 JVM 爆炸的终极方案，但对于小型地图来说开启此模式会导致速度变慢且性能浪费。小世界建议关闭。",
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = enableSlicing,
                    onCheckedChange = { viewModel.updateEnableSlicing(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // 地图预览战术节能模式（点哪里亮哪里）
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PREVIEW_ENERGY_SAVING // 地图预览战术节能",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "启用时，地图初始处于灰暗状态，仅当点击某个512x512的局域网格时，才提取并渲染该局部的图像像素。这可以极致地防止因全盘地图扫描导致的 Android OOM 杀进程现象。",
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = energySavingMode,
                    onCheckedChange = { viewModel.updateEnergySavingMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // 地图项目
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MAP_CONVERSION // 转换地图项目",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "物理地图画作解码算法开销极大。为避免 JVM 虚拟机堆栈耗尽 (OOM)，强烈建议中低内存端设备保持 OFF 状态。",
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = processMaps,
                    onCheckedChange = { viewModel.updateProcessMaps(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // 新增：JSON 断点数据库导出、导入与迁移卡片
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BREAKPOINT_MIGRATION // 断点备份与迁移",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "将转换核心所记录的各个存档切片进度导出为 breakpoints_backup.json 文件，或从外部选择备份文件导入。支持手动修改其中内容以实现特殊情况下的强制覆写与续转偏置重置。",
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch() },
                        shape = AppShapes.small,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("IMPORT / 导入", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { folderPickerLauncher.launch() },
                        shape = AppShapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EXPORT / 导出", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // 断点清空
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DESTRUCTIVE_COMMAND // 物理擦除",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "清空所有存储在数据库寄存器中的物理切片断点。此指令执行后将不可回滚，下次转换必须全新全量扫描。",
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { showClearDialog = true },
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("PURGE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = AppShapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REGISTRY_PURGE_CONFIRM", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                }
            },
            text = { 
                Text(
                    "注意：强制清空世界转换器的断点记录记录（BREAK_POINTS）将解除该文件的续转锁。下一次执行相同世界数据转码时，数据流将执行重头对齐（FROM_SCRATCH）。确定擦除？",
                    style = MaterialTheme.typography.bodySmall
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        scope.launch {
                            viewModel.clearAllProgress()
                            snackbarHostState.showSnackbar("PURGE_OK // 断点记录清除成功")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("CONFIRM_PURGE // 确定物理擦除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("ABORT")
                }
            }
        )
    }
}