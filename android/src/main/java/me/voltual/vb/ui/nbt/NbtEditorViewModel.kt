// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证 of the License.
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

    // ==========================================
    // NBT 历史快照撤销/重做管理栈 (限制最大历史为 50 次)
    // ==========================================
    private val maxHistorySize = 50
    private val undoStack = ArrayDeque<CompoundTag>()
    private val redoStack = ArrayDeque<CompoundTag>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun loadNbt(nbt: EditableNbt) {
        this.editableNbt = nbt
        undoStack.clear()
        redoStack.clear()
        updateHistoryStates()
        refreshTree()
    }

    /**
     * 在发生任何数据突变前，克隆并保存快照到撤销栈
     */
    private fun saveSnapshot() {
        val root = getRootCompound() ?: return
        // 执行深克隆，以保存完全独立的历史备份
        val snapshot = root.clone() as CompoundTag
        
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeLast()
        }
        undoStack.push(snapshot)
        // 任何新写入动作发生时，都清空重做栈
        redoStack.clear()
        updateHistoryStates()
    }

    private fun updateHistoryStates() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    /**
     * 撤销操作：弹出最新历史快照，并将当前状态压入重做栈
     */
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

    /**
     * 重做操作：弹出重做快照，并将当前状态压回撤销栈
     */
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

        fun traverse(key: String?, tag: Tag<*>?, parent: Tag<*>?, depth: Int, isListElement: Boolean) {
            if (tag == null) return
            val node = NbtUiNode(key, tag, parent, depth, isListElement)
            visibleNodes.add(node)

            if (node.isContainer && node.isExpanded) {
                when (tag) {
                    is CompoundTag -> {
                        tag.value?.forEach { (subKey, subTag) ->
                            traverse(subKey, subTag, tag, depth + 1, false)
                        }
                    }
                    is ListTag<*, *> -> {
                        tag.value?.forEachIndexed { index, subTag ->
                            traverse("[$index]", subTag, tag, depth + 1, true)
                        }
                    }
                }
            }
        }

        nbt.getTags().forEach { (key, tag) ->
            traverse(key, tag, null, 0, false)
        }
    }

    fun toggleNodeExpansion(node: NbtUiNode) {
        if (!node.isContainer) return
        node.isExpanded = !node.isExpanded
        refreshTree()
    }

    fun updateTagValue(node: NbtUiNode, value: Any) {
        try {
            saveSnapshot() // 保存快照
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

        saveSnapshot() // 保存快照
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

        saveSnapshot() // 保存快照
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
        saveSnapshot() // 保存快照
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

        saveSnapshot() // 保存快照
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
        saveSnapshot() // 保存快照
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
            // 保存成功后清空历史，减少多余内存占用
            undoStack.clear()
            redoStack.clear()
            updateHistoryStates()
        }
        return success
    }
}