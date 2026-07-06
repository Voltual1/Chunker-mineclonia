//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.ui.nbt

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.hivemc.chunker.nbt.TagType
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag
import com.hivemc.chunker.nbt.tags.collection.ListTag
import com.hivemc.chunker.nbt.tags.primitive.*
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture

class NbtEditorViewModel : ViewModel() {

    var editableNbt: EditableNbt? by mutableStateOf(null)
        private set

    var treeVersion by mutableStateOf(0)
        private set

    val visibleNodes = mutableStateListOf<NbtUiNode>()

    private var clipboardTag: Tag<*>? = null
    private var clipboardKey: String = ""

    // 历史撤销重做栈
    private val maxHistorySize = 50
    private val undoStack = ArrayDeque<CompoundTag>()
    private val redoStack = ArrayDeque<CompoundTag>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    // 搜索与节点定位状态
    var searchQuery by mutableStateOf("")
        private set
    var searchResults = mutableStateListOf<String>() // 匹配节点的绝对路径列表
    var currentSearchIndex by mutableStateOf(-1)
    val expandedPaths = mutableStateListOf<String>() // 存储需要展开的节点绝对路径

    fun loadNbt(nbt: EditableNbt) {
        this.editableNbt = nbt
        undoStack.clear()
        redoStack.clear()
        searchResults.clear()
        expandedPaths.clear()
        expandedPaths.add("root")
        searchQuery = ""
        currentSearchIndex = -1
        updateHistoryStates()
        refreshTree()
    }

