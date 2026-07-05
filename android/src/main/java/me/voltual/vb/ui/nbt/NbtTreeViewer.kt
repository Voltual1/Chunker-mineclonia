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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivemc.chunker.nbt.TagType
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag
import com.hivemc.chunker.nbt.tags.collection.ListTag
import com.hivemc.chunker.nbt.tags.primitive.ByteTag
import com.hivemc.chunker.nbt.tags.primitive.StringTag

@Composable
fun NbtTreeViewer(
    rootTag: Tag<*>,
    expandedPaths: Set<String>,
    onToggleNode: (String) -> Unit,
    onNodeLongClick: (NbtUiNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val syntaxColors = LocalNbtSyntaxColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        NbtNode(
            key = null,
            tag = rootTag,
            path = "root",
            expandedPaths = expandedPaths,
            onToggleNode = onToggleNode,
            onNodeLongClick = onNodeLongClick,
            depth = 0,
            isLast = true,
            syntaxColors = syntaxColors
        )
    }
}

@Composable
private fun NbtNode(
    key: String?,
    tag: Tag<*>,
    path: String,
    expandedPaths: Set<String>,
    onToggleNode: (String) -> Unit,
    onNodeLongClick: (NbtUiNode) -> Unit,
    depth: Int,
    isLast: Boolean,
    syntaxColors: NbtSyntaxColorPalette
) {
    val paddingStart = (depth * 16).dp
    val isExpanded = expandedPaths.contains(path)
    val isContainer = tag.type == TagType.COMPOUND || tag.type == TagType.LIST

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(start = paddingStart, top = 2.dp, bottom = 2.dp)
            .clickable {
                if (isContainer) {
                    onToggleNode(path)
                } else {
                    onNodeLongClick(NbtUiNode(key ?: "", tag, null, depth, false))
                }
            }
    ) {
        if (isContainer) {
            val hasChildren = when (tag) {
                is CompoundTag -> tag.size() > 0
                is ListTag<*, *> -> tag.size() > 0
                else -> false
            }

            if (hasChildren) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 4.dp, top = 4.dp)
                        .clickable { onToggleNode(path) }
                )
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        Column {
            when (tag) {
                is CompoundTag -> {
                    val text = buildAnnotatedString {
                        if (key != null) {
                            withStyle(SpanStyle(color = syntaxColors.key)) { append("\"$key\"") }
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(": ") }
                        }
                        if (tag.size() == 0) {
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append("{}") }
                            if (!isLast) withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(",") }
                        } else if (!isExpanded) {
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append("{ ") }
                            withStyle(SpanStyle(color = syntaxColors.punctuation, background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
                                append("Compound (${tag.size()})") 
                            }
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(" }") }
                            if (!isLast) withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(",") }
                        } else {
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append("{") }
                        }
                    }
                    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    
                    if (isExpanded && tag.size() > 0) {
                        val entries = tag.value?.entries?.toList() ?: emptyList()
                        entries.forEachIndexed { index, entry ->
                            NbtNode(
                                key = entry.key,
                                tag = entry.value,
                                path = "$path.${entry.key}",
                                expandedPaths = expandedPaths,
                                onToggleNode = onToggleNode,
                                onNodeLongClick = onNodeLongClick,
                                depth = depth + 1,
                                isLast = index == entries.lastIndex,
                                syntaxColors = syntaxColors
                            )
                        }
                        Text(
                            text = if (isLast) "}" else "},",
                            color = syntaxColors.punctuation,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                }
                is ListTag<*, *> -> {
                    val text = buildAnnotatedString {
                        if (key != null) {
                            withStyle(SpanStyle(color = syntaxColors.key)) { append("\"$key\"") }
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(": ") }
                        }
                        if (tag.size() == 0) {
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append("[]") }
                            if (!isLast) withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(",") }
                        } else if (!isExpanded) {
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append("[ ") }
                            withStyle(SpanStyle(color = syntaxColors.punctuation, background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
                                append("List (${tag.size()})") 
                            }
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(" ]") }
                            if (!isLast) withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(",") }
                        } else {
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append("[") }
                        }
                    }
                    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

                    if (isExpanded && tag.size() > 0) {
                        val listValues = tag.value ?: emptyList()
                        listValues.forEachIndexed { index, childElement ->
                            NbtNode(
                                key = index.toString(), 
                                tag = childElement,
                                path = "$path[$index]",
                                expandedPaths = expandedPaths,
                                onToggleNode = onToggleNode,
                                onNodeLongClick = onNodeLongClick,
                                depth = depth + 1,
                                isLast = index == listValues.lastIndex,
                                syntaxColors = syntaxColors
                            )
                        }
                        Text(
                            text = if (isLast) "]" else "],",
                            color = syntaxColors.punctuation,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    // 原子节点类型高亮渲染
                    val text = buildAnnotatedString {
                        if (key != null) {
                            withStyle(SpanStyle(color = syntaxColors.key)) { 
                                val keyStr = if (path.endsWith("]")) key else "\"$key\""
                                append(keyStr) 
                            }
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(": ") }
                        }

                        when (tag) {
                            is StringTag -> {
                                withStyle(SpanStyle(color = syntaxColors.string)) { append("\"${tag.value}\"") }
                            }
                            is ByteTag -> {
                                val isBool = key?.startsWith("is") == true || key?.startsWith("has") == true
                                val color = if (isBool) syntaxColors.boolean else syntaxColors.number
                                val valStr = if (isBool) (if (tag.value == 1.toByte()) "true" else "false") else "${tag.value}b"
                                withStyle(SpanStyle(color = color)) { append(valStr) }
                            }
                            else -> {
                                withStyle(SpanStyle(color = syntaxColors.number)) { append(tag.boxedValue.toString()) }
                            }
                        }
                        
                        if (!isLast) {
                            withStyle(SpanStyle(color = syntaxColors.punctuation)) { append(",") }
                        }
                    }
                    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}