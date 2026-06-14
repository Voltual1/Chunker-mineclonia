package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizeScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val themeStore: ThemeColorDataStore = koinInject()

    // 加载初始状态
    var lightColors by remember { mutableStateOf(ThemeColorDataStore.DEFAULT_COLORS.lightSet) }
    var darkColors by remember { mutableStateOf(ThemeColorDataStore.DEFAULT_COLORS.darkSet) }
    
    // 圆屏适配状态
    var roundScreenEnabled by remember { mutableStateOf(false) }
    var roundLeft by remember { mutableStateOf(0f) }
    var roundTop by remember { mutableStateOf(0f) }
    var roundRight by remember { mutableStateOf(0f) }
    var roundBottom by remember { mutableStateOf(0f) }

    // 异步初始化数据
    LaunchedEffect(Unit) {
        val colors = themeStore.colorsFlow.first()
        lightColors = colors.lightSet
        darkColors = colors.darkSet
        
        val paddings = themeStore.roundScreenPaddingFlow.first()
        roundScreenEnabled = paddings.enabled
        roundLeft = paddings.left
        roundTop = paddings.top
        roundRight = paddings.right
        roundBottom = paddings.bottom
    }

    var showSavedMessage by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var translate by remember { mutableStateOf(false) }

    val globalBackgroundPickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onResult = { file -> file?.let { scope.launch { themeStore.saveGlobalBackgroundUri(it.path) } } }
    )

    val lightDrawerBgPickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onResult = { file -> file?.let { scope.launch { themeStore.saveDrawerHeaderLightBackgroundUri(it.path) } } }
    )

    val darkDrawerBgPickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onResult = { file -> file?.let { scope.launch { themeStore.saveDrawerHeaderDarkBackgroundUri(it.path) } } }
    )

    val globalBackgroundUri by themeStore.globalBackgroundUriFlow.collectAsState(initial = null)
    val lightDrawerBgUri by themeStore.drawerHeaderLightBackgroundUriFlow.collectAsState(initial = null)
    val darkDrawerBgUri by themeStore.drawerHeaderDarkBackgroundUriFlow.collectAsState(initial = null)

    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            delay(2000)
            showSavedMessage = false
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("恢复默认主题") },
            text = { Text("确定要恢复所有颜色、设置和背景图片为默认值吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            lightColors = ThemeColorDataStore.DEFAULT_COLORS.lightSet
                            darkColors = ThemeColorDataStore.DEFAULT_COLORS.darkSet
//                            themeStore.saveGlobalBackgroundUri(null)
//                            themeStore.saveDrawerHeaderLightBackgroundUri(null)
//                            themeStore.saveDrawerHeaderDarkBackgroundUri(null)
                            themeStore.saveRoundScreenPaddings(false, 0f, 0f, 0f, 0f)
                            ThemeManager.updateCustomColors(ThemeColorDataStore.DEFAULT_COLORS)
                        }
                        showResetDialog = false
                    }
                ) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("取消") } }
        )
    }

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showSavedMessage) {
                Text(
                    text = "主题已保存！",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showResetDialog = true }) { Icon(Icons.Filled.Refresh, "恢复默认设置") }
                IconButton(onClick = { translate = !translate }) { Icon(Icons.Filled.Language, "翻译") }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
