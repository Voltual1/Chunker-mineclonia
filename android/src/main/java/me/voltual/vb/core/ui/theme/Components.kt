// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.vb.core.ui.theme

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

// 战术工业风高亮按钮
@Composable
fun BBQButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  text: @Composable () -> Unit,
  enabled: Boolean = true,
  shape: Shape = AppShapes.small, // 更改为小硬角
  contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
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
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    contentPadding = contentPadding,
  ) {
    text()
  }
}

// 战术框线按钮
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
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), // 增粗线条
    shape = shape,
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        containerColor = Color.Transparent
    ),
    contentPadding = contentPadding,
  ) {
    text()
  }
}

// 战术控制面板卡片 (硬切角与微细框)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  border: BorderStroke? = null,
  shape: Shape = AppShapes.medium,
  content: @Composable () -> Unit,
) {
  val defaultBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  Card(
    modifier = modifier,
    onClick = onClick ?: {},
    shape = shape,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // 扁平化，去除发散阴影
    border = border ?: defaultBorder,
  ) {
    content()
  }
}

// 战术终端功能按键
@Composable
fun BBQIconButton(
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  tint: Color = MaterialTheme.colorScheme.primary,
) {
  IconButton(
    onClick = onClick, 
    modifier = modifier
      .size(48.dp)
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), AppShapes.small)
      .background(MaterialTheme.colorScheme.surfaceContainerLow, AppShapes.small)
  ) {
    Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
  }
}

// 战术带文本开关
@Composable
fun SwitchWithText(
  text: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically, 
    modifier = modifier
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), AppShapes.small)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest, AppShapes.small)
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Switch(
      checked = checked, 
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
      )
    )
    Spacer(Modifier.width(12.dp))
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
  }
}

// 战术通知控制面板
@Composable
fun BBQSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = AppShapes.small,
  containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  actionColor: Color = MaterialTheme.colorScheme.primary,
  dismissActionContentColor: Color = contentColor,
) {
  Snackbar(
    modifier = modifier
      .padding(12.dp)
      .border(1.5.dp, actionColor.copy(alpha = 0.8f), shape), // 赋予边缘指示光带效果
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
    dismissActionContentColor = dismissActionContentColor,
    content = { Text(text = snackbarData.visuals.message, style = MaterialTheme.typography.bodyMedium) },
    action =
      snackbarData.visuals.actionLabel?.let { label ->
        {
          TextButton(
            onClick = { snackbarData.performAction() },
            colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
          ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
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

@Composable
fun BBQSuccessSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = true,
  shape: Shape = AppShapes.small,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionColor = MaterialTheme.colorScheme.primary
  )
}

@Composable
fun BBQErrorSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = AppShapes.small,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
    actionColor = MaterialTheme.colorScheme.error
  )
}

@Composable
fun BBQWarningSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = AppShapes.small,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.messageDefaultBg,
    contentColor = MaterialTheme.colorScheme.onSurface,
    actionColor = MaterialTheme.colorScheme.tertiary
  )
}

@Composable
fun BBQInfoSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = AppShapes.small,
) {
  BBQSnackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = MaterialTheme.messageCommentBg,
    contentColor = MaterialTheme.colorScheme.onSurface,
    actionColor = MaterialTheme.colorScheme.secondary
  )
}

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
    SnackbarHost(
      hostState = hostState,
      modifier = modifier.align(Alignment.TopCenter),
      snackbar = snackbar,
    )
  }
}

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
    modifier = modifier
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), AppShapes.small),
    content = content,
  )
}

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
    modifier = modifier
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), AppShapes.small),
    content = content,
  )
}

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

@Composable
fun BBQPullRefreshIndicator(
  state: PullToRefreshState,
  isRefreshing: Boolean,
  modifier: Modifier = Modifier,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = MaterialTheme.colorScheme.primary,
  containerShape: Shape = AppShapes.small,
) {
  PullToRefreshDefaults.Indicator(
    state = state,
    isRefreshing = isRefreshing,
    modifier = modifier,
    containerColor = backgroundColor,
    color = contentColor,
  )
}