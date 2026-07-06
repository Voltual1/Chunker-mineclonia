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

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivemc.chunker.nbt.TagType
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag
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

    var activeMenuNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showRenameDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showAddTagDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var editingValueNode by remember { mutableStateOf<NbtUiNode?>(null) }

    val navigationState = me.voltual.vb.ui.LocalNavigationState.current

    LaunchedEffect(isModified, viewModel.editableNbt, viewModel.canUndo, viewModel.canRedo, navigationState.currentRoute) {
        if (navigationState.currentRoute is ChunkNbtEditorDest || navigationState.currentRoute is NbtEditorDest) {
            val actionsList = mutableListOf<TopAppBarAction>()
            
            // 1. 注入撤销（Undo）按钮
            actionsList.add(
                TopAppBarAction(
                    icon = { tint -> Icon(Icons.Default.Undo, contentDescription = "撤销", tint = if (viewModel.canUndo) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.3f)) },
                    description = "撤销",
                    onClick = { viewModel.performUndo() }
                )
            )
            
            // 2. 注入重做（Redo）按钮
            actionsList.add(
                TopAppBarAction(
                    icon = { tint -> Icon(Icons.Default.Redo, contentDescription = "重做", tint = if (viewModel.canRedo) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.3f)) },
                    description = "重做",
                    onClick = { viewModel.performRedo() }
                )
            )

            // 3. 只有被修改过且有内容时才追加保存按钮
            if (isModified) {
                actionsList.add(
                    TopAppBarAction(
                        icon = { tint -> Icon(Icons.Default.Save, contentDescription = "保存", tint = tint) },
                        description = "保存",
                        onClick = {
                            if (viewModel.saveChanges()) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("数据已成功保存！") }
                            }
                        }
                    )
                )
            }

            topAppBarController.updateActions(actionsList)
            topAppBarController.customTitle = viewModel.editableNbt?.getRootTitle() ?: "NBT 属性查看"
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        val rootTag = (editableNbt as? ChunkEditableNbt)?.let {
            val prop = it::class.java.getDeclaredField("rootTag")
            prop.isAccessible = true
            prop.get(it) as? CompoundTag
        }
        
        if (rootTag == null || rootTag.size() == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("当前区块无 NBT 属性", color = Color(0xFF94A3B8))
            }
        } else {
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(16.dp)
            ) {
                key(viewModel.treeVersion) {
                    NbtTreeViewer(
                        rootTag = rootTag,
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
                },
                onEditValue = {
                    editingValueNode = node
                    activeMenuNode = null
                }
            )
        }

        editingValueNode?.let { node ->
            EditValueDialog(
                node = node,
                onDismiss = { editingValueNode = null },
                onConfirm = { newValue ->
                    viewModel.updateTagValue(node, newValue)
                    editingValueNode = null
                }
            )
        }

        showRenameDialogNode?.let { node ->
            RenameDialog(
                initialName = node.key ?: "", // 修复：处理可空 key
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

@Composable
fun NbtNodeContextMenu(
    node: NbtUiNode,
    hasClipboard: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onPasteOverwrite: () -> Unit,
    onPasteSubTag: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onAddSubTag: () -> Unit,
    onEditValue: () -> Unit
) {
    val isContainer = node.tag.type == TagType.COMPOUND || node.tag.type == TagType.LIST
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "操作: ${node.key?.ifEmpty { "Root" } ?: "Root"}") }, // 修复：处理可空 key
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isContainer) {
                    TextButton(onClick = onEditValue, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("修改数值")
                        }
                    }
                }
                TextButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("复制")
                    }
                }
                if (hasClipboard) {
                    TextButton(onClick = onPasteOverwrite, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("覆盖粘贴")
                        }
                    }
                    if (node.isContainer) {
                        TextButton(onClick = onPasteSubTag, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ContentPasteGo, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("粘贴为子节点")
                            }
                        }
                    }
                }
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("重命名键名")
                    }
                }
                if (node.isContainer) {
                    TextButton(onClick = onAddSubTag, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("添加子标签")
                        }
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("删除")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun EditValueDialog(
    node: NbtUiNode,
    onDismiss: () -> Unit,
    onConfirm: (Any) -> Unit
) {
    var textValue by remember { mutableStateOf(node.tag.boxedValue?.toString() ?: "") }
    var checked by remember { mutableStateOf(if (node.tag.boxedValue is Byte) (node.tag.boxedValue == 1.toByte()) else false) }
    // 修复：处理可空 key
    val isBoolean = node.key?.startsWith("is") == true || node.key?.startsWith("has") == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改数值 (${node.tag.type.tagClass?.simpleName})") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (node.tag.type == TagType.BYTE && isBoolean) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("布尔值状态")
                        Switch(checked = checked, onCheckedChange = { checked = it })
                    }
                } else {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text("值") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (node.tag.type == TagType.BYTE && isBoolean) {
                        onConfirm(if (checked) 1.toByte() else 0.toByte())
                    } else {
                        val converted: Any? = when (node.tag.type) {
                            TagType.BYTE -> textValue.toByteOrNull()
                            TagType.SHORT -> textValue.toShortOrNull()
                            TagType.INT -> textValue.toIntOrNull()
                            TagType.LONG -> textValue.toLongOrNull()
                            TagType.FLOAT -> textValue.toFloatOrNull()
                            TagType.DOUBLE -> textValue.toDoubleOrNull()
                            TagType.STRING -> textValue
                            else -> null
                        }
                        if (converted != null) {
                            onConfirm(converted)
                        }
                    }
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名 NBT 键") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("新键名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(newName) }) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubTagDialog(
    isCompound: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, TagType<*, *>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val types = listOf(
        TagType.BYTE, TagType.SHORT, TagType.INT, TagType.LONG,
        TagType.FLOAT, TagType.DOUBLE, TagType.STRING,
        TagType.COMPOUND, TagType.LIST
    )
    var selectedType by remember { mutableStateOf(types.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建新 NBT 子标签") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isCompound) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("键名 (Name)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.tagClass?.simpleName ?: "Unknown",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择标签类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.tagClass?.simpleName ?: "Unknown") },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, selectedType) }) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("取消") }
        }
    )
}