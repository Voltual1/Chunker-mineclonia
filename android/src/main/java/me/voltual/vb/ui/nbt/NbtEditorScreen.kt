// Copyright (c) 2026 ivancesaridev (https://github.com/ivancesaridev/json_viewer)
// Modified by Voltual for Vector-Breakthrough
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
    var showSearchBar by remember { mutableStateOf(false) }

    var activeMenuNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showRenameDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showAddTagDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var editingValueNode by remember { mutableStateOf<NbtUiNode?>(null) }

    LaunchedEffect(isModified, viewModel.editableNbt, viewModel.canUndo, viewModel.canRedo) {
        val actionsList = mutableListOf<TopAppBarAction>()
        
        actionsList.add(
            TopAppBarAction(
                icon = { tint -> Icon(Icons.Default.Search, contentDescription = "搜索", tint = tint) },
                description = "搜索",
                onClick = { showSearchBar = !showSearchBar }
            )
        )
        actionsList.add(
            TopAppBarAction(
                icon = { tint -> Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销", tint = if (viewModel.canUndo) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.3f)) },
                description = "撤销",
                onClick = { viewModel.performUndo() }
            )
        )
        actionsList.add(
            TopAppBarAction(
                icon = { tint -> Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做", tint = if (viewModel.canRedo) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.3f)) },
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B10))
    ) {
        // 通过属性访问根节点
        val rootTag = editableNbt.rootTag
        
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.weight(1f).height(48.dp),
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
                                style = MaterialTheme.typography.labelSmall
                            )
                            IconButton(onClick = { viewModel.previousSearchResult() }) {
                                Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.nextSearchResult() }) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (rootTag.size() == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("EMPTY_CELL_STREAM // 区块无有效数据", color = Color(0xFF44474F))
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
                    coroutineScope.launch { snackbarHostState.showSnackbar("DATA_COPIED // 属性已存入寄存器") }
                },
                onPasteOverwrite = {
                    if (viewModel.pasteOverwrite(node)) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("OVERWRITE_OK // 覆盖修改完成") }
                    }
                    activeMenuNode = null
                },
                onPasteSubTag = {
                    if (viewModel.pasteSubTag(node)) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("MOUNT_SUB // 子节点挂载成功") }
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
        title = { Text(text = "INTERRUPT // 操作: ${node.key?.ifEmpty { "Root" } ?: "Root"}") },
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
                            Text("UPDATE_VAL // 更改数值")
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
                        Text("REGISTER_COPY // 复制属性")
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
                            Text("OVERWRITE_PASTE // 覆盖粘贴")
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
                                Text("MOUNT_SUB // 粘贴为子标签")
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
                        Text("RENAME_KEY // 键名重构")
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
                            Text("INSERT_TAG // 插入子属性")
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
                        Text("DELETE_TAG // 彻底删除")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
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
        title = { Text("COMPILATION_VAL (${node.tag.type.tagClass?.simpleName})") },
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
                Text("COMMIT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
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
        title = { Text("RENAME_COMPACT_KEY") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("NEW_IDENTIFIER_NAME") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(newName) }) { Text("REWRITE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
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
        title = { Text("GENERATE_NEW_TAG") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isCompound) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("TAG_IDENTIFIER_NAME") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.tagClass?.simpleName ?: "Unknown",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SELECT_DAT_TYPE") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
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
            Button(onClick = { onConfirm(name, selectedType) }) { Text("COMPILE") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("ABORT") }
        }
    )
}