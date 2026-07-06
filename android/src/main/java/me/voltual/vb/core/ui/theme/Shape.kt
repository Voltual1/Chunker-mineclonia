//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.vb.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 战术几何低圆角，体现极客与硬核工业线条
val AppShapes = Shapes(
    small = RoundedCornerShape(2.dp),    // 按钮/徽章/小切口
    medium = RoundedCornerShape(4.dp),   // 卡片/对话框/浮动面板
    large = RoundedCornerShape(8.dp),    // 大卡片/侧边栏容器
    extraLarge = RoundedCornerShape(12.dp)
)

val AppCircleShape = CircleShape