// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.vb.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * 一个可复用的、符合 Material 3 风格的分页控制栏。
 *
 * @param modifier 修饰符。
 * @param currentPage 当前页码。
 * @param totalPages 总页码。
 * @param onPrevClick "上一页" 按钮的点击事件。
 * @param onNextClick "下一页" 按钮的点击事件。
 * @param onPageClick 页码文本的点击事件，通常用于弹出页面跳转对话框。
 * @param isPrevEnabled "上一页" 按钮是否可用。
 * @param isNextEnabled "下一页" 按钮是否可用。
 * @param showTotalPages 是否显示总页数。如果为 false，则只显示当前页码。
 * @param extraControls 一个可选的 Composable lambda，用于在控制栏中添加额外的组件（例如开关）。
 */
@Composable
fun PaginationControls(
  modifier: Modifier = Modifier,
  currentPage: Int,
  totalPages: Int,
  onPrevClick: () -> Unit,
  onNextClick: () -> Unit,
  onPageClick: () -> Unit,
  isPrevEnabled: Boolean,
  isNextEnabled: Boolean,
  showTotalPages: Boolean = true,
  extraControls: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // 将上一页/下一页和页码组合在一起
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onPrevClick, enabled = isPrevEnabled) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "上一页",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // 根据 showTotalPages 参数决定显示内容
      val pageText =
        if (showTotalPages) {
          "第 $currentPage 页 / 共 $totalPages 页"
        } else {
          "第 $currentPage 页"
        }

      Text(
        text = pageText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp).clickable(onClick = onPageClick),
      )

      IconButton(onClick = onNextClick, enabled = isNextEnabled) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "下一页",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // 额外的控制组件
    extraControls()
  }
}

/**
 * 页面跳转对话框。
 *
 * @param currentPage 当前页码。
 * @param totalPages 总页码。
 * @param onDismiss 对话框关闭请求。
 * @param onConfirm 确认跳转页码的回调。
 * @param shape 对话框的形状。
 */
@Composable
fun PageJumpDialog(
  currentPage: Int,
  totalPages: Int,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  var pageInput by remember { mutableStateOf(currentPage.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = shape,
    title = { Text("跳转页面") },
    text = {
      Column {
        Text("请输入页码 (1-$totalPages)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = pageInput,
          onValueChange = { pageInput = it },
          modifier = Modifier.padding(top = 8.dp),
          keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedLabelColor = MaterialTheme.colorScheme.primary,
              cursorColor = MaterialTheme.colorScheme.primary,
              focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val page = pageInput.toIntOrNull()
          if (page != null && page in 1..totalPages) {
            onConfirm(page)
          }
        }
      ) {
        Text("跳转")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}
