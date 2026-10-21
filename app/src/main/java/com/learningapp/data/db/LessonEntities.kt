package com.learningapp.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A lesson from assets/content/<lang>/lessons.json. Text fields hold JSON maps keyed by main language. */
@Entity(tableName = "lessons", indices = [Index(value = ["lang", "sortOrder"])])
data class LessonEntity(
    @PrimaryKey val id: String,
    val lang: String,
    val unitId: String,
    val unitTitleJson: String,
    val topic: String,
    val level: String,
    val titleJson: String,
    /** Position across all units; lesson N unlocks when lesson N-1 is done. */
    val sortOrder: Int,
    val wordIdsJson: String,
    val exercisesJson: String,
)

/** The user's result for a lesson. No row = never finished. */
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val firstCompletedAtMs: Long,
    val lastCompletedAtMs: Long,
    /** 0..1, fraction of exercises right the first time. */
    val bestAccuracy: Double,
    val attempts: Int,
    val xpEarned: Int,
)

/** Totals for one day and one target language; lessons and flashcard reviews both add to it. */
@Entity(tableName = "daily_activity", primaryKeys = ["epochDay", "lang"])
data class DailyActivityEntity(
    val epochDay: Long,
    val lang: String,
    val xp: Int = 0,
    val activeSeconds: Long = 0,
    val answered: Int = 0,
    val correct: Int = 0,
)

/** Lesson row joined with its progress, for the Learn list. */
data class LessonRow(
    val id: String,
    val unitId: String,
    val unitTitleJson: String,
    val level: String,
    val titleJson: String,
    val sortOrder: Int,
    val completed: Boolean,
    val bestAccuracy: Double?,
    /** 0 with [completed] = tested out by the placement quiz. */
    val attempts: Int = 0,
)
