package com.learningapp.domain.lesson

import com.learningapp.data.content.ContentExercise
import com.learningapp.data.db.CardRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LessonEngineTest {

    private fun card(id: String, text: String, tr: String) = CardRow(id, text, "", tr, "", null, null, null, null, null)

    private val words = listOf(
        card("es_hola", "hola", "hello"),
        card("es_adios", "adiós", "goodbye"),
        card("es_gracias", "gracias", "thank you"),
        card("es_por_favor", "por favor", "please"),
        card("es_perdon", "perdón", "sorry"),
    )
    private val builder = ExerciseBuilder(words, "en", Random(42))

    // --- AnswerChecker ---

    @Test
    fun ignoresCasePunctuationAndSpaces() {
        assertEquals(Verdict.Correct, AnswerChecker.checkText("  ¡ADIÓS!  ", listOf("Adiós")).verdict)
        assertEquals(Verdict.Correct, AnswerChecker.checkText("buenos   dias? no", listOf("¿Buenos dias? No.")).verdict)
    }

    @Test
    fun missingAccentIsASlipButCountsAsRight() {
        val r = AnswerChecker.checkText("adios", listOf("Adiós"))
        assertEquals(Verdict.AccentSlip, r.verdict)
        assertTrue(r.isCorrect)
        assertEquals("Adiós", r.expected)
    }

    @Test
    fun nAndEnyeAreDifferentLetters() {
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("el nino", listOf("El niño")).verdict)
    }

    @Test
    fun wrongOrEmptyIsWrong() {
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("hola", listOf("Adiós")).verdict)
        assertEquals(Verdict.Wrong, AnswerChecker.checkText("   ", listOf("Adiós")).verdict)
    }

    @Test
    fun anyAcceptedAnswerWorks() {
        assertTrue(AnswerChecker.checkText("muchas gracias", listOf("Gracias", "Muchas gracias")).isCorrect)
    }

    // --- ExerciseBuilder ---

    @Test
    fun multipleChoiceHasRightAnswerAndThreeOthersFromUnit() {
        val mc = builder.build(ContentExercise.Mc("es_hola")) as Exercise.MultipleChoice
        assertEquals("hola", mc.prompt)
        assertEquals(4, mc.options.size)
        assertEquals("hello", mc.options[mc.correctIndex])
        assertEquals(4, mc.options.toSet().size)
        assertTrue(mc.options.all { o -> words.any { it.translation == o } })
    }

    @Test
    fun unknownWordIsSkipped() {
        assertEquals(null, builder.build(ContentExercise.Mc("es_missing")))
    }

    @Test
    fun fillSplitsAroundBlank() {
        val f = builder.build(ContentExercise.Fill("Buenos ___, ¿cómo está?", "días", mapOf("en" to "Good morning"))) as Exercise.FillBlank
        assertEquals("Buenos ", f.before)
        assertEquals(", ¿cómo está?", f.after)
        assertEquals("Good morning", f.hint)
    }

    @Test
    fun orderChipsAreShuffledButSameWords() {
        val o = builder.build(ContentExercise.Order("Quiero pan con queso", mapOf("en" to "I want bread with cheese"))) as Exercise.WordOrder
        assertEquals(listOf("Quiero", "pan", "con", "queso"), o.answer)
        assertNotEquals(o.answer, o.chips)
        assertEquals(o.answer.sorted(), o.chips.sorted())
        assertTrue(AnswerChecker.check(o, Answer.Chips(o.answer)).isCorrect)
        assertFalse(AnswerChecker.check(o, Answer.Chips(o.chips)).isCorrect)
    }

    @Test
    fun translateUsesMainLanguageSource() {
        val t = builder.build(ContentExercise.Translate(mapOf("en" to "Goodbye!"), listOf("Adiós"))) as Exercise.Translate
        assertEquals("Goodbye!", t.source)
        assertTrue(AnswerChecker.check(t, Answer.Text("adiós")).isCorrect)
    }

    // --- LessonSession ---

    private fun sessionOf3() = LessonSession(
        listOf(
            Exercise.Listen("hola", "hello"),
            Exercise.Listen("adiós", "goodbye"),
            Exercise.Listen("gracias", "thank you"),
        )
    )

    @Test
    fun allRightFirstTime() {
        val s = sessionOf3()
        listOf("hola", "adiós", "gracias").forEach { s.submit(Answer.Text(it)); s.next() }
        assertTrue(s.finished)
        assertEquals(1.0, s.accuracy, 1e-9)
        assertEquals(13, s.xp)
        assertEquals(1f, s.progress)
    }

    @Test
    fun wrongAnswerComesBackOnceAtTheEnd() {
        val s = sessionOf3()
        assertFalse(s.submit(Answer.Text("nope")).isCorrect); s.next()   // hola wrong -> retry later
        s.submit(Answer.Text("adiós")); s.next()
        s.submit(Answer.Text("gracias")); s.next()
        assertFalse(s.finished)
        assertTrue(s.isRetry)
        assertEquals("hola", (s.exercise as Exercise.Listen).text)
        assertFalse(s.submit(Answer.Text("still wrong")).isCorrect); s.next() // no second retry
        assertTrue(s.finished)
        assertEquals(2.0 / 3, s.accuracy, 1e-9)
        assertEquals(12, s.xp)
        assertEquals(4, s.answered)
        assertEquals(2, s.correct)
    }

    @Test
    fun progressGrowsWithRetries() {
        val s = sessionOf3()
        assertEquals(0f, s.progress)
        s.submit(Answer.Text("x")); s.next()
        assertEquals(1f / 4, s.progress) // 1 done of 3 + 1 retry
    }
}
