// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
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
import com.hivemc.chunker.nbt.tags.primitive.StringTag
import com.hivemc.chunker.nbt.tags.primitive.ByteTag
import com.hivemc.chunker.nbt.tags.primitive.ShortTag
import com.hivemc.chunker.nbt.tags.primitive.IntTag
import com.hivemc.chunker.nbt.tags.primitive.LongTag
import com.hivemc.chunker.nbt.tags.primitive.FloatTag
import com.hivemc.chunker.nbt.tags.primitive.DoubleTag

class NbtEditorViewModel : ViewModel() {

    // 当前正在编辑的 Nbt 封装数据源
    var editableNbt: EditableNbt? by mutableStateOf(null)
        private set

    // 展平后的 UI 节点列表（只包含当前可见/展开的节点）
    val visibleNodes = mutableStateListOf<NbtUiNode>()

    // 全局剪贴板
    private var clipboardTag: Tag<*>? = null
    private var clipboardKey: String = ""

    /**
     * 加载数据源并初始化可见节点
     */
    fun loadNbt(nbt: EditableNbt) {
        this.editableNbt = nbt
        refreshTree()
    }

    /**
     * 重建展平后的可见树形结构
     */
    fun refreshTree() {
        val nbt = editableNbt ?: return
        visibleNodes.clear()

        // 递归生成可见列表
        fun traverse(key: String, tag: Tag<*>, parent: Tag<*>?, depth: Int, isListElement: Boolean) {
            val node = NbtUiNode(key, tag, parent, depth, isListElement)
            visibleNodes.add(node)

            // 如果是复合容器，且处于展开状态，则继续向下递归
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

        // 处理根节点的所有子项
        nbt.getTags().forEach { (key, tag) ->
            traverse(key, tag, null, 0, false)
        }
    }

    /**
     * 展开或折叠容器节点
     */
    fun toggleNodeExpansion(node: NbtUiNode) {
        if (!node.isContainer) return
        node.isExpanded = !node.isExpanded
        refreshTree()
    }

    /**
     * 修改原子节点数值
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 复制节点
     */
    fun copyNode(node: NbtUiNode) {
        clipboardTag = node.tag.clone()
        clipboardKey = node.key
    }

    /**
     * 判断剪贴板是否为空
     */
    fun hasClipboardData(): Boolean {
        return clipboardTag != null
    }

        /**
     * 粘贴并覆盖当前节点
     */
    @Suppress("UNCHECKED_CAST")
    fun pasteOverwrite(node: NbtUiNode): Boolean {
        val copiedTag = clipboardTag?.clone() ?: return false
        val parent = node.parent

        if (parent == null) {
            // 根子节点覆盖
            editableNbt?.let { nbt ->
                nbt.removeRootTag(node.key)
                nbt.addRootTag(clipboardKey.ifEmpty { node.key }, copiedTag)
                refreshTree()
                return true
            }
        } else {
            when (parent) {
                is CompoundTag -> {
                    parent.remove(node.key)
                    parent.put(clipboardKey.ifEmpty { node.key }, copiedTag)
                    editableNbt?.markModified()
                    refreshTree()
                    return true
                }
                is ListTag<*, *> -> {
                    // 使用星投影规避运行期类型擦除，在内部转换为具体类型
                    val list = parent.value as? MutableList<Tag<Any>> ?: return false
                    val index = list.indexOf(node.tag as Tag<Any>)
                    if (index != -1) {
                        list[index] = copiedTag as Tag<Any>
                        editableNbt?.markModified()
                        refreshTree()
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 粘贴为子节点
     */
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
                refreshTree()
                return true
            }
            is ListTag<*, *> -> {
                val listTag = tag as ListTag<Tag<Any>, Any>
                listTag.add(copiedTag as Tag<Any>)
                node.isExpanded = true
                editableNbt?.markModified()
                refreshTree()
                return true
            }
        }
        return false
    }

    /**
     * 删除节点
     */
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
        refreshTree()
    }

    /**
     * 重命名节点（仅限 Compound 里的子项）
     */
    fun renameNode(node: NbtUiNode, newName: String): Boolean {
        if (newName.isEmpty() || newName == node.key) return false
        val parent = node.parent

        if (parent == null) {
            editableNbt?.let { nbt ->
                nbt.removeRootTag(node.key)
                nbt.addRootTag(newName, node.tag)
                refreshTree()
                return true
            }
        } else if (parent is CompoundTag) {
            if (parent.contains(newName)) return false // 重名冲突
            parent.remove(node.key)
            parent.put(newName, node.tag)
            editableNbt?.markModified()
            refreshTree()
            return true
        }
        return false
    }

    

    /**
     * 新建并插入子标签
     */
    @Suppress("UNCHECKED_CAST")
    fun addSubTag(node: NbtUiNode, name: String, type: TagType<*, *>): Boolean {
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
                refreshTree()
                return true
            }
            is ListTag<*, *> -> {
                val listTag = target as ListTag<Tag<Any>, Any>
                listTag.add(newTag as Tag<Any>)
                node.isExpanded = true
                editableNbt?.markModified()
                refreshTree()
                return true
            }
        }
        return false
    }

    /**
     * 保存更改
     */
    fun saveChanges(): Boolean {
        return editableNbt?.save() ?: false
    }
}