package me.voltual.vb.ui.settings.ftp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.voltual.vb.core.ftp.FtpServerManager
import me.voltual.vb.data.FtpSettingsDataStore
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
    val ftpSettingsStore: FtpSettingsDataStore = koinInject()
    val scope = rememberCoroutineScope()
    
    val worldDir = remember {
        val externalDir = context.getExternalFilesDir(null)
        if (externalDir != null) {
            File(externalDir, "worlds")
        } else {
            File(context.filesDir, "worlds")
        }
    }

    val ftpSettingsState by ftpSettingsStore.ftpSettingsFlow.collectAsState(initial = null)

    var portInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isFtpRunning by remember { mutableStateOf(ftpManager.isRunning) }

    LaunchedEffect(ftpSettingsState) {
        ftpSettingsState?.let {
            portInput = it.port.toString()
            usernameInput = it.username
            passwordInput = it.password
            isFtpRunning = it.isRunning
        }
    }

    var serverStatusText by remember(isFtpRunning, portInput) {
        mutableStateOf(if (isFtpRunning) "运行中 (端口: $portInput)" else "已停止")
    }

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

            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("FTP 端口") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFtpRunning
            )

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("FTP 用户名") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFtpRunning
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("FTP 密码") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFtpRunning
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        if (isFtpRunning) {
                            ftpManager.stopServer()
                            isFtpRunning = false
                            serverStatusText = "已停止"
                        } else {
                            val port = portInput.toIntOrNull() ?: (20000..30000).random()
                            ftpSettingsStore.updateSettings {
                                it.copy(port = port, username = usernameInput, password = passwordInput)
                            }
                            val success = ftpManager.startServer(ftpRootDir = worldDir)
                            if (success) {
                                isFtpRunning = true
                                serverStatusText = "运行中 (端口: $port)"
                            } else {
                                serverStatusText = "启动失败"
                            }
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