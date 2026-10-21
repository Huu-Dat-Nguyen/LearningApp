package com.learningapp.domain.speech

import com.learningapp.domain.lesson.AnswerChecker
import com.learningapp.domain.lesson.LanguageRules
import kotlin.math.roundToInt

enum class WordMark { Right, Close, Missed }

/** One word of the target sentence, as written, with how it was heard. */
data class MarkedWord(val text: String, val mark: WordMark)

data class SpeechScore(
    /** 0..100 */
    val score: Int,
    val words: List<MarkedWord>,
    /** The recognizer guess that scored best. */
    val heard: String,
) {
    val label: String get() = when {
        score >= GREAT -> "Great"
        score >= ALMOST -> "Almost"
        else -> "Try again"
    }

    companion object {
        const val GREAT = 80
        const val ALMOST = 50
    }
}

/**
 * Scores what the speech recognizer heard against the target sentence, word by word.
 * Case, punctuation and accents are ignored (recognizers often drop accents).
 * Words are aligned with an edit distance so one missing word doesn't shift the rest.
 */
object PronunciationScorer {

    /** Words for spaced languages; single characters for Japanese (no spaces). */
    private fun tokens(s: String, lang: String): List<String> =
        if (LanguageRules.of(lang).spaced) AnswerChecker.fold(s, lang).split(" ").filter { it.isNotEmpty() }
        else AnswerChecker.normalize(s, lang).map { it.toString() }

    /** Letters-level edit distance between two words. */
    internal fun letterDistance(a: String, b: String): Int {
        val d = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = d[0]
            d[0] = i
            for (j in 1..b.length) {
                val tmp = d[j]
                d[j] = minOf(d[j] + 1, d[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return d[b.length]
    }

    /** One letter off counts as close for words of 4+ letters. */
    private fun isClose(target: String, heard: String) = target.length >= 4 && letterDistance(target, heard) == 1

    /** Scores one recognizer guess. */
    fun score(target: String, heard: String, lang: String = "es"): SpeechScore {
        val spaced = LanguageRules.of(lang).spaced
        val shown = if (spaced) target.split(" ").filter { it.isNotBlank() } else tokens(target, lang)
        val t = tokens(target, lang)
        val h = tokens(heard, lang)
        if (t.isEmpty()) return SpeechScore(0, emptyList(), heard)

        // cost[i][j]: best cost aligning t[0 until i] with h[0 until j]
        val n = t.size
        val m = h.size
        val cost = Array(n + 1) { DoubleArray(m + 1) }
        for (i in 0..n) cost[i][0] = i.toDouble()
        for (j in 0..m) cost[0][j] = j.toDouble()
        for (i in 1..n) for (j in 1..m) {
            val sub = when {
                t[i - 1] == h[j - 1] -> 0.0
                isClose(t[i - 1], h[j - 1]) -> 0.5
                else -> 1.0
            }
            cost[i][j] = minOf(cost[i - 1][j - 1] + sub, cost[i - 1][j] + 1, cost[i][j - 1] + 1)
        }

        // Walk back to mark each target word.
        val marks = Array(n) { WordMark.Missed }
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            val sub = when {
                t[i - 1] == h[j - 1] -> 0.0
                isClose(t[i - 1], h[j - 1]) -> 0.5
                else -> 1.0
            }
            when {
                cost[i][j] == cost[i - 1][j - 1] + sub -> {
                    marks[i - 1] = when (sub) { 0.0 -> WordMark.Right; 0.5 -> WordMark.Close; else -> WordMark.Missed }
                    i--; j--
                }
                cost[i][j] == cost[i - 1][j] + 1 -> i-- // target word not said
                else -> j-- // extra heard word
            }
        }

        val points = marks.sumOf { when (it) { WordMark.Right -> 1.0; WordMark.Close -> 0.5; WordMark.Missed -> 0.0 } }
        // Extra words lower the score a little so saying everything at once doesn't win.
        val extra = (m - n).coerceAtLeast(0) * 0.5
        val score = (((points - extra).coerceAtLeast(0.0)) / n * 100).roundToInt().coerceIn(0, 100)
        // shown and t have the same length unless the target has words that fold to nothing (just punctuation).
        val display = if (shown.size == n) shown else t
        return SpeechScore(score, display.mapIndexed { k, w -> MarkedWord(w, marks[k]) }, heard)
    }

    /** Scores every recognizer guess and keeps the best. */
    fun best(target: String, guesses: List<String>, lang: String = "es"): SpeechScore =
        guesses.map { score(target, it, lang) }.maxByOrNull { it.score } ?: score(target, "", lang)
}
