// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.vb.core.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    shape: Shape = AppShapes.medium,
    onAgreed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val agreementDataStore: UserAgreementDataStore = koinInject()
    var currentAgreementIndex by remember { mutableStateOf(0) }
    val agreementContents = remember { mutableStateMapOf<Int, String>() }
    var animationForward by remember { mutableStateOf(true) }

    val agreements = remember { 
        listOf(
            AgreementItem("《用户协议及隐私政策》", R.raw.useragreement)
        ) 
    }

    LaunchedEffect(Unit) {
        agreements.forEachIndexed { index, item ->
            val content = withContext(Dispatchers.IO) { loadRawResourceText(context, item.resId) }
            agreementContents[index] = content
        }
    }

    Dialog(
        onDismissRequest = {}, 
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, shape), // 强对比战术边框
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 扁平化
        ) {
            val mainScrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxWidth().verticalScroll(mainScrollState).padding(20.dp)) {
                // 顶部战术标志区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM_ACCESS_PROTOCOL",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary))
                }

                Text(
                    text = "PROTOCOL_VERIFICATION_REQUIRED // 需要您确认数据接入规范",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = TextAlign.Center
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 加载区硬边缘卡片包裹
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), AppShapes.small),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = AppShapes.small
                        ) {
                            Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                                MarkDownText(
                                    content = agreementContents[targetIndex] ?: "正在解密核心数据流...",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
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
                            shape = AppShapes.small,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("PREV // 上一页", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // 第一个协议时展示“不同意”按钮 (战术阻断红色样式)
                        Button(
                            onClick = {
                                scope.launch {
                                    agreementContents[0] = """
                                        ### ACCESS_DENIED // 授权阻断
                                        
                                        很抱歉，拒绝本项目的《用户协议及隐私政策》将导致底层数据分析链无法成功构建。
                                        
                                        如需退出程序，请执行：
                                        ■ **关闭后台应用** 或 
                                        ■ **返回手机主屏幕 (HOME)** 
                                        
                                        期待您的下一次接入认证。
                                    """.trimIndent()
                                    mainScrollState.animateScrollTo(0)
                                }
                            },
                            shape = AppShapes.small,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text("REJECT // 拒绝授权", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
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
                        shape = AppShapes.small,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (currentAgreementIndex < agreements.size - 1) "CONTINUE" else "ACCEPT // 同意并加载",
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            fontSize = 12.sp
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
        "FATAL_ERROR: 核心加密数据流加载失败，请检查链路或重启系统"
    }
}

private suspend fun saveAgreement(ds: UserAgreementDataStore, index: Int) {
    when (index) {
        0 -> ds.acceptUserAgreement()
    }
}