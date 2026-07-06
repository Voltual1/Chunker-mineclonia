package me.voltual.vb.ui.settings.ftp

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.voltual.vb.core.ftp.FtpServerManager
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQButton
import me.voltual.vb.core.ui.theme.BBQCard
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

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部战术标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "FTP_DATA_BRIDGE // 中转站配置",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 1. 链路状态面板
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LINK_STATUS // 实时链路监控",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), AppShapes.small)
                        .padding(12.dp)
                ) {
                    // 模拟 LED 指示灯
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isFtpRunning) Color(0xFF10B981) else Color(0xFFEF4444),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isFtpRunning) "ACTIVE_STATION // 运行中 [端口: $portInput]" else "LINK_IDLE // 已停止服务",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isFtpRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // 2. 挂载路径面板
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MOUNT_POINT // 物理存储映射",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = worldDir.absolutePath,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "■ 注意：中转站根目录已锁定，请勿手动修改物理软链接。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
            }
        }

        // 3. 链路参数配置
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "LINK_CONFIG // 接入参数设定",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text("BRIDGE_PORT") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFtpRunning,
                    shape = AppShapes.small,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("USER_ID") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFtpRunning,
                    shape = AppShapes.small,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("AUTH_KEY") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFtpRunning,
                    shape = AppShapes.small,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 启动/停止动作按钮
        BBQButton(
            onClick = {
                scope.launch {
                    if (isFtpRunning) {
                        ftpManager.stopServer()
                        isFtpRunning = false
                        snackbarHostState.showSnackbar("DISCONNECTED // 链路已切断")
                    } else {
                        val port = portInput.toIntOrNull() ?: (20000..30000).random()
                        ftpSettingsStore.updateSettings {
                            it.copy(port = port, username = usernameInput, password = passwordInput)
                        }
                        val success = ftpManager.startServer(ftpRootDir = worldDir)
                        if (success) {
                            isFtpRunning = true
                            snackbarHostState.showSnackbar("CONNECTED // 战术数据链路已建立")
                        } else {
                            snackbarHostState.showSnackbar("FAILED // 端口冲突或权限不足")
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isFtpRunning) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isFtpRunning) "TERMINATE_LINK // 切断链路" else "ESTABLISH_LINK // 建立中转链路",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}