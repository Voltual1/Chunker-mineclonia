package me.voltual.vb.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import me.voltual.vb.core.database.entity.ConversionTaskEntity

@Dao
interface ConversionTaskDao {
    @Query("SELECT * FROM conversion_tasks WHERE worldId = :worldId")
    suspend fun getTask(worldId: String): ConversionTaskEntity?

    @Query("SELECT * FROM conversion_tasks")
    suspend fun getAllTasks(): List<ConversionTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ConversionTaskEntity)

    @Query("DELETE FROM conversion_tasks WHERE worldId = :worldId")
    suspend fun deleteTask(worldId: String)

    @Query("DELETE FROM conversion_tasks")
    suspend fun clearAll()
}