package com.learningapp.domain.speech

import com.learningapp.data.repo.PracticeSentence
import com.learningapp.data.repo.orderSentences
import com.learningapp.data.repo.speakXp
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class PronunciationScorerTest {

    private fun marks(s: SpeechScore) = s.words.map { it.mark }

    @Test
    fun perfectIgnoringCasePunctuationAndAccents() {
        val s = PronunciationScorer.score("¿Dónde está el baño?", "donde esta el baño")
        assertEquals(100, s.score)
        assertEquals(listOf("¿Dónde", "está", "el", "baño?"), s.words.map { it.text }) // shown as written
        assertEquals("Great", s.label)
    }

    @Test
    fun nForEnyeIsOnlyClose() {
        val s = PronunciationScorer.score("el baño", "el bano")
        assertEquals(listOf(WordMark.Right, WordMark.Close), marks(s))
    }

    @Test
    fun missingWordDoesNotShiftTheRest() {
        val s = PronunciationScorer.score("Quiero pan con queso", "quiero con queso")
        assertEquals(listOf(WordMark.Right, WordMark.Missed, WordMark.Right, WordMark.Right), marks(s))
        assertEquals(75, s.score)
        assertEquals("Almost", s.label)
    }

    @Test
    fun oneLetterOffIsClose() {
        val s = PronunciationScorer.score("Muchas gracias", "muchas graciaz")
        assertEquals(listOf(WordMark.Right, WordMark.Close), marks(s))
        assertEquals(75, s.score)
    }

    @Test
    fun shortWordsMustMatchExactly() {
        val s = PronunciationScorer.score("el pan", "al pan")
        assertEquals(listOf(WordMark.Missed, WordMark.Right), marks(s))
    }

    @Test
    fun extraWordsCostALittle() {
        assertEquals(75, PronunciationScorer.score("hola amigo", "hola hola amigo").score)
    }

    @Test
    fun nothingHeard() {
        val s = PronunciationScorer.score("Buenas noches", "")
        assertEquals(0, s.score)
        assertEquals("Try again", s.label)
        assertEquals(listOf(WordMark.Missed, WordMark.Missed), marks(s))
    }

    @Test
    fun bestGuessWins() {
        val s = PronunciationScorer.best("Buenas noches", listOf("buenas notches", "buenas noches", "bueno"))
        assertEquals(100, s.score)
        assertEquals("buenas noches", s.heard)
    }

    @Test
    fun letterDistance() {
        assertEquals(0, PronunciationScorer.letterDistance("hola", "hola"))
        assertEquals(1, PronunciationScorer.letterDistance("hola", "ola"))
        assertEquals(2, PronunciationScorer.letterDistance("casa", "cosas"))
    }

    @Test
    fun xpThresholds() {
        assertEquals(2, speakXp(80))
        assertEquals(1, speakXp(50))
        assertEquals(0, speakXp(49))
    }

    @Test
    fun neverPractisedFirstThenOldest() {
        val all = listOf(PracticeSentence("a", ""), PracticeSentence("b", ""), PracticeSentence("c", ""), PracticeSentence("a", ""))
        val ordered = orderSentences(all, mapOf("a" to 10L, "b" to 5L), Random(3))
        assertEquals(listOf("c", "b", "a"), ordered.map { it.text })
    }
}
