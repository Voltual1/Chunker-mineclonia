// [file name]: me.voltual.vb.ui.terminal.TerminalScreen.kt
package me.voltual.vb.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    DisposableEffect(Unit) {
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
        
        onDispose {
            topAppBarController.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        session?.let { termSession ->
            TerminalViewAndroidView(
                session = termSession,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("退出转换") },
                text = { Text("检测到转换任务正在进行。您可以选择将转换留在后台继续运行（直接退出），或者强行中止当前的转换任务。") },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
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
                        Button(
                            onClick = {
                                showExitDialog = false
                                viewModel.stopExecution(navigator)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("强行中止")
                        }
                    }
                }
            )
        }
    }
}