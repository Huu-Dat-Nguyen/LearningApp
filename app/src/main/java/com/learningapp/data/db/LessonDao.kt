package com.learningapp.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Upsert
    suspend fun upsertLessons(lessons: List<LessonEntity>)

    @Query("SELECT COUNT(*) FROM lessons WHERE lang = :lang")
    suspend fun lessonCount(lang: String): Int

    @Query(
        """
        SELECT l.id, l.unitId, l.unitTitleJson, l.level, l.titleJson, l.sortOrder,
               (p.lessonId IS NOT NULL) AS completed, p.bestAccuracy, COALESCE(p.attempts, 0) AS attempts
        FROM lessons l LEFT JOIN lesson_progress p ON p.lessonId = l.id
        WHERE l.lang = :lang
        ORDER BY l.sortOrder
        """
    )
    fun lessons(lang: String): Flow<List<LessonRow>>

    @Query("SELECT * FROM lessons WHERE lang = :lang ORDER BY sortOrder")
    suspend fun allLessons(lang: String): List<LessonEntity>

    /** True when the user has any lesson or flashcard progress (an install from before onboarding existed). */
    @Query("SELECT EXISTS(SELECT 1 FROM lesson_progress) OR EXISTS(SELECT 1 FROM user_words)")
    suspend fun hasAnyProgress(): Boolean

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun lesson(id: String): LessonEntity?

    @Query("SELECT * FROM lessons WHERE unitId = :unitId ORDER BY sortOrder")
    suspend fun lessonsInUnit(unitId: String): List<LessonEntity>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :id")
    suspend fun progress(id: String): LessonProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: LessonProgressEntity)
}

@Dao
interface ActivityDao {

    @Query("SELECT * FROM daily_activity WHERE epochDay = :day AND lang = :lang")
    suspend fun day(day: Long, lang: String): DailyActivityEntity?

    @Upsert
    suspend fun upsert(activity: DailyActivityEntity)

    /** Adds to today's totals, creating the row if needed. */
    @Transaction
    suspend fun add(day: Long, lang: String, xp: Int = 0, seconds: Long = 0, answered: Int = 0, correct: Int = 0) {
        val cur = day(day, lang) ?: DailyActivityEntity(day, lang)
        upsert(
            cur.copy(
                xp = cur.xp + xp,
                activeSeconds = cur.activeSeconds + seconds,
                answered = cur.answered + answered,
                correct = cur.correct + correct,
            )
        )
    }

    @Query("SELECT * FROM daily_activity WHERE lang = :lang AND epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun range(lang: String, from: Long, to: Long): Flow<List<DailyActivityEntity>>

    /** Days with any XP, newest first, for the streak. */
    @Query("SELECT epochDay FROM daily_activity WHERE lang = :lang AND xp > 0 ORDER BY epochDay DESC")
    fun activeDays(lang: String): Flow<List<Long>>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM daily_activity WHERE lang = :lang")
    fun totalXp(lang: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_words WHERE targetLang = :lang")
    fun wordsStarted(lang: String): Flow<Int>
}
