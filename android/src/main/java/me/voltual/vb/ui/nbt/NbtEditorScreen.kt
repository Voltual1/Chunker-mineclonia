// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.nbt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivemc.chunker.nbt.TagType
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag
import com.hivemc.chunker.nbt.tags.collection.ListTag
import com.hivemc.chunker.nbt.tags.primitive.*
import kotlinx.coroutines.launch
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

    // 首次进入时加载数据源
    LaunchedEffect(editableNbt) {
        viewModel.loadNbt(editableNbt)
    }

    val visibleNodes = viewModel.visibleNodes
    val isModified = viewModel.editableNbt?.isModified == true

    // 上下文菜单与对话框相关状态
    var activeMenuNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showRenameDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }
    var showAddTagDialogNode by remember { mutableStateOf<NbtUiNode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = viewModel.editableNbt?.getRootTitle() ?: "NBT 编辑器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isModified) {
                        IconButton(onClick = {
                            if (viewModel.saveChanges()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("保存成功！")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("保存失败，请重试")
                                }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "保存修改")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isModified) {
                ExtendedFloatingActionButton(
                    text = { Text("保存修改") },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    onClick = {
                        if (viewModel.saveChanges()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("数据已持久化")
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("保存失败")
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(visibleNodes, key = { it.id }) { node ->
                    NbtNodeItem(
                        node = node,
                        onNodeClick = { viewModel.toggleNodeExpansion(node) },
                        onValueChange = { value -> viewModel.updateTagValue(node, value) },
                        onLongClick = { activeMenuNode = node }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }

            // 长按菜单弹出
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

            // 各种对话框处理
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NbtNodeItem(
    node: NbtUiNode,
    onNodeClick: () -> Unit,
    onValueChange: (Any) -> Unit,
    onLongClick: () -> Unit
) {
    val tag = node.tag
    val indentation = (node.depth * 20).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNodeClick() },
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp)
            .padding(start = indentation),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (tag.type) {
            TagType.COMPOUND -> Icons.Default.Folder
            TagType.LIST -> Icons.Default.List
            TagType.BYTE -> Icons.Default.ToggleOn
            TagType.SHORT -> Icons.Default.LooksOne
            TagType.INT -> Icons.Default.Filter1
            TagType.LONG -> Icons.Default.Filter2
            TagType.FLOAT -> Icons.Default.Layers
            TagType.DOUBLE -> Icons.Default.BlurOn
            TagType.STRING -> Icons.Default.TextFields
            else -> Icons.Default.InsertDriveFile
        }

        Icon(
            imageVector = icon,
            contentDescription = tag.type.tagClass?.simpleName ?: "Tag",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.key.ifEmpty { "Root" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))
            NbtValueEditor(node = node, onValueChange = onValueChange)
        }

        if (node.isContainer) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NbtValueEditor(node: NbtUiNode, onValueChange: (Any) -> Unit) {
    when (val tag = node.tag) {
        is ByteTag -> {
            val isBooleanStyle = node.key.startsWith("is") || node.key.startsWith("has")
            if (isBooleanStyle) {
                var checked by remember(tag.value) { mutableStateOf(tag.value == 1.toByte()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            checked = it
                            onValueChange(if (it) 1.toByte() else 0.toByte())
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (checked) "True" else "False", fontSize = 14.sp)
                }
            } else {
                var textValue by remember(tag.value) { mutableStateOf((tag.value.toInt() and 0xFF).toString()) }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        it.toIntOrNull()?.coerceIn(0, 255)?.let { byteVal ->
                            onValueChange(byteVal.toByte())
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            }
        }
        is ShortTag -> {
            var textValue by remember(tag.value) { mutableStateOf(tag.value.toString()) }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    it.toShortOrNull()?.let { shortVal -> onValueChange(shortVal) }
                },
                modifier = Modifier.width(140.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
        is IntTag -> {
            var textValue by remember(tag.value) { mutableStateOf(tag.value.toString()) }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    it.toIntOrNull()?.let { intVal -> onValueChange(intVal) }
                },
                modifier = Modifier.width(180.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
        is LongTag -> {
            var textValue by remember(tag.value) { mutableStateOf(tag.value.toString()) }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    it.toLongOrNull()?.let { longVal -> onValueChange(longVal) }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
        is FloatTag -> {
            var textValue by remember(tag.value) { mutableStateOf(tag.value.toString()) }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    it.toFloatOrNull()?.let { floatVal -> onValueChange(floatVal) }
                },
                modifier = Modifier.width(180.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
        is DoubleTag -> {
            var textValue by remember(tag.value) { mutableStateOf(tag.value.toString()) }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    it.toDoubleOrNull()?.let { doubleVal -> onValueChange(doubleVal) }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
        is StringTag -> {
            var textValue by remember(tag.value) { mutableStateOf(tag.value ?: "") }
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    onValueChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
        is CompoundTag -> {
            Text(text = "Compound: ${tag.size()} 个项目", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is ListTag<*, *> -> {
            Text(text = "List: ${tag.size()} 个元素", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> {
            Text(text = "二进制或不支持直接编辑的 NBT 数据", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onAddSubTag: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "操作: ${node.key.ifEmpty { "Root" }}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("复制")
                    }
                }
                if (hasClipboard) {
                    TextButton(onClick = onPasteOverwrite, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("覆盖粘贴")
                        }
                    }
                    if (node.isContainer) {
                        TextButton(onClick = onPasteSubTag, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Icon(Icons.Default.ContentPasteGo, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("粘贴为子节点")
                            }
                        }
                    }
                }
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("重命名")
                    }
                }
                if (node.isContainer) {
                    TextButton(onClick = onAddSubTag, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
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
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}