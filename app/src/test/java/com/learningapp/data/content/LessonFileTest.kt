package com.learningapp.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the bundled lessons so a bad content edit fails the build, not the app. */
class LessonFileTest {

    private val lessons = LessonParser.parse(File("src/main/assets/content/es/lessons.json").readText(Charsets.UTF_8))
    private val wordIds = ContentParser.parse(File("src/main/assets/content/es/words.json").readText(Charsets.UTF_8))
        .words.map { it.id }.toSet()

    private val allLessons get() = lessons.units.flatMap { it.lessons }

    @Test
    fun structure() {
        assertEquals("es", lessons.lang)
        assertEquals(5, lessons.units.size)
        assertEquals(10, allLessons.size)
        assertEquals(allLessons.size, allLessons.map { it.id }.toSet().size)
        assertEquals(lessons.units.size, lessons.units.map { it.id }.toSet().size)
    }

    @Test
    fun everyLessonUsesKnownWordsAndAllExerciseTypes() {
        allLessons.forEach { l ->
            assertTrue("${l.id} unknown word", wordIds.containsAll(l.words))
            val types = l.exercises.map { it::class }.toSet()
            assertEquals("${l.id} types", 5, types.size)
            l.exercises.filterIsInstance<ContentExercise.Mc>().forEach {
                assertTrue("${l.id} mc ${it.wordId}", it.wordId in l.words)
            }
        }
    }

    @Test
    fun exerciseFieldsAreFilled() {
        allLessons.flatMap { l -> l.exercises.map { l.id to it } }.forEach { (id, e) ->
            when (e) {
                is ContentExercise.Fill -> {
                    assertEquals("$id fill blank", 1, Regex("___").findAll(e.sentence).count())
                    assertTrue("$id fill", e.answer.isNotBlank() && e.hint["en"]?.isNotBlank() == true)
                }
                is ContentExercise.Order -> assertTrue("$id order", e.answer.split(" ").size in 3..6 && e.translation["en"] != null)
                is ContentExercise.Listen -> assertTrue("$id listen", e.text.isNotBlank() && e.translation["en"] != null)
                is ContentExercise.Translate -> assertTrue("$id translate", e.answers.isNotEmpty() && e.source["en"] != null)
                is ContentExercise.Mc -> Unit
            }
        }
    }

    @Test
    fun everyLessonBuildsAndItsOwnAnswersPass() {
        val words = ContentParser.parse(File("src/main/assets/content/es/words.json").readText(Charsets.UTF_8)).words
        lessons.units.forEach { unit ->
            val unitWordIds = unit.lessons.flatMap { it.words }.toSet()
            val cards = words.filter { it.id in unitWordIds }.map {
                com.learningapp.data.db.CardRow(it.id, it.text, it.example, it.translations.getValue("en").text, "", null, null, null, null, null)
            }
            val builder = com.learningapp.domain.lesson.ExerciseBuilder(cards, "en", kotlin.random.Random(1))
            unit.lessons.forEach { l ->
                val built = builder.build(l.exercises)
                assertEquals("${l.id} all built", l.exercises.size, built.size)
                built.forEach { ex ->
                    if (ex is com.learningapp.domain.lesson.Exercise.MultipleChoice) assertEquals("${l.id} mc options", 4, ex.options.size)
                    val answer = when (ex) {
                        is com.learningapp.domain.lesson.Exercise.MultipleChoice -> com.learningapp.domain.lesson.Answer.Choice(ex.correctIndex)
                        is com.learningapp.domain.lesson.Exercise.WordOrder -> com.learningapp.domain.lesson.Answer.Chips(ex.answer)
                        else -> com.learningapp.domain.lesson.Answer.Text(ex.solution)
                    }
                    assertTrue("${l.id} ${ex.solution}", com.learningapp.domain.lesson.AnswerChecker.check(ex, answer).isCorrect)
                }
            }
        }
    }

    @Test
    fun entitiesRoundTrip() {
        val entities = LessonParser.toEntities(lessons)
        assertEquals((0 until 10).toList(), entities.map { it.sortOrder })
        val first = entities.first()
        assertEquals(allLessons.first().exercises, LessonParser.exercises(first))
        assertEquals(allLessons.first().words, LessonParser.wordIds(first))
        assertEquals("Greetings", LessonParser.text(first.unitTitleJson, "en"))
        assertEquals("Greetings", LessonParser.text(first.unitTitleJson, "vi")) // falls back to English
    }
}
