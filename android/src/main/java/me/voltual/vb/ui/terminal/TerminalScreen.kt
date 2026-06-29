package me.voltual.vb.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.voltual.vb.core.ui.icons.drawable.CubeOff
import me.voltual.vb.ui.LocalNavigator
import me.voltual.vb.ui.LocalTopAppBarController
import me.voltual.vb.ui.TopAppBarAction
import me.voltual.vb.ui.TerminalExec
import me.voltual.vb.ui.TerminalViewAndroidView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TerminalScreen(
    args: TerminalExec,
    modifier: Modifier = Modifier,
    viewModel: TerminalViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val topAppBarController = LocalTopAppBarController.current
    val session by viewModel.session.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(args) {
        viewModel.startExecution(args, navigator)

        val stopAction = TopAppBarAction(
            icon = { tint ->
                Icon(
                    imageVector = CubeOff,
                    contentDescription = "强行停止",
                    tint = tint
                )
            },
            description = "停止转换",
            onClick = {
                showExitDialog = true
            }
        )
        topAppBarController.updateActions(listOf(stopAction))
    }

    // 初始化一个只有 1 页的 Pager 状态
    val pagerState = rememberPagerState(initialPage = 0) { 1 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> {
                    session?.let { activeSession ->
                        TerminalViewAndroidView(
                            session = activeSession,
                            modifier = Modifier.fillMaxSize(),
                            initialTextSize = 36 
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 退出确认弹窗
if (showExitDialog) {
    AlertDialog(
        onDismissRequest = { showExitDialog = false },
        title = { Text("退出转换") },
        text = { Text("检测到转换任务正在进行。您可以选择将转换留在后台继续运行（直接退出），强行中止当前任务，或者直接退出并彻底杀死整个应用进程。") },
        confirmButton = {
            // 使用 FlowRow 避免小屏幕手机上 4 个按钮水平排列导致文字超出边界截断
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("取消")
                }
                
                TextButton(onClick = {
                    showExitDialog = false
                    navigator.goBack()
                }) {
                    Text("直接退出")
                }

                OutlinedButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.stopExecution(navigator)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("强行中止")
                }

                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.killApplicationProcess()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("杀死应用")
                }
            }
        }
    )
}
    }
}