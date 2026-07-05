// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
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
import java.util.concurrent.CompletableFuture

class NbtEditorViewModel : ViewModel() {

    var editableNbt: EditableNbt? by mutableStateOf(null)
        private set

    // 引入树的响应式版本戳，每次数据修改递增以通知 Compose 重组渲染
    var treeVersion by mutableStateOf(0)
        private set

    val visibleNodes = mutableStateListOf<NbtUiNode>()

    private var clipboardTag: Tag<*>? = null
    private var clipboardKey: String = ""

    fun loadNbt(nbt: EditableNbt) {
        this.editableNbt = nbt
        refreshTree()
    }

    fun refreshTree() {
        val nbt = editableNbt ?: return
        visibleNodes.clear()

        fun traverse(key: String, tag: Tag<*>, parent: Tag<*>?, depth: Int, isListElement: Boolean) {
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

    /**
     * 更新值时增加 treeVersion 版本计数，迫使 Compose 重组界面
     */
    fun updateTagValue(node: NbtUiNode, value: Any) {
        try {
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
            treeVersion++ // 递增版本戳
            refreshTree()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyNode(node: NbtUiNode) {
        clipboardTag = node.tag.clone()
        clipboardKey = node.key
    }

    fun hasClipboardData(): Boolean {
        return clipboardTag != null
    }

    @Suppress("UNCHECKED_CAST")
fun pasteOverwrite(node: NbtUiNode): Boolean {
    val copiedTag = clipboardTag?.clone() ?: return false
    val parent = node.parent

    if (parent == null) {
        editableNbt?.let { nbt ->
            // 重构根 CompoundTag 内部的 Map，保留原有节点顺序
            val root = (nbt as ChunkEditableNbt).let {
                val prop = it::class.java.getDeclaredField("rootTag")
                prop.isAccessible = true
                prop.get(it) as CompoundTag
            }
            val originalMap = root.value ?: it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap()
            val newMap = it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap<String, Tag<*>>(originalMap.size)
            
            originalMap.forEach { (k, v) ->
                if (k == node.key) {
                    newMap[clipboardKey.ifEmpty { node.key }] = copiedTag
                } else {
                    newMap[k] = v
                }
            }
            
            val valProp = CompoundTag::class.java.getDeclaredField("value")
            valProp.isAccessible = true
            valProp.set(root, newMap)
            
            nbt.markModified()
            treeVersion++
            refreshTree()
            return true
        }
    } else {
        when (parent) {
            is CompoundTag -> {
                val originalMap = parent.value ?: it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap()
                val newMap = it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap<String, Tag<*>>(originalMap.size)
                
                originalMap.forEach { (k, v) ->
                    if (k == node.key) {
                        newMap[clipboardKey.ifEmpty { node.key }] = copiedTag
                    } else {
                        newMap[k] = v
                    }
                }
                
                val valProp = CompoundTag::class.java.getDeclaredField("value")
                valProp.isAccessible = true
                valProp.set(parent, newMap)
                
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
        if (parent == null) {
            editableNbt?.removeRootTag(node.key)
        } else {
            when (parent) {
                is CompoundTag -> parent.remove(node.key)
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

    if (parent == null) {
        editableNbt?.let { nbt ->
            val root = (nbt as ChunkEditableNbt).let {
                val prop = it::class.java.getDeclaredField("rootTag")
                prop.isAccessible = true
                prop.get(it) as CompoundTag
            }
            val originalMap = root.value ?: it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap()
            if (originalMap.containsKey(newName)) return false
            
            val newMap = it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap<String, Tag<*>>(originalMap.size)
            originalMap.forEach { (k, v) ->
                if (k == node.key) {
                    newMap[newName] = node.tag
                } else {
                    newMap[k] = v
                }
            }
            
            val valProp = CompoundTag::class.java.getDeclaredField("value")
            valProp.isAccessible = true
            valProp.set(root, newMap)
            
            nbt.markModified()
            treeVersion++
            refreshTree()
            return true
        }
    } else if (parent is CompoundTag) {
        val originalMap = parent.value ?: it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap()
        if (originalMap.containsKey(newName)) return false
        
        val newMap = it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap<String, Tag<*>>(originalMap.size)
        originalMap.forEach { (k, v) ->
            if (k == node.key) {
                newMap[newName] = node.tag
            } else {
                newMap[k] = v
            }
        }
        
        val valProp = CompoundTag::class.java.getDeclaredField("value")
        valProp.isAccessible = true
        valProp.set(parent, newMap)
        
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
        return editableNbt?.save() ?: false
    }
}