package me.voltual.vb.ui.settings.cache

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.core.ui.theme.BBQButton
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

// 定义清理类型
enum class ClearType {
    CONVERSION_ONLY, ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: CacheSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    var pendingClearType by remember { mutableStateOf<ClearType?>(null) }

    Scaffold(
        topBar = {},
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 战术标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "MEMORY_SECTOR_PURGER // 存储扇区清理",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "STATUS // 检测到以下各数据分区缓存残留，清空缓存不影响输入源存档文件安全。",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 数据看板卡片
            BBQCard(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 使用自定义的 Row 渲染函数，确保右侧数值永不折行
        CacheDataRow(label = "■ 转换输出目录 (worlds/world_output)", value = viewModel.outputFolderSize)
        CacheDataRow(label = "■ 打包压缩介质 (worlds/world_output.zip)", value = viewModel.zipFileSize)
        CacheDataRow(label = "■ 应用系统缓存堆栈 (app/cache)", value = viewModel.systemCacheSize)

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom // 改为底部对齐
        ) {
            Text(
                "SECTORS_TOTAL_VOLUME // 缓存总容积", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.weight(1f) // 占据剩余空间
            )
            Text(
                text = viewModel.totalSize, 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.wrapContentWidth() // 确保数值完整显示
            )
        }
    }
}

            Spacer(modifier = Modifier.height(32.dp))

            // 按钮 1：仅清除转换缓存 (安全级警告橙色线条)
            OutlinedButton(
                onClick = { pendingClearType = ClearType.CONVERSION_ONLY },
                shape = AppShapes.small,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "清除转换缓存")
                Spacer(modifier = Modifier.width(12.dp))
                Text("PURGE_CONVERSION_CACHE // 仅清除转换数据", fontWeight = FontWeight.Bold)
            }

            // 按钮 2：清除全部缓存 (高危警告红实色按键)
            Button(
                onClick = { pendingClearType = ClearType.ALL },
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "清除全部缓存")
                Spacer(modifier = Modifier.width(12.dp))
                Text("PURGE_ALL_SECTORS // 物理清除全部缓存", fontWeight = FontWeight.Black)
            }
        }

        // 二次高危指令确认安全阀
        pendingClearType?.let { type ->
            val title = if (type == ClearType.ALL) "CONFIRM_SYSTEM_PURGE" else "CONFIRM_SECTOR_PURGE"
            val text = if (type == ClearType.ALL) {
                "此指令将擦除物理存储介质中的：\n1. 转换导出的世界文件夹及其对应的 zip 压缩包。\n2. 系统底层的临时缓存寄存器文件。\n\n这会导致正在进行中的中转状态丢失，是否继续写入擦除协议？"
            } else {
                "此指令将移除输出目录 worlds/world_output/ 下的全部缓存数据与打包压缩成果。是否继续写入擦除协议？"
            }

            AlertDialog(
                onDismissRequest = { pendingClearType = null },
                shape = AppShapes.medium,
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(title, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                    }
                },
                text = { Text(text, style = MaterialTheme.typography.bodySmall) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (type == ClearType.ALL) {
                                viewModel.clearAllCache()
                            } else {
                                viewModel.clearConversionCache()
                            }
                            pendingClearType = null
                        },
                        shape = AppShapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("EXECUTE_PURGE", fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingClearType = null }, shape = AppShapes.small) {
                        Text("ABORT")
                    }
                }
            )
        }
    }
}

@Composable
private fun CacheDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label, 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f), // 标签占满剩余空间
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), 
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp).wrapContentWidth() // 数值不准折行
        )
    }
}