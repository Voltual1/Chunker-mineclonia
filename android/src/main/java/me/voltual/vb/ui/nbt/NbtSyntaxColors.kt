// Copyright (c) 2026 ivancesaridev (https://github.com/ivancesaridev/json_viewer)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package me.voltual.vb.ui.nbt

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NbtSyntaxColorPalette(
    val key: Color = Color(0xFFF43F5E),         // 红色 (NBT Key)
    val punctuation: Color = Color(0xFF94A3B8), // 灰色 (大括号，冒号)
    val string: Color = Color(0xFF34D399),      // 绿色 (StringTag)
    val number: Color = Color(0xFF38BDF8),      // 蓝色 (Byte/Short/Int/Long/Float/Double)
    val boolean: Color = Color(0xFFF59E0B),     // 黄色 (布尔型 Byte)
    val nullValue: Color = Color(0xFF64748B)    // 深灰 (空值)
)

val LocalNbtSyntaxColors = staticCompositionLocalOf { NbtSyntaxColorPalette() }