    private fun saveSnapshot() {
        val root = getRootCompound() ?: return
        val snapshot = root.clone() as CompoundTag
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeLast()
        }
        undoStack.push(snapshot)
        redoStack.clear()
        updateHistoryStates()
    }

    private fun updateHistoryStates() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun performUndo() {
        val nbt = editableNbt ?: return
        val root = getRootCompound() ?: return
        if (undoStack.isEmpty()) return

        val currentSnapshot = root.clone() as CompoundTag
        redoStack.push(currentSnapshot)

        val previousSnapshot = undoStack.pop()
        applyRootSnapshot(previousSnapshot)

        nbt.markModified()
        treeVersion++
        updateHistoryStates()
        refreshTree()
    }

    fun performRedo() {
        val nbt = editableNbt ?: return
        val root = getRootCompound() ?: return
        if (redoStack.isEmpty()) return

        val currentSnapshot = root.clone() as CompoundTag
        undoStack.push(currentSnapshot)

        val nextSnapshot = redoStack.pop()
        applyRootSnapshot(nextSnapshot)

        nbt.markModified()
        treeVersion++
        updateHistoryStates()
        refreshTree()
    }

    private fun getRootCompound(): CompoundTag? {
        val nbt = editableNbt as? ChunkEditableNbt ?: return null
        return try {
            val prop = nbt::class.java.getDeclaredField("rootTag")
            prop.isAccessible = true
            prop.get(nbt) as CompoundTag
        } catch (e: Exception) {
            null
        }
    }

    private fun applyRootSnapshot(snapshot: CompoundTag) {
        val nbt = editableNbt as? ChunkEditableNbt ?: return
        try {
            val prop = nbt::class.java.getDeclaredField("rootTag")
            prop.isAccessible = true
            prop.set(nbt, snapshot)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshTree() {
        val nbt = editableNbt ?: return
        visibleNodes.clear()

        fun traverse(key: String?, tag: Tag<*>?, parent: Tag<*>?, depth: Int, isListElement: Boolean, parentPath: String) {
            if (tag == null) return
            // 构造唯一的绝对路径标识
            val currentPath = if (parentPath == "root") "root.${key ?: ""}" else "$parentPath.${key ?: ""}"
            
            val node = NbtUiNode(key, tag, parent, depth, isListElement, id = currentPath)
            node.isExpanded = expandedPaths.contains(currentPath)
            visibleNodes.add(node)

            if (node.isContainer && node.isExpanded) {
                when (tag) {
                    is CompoundTag -> {
                        tag.value?.forEach { (subKey, subTag) ->
                            traverse(subKey, subTag, tag, depth + 1, false, currentPath)
                        }
                    }
                    is ListTag<*, *> -> {
                        tag.value?.forEachIndexed { index, subTag ->
                            traverse("[$index]", subTag, tag, depth + 1, true, currentPath)
                        }
                    }
                }
            }
        }

        nbt.getTags().forEach { (key, tag) ->
            traverse(key, tag, null, 0, false, "root")
        }
    }

    fun toggleNode(path: String) {
        if (expandedPaths.contains(path)) {
            expandedPaths.remove(path)
        } else {
            expandedPaths.add(path)
        }
        refreshTree()
    }

    // 搜索执行与高亮自动展开逻辑 (DFS)
    fun performSearch(query: String) {
        searchQuery = query
        searchResults.clear()
        currentSearchIndex = -1
        if (query.isBlank()) {
            refreshTree()
            return
        }

        val root = getRootCompound() ?: return

        // 递归搜索满足条件的节点并记录路径
        fun searchDfs(key: String, tag: Tag<*>, path: String) {
            val isMatch = key.contains(query, ignoreCase = true) || 
                          (!tag.type.let { it == TagType.COMPOUND || it == TagType.LIST } && 
                           tag.boxedValue?.toString()?.contains(query, ignoreCase = true) == true)
            
            if (isMatch) {
                searchResults.add(path)
            }

            when (tag) {
                is CompoundTag -> {
                    tag.value?.forEach { (subKey, subTag) ->
                        searchDfs(subKey, subTag, "$path.$subKey")
                    }
                }
                is ListTag<*, *> -> {
                    tag.value?.forEachIndexed { index, subTag ->
                        searchDfs("[$index]", subTag, "$path.[$index]")
                    }
                }
            }
        }

        root.value?.forEach { (k, v) ->
            searchDfs(k, v, "root.$k")
        }

        if (searchResults.isNotEmpty()) {
            currentSearchIndex = 0
            navigateToSearchResult(searchResults[0])
        }
        refreshTree()
    }

    /**
     * 自动展开找到的搜索结果所有层级的父节点，并使节点可见
     */
    private fun navigateToSearchResult(path: String) {
        val parts = path.split(".")
        var runningPath = "root"
        // 依次展开每一层父容器
        for (i in 1 until parts.size) {
            runningPath += "." + parts[i]
            if (!expandedPaths.contains(runningPath)) {
                expandedPaths.add(runningPath)
            }
        }
    }

    fun nextSearchResult() {
        if (searchResults.isEmpty()) return
        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size
        navigateToSearchResult(searchResults[currentSearchIndex])
        refreshTree()
    }

    fun previousSearchResult() {
        if (searchResults.isEmpty()) return
        currentSearchIndex = if (currentSearchIndex - 1 < 0) searchResults.size - 1 else currentSearchIndex - 1
        navigateToSearchResult(searchResults[currentSearchIndex])
        refreshTree()
    }

    fun updateTagValue(node: NbtUiNode, value: Any) {
        try {
            saveSnapshot()
            when (val tag = node.tag) {
                is ByteTag -> tag.setValue((value as Number).toByte())
                is ShortTag -> tag.setValue((value as Number).toShort())
                is IntTag -> tag.setValue((value as Number).toInt())
                is LongTag -> tag.setValue((value as Number).toLong())
                is FloatTag -> tag.setValue((value as Number).toFloat())
                is DoubleTag -> tag.setValue((value as Number).toDouble())
                is StringTag -> tag.setValue(value.toString())
            }
            editableNbt?.markModified()
            treeVersion++
            refreshTree()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyNode(node: NbtUiNode) {
        clipboardTag = node.tag.clone()
        clipboardKey = node.key ?: ""
    }

    fun hasClipboardData(): Boolean {
        return clipboardTag != null
    }

    @Suppress("UNCHECKED_CAST")
    fun pasteOverwrite(node: NbtUiNode): Boolean {
        val copiedTag = clipboardTag?.clone() ?: return false
        val parent = node.parent

        saveSnapshot()
        if (parent == null) {
            editableNbt?.let { nbt ->
                val root = getRootCompound() ?: return false
                val originalMap = root.value ?: return false
                val backupList = originalMap.entries.map { it.key to it.value }
                originalMap.clear()
                
                for ((k, v) in backupList) {
                    if (k == node.key) {
                        root.put(clipboardKey.ifEmpty { node.key ?: "" }, copiedTag)
                    } else if (v != null) {
                        root.put(k, v)
                    }
                }
                
                nbt.markModified()
                treeVersion++
                refreshTree()
                return true
            }
        } else {
            when (parent) {
                is CompoundTag -> {
                    val originalMap = parent.value ?: return false
                    val backupList = originalMap.entries.map { it.key to it.value }
                    originalMap.clear()
                    
                    for ((k, v) in backupList) {
                        if (k == node.key) {
                            parent.put(clipboardKey.ifEmpty { node.key ?: "" }, copiedTag)
                        } else if (v != null) {
                            parent.put(k, v)
                        }
                    }
                    
                    editableNbt?.markModified()
                    treeVersion++
                    refreshTree()
                    return true
                }
                is ListTag<*, *> -> {
                    val list = parent.value as? MutableList<Tag<Any>> ?: return false
                    val index = list.indexOf(node.tag as Tag<Any>)
                    if (index != -1) {
                        list[index] = copiedTag as Tag<Any>
                        editableNbt?.markModified()
                        treeVersion++
                        refreshTree()
                        return true
                    }
                }
            }
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    fun pasteSubTag(node: NbtUiNode): Boolean {
        val copiedTag = clipboardTag?.clone() ?: return false
        val tag = node.tag

        saveSnapshot()
        when (tag) {
            is CompoundTag -> {
                val targetKey = if (clipboardKey.startsWith("[")) "PastedTag" else clipboardKey
                tag.put(targetKey, copiedTag)
                node.isExpanded = true
                editableNbt?.markModified()
                treeVersion++
                refreshTree()
                return true
            }
            is ListTag<*, *> -> {
                val listTag = tag as ListTag<Tag<Any>, Any>
                listTag.add(copiedTag as Tag<Any>)
                node.isExpanded = true
                editableNbt?.markModified()
                treeVersion++
                refreshTree()
                return true
            }
        }
        return false
    }

    fun deleteNode(node: NbtUiNode) {
        val parent = node.parent
        saveSnapshot()
        if (parent == null) {
            editableNbt?.removeRootTag(node.key ?: "")
        } else {
            when (parent) {
                is CompoundTag -> parent.remove(node.key ?: "")
                is ListTag<*, *> -> {
                    val list = parent.value
                    list?.remove(node.tag)
                }
            }
            editableNbt?.markModified()
        }
        treeVersion++
        refreshTree()
    }

    fun renameNode(node: NbtUiNode, newName: String): Boolean {
        if (newName.isEmpty() || newName == node.key) return false
        val parent = node.parent

        saveSnapshot()
        if (parent == null) {
            editableNbt?.let { nbt ->
                val root = getRootCompound() ?: return false
                val originalMap = root.value
                if (originalMap == null || originalMap.containsKey(newName)) return false
                
                val backupList = originalMap.entries.map { it.key to it.value }
                originalMap.clear()
                
                for ((k, v) in backupList) {
                    if (k == node.key) {
                        root.put(newName, node.tag)
                    } else if (v != null) {
                        root.put(k, v)
                    }
                }
                
                nbt.markModified()
                treeVersion++
                refreshTree()
                return true
            }
        } else if (parent is CompoundTag) {
            val originalMap = parent.value
            if (originalMap == null || originalMap.containsKey(newName)) return false
            
            val backupList = originalMap.entries.map { it.key to it.value }
            originalMap.clear()
            
            for ((k, v) in backupList) {
                if (k == node.key) {
                    parent.put(newName, node.tag)
                } else if (v != null) {
                    parent.put(k, v)
                }
            }
            
            editableNbt?.markModified()
            treeVersion++
            refreshTree()
            return true
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    fun addSubTag(node: NbtUiNode, name: String, type: TagType<*, *>) : Boolean {
        val constructor = type.constructor ?: return false
        val newTag = constructor.get()

        val target = node.tag
        saveSnapshot()
        when (target) {
            is CompoundTag -> {
                if (name.isEmpty()) return false
                if (target.contains(name)) return false
                target.put(name, newTag)
                node.isExpanded = true
                editableNbt?.markModified()
                treeVersion++
                refreshTree()
                return true
            }
            is ListTag<*, *> -> {
                val listTag = target as ListTag<Tag<Any>, Any>
                listTag.add(newTag as Tag<Any>)
                node.isExpanded = true
                editableNbt?.markModified()
                treeVersion++
                refreshTree()
                return true
            }
        }
        return false
    }

    fun saveChanges(): Boolean {
        val success = editableNbt?.save() ?: false
        if (success) {
            undoStack.clear()
            redoStack.clear()
            updateHistoryStates()
        }
        return success
    }
}