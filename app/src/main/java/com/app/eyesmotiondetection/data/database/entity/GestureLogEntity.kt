package com.app.eyesmotiondetection.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_logs")
data class GestureLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gestureName: String,
    val actionName: String,
    val timestamp: Long,
    val confidence: Float
)
