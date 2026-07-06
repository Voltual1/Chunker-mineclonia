// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.nbt

import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag

class CompoundEditableNbt(
    val rootTag: CompoundTag,
    private val title: String,
    private val onSave: (CompoundTag) -> Boolean
) : EditableNbt() {

    override fun getRootTag(): CompoundTag = rootTag

    override fun getTags(): List<Pair<String, Tag<*>>> {
        val valueMap = rootTag.value ?: return emptyList()
        return valueMap.entries.map { it.key to it.value }
    }

    override fun save(): Boolean {
        val success = onSave(rootTag)
        if (success) {
            clearModified()
        }
        return success
    }

    override fun getRootTitle(): String = title

    override fun addRootTag(name: String, tag: Tag<*>) {
        rootTag.put(name, tag)
        markModified()
    }

    override fun removeRootTag(name: String) {
        rootTag.remove(name)
        markModified()
    }

    fun notifyChange() {
        markModified()
    }
}