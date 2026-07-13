package me.voltual.vb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChunkerSettingsDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        val KEY_THREAD_COUNT = intPreferencesKey("thread_count")
        val KEY_PROCESS_MAPS = booleanPreferencesKey("process_maps")
        val KEY_ENERGY_SAVING_MODE = booleanPreferencesKey("energy_saving_mode")
        val KEY_ENABLE_SLICING = booleanPreferencesKey("enable_slicing")
        
        // 获取系统推荐的最大核心数
        val maxAvailableCores: Int
            get() = Math.max(1, Runtime.getRuntime().availableProcessors())
    }

    // 线程并发数流动，默认保守设置为1
    val threadCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_THREAD_COUNT] ?: 1
    }

    // 地图转换开关流动，默认关闭
    val processMaps: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PROCESS_MAPS] ?: false
    }

    // 地图预览节能模式（点哪里亮哪里)
    val energySavingMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ENERGY_SAVING_MODE] ?: true
    }

    // 内存安全切片模式（大世界必需），默认关闭
    val enableSlicing: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ENABLE_SLICING] ?: false
    }

    suspend fun setThreadCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_THREAD_COUNT] = count.coerceIn(1, maxAvailableCores)
        }
    }

    suspend fun setProcessMaps(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PROCESS_MAPS] = enabled
        }
    }

    suspend fun setEnergySavingMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ENERGY_SAVING_MODE] = enabled
        }
    }

    suspend fun setEnableSlicing(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ENABLE_SLICING] = enabled
        }
    }
}