package me.voltual.vb.ui.settings.chunker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.theme.AppShapes
import me.voltual.vb.core.ui.theme.BBQCard
import me.voltual.vb.data.model.ConversionManifest
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun BreakpointManagerScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BreakpointManagerViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<Any>()

    val filteredList = remember(state.manifests, state.searchQuery) {
        state.manifests.filter {
            it.worldId.contains(state.searchQuery, ignoreCase = true) ||
            it.inputPath.contains(state.searchQuery, ignoreCase = true) ||
            it.outputPath.contains(state.searchQuery, ignoreCase = true)
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = paneNavigator,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        listPane = {
            AnimatedPane {
                Scaffold(
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BREAKPOINT_REGISTRY // 续转断点物理库",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Filter WorldID / Paths ...", fontSize = 12.sp) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                shape = AppShapes.small,
                                maxLines = 1,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    val newManifest = viewModel.createEmptyManifest()
                                    paneNavigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        newManifest
                                    )
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = AppShapes.medium
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Create Breakpoint")
                        }
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO_BREAKPOINTS_FOUND // 空注册段",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = filteredList, key = { it.worldId }) { manifest ->
                                BreakpointItemCard(
                                    manifest = manifest,
                                    onClick = {
                                        scope.launch {
                                            paneNavigator.navigateTo(
                                                ListDetailPaneScaffoldRole.Detail,
                                                manifest
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        detailPane = {
            val selectedKey = paneNavigator.currentDestination?.contentKey
            AnimatedPane {
                if (selectedKey is ConversionManifest) {
                    BreakpointEditorPane(
                        manifest = selectedKey,
                        onSave = { originalId, manifest ->
                            viewModel.saveManifest(originalId, manifest)
                            scope.launch {
                                snackbarHostState.showSnackbar("SAVE_OK // 断点覆写写入成功")
                                paneNavigator.navigateBack()
                            }
                        },
                        onDelete = { worldId ->
                            viewModel.deleteManifest(worldId)
                            scope.launch {
                                snackbarHostState.showSnackbar("DELETE_OK // 断点注册段已被物理抹除")
                                paneNavigator.navigateBack()
                            }
                        },
                        onDismiss = {
                            scope.launch { paneNavigator.navigateBack() }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "SELECT_A_BREAKPOINT_RECORD_TO_EDIT // 请选择左侧注册表条目进行热改",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun BreakpointItemCard(
    manifest: ConversionManifest,
    onClick: () -> Unit
) {
    BBQCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = manifest.worldId,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (manifest.isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "INDEX: ${manifest.progressIndex}  //  FORMAT: ${manifest.format}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "IN: ${manifest.inputPath}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = "OUT: ${manifest.outputPath}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BreakpointEditorPane(
    manifest: ConversionManifest,
    onSave: (String?, ConversionManifest) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    var worldId by remember(manifest) { mutableStateOf(manifest.worldId) }
    var inputPath by remember(manifest) { mutableStateOf(manifest.inputPath) }
    var outputPath by remember(manifest) { mutableStateOf(manifest.outputPath) }
    var format by remember(manifest) { mutableStateOf(manifest.format) }
    var progressIndexStr by remember(manifest) { mutableStateOf(manifest.progressIndex.toString()) }
    var base64Key by remember(manifest) { mutableStateOf(manifest.lastBedrockKeyBase64 ?: "") }
    var isActive by remember(manifest) { mutableStateOf(manifest.isActive) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONFIGURATION // 断点覆写终端",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Pane")
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // WorldId Editor
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("WORLD_ID // 唯一识别标识", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = worldId,
                onValueChange = { worldId = it },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.small,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }

        // Input Path
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("INPUT_PATH // 输入世界源路径", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = inputPath,
                onValueChange = { inputPath = it },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.small,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        // Output Path
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("OUTPUT_PATH // 转换输出目标路径", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = outputPath,
                onValueChange = { outputPath = it },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.small,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        // Format
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TARGET_FORMAT // 目标格式标识符", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = format,
                onValueChange = { format = it },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.small,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }

        // ProgressIndex
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PROGRESS_INDEX // 当前物理断点偏置量", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = progressIndexStr,
                onValueChange = { progressIndexStr = it },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.small,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }

        // Bedrock Base64 Key
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("LAST_BEDROCK_KEY_BASE64 // 基岩迭代偏移哈希 (Base64)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = base64Key,
                onValueChange = { base64Key = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("None (Null pointer)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) },
                shape = AppShapes.small,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }

        // Active State Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIVE_CONVERSION_TASK // 设置为当前活跃任务",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "系统在重起续转时，会且仅会重拉 ACTIVE 断点执行并载入。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = { isActive = it },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onDelete(manifest.worldId) },
                modifier = Modifier.weight(1f),
                shape = AppShapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ERASE", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val progressVal = progressIndexStr.toIntOrNull() ?: 0
                    val updated = ConversionManifest(
                        worldId = worldId,
                        inputPath = inputPath,
                        outputPath = outputPath,
                        format = format,
                        progressIndex = progressVal,
                        lastBedrockKeyBase64 = base64Key.takeIf { it.isNotBlank() },
                        isActive = isActive
                    )
                    onSave(manifest.worldId, updated)
                },
                modifier = Modifier.weight(1.5f),
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("OVERWRITE_SAVE", fontWeight = FontWeight.Black)
            }
        }
    }
}