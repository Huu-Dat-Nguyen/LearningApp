package com.learningapp.domain.lesson

import java.text.Normalizer

enum class Verdict { Correct, AccentSlip, Wrong }

/** [note] explains an accent slip, e.g. "Watch the accent: días". */
data class CheckResult(val verdict: Verdict, val note: String? = null) {
    /** Accent slips count as right. */
    val isCorrect: Boolean get() = verdict != Verdict.Wrong
}

object AnswerChecker {

    /** Lowercase, drop punctuation (¿ ¡ ? ! . , ; : " quotes), collapse spaces. Keeps accents. */
    fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("[¿¡?!.,;:\"“”«»—–-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** [normalize] and also strip accents (ñ is kept as its own letter). */
    fun fold(s: String): String =
        Normalizer.normalize(normalize(s).replace("ñ", "\u0000"), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("\u0000", "ñ")

    /** Compares typed text with any of the accepted [answers]. */
    fun checkText(input: String, answers: List<String>): CheckResult {
        val typed = normalize(input)
        if (typed.isEmpty()) return CheckResult(Verdict.Wrong)
        if (answers.any { normalize(it) == typed }) return CheckResult(Verdict.Correct)
        val slip = answers.firstOrNull { fold(it) == fold(typed) }
        return if (slip != null) CheckResult(Verdict.AccentSlip, "Watch the accent: ${slip.trim()}") else CheckResult(Verdict.Wrong)
    }

    fun check(exercise: Exercise, answer: Answer): CheckResult = when (exercise) {
        is Exercise.MultipleChoice ->
            if ((answer as? Answer.Choice)?.index == exercise.correctIndex) CheckResult(Verdict.Correct) else CheckResult(Verdict.Wrong)
        is Exercise.WordOrder ->
            if ((answer as? Answer.Chips)?.chips == exercise.answer) CheckResult(Verdict.Correct) else CheckResult(Verdict.Wrong)
        is Exercise.FillBlank -> checkText((answer as? Answer.Text)?.text.orEmpty(), listOf(exercise.answer))
        is Exercise.Listen -> checkText((answer as? Answer.Text)?.text.orEmpty(), listOf(exercise.text))
        is Exercise.Translate -> checkText((answer as? Answer.Text)?.text.orEmpty(), exercise.answers)
    }
}
