package com.learningapp.data.repo

import com.learningapp.data.content.ContentExercise
import com.learningapp.data.content.LessonParser
import com.learningapp.data.db.ActivityDao
import com.learningapp.data.db.LessonEntity
import com.learningapp.data.db.SpeakDao
import com.learningapp.data.db.SpeakPracticeEntity
import com.learningapp.domain.speech.SpeechScore
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class PracticeSentence(val text: String, val translation: String)

/** What the Speak screen needs from storage. [SpeakRepository] implements it; tests use a fake. */
interface SpeakStore {
    suspend fun targetLang(): String
    suspend fun sentences(): List<PracticeSentence>
    /** Saves an attempt; returns the XP given (0 if this sentence already gave XP today). */
    suspend fun recordAttempt(sentence: String, score: Int): Int
}

/** XP for one attempt: 2 for 80+, 1 for 50–79, else 0. */
fun speakXp(score: Int): Int = when {
    score >= SpeechScore.GREAT -> 2
    score >= SpeechScore.ALMOST -> 1
    else -> 0
}

/** Sentences from a lesson's listen and word-order exercises. */
internal fun lessonSentences(lesson: LessonEntity, mainLang: String): List<PracticeSentence> =
    LessonParser.exercises(lesson).mapNotNull { e ->
        when (e) {
            is ContentExercise.Listen -> PracticeSentence(e.text, e.translation[mainLang] ?: e.translation["en"].orEmpty())
            is ContentExercise.Order -> PracticeSentence(e.answer, e.translation[mainLang] ?: e.translation["en"].orEmpty())
            else -> null
        }
    }

/** Never-practised sentences first, then oldest practice first; shuffled within each day group. */
internal fun orderSentences(all: List<PracticeSentence>, lastPracticed: Map<String, Long>, random: Random): List<PracticeSentence> =
    all.distinctBy { it.text }
        .shuffled(random)
        .sortedBy { lastPracticed[it.text] ?: Long.MIN_VALUE }

@Singleton
class SpeakRepository @Inject constructor(
    private val speakDao: SpeakDao,
    private val activityDao: ActivityDao,
    private val prefs: UserPrefs,
    private val clock: Clock,
) : SpeakStore {
    private fun today() = LocalDate.now(clock).toEpochDay()

    override suspend fun targetLang(): String = prefs.targetLang.first()

    override suspend fun sentences(): List<PracticeSentence> {
        val lang = prefs.targetLang.first()
        val main = prefs.mainLang.first()
        val fromWords = speakDao.startedWordExamples(lang, main).map { PracticeSentence(it.text, it.translation) }
        val fromLessons = speakDao.finishedLessons(lang).flatMap { lessonSentences(it, main) }
        val all = (fromLessons + fromWords).ifEmpty {
            speakDao.firstLesson(lang)?.let { lessonSentences(it, main) }.orEmpty()
        }
        val practiced = speakDao.practice(lang).associate { it.sentence to it.lastPracticedEpochDay }
        return orderSentences(all, practiced, Random.Default)
    }

    override suspend fun recordAttempt(sentence: String, score: Int): Int {
        val lang = prefs.targetLang.first()
        val day = today()
        val old = speakDao.practice(lang, sentence)
        val xp = if (old?.xpEpochDay == day) 0 else speakXp(score)
        speakDao.upsert(
            SpeakPracticeEntity(
                lang = lang,
                sentence = sentence,
                lastPracticedEpochDay = day,
                bestScore = maxOf(old?.bestScore ?: 0, score),
                xpEpochDay = if (xp > 0) day else old?.xpEpochDay ?: -1,
            )
        )
        activityDao.add(day, lang, xp = xp, answered = 1, correct = if (score >= SpeechScore.ALMOST) 1 else 0)
        return xp
    }
}
