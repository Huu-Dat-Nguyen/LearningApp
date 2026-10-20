package com.learningapp.data.repo

import com.learningapp.data.db.ActivityDao
import com.learningapp.data.db.CardRow
import com.learningapp.data.db.ReviewLogEntity
import com.learningapp.data.db.UserWordEntity
import com.learningapp.data.db.WordDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val dao: WordDao,
    private val activityDao: ActivityDao,
    private val prefs: UserPrefs,
    private val clock: Clock,
) : ReviewRepository {
    fun today(): LocalDate = LocalDate.now(clock)

    override suspend fun targetLang(): String = prefs.targetLang.first()

    override suspend fun dueCards(): List<CardRow> = dueCards(200)

    suspend fun dueCards(limit: Int): List<CardRow> =
        dao.dueCards(prefs.targetLang.first(), prefs.mainLang.first(), today().toEpochDay(), limit)

    override suspend fun newCards(limit: Int): List<CardRow> =
        dao.newCards(prefs.targetLang.first(), prefs.mainLang.first(), limit)

    /** New cards still allowed today under [dailyLimit]. */
    override suspend fun newCardsLeftToday(dailyLimit: Int): Int {
        val start = today().atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val end = today().plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val started = dao.newStartedBetween(prefs.targetLang.first(), start, end)
        return (dailyLimit - started).coerceAtLeast(0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun dueCount(): Flow<Int> = prefs.targetLang.flatMapLatest { dao.dueCount(it, today().toEpochDay()) }

    /** Stores the state the scheduler computed after a review, plus a log row. */
    override suspend fun saveReview(newState: UserWordEntity, grade: Int, previousIntervalDays: Int) {
        val log = ReviewLogEntity(
            wordId = newState.wordId,
            grade = grade,
            reviewedAtMs = clock.millis(),
            previousIntervalDays = previousIntervalDays,
            newIntervalDays = newState.intervalDays,
        )
        dao.saveReview(newState, log)
        // 1 XP per card, so flashcard-only days still count for the streak. Again counts as not correct.
        activityDao.add(today().toEpochDay(), newState.targetLang, xp = REVIEW_XP, answered = 1, correct = if (grade == 0) 0 else 1)
    }

    companion object {
        const val REVIEW_XP = 1
    }
}
