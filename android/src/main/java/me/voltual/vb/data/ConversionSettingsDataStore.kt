// Copyright (C) 2025 Voltual
package me.voltual.vb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversionSettingsDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        val KEY_THREAD_COUNT = intPreferencesKey("thread_count")
        val KEY_PROCESS_MAPS = booleanPreferencesKey("process_maps")
        
        // 获取系统推荐的最大核心数
        val maxAvailableCores: Int
            get() = Math.max(1, Runtime.getRuntime().availableProcessors())
    }

    // 线程并发数流动，默认保守设置为 1（最防OOM），允许用户滑块调节
    val threadCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_THREAD_COUNT] ?: 1
    }

    // 地图转换开关流动，默认关闭（地图最容易导致 JVM 分配内存失败）
    val processMaps: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PROCESS_MAPS] ?: false
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
}