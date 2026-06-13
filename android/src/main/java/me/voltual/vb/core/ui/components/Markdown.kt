// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。

// 该组件Markdown.kt的初始版本来源自https://github.com/rikkahub/rikkahub
// 本版本仅作简化修改适应项目实际用途
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.voltual.vb.core.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

private val flavour by lazy { GFMFlavourDescriptor(makeHttpsAutoLinks = true, useSafeLinks = true) }

private val parser by lazy { MarkdownParser(flavour) }

private val INLINE_LATEX_REGEX = Regex("\\\\\\((.+?)\\\\\\)")
private val BLOCK_LATEX_REGEX = Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)
val THINKING_REGEX = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.DOT_MATCHES_ALL)
private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```|`[^`\n]*`", RegexOption.DOT_MATCHES_ALL)

// 预处理markdown内容
private fun preProcess(content: String): String {
  // 先找出所有代码块的位置
  val codeBlocks = mutableListOf<IntRange>()
  CODE_BLOCK_REGEX.findAll(content).forEach { match -> codeBlocks.add(match.range) }

  // 检查位置是否在代码块内
  fun isInCodeBlock(position: Int): Boolean {
    return codeBlocks.any { range -> position in range }
  }

  // 替换行内公式 \( ... \) 到 $ ... $，但跳过代码块内的内容
  var result =
    INLINE_LATEX_REGEX.replace(content) { matchResult ->
      if (isInCodeBlock(matchResult.range.first)) {
        matchResult.value // 保持原样
      } else {
        "$" + matchResult.groupValues[1] + "$"
      }
    }

  // 替换块级公式 \[ ... \] 到 $$ ... $$，但跳过代码块内的内容
  result =
    BLOCK_LATEX_REGEX.replace(result) { matchResult ->
      if (isInCodeBlock(matchResult.range.first)) {
        matchResult.value // 保持原样
      } else {
        "$$" + matchResult.groupValues[1] + "$$"
      }
    }

  // 替换思考
  result =
    result.replace(THINKING_REGEX) { matchResult ->
      matchResult.groupValues[1].lines().filter { it.isNotBlank() }.joinToString("\n") { ">$it" }
    }

  return result
}

@Composable
fun MarkDownText(
  content: String,
  modifier: Modifier = Modifier,
  style: TextStyle = LocalTextStyle.current,
  onClickCitation: (String) -> Unit = {},
) {
  var (data, setData) =
    remember {
      val preprocessed = preProcess(content)
      val astTree = parser.buildMarkdownTreeFromString(preprocessed)
      mutableStateOf(value = preprocessed to astTree, policy = referentialEqualityPolicy())
    }

  // 监听内容变化，重新解析AST树
  // 这里在后台线程解析AST树, 防止频繁更新的时候掉帧
  val updatedContent by rememberUpdatedState(content)
  LaunchedEffect(Unit) {
    snapshotFlow { updatedContent }
      .distinctUntilChanged()
      .mapLatest {
        val preprocessed = preProcess(it)
        val astTree = parser.buildMarkdownTreeFromString(preprocessed)
        preprocessed to astTree
      }
      .catch { exception -> exception.printStackTrace() }
      .flowOn(Dispatchers.Default) // 在后台线程解析AST树
      .collect { setData(it) }
  }

  val (preprocessed, astTree) = data
  ProvideTextStyle(style) {
    Column(modifier = modifier.padding(start = 4.dp)) {
      astTree.children.fastForEach { child ->
        MarkdownNode(node = child, content = preprocessed, onClickCitation = onClickCitation)
      }
    }
  }
}

object HeaderStyle {
  val H1 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 24.sp)

  val H2 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 20.sp)

  val H3 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 18.sp)

  val H4 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 16.sp)

  val H5 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 14.sp)

  val H6 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
