// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.vb.core.ui.theme

// --- Kotlin 协程 ---
// --- Compose 核心基础 (UI, Layout, Foundation) ---
// --- Compose UI 配置 (Graphics, Positioning, Text) ---
// --- Material Design 3 组件 ---
// --- 图片加载 (Coil 3) ---

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import coil3.compose.rememberAsyncImagePainter

// 基础按钮组件
@Composable
fun BBQButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  text: @Composable () -> Unit,
  enabled: Boolean = true,
  shape: Shape = AppShapes.medium,
  contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
) {
  Button(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
    contentPadding = contentPadding,
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
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shape = shape,
    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    contentPadding = contentPadding,
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
  content: @Composable () -> Unit,
) {
  Card(
    modifier = modifier,
    onClick = onClick ?: {},
    shape = shape,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = border,
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
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val globalBackgroundUriState =
    ThemeColorStore.getGlobalBackgroundUriFlow(context).collectAsState(initial = null)
  val globalBackgroundUri by globalBackgroundUriState

  if (globalBackgroundUri == null) {
    BBQCard(
      modifier = modifier,
      onClick = onClick,
      border = border,
      shape = shape,
      content = content,
    )
    return
  }

  Card(
    modifier = modifier,
    onClick = onClick ?: {},
    shape = shape,
    colors =
      CardDefaults.cardColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = border,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = rememberAsyncImagePainter(model = globalBackgroundUri),
        contentDescription = "Global Background",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().matchParentSize(),
      )

      Box(
        modifier =
          Modifier.fillMaxSize()
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
  tint: Color = MaterialTheme.colorScheme.primary,
) {
  IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
    Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
  }
}

// 移动帖子详情页"带文本的开关"到theme下的公共位置以便复用
@Composable
fun SwitchWithText(
  text: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    Switch(checked = checked, onCheckedChange = onCheckedChange)
    Spacer(Modifier.width(8.dp))
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
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
  dismissActionContentColor: Color = contentColor,
) {
  // 使用基础版 Snackbar 重载，它支持 dismissAction
  Snackbar(
    modifier = modifier.padding(12.dp),
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
    dismissActionContentColor = dismissActionContentColor,
    // 设置中间的文本内容
    content = { Text(text = snackbarData.visuals.message) },
    // 设置右侧的动作按钮（如果有的话）
    action =
      snackbarData.visuals.actionLabel?.let { label ->
        {
          TextButton(
            onClick = { snackbarData.performAction() },
            colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
          ) {
            Text(label)
          }
        }
      },
    dismissAction = {
      IconButton(onClick = { snackbarData.dismiss() }) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "关闭",
          tint = dismissActionContentColor,
        )
      }
    },
  )
}

// 成功状态的 Snackbar
@Composable
fun BBQSuccessSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = true,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  )
}

// 错误状态的 Snackbar
@Composable
fun BBQErrorSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
  )
}

// 警告状态的 Snackbar
@Composable
fun BBQWarningSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.messageDefaultBg,
    contentColor = MaterialTheme.colorScheme.onSurface,
  )
}

// 信息状态的 Snackbar
@Composable
fun BBQInfoSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.messageCommentBg,
    contentColor = MaterialTheme.colorScheme.onSurface,
  )
}

// 自定义 Snackbar Host
@Composable
fun BBQSnackbarHost(
  hostState: SnackbarHostState,
  modifier: Modifier = Modifier,
  snackbar: @Composable (SnackbarData) -> Unit = { snackbarData ->
    if (snackbarData.visuals.message.contains("1DM")) {
      BBQInfoSnackbar(snackbarData)
    } else {
      BBQSnackbar(snackbarData)
    }
  },
) {
  Box(modifier = Modifier.fillMaxSize()) {
    // 在 BoxScope 内部调用 SnackbarHost
    SnackbarHost(
      hostState = hostState,
      modifier = modifier.align(Alignment.TopCenter),
      snackbar = snackbar,
    )
  }
}

/** 自定义基础 DropdownMenu 参数与原版完全一致，默认添加了 surfaceVariant 背景色 */
@Composable
fun BBQDropdownMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  offset: DpOffset = DpOffset(0.dp, 0.dp),
  scrollState: ScrollState = rememberScrollState(),
  properties: PopupProperties = PopupProperties(focusable = true),
  content: @Composable ColumnScope.() -> Unit,
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    offset = offset,
    scrollState = scrollState,
    properties = properties,
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    content = content,
  )
}

/** 自定义 ExposedDropdownMenu 必须在 ExposedDropdownMenuBoxScope 下使用 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.BBQExposedDropdownMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  scrollState: ScrollState = rememberScrollState(),
  content: @Composable ColumnScope.() -> Unit,
) {
  ExposedDropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    scrollState = scrollState,
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    content = content,
  )
}

// 为了方便调用，同时提供一个 Box 的包装（虽然它只是透明转发）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQExposedDropdownMenuBox(
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ExposedDropdownMenuBoxScope.() -> Unit,
) {
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    modifier = modifier,
    content = content,
  )
}

/**
 * 自定义的下拉刷新指示器，使用 MaterialTheme 语义颜色。 兼容 Compose Material 3 1.4.0 及以上版本。
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
