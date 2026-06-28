// [file name]: me.voltual.vb.ui.terminal.TerminalScreen.kt
package me.voltual.vb.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    // 进入终端时注入强行停止（Stop）动作，离开页面时清空
    DisposableEffect(Unit) {
        viewModel.startExecution(args, navigator)

        val stopAction = TopAppBarAction(
            icon = { tint ->
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "强行停止",
                    tint = tint
                )
            },
            description = "停止转换",
            onClick = {
                viewModel.stopExecution(navigator)
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
            .background(Color(0xFF121212)) // 与 TerminalView 暗色背景融为一体
    ) {
        session?.let { termSession ->
            TerminalViewAndroidView(
                session = termSession,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}