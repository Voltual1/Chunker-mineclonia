package me.voltual.vb.ui.packconverter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFilePicker
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 材质包名称输入
        OutlinedTextField(
            value = packName,
            onValueChange = { viewModel.setPackName(it) },
            label = { Text("材质包名称") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning,
            singleLine = true
        )

        // 路径选择卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 选择输入文件
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("输入材质包 (Java 版 ZIP)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (inputUri != null) "已选择输入文件" else "未选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (inputUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = { filePicker.launch() },
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("选择")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 选择输出目录
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("输出目录 (基岩版 .mcpack)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (outputTreeUri != null) "已选择输出目录" else "未选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (outputTreeUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = { folderPicker.launch() },
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("选择")
                    }
                }
            }
        }

        // 调试模式开关
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = debugMode,
                onCheckedChange = { viewModel.setDebugMode(it) },
                enabled = !isRunning
            )
            Spacer(Modifier.width(8.dp))
            Text("调试模式 (Debug Mode)", style = MaterialTheme.typography.bodyMedium)
        }

        // 转换按钮
        Button(
            onClick = { viewModel.startConversion() },
            enabled = !isRunning && inputUri != null && outputTreeUri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("转换中...")
            } else {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("开始转换")
            }
        }

        // 输出日志区域
        Text("转换日志输出", style = MaterialTheme.typography.titleSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF121212), MaterialTheme.shapes.small)
                .padding(8.dp)
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
                Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}