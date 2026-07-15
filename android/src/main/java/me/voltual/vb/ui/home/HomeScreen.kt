package me.voltual.vb.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke 
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.core.ui.theme.BBQButton
import me.voltual.vb.ui.FtpSettings
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    var showSafWarningDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForFolderPicker { folder ->
        viewModel.useExistingInput = false
        viewModel.selectedFolder = folder
    }

    LaunchedEffect(Unit) {
        viewModel.checkExistingInput(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = (viewModel.selectedFolder != null || viewModel.useExistingInput) && !viewModel.isCopying
        ) { page ->
            when (page) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 战术标题
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(4.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PHASE 01 // 准备世界存档",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 选项 A：手动从 SAF 选择
                        BBQCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            onClick = {
                                if (!viewModel.isCopying) {
                                    showSafWarningDialog = true
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "选择文件夹",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (viewModel.useExistingInput) "[SYS_MOUNTED] 已挂载中转站存档" else (viewModel.selectedFolder?.name ?: "MANUAL_SELECT // 本地世界存档文件夹"),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (viewModel.selectedFolder != null && !viewModel.useExistingInput) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 选项 B：FTP 存档提示（高亮警告黄）
                        if (viewModel.hasExistingInput) {
                            Surface(
                                shape = AppShapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber, 
                                            contentDescription = "Warning",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SYSTEM DETECTED // 发现中转站 (FTP) 存档缓存",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.selectedFolder = null
                                            viewModel.useExistingInput = true
                                            scope.launch {
                                                pagerState.animateScrollToPage(1)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        shape = AppShapes.small,
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Use FTP")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("MOUNT // 挂载中转存档", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        AnimatedVisibility(visible = viewModel.selectedFolder != null || viewModel.useExistingInput) {
                            // 修复：显式传递 text 参数，解决编译错误
                            BBQButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("NEXT_PHASE // 确认源文件", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(imageVector = Icons.Default.NavigateNext, contentDescription = "下一步")
                                    }
                                }
                            )
                        }
                    }
                }
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }) {
                                Icon(imageVector = Icons.Default.NavigateBefore, contentDescription = "返回", tint = MaterialTheme.colorScheme.primary)
                            }
                            Box(modifier = Modifier.width(4.dp).height(20.dp).background(MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PHASE 02 // 设定目标格式",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            label = { Text("[ SEARCH_FORMAT ]") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "搜索") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            singleLine = true,
                            shape = AppShapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.filteredFormats) { format ->
                                val isSelected = viewModel.selectedFormat == format
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = AppShapes.small,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectedFormat = format
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = format,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "已选择",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 修复：显式传递 text 参数，解决编译错误
                        BBQButton(
                            onClick = {
                                viewModel.startCopyAndNavigate(context, navigator)
                            },
                            enabled = viewModel.selectedFormat != null && !viewModel.isCopying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            text = {
                                Text("EXECUTE // 启动重构转换", fontWeight = FontWeight.Black)
                            }
                        )
                    }
                }
            }
        }

        // 战术风 SAF 警告提示框
        if (showSafWarningDialog) {
            AlertDialog(
                onDismissRequest = { showSafWarningDialog = false },
                shape = AppShapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SYSTEM ALERT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("■ 覆写警告：手动选择文件夹将覆盖 world_input 下的已有数据。")
                        Text("■ 性能劣化：Android SAF 接口对碎片化存档的 I/O 操作极其缓慢。")
                        Text("■ 最优策略：对于超过 50MB 的存档，强烈建议使用 [世界中转站 (FTP)] 进行内网直传。")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSafWarningDialog = false
                            folderPickerLauncher.launch()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.outline)
                    ) {
                        Text("IGNORE // 强行手动选择")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showSafWarningDialog = false
                            navigator.navigate(FtpSettings)
                        },
                        shape = AppShapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = "FTP")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ROUTE // 开启 FTP", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 战术进度覆盖面板
        if (viewModel.isCopying) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "I/O OPERATION IN PROGRESS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LinearProgressIndicator(
                        progress = { viewModel.copyProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = viewModel.copyStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}