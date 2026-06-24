package me.voltual.vb.ui.export

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import me.voltual.vb.core.ui.theme.BBQCard
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

    // 注册本地文件夹选择器，用于 SAF 导出
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
                Text(
                    text = "打包失败，未找到转换后的存档文件",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
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

                    // 选项 1：本地 file 导出
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
                                Text("将 world_output.zip 导出到选定的外部文件夹", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
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