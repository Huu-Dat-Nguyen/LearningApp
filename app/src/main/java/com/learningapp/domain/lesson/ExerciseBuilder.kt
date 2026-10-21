package com.learningapp.domain.lesson

import com.learningapp.data.content.ContentExercise
import com.learningapp.data.db.CardRow
import kotlin.random.Random

/**
 * Turns lesson content into [Exercise]s for one main language.
 * Shuffling uses [random] so tests can pass a seeded one.
 */
class ExerciseBuilder(
    private val unitWords: List<CardRow>,
    private val mainLang: String,
    private val random: Random = Random.Default,
    private val targetLang: String = "es",
) {
    private val rules = LanguageRules.of(targetLang)

    private val byId = unitWords.associateBy { it.wordId }

    fun build(content: List<ContentExercise>): List<Exercise> = content.mapNotNull(::build)

    fun build(e: ContentExercise): Exercise? = when (e) {
        is ContentExercise.Mc -> multipleChoice(e.wordId)
        is ContentExercise.Fill -> {
            val (before, after) = e.sentence.split("___", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            Exercise.FillBlank(before, after, e.answer, text(e.hint), e.accept)
        }
        is ContentExercise.Order -> {
            val words = e.chips ?: e.answer.split(" ").filter { it.isNotBlank() }
            Exercise.WordOrder(shuffledNotSame(words), words, text(e.translation), joiner = if (rules.spaced) " " else "")
        }
        is ContentExercise.Listen -> Exercise.Listen(e.text, text(e.translation), e.accept)
        is ContentExercise.Translate -> Exercise.Translate(text(e.source), e.answers)
    }

    private fun text(map: Map<String, String>) = map[mainLang] ?: map["en"] ?: map.values.firstOrNull().orEmpty()

    /** Target word as the prompt; the right translation plus up to 3 others from the same unit. */
    private fun multipleChoice(wordId: String): Exercise? {
        val word = byId[wordId] ?: return null
        val distractors = unitWords.filter { it.wordId != wordId && it.translation != word.translation }
            .map { it.translation }.distinct().shuffled(random).take(3)
        val options = (distractors + word.translation).shuffled(random)
        return Exercise.MultipleChoice(word.text, options, options.indexOf(word.translation), word.reading)
    }

    /** Shuffles chips, avoiding handing back the answer already in order. */
    private fun shuffledNotSame(words: List<String>): List<String> {
        if (words.toSet().size < 2) return words
        repeat(10) {
            val s = words.shuffled(random)
            if (s != words) return s
        }
        return words.reversed()
    }
}
