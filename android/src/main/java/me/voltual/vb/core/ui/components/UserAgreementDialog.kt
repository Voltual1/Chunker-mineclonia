// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.core.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.R
import me.voltual.vb.core.ui.animation.materialSharedAxisX
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.data.UserAgreementDataStore
import org.koin.compose.koinInject

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserAgreementDialog(
  onDismissRequest: () -> Unit,
  shape: Shape = AppShapes.medium,
  onAgreed: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val agreementDataStore: UserAgreementDataStore = koinInject()
  var currentAgreementIndex by remember { mutableStateOf(0) }
  val agreementContents = remember { mutableStateMapOf<Int, String>() }
  var animationForward by remember { mutableStateOf(true) }

  // 协议列表
  val agreements = remember { listOf(AgreementItem("《 用户协议》", R.raw.useragreement)) }

  LaunchedEffect(Unit) {
    agreements.forEachIndexed { index, item ->
      val content = withContext(Dispatchers.IO) { loadRawResourceText(context, item.resId) }
      agreementContents[index] = content
    }
  }

  Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Card(
      modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 24.dp),
      shape = shape,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      val mainScrollState = rememberScrollState()

      Column(modifier = Modifier.fillMaxWidth().verticalScroll(mainScrollState).padding(24.dp)) {
        Text(
          text = "服务协议与隐私政策",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
        )

        Text(
          text = "请阅读并同意以下条款以继续使用",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
          targetState = currentAgreementIndex,
          transitionSpec = { materialSharedAxisX(forward = animationForward, slideDistance = 30) },
          label = "AgreementTransition",
        ) { targetIndex ->
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = agreements[targetIndex].title,
              style =
                MaterialTheme.typography.titleMedium.copy(
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold,
                ),
              modifier = Modifier.fillMaxWidth(),
              textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MarkDownText(
              content = agreementContents[targetIndex] ?: "正在加载...",
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 16.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (currentAgreementIndex > 0) {
            FilledTonalButton(
              onClick = {
                animationForward = false
                currentAgreementIndex--
                scope.launch { mainScrollState.animateScrollTo(0) }
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("上一个")
            }
          } else {
            Spacer(modifier = Modifier.weight(1f))
          }

          Button(
            onClick = {
              scope.launch {
                saveAgreement(agreementDataStore, currentAgreementIndex)
                if (currentAgreementIndex < agreements.size - 1) {
                  animationForward = true
                  currentAgreementIndex++
                  mainScrollState.animateScrollTo(0)
                } else {
                  onAgreed()
                }
              }
            },
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = if (currentAgreementIndex < agreements.size - 1) "同意并继续" else "同意并进入",
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

private data class AgreementItem(val title: String, val resId: Int)

private fun loadRawResourceText(context: android.content.Context, resId: Int): String {
  return try {
    context.resources.openRawResource(resId).use { it.bufferedReader().readText() }
  } catch (e: Exception) {
    "加载失败"
  }
}

private suspend fun saveAgreement(ds: UserAgreementDataStore, index: Int) {
  when (index) {
    0 -> ds.acceptUserAgreement()
  }
}
