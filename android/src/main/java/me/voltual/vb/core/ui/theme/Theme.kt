//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import me.voltual.pyrolysis.Res
import me.voltual.pyrolysis.unifont

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

// 扩展属性适配 calculateIsDark
val MaterialTheme.messageLikeBg: Color
    @Composable get() {
        val isDark = ThemeManager.calculateIsDark(isSystemInDarkTheme())
        val customColors = ThemeManager.customColorSet
        return if (isDark) customColors?.darkSet?.messageLikeBg ?: message_like_bg_dark
        else customColors?.lightSet?.messageLikeBg ?: message_like_bg
    }

val MaterialTheme.messageCommentBg: Color
    @Composable get() {
        val isDark = ThemeManager.calculateIsDark(isSystemInDarkTheme())
        val customColors = ThemeManager.customColorSet
        return if (isDark) customColors?.darkSet?.messageCommentBg ?: message_comment_bg_dark
        else customColors?.lightSet?.messageCommentBg ?: message_comment_bg
    }

val MaterialTheme.messageDefaultBg: Color
    @Composable get() {
        val isDark = ThemeManager.calculateIsDark(isSystemInDarkTheme())
        val customColors = ThemeManager.customColorSet
        return if (isDark) customColors?.darkSet?.messageDefaultBg ?: message_default_bg_dark
        else customColors?.lightSet?.messageDefaultBg ?: message_default_bg
    }

val MaterialTheme.billingIncome: Color
    @Composable get() {
        val isDark = ThemeManager.calculateIsDark(isSystemInDarkTheme())
        val customColors = ThemeManager.customColorSet
        return if (isDark) customColors?.darkSet?.billingIncome ?: billing_income_dark
        else customColors?.lightSet?.billingIncome ?: billing_income
    }

val MaterialTheme.billingExpense: Color
    @Composable get() {
        val isDark = ThemeManager.calculateIsDark(isSystemInDarkTheme())
        val customColors = ThemeManager.customColorSet
        return if (isDark) customColors?.darkSet?.billingExpense ?: billing_expense_dark
        else customColors?.lightSet?.billingExpense ?: billing_expense
    }

@Composable
fun getAppTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
        displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
    )
}

@Composable
fun BBQTheme(
    content: @Composable () -> Unit
) {   
    val isDark = ThemeManager.calculateIsDark(isSystemInDarkTheme())
    val customColors = ThemeManager.customColorSet
    
    val colorScheme = if (isDark) {
        customColors?.darkSet?.toDarkColorScheme() ?: darkScheme
    } else {
        customColors?.lightSet?.toLightColorScheme() ?: lightScheme
    }

    val unifontFamily = FontFamily(Font(resource = Res.font.unifont))
        
    MaterialTheme(
        colorScheme = colorScheme,
        typography = getAppTypography(unifontFamily),
        content = content
    )
}

private fun ColorSet.toLightColorScheme() = lightColorScheme(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    surface = surface, onSurface = onSurface, surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline, error = error, onError = onError, background = background, onBackground = onBackground
)

private fun ColorSet.toDarkColorScheme() = darkColorScheme(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    surface = surface, onSurface = onSurface, surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline, error = error, onError = onError, background = background, onBackground = onBackground
)