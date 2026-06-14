//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** 当前 Navigator 实例 */
val LocalNavigator = compositionLocalOf<Navigator> { error("No Navigator provided") }

/** 当前 NavigationState 实例 */
val LocalNavigationState = compositionLocalOf<NavigationState> { error("No NavigationState provided") }

/** 全局 SnackbarHostState */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

class TopAppBarAction(
  val icon: @Composable (tint: Color) -> Unit,
  val description: String,
  val onClick: () -> Unit,
  val tint: (@Composable () -> Color)? = null,
)

class TopAppBarController {
  var actions by mutableStateOf<List<TopAppBarAction>>(emptyList())
    private set

  var customTitle by mutableStateOf<String?>(null)
  
  // 新增：支持完全自定义的标题区域
  var titleContent by mutableStateOf<(@Composable () -> Unit)?>(null)

  fun updateActions(newActions: List<TopAppBarAction>) {
    actions = newActions
  }

  fun clear() {
    actions = emptyList()
    customTitle = null
    titleContent = null
  }
}

val LocalTopAppBarController = compositionLocalOf<TopAppBarController> { error("No TopAppBarController provided") }