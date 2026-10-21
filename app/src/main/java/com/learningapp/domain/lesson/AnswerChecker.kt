package com.learningapp.domain.lesson

import java.text.Normalizer

enum class Verdict { Correct, AccentSlip, Wrong }

/** [expected] is the right spelling, set for an accent slip (the screen shows "Watch the accent: …"). */
data class CheckResult(val verdict: Verdict, val expected: String? = null) {
    /** Accent slips count as right. */
    val isCorrect: Boolean get() = verdict != Verdict.Wrong
}

object AnswerChecker {

    // Latin and Japanese punctuation, plus dashes. Apostrophes stay (French l'eau).
    private val punctuation = Regex("[¿¡?!.,;:\"“”«»—–\\-。、！？「」『』・（）()…]")

    /**
     * Lowercase, unify apostrophes, drop punctuation, collapse spaces. Keeps accents.
     * For languages written without spaces (Japanese), spaces are dropped too.
     */
    fun normalize(s: String, lang: String = "es"): String {
        val out = s.lowercase()
            .replace('’', '\'').replace('‘', '\'')
            .replace('　', ' ')
            .replace(punctuation, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (LanguageRules.of(lang).spaced) out else out.replace(" ", "")
    }

    /** [normalize] and also strip accents, except letters the language treats as its own (Spanish ñ). */
    fun fold(s: String, lang: String = "es"): String {
        val keep = LanguageRules.of(lang).keepLetters.toList()
        var t = normalize(s, lang)
        keep.forEachIndexed { i, c -> t = t.replace(c.toString(), "\u0000$i\u0000") }
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        keep.forEachIndexed { i, c -> t = t.replace("\u0000$i\u0000", c.toString()) }
        return t
    }

    /** Compares typed text with any of the accepted [answers] in target language [lang]. */
    fun checkText(input: String, answers: List<String>, lang: String = "es"): CheckResult {
        val typed = normalize(input, lang)
        if (typed.isEmpty()) return CheckResult(Verdict.Wrong)
        if (answers.any { normalize(it, lang) == typed }) return CheckResult(Verdict.Correct)
        if (!LanguageRules.of(lang).accentSlips) return CheckResult(Verdict.Wrong)
        val slip = answers.firstOrNull { fold(it, lang) == fold(typed, lang) }
        return if (slip != null) CheckResult(Verdict.AccentSlip, slip.trim()) else CheckResult(Verdict.Wrong)
    }

    fun check(exercise: Exercise, answer: Answer, lang: String = "es"): CheckResult = when (exercise) {
        is Exercise.MultipleChoice ->
            if ((answer as? Answer.Choice)?.index == exercise.correctIndex) CheckResult(Verdict.Correct) else CheckResult(Verdict.Wrong)
        is Exercise.WordOrder ->
            if ((answer as? Answer.Chips)?.chips == exercise.answer) CheckResult(Verdict.Correct) else CheckResult(Verdict.Wrong)
        is Exercise.FillBlank -> checkText((answer as? Answer.Text)?.text.orEmpty(), listOf(exercise.answer) + exercise.accept, lang)
        is Exercise.Listen -> checkText((answer as? Answer.Text)?.text.orEmpty(), listOf(exercise.text) + exercise.accept, lang)
        is Exercise.Translate -> checkText((answer as? Answer.Text)?.text.orEmpty(), exercise.answers, lang)
    }
}
