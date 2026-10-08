package com.app.eyesmotiondetection.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.eyesmotiondetection.data.database.dao.GestureLogDao
import com.app.eyesmotiondetection.data.database.dao.GestureMappingDao
import com.app.eyesmotiondetection.data.database.entity.GestureLogEntity
import com.app.eyesmotiondetection.data.database.entity.GestureMappingEntity

@Database(
    entities = [GestureMappingEntity::class, GestureLogEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gestureMappingDao(): GestureMappingDao
    abstract fun gestureLogDao(): GestureLogDao
}
