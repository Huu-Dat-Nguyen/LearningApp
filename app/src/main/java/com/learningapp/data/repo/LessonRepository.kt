package com.learningapp.data.repo

import androidx.room.withTransaction
import com.learningapp.data.content.ContentExercise
import com.learningapp.data.content.LessonParser
import com.learningapp.data.db.ActivityDao
import com.learningapp.data.db.AppDatabase
import com.learningapp.data.db.CardRow
import com.learningapp.data.db.LessonDao
import com.learningapp.data.db.LessonProgressEntity
import com.learningapp.data.db.LessonRow
import kotlinx.coroutines.flow.map
import com.learningapp.data.db.WordDao
import com.learningapp.domain.srs.Grade
import com.learningapp.domain.srs.Sm2Scheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

enum class LessonState { Done, Next, Locked }

data class LessonItem(val id: String, val title: String, val state: LessonState, val bestAccuracy: Double?)

data class UnitItem(val id: String, val title: String, val level: String, val lessons: List<LessonItem>)

/** Everything the lesson player needs: exercises plus the words they use (with main-language translations). */
data class LessonData(
    val id: String,
    val title: String,
    val unitId: String,
    val targetLang: String,
    val mainLang: String,
    val exercises: List<ContentExercise>,
    val lessonWordIds: List<String>,
    /** All words of the lesson's unit, used for multiple-choice options. */
    val unitWords: List<CardRow>,
)

data class LessonResult(val lessonId: String, val accuracy: Double, val xp: Int, val answered: Int, val correct: Int)

/** Groups lesson rows into units; the first unfinished lesson is Next, later unfinished ones Locked. */
internal fun toUnits(rows: List<LessonRow>, mainLang: String): List<UnitItem> {
    var nextGiven = false
    val items = rows.map { r ->
        val state = when {
            r.completed -> LessonState.Done
            !nextGiven -> { nextGiven = true; LessonState.Next }
            else -> LessonState.Locked
        }
        r to LessonItem(r.id, LessonParser.text(r.titleJson, mainLang), state, r.bestAccuracy)
    }
    return items.groupBy { it.first.unitId }.map { (unitId, list) ->
        val first = list.first().first
        UnitItem(unitId, LessonParser.text(first.unitTitleJson, mainLang), first.level, list.map { it.second })
    }
}

@Singleton
class LessonRepository @Inject constructor(
    private val db: AppDatabase,
    private val lessonDao: LessonDao,
    private val wordDao: WordDao,
    private val activityDao: ActivityDao,
    private val prefs: UserPrefs,
    private val clock: Clock,
) {
    private fun today(): LocalDate = LocalDate.now(clock)

    /** Units in order; the first unfinished lesson is Next, everything after it Locked. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun units(): Flow<List<UnitItem>> =
        combine(prefs.targetLang, prefs.mainLang) { t, m -> t to m }.flatMapLatest { (target, main) ->
            lessonDao.lessons(target).map { rows -> toUnits(rows, main) }
        }

    /** The lesson Home's "Continue lesson" should open: the first one not done, or null when all are done. */
    suspend fun nextLesson(): LessonItem? =
        units().first().flatMap { it.lessons }.firstOrNull { it.state == LessonState.Next }

    suspend fun lesson(id: String): LessonData? {
        val entity = lessonDao.lesson(id) ?: return null
        val main = prefs.mainLang.first()
        val unitWordIds = lessonDao.lessonsInUnit(entity.unitId).flatMap { LessonParser.wordIds(it) }.distinct()
        return LessonData(
            id = entity.id,
            title = LessonParser.text(entity.titleJson, main),
            unitId = entity.unitId,
            targetLang = entity.lang,
            mainLang = main,
            exercises = LessonParser.exercises(entity),
            lessonWordIds = LessonParser.wordIds(entity),
            unitWords = wordDao.cardsByIds(unitWordIds, main),
        )
    }

    /**
     * Saves a finished lesson: best result, today's XP/answers, and adds the lesson's
     * new words to the flashcards as if answered Good (so they come back tomorrow).
     */
    suspend fun completeLesson(result: LessonResult, lessonWordIds: List<String>) {
        val now = clock.millis()
        val day = today()
        val target = prefs.targetLang.first()
        db.withTransaction {
            val old = lessonDao.progress(result.lessonId)
            lessonDao.upsertProgress(
                LessonProgressEntity(
                    lessonId = result.lessonId,
                    firstCompletedAtMs = old?.firstCompletedAtMs ?: now,
                    lastCompletedAtMs = now,
                    bestAccuracy = maxOf(old?.bestAccuracy ?: 0.0, result.accuracy),
                    attempts = (old?.attempts ?: 0) + 1,
                    xpEarned = (old?.xpEarned ?: 0) + result.xp,
                )
            )
            activityDao.add(day.toEpochDay(), target, xp = result.xp, answered = result.answered, correct = result.correct)
            lessonWordIds.forEach { id ->
                if (wordDao.userWord(id) == null) {
                    wordDao.upsertUserWord(Sm2Scheduler.review(null, id, target, Grade.Good, day, now))
                }
            }
        }
    }
}
