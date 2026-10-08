package com.app.eyesmotiondetection.di

import android.content.Context
import androidx.room.Room
import com.app.eyesmotiondetection.data.database.AppDatabase
import com.app.eyesmotiondetection.data.database.MIGRATION_1_2
import com.app.eyesmotiondetection.data.database.dao.GestureLogDao
import com.app.eyesmotiondetection.data.database.dao.GestureMappingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "eye_tracking_db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideGestureMappingDao(db: AppDatabase): GestureMappingDao = db.gestureMappingDao()

    @Provides
    fun provideGestureLogDao(db: AppDatabase): GestureLogDao = db.gestureLogDao()
}
