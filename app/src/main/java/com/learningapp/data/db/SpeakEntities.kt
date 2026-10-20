package com.learningapp.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/** Last time a speaking sentence was practised and its best score. Keyed by the sentence text. */
@Entity(tableName = "speak_practice", primaryKeys = ["lang", "sentence"])
data class SpeakPracticeEntity(
    val lang: String,
    val sentence: String,
    val lastPracticedEpochDay: Long,
    val bestScore: Int,
    /** Day XP was last given for this sentence (XP once per sentence per day). */
    val xpEpochDay: Long = -1,
)

/** A sentence the user has met, with its main-language meaning. */
data class SentenceRow(val text: String, val translation: String)

@Dao
interface SpeakDao {
    @Query("SELECT * FROM speak_practice WHERE lang = :lang")
    suspend fun practice(lang: String): List<SpeakPracticeEntity>

    @Query("SELECT * FROM speak_practice WHERE lang = :lang AND sentence = :sentence")
    suspend fun practice(lang: String, sentence: String): SpeakPracticeEntity?

    @Upsert
    suspend fun upsert(p: SpeakPracticeEntity)

    /** Example sentences of words the user has started. */
    @Query(
        """
        SELECT w.example AS text, t.example AS translation
        FROM user_words u
        JOIN words w ON w.id = u.wordId
        JOIN word_translations t ON t.wordId = w.id AND t.lang = :mainLang
        WHERE u.targetLang = :lang AND w.example != ''
        ORDER BY w.sortOrder
        """
    )
    suspend fun startedWordExamples(lang: String, mainLang: String): List<SentenceRow>

    @Query(
        """
        SELECT l.* FROM lessons l JOIN lesson_progress p ON p.lessonId = l.id
        WHERE l.lang = :lang ORDER BY l.sortOrder
        """
    )
    suspend fun finishedLessons(lang: String): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE lang = :lang ORDER BY sortOrder LIMIT 1")
    suspend fun firstLesson(lang: String): LessonEntity?
}
