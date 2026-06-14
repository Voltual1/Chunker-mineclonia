// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.pyrolysis.core.ui.theme

// --- Kotlin 协程 ---
import kotlinx.coroutines.launch

// --- Compose 核心基础 (UI, Layout, Foundation) ---
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*

// --- Compose UI 配置 (Graphics, Positioning, Text) ---
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

// --- Material Design 3 组件 ---
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState

// --- 图片加载 (Coil 3) ---
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

// --- 项目自定义与本地数据 ---
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.data.unified.*
import me.voltual.pyrolysis.core.ui.components.LinkifyText

// 基础按钮组件
@Composable
fun BBQButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    enabled: Boolean = true,
    shape: Shape = AppShapes.medium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = contentPadding
    ) {
        text()
    }
}

// 轮廓按钮组件
@Composable
fun BBQOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    enabled: Boolean = true,
    shape: Shape = AppShapes.small,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = contentPadding
    ) {
        text()
    }
}

// 卡片组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    shape: Shape = AppShapes.medium,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = border
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQBackgroundCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    shape: Shape = AppShapes.medium,
    backgroundAlpha: Float = 0.9f,
    content: @Composable () -> Unit
) {
    val themeStore: ThemeColorDataStore = koinInject()
    val globalBackgroundUri by themeStore.globalBackgroundUriFlow.collectAsState(initial = null)

    if (globalBackgroundUri == null) {
        BBQCard(
            modifier = modifier,
            onClick = onClick,
            border = border,
            shape = shape,
            content = content
        )
        return
    }

    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = border
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = globalBackgroundUri),
                contentDescription = "Global Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .matchParentSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha))
            ) {
                content()
            }
        }
    }
}

// 图标按钮组件
@Composable
fun BBQIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

//移动帖子详情页"带文本的开关"到theme下的公共位置以便复用
@Composable
fun SwitchWithText(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ImagePreviewItem(
    imageUrl: String,
    onRemoveClick: () -> Unit,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(MaterialTheme.shapes.medium)
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "预览图片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onImageClick),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.BrokenImage, contentDescription = "加载失败")
                }
            }
        )

        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除图片",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// 自定义 Snackbar 组件
@Composable
fun BBQSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    dismissActionContentColor: Color = contentColor
) {
    Snackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        actionColor = actionColor,
        dismissActionContentColor = dismissActionContentColor
    )
}

// 成功状态的 Snackbar
@Composable
fun BBQSuccessSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium
) {
    BBQSnackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

// 错误状态的 Snackbar
@Composable
fun BBQErrorSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium
) {
    BBQSnackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )
}

// 警告状态的 Snackbar
@Composable
fun BBQWarningSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium
) {
    BBQSnackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = MaterialTheme.messageDefaultBg,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

// 信息状态的 Snackbar
@Composable
fun BBQInfoSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium
) {
    BBQSnackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = MaterialTheme.messageCommentBg,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

// 自定义 Snackbar Host
@Composable
fun BBQSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { snackbarData ->
        if (snackbarData.visuals.message.contains("info:")) {
            BBQInfoSnackbar(snackbarData)
        } else {
            BBQSnackbar(snackbarData)
        }
    }
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = snackbar
    )
}

/**
 * 商店切换下拉菜单组件
 * 用于在不同商店之间切换，可在多个界面复用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppStoreDropdownMenu(
    selectedStore: AppStore,
    onStoreChange: (AppStore) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "选择商店",
    appStores: List<AppStore> = AppStore.entries
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            readOnly = true,
            value = selectedStore.displayName,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = AppShapes.medium,
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            onDismissRequest = { expanded = false }
        ) {
            appStores.forEach { store ->
                DropdownMenuItem(
                    text = {
                        Text(store.displayName, style = MaterialTheme.typography.bodyMedium)
                    },
                    onClick = {
                        onStoreChange(store)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 商店切换卡片组件
 * 包含标签和下拉菜单的完整卡片形式
 */
@Composable
fun AppStoreSelectorCard(
    selectedStore: AppStore,
    onStoreChange: (AppStore) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String = "商店选择",
    description: String? = "选择要浏览的应用商店"
) {
    BBQCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            AppStoreDropdownMenu(
                selectedStore = selectedStore,
                onStoreChange = onStoreChange,
                enabled = enabled,
                label = "当前商店"
            )

            Text(
                "已选择: ${selectedStore.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// 该组件ModelList.kt（下载列表）的原始版本来源自 https://github.com/rikkahub/rikkahub
// 本版本仅作简化修改适应项目实际用途
// License: AGPLv3 (Compatible with GPLv3)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSourceDrawer(
    show: Boolean,
    onDismissRequest: () -> Unit,
    sources: List<UnifiedDownloadSource>,
    shape: Shape = AppShapes.medium,
    onSourceSelected: (UnifiedDownloadSource) -> Unit
) {
    if (show) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            shape = shape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight(0.5f)
                    .imePadding()
            ) {
                Text(
                    text = "选择下载源",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sources) { source ->
                        DownloadSourceItem(
                            source = source,
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    onDismissRequest()
                                    onSourceSelected(source)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSourceItem(
    source: UnifiedDownloadSource,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (source.isOfficial) Icons.Default.Download else Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 应用网格项组件
 * 显示单个应用的信息，包括图标和名称
 */
@Composable
fun AppGridItem(
    app: UnifiedAppItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = app.iconUrl,
                contentDescription = app.name,
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 8.dp)
            )
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                minLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun AppListItem(
    app: UnifiedAppItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = app.iconUrl,                contentDescription = app.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                app.info?.let { infoText ->
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 2000
                        )
                    )
                }
            }
        }
    }
}

/**
 * 应用网格组件
 * 显示应用列表的网格布局
 */
@Composable
fun AppGrid(
    apps: List<UnifiedAppItem>,
    columns: Int,
    onItemClick: (UnifiedAppItem) -> Unit,
    gridState: LazyGridState = rememberLazyGridState()
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        state = gridState
    ) {
        items(apps, key = { it.uniqueId }) { app ->
            AppGridItem(
                app = app,
                onClick = { onItemClick(app) }
            )
        }
    }
}

/**
 * 应用列表组件
 * 显示应用列表的纵向滚动布局
 */
@Composable
fun AppList(
    apps: List<UnifiedAppItem>,
    onItemClick: (UnifiedAppItem) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        state = listState
    ) {
        items(
            items = apps,
            key = { it.uniqueId }
        ) { app ->
            AppListItem(
                app = app,
                onClick = { onItemClick(app) }
            )
        }
    }
}

@Composable
fun AppFavoriteCard(
    state: UnifiedFavoriteState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onToggle(!state.isFavorite) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (state.isFavorite) "已加入收藏" else "收藏此应用",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                if (state.favoriteCount >= 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已有 ${state.favoriteCount} 人收藏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val scale by animateFloatAsState(
                targetValue = if (state.isFavorite) 1.2f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "starScale"
            )

            Icon(
                imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 评论项组件
 * 显示单条评论，支持回复和长按操作
 * @param comment 评论数据
 * @param onReply 回复按钮点击回调
 * @param onLongClick 长按评论的回调
 * @param onUserClick 点击用户头像或用户名的回调
 *
 * 注意：该组件内部通过 LocalNavigator 实现 LinkifyText 的链接跳转，无需外部传入导航控制器。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedCommentItem(
    comment: UnifiedComment,
    onReply: () -> Unit,
    onLongClick: () -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = comment.sender.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onUserClick),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text(comment.sender.displayName, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "回复",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onReply)
                )
            }
            Spacer(Modifier.height(8.dp))
            if (comment.content.isNotEmpty()) {
                // LinkifyText 已迁移至 Navigation 3，内部使用 LocalNavigator，无需传参
                LinkifyText(
                    text = comment.content,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }

            if (comment.fatherReply != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (comment.fatherReply.content.isNotEmpty()) {
                            LinkifyText(
                                text = "回复 @${comment.fatherReply.sender.displayName}: ${comment.fatherReply.content}",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 自定义基础 DropdownMenu
 * 参数与原版完全一致，默认添加了 surfaceVariant 背景色
 */
@Composable
fun BBQDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        content = content
    )
}

/**
 * 自定义 ExposedDropdownMenu
 * 必须在 ExposedDropdownMenuBoxScope 下使用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.BBQExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        scrollState = scrollState,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        content = content
    )
}

// 为了方便调用，同时提供一个 Box 的包装（虽然它只是透明转发）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        content = content
    )
}

/**
 * 自定义的下拉刷新指示器，使用 MaterialTheme 语义颜色。
 * 兼容 Compose Material 3 1.4.0 及以上版本。
 *
 * @param state [PullToRefreshState] 状态对象。
 * @param isRefreshing Boolean，指示是否正在进行刷新。
 * @param modifier Modifier 应用于此指示器的修饰符。
 * @param backgroundColor 指示器容器的背景色，默认使用 [MaterialTheme.colorScheme.surface]。
 * @param contentColor 指示器的颜色，默认使用 [MaterialTheme.colorScheme.primary]。
 * @param containerShape 指示器容器的形状，默认使用 [PullToRefreshDefaults.indicatorShape]。
 */
@Composable
fun BBQPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    containerShape: Shape = PullToRefreshDefaults.indicatorShape,
) {
    PullToRefreshDefaults.Indicator(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier,
        containerColor = backgroundColor,
        color = contentColor,
    )
}