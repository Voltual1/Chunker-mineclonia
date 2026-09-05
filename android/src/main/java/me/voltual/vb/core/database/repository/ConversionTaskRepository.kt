package me.voltual.vb.core.database.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import me.voltual.vb.core.database.dao.ConversionTaskDao
import me.voltual.vb.core.database.entity.ConversionTaskEntity
import me.voltual.vb.data.model.ConversionManifest

class ConversionTaskRepository(private val dao: ConversionTaskDao) {

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    suspend fun getManifest(worldId: String): ConversionManifest? {
        val entity = dao.getTask(worldId) ?: return null
        return try {
            jsonFormat.decodeFromString<ConversionManifest>(entity.stateJson)
        } catch(e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveManifest(manifest: ConversionManifest) {
        try {
            val json = jsonFormat.encodeToString(manifest)
            dao.insertTask(ConversionTaskEntity(manifest.worldId, json))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteManifest(worldId: String) {
        dao.deleteTask(worldId)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun getActiveManifest(): ConversionManifest? {
        return dao.getAllTasks().mapNotNull { 
            try { jsonFormat.decodeFromString<ConversionManifest>(it.stateJson) } catch(e: Exception) { null } 
        }.find { it.isActive }
    }

    suspend fun clearActiveManifests() {
        val activeTasks = dao.getAllTasks().mapNotNull { 
            try { jsonFormat.decodeFromString<ConversionManifest>(it.stateJson) } catch(e: Exception) { null } 
        }.filter { it.isActive }

        activeTasks.forEach {
            saveManifest(it.copy(isActive = false))
        }
    }

    /** 
     * 读取存储在数据库底座中的全量转换断点 
     */
    suspend fun getAllManifests(): List<ConversionManifest> {
        return dao.getAllTasks().mapNotNull { entity ->
            try {
                jsonFormat.decodeFromString<ConversionManifest>(entity.stateJson)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}