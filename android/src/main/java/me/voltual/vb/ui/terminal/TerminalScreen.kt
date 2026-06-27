// [file name]: me.voltual.vb.ui.terminal.TerminalScreen.kt
package me.voltual.vb.ui.terminal

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
    val context = LocalContext.current

    LaunchedEffect(args) {
        viewModel.startExecution(args, navigator)
    }

    val pagerState = rememberPagerState(initialPage = 0) { 1 }

    Box(modifier = Modifier.fillMaxSize()) {
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

        // 内存监控悬浮窗条
        MemoryMonitorOverlay(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            context = context
        )
    }
}

@Composable
fun MemoryMonitorOverlay(modifier: Modifier = Modifier, context: Context) {
    var memoryStats by remember { mutableStateOf(getMemoryStats(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            memoryStats = getMemoryStats(context)
            delay(500) // 每半秒刷新一次
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xBB000000), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = "VM Heap: ${memoryStats.usedMb}MB / ${memoryStats.maxMb}MB",
                color = if (memoryStats.isDanger) Color(0xFFFF5252) else Color(0xFF69F0AE),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sys RAM: ${memoryStats.sysFreeMb}MB Free",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

data class MemoryStats(
    val usedMb: Long,
    val maxMb: Long,
    val isDanger: Boolean,
    val sysFreeMb: Long
)

fun getMemoryStats(context: Context): MemoryStats {
    val runtime = Runtime.getRuntime()
    val maxMemory = runtime.maxMemory()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()

    val maxMb = maxMemory / (1024 * 1024)
    val usedMb = usedMemory / (1024 * 1024)
    
    // 如果占用超过堆最大限制的 85%，标红警告
    val isDanger = (usedMemory.toDouble() / maxMemory.toDouble()) > 0.85

    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val sysFreeMb = memoryInfo.availMem / (1024 * 1024)

    return MemoryStats(usedMb, maxMb, isDanger, sysFreeMb)
}