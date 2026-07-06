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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivemc.chunker.nbt.TagType
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag
import kotlinx.coroutines.launch
import me.voltual.vb.core.ui.theme.AppShapes
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
    var showSearchBar by remember { mutableStateOf(false) }

    // 上下文与对话框状态
    var activeMenuNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showRenameDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showAddTagDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var editingValueNode by remember { mutableStateOf<NbtUiNode?>(null) }

    // 动态在 TopAppBar 注册动作
    LaunchedEffect(isModified, viewModel.editableNbt, viewModel.canUndo, viewModel.canRedo) {
        val actionsList = mutableListOf<TopAppBarAction>()
        
        actionsList.add(
            TopAppBarAction(
                icon = { tint -> Icon(Icons.Default.Search, contentDescription = "搜索属性", tint = tint) },
                description = "搜索属性",
                onClick = { showSearchBar = !showSearchBar }
            )
        )
        actionsList.add(
            TopAppBarAction(
                icon = { tint -> Icon(Icons.Default.Undo, contentDescription = "撤销", tint = if (viewModel.canUndo) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.3f)) },
                description = "撤销",
                onClick = { viewModel.performUndo() }
            )
        )
        actionsList.add(
            TopAppBarAction(
                icon = { tint -> Icon(Icons.Default.Redo, contentDescription = "重做", tint = if (viewModel.canRedo) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.3f)) },
                description = "重做",
                onClick = { viewModel.performRedo() }
            )
        )
        if (isModified) {
            actionsList.add(
                TopAppBarAction(
                    icon = { tint -> Icon(Icons.Default.Save, contentDescription = "保存", tint = tint) },
                    description = "保存",
                    onClick = {
                        if (viewModel.saveChanges()) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("SAVE_OK // 写入字节流成功") }
                        }
                    }
                )
            )
        }

        topAppBarController.updateActions(actionsList)
        topAppBarController.customTitle = viewModel.editableNbt?.getRootTitle() ?: "NBT COMPILER"
    }

    DisposableEffect(Unit) {
        onDispose {
            topAppBarController.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B10)) // 极硬灰黑底色
    ) {
        val rootTag = (editableNbt as? ChunkEditableNbt)?.let {
            val prop = it::class.java.getDeclaredField("rootTag")
            prop.isAccessible = true
            prop.get(it) as? CompoundTag
        }
        
        Column(modifier = Modifier.fillMaxSize()) {
            // 战术风格可折叠搜索条
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.performSearch(it) },
                            placeholder = { Text("FILTER_QUERY // 匹配键/值...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = AppShapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.tertiary, // 高亮警告黄
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            trailingIcon = {
                                if (viewModel.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.performSearch("") }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            }
                        )
                        if (viewModel.searchResults.isNotEmpty()) {
                            Text(
                                text = "MATCH // ${viewModel.currentSearchIndex + 1}/${viewModel.searchResults.size}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.previousSearchResult() }) {
                                Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.nextSearchResult() }) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (rootTag == null || rootTag.size() == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("EMPTY_CELL_STREAM // 区块无有效 NBT 载荷", color = Color(0xFF44474F), style = MaterialTheme.typography.labelSmall)
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
                        // 顶部极客说明饰条
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "NBT_COMPILER_REGISTRY // DECRYPTING_BYTE_STREAM",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }

                        key(viewModel.treeVersion, viewModel.searchQuery) {
                            NbtTreeViewer(
                                rootTag = rootTag,
                                expandedPaths = viewModel.expandedPaths.toSet(),
                                onToggleNode = { path -> viewModel.toggleNode(path) },
                                onNodeLongClick = { node ->
                                    activeMenuNode = node
                                },
                                searchQuery = viewModel.searchQuery
                            )
                        }
                    }
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
                    coroutineScope.launch { snackbarHostState.showSnackbar("DATA_COPIED // 已载入临时寄存器") }
                },
                onPasteOverwrite = {
                    if (viewModel.pasteOverwrite(node)) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("OVERWRITE_OK // 覆盖修改完成") }
                    }
                    activeMenuNode = null
                },
                onPasteSubTag = {
                    if (viewModel.pasteSubTag(node)) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("MOUNT_SUB // 已挂载为子标签") }
                    }
                    activeMenuNode = null
                },
                onDelete = {
                    viewModel.deleteNode(node)
                    activeMenuNode = null
                    coroutineScope.launch { snackbarHostState.showSnackbar("DELETE_OK // 字段已抹除") }
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
                initialName = node.key ?: "",
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
        shape = AppShapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = "INTERRUPT // 操作: ${node.key?.ifEmpty { "Root" } ?: "Root"}", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isContainer) {
                    TextButton(onClick = onEditValue, modifier = Modifier.fillMaxWidth(), shape = AppShapes.small) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("UPDATE_VAL // 更改数值")
                        }
                    }
                }
                TextButton(onClick = onCopy, modifier = Modifier.fillMaxWidth(), shape = AppShapes.small) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("REGISTER_COPY // 复制属性")
                    }
                }
                if (hasClipboard) {
                    TextButton(onClick = onPasteOverwrite, modifier = Modifier.fillMaxWidth(), shape = AppShapes.small) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("OVERWRITE_PASTE // 覆盖粘贴")
                        }
                    }
                    if (node.isContainer) {
                        TextButton(onClick = onPasteSubTag, modifier = Modifier.fillMaxWidth(), shape = AppShapes.small) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ContentPasteGo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("MOUNT_SUB // 粘贴为子标签")
                            }
                        }
                    }
                }
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth(), shape = AppShapes.small) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("RENAME_KEY // 键名重构")
                    }
                }
                if (node.isContainer) {
                    TextButton(onClick = onAddSubTag, modifier = Modifier.fillMaxWidth(), shape = AppShapes.small) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("INSERT_TAG // 插入子属性")
                        }
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("DELETE_TAG // 彻底删除")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, shape = AppShapes.small) { Text("ABORT") }
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
    val isBoolean = node.key?.startsWith("is") == true || node.key?.startsWith("has") == true

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppShapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("COMPILATION_VAL (${node.tag.type.tagClass?.simpleName})", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (node.tag.type == TagType.BYTE && isBoolean) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("布尔链路状态 BOOLEAN_STATE")
                        Switch(checked = checked, onCheckedChange = { checked = it })
                    }
                } else {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text("BINARY_DATA_VALUE") },
                        singleLine = true,
                        shape = AppShapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                shape = AppShapes.small,
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
                Text("COMMIT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = AppShapes.small) { Text("ABORT") }
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
        shape = AppShapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("RENAME_COMPACT_KEY", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("NEW_IDENTIFIER_NAME") },
                singleLine = true,
                shape = AppShapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(newName) }, shape = AppShapes.small) { Text("REWRITE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = AppShapes.small) { Text("ABORT") }
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
        shape = AppShapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("GENERATE_NEW_TAG", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isCompound) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("TAG_IDENTIFIER_NAME") },
                        singleLine = true,
                        shape = AppShapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
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
                        label = { Text("SELECT_DAT_TYPE") },
                        shape = AppShapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
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
            Button(onClick = { onConfirm(name, selectedType) }, shape = AppShapes.small) { Text("COMPILE") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }, shape = AppShapes.small) { Text("ABORT") }
        }
    )
}