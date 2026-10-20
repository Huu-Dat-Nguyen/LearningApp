package com.learningapp.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class, WordTranslationEntity::class, UserWordEntity::class, ReviewLogEntity::class,
        LessonEntity::class, LessonProgressEntity::class, DailyActivityEntity::class,
    ],
    version = 2,
    exportSchema = true,
    // v2 only adds the lesson and activity tables, so Room can migrate automatically.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun lessonDao(): LessonDao
    abstract fun activityDao(): ActivityDao
}
