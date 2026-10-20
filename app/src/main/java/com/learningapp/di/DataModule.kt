package com.learningapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.learningapp.data.db.AppDatabase
import com.learningapp.data.db.ActivityDao
import com.learningapp.data.db.LessonDao
import com.learningapp.data.db.SpeakDao
import com.learningapp.data.db.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "learningapp.db").build()

    @Provides
    fun wordDao(db: AppDatabase): WordDao = db.wordDao()

    @Provides
    fun lessonDao(db: AppDatabase): LessonDao = db.lessonDao()

    @Provides
    fun activityDao(db: AppDatabase): ActivityDao = db.activityDao()

    @Provides
    fun speakDao(db: AppDatabase): SpeakDao = db.speakDao()

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()
}