private fun MarkdownNode(
  node: ASTNode,
  content: String,
  modifier: Modifier = Modifier,
  onClickCitation: (String) -> Unit = {},
  listLevel: Int = 0,
) {
  when (node.type) {
    // 文件根节点
    MarkdownElementTypes.MARKDOWN_FILE -> {
      node.children.fastForEach { child ->
        MarkdownNode(
          node = child,
          content = content,
          modifier = modifier,
          onClickCitation = onClickCitation,
        )
      }
    }

    // 段落
    MarkdownElementTypes.PARAGRAPH -> {
      Paragraph(
        node = node,
        content = content,
        modifier = modifier,
        onClickCitation = onClickCitation,
      )
    }

    // 标题
    MarkdownElementTypes.ATX_1,
    MarkdownElementTypes.ATX_2,
    MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4,
    MarkdownElementTypes.ATX_5,
    MarkdownElementTypes.ATX_6 -> {
      val style =
        when (node.type) {
          MarkdownElementTypes.ATX_1 -> HeaderStyle.H1
          MarkdownElementTypes.ATX_2 -> HeaderStyle.H2
          MarkdownElementTypes.ATX_3 -> HeaderStyle.H3
          MarkdownElementTypes.ATX_4 -> HeaderStyle.H4
          MarkdownElementTypes.ATX_5 -> HeaderStyle.H5
          MarkdownElementTypes.ATX_6 -> HeaderStyle.H6
          else -> throw IllegalArgumentException("Unknown header type")
        }
      ProvideTextStyle(value = style) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          node.children.fastForEach { node ->
            if (node.type == MarkdownTokenTypes.ATX_CONTENT) {
              Paragraph(
                node = node,
                content = content,
                onClickCitation = onClickCitation,
                modifier = modifier.padding(vertical = 16.dp),
                trim = true,
              )
            }
          }
        }
      }
    }

    // 列表
    MarkdownElementTypes.UNORDERED_LIST -> {
      UnorderedListNode(
        node = node,
        content = content,
        modifier = modifier.padding(vertical = 4.dp),
        onClickCitation = onClickCitation,
        level = listLevel,
      )
    }

    MarkdownElementTypes.ORDERED_LIST -> {
      OrderedListNode(
        node = node,
        content = content,
        modifier = modifier.padding(vertical = 4.dp),
        onClickCitation = onClickCitation,
        level = listLevel,
      )
    }

    // Checkbox - 简化版本，只显示文本
    GFMTokenTypes.CHECK_BOX -> {
      val isChecked = node.getTextInNode(content).trim() == "[x]"
      val checkText = if (isChecked) "[✓] " else "[ ] "
      Text(text = checkText, modifier = modifier, color = MaterialTheme.colorScheme.primary)
    }

    // 引用块
    MarkdownElementTypes.BLOCK_QUOTE -> {
      ProvideTextStyle(LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)) {
        val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        Column(
          modifier =
            Modifier.drawWithContent {
                drawContent()
                drawRect(color = bgColor, size = size)
                drawRect(color = borderColor, size = Size(10f, size.height))
              }
              .padding(8.dp)
        ) {
          node.children.fastForEach { child ->
            MarkdownNode(node = child, content = content, onClickCitation = onClickCitation)
          }
        }
      }
    }

    // 链接
    MarkdownElementTypes.INLINE_LINK -> {
      val linkText =
        node
          .findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)
          ?.findChildOfTypeRecursive(GFMTokenTypes.GFM_AUTOLINK, MarkdownTokenTypes.TEXT)
          ?.getTextInNode(content) ?: ""
      val linkDest =
        node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
          ?: ""
      val context = LocalContext.current
      Text(
        text = linkText,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier =
          modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, linkDest.toUri())
            context.startActivity(intent)
          },
      )
    }

    // 加粗和斜体
    MarkdownElementTypes.EMPH -> {
      ProvideTextStyle(TextStyle(fontStyle = FontStyle.Italic)) {
        node.children.fastForEach { child ->
          MarkdownNode(
            node = child,
            content = content,
            modifier = modifier,
            onClickCitation = onClickCitation,
          )
        }
      }
    }

    MarkdownElementTypes.STRONG -> {
      ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold)) {
        node.children.fastForEach { child ->
          MarkdownNode(
            node = child,
            content = content,
            modifier = modifier,
            onClickCitation = onClickCitation,
          )
        }
      }
    }

    // GFM 特殊元素
    GFMElementTypes.STRIKETHROUGH -> {
      Text(
        text = node.getTextInNode(content),
        textDecoration = TextDecoration.LineThrough,
        modifier = modifier,
      )
    }

    // 表格 - 简化版本，只显示文本表格
    GFMElementTypes.TABLE -> {
      SimpleTableNode(node = node, content = content, modifier = modifier)
    }

    MarkdownTokenTypes.HORIZONTAL_RULE -> {
      HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        thickness = 0.5.dp,
      )
    }

    /*        // 图片 - 简化版本，只显示链接文本
            MarkdownElementTypes.IMAGE -> {
                val altText = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content) ?: ""
                val imageUrl =
                    node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content) ?: ""
                Text(
                    text = "[图片: $altText]($imageUrl)",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, imageUrl.toUri())
                        LocalContext.current.startActivity(intent)
                    }
                )
            }
    */
    // 行内代码
    MarkdownElementTypes.CODE_SPAN -> {
      val code = node.getTextInNode(content).trim('`')
      Text(
        text = code,
        fontFamily = FontFamily.Monospace,
        modifier =
          modifier
            .background(
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
              RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
      )
    }

    // 代码块
    MarkdownElementTypes.CODE_FENCE -> {
      val contentStartIndex =
        node.children.indexOfFirst { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
      if (contentStartIndex == -1) return
      val eolElement =
        node.children.subList(0, contentStartIndex).findLast { it.type == MarkdownTokenTypes.EOL }
          ?: return
      val codeContentStartOffset = eolElement.endOffset
      val codeContentEndOffset =
        node.children.findLast { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }?.endOffset
          ?: return
      val code = content.substring(codeContentStartOffset, codeContentEndOffset).trimIndent()

      val language =
        node.findChildOfTypeRecursive(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content)
          ?: "plaintext"

      SimpleCodeBlock(
        code = code,
        language = language,
        modifier = modifier.padding(bottom = 4.dp).fillMaxWidth(),
      )
    }

    MarkdownTokenTypes.TEXT -> {
      val text = node.getTextInNode(content)
      Text(text = text, modifier = modifier)
    }

    // 其他类型的节点，递归处理子节点
    else -> {
      // 递归处理其他节点的子节点
      node.children.fastForEach { child ->
        MarkdownNode(
          node = child,
          content = content,
          modifier = modifier,
          onClickCitation = onClickCitation,
        )
      }
    }
  }
}

