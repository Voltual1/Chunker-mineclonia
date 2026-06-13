// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui.settings.update

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import me.voltual.vb.core.ui.components.UpdateDialog
import me.voltual.vb.core.utils.UpdateChecker
import me.voltual.vb.data.UpdateInfo
import me.voltual.vb.data.UpdateSettingsDataStore
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class UpdateSettingsViewModel : ViewModel() {

  val autoCheckUpdates: Flow<Boolean> = UpdateSettingsDataStore.autoCheckUpdates

  suspend fun setAutoCheckUpdates(value: Boolean) {
    UpdateSettingsDataStore.setAutoCheckUpdates(value)
  }

  fun checkForUpdates(
    context: Context,
    onUpdateResult: (me.voltual.vb.core.utils.UpdateCheckResult) -> Unit,
  ) {
    UpdateChecker.checkForUpdates(context, onUpdateResult)
  }

  @Composable
  private fun showUpdateDialog(updateInfo: UpdateInfo) {
    UpdateDialog(updateInfo = updateInfo) {}
  }
}
