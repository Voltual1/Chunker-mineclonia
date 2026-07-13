package me.voltual.vb.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "conversion_tasks")
data class ConversionTaskEntity(
    @PrimaryKey val worldId: String,
    val stateJson: String
)