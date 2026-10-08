package com.app.eyesmotiondetection.data.repository

import com.app.eyesmotiondetection.data.database.dao.GestureLogDao
import com.app.eyesmotiondetection.data.database.entity.GestureLogEntity
import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.EyeGesture
import com.app.eyesmotiondetection.domain.model.GestureLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureLogRepository @Inject constructor(
    private val dao: GestureLogDao
) {
    fun getRecentLogs(limit: Int = 100): Flow<List<GestureLog>> =
        dao.getRecentLogs(limit).map { list -> list.mapNotNull { it.toDomainOrNull() } }

    suspend fun logGesture(log: GestureLog) = dao.insertLog(log.toEntity())

    suspend fun clearLogs() = dao.clearAllLogs()

    /** Live count of logs since local midnight; the boundary is computed on each collection. */
    fun observeTodayCount(): Flow<Int> = flow {
        emitAll(dao.observeCountSince(startOfDayMillis(Instant.now(), ZoneId.systemDefault())))
    }

    private fun GestureLogEntity.toDomainOrNull(): GestureLog? = runCatching {
        GestureLog(
            id = id,
            gesture = EyeGesture.valueOf(gestureName),
            action = AppAction.valueOf(actionName),
            timestamp = timestamp,
            confidence = confidence
        )
    }.getOrNull()

    private fun GestureLog.toEntity() = GestureLogEntity(
        id = id,
        gestureName = gesture.name,
        actionName = action.name,
        timestamp = timestamp,
        confidence = confidence
    )
}

internal fun startOfDayMillis(now: Instant, zone: ZoneId): Long =
    now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
