package com.app.eyesmotiondetection.di

import com.app.eyesmotiondetection.detection.EyeGestureDetector
import com.app.eyesmotiondetection.detection.FaceAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetectionModule {

    @Provides @Singleton
    fun provideFaceAnalyzer(detector: EyeGestureDetector): FaceAnalyzer =
        FaceAnalyzer(detector::processFaceData)
}
