package me.voltual.vb.ui.settings.cache

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

// 定义清理类型
enum class ClearType {
    CONVERSION_ONLY, ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: CacheSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    
    // 控制弹窗状态与清理模式
    var pendingClearType by remember { mutableStateOf<ClearType?>(null) }

    Scaffold(
        topBar = {},
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "缓存管理",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("转换输出目录 (worlds/world_output)", style = MaterialTheme.typography.bodyMedium)
                        Text(viewModel.outputFolderSize, style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("打包压缩包 (worlds/world_output.zip)", style = MaterialTheme.typography.bodyMedium)
                        Text(viewModel.zipFileSize, style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("应用系统缓存 (cache)", style = MaterialTheme.typography.bodyMedium)
                        Text(viewModel.systemCacheSize, style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("总计缓存大小", style = MaterialTheme.typography.titleSmall)
                        Text(viewModel.totalSize, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 按钮 1：仅清除转换缓存
            OutlinedButton(
                onClick = { pendingClearType = ClearType.CONVERSION_ONLY },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "清除转换缓存")
                Spacer(modifier = Modifier.width(8.dp))
                Text("仅清除转换缓存")
            }

            // 按钮 2：清除全部缓存
            Button(
                onClick = { pendingClearType = ClearType.ALL },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "清除全部缓存")
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除全部缓存")
            }
        }

        // 统一处理的确认弹窗
        pendingClearType?.let { type ->
            val title = if (type == ClearType.ALL) "确认清除全部缓存" else "确认清除转换缓存"
            val text = if (type == ClearType.ALL) {
                "此操作将永久删除：\n1. 本地转换输出目录及对应压缩包\n2. 临时系统缓存文件（网络图片/中转站残留等）。\n\n源存档不受影响。是否确认彻底清理？"
            } else {
                "此操作将永久删除本地转换输出的世界文件夹及其对应的压缩包。源存档不受影响。是否继续？"
            }

            AlertDialog(
                onDismissRequest = { pendingClearType = null },
                title = { Text(title) },
                text = { Text(text) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (type == ClearType.ALL) {
                                viewModel.clearAllCache()
                            } else {
                                viewModel.clearConversionCache()
                            }
                            pendingClearType = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingClearType = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}