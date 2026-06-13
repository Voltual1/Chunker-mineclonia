// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui.settings.update

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.components.UpdateDialog
import me.voltual.vb.core.ui.theme.SwitchWithText
import me.voltual.vb.core.utils.UpdateCheckResult
import me.voltual.vb.data.UpdateInfo
import org.koin.androidx.compose.koinViewModel

@Composable
fun UpdateSettingsScreen(
  viewModel: UpdateSettingsViewModel = koinViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState(initial = false)
  var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
  var showDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    SwitchWithText(
      text = "自动检查更新",
      checked = autoCheckUpdates,
      onCheckedChange = { checked -> scope.launch { viewModel.setAutoCheckUpdates(checked) } },
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
      onClick = {
        viewModel.checkForUpdates(context) { result ->
          when (result) {
            is UpdateCheckResult.Success -> {
              updateInfo = result.updateInfo
              showDialog = true
            }
            is UpdateCheckResult.NoUpdate -> {
              scope.launch { snackbarHostState.showSnackbar(result.message) }
            }
            is UpdateCheckResult.Error -> {
              scope.launch { snackbarHostState.showSnackbar(result.message) }
            }
          }
        }
      },
      modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
      Text("手动检查更新")
    }
  }

  if (showDialog && updateInfo != null) {
    UpdateDialog(updateInfo = updateInfo!!) { showDialog = false }
  }
}
