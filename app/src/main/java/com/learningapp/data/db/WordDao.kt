package com.learningapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Upsert
    suspend fun upsertWords(words: List<WordEntity>)

    @Upsert
    suspend fun upsertTranslations(translations: List<WordTranslationEntity>)

    @Transaction
    suspend fun upsertContent(words: List<WordEntity>, translations: List<WordTranslationEntity>) {
        upsertWords(words)
        upsertTranslations(translations)
    }

    @Query("SELECT COUNT(*) FROM words WHERE lang = :lang")
    suspend fun wordCount(lang: String): Int

    /** Cards due on or before [today], oldest first. */
    @Query(
        """
        SELECT w.id AS wordId, w.text, w.example, w.reading, t.text AS translation, t.example AS translationExample,
               u.easeFactor, u.intervalDays, u.repetitions, u.lapses, u.dueEpochDay
        FROM user_words u
        JOIN words w ON w.id = u.wordId
        JOIN word_translations t ON t.wordId = w.id AND t.lang = :mainLang
        WHERE u.targetLang = :targetLang AND u.dueEpochDay <= :today
        ORDER BY u.dueEpochDay, w.sortOrder
        LIMIT :limit
        """
    )
    suspend fun dueCards(targetLang: String, mainLang: String, today: Long, limit: Int): List<CardRow>

    /** Words never reviewed, in content order. */
    @Query(
        """
        SELECT w.id AS wordId, w.text, w.example, w.reading, t.text AS translation, t.example AS translationExample,
               NULL AS easeFactor, NULL AS intervalDays, NULL AS repetitions, NULL AS lapses, NULL AS dueEpochDay
        FROM words w
        JOIN word_translations t ON t.wordId = w.id AND t.lang = :mainLang
        LEFT JOIN user_words u ON u.wordId = w.id
        WHERE w.lang = :targetLang AND u.wordId IS NULL
        ORDER BY w.sortOrder
        LIMIT :limit
        """
    )
    suspend fun newCards(targetLang: String, mainLang: String, limit: Int): List<CardRow>

    @Query("SELECT COUNT(*) FROM user_words WHERE targetLang = :targetLang AND dueEpochDay <= :today")
    fun dueCount(targetLang: String, today: Long): Flow<Int>

    /** Number of new cards started (first reviewed) on a given day, for the daily new-card limit. */
    @Query(
        """
        SELECT COUNT(*) FROM review_log r
        JOIN words w ON w.id = r.wordId
        WHERE w.lang = :targetLang AND r.previousIntervalDays = 0
          AND r.reviewedAtMs >= :dayStartMs AND r.reviewedAtMs < :dayEndMs
          AND r.id = (SELECT MIN(r2.id) FROM review_log r2 WHERE r2.wordId = r.wordId)
        """
    )
    suspend fun newStartedBetween(targetLang: String, dayStartMs: Long, dayEndMs: Long): Int

    @Upsert
    suspend fun upsertUserWord(state: UserWordEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: ReviewLogEntity)

    /** Saves the new review state and its log entry together. */
    @Transaction
    suspend fun saveReview(state: UserWordEntity, log: ReviewLogEntity) {
        upsertUserWord(state)
        insertLog(log)
    }

    /** Words with their main-language translation, in content order (review state left empty). */
    @Query(
        """
        SELECT w.id AS wordId, w.text, w.example, w.reading, t.text AS translation, t.example AS translationExample,
               NULL AS easeFactor, NULL AS intervalDays, NULL AS repetitions, NULL AS lapses, NULL AS dueEpochDay
        FROM words w JOIN word_translations t ON t.wordId = w.id AND t.lang = :mainLang
        WHERE w.id IN (:ids)
        ORDER BY w.sortOrder
        """
    )
    suspend fun cardsByIds(ids: List<String>, mainLang: String): List<CardRow>

    @Query("SELECT * FROM user_words WHERE wordId = :wordId")
    suspend fun userWord(wordId: String): UserWordEntity?
}
