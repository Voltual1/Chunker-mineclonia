//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.vb.core.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemeManager {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    
    var customColorSet by mutableStateOf<CustomColorSet?>(null)
    
    fun updateCustomColors(colors: CustomColorSet) {
        customColorSet = colors
    }
    
    /**
     * 根据当前模式和系统状态计算最终是否为暗色
     */
    fun calculateIsDark(systemIsDark: Boolean): Boolean {
        return when (themeMode) {
            ThemeMode.SYSTEM -> systemIsDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    /**
     * 切换主题逻辑：系统 -> 亮色 -> 暗色 循环
     */
    fun toggleTheme(systemIsDark: Boolean) {
        themeMode = when (themeMode) {
            ThemeMode.SYSTEM -> if (systemIsDark) ThemeMode.LIGHT else ThemeMode.DARK
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
    }

    // 仅用于向后兼容，不建议在 Composable 中直接使用
    val isAppDarkTheme: Boolean
        get() = themeMode == ThemeMode.DARK
}