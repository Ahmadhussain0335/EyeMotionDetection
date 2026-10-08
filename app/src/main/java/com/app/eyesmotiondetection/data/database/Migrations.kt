package com.app.eyesmotiondetection.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 had no uniqueness on gesture_mappings.gestureName, so re-seeding could create duplicates.
 * Keep the newest row per gesture, then enforce one mapping per gesture.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM gesture_mappings WHERE id NOT IN " +
                "(SELECT MAX(id) FROM gesture_mappings GROUP BY gestureName)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_gesture_mappings_gestureName " +
                "ON gesture_mappings (gestureName)"
        )
    }
}
