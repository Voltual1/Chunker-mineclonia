package me.voltual.vb.ui.stitch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    val sourcePicker = rememberLauncherForFolderPicker { viewModel.sourceFolder = it }
    val destPicker = rememberLauncherForFolderPicker { viewModel.destFolder = it }

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
            Text("PHASE: STITCH // 存档区块移植", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)

            BBQCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (!viewModel.isPreparing && !viewModel.isStitching) sourcePicker.launch() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SOURCE // 源存档 (提供区块)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(viewModel.sourceFolder?.name ?: "点击选择", fontWeight = FontWeight.Bold)
                }
            }

            BBQCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (!viewModel.isPreparing && !viewModel.isStitching) destPicker.launch() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TARGET // 目标存档 (被覆盖底图)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    Text(viewModel.destFolder?.name ?: "点击选择", fontWeight = FontWeight.Bold)
                }
            }

            // 参数
            OutlinedTextField(
                value = viewModel.dimension,
                onValueChange = { viewModel.dimension = it },
                label = { Text("作用维度") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = viewModel.minX, onValueChange = { viewModel.minX = it }, label = { Text("Min X") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = viewModel.maxX, onValueChange = { viewModel.maxX = it }, label = { Text("Max X") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = viewModel.minZ, onValueChange = { viewModel.minZ = it }, label = { Text("Min Z") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = viewModel.maxZ, onValueChange = { viewModel.maxZ = it }, label = { Text("Max Z") }, modifier = Modifier.weight(1f))
            }

            BBQButton(
                onClick = { viewModel.startStitch() },
                enabled = viewModel.sourceFolder != null && viewModel.destFolder != null && !viewModel.isStitching,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                text = { Text("EXECUTE // 启动缝合", fontWeight = FontWeight.Black) }
            )
        }

        if (viewModel.isPreparing || viewModel.isStitching || viewModel.stitchSuccess || viewModel.stitchError != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (viewModel.stitchSuccess) {
                        Text("SUCCESS // 缝合完成", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        BBQButton(onClick = { navigator.navigate(Export) }, text = { Text("前往导出") })
                    } else if (viewModel.stitchError != null) {
                        Text("ERROR: ${viewModel.stitchError}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.stitchError = null }) { Text("返回") }
                    } else {
                        CircularProgressIndicator()
                        Text(if (viewModel.isPreparing) viewModel.prepareStatus else "缝合进度: ${(viewModel.stitchProgress).toInt()}%")
                    }
                }
            }
        }
    }
}