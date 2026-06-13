// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.animation.*
import me.voltual.vb.core.ui.theme.ThemeCustomizeScreen
import me.voltual.vb.ui.settings.update.UpdateSettingsScreen

@Composable
fun BBQNavDisplay(
  backStack: List<NavKey>,
  onBack: () -> Unit,
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier,
) {
  val mySceneStrategy = remember { DialogSceneStrategy<NavKey>() }
  val slideDistance = rememberSlideDistance() // 获取 30dp 对应的像素值

  val decorators =
    listOf(
      rememberSaveableStateHolderNavEntryDecorator<NavKey>(), // 保持 UI 状态（如滚动位置）
      rememberViewModelStoreNavEntryDecorator<NavKey>(), // 为每个 Entry 提供独立的 ViewModel 存储
    )

  val scope = rememberCoroutineScope()

  NavDisplay(
    backStack = backStack,
    onBack = onBack,
    entryDecorators = decorators, // 传入装饰器
    modifier = modifier.fillMaxSize(),
    sceneStrategy = mySceneStrategy,

    //  前进动画：当新页面入栈时触发
    transitionSpec = { materialSharedAxisX(forward = true, slideDistance = slideDistance) },

    //  返回动画：当页面出栈（Pop）时触发
    popTransitionSpec = { materialSharedAxisX(forward = false, slideDistance = slideDistance) },
    // 使用手动实现的 entryProvider 闭包
    entryProvider = { key ->
      when (key) {
        is Home -> NavEntry(key) {  }

        is ThemeCustomize ->
          NavEntry(key) { ThemeCustomizeScreen(modifier = Modifier.fillMaxSize()) }

        is UpdateSettings ->
          NavEntry(key) { UpdateSettingsScreen(snackbarHostState = snackbarHostState) }
        // 保底逻辑
        else ->
          NavEntry(key) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
              Text("Unknown Key: ${key::class.simpleName}", color = Color.Red)
            }
          }
      }
    },
  )
}