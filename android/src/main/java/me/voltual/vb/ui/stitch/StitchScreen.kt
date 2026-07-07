package me.voltual.vb.ui.stitch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQButton
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.ui.Export
import me.voltual.vb.ui.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchScreen(
    modifier: Modifier = Modifier,
    viewModel: StitchViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    val sourcePicker = rememberLauncherForFolderPicker { folder ->
        viewModel.sourceFolder = folder
    }

    val destPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.destFolder = folder
    }

    val dimensions = listOf("minecraft:overworld" to "主世界", "minecraft:the_nether" to "下界", "minecraft:the_end" to "末地")
    var dimExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 源世界
            BBQCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (!viewModel.isPreparing && !viewModel.isStitching) sourcePicker.launch() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SOURCE // 提供区块的源世界", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = viewModel.sourceFolder?.name ?: "点击选择文件夹",
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.sourceFolder != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowDownward, null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            }

            // 目标世界
            BBQCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (!viewModel.isPreparing && !viewModel.isStitching) destPicker.launch() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DESTINATION // 被缝合的目标世界 (只覆盖区块, 不动玩家数据)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = viewModel.destFolder?.name ?: "点击选择文件夹",
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.destFolder != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 参数设定
            Text("PARAMETERS // 缝合参数", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)

            ExposedDropdownMenuBox(
                expanded = dimExpanded,
                onExpandedChange = { dimExpanded = !dimExpanded }
            ) {
                OutlinedTextField(
                    value = dimensions.find { it.first == viewModel.dimension }?.second ?: "未知",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("作用维度") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dimExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = AppShapes.small
                )
                ExposedDropdownMenu(expanded = dimExpanded, onDismissRequest = { dimExpanded = false }) {
                    dimensions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.dimension = key
                                dimExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Bounding Box [区块坐标 Chunk Coordinates]:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = viewModel.minX, onValueChange = { viewModel.minX = it }, label = { Text("Min X") }, modifier = Modifier.weight(1f), shape = AppShapes.small, singleLine = true)
                OutlinedTextField(value = viewModel.minZ, onValueChange = { viewModel.minZ = it }, label = { Text("Min Z") }, modifier = Modifier.weight(1f), shape = AppShapes.small, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = viewModel.maxX, onValueChange = { viewModel.maxX = it }, label = { Text("Max X") }, modifier = Modifier.weight(1f), shape = AppShapes.small, singleLine = true)
                OutlinedTextField(value = viewModel.maxZ, onValueChange = { viewModel.maxZ = it }, label = { Text("Max Z") }, modifier = Modifier.weight(1f), shape = AppShapes.small, singleLine = true)
            }

            Spacer(Modifier.height(16.dp))

BBQButton(
    onClick = { viewModel.startStitch() },
    enabled = viewModel.sourceFolder != null && viewModel.destFolder != null && !viewModel.isPreparing && !viewModel.isStitching && !viewModel.stitchSuccess,
    modifier = Modifier.fillMaxWidth().height(56.dp),
    text = {
        Text("EXECUTE STITCH // 启动移植缝合", fontWeight = FontWeight.Black)
    }
)
            }
        }

        // 状态遮罩层
        if (viewModel.isPreparing || viewModel.isStitching || viewModel.stitchSuccess || viewModel.stitchError != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
if (viewModel.stitchSuccess) {
    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(16.dp))
    Text("缝合圆满完成！", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    BBQButton(
        onClick = { navigator.navigate(Export) },
        text = {
            Text("前往导出界面")
        }
    )
} else if (viewModel.stitchError != null) {
                        Text("ERROR // 缝合故障", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text(viewModel.stitchError!!, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { viewModel.stitchError = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("确认") }
                    } else {
                        Text(if (viewModel.isPreparing) viewModel.prepareStatus else "STITCHING IN PROGRESS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(24.dp))
                        val progress = if (viewModel.isPreparing) viewModel.prepareProgress else viewModel.stitchProgress / 100f
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}