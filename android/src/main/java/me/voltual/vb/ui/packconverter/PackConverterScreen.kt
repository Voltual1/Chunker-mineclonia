package me.voltual.vb.ui.packconverter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.compose.rememberLauncherForFilePicker
import com.anggrayudi.storage.compose.rememberLauncherForFolderPicker
import me.voltual.vb.core.ui.components.MarkDownText
import me.voltual.vb.ui.TerminalViewAndroidView
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackConverterScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: PackConverterViewModel = koinViewModel()
) {
    val packName by viewModel.packName.collectAsState()
    val inputUri by viewModel.inputUri.collectAsState()
    val outputTreeUri by viewModel.outputTreeUri.collectAsState()
    val debugMode by viewModel.debugMode.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val session by viewModel.session.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForFilePicker(
        filterMimeTypes = setOf("application/zip", "application/java-archive"),
        allowMultiple = false
    ) { files ->
        val file = files.firstOrNull()
        if (file != null) {
            viewModel.setInputUri(file.uri, file.name ?: "Unknown")
        }
    }

    val folderPicker = rememberLauncherForFolderPicker { folder ->
        viewModel.setOutputTreeUri(folder.uri)
    }

    val helpMarkdown = """
        ### 仅支持Java版材质包转换成基岩版材质包
        ### 说明：已跳过精灵图(Spritesheet)合并
        为避免在 Android 设备上因大图合并导致转换卡死，本程序在转换时已**跳过精灵图合并**。部分依赖合并精灵图的粒子及 UI 贴图可能无法完美呈现，敬请知悉。

        ### 关于 PackConverter
        本转换器基于上游 [PackConverter](https://github.com/GeyserMC/PackConverter) 开发，上游也目前仍处于开发阶段（Work in Progress）。
        
        **免责声明：**
        1. 转换功能目前极不稳定，**请勿在生产环境中使用，预期可能会遇到各种 Bug！**
        2. 本工具**不支持完整转换自定义物品（Custom Items）**。它仅转换贴图本身，不会生成任何 Geyser 映射。如果需要创建此类映射，请参考 [Rainbow](https://github.com/GeyserMC/Rainbow/) 项目。

        ---
        以下为上游项目介绍：

        # PackConverter

        [![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
        [![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](http://discord.geysermc.org/)

        PackConverter is a library for converting Java Edition resource packs to Bedrock Edition.

        This is based on the Node.js module ConvertJavaTextureToBedrockApi by ozelot379. 

        **Please note, this project is still a work in progress and should not be used on production. Expect bugs!**

        **This project also does not convert custom items fully, it will only convert the textures, but does not create any Geyser mappings.**

        If you are looking for a program capable of creating such custom item mappings, take a look at [Rainbow](https://github.com/GeyserMC/Rainbow/).

        ## Usage
        - Ensure Java is installed, you can use [PaperMC's guide](https://docs.papermc.io/misc/java-install/) on installing java if you do not have Java installed.
        - Download Thunder, the PackConverter GUI, from the Actions tab on GitHub.
        - Double-click on the JAR file to open up the UI, then select your java pack and hit convert!

        ## CLI Usage
        You can also use PackConverter in a CLI, by downloading Thunder (See `Usage`) then running the jar file with some parameters, an example can be seen below:

        ```bash
        java -jar Thunder.jar nogui --input "C:\path\to\pack.zip"
        ```

        You can also enable debug mode by adding `debug` as an additional argument, this also works for the GUI.

        ## Compiling
        1. Clone the repo to your computer
        2. Run gradlew build and locate to bootstrap/build folder.
    """.trimIndent()

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("关于材质包转换") },
            text = {
                Box(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkDownText(content = helpMarkdown)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部栏：标题与帮助按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "材质包转换",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Help,
                    contentDescription = "帮助说明"
                )
            }
        }

        // 材质包名称输入
        OutlinedTextField(
            value = packName,
            onValueChange = { viewModel.setPackName(it) },
            label = { Text("材质包名称") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning,
            singleLine = true
        )

        // 路径选择卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 选择输入文件
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("输入材质包 (Java 版 ZIP)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (inputUri != null) "已选择输入文件" else "未选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (inputUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = { filePicker.launch() },
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("选择")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 选择输出目录
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("输出目录 (基岩版 .mcpack)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (outputTreeUri != null) "已选择输出目录" else "未选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (outputTreeUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = { folderPicker.launch() },
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("选择")
                    }
                }
            }
        }

        // 调试模式开关
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = debugMode,
                onCheckedChange = { viewModel.setDebugMode(it) },
                enabled = !isRunning
            )
            Spacer(Modifier.width(8.dp))
            Text("调试模式 (Debug Mode)", style = MaterialTheme.typography.bodyMedium)
        }

        // 转换按钮
        Button(
            onClick = { viewModel.startConversion() },
            enabled = !isRunning && inputUri != null && outputTreeUri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("转换中...")
            } else {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("开始转换")
            }
        }

        // 输出日志区域
        Text("转换日志输出", style = MaterialTheme.typography.titleSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF121212), MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            session?.let { activeSession ->
                TerminalViewAndroidView(
                    session = activeSession,
                    modifier = Modifier.fillMaxSize(),
                    initialTextSize = 28
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}