package me.voltual.vb.ui.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import me.voltual.vb.ui.LocalNavigator
import me.voltual.vb.ui.TerminalExec
import me.voltual.vb.ui.TerminalViewAndroidView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TerminalScreen(
    args: TerminalExec,
    viewModel: TerminalViewModel = koinViewModel()
) {
    val session by viewModel.session.collectAsState()
    val navigator = LocalNavigator.current

    LaunchedEffect(args) {
        viewModel.startExecution(args, navigator)
    }

    // 初始化一个只有 1 页的 Pager 状态
    val pagerState = rememberPagerState(initialPage = 0) { 1 }

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
        }        
    }
}
}
}