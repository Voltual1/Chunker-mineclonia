// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import com.anggrayudi.storage.file.DocumentFileCompat
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPreviewScreen(
    initialFolderUri: String,
    modifier: Modifier = Modifier,
    viewModel: MapPreviewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var currentUri by remember { mutableStateOf(initialFolderUri) }

    // SimpleStorage 文件夹选择器
    val folderPicker = rememberLauncherForFolderPicker { folder ->
        currentUri = folder.uri.toString()
        // 这里后续可以触发真正的 Chunker 加载逻辑
        // viewModel.loadAndRenderPreviewFromUri(context, folder)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地图预览 (测试)") },
                actions = {
                    IconButton(onClick = { folderPicker.launch() }) {
                        Icon(Icons.Default.FolderOpen, "打开存档")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentUri.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { folderPicker.launch() }) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("选择 Minecraft 存档文件夹")
                    }
                }
            } else {
                Text(
                    text = "当前路径: $currentUri",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1
                )
                
                if (viewModel.isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(viewModel.statusMessage)
                    }
                } else {
                    // 显示生成的 Region Bitmaps 列表
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(128.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(viewModel.regionBitmaps.toList()) { (region, bitmap) ->
                            Card(
                                modifier = Modifier.padding(4.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = "r.${region.regionX()}.${region.regionZ()}",
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}