@Composable
private fun UnorderedListNode(
  node: ASTNode,
  content: String,
  modifier: Modifier = Modifier,
  onClickCitation: (String) -> Unit = {},
  level: Int = 0,
) {
  val bulletStyle =
    when (level % 3) {
      0 -> "• "
      1 -> "◦ "
      else -> "▪ "
    }

  Column(modifier = modifier.padding(start = (level * 8).dp)) {
    node.children.fastForEach { child ->
      if (child.type == MarkdownElementTypes.LIST_ITEM) {
        ListItemNode(
          node = child,
          content = content,
          bulletText = bulletStyle,
          onClickCitation = onClickCitation,
          level = level,
        )
      }
    }
  }
}

@Composable
private fun OrderedListNode(
  node: ASTNode,
  content: String,
  modifier: Modifier = Modifier,
  onClickCitation: (String) -> Unit = {},
  level: Int = 0,
) {
  Column(modifier.padding(start = (level * 8).dp)) {
    var index = 1
    node.children.fastForEach { child ->
      if (child.type == MarkdownElementTypes.LIST_ITEM) {
        val numberText =
          child.findChildOfTypeRecursive(MarkdownTokenTypes.LIST_NUMBER)?.getTextInNode(content)
            ?: "$index. "
        ListItemNode(
          node = child,
          content = content,
          bulletText = numberText,
          onClickCitation = onClickCitation,
          level = level,
        )
        index++
      }
    }
  }
}

