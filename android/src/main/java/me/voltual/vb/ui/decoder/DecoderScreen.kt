package me.voltual.vb.ui.decoder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
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
                text = "一键将特定损坏或由于格式特殊无法直接读取的基岩版世界存档（免责声明:不一定能成功），还原为标准国际基岩版支持的正常存档格式。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // 1. 输入文件夹选择
            Text(
                text = "第一步：选择待还原的基岩版世界存档根目录",
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
                            contentDescription = "选择待还原存档",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.selectedInputFolder?.name ?: "点击选择需要还原的存档文件夹 (包含db)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // 2. 输出文件夹选择
            Text(
                text = "第二步：选择还原目标导出目录",
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
                            text = viewModel.selectedOutputFolder?.name ?: "点击选择还原完成后的导出目标文件夹",
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
                        !viewModel.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("一键智能还原")
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