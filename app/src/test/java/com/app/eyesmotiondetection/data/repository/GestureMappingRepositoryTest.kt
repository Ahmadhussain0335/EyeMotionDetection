package com.app.eyesmotiondetection.data.repository

import com.app.eyesmotiondetection.data.database.dao.GestureMappingDao
import com.app.eyesmotiondetection.data.database.entity.GestureMappingEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureMappingRepositoryTest {

    private val dao = mockk<GestureMappingDao>(relaxed = true)
    private val repository = GestureMappingRepository(dao)

    @Test
    fun `seeding defaults never overwrites existing mappings`() = runTest {
        repository.insertDefaultMappings()
        coVerify(exactly = 1) { dao.insertMappingsIfAbsent(any()) }
        coVerify(exactly = 0) { dao.insertMappings(any()) }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    @Test
    fun `reset replaces all mappings atomically with one row per gesture`() = runTest {
        val captured = slot<List<GestureMappingEntity>>()
        coEvery { dao.replaceAll(capture(captured)) } returns Unit

        repository.resetToDefaults()

        val names = captured.captured.map { it.gestureName }
        assertEquals(GestureMappingRepository.defaultMappings.size, names.size)
        assertEquals(names.size, names.toSet().size)
        assertEquals(true, captured.captured.all { it.id == 0L })
    }

    @Test
    fun `rows with unknown enum names are skipped`() = runTest {
        coEvery { dao.getMappingByGesture(any()) } returns
            GestureMappingEntity(id = 1, gestureName = "BLINK_BOTH", actionName = "REMOVED_ACTION")
        assertEquals(null, repository.getMappingForGesture(com.app.eyesmotiondetection.domain.model.EyeGesture.BLINK_BOTH))
    }
}
