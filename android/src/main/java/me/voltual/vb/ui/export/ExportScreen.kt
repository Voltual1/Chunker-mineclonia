package me.voltual.vb.ui.export

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.callback.SingleFileConflictCallback
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import com.anggrayudi.storage.file.copyFileTo
import com.anggrayudi.storage.media.FileDescription
import com.anggrayudi.storage.result.SingleFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.core.utils.WorldExporter
import me.voltual.vb.ui.LocalNavigator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    var isZipping by remember { mutableStateOf(false) }
    var zipSuccess by remember { mutableStateOf(false) }
    
    // 暂时注释掉无线分享的状态
    // var isServerRunning by remember { mutableStateOf(false) }
    // var serverUrl by remember { mutableStateOf("") }

    var isExportingToLocal by remember { mutableStateOf(false) }
    var localExportStatus by remember { mutableStateOf("") }

    // 初始化：自动将 world_output 目录打包为 ZIP
    LaunchedEffect(Unit) {
        isZipping = true
        withContext(Dispatchers.IO) {
            val sourceDir = File(context.filesDir, "world_output")
            val targetZip = File(context.filesDir, "world_output.zip")
            zipSuccess = WorldExporter.zipFolder(sourceDir, targetZip)
        }
        isZipping = false
    }

    // 自动在退出页面时关闭 HTTP 服务（暂时注释掉，避免潜在的 bug 影响）
    /*
    DisposableEffect(Unit) {
        onDispose {
            WorldExporter.stopHttpServer()
        }
    }
    */

    // 注册本地文件夹选择器，用于 SAF 导出
    val folderPickerLauncher = rememberLauncherForFolderPicker { targetFolder ->
        isExportingToLocal = true
        localExportStatus = "正在复制到目标目录..."
        scope.launch(Dispatchers.IO) {
            val localZipFile = File(context.filesDir, "world_output.zip")
            val docZipFile = DocumentFile.fromFile(localZipFile)
            
            val fileDescription = FileDescription("world_output", "", "application/zip")
            val conflictCallback = object : SingleFileConflictCallback<DocumentFile>(scope) {
                override fun onFileConflict(destinationFile: DocumentFile, action: FileConflictAction) {
                    action.confirmResolution(ConflictResolution.REPLACE)
                }
            }

            docZipFile.copyFileTo(
                context = context,
                targetFolder = targetFolder,
                fileDescription = fileDescription,
                onConflict = conflictCallback
            ).collect { result ->
                withContext(Dispatchers.Main) {
                    when (result) {
                        is SingleFileResult.Completed -> {
                            isExportingToLocal = false
                            Toast.makeText(context, "导出成功！", Toast.LENGTH_SHORT).show()
                        }
                        is SingleFileResult.Error -> {
                            isExportingToLocal = false
                            Toast.makeText(context, "导出失败: ${result.errorCode}", Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {}, // TopBar 留空
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isZipping) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("正在打包世界存档，请稍候...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (!zipSuccess) {
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

                    // 选项 2：局域网无线导出（已暂时注释掉）
                    /*
                    BBQCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (isServerRunning) {
                                WorldExporter.stopHttpServer()
                                isServerRunning = false
                            } else {
                                WorldExporter.startHttpServer(
                                    context = context,
                                    onStarted = { url ->
                                        serverUrl = url
                                        isServerRunning = true
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
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
                                contentDescription = "局域网分享",
                                tint = if (isServerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text(
                                    text = if (isServerRunning) "关闭无线分享服务" else "局域网无线分享",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isServerRunning) "服务已启动，点击可关闭" else "在电脑浏览器直接输入地址下载",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // 展示无线服务的下载链接
                    if (isServerRunning) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("请在电脑或另一台设备的浏览器中输入：", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = serverUrl,
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("确保两台设备连接在同一个 Wi-Fi 网络下", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    */
                }
            }

            if (isExportingToLocal) {
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
                            Text(text = localExportStatus)
                        }
                    }
                )
            }
        }
    }
}