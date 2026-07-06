// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.nbt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag

/**
 * 封装 NBT 数据的抽象类，用于在 UI 层进行编辑和状态跟踪。
 */
abstract class EditableNbt {

    // 状态跟踪：数据是否被修改
    var isModified by mutableStateOf(false)
        protected set

    // 是否允许修改根节点（例如重命名根标签等）
    var enableRootModifications: Boolean = true

    /**
     * 获取底层的根 CompoundTag。
     * 修复：移除反射，改为抽象方法实现。
     */
    abstract fun getRootTag(): CompoundTag

    /**
     * 设置为已修改状态
     */
    fun markModified() {
        isModified = true
    }

    /**
     * 重置修改状态（通常在保存后调用）
     */
    fun clearModified() {
        isModified = false
    }

    /**
     * 获取根节点的所有子标签。
     */
    abstract fun getTags(): List<Pair<String, Tag<*>>>

    /**
     * 执行保存操作
     * @return 是否保存成功
     */
    abstract fun save(): Boolean

    /**
     * 获取 UI 上显示的根标题
     */
    abstract fun getRootTitle(): String

    /**
     * 向根 Compound 节点添加一个新的标签
     */
    abstract fun addRootTag(name: String, tag: Tag<*>)

    /**
     * 从根 Compound 节点移除一个标签
     */
    abstract fun removeRootTag(name: String)
}