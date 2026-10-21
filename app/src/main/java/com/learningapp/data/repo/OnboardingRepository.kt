package com.learningapp.data.repo

import com.learningapp.data.content.ContentCatalog
import com.learningapp.data.content.ContentExercise
import com.learningapp.data.content.ContentSeeder
import com.learningapp.data.content.LessonParser
import com.learningapp.data.db.LessonDao
import com.learningapp.data.db.LessonEntity
import com.learningapp.data.db.WordDao
import com.learningapp.domain.lesson.Exercise
import com.learningapp.domain.lesson.ExerciseBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** One placement question and the unit (0-based) it comes from. */
data class PlacementQuestion(val unitIndex: Int, val exercise: Exercise.MultipleChoice)

/** Everything onboarding collects. */
data class OnboardingChoices(
    val mainLang: String,
    val targetLang: String,
    val startUnitIndex: Int,
    val goalMinutes: Int,
    val reminderEnabled: Boolean,
    val reminderMinuteOfDay: Int,
)

object Placement {
    const val QUESTIONS_PER_UNIT = 2
    const val UNITS = 3

    /** 0–2 right → unit 1, 3–4 → unit 2, 5–6 → unit 3 (0-based index returned). */
    fun startUnit(correct: Int): Int = when {
        correct >= 5 -> 2
        correct >= 3 -> 1
        else -> 0
    }

    /** Lesson ids grouped by unit, in order; used to pick question words. */
    fun units(lessons: List<LessonEntity>): List<List<LessonEntity>> =
        lessons.groupBy { it.unitId }.values.toList()
}

/** What onboarding needs from storage. [OnboardingRepository] implements it; tests use a fake. */
interface OnboardingStore {
    suspend fun needsOnboarding(): Boolean
    fun pairs(): Map<String, Set<String>>
    suspend fun placementQuestions(main: String, target: String): List<PlacementQuestion>
    suspend fun finish(c: OnboardingChoices)
}

@Singleton
class OnboardingRepository @Inject constructor(
    private val prefs: UserPrefs,
    private val catalog: ContentCatalog,
    private val seeder: ContentSeeder,
    private val lessonDao: LessonDao,
    private val wordDao: WordDao,
    private val lessons: LessonRepository,
) : OnboardingStore {
    /**
     * True when onboarding should show. Installs from before onboarding existed
     * (they already have progress) are marked done and skip it.
     */
    override suspend fun needsOnboarding(): Boolean {
        prefs.onboardingDone.first()?.let { return !it }
        val existing = lessonDao.hasAnyProgress()
        if (existing) prefs.setOnboardingDone(true)
        return !existing
    }

    override fun pairs(): Map<String, Set<String>> = catalog.pairs

    override suspend fun placementQuestions(main: String, target: String): List<PlacementQuestion> =
        placementQuestions(main, target, Random.Default)

    /** Two questions from each of the first three units, easiest first. */
    suspend fun placementQuestions(main: String, target: String, random: Random): List<PlacementQuestion> {
        seeder.seed(target)
        val units = Placement.units(lessonDao.allLessons(target)).take(Placement.UNITS)
        return units.flatMapIndexed { index, unitLessons ->
            val wordIds = unitLessons.flatMap { LessonParser.wordIds(it) }.distinct()
            val cards = wordDao.cardsByIds(wordIds, main)
            val builder = ExerciseBuilder(cards, main, random)
            cards.shuffled(random).take(Placement.QUESTIONS_PER_UNIT).mapNotNull { card ->
                (builder.build(ContentExercise.Mc(card.wordId)) as? Exercise.MultipleChoice)?.let { PlacementQuestion(index, it) }
            }
        }
    }

    override suspend fun finish(c: OnboardingChoices) {
        prefs.setLanguages(c.mainLang, c.targetLang)
        prefs.setDailyGoal(c.goalMinutes)
        prefs.setReminder(c.reminderEnabled, c.reminderMinuteOfDay)
        seeder.seed(c.targetLang)
        lessons.testOut(c.targetLang, c.startUnitIndex)
        prefs.setOnboardingDone(true)
    }
}
