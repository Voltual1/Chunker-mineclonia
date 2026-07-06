// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
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
 * 展平后的 NBT 树节点 UI 状态，支持可空 key 
 */
class NbtUiNode(
    val key: String?,            // 可空键名
    val tag: Tag<*>,
    val parent: Tag<*>?,
    val depth: Int,
    val isListElement: Boolean,
    val id: String = java.util.UUID.randomUUID().toString()
) {
    var isExpanded by mutableStateOf(false)

    val isContainer: Boolean
        get() = tag.type == com.hivemc.chunker.nbt.TagType.COMPOUND || tag.type == com.hivemc.chunker.nbt.TagType.LIST
}