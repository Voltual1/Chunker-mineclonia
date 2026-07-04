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

/**
 * 展平后的 NBT 树节点 UI 状态
 */
class NbtUiNode(
    val key: String,             // 键名（若为 List 的子元素，则一般是索引 "[0]", "[1]" 等）
    val tag: Tag<*>,             // Chunker NBT 标签实例
    val parent: Tag<*>?,         // 父节点标签引用（用于操作数据源）
    val depth: Int,              // 缩进深度
    val isListElement: Boolean,  // 标识该节点是否是 ListTag 的直接子项
    val id: String = java.util.UUID.randomUUID().toString() // 唯一标识符，作为 Compose Key
) {
    // 复合节点（Compound / List）是否展开
    var isExpanded by mutableStateOf(false)

    // 缓存当前节点是否为容器节点
    val isContainer: Boolean
        get() = tag.type == com.hivemc.chunker.nbt.TagType.COMPOUND || tag.type == com.hivemc.chunker.nbt.TagType.LIST
}