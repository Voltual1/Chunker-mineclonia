package me.voltual.vb.ui.chunker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.vb.core.ui.icons.drawable.CubeOff
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.ui.LocalNavigator
import me.voltual.vb.ui.LocalTopAppBarController
import me.voltual.vb.ui.TopAppBarAction
import me.voltual.vb.ui.TerminalExec
import me.voltual.vb.ui.TerminalViewAndroidView
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChunkerScreen(
    args: TerminalExec,
    modifier: Modifier = Modifier,
    viewModel: ChunkerViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val topAppBarController = LocalTopAppBarController.current
    val session by viewModel.session.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    // 网格颜色：根据当前主色调设置极低透明度的线条
    val gridColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)

    LaunchedEffect(args) {
        viewModel.startExecution(args, navigator)

        val stopAction = TopAppBarAction(
            icon = { tint ->
                Icon(
                    imageVector = CubeOff,
                    contentDescription = "FORCE_STOP",
                    tint = tint
                )
            },
            description = "TERMINATE_PROC",
            onClick = {
                showExitDialog = true
            }
        )
        topAppBarController.updateActions(listOf(stopAction))
    }

    val pagerState = rememberPagerState(initialPage = 0) { 1 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08080C)) // 使用比纯黑更具深度的战术底色
            .drawBehind {
                // 绘制战术栅格背景
                val gridSize = 24.dp.toPx()
                for (x in 0..size.width.toInt() step gridSize.toInt()) {
                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step gridSize.toInt()) {
                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "INITIALIZING_VIRTUAL_TERMINAL...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        // 顶部状态装饰条
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = AppShapes.small
                            ) {
                                Text(
                                    text = " LIVE_STREAM // CONVERSION_PROCESS ",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 战术指令干预面板
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                shape = AppShapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                title = { 
                    Column {
                        Text(
                            "SYSTEM_INTERVENTION", 
                            fontWeight = FontWeight.Black, 
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                },
                text = { 
                    Text(
                        "检测到转换任务正在执行。您可以选择将任务保留在后台异步处理（DETACH），强行中止当前任务流（ABORT），或彻底关闭整个应用系统进程（SHUTDOWN）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 动作：取消
                        TextButton(
                            onClick = { showExitDialog = false },
                            shape = AppShapes.small
                        ) {
                            Text("RETURN", color = MaterialTheme.colorScheme.outline)
                        }
                        
                        // 动作：后台运行（方舟风格按钮）
                        Button(
                            onClick = {
                                showExitDialog = false
                                navigator.goBack()
                            },
                            shape = AppShapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text("DETACH", fontWeight = FontWeight.Bold)
                        }

                        // 动作：强行中止
                        OutlinedButton(
                            onClick = {
                                showExitDialog = false
                                viewModel.stopExecution(navigator)
                            },
                            shape = AppShapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text("ABORT_TASK", fontWeight = FontWeight.Bold)
                        }

                        // 动作：杀死应用（方舟高红危险色）
                        Button(
                            onClick = {
                                showExitDialog = false
                                viewModel.killApplicationProcess()
                            },
                            shape = AppShapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("SHUTDOWN_SYS", fontWeight = FontWeight.Black)
                        }
                    }
                }
            )
        }
    }
}