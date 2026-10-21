package com.learningapp.domain.lesson

import com.learningapp.domain.speech.PronunciationScorer
import com.learningapp.domain.speech.WordMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageRulesTest {

    @Test
    fun frenchAccentsAreSlipsAndApostrophesKept() {
        assertEquals(Verdict.AccentSlip, AnswerChecker.checkText("le cafe", listOf("le café"), "fr").verdict)
        assertEquals(Verdict.Correct, AnswerChecker.checkText("l’eau", listOf("l'eau"), "fr").verdict) // curly apostrophe
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("leau", listOf("l'eau"), "fr").verdict)
        assertEquals(Verdict.AccentSlip, AnswerChecker.checkText("garcon", listOf("garçon"), "fr").verdict)
    }

    @Test
    fun vietnameseToneMarksMustBeTyped() {
        assertEquals(Verdict.Correct, AnswerChecker.checkText("Xin chào", listOf("xin chào"), "vi").verdict)
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("xin chao", listOf("xin chào"), "vi").verdict)
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("ma", listOf("má"), "vi").verdict)
    }

    @Test
    fun japaneseIgnoresSpacesAndJapanesePunctuation() {
        assertEquals(Verdict.Correct, AnswerChecker.checkText("こんにちは", listOf("こんにちは。"), "ja").verdict)
        assertEquals(Verdict.Correct, AnswerChecker.checkText("お水 を ください", listOf("お水をください"), "ja").verdict)
        assertEquals(Verdict.Correct, AnswerChecker.checkText("おみずをください", listOf("お水をください", "おみずをください"), "ja").verdict)
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("おちゃをください", listOf("お水をください"), "ja").verdict)
    }

    @Test
    fun spanishEnyeStaysItsOwnLetter() {
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("nino", listOf("niño"), "es").verdict)
        assertEquals(Verdict.AccentSlip, AnswerChecker.checkText("adios", listOf("adiós"), "es").verdict)
    }

    @Test
    fun japaneseWordOrderUsesContentChipsAndNoSpaces() {
        val builder = ExerciseBuilder(emptyList(), "en", kotlin.random.Random(1), "ja")
        val ex = builder.build(
            com.learningapp.data.content.ContentExercise.Order("私は 水を 飲みます", mapOf("en" to "I drink water"), listOf("私は", "水を", "飲みます"))
        ) as Exercise.WordOrder
        assertEquals(listOf("私は", "水を", "飲みます"), ex.answer)
        assertEquals("私は水を飲みます", ex.solution)
        assertTrue(AnswerChecker.check(ex, Answer.Chips(ex.answer), "ja").isCorrect)
    }

    @Test
    fun japaneseSpeechIsScoredByCharacter() {
        val s = PronunciationScorer.score("ありがとう。", "ありがと", "ja")
        assertEquals(listOf("あ", "り", "が", "と", "う"), s.words.map { it.text })
        assertEquals(listOf(WordMark.Right, WordMark.Right, WordMark.Right, WordMark.Right, WordMark.Missed), s.words.map { it.mark })
        assertEquals(80, s.score)
    }

    @Test
    fun frenchSpeechIgnoresAccents() {
        assertEquals(100, PronunciationScorer.score("Un café, s'il vous plaît", "un cafe s'il vous plait", "fr").score)
    }
}
