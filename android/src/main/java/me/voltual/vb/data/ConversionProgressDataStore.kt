// [file name]: me.voltual.vb.data.ConversionProgressDataStore.kt
package me.voltual.vb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Stores the sliced conversion progress for worlds to allow resume functionality.
 * The world identity is determined by hashing its icon.png/world_icon.jpeg.
 */
class ConversionProgressDataStore(private val dataStore: DataStore<Preferences>) {
    
    suspend fun getProgress(worldHash: String): Int {
        val key = intPreferencesKey("progress_$worldHash")
        return dataStore.data.map { preferences ->
            preferences[key] ?: 0
        }.first()
    }

    suspend fun saveProgress(worldHash: String, index: Int) {
        val key = intPreferencesKey("progress_$worldHash")
        dataStore.edit { preferences ->
            preferences[key] = index
        }
    }

    suspend fun clearProgress(worldHash: String) {
        val key = intPreferencesKey("progress_$worldHash")
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}