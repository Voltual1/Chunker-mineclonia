package me.voltual.vb.ui.settings.conversion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: ConversionSettingsViewModel = koinViewModel()
) {
    val threadCount by viewModel.threadCount.collectAsState()
    val processMaps by viewModel.processMaps.collectAsState()
    
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "转换性能配置",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "根据您当前手机的硬件配置，调整多线程并行度以实现最大速度或最大兼容性。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 线程并发滑块
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "并行任务线程数",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$threadCount 线程",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = threadCount.toFloat(),
                    onValueChange = { viewModel.updateThreadCount(it.toInt()) },
                    valueRange = 1f..viewModel.maxCores.toFloat(),
                    steps = if (viewModel.maxCores > 1) viewModel.maxCores - 2 else 0
                )
                
                Text(
                    text = "系统检测到最大可用 CPU 核心数: ${viewModel.maxCores}。在 2GB/4GB 运存的旧机型上请硬性保持为 1 线程以防止 OOM 崩溃。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 地图读取转换开关
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "转换地图数据 (Maps)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "转换地图画作文件。由于地图资源解析极为消耗虚拟机内存，建议默认保持关闭，避免特定存档加载地图 OOM。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = processMaps,
                    onCheckedChange = { viewModel.updateProcessMaps(it) }
                )
            }
        }
    }
}