@Composable
private fun ListItemNode(
  node: ASTNode,
  content: String,
  bulletText: String,
  onClickCitation: (String) -> Unit = {},
  level: Int,
) {
  Column {
    // 分离列表项的直接内容和嵌套列表
    val (directContent, nestedLists) = separateContentAndLists(node)
    // directContent 渲染处理
    if (directContent.isNotEmpty()) {
      Row {
        Text(text = bulletText, modifier = Modifier.alignByBaseline())
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          itemVerticalAlignment = Alignment.CenterVertically,
        ) {
          directContent.fastForEach { contentChild ->
            MarkdownNode(
              node = contentChild,
              content = content,
              onClickCitation = onClickCitation,
              listLevel = level,
            )
          }
        }
      }
    }
    // nestedLists 渲染处理
    nestedLists.fastForEach { nestedList ->
      MarkdownNode(
        node = nestedList,
        content = content,
        onClickCitation = onClickCitation,
        listLevel = level + 1, // 增加层级
      )
    }
  }
}

// 分离列表项的直接内容和嵌套列表
private fun separateContentAndLists(listItemNode: ASTNode): Pair<List<ASTNode>, List<ASTNode>> {
  val directContent = mutableListOf<ASTNode>()
  val nestedLists = mutableListOf<ASTNode>()
  listItemNode.children.fastForEach { child ->
    when (child.type) {
      MarkdownElementTypes.UNORDERED_LIST,
      MarkdownElementTypes.ORDERED_LIST -> {
        nestedLists.add(child)
      }

      else -> {
        directContent.add(child)
      }
    }
  }
  return directContent to nestedLists
}

@Composable
private fun Paragraph(
  node: ASTNode,
  content: String,
  trim: Boolean = false,
  onClickCitation: (String) -> Unit = {},
  modifier: Modifier,
) {
  if (node.findChildOfTypeRecursive(MarkdownElementTypes.IMAGE) != null) {
    FlowRow(modifier = modifier) {
      node.children.fastForEach { child ->
        MarkdownNode(node = child, content = content, onClickCitation = onClickCitation)
      }
    }
    return
  }

  val colorScheme = MaterialTheme.colorScheme
  val inlineContents = remember { mutableStateMapOf<String, InlineTextContent>() }

  val textStyle = LocalTextStyle.current
  val density = LocalDensity.current
  FlowRow(
    modifier =
      modifier.then(if (node.nextSibling() != null) Modifier.padding(bottom = 4.dp) else Modifier)
  ) {
    val annotatedString =
      remember(content) {
        buildAnnotatedString {
          node.children.fastForEach { child ->
            appendMarkdownNodeContent(
              node = child,
              content = content,
              inlineContents = inlineContents,
              colorScheme = colorScheme,
              onClickCitation = onClickCitation,
              style = textStyle,
              density = density,
              trim = trim,
            )
          }
        }
      }
    Text(
      text = annotatedString,
      modifier = Modifier,
      inlineContent = inlineContents,
      softWrap = true,
      overflow = TextOverflow.Visible,
    )
  }
}

