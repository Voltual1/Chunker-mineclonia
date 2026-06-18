//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package me.voltual.vb.ui.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager // 引入 Compose 剪贴板管理器
import androidx.compose.ui.text.AnnotatedString // 引入富文本类型
import androidx.compose.ui.unit.dp
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.ui.components.ListItem
import me.voltual.vb.core.ui.theme.ThemeManager
import me.voltual.vb.core.ui.theme.billing_expense
import me.voltual.vb.core.ui.theme.billing_expense_dark
import me.voltual.vb.core.ui.theme.billing_income
import me.voltual.vb.core.ui.theme.billing_income_dark
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@Composable
fun LogScreen(
    viewModel: LogViewModel,
//    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState, // 添加 SnackbarHostState 参数
    modifier: Modifier = Modifier
) {
    val logs by viewModel.logs.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCount = viewModel.selectedItems.collectAsState().value.size
    val coroutineScope = rememberCoroutineScope()

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showSelectionOptions by remember { mutableStateOf(false) }

    // 获取 Compose 的剪贴板管理器
    val clipboardManager = LocalClipboardManager.current

    // 监听复制事件
    LaunchedEffect(Unit) {
        viewModel.copyEvent.collect { (textToCopy, count) ->
            // 使用 LocalClipboardManager 设置剪贴板文本
            clipboardManager.setText(AnnotatedString(textToCopy))
            
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "已复制 $count 条日志",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有任何日志记录", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    val isSelected = viewModel.selectedItems.collectAsState().value.contains(log.id)
                    LogListItem(
                        log = log,
                        isSelected = isSelected,
                        onToggleSelection = { viewModel.toggleSelection(log.id) },
                        onStartSelection = { viewModel.startSelectionMode(log.id) },
                        isSelectionMode = isSelectionMode
                    )
                }
            }
        }

        // 选择模式下的浮动操作按钮
        if (isSelectionMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { viewModel.copySelectedLogs() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.ContentCopy, "复制")
                }
                SmallFloatingActionButton(
                    onClick = { viewModel.deleteSelected() },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(Icons.Default.Delete, "删除")
                }
            }

            // 选择模式下的顶部操作栏（固定在顶部）
            SelectionTopBar(
                selectedCount = selectedCount,
                onClose = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onInvertSelection = { viewModel.invertSelection() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else {
            // 非选择模式下的操作按钮
            FloatingActionButton(
                onClick = { showSelectionOptions = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Menu, "操作菜单")
            }
        }
    }

    // 清空确认对话框
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清空日志") },
            text = { Text("确定要清空所有日志记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showClearAllDialog = false
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 操作菜单
    if (showSelectionOptions) {
        DropdownMenu(
            expanded = showSelectionOptions,
            onDismissRequest = { showSelectionOptions = false }
        ) {
            DropdownMenuItem(
                text = { Text("进入选择模式") },
                onClick = {
                    viewModel.startSelectionMode()
                    showSelectionOptions = false
                }
            )
            DropdownMenuItem(
                text = { Text("清空全部日志") },
                onClick = {
                    showClearAllDialog = true
                    showSelectionOptions = false
                }
            )
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：标题和关闭按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, "取消选择")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "已选择 $selectedCount 项",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 右侧：操作按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSelectAll,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.SelectAll, "全选")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onInvertSelection,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.SyncAlt, "反选")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogListItem(
    log: LogEntry,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit
) {
    val isDarkTheme = ThemeManager.isAppDarkTheme
    val statusColor = if (log.status == "SUCCESS") {
        if (isDarkTheme) billing_income_dark else billing_income
    } else {
        if (isDarkTheme) billing_expense_dark else billing_expense
    }
    val statusIcon = if (log.status == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Error
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        statusColor.copy(alpha = 0.1f)
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onStartSelection()
                    }
                }
            )
    ) {
        ListItem(
            title = "[${log.type}] - ${log.status}",
            content = "请求: ${log.requestBody}\n响应: ${log.responseBody}",
            time = log.formattedTime(),
            icon = statusIcon,
            backgroundColor = Color.Transparent,
            onClick = null
        )
    }
}