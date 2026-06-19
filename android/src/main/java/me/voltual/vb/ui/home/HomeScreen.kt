package me.voltual.vb.ui.home

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.callback.SingleFolderConflictCallback
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import com.anggrayudi.storage.file.copyFolderTo
import com.anggrayudi.storage.result.SingleFolderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.ui.theme.*
import me.voltual.vb.ui.LocalNavigator
import me.voltual.vb.ui.TerminalExec
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    var selectedFolder by remember { mutableStateOf<DocumentFile?>(null) }
    var selectedFormat by remember { mutableStateOf("JAVA_1_20_5") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var isCopying by remember { mutableStateOf(false) }
    var copyProgress by remember { mutableStateOf(0f) }
    var copyStatusText by remember { mutableStateOf("") }

    val formats = remember {
        listOf(
            "JAVA_1_21",
            "JAVA_1_20_5",
            "JAVA_1_19_4",
            "JAVA_1_18_2",
            "BEDROCK_R21_80",
            "BEDROCK_R20_80",
            "BEDROCK_R19_30"
        )
    }

    val folderPickerLauncher = rememberLauncherForFolderPicker { folder ->
        selectedFolder = folder
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chunker 存档转换器",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

/*            BBQCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    folderPickerLauncher.launch()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "选择文件夹",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "源存档文件夹",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = selectedFolder?.name ?: "点击选择存档文件夹...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedFolder != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }*/

            BBQExposedDropdownMenuBox(
    expanded = dropdownExpanded,
    onExpandedChange = { dropdownExpanded = it },
    modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = selectedFormat,
        onValueChange = {},
        readOnly = true,
        label = { Text("目标转换格式") },
        trailingIcon = { 
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) 
        },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = true
            )
    )
    
    BBQExposedDropdownMenu(
        expanded = dropdownExpanded,
        // 关键点 1：原版 ExposedDropdownMenu 在点击外部或 Item 时会触发 dismiss
        onDismissRequest = { dropdownExpanded = false } 
    ) {
        formats.forEach { format ->
            DropdownMenuItem(
                text = { Text(format) },
                onClick = {
                    selectedFormat = format
                    dropdownExpanded = false // 关键点 2：选中后关闭
                },
                // 确保自适应布局 M3 的通配 padding
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding 
            )
        }
    }
}

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val source = selectedFolder
                    if (source != null) {
                        isCopying = true
                        scope.launch(Dispatchers.IO) {
                            val inputDir = File(context.filesDir, "world_input")
                            if (inputDir.exists()) {
                                inputDir.deleteRecursively()
                            }
                            inputDir.mkdirs()

                            val targetParentDoc = DocumentFile.fromFile(context.filesDir)
                            val conflictCallback = object : SingleFolderConflictCallback(scope) {
                                override fun onParentConflict(
                                    destinationFolder: DocumentFile,
                                    action: ParentFolderConflictAction,
                                    canMerge: Boolean
                                ) {
                                    action.confirmResolution(ConflictResolution.REPLACE)
                                }
                            }

                            source.copyFolderTo(
                                context = context,
                                targetParentFolder = targetParentDoc,
                                skipEmptyFiles = false,
                                newFolderNameInTargetPath = "world_input",
                                onConflict = conflictCallback
                            ).collect { result ->
                                withContext(Dispatchers.Main) {
                                    when (result) {
                                        is SingleFolderResult.Preparing -> {
                                            copyStatusText = "正在准备文件..."
                                        }
                                        is SingleFolderResult.CountingFiles -> {
                                            copyStatusText = "正在计算文件数量..."
                                        }
                                        is SingleFolderResult.Starting -> {
                                            copyStatusText = "开始复制文件..."
                                        }
                                        is SingleFolderResult.InProgress -> {
                                            copyProgress = result.progress / 100f
                                            copyStatusText = "已复制: ${(result.progress).toInt()}% (${Formatter.formatFileSize(context, result.bytesMoved)})"
                                        }
                                        is SingleFolderResult.Completed -> {
                                            isCopying = false
                                            copyStatusText = "复制完成！"
                                            val localInputPath = File(context.filesDir, "world_input").absolutePath
                                            val localOutputPath = File(context.filesDir, "world_output").absolutePath
                                            val outputDir = File(localOutputPath)
                                            if (outputDir.exists()) {
                                                outputDir.deleteRecursively()
                                            }
                                            outputDir.mkdirs()

                                            navigator.navigate(
                                                TerminalExec(
                                                    inputPath = localInputPath,
                                                    outputPath = localOutputPath,
                                                    format = selectedFormat
                                                )
                                            )
                                        }
                                        is SingleFolderResult.Error -> {
                                            isCopying = false
                                            copyStatusText = "复制失败: ${result.errorCode}"
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = selectedFolder != null && !isCopying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("开始复制并转换")
            }
        }

/*        if (isCopying) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("正在处理存档") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = copyStatusText)
                        LinearProgressIndicator(
                            progress = { copyProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }*/
    }
}