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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager 
import androidx.compose.ui.text.AnnotatedString 
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.ui.theme.AppShapes
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
    snackbarHostState: SnackbarHostState, 
    modifier: Modifier = Modifier
) {
    val logs by viewModel.logs.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val selectedCount = selectedItems.size
    val coroutineScope = rememberCoroutineScope()

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showSelectionOptions by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.copyEvent.collect { (textToCopy, count) ->
            clipboardManager.setText(AnnotatedString(textToCopy))
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "ACCESS_DATA // 已提取 $count 条记录至剪贴板",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.refreshFileLogs()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (logs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "EMPTY_REGISTRY // 暂无系统运行日志", 
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = if (isSelectionMode) 80.dp else 16.dp, bottom = 100.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    val isSelected = selectedItems.contains(log.id)
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

        // 战术操作悬浮按钮组
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            if (isSelectionMode) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FloatingActionButton(
                        onClick = { viewModel.copySelectedLogs() },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = AppShapes.small
                    ) {
                        Icon(Icons.Default.ContentCopy, "EXTRACT")
                    }
                    FloatingActionButton(
                        onClick = { viewModel.deleteSelected() },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        shape = AppShapes.small
                    ) {
                        Icon(Icons.Default.Delete, "PURGE")
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { showSelectionOptions = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = AppShapes.small
                ) {
                    Icon(Icons.Default.Terminal, "MENU")
                }
            }
        }

        // 选择模式下的战术覆盖顶栏
        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedCount,
                onClose = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onInvertSelection = { viewModel.invertSelection() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            shape = AppShapes.medium,
            title = { Text("DANGER_ZONE // 清空注册表", fontWeight = FontWeight.Black) },
            text = { Text("此操作将永久抹除当前所有运行日志记录（包含磁盘缓存日志），确定执行？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllLogs()
                    showClearAllDialog = false
                }) {
                    Text("EXEC_PURGE // 确认清除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("ABORT")
                }
            }
        )
    }

    if (showSelectionOptions) {
        DropdownMenu(
            expanded = showSelectionOptions,
            onDismissRequest = { showSelectionOptions = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), AppShapes.small)
        ) {
            DropdownMenuItem(
                text = { Text("SELECT_MODE // 进入选择模式") },
                leadingIcon = { Icon(Icons.Default.Checklist, null) },
                onClick = {
                    viewModel.startSelectionMode()
                    showSelectionOptions = false
                }
            )
            DropdownMenuItem(
                text = { Text("PURGE_ALL // 清空所有日志", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
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
            .padding(12.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, AppShapes.small),
        shape = AppShapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "ABORT")
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "TARGETS_LOCKED // $selectedCount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSelectAll, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SelectAll, "ALL")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onInvertSelection, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SyncAlt, "INVERT")
                }
            }
        }
    }
}

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
    
    val itemBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    val itemBgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(AppShapes.small)
            .background(itemBgColor)
            .border(1.dp, itemBorderColor, AppShapes.small)
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() },
                onLongClick = { if (!isSelectionMode) onStartSelection() }
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // 状态色标条
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(statusColor)
            )
            
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RECORD // ${log.type}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = log.formattedTime(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STATUS // ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = log.status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "■ IN_DATA: ${log.requestBody.take(60)}${if (log.requestBody.length > 60) "..." else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "■ OUT_MSG: ${log.responseBody.take(100)}${if (log.responseBody.length > 100) "..." else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}