package com.app.eyesmotiondetection.data.database.dao

import androidx.room.*
import com.app.eyesmotiondetection.data.database.entity.GestureLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureLogDao {
    @Query("SELECT * FROM gesture_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<GestureLogEntity>>

    @Query("SELECT * FROM gesture_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<GestureLogEntity>>

    @Insert
    suspend fun insertLog(log: GestureLogEntity)

    @Query("DELETE FROM gesture_logs WHERE timestamp < :olderThan")
    suspend fun deleteOldLogs(olderThan: Long)

    @Query("DELETE FROM gesture_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM gesture_logs WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>
}
