// [file name]: me.voltual.vb.data.ConversionProgressDataStore.kt
package me.voltual.vb.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Stores the sliced conversion progress for worlds to allow resume functionality.
 * This object is fully independent of dependency injection frameworks (Koin) to allow multi-process safety.
 */
object ConversionProgressDataStore {
    private var dataStoreInstance: DataStore<Preferences>? = null

    @Synchronized
    private fun getDataStore(context: Context): DataStore<Preferences> {
        if (dataStoreInstance == null) {
            val appContext = context.applicationContext
            dataStoreInstance = PreferenceDataStoreFactory.create(
                produceFile = {
                    File(appContext.filesDir, "datastore/conversion_progress_prefs.preferences_pb")
                }
            )
        }
        return dataStoreInstance!!
    }

    suspend fun getProgress(context: Context, worldHash: String): Int {
        val key = intPreferencesKey("progress_$worldHash")
        return getDataStore(context).data.map { preferences ->
            preferences[key] ?: 0
        }.first()
    }

    suspend fun saveProgress(context: Context, worldHash: String, index: Int) {
        val key = intPreferencesKey("progress_$worldHash")
        getDataStore(context).edit { preferences ->
            preferences[key] = index
        }
    }

    suspend fun clearProgress(context: Context, worldHash: String) {
        val key = intPreferencesKey("progress_$worldHash")
        getDataStore(context).edit { preferences ->
            preferences.remove(key)
        }
    }
}