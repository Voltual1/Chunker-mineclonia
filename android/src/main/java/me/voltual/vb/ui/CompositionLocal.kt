// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** 当前 Navigator 实例，供任何 Composable 发起类型安全导航。 */
val LocalNavigator = compositionLocalOf<Navigator> { error("No Navigator provided") }

/** 当前 NavigationState 实例，供需要读取状态的 Composable 使用。 */
val LocalNavigationState =
  compositionLocalOf<NavigationState> { error("No NavigationState provided") }

/** 全局 SnackbarHostState，避免层层传递。 */
val LocalSnackbarHostState =
  compositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

class TopAppBarAction(
  val icon: ImageVector,
  val description: String,
  val onClick: () -> Unit,
  // 改为 Lambda，默认返回 null 表示使用默认颜色
  val tint: (@Composable () -> Color)? = null,
)

class TopAppBarController {
  // 将变量设为 private，避免自动生成公开的 setActions 方法
  var actions by mutableStateOf<List<TopAppBarAction>>(emptyList())
    private set // 重点：只有内部能改，外部只能看或调用 fun

  var customTitle by mutableStateOf<String?>(null)

  // 此时这个方法就不会和自动生成的 setter 冲突了
  fun updateActions(newActions: List<TopAppBarAction>) {
    actions = newActions
  }

  fun clear() {
    actions = emptyList()
    customTitle = null
  }
}

val LocalTopAppBarController =
  compositionLocalOf<TopAppBarController> { error("No TopAppBarController provided") }
