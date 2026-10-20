package com.learningapp.di

import com.learningapp.data.repo.ReviewRepository
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
}
