package me.voltual.vb.ui.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import me.voltual.vb.ui.TerminalExec
import me.voltual.vb.ui.TerminalViewAndroidView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TerminalScreen(
    args: TerminalExec,
    viewModel: TerminalViewModel = koinViewModel()
) {
    val session by viewModel.session.collectAsState()

    LaunchedEffect(args) {
        viewModel.startExecution(args)
    }

    if (session != null) {
        TerminalViewAndroidView(
            modifier = Modifier.fillMaxSize(),
            onSessionCreated = { /* 已经在 ViewModel 中处理 */ }
        )
    }
}