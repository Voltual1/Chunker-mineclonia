// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.voltual.vb.BBQApplication
import me.voltual.vb.R
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.core.ui.theme.BBQSnackbarHost
import me.voltual.vb.core.ui.theme.BBQTheme
import me.voltual.vb.core.ui.theme.ThemeManager

class CrashLogActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 先从 Intent 获取，如果没有再从数据库加载
    val initialCrashReport = intent.getStringExtra("CRASH_REPORT")

    setContent {
      val crashReportState = remember { mutableStateOf(initialCrashReport ?: "Loading...") }
      val context = LocalContext.current
      val snackbarHostState = remember { SnackbarHostState() } // 创建 SnackbarHostState
      val scope = rememberCoroutineScope() // 创建 CoroutineScope

      LaunchedEffect(Unit) {
        if (initialCrashReport == null) { // 如果没有传递参数，才从数据库加载
          CoroutineScope(Dispatchers.IO).launch {
            val logEntry =
              BBQApplication.instance.database.logDao().getAllLogs().first().firstOrNull {
                it.type == "CRASH"
              }
            val crashReport = logEntry?.responseBody ?: "No crash report available."
            crashReportState.value = crashReport
          }
        }
      }

      BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
        Scaffold(
          snackbarHost = { BBQSnackbarHost(snackbarHostState) },
          modifier = Modifier.fillMaxSize(),
          content = { innerPadding ->
            Surface(
              modifier = Modifier.fillMaxSize().padding(innerPadding),
              color = MaterialTheme.colorScheme.background,
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                CrashLogScreen(crashReport = crashReportState.value)

                FloatingActionButton(
                  onClick = {
                    val clipboard =
                      context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Crash Report", crashReportState.value)
                    clipboard.setPrimaryClip(clip)
                    scope.launch {
                      snackbarHostState.showSnackbar(
                        message =
                          context.getString(
                            R.string.crash_report_copied
                          ), // 使用 stringResource 获取字符串
                        duration = SnackbarDuration.Short,
                      )
                    }
                  },
                  modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                  Icon(Icons.Filled.ContentCopy, "复制崩溃报告")
                }
              }
            }
          },
        )
      }
    }
  }

  companion object {
    fun start(context: Context, crashReport: String) {
      val intent =
        Intent(context, CrashLogActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          putExtra("CRASH_REPORT", crashReport) // 将崩溃报告传递给 Activity
        }
      context.startActivity(intent)
    }
  }
}

@Composable
fun CrashLogScreen(crashReport: String) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // 使用 MaterialTheme.colorScheme.errorContainer 作为背景色
        Text(
          text = "应用崩溃了！",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onErrorContainer, // 使用语义颜色
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "崩溃报告：",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant, // 使用语义颜色
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = crashReport,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface, // 使用语义颜色
        )
      }
    }
  }
}
