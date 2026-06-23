//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui

import androidx.compose.ui.focus.FocusManager
import androidx.navigation3.runtime.NavKey

/** Handles navigation events (forward and back) by updating the navigation state. */
class Navigator(
  val state: NavigationState,
  private val focusManager: FocusManager? = null,
  private val topAppBarController: TopAppBarController? = null,
) {
  private fun forceCleanup() {
    focusManager?.clearFocus(force = true) 
    topAppBarController?.clear()
  }

  fun logoutAndReset() {
    state.resetToStart()
  }

  fun navigate(route: NavKey) {
    forceCleanup()

    if (route in state.backStacks.keys) {
      state.topLevelRoute = route
    } else {
      val currentStack = state.backStacks[state.topLevelRoute]
      
      if (route is Export && currentStack != null) {
          currentStack.removeAll { it is TerminalExec }
          
      }
      currentStack?.add(route)
    }
  }

  fun goBack() {
    forceCleanup()

    val currentStack =
      state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")

    if (currentStack.lastOrNull() == state.topLevelRoute) {
      state.topLevelRoute = state.startRoute
    } else {
      currentStack.removeLastOrNull()
    }
  }
}