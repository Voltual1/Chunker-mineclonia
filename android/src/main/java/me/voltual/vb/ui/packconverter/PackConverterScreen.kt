package me.voltual.vb.ui.packconverter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anggrayudi.storage.compose.rememberLauncherForFilePicker
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import me.voltual.vb.core.ui.components.MarkDownText
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQButton
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.ui.TerminalViewAndroidView
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackConverterScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: PackConverterViewModel = koinViewModel()
) {
    val packName by viewModel.packName.collectAsState()
    val inputUri by viewModel.inputUri.collectAsState()
    val outputTreeUri by viewModel.outputTreeUri.collectAsState()
    val debugMode by viewModel.debugMode.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val session by viewModel.session.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForFilePicker(
        filterMimeTypes = setOf("application/zip", "application/java-archive"),
        allowMultiple = false
    ) { files ->
        val file = files.firstOrNull()
        if (file != null) {
            viewModel.setInputUri(file.uri, file.name ?: "Unknown")
        }
    }

    val folderPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.setOutputTreeUri(folder.uri)
    }

    val helpMarkdown = """
        ### 材质转码协议说明 (Java to Bedrock)
        
        ■ **核心限制**：当前逻辑仅支持 Java 版 ZIP 压缩介质转码至 Bedrock 标准格式。
        ■ **精灵图跳过**：已自动跳过 Spritesheet 合并以防止 Android 内存溢出导致系统崩溃。
        ■ **免责声明**：本组件基于上游 [PackConverter](https://github.com/GeyserMC/PackConverter) 开发，目前处于 WIP 阶段。部分自定义物品映射可能失效。
        
        ---
        **AUTHOR: GEYSER_MC / VOLTUAL**
    """.trimIndent()

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            shape = AppShapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("TRANSCODER_MANUAL // 帮助文档", fontWeight = FontWeight.Black) },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MarkDownText(content = helpMarkdown)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }, shape = AppShapes.small) {
                    Text("ACKNOWLEDGEMENT")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部战术标识
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(width = 4.dp, height = 20.dp).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(12.dp))
                Text(
                    "ASSET_TRANSCODER_BAY // 转码分析舱",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Help,
                    contentDescription = "Manual",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 1. 转码指令集配置
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = packName,
                onValueChange = { viewModel.setPackName(it) },
                label = { Text("ASSET_IDENTIFIER // 材质包标识名") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning,
                singleLine = true,
                shape = AppShapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // 路径挂载卡片
            BBQCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 输入挂载
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SOURCE_MEDIA // 输入介质 (Java ZIP)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (inputUri != null) "READY // 已锁定文件" else "IDLE // 等待挂载",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (inputUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = { filePicker.launch() },
                            enabled = !isRunning,
                            shape = AppShapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("MOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // 输出挂载
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("EXPORT_LINK // 输出目录 (MCPACK)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (outputTreeUri != null) "LINKED // 已建立链路" else "IDLE // 等待映射",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (outputTreeUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = { folderPicker.launch() },
                            enabled = !isRunning,
                            shape = AppShapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("LINK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. 战术参数开关
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), AppShapes.small)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Checkbox(
                checked = debugMode,
                onCheckedChange = { viewModel.setDebugMode(it) },
                enabled = !isRunning,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
            )
            Spacer(Modifier.width(8.dp))
            Text("ENABLE_TRACE_LOG // 开启调试模式追踪", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }

        // 3. 执行启动按钮
        BBQButton(
            onClick = { viewModel.startConversion() },
            enabled = !isRunning && inputUri != null && outputTreeUri != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("TRANSCODING_IN_PROGRESS...", fontWeight = FontWeight.Black)
                    } else {
                        Icon(Icons.Default.Build, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("START_TRANSCODE // 启动重构", fontWeight = FontWeight.Black)
                    }
                }
            }
        )

        // 4. 转码监控区
        Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("LIVE_TELEMETRY // 实时转码流", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.primary)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF08080C)) // 深色监控屏底色
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), AppShapes.small)
                    .padding(4.dp)
            ) {
                session?.let { activeSession ->
                    TerminalViewAndroidView(
                        session = activeSession,
                        modifier = Modifier.fillMaxSize(),
                        initialTextSize = 28
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NO_STREAM_ACTIVE // 等待任务触发", color = Color(0xFF44474F), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}