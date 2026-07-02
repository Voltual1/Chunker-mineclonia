package me.voltual.vb.ui.decoder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import com.anggrayudi.storage.compose.rememberLauncherForFilePicker
import kotlinx.coroutines.launch
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

    val metaFilePicker = rememberLauncherForFilePicker { files ->
        if (files.isNotEmpty()) {
            viewModel.selectedMetaFile = files.first()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "存档解码还原",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "使用 LayerV2StreamCodec 重新还原已混淆或已对齐加密的世界文件流。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // 1. 输入文件夹选择
            Text(
                text = "步骤 1：选择加密的世界存档源目录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
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
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "选择文件夹",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.selectedInputFolder?.name ?: "点击选择被加密的存档文件夹",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // 2. Meta 密钥映射文件选择
            Text(
                text = "步骤 2：选择密钥种子文件 (Meta File)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            BBQCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                onClick = {
                    if (!viewModel.isProcessing) {
                        metaFilePicker.launch()
                    }
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "选择密钥文件",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.selectedMetaFile?.name ?: "点击选择密钥引导文件 (通常为 key.bin)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // 3. 标识符
            Text(
                text = "步骤 3：配置转换标识符 (Identifier)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.identifier,
                onValueChange = { viewModel.identifier = it },
                label = { Text("Identifier") },
                leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = "密钥") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !viewModel.isProcessing
            )

            // 4. 输出文件夹选择
            Text(
                text = "步骤 4：选择还原结果导出目标目录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
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
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "选择输出文件夹",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.selectedOutputFolder?.name ?: "点击选择已还原的导出目标文件夹",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.startDecoding { success, msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                },
                enabled = viewModel.selectedInputFolder != null &&
                        viewModel.selectedOutputFolder != null &&
                        viewModel.selectedMetaFile != null &&
                        viewModel.identifier.isNotBlank() &&
                        !viewModel.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("启动后台解码作业")
            }
        }

        if (viewModel.isProcessing) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("正在处理存档") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = viewModel.progressText)
                        LinearProgressIndicator(
                            progress = { viewModel.progressVal },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}