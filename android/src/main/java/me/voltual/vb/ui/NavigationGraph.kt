//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import me.voltual.vb.ui.terminal.TerminalScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import me.voltual.vb.ui.TerminalExec
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import me.voltual.vb.core.ui.animation.*
import me.voltual.vb.core.ui.theme.ThemeCustomizeScreen
import me.voltual.vb.ui.home.HomeScreen
import me.voltual.vb.ui.log.LogScreen
import me.voltual.vb.ui.log.LogViewModel
import me.voltual.vb.ui.settings.update.UpdateSettingsScreen
import me.voltual.vb.ui.settings.update.UpdateSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BBQNavDisplay(
    backStack: List<NavKey>,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    // 平台页面注入器：允许 注入所有高耦合页面
    platformEntryProvider: @Composable (NavKey) -> (@Composable () -> Unit)? = { null }
) {
    val mySceneStrategy = remember { DialogSceneStrategy<NavKey>() }
    val slideDistance = rememberSlideDistance()

    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>()
    )

    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryDecorators = decorators,
        modifier = modifier.fillMaxSize(),
        sceneStrategy = mySceneStrategy,

        transitionSpec = {
            materialSharedAxisX(
                forward = true,
                slideDistance = slideDistance
            )
        },

        popTransitionSpec = {
            materialSharedAxisX(
                forward = false,
                slideDistance = slideDistance
            )
        },

        // 统一在 NavEntry 内部处理 Composable 作用域与平台注入
        entryProvider = { key ->
            NavEntry(key) {
                val platformContent = platformEntryProvider(key)
                if (platformContent != null) {
                    platformContent()
                } else {
                    // 匹配通用页面或提供跨平台保底
                    when (key) {
                        is Home -> {
                            HomeScreen(modifier = Modifier.fillMaxSize())
                        }
                        
                        is TerminalExec -> {
                            TerminalScreen(key)
                        }
                        
                                               is LogViewer -> {
                            val viewModel: LogViewModel = koinViewModel()
                            LogScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is ThemeCustomize -> {
                            ThemeCustomizeScreen(modifier = Modifier.fillMaxSize())
                        }

                        is UpdateSettings -> {
                            val viewModel: UpdateSettingsViewModel = koinViewModel()
                            UpdateSettingsScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState
                            )
                        }

                        // 保底逻辑
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Unknown Key: ${key::class.simpleName}", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    )
}