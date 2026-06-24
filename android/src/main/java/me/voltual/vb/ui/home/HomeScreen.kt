package me.voltual.vb.ui.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.ui.FtpSettings
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
        viewModel.useExistingInput = false // 既然手动选择了，重置为不使用 FTP 中转
        viewModel.selectedFolder = folder
    }

    // 页面初始化与复原时检测中转站世界
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
                        Text(
                            text = "第一步：准备世界存档",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // 选项 A：手动从 SAF 选择（带预警弹窗）
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
                                        text = if (viewModel.useExistingInput) "已选择使用中转站存档" else (viewModel.selectedFolder?.name ?: "手动选择本地世界存档文件夹"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (viewModel.selectedFolder != null && !viewModel.useExistingInput) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 选项 B：检测到 FTP 存在输入存档直接转换
                        if (viewModel.hasExistingInput) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "检测到世界中转站 (FTP) 目录下已存在 world_input 存档文件！",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.selectedFolder = null
                                            viewModel.useExistingInput = true
                                            scope.launch {
                                                pagerState.animateScrollToPage(1)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "使用中转站")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("直接转换中转站存档")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        AnimatedVisibility(visible = viewModel.selectedFolder != null || viewModel.useExistingInput) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("下一步：选择目标格式")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.NavigateNext, contentDescription = "下一步")
                            }
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
                                Icon(imageVector = Icons.Default.NavigateBefore, contentDescription = "返回")
                            }
                            Text(
                                text = "第二步：选择输出格式",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            label = { Text("搜索格式 (例如: JAVA, BEDROCK)") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "搜索") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            singleLine = true
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.filteredFormats) { format ->
                                val isSelected = viewModel.selectedFormat == format
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                                            style = MaterialTheme.typography.bodyLarge,
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

                        Button(
                            onClick = {
                                viewModel.startCopyAndNavigate(context, navigator)
                            },
                            enabled = viewModel.selectedFormat != null && !viewModel.isCopying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("开始复制并转换")
                        }
                    }
                }
            }
        }

        // SAF 限制与覆盖确认对话框
        if (showSafWarningDialog) {
            AlertDialog(
                onDismissRequest = { showSafWarningDialog = false },
                title = { Text("手动选择及覆盖提示") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. 手动选择文件夹会覆盖当前世界中转站下的已有 world_input 数据。")
                        Text("2. 由于 Android 系统 SAF 存储访问框架的接口和跨进程 Binder 限制，对于含数千小碎片文件的世界存档，手动复制将极其缓慢。")
                        Text("3. 建议：对大型世界存档，强烈建议通过内建 [世界中转站 (FTP)] 进行高速无线传输。")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSafWarningDialog = false
                            folderPickerLauncher.launch()
                        }
                    ) {
                        Text("坚持手动选择")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSafWarningDialog = false
                            navigator.navigate(FtpSettings)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = "FTP")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("去开启 FTP 传输")
                    }
                }
            )
        }

        // 复制进度弹窗
        if (viewModel.isCopying) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("正在处理存档") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = viewModel.copyStatusText)
                        LinearProgressIndicator(
                            progress = { viewModel.copyProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}