/*                item {
                    GlobalBackgroundEditor(
                        title = "主页背景图片",
                        backgroundUri = globalBackgroundUri,
                        onSelectImage = { globalBackgroundPickerLauncher.launch() },
                        onReset = { scope.launch { themeStore.saveGlobalBackgroundUri(null) } }
                    )
                }*/

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

                item { Text(text = "显示设置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item { SwitchWithText(text = "启用边距调整", checked = roundScreenEnabled, onCheckedChange = { roundScreenEnabled = it }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item { OutlinedTextField(value = roundLeft.toString(), onValueChange = { roundLeft = it.toFloatOrNull() ?: roundLeft }, label = { Text("左内边距 (dp)") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), enabled = roundScreenEnabled) }
                item { OutlinedTextField(value = roundTop.toString(), onValueChange = { roundTop = it.toFloatOrNull() ?: roundTop }, label = { Text("上内边距 (dp)") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), enabled = roundScreenEnabled) }
                item { OutlinedTextField(value = roundRight.toString(), onValueChange = { roundRight = it.toFloatOrNull() ?: roundRight }, label = { Text("右内边距 (dp)") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), enabled = roundScreenEnabled) }
                item { OutlinedTextField(value = roundBottom.toString(), onValueChange = { roundBottom = it.toFloatOrNull() ?: roundBottom }, label = { Text("下内边距 (dp)") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), enabled = roundScreenEnabled) }
                
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

                item {
                    PrimaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("日间模式") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("夜间模式") })
                    }
                }

                when (selectedTab) {
                    0 -> {
/*                        item {
                            DrawerBackgroundEditor(
                                title = "侧边栏背景 (日间)",
                                description = "仅修改侧边栏头部背景图片",
                                backgroundUri = lightDrawerBgUri,
                                onSelectImage = { lightDrawerBgPickerLauncher.launch() },
                                onReset = { scope.launch { themeStore.saveDrawerHeaderLightBackgroundUri(null) } }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) }*/
                        items(lightColors.toList(), key = { "light_" + it.first }) { (name, color) ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ColorEditItem(colorName = name, currentColor = color, onColorChange = { newColor -> lightColors = lightColors.copyWith(name, newColor) }, translate = translate)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                    1 -> {
/*                        item {
                            DrawerBackgroundEditor(
                                title = "侧边栏背景 (夜间)",
                                description = "仅修改侧边栏头部背景图片",
                                backgroundUri = darkDrawerBgUri,
                                onSelectImage = { darkDrawerBgPickerLauncher.launch() },
                                onReset = { scope.launch { themeStore.saveDrawerHeaderDarkBackgroundUri(null) } }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) }*/
                        items(darkColors.toList(), key = { "dark_" + it.first }) { (name, color) ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ColorEditItem(colorName = name, currentColor = color, onColorChange = { newColor -> darkColors = darkColors.copyWith(name, newColor) }, translate = translate)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                val newColors = CustomColorSet(lightColors, darkColors)
                scope.launch {
                    themeStore.saveColors(newColors)
                    themeStore.saveRoundScreenPaddings(roundScreenEnabled, roundLeft, roundTop, roundRight, roundBottom)
                    ThemeManager.updateCustomColors(newColors)
                }
                showSavedMessage = true
            },
            icon = { Icon(Icons.Filled.Save, "保存") },
            text = { Text("保存并应用") },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun DrawerBackgroundEditor(
    title: String,
    description: String,
    backgroundUri: String?,
    onSelectImage: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            DrawerHeaderPreview(modifier = Modifier.fillMaxSize(), backgroundUri = backgroundUri)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSelectImage, modifier = Modifier.weight(1f)) { Text("选择图片") }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("恢复默认") }
        }
    }
}

@Composable
fun ColorEditItem(colorName: String, currentColor: Color, onColorChange: (Color) -> Unit, translate: Boolean) {
    var hexValue by remember(currentColor) { mutableStateOf(currentColor.toHex()) }
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        HsvColorPickerDialog(
            initialColor = currentColor,
            onColorSelected = { newColor ->
                onColorChange(newColor)
                hexValue = newColor.toHex()
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).background(currentColor).border(1.dp, MaterialTheme.colorScheme.outline))
        Text(text = if (translate) colorNameTranslations[colorName] ?: colorName else colorName, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hexValue,
                onValueChange = {
                    val newHex = it.take(6)
                    hexValue = newHex
                    if (newHex.isValidHex()) {
                        onColorChange(newHex.toComposeColor())
                    }
                },
                label = { Text("HEX") },
                modifier = Modifier.width(100.dp),
                maxLines = 1,
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showColorPicker = true }) { Icon(imageVector = Icons.Filled.ColorLens, contentDescription = "选择颜色") }
        }
    }
}

@Composable
fun HsvColorPickerDialog(
    initialColor: Color, 
    onColorSelected: (Color) -> Unit, 
    onDismiss: () -> Unit
) {
    // 这样调用
val hsvArray = initialColor.toHsv()
var hue by remember { mutableStateOf(hsvArray[0]) }
var saturation by remember { mutableStateOf(hsvArray[1]) }
var value by remember { mutableStateOf(hsvArray[2]) }

    val currentColor = remember(hue, saturation, value) { Color.hsv(hue, saturation, value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    )
                    Column {
                        Text("HEX: ${currentColor.toHex()}")
                        Text("RGB: ${currentColor.red.to255()}, ${currentColor.green.to255()}, ${currentColor.blue.to255()}")
                    }
                }
                Text("色相 (0-360°)", style = MaterialTheme.typography.labelMedium)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f, modifier = Modifier.fillMaxWidth())
                
                Text("饱和度 (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
                
                Text("亮度 (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onColorSelected(currentColor) }) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = AppShapes.medium
    )
}

@Composable
private fun GlobalBackgroundEditor(title: String, backgroundUri: String?, onSelectImage: () -> Unit, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (backgroundUri != null) {
                Image(painter = rememberAsyncImagePainter(model = backgroundUri), contentDescription = "Global Background Preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(text = "未选择图片", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSelectImage, modifier = Modifier.weight(1f)) { Text("选择图片") }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("恢复默认") }
        }
    }
}

@Composable
private fun DrawerHeaderPreview(modifier: Modifier = Modifier, backgroundUri: String?) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
        if (backgroundUri != null) {
            Image(painter = rememberAsyncImagePainter(model = backgroundUri), contentDescription = "Drawer Header Background Preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary))
        }
    }
}

fun Color.toHex(): String {
    val rgb = this.toArgb() and 0xFFFFFF
    // 转换为 16 进制字符串，并强制大写
    val hex = rgb.toString(16).uppercase()
    // 确保是 6 位数，不足的前面补 '0'
    return hex.padStart(6, '0')
}
// 替代 android.graphics.Color.parseColor("#$newHex") 的纯 Kotlin 方案
fun String.toComposeColor(): Color {
    // 将 6 位 Hex 字符串转换为 Long，并补上最高位的 Alpha 通道（FF 表示完全不透明）
    val colorLong = this.toLong(16) or 0xFF000000L
    return Color(colorLong)
}

fun String.isValidHex(): Boolean =
    this.length == 6 && this.matches(Regex("[0-9A-Fa-f]{6}"))

fun Float.to255(): Int = (this * 255).roundToInt()

fun Color.toHsv(): FloatArray {
    // 显式指定为 Float，防止跨平台编译器将其误判为内部的 ULong 颜色格式
    val r: Float = this.red
    val g: Float = this.green
    val b: Float = this.blue

    // 显式指定 maxOf / minOf 的泛型类型为 <Float>，彻底解决推导失败
    val max = maxOf<Float>(r, g, b)
    val min = minOf<Float>(r, g, b)
    val delta = max - min

    var h = 0f
    val s = if (max == 0f) 0f else delta / max
    val v = max

    // 修复上一版的拼写错误 (p 改为 !=)
    if (delta != 0f) {
        h = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        h *= 60f
        if (h < 0f) h += 360f
    }

    return floatArrayOf(h, s, v)
}

val colorNameTranslations = mapOf(
    "primary" to "主要颜色",
    "onPrimary" to "主要文字颜色",
    "primaryContainer" to "主要容器颜色",
    "onPrimaryContainer" to "主要容器文字颜色",
    "secondary" to "次要颜色",
    "onSecondary" to "次要文字颜色",
    "secondaryContainer" to "次要容器颜色",
    "onSecondaryContainer" to "次要容器文字颜色",
    "surface" to "表面颜色",
    "onSurface" to "表面文字颜色",
    "surfaceVariant" to "表面变体颜色",
    "onSurfaceVariant" to "表面变体文字颜色",
    "outline" to "轮廓颜色",
    "error" to "错误颜色",
    "onError" to "错误文字颜色",
    "background" to "背景颜色",
    "onBackground" to "背景文字颜色",
    "messageLikeBg" to "点赞消息背景色",
    "messageCommentBg" to "评论消息背景色",
    "messageDefaultBg" to "普通消息背景色",
    "billingIncome" to "收入颜色",
    "billingExpense" to "支出颜色"
)