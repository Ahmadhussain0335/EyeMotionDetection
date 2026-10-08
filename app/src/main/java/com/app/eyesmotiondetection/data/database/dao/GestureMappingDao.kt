package com.app.eyesmotiondetection.data.database.dao

import androidx.room.*
import com.app.eyesmotiondetection.data.database.entity.GestureMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureMappingDao {
    @Query("SELECT * FROM gesture_mappings")
    fun getAllMappings(): Flow<List<GestureMappingEntity>>

    @Query("SELECT * FROM gesture_mappings WHERE gestureName = :gestureName LIMIT 1")
    suspend fun getMappingByGesture(gestureName: String): GestureMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: GestureMappingEntity): Long

    @Update
    suspend fun updateMapping(mapping: GestureMappingEntity)

    @Delete
    suspend fun deleteMapping(mapping: GestureMappingEntity)

    @Query("DELETE FROM gesture_mappings")
    suspend fun deleteAllMappings()

    /** Keeps existing rows; used to seed defaults without overwriting user choices. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMappingsIfAbsent(mappings: List<GestureMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<GestureMappingEntity>)

    @Transaction
    suspend fun replaceAll(mappings: List<GestureMappingEntity>) {
        deleteAllMappings()
        insertMappings(mappings)
    }
}
