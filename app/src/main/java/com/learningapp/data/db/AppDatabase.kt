package com.learningapp.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class, WordTranslationEntity::class, UserWordEntity::class, ReviewLogEntity::class,
        LessonEntity::class, LessonProgressEntity::class, DailyActivityEntity::class,
        SpeakPracticeEntity::class,
    ],
    version = 3,
    exportSchema = true,
    // Each version only adds tables, so Room can migrate automatically.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun lessonDao(): LessonDao
    abstract fun activityDao(): ActivityDao
    abstract fun speakDao(): SpeakDao
}
