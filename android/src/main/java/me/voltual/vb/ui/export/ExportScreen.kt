package me.voltual.vb.ui.export

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import me.voltual.vb.core.ui.components.MarkDownText
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.ui.FtpSettings
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    val folderPickerLauncher = rememberLauncherForFolderPicker { targetFolder ->
        viewModel.exportToLocal(
            targetFolder = targetFolder,
            onComplete = {
                Toast.makeText(context, "导出成功！", Toast.LENGTH_SHORT).show()
            },
            onError = { errorCode ->
                Toast.makeText(context, "导出失败: $errorCode", Toast.LENGTH_LONG).show()
            }
        )
    }

    Scaffold(
        topBar = {},
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (viewModel.isZipping) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("正在打包世界存档，请稍候...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (!viewModel.zipSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "打包失败，未找到转换后的存档文件",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { navigator.navigate(FtpSettings) }) {
                        Text("进入 FTP 设置管理已有世界")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "转换已完成！请选择导出方式：",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    BBQCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            folderPickerLauncher.launch()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = "保存到本地",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text("保存到手机目录", style = MaterialTheme.typography.titleMedium)
                                Text("将 world_output.zip 导出到选定外部文件夹", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    BBQCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            navigator.navigate(FtpSettings)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "局域网/FTP传输",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text("世界中转站 (局域网FTP)", style = MaterialTheme.typography.titleMedium)
                                Text("开启FTP服务直接管理文件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            MarkDownText(
                                content = "什么是FTP？请[点击搜索\"FTP文件传输协议\"](https://www.bing.com/search?q=ftp%E6%96%87%E4%BB%B6%E4%BC%A0%E8%BE%93%E5%8D%8F%E8%AE%AE%E7%AB%AF%E5%8F%A3)了解详细机制与说明。"
                            )
                        }
                    }
                }
            }

            if (viewModel.isExportingToLocal) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = { Text("正在导出") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = viewModel.localExportStatus)
                        }
                    }
                )
            }
        }
    }
}