package me.voltual.vb.ui.settings.ftp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.voltual.vb.core.ftp.FtpServerManager
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FtpSettingsScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val ftpManager: FtpServerManager = koinInject()
    
    // 获取当前应用私有外部存储空间，专门管理 World 文件夹
    val worldDir = remember {
        val externalDir = context.getExternalFilesDir(null)
        if (externalDir != null) {
            File(externalDir, "worlds")
        } else {
            File(context.filesDir, "worlds")
        }
    }

    var isFtpRunning by remember { mutableStateOf(ftpManager.isRunning) }
    var serverStatusText by remember { mutableStateOf(if (isFtpRunning) "运行中" else "未启动") }
    var portInput by remember { mutableStateOf("2121") }
    var usernameInput by remember { mutableStateOf("admin") }
    var passwordInput by remember { mutableStateOf("admin123") }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "FTP 世界管理中转站", style = MaterialTheme.typography.titleLarge)
            
            // 运行状态指示器
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "当前服务状态:", style = MaterialTheme.typography.bodyLarge)
                SuggestionChip(
                    onClick = { },
                    label = { Text(serverStatusText) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isFtpRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                )
            }

            Text(
                text = "外部挂载目录:\n${worldDir.absolutePath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // 属性配置
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("FTP 端口") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("FTP 用户名") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("FTP 密码") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // 控制按钮
            Button(
                onClick = {
                    if (isFtpRunning) {
                        ftpManager.stopServer()
                        isFtpRunning = false
                        serverStatusText = "已停止"
                    } else {
                        val port = portInput.toIntOrNull() ?: 2121
                        val success = ftpManager.startServer(
                            port = port,
                            username = usernameInput,
                            password = passwordInput,
                            ftpRootDir = worldDir
                        )
                        if (success) {
                            isFtpRunning = true
                            serverStatusText = "运行中 (端口: $port)"
                        } else {
                            serverStatusText = "启动失败"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFtpRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isFtpRunning) "关闭 FTP 服务" else "开启 FTP 服务")
            }
        }
    }
}