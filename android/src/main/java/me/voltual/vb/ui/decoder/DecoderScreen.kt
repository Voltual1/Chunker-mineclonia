package me.voltual.vb.ui.decoder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQButton
import me.voltual.vb.core.ui.theme.BBQCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoderScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: DecoderViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val folderInputPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.selectedInputFolder = folder
    }

    val folderOutputPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.selectedOutputFolder = folder
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 系统警告与说明面板
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = AppShapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SYS_NOTICE // 核心功能说明",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "本模块通过执行 [ LayerV2_Stream_Codec ] 逻辑，尝试对非标准或受损的 Bedrock 存档进行字节流对齐与智能还原。不保证 100% 的重组成功率。",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // 2. PHASE 01: 输入路径选择
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PHASE 01 // SOURCE_BINARY",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                BBQCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    onClick = {
                        if (!viewModel.isProcessing) {
                            folderInputPicker.launch()
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = viewModel.selectedInputFolder?.name ?: "MOUNT_INPUT_SOURCE // 选择待还原存档",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (viewModel.selectedInputFolder != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // 3. PHASE 02: 输出路径选择
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PHASE 02 // RECONSTRUCTION_PATH",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                BBQCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    onClick = {
                        if (!viewModel.isProcessing) {
                            folderOutputPicker.launch()
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = viewModel.selectedOutputFolder?.name ?: "MOUNT_OUTPUT_TARGET // 选择还原目标目录",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (viewModel.selectedOutputFolder != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 执行按钮
            BBQButton(
                onClick = {
                    viewModel.startDecoding { success, msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(if (success) "COMPLETED // $msg" else "ABORTED // $msg")
                        }
                    }
                },
                enabled = viewModel.selectedInputFolder != null &&
                        viewModel.selectedOutputFolder != null &&
                        !viewModel.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsInputComponent, null)
                        Spacer(Modifier.width(12.dp))
                        Text("INITIATE_RECONSTRUCTION // 启动智能重组", fontWeight = FontWeight.Black)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 4. 战术进度覆盖面板
        if (viewModel.isProcessing) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Terminal, 
                        contentDescription = null, 
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "CORE_DECRYPTION_IN_PROGRESS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { viewModel.progressVal },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "■ STATUS: ${viewModel.progressText}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}