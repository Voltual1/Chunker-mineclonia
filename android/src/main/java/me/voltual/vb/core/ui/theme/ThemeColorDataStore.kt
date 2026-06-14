//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CustomColorSet(
    val lightSet: ColorSet = ColorSet.defaultLight(),
    val darkSet: ColorSet = ColorSet.defaultDark()
)

data class ColorSet(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    val onError: Color,
    val background: Color,
    val onBackground: Color,
    val messageLikeBg: Color,
    val messageCommentBg: Color,
    val messageDefaultBg: Color,
    val billingIncome: Color,
    val billingExpense: Color
) {
    fun toList(): List<Pair<String, Color>> = listOf(
        "primary" to primary, "onPrimary" to onPrimary, "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer, "secondary" to secondary, "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer, "onSecondaryContainer" to onSecondaryContainer,
        "surface" to surface, "onSurface" to onSurface, "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant, "outline" to outline, "error" to error,
        "onError" to onError, "background" to background, "onBackground" to onBackground,
        "messageLikeBg" to messageLikeBg, "messageCommentBg" to messageCommentBg,
        "messageDefaultBg" to messageDefaultBg, "billingIncome" to billingIncome, "billingExpense" to billingExpense
    )

    fun copyWith(name: String, newColor: Color): ColorSet {
        return when (name) {
            "primary" -> copy(primary = newColor)
            "onPrimary" -> copy(onPrimary = newColor)
            "primaryContainer" -> copy(primaryContainer = newColor)
            "onPrimaryContainer" -> copy(onPrimaryContainer = newColor)
            "secondary" -> copy(secondary = newColor)
            "onSecondary" -> copy(onSecondary = newColor)
            "secondaryContainer" -> copy(secondaryContainer = newColor)
            "onSecondaryContainer" -> copy(onSecondaryContainer = newColor)
            "surface" -> copy(surface = newColor)
            "onSurface" -> copy(onSurface = newColor)
            "surfaceVariant" -> copy(surfaceVariant = newColor)
            "onSurfaceVariant" -> copy(onSurfaceVariant = newColor)
            "outline" -> copy(outline = newColor)
            "error" -> copy(error = newColor)
            "onError" -> copy(onError = newColor)
            "background" -> copy(background = newColor)
            "onBackground" -> copy(onBackground = newColor)
            "messageLikeBg" -> copy(messageLikeBg = newColor)
            "messageCommentBg" -> copy(messageCommentBg = newColor)
            "messageDefaultBg" -> copy(messageDefaultBg = newColor)
            "billingIncome" -> copy(billingIncome = newColor)
            "billingExpense" -> copy(billingExpense = newColor)
            else -> this
        }
    }

    companion object {
        fun defaultLight() = ColorSet(
            primary = primaryLight,
            onPrimary = onPrimaryLight,
            primaryContainer = primaryContainerLight,
            onPrimaryContainer = onPrimaryContainerLight,
            secondary = secondaryLight,
            onSecondary = onSecondaryLight,
            secondaryContainer = secondaryContainerLight,
            onSecondaryContainer = onSecondaryContainerLight,
            surface = surfaceLight,
            onSurface = onSurfaceLight,
            surfaceVariant = surfaceVariantLight,
            onSurfaceVariant = onSurfaceVariantLight,
            outline = outlineLight,
            error = errorLight,
            onError = onErrorLight,
            background = backgroundLight,
            onBackground = onBackgroundLight,
            messageLikeBg = message_like_bg,
            messageCommentBg = message_comment_bg,
            messageDefaultBg = message_default_bg,
            billingIncome = billing_income,
            billingExpense = billing_expense
        )

        fun defaultDark() = ColorSet(
            primary = primaryDark,
            onPrimary = onPrimaryDark,
            primaryContainer = primaryContainerDark,
            onPrimaryContainer = onPrimaryContainerDark,
            secondary = secondaryDark,
            onSecondary = onSecondaryDark,
            secondaryContainer = secondaryContainerDark,
            onSecondaryContainer = onSecondaryContainerDark,
            surface = surfaceDark,
            onSurface = onSurfaceDark,
            surfaceVariant = surfaceVariantDark,
            onSurfaceVariant = onSurfaceVariantDark,
            outline = outlineDark,
            error = errorDark,
            onError = onErrorDark,
            background = backgroundDark,
            onBackground = onBackgroundDark,
            messageLikeBg = message_like_bg_dark,
            messageCommentBg = message_comment_bg_dark,
            messageDefaultBg = message_default_bg_dark,
            billingIncome = billing_income_dark,
            billingExpense = billing_expense_dark
        )
    }
}

class ThemeColorDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        val DEFAULT_COLORS = CustomColorSet(
            lightSet = ColorSet.defaultLight(),
            darkSet = ColorSet.defaultDark()
        )

        private val LIGHT_COLOR_KEYS = listOf(
            "light_primary", "light_onPrimary", "light_primaryContainer", "light_onPrimaryContainer", "light_secondary",
            "light_onSecondary", "light_secondaryContainer", "light_onSecondaryContainer", "light_surface", "light_onSurface",
            "light_surfaceVariant", "light_onSurfaceVariant", "light_outline", "light_error", "light_onError", "light_background",
            "light_onBackground", "light_messageLikeBg", "light_messageCommentBg", "light_messageDefaultBg", "light_billingIncome",
            "light_billingExpense"
        )

        private val DARK_COLOR_KEYS = listOf(
            "dark_primary", "dark_onPrimary", "dark_primaryContainer", "dark_onPrimaryContainer", "dark_secondary",
            "dark_onSecondary", "dark_secondaryContainer", "dark_onSecondaryContainer", "dark_surface", "dark_onSurface",
            "dark_surfaceVariant", "dark_onSurfaceVariant", "dark_outline", "dark_error", "dark_onError", "dark_background",
            "dark_onBackground", "dark_messageLikeBg", "dark_messageCommentBg", "dark_messageDefaultBg", "dark_billingIncome",
            "dark_billingExpense"
        )

        private val DRAWER_HEADER_LIGHT_BG_URI_KEY = stringPreferencesKey("drawer_header_light_bg_uri")
        private val DRAWER_HEADER_DARK_BG_URI_KEY = stringPreferencesKey("drawer_header_dark_bg_uri")
        private val GLOBAL_BACKGROUND_URI_KEY = stringPreferencesKey("global_background_uri")
        private val ROUND_SCREEN_ENABLED_KEY = booleanPreferencesKey("round_screen_enabled")
        private val ROUND_SCREEN_LEFT_PADDING_KEY = floatPreferencesKey("round_screen_left_padding")
        private val ROUND_SCREEN_TOP_PADDING_KEY = floatPreferencesKey("round_screen_top_padding")
        private val ROUND_SCREEN_RIGHT_PADDING_KEY = floatPreferencesKey("round_screen_right_padding")
        private val ROUND_SCREEN_BOTTOM_PADDING_KEY = floatPreferencesKey("round_screen_bottom_padding")
    }

    suspend fun saveGlobalBackgroundUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) preferences[GLOBAL_BACKGROUND_URI_KEY] = uri
            else preferences.remove(GLOBAL_BACKGROUND_URI_KEY)
        }
    }

    val globalBackgroundUriFlow: Flow<String?> = dataStore.data.map { it[GLOBAL_BACKGROUND_URI_KEY] }

    suspend fun saveColors(colors: CustomColorSet) {
        dataStore.edit { preferences ->
            colors.lightSet.toList().forEachIndexed { index, (_, color) ->
                preferences[intPreferencesKey(LIGHT_COLOR_KEYS[index])] = color.toArgb()
            }
            colors.darkSet.toList().forEachIndexed { index, (_, color) ->
                preferences[intPreferencesKey(DARK_COLOR_KEYS[index])] = color.toArgb()
            }
        }
    }

    val colorsFlow: Flow<CustomColorSet> = dataStore.data.map { preferences ->
        val lightSet = loadColorSetFromPrefs(preferences, LIGHT_COLOR_KEYS, DEFAULT_COLORS.lightSet)
        val darkSet = loadColorSetFromPrefs(preferences, DARK_COLOR_KEYS, DEFAULT_COLORS.darkSet)
        CustomColorSet(lightSet, darkSet)
    }

    private fun loadColorSetFromPrefs(preferences: Preferences, keys: List<String>, defaultSet: ColorSet): ColorSet {
        return try {
            if (keys.any { !preferences.contains(intPreferencesKey(it)) }) return defaultSet
            ColorSet(
                primary = Color(preferences[intPreferencesKey(keys[0])]!!),
                onPrimary = Color(preferences[intPreferencesKey(keys[1])]!!),
                primaryContainer = Color(preferences[intPreferencesKey(keys[2])]!!),
                onPrimaryContainer = Color(preferences[intPreferencesKey(keys[3])]!!),
                secondary = Color(preferences[intPreferencesKey(keys[4])]!!),
                onSecondary = Color(preferences[intPreferencesKey(keys[5])]!!),
                secondaryContainer = Color(preferences[intPreferencesKey(keys[6])]!!),
                onSecondaryContainer = Color(preferences[intPreferencesKey(keys[7])]!!),
                surface = Color(preferences[intPreferencesKey(keys[8])]!!),
                onSurface = Color(preferences[intPreferencesKey(keys[9])]!!),
                surfaceVariant = Color(preferences[intPreferencesKey(keys[10])]!!),
                onSurfaceVariant = Color(preferences[intPreferencesKey(keys[11])]!!),
                outline = Color(preferences[intPreferencesKey(keys[12])]!!),
                error = Color(preferences[intPreferencesKey(keys[13])]!!),
                onError = Color(preferences[intPreferencesKey(keys[14])]!!),
                background = Color(preferences[intPreferencesKey(keys[15])]!!),
                onBackground = Color(preferences[intPreferencesKey(keys[16])]!!),
                messageLikeBg = Color(preferences[intPreferencesKey(keys[17])]!!),
                messageCommentBg = Color(preferences[intPreferencesKey(keys[18])]!!),
                messageDefaultBg = Color(preferences[intPreferencesKey(keys[19])]!!),
                billingIncome = Color(preferences[intPreferencesKey(keys[20])]!!),
                billingExpense = Color(preferences[intPreferencesKey(keys[21])]!!)
            )
        } catch (e: Exception) {
            defaultSet
        }
    }

    suspend fun saveDrawerHeaderLightBackgroundUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) preferences[DRAWER_HEADER_LIGHT_BG_URI_KEY] = uri
            else preferences.remove(DRAWER_HEADER_LIGHT_BG_URI_KEY)
        }
    }

    val drawerHeaderLightBackgroundUriFlow: Flow<String?> = dataStore.data.map { it[DRAWER_HEADER_LIGHT_BG_URI_KEY] }

    suspend fun saveDrawerHeaderDarkBackgroundUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) preferences[DRAWER_HEADER_DARK_BG_URI_KEY] = uri
            else preferences.remove(DRAWER_HEADER_DARK_BG_URI_KEY)
        }
    }

    val drawerHeaderDarkBackgroundUriFlow: Flow<String?> = dataStore.data.map { it[DRAWER_HEADER_DARK_BG_URI_KEY] }

    suspend fun saveRoundScreenPaddings(enabled: Boolean, left: Float, top: Float, right: Float, bottom: Float) {
        dataStore.edit { prefs ->
            prefs[ROUND_SCREEN_ENABLED_KEY] = enabled
            prefs[ROUND_SCREEN_LEFT_PADDING_KEY] = left
            prefs[ROUND_SCREEN_TOP_PADDING_KEY] = top
            prefs[ROUND_SCREEN_RIGHT_PADDING_KEY] = right
            prefs[ROUND_SCREEN_BOTTOM_PADDING_KEY] = bottom
        }
    }

    data class RoundScreenPaddings(val enabled: Boolean, val left: Float, val top: Float, val right: Float, val bottom: Float)

    val roundScreenPaddingFlow: Flow<RoundScreenPaddings> = dataStore.data.map { prefs ->
        RoundScreenPaddings(
            enabled = prefs[ROUND_SCREEN_ENABLED_KEY] ?: false,
            left = prefs[ROUND_SCREEN_LEFT_PADDING_KEY] ?: 0f,
            top = prefs[ROUND_SCREEN_TOP_PADDING_KEY] ?: 0f,
            right = prefs[ROUND_SCREEN_RIGHT_PADDING_KEY] ?: 0f,
            bottom = prefs[ROUND_SCREEN_BOTTOM_PADDING_KEY] ?: 0f
        )
    }
}