@Composable
private fun SimpleTableNode(node: ASTNode, content: String, modifier: Modifier = Modifier) {
  // 简化表格显示，只显示文本内容
  val rows = node.children.filter { it.type == GFMElementTypes.ROW }

  Column(
    modifier =
      modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)).padding(8.dp)
  ) {
    rows.forEach { row ->
      val cells = row.children.filter { it.type == GFMTokenTypes.CELL }
      Row {
        cells.forEach { cell ->
          Text(
            text = cell.getTextInNode(content).trim(),
            modifier = Modifier.weight(1f).padding(4.dp),
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
      if (row != rows.last()) {
        HorizontalDivider(
          modifier = Modifier.padding(vertical = 4.dp),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        )
      }
    }
  }
}

@Composable
private fun SimpleCodeBlock(code: String, language: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier,
  ) {
    Column {
      // 语言标签
      if (language.isNotBlank() && language != "plaintext") {
        Text(
          text = language,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
      }
      // 代码内容
      Text(
        text = code,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(12.dp),
      )
    }
  }
}

private fun AnnotatedString.Builder.appendMarkdownNodeContent(
  node: ASTNode,
  content: String,
  trim: Boolean = false,
  inlineContents: MutableMap<String, InlineTextContent>,
  colorScheme: ColorScheme,
  density: Density,
  style: TextStyle,
  onClickCitation: (String) -> Unit = {},
) {
  when {
    node.type == MarkdownTokenTypes.BLOCK_QUOTE -> {}

    node.type == GFMTokenTypes.GFM_AUTOLINK -> {
      val link = node.getTextInNode(content)
      withLink(LinkAnnotation.Url(link)) {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(link) }
      }
    }

    node is LeafASTNode -> {
      append(node.getTextInNode(content).let { if (trim) it.trim() else it })
    }

    node.type == MarkdownElementTypes.EMPH -> {
      withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
        node.children.trim(MarkdownTokenTypes.EMPH, 1).fastForEach {
          appendMarkdownNodeContent(
            node = it,
            content = content,
            inlineContents = inlineContents,
            colorScheme = colorScheme,
            density = density,
            style = style,
            onClickCitation = onClickCitation,
          )
        }
      }
    }

    node.type == MarkdownElementTypes.STRONG -> {
      withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
        node.children.trim(MarkdownTokenTypes.EMPH, 2).fastForEach {
          appendMarkdownNodeContent(
            node = it,
            content = content,
            inlineContents = inlineContents,
            colorScheme = colorScheme,
            density = density,
            style = style,
            onClickCitation = onClickCitation,
          )
        }
      }
    }

    node.type == GFMElementTypes.STRIKETHROUGH -> {
      withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
        node.children.trim(GFMTokenTypes.TILDE, 2).fastForEach {
          appendMarkdownNodeContent(
            node = it,
            content = content,
            inlineContents = inlineContents,
            colorScheme = colorScheme,
            density = density,
            style = style,
            onClickCitation = onClickCitation,
          )
        }
      }
    }

    node.type == MarkdownElementTypes.INLINE_LINK -> {
      val linkDest =
        node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
          ?: ""
      val linkText =
        node
          .findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)
          ?.getTextInNode(content)
          ?.trim { it == '[' || it == ']' } ?: linkDest

      withLink(LinkAnnotation.Url(linkDest)) {
        withStyle(
          SpanStyle(color = colorScheme.primary, textDecoration = TextDecoration.Underline)
        ) {
          append(linkText)
        }
      }
    }

    node.type == MarkdownElementTypes.AUTOLINK -> {
      val links = node.children.trim(MarkdownTokenTypes.LT, 1).trim(MarkdownTokenTypes.GT, 1)
      links.fastForEach { link ->
        withLink(LinkAnnotation.Url(link.getTextInNode(content))) {
          withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(link.getTextInNode(content)) }
        }
      }
    }

    node.type == MarkdownElementTypes.CODE_SPAN -> {
      val code = node.getTextInNode(content).trim('`')
      withStyle(
        SpanStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 0.95.em,
          background = colorScheme.secondaryContainer.copy(alpha = 0.2f),
        )
      ) {
        append(code)
      }
    }

    // 其他类型继续递归处理
    else -> {
      node.children.fastForEach {
        appendMarkdownNodeContent(
          node = it,
          content = content,
          inlineContents = inlineContents,
          colorScheme = colorScheme,
          density = density,
          style = style,
          onClickCitation = onClickCitation,
        )
      }
    }
  }
}

private fun ASTNode.getTextInNode(text: String): String {
  return text.substring(startOffset, endOffset)
}

private fun ASTNode.nextSibling(): ASTNode? {
  val brother = this.parent?.children ?: return null
  for (i in brother.indices) {
    if (brother[i] == this) {
      if (i + 1 < brother.size) {
        return brother[i + 1]
      }
    }
  }
  return null
}

private fun ASTNode.findChildOfTypeRecursive(vararg types: IElementType): ASTNode? {
  if (this.type in types) return this
  for (child in children) {
    val result = child.findChildOfTypeRecursive(*types)
    if (result != null) return result
  }
  return null
}

private fun List<ASTNode>.trim(type: IElementType, size: Int): List<ASTNode> {
  if (this.isEmpty() || size <= 0) return this
  var start = 0
  var end = this.size
  // 从头裁剪
  var trimmed = 0
  while (start < end && trimmed < size && this[start].type == type) {
    start++
    trimmed++
  }
  // 从尾裁剪
  trimmed = 0
  while (end > start && trimmed < size && this[end - 1].type == type) {
    end--
    trimmed++
  }
  return this.subList(start, end)
}
