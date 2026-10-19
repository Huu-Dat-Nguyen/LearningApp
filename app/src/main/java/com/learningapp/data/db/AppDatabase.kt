package com.learningapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WordEntity::class, WordTranslationEntity::class, UserWordEntity::class, ReviewLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
