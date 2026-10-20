package com.learningapp.di

import com.learningapp.data.repo.LessonRepository
import com.learningapp.data.repo.LessonStore
import com.learningapp.data.repo.ReviewRepository
import com.learningapp.data.repo.SpeakRepository
import com.learningapp.data.repo.SpeakStore
import com.learningapp.data.repo.WordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds
    abstract fun reviewRepository(repo: WordRepository): ReviewRepository

    @Binds
    abstract fun lessonStore(repo: LessonRepository): LessonStore

    @Binds
    abstract fun speakStore(repo: SpeakRepository): SpeakStore
}
