package com.app.eyesmotiondetection.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gesture_mappings",
    indices = [Index(value = ["gestureName"], unique = true)]
)
data class GestureMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gestureName: String,
    val actionName: String,
    val isEnabled: Boolean = true,
    val sensitivity: Float = 0.5f,
    val cooldownMs: Long = 1000L
)
