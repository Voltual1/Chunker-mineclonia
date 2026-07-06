package me.voltual.vb.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var lightColors by remember { mutableStateOf(ThemeColorDataStore.DEFAULT_COLORS.lightSet) }
    var darkColors by remember { mutableStateOf(ThemeColorDataStore.DEFAULT_COLORS.darkSet) }
    
    var roundScreenEnabled by remember { mutableStateOf(false) }
    var roundLeft by remember { mutableStateOf(0f) }
    var roundTop by remember { mutableStateOf(0f) }
    var roundRight by remember { mutableStateOf(0f) }
    var roundBottom by remember { mutableStateOf(0f) }

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
            shape = AppShapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("RESTORE_THEME_CONFIRM", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                }
            },
            text = { Text("确定要清除所有自定义色彩寄存器和边距，恢复系统默认战术规范（Default Colors）吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            lightColors = ThemeColorDataStore.DEFAULT_COLORS.lightSet
                            darkColors = ThemeColorDataStore.DEFAULT_COLORS.darkSet
                            themeStore.saveRoundScreenPaddings(false, 0f, 0f, 0f, 0f)
                            ThemeManager.updateCustomColors(ThemeColorDataStore.DEFAULT_COLORS)
                        }
                        showResetDialog = false
                    },
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("RESTORE") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("ABORT") } }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showSavedMessage) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = AppShapes.small
                ) {
                    Text(
                        text = "APPLY_SUCCESS // 外观控制台参数重载完毕",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // 头部功能操作区
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(4.dp).height(20.dp).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "VISUAL_CALIBRATOR // 外观校准",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), AppShapes.small).size(36.dp)
                    ) { Icon(Icons.Filled.Refresh, "恢复", modifier = Modifier.size(18.dp)) }
                    IconButton(
                        onClick = { translate = !translate },
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), AppShapes.small).size(36.dp)
                    ) { Icon(Icons.Filled.Language, "翻译", modifier = Modifier.size(18.dp)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
                // 1. 物理安全间距校准
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), AppShapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "SAFE_MARGINS_CALIBRATION // 安全边界微调", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.primary)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("启用非对称安全边距", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = roundScreenEnabled, 
                                onCheckedChange = { roundScreenEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        // 紧凑四方向输入网格
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = roundLeft.toString(), 
                                onValueChange = { roundLeft = it.toFloatOrNull() ?: roundLeft }, 
                                label = { Text("L_PAD (DP)", fontSize = 10.sp) }, 
                                modifier = Modifier.weight(1f), 
                                enabled = roundScreenEnabled,
                                singleLine = true,
                                shape = AppShapes.small
                            )
                            OutlinedTextField(
                                value = roundTop.toString(), 
                                onValueChange = { roundTop = it.toFloatOrNull() ?: roundTop }, 
                                label = { Text("T_PAD (DP)", fontSize = 10.sp) }, 
                                modifier = Modifier.weight(1f), 
                                enabled = roundScreenEnabled,
                                singleLine = true,
                                shape = AppShapes.small
                            )
                            OutlinedTextField(
                                value = roundRight.toString(), 
                                onValueChange = { roundRight = it.toFloatOrNull() ?: roundRight }, 
                                label = { Text("R_PAD (DP)", fontSize = 10.sp) }, 
                                modifier = Modifier.weight(1f), 
                                enabled = roundScreenEnabled,
                                singleLine = true,
                                shape = AppShapes.small
                            )
                            OutlinedTextField(
                                value = roundBottom.toString(), 
                                onValueChange = { roundBottom = it.toFloatOrNull() ?: roundBottom }, 
                                label = { Text("B_PAD (DP)", fontSize = 10.sp) }, 
                                modifier = Modifier.weight(1f), 
                                enabled = roundScreenEnabled,
                                singleLine = true,
                                shape = AppShapes.small
                            )
                        }
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }

                // 2. 战术色彩通道选择
                item {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab, 
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        indicator = { tabPositions ->
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0, 
                            onClick = { selectedTab = 0 }, 
                            text = { Text("LIGHT_MODE_CH // 日间通道", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1, 
                            onClick = { selectedTab = 1 }, 
                            text = { Text("DARK_MODE_CH // 夜间通道", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }

                when (selectedTab) {
                    0 -> {
                        items(lightColors.toList(), key = { "light_" + it.first }) { (name, color) ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ColorEditItem(colorName = name, currentColor = color, onColorChange = { newColor -> lightColors = lightColors.copyWith(name, newColor) }, translate = translate)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            }
                        }
                    }
                    1 -> {
                        items(darkColors.toList(), key = { "dark_" + it.first }) { (name, color) ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ColorEditItem(colorName = name, currentColor = color, onColorChange = { newColor -> darkColors = darkColors.copyWith(name, newColor) }, translate = translate)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }

        // 宽体战术保存按钮
        Button(
            onClick = {
                val newColors = CustomColorSet(lightColors, darkColors)
                scope.launch {
                    themeStore.saveColors(newColors)
                    themeStore.saveRoundScreenPaddings(roundScreenEnabled, roundLeft, roundTop, roundRight, roundBottom)
                    ThemeManager.updateCustomColors(newColors)
                }
                showSavedMessage = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 4.dp),
            shape = AppShapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Save, "保存")
            Spacer(modifier = Modifier.width(12.dp))
            Text("COMMIT_AND_LOAD // 写入并生效", fontWeight = FontWeight.Black)
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

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(currentColor).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), AppShapes.small))
        Text(
            text = if (translate) colorNameTranslations[colorName] ?: colorName else colorName.uppercase(), 
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp), 
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
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
                modifier = Modifier.width(90.dp).height(46.dp),
                maxLines = 1,
                singleLine = true,
                shape = AppShapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showColorPicker = true },
                modifier = Modifier.size(36.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), AppShapes.small)
            ) { Icon(imageVector = Icons.Filled.ColorLens, contentDescription = "选择颜色", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun HsvColorPickerDialog(
    initialColor: Color, 
    onColorSelected: (Color) -> Unit, 
    onDismiss: () -> Unit
) {
    val hsvArray = initialColor.toHsv()
    var hue by remember { mutableStateOf(hsvArray[0]) }
    var saturation by remember { mutableStateOf(hsvArray[1]) }
    var value by remember { mutableStateOf(hsvArray[2]) }

    val currentColor = remember(hue, saturation, value) { Color.hsv(hue, saturation, value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CALIBRATE_COLOR // 色彩校准", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, AppShapes.small)
                    )
                    Column {
                        Text("HEX_VAL: #${currentColor.toHex()}", style = MaterialTheme.typography.labelSmall)
                        Text("RGB_VAL: ${currentColor.red.to255()}, ${currentColor.green.to255()}, ${currentColor.blue.to255()}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("色相 HUE (0-360°)", style = MaterialTheme.typography.labelMedium)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f, modifier = Modifier.fillMaxWidth())
                
                Text("饱和度 SATURATION (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
                
                Text("亮度 VALUE (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onColorSelected(currentColor) }, shape = AppShapes.small) { Text("COMMIT") } },
        dismissButton = { TextButton(onClick = onDismiss, shape = AppShapes.small) { Text("ABORT") } },
        shape = AppShapes.medium
    )
}

fun Color.toHex(): String {
    val rgb = this.toArgb() and 0xFFFFFF
    val hex = rgb.toString(16).uppercase()
    return hex.padStart(6, '0')
}

fun String.toComposeColor(): Color {
    val colorLong = this.toLong(16) or 0xFF000000L
    return Color(colorLong)
}

fun String.isValidHex(): Boolean =
    this.length == 6 && this.matches(Regex("[0-9A-Fa-f]{6}"))

fun Float.to255(): Int = (this * 255).roundToInt()

fun Color.toHsv(): FloatArray {
    val r: Float = this.red
    val g: Float = this.green
    val b: Float = this.blue

    val max = maxOf<Float>(r, g, b)
    val min = minOf<Float>(r, g, b)
    val delta = max - min

    var h = 0f
    val s = if (max == 0f) 0f else delta / max
    val v = max

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
    "primary" to "核心主色",
    "onPrimary" to "主要前景文字",
    "primaryContainer" to "主要容器色彩",
    "onPrimaryContainer" to "主要容器文字",
    "secondary" to "次要辅助色",
    "onSecondary" to "次要前景文字",
    "secondaryContainer" to "次要容器色彩",
    "onSecondaryContainer" to "次要容器文字",
    "surface" to "操作控制台表面",
    "onSurface" to "控制台表面文字",
    "surfaceVariant" to "终端表面变体",
    "onSurfaceVariant" to "终端变体文字",
    "outline" to "战术框架边缘",
    "error" to "严重故障警报",
    "onError" to "严重故障文字",
    "background" to "系统最深背景",
    "onBackground" to "深色背景文字",
    "messageLikeBg" to "系统绿色高亮",
    "messageCommentBg" to "系统蓝色高亮",
    "messageDefaultBg" to "系统黄色高亮",
    "billingIncome" to "正常链路指示",
    "billingExpense" to "阻断异常指示"
)