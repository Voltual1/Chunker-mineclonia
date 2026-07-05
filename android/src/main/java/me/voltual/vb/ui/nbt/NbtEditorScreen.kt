// Copyright (c) 2026 ivancesaridev (https://github.com/ivancesaridev/json_viewer)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package me.voltual.vb.ui.nbt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.voltual.vb.ui.LocalTopAppBarController
import me.voltual.vb.ui.TopAppBarAction
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NbtEditorScreen(
    editableNbt: EditableNbt,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: NbtEditorViewModel = koinViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val topAppBarController = LocalTopAppBarController.current

    LaunchedEffect(editableNbt) {
        viewModel.loadNbt(editableNbt)
    }

    val isModified = viewModel.editableNbt?.isModified == true
    var expandedPaths by remember { mutableStateOf(setOf("root")) }

    // 上下文与对话框状态
    var activeMenuNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showRenameDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showAddTagDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }

    // 利用 TopAppBarController 动态接管 Toolbar 标题及行为动作
    LaunchedEffect(isModified, viewModel.editableNbt) {
        topAppBarController.updateActions(
            if (isModified) {
                listOf(
                    TopAppBarAction(
                        icon = { tint -> Icon(Icons.Default.Save, contentDescription = "保存", tint = tint) },
                        description = "保存",
                        onClick = {
                            if (viewModel.saveChanges()) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("数据已保存！") }
                            }
                        }
                    )
                )
            } else emptyList()
        )
        topAppBarController.customTitle = viewModel.editableNbt?.getRootTitle() ?: "NBT 属性查看"
    }

    // 销毁时清理自定义 Title
    DisposableEffect(Unit) {
        onDispose {
            topAppBarController.clear()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A)) // 采用深色背景面板渲染高亮树
        ) {
            val rootTag = (editableNbt as? ChunkEditableNbt)?.getTags() // 取出根 Compound
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (rootTag == null || rootTag.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("当前区块无 NBT 属性", color = Color(0xFF94A3B8))
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        // 组装高亮 NBT JSON 树
                        NbtTreeViewer(
                            rootTag = (editableNbt as ChunkEditableNbt).let { 
                                // 获取到底层原始 Compound 节点进行语法树绘制
                                val prop = it::class.java.getDeclaredField("rootTag")
                                prop.isAccessible = true
                                prop.get(it) as CompoundTag
                            },
                            expandedPaths = expandedPaths,
                            onToggleNode = { path ->
                                expandedPaths = if (expandedPaths.contains(path)) {
                                    expandedPaths - path
                                } else {
                                    expandedPaths + path
                                }
                            },
                            onNodeLongClick = { node ->
                                activeMenuNode = node
                            }
                        )
                    }
                }
            }
        }

        // 上下文动作及各项对话框渲染逻辑保持不变...
        activeMenuNode?.let { node ->
            NbtNodeContextMenu(
                node = node,
                hasClipboard = viewModel.hasClipboardData(),
                onDismiss = { activeMenuNode = null },
                onCopy = {
                    viewModel.copyNode(node)
                    activeMenuNode = null
                    coroutineScope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
                },
                onPasteOverwrite = {
                    if (viewModel.pasteOverwrite(node)) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("已覆盖粘贴") }
                    }
                    activeMenuNode = null
                },
                onPasteSubTag = {
                    if (viewModel.pasteSubTag(node)) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("已作为子项粘贴") }
                    }
                    activeMenuNode = null
                },
                onDelete = {
                    viewModel.deleteNode(node)
                    activeMenuNode = null
                },
                onRename = {
                    showRenameDialogNode = node
                    activeMenuNode = null
                },
                onAddSubTag = {
                    showAddTagDialogNode = node
                    activeMenuNode = null
                }
            )
        }

        showRenameDialogNode?.let { node ->
            RenameDialog(
                initialName = node.key,
                onDismiss = { showRenameDialogNode = null },
                onConfirm = { newName ->
                    viewModel.renameNode(node, newName)
                    showRenameDialogNode = null
                }
            )
        }

        showAddTagDialogNode?.let { node ->
            AddSubTagDialog(
                isCompound = node.tag is CompoundTag,
                onDismiss = { showAddTagDialogNode = null },
                onConfirm = { name, type ->
                    viewModel.addSubTag(node, name, type)
                    showAddTagDialogNode = null
                }
            )
        }
    }
}