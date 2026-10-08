package com.app.eyesmotiondetection.data.repository

import com.app.eyesmotiondetection.data.database.dao.GestureMappingDao
import com.app.eyesmotiondetection.data.database.entity.GestureMappingEntity
import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.EyeGesture
import com.app.eyesmotiondetection.domain.model.GestureMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureMappingRepository @Inject constructor(
    private val dao: GestureMappingDao
) {
    fun getAllMappings(): Flow<List<GestureMapping>> =
        dao.getAllMappings().map { list -> list.mapNotNull { it.toDomainOrNull() } }

    suspend fun getMappingForGesture(gesture: EyeGesture): GestureMapping? =
        dao.getMappingByGesture(gesture.name)?.toDomainOrNull()

    suspend fun saveMapping(mapping: GestureMapping) = dao.insertMapping(mapping.toEntity())

    suspend fun deleteMapping(mapping: GestureMapping) = dao.deleteMapping(mapping.toEntity())

    suspend fun deleteAll() = dao.deleteAllMappings()

    /** Seeds defaults for gestures that have no mapping yet; existing mappings are kept. */
    suspend fun insertDefaultMappings() =
        dao.insertMappingsIfAbsent(defaultMappings.map { it.toEntity() })

    /** Atomically replaces every mapping with the defaults. */
    suspend fun resetToDefaults() = dao.replaceAll(defaultMappings.map { it.toEntity() })

    private fun GestureMappingEntity.toDomainOrNull(): GestureMapping? = runCatching {
        GestureMapping(
            id = id,
            gesture = EyeGesture.valueOf(gestureName),
            action = AppAction.valueOf(actionName),
            isEnabled = isEnabled,
            sensitivity = sensitivity,
            cooldownMs = cooldownMs
        )
    }.getOrNull()

    private fun GestureMapping.toEntity() = GestureMappingEntity(
        id = id,
        gestureName = gesture.name,
        actionName = action.name,
        isEnabled = isEnabled,
        sensitivity = sensitivity,
        cooldownMs = cooldownMs
    )

    companion object {
        val defaultMappings = listOf(
            GestureMapping(gesture = EyeGesture.LOOK_UP, action = AppAction.SCROLL_UP),
            GestureMapping(gesture = EyeGesture.LOOK_DOWN, action = AppAction.SCROLL_DOWN),
            GestureMapping(gesture = EyeGesture.WINK_LEFT, action = AppAction.BACK),
            GestureMapping(gesture = EyeGesture.WINK_RIGHT, action = AppAction.TAP),
            GestureMapping(gesture = EyeGesture.FAST_BLINK_MULTI, action = AppAction.LOCK_SCREEN),
            GestureMapping(gesture = EyeGesture.DOUBLE_WINK, action = AppAction.OPEN_NOTIFICATIONS),
            GestureMapping(gesture = EyeGesture.LOOK_LEFT, action = AppAction.SWIPE_LEFT),
            GestureMapping(gesture = EyeGesture.LOOK_RIGHT, action = AppAction.SWIPE_RIGHT),
            GestureMapping(gesture = EyeGesture.EYES_WIDE_OPEN, action = AppAction.HOME),
            GestureMapping(gesture = EyeGesture.BLINK_BOTH, action = AppAction.TAP)
        )
    }
}
