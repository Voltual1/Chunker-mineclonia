// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/** Create a navigation state that persists config changes and process death. */
@Composable
fun rememberNavigationState(startRoute: NavKey, topLevelRoutes: Set<NavKey>): NavigationState {
  val topLevelRoute =
    rememberSerializable(
      startRoute,
      topLevelRoutes,
      serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
      mutableStateOf(startRoute)
    }

  val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

  return remember(startRoute, topLevelRoutes) {
    NavigationState(startRoute = startRoute, topLevelRoute = topLevelRoute, backStacks = backStacks)
  }
}

class NavigationState(
  val startRoute: NavKey,
  topLevelRoute: MutableState<NavKey>,
  val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
  var topLevelRoute: NavKey by topLevelRoute

  val currentRoute: NavKey?
    get() = backStacks[topLevelRoute]?.lastOrNull() ?: backStacks[startRoute]?.lastOrNull()

  val stacksInUse: List<NavKey>
    get() =
      if (topLevelRoute == startRoute) {
        listOf(startRoute)
      } else {
        listOf(startRoute, topLevelRoute)
      }

  fun resetToStart() {
    topLevelRoute = startRoute

    // 遍历堆栈进行清理
    backStacks.forEach { (key, stack) ->
      if (key == startRoute) {
        while (stack.size > 1) {
          stack.removeLastOrNull()
        }
      } else {
        if (stack.isNotEmpty()) {
          while (stack.size > 1) {
            stack.removeLastOrNull()
          }
        }
      }
    }
  }
}

/** Convert NavigationState into NavEntries. */
@Composable
fun NavigationState.toEntries(
  entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {

  val decoratedEntries = backStacks.mapValues { (_, stack) ->
    val decorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>())
    rememberDecoratedNavEntries(
      backStack = stack,
      entryDecorators = decorators,
      entryProvider = entryProvider,
    )
  }

  return remember(topLevelRoute, startRoute, decoratedEntries) {
    val routesInUse =
      if (topLevelRoute == startRoute) {
        listOf(startRoute)
      } else {
        listOf(startRoute, topLevelRoute)
      }

    routesInUse.flatMap { decoratedEntries[it] ?: emptyList() }.toMutableStateList()
  }
}
