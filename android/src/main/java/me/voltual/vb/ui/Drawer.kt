//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.vb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import me.voltual.vb.core.ui.icons.drawable.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey        
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.data.DrawerMenuDataStore
import org.koin.inject.KoinInject // 修复错误声明，如果不用注解就直接注入

sealed class IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource()
}

data class DrawerItem(
    val id: String, 
    val label: String,
    val icon: IconSource, 
    val route: AppDestination
)

@Composable
fun DrawerHeader(modifier: Modifier = Modifier, backgroundUri: String?) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.BottomStart
    ) {
        Column {
            Text(
                text = "VECTOR BREAKTHROUGH",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SYSTEM STATUS // ONLINE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun NavigationDrawerItems(
    navigator: Navigator,
    currentTopLevelRoute: NavKey?,           
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val allDrawerItems = remember {
        mutableListOf(
            DrawerItem("home", "首页 // HOME", IconSource.Vector(IcMenuHome), Home),
            DrawerItem("stitch", "存档缝合 // STITCH", IconSource.Vector(Icons.Default.MergeType), StitchDest),
            DrawerItem("map_preview", "地图预览 // MAP", IconSource.Vector(Icons.Default.Map), MapPreviewDest()),
            DrawerItem("pack_converter", "材质转换 // PACK", IconSource.Vector(Icons.Default.Build), PackConverterDest),
            DrawerItem("decoder", "存档还原 // DECODE", IconSource.Vector(healing), DecoderDest),
            DrawerItem("logs", "系统日志 // LOGS", IconSource.Vector(WorkLog), LogViewer),
            DrawerItem("ftp_settings", "文件管理 // FTP", IconSource.Vector(Icons.Default.Share), FtpSettings),
            DrawerItem("conversion_settings", "转换设置 // CONFIG", IconSource.Vector(Icons.Default.Settings), ChunkerSettings),
            DrawerItem("cache_settings", "缓存管理 // CACHE", IconSource.Vector(Icons.Default.DeleteSweep), CacheSettings),
            DrawerItem("update_settings", "检查更新 // UPDATE", IconSource.Vector(Asusupdate), UpdateSettings),
            DrawerItem("settings", "主题校准 // THEME", IconSource.Vector(IcMenuSettings), ThemeCustomize),
        )
    }
    val allItemsMap = remember { allDrawerItems.associateBy { it.id } }

    var orderedItems by remember { mutableStateOf<List<DrawerItem>>(emptyList()) }
    var draggedItem by remember { mutableStateOf<DrawerItem?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var itemHeight by remember { mutableStateOf(0) }
    val drawerMenuDataStore: DrawerMenuDataStore = org.koin.compose.koinInject()

    var selectedItemId by remember { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        val savedOrder = drawerMenuDataStore.loadMenuOrder().first()
        orderedItems = if (savedOrder.isEmpty()) {
            allDrawerItems
        } else {
            val ordered = savedOrder.mapNotNull { allItemsMap[it] }
            val newItems = allDrawerItems.filter { it.id !in savedOrder }
            ordered + newItems
        }
    }

    LaunchedEffect(currentTopLevelRoute) {
        currentTopLevelRoute?.let { currentRoute ->
            val matchedItem = orderedItems.find { 
                it.route::class == currentRoute::class 
            }
            if (matchedItem != null && matchedItem.id != selectedItemId) {
                selectedItemId = matchedItem.id
            }
        }
    }

    val placeholderIndex by remember(draggedItem, dragOffsetY) {
        derivedStateOf {
            draggedItem?.let {
                val initialIndex = orderedItems.indexOf(it)
                val displacement = if (itemHeight > 0) (dragOffsetY / itemHeight).toInt() else 0
                (initialIndex + displacement).coerceIn(0, orderedItems.size - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(orderedItems, key = { it.id }) { item ->
                val isBeingDragged = item.id == draggedItem?.id
                val index = orderedItems.indexOf(item)
                val showPlaceholder = placeholderIndex == index && draggedItem != null && placeholderIndex != orderedItems.indexOf(draggedItem)

                if (showPlaceholder) {
                    val isDraggedDown = placeholderIndex!! > orderedItems.indexOf(draggedItem)
                    if (isDraggedDown) {
                        ItemContent(item, selectedItemId, { selectedItemId = it }, false, scope, drawerState, navigator)
                        PlaceholderItem(modifier = Modifier.onSizeChanged { itemHeight = it.height })
                    } else {
                        PlaceholderItem(modifier = Modifier.onSizeChanged { itemHeight = it.height })
                        ItemContent(item, selectedItemId, { selectedItemId = it }, false, scope, drawerState, navigator)
                    }
                } else {
                    ItemContent(
                        item = item,
                        selectedItemId = selectedItemId,
                        onItemClick = { selectedItemId = it },
                        isDragged = isBeingDragged,
                        scope = scope,
                        drawerState = drawerState,
                        navigator = navigator,
                        modifier = Modifier
                            .onSizeChanged { itemHeight = it.height }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedItem = item },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        placeholderIndex?.let { toIndex ->
                                            val fromIndex = orderedItems.indexOf(draggedItem!!)
                                            if (fromIndex != toIndex) {
                                                val newList = orderedItems.toMutableList().apply {
                                                    add(toIndex, removeAt(fromIndex))
                                                }
                                                orderedItems = newList
                                                scope.launch {
                                                    drawerMenuDataStore.saveMenuOrder(newList.map { it.id })
                                                }
                                            }
                                        }
                                        draggedItem = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedItem = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                    )
                }
            }
        }

        draggedItem?.let { item ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = dragOffsetY
                        shadowElevation = 8f
                    }
                    .padding(horizontal = 12.dp)
            ) {
                ItemContent(item, selectedItemId, { selectedItemId = it }, false, scope, drawerState, navigator)
            }
        }
    }
}

@Composable
private fun ItemContent(
    item: DrawerItem,
    selectedItemId: String,
    onItemClick: (String) -> Unit,
    isDragged: Boolean,
    scope: CoroutineScope,
    drawerState: DrawerState,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedItemId == item.id

    val itemBorder = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, Color.Transparent)
    }

    NavigationDrawerItem(
        label = { 
            Text(
                text = item.label,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge
            ) 
        },
        icon = {
            val iconModifier = Modifier.size(20.dp)
            when (val source = item.icon) {
                is IconSource.Vector -> Icon(
                    imageVector = source.imageVector, 
                    contentDescription = null, 
                    modifier = iconModifier,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        selected = isSelected,
        onClick = {
            onItemClick(item.id)
            scope.launch { drawerState.close() }    
            navigator.navigate(item.route)
        },
        modifier = modifier
            .padding(vertical = 2.dp)
            .border(itemBorder, AppShapes.small)
            .graphicsLayer { alpha = if (isDragged) 0f else 1f },
        shape = AppShapes.small,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun PlaceholderItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 2.dp),
        shape = AppShapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {}
}