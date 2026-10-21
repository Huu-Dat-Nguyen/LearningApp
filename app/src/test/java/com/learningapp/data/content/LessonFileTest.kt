package com.learningapp.data.content

import com.learningapp.data.db.CardRow
import com.learningapp.domain.lesson.Answer
import com.learningapp.domain.lesson.AnswerChecker
import com.learningapp.domain.lesson.Exercise
import com.learningapp.domain.lesson.ExerciseBuilder
import com.learningapp.domain.lesson.LanguageRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Checks every course's lessons so a bad content edit fails the build, not the app. */
class LessonFileTest {

    private val courses: List<String> =
        File("src/main/assets/content").listFiles().orEmpty().filter { File(it, "lessons.json").exists() }.map { it.name }.sorted()

    private fun lessons(lang: String) = LessonParser.parse(File("src/main/assets/content/$lang/lessons.json").readText(Charsets.UTF_8))
    private fun words(lang: String) = ContentParser.parse(File("src/main/assets/content/$lang/words.json").readText(Charsets.UTF_8)).words
    private fun mains(target: String) = SUPPORTED_LANGUAGES.map { it.code }.filter { it != target }.toSet()

    @Test
    fun structure() {
        assertEquals(listOf("en", "es", "fr", "ja", "vi"), courses)
        courses.forEach { lang ->
            val file = lessons(lang)
            val all = file.units.flatMap { it.lessons }
            assertEquals(lang, file.lang)
            assertEquals("$lang units", 10, file.units.size)
            assertEquals("$lang lessons", 20, all.size)
            assertEquals("$lang lesson ids", all.size, all.map { it.id }.toSet().size)
        }
    }

    @Test
    fun everyLessonUsesKnownWordsAndAllExerciseTypes() {
        courses.forEach { lang ->
            val wordIds = words(lang).map { it.id }.toSet()
            lessons(lang).units.flatMap { it.lessons }.forEach { l ->
                assertTrue("${l.id} unknown word", wordIds.containsAll(l.words))
                assertEquals("${l.id} types", 5, l.exercises.map { it::class }.toSet().size)
                l.exercises.filterIsInstance<ContentExercise.Mc>().forEach { assertTrue("${l.id} mc ${it.wordId}", it.wordId in l.words) }
            }
        }
    }

    @Test
    fun allTextIsInEveryMainLanguage() {
        courses.forEach { lang ->
            val m = mains(lang)
            lessons(lang).units.forEach { u ->
                assertEquals("${u.id} title", m, u.title.keys)
                u.lessons.forEach { l ->
                    assertEquals("${l.id} title", m, l.title.keys)
                    l.exercises.forEach { e ->
                        val map = when (e) {
                            is ContentExercise.Fill -> e.hint
                            is ContentExercise.Order -> e.translation
                            is ContentExercise.Listen -> e.translation
                            is ContentExercise.Translate -> e.source
                            is ContentExercise.Mc -> null
                        }
                        if (map != null) assertEquals("${l.id} $e", m, map.keys)
                    }
                }
            }
        }
    }

    @Test
    fun exerciseFieldsAreFilled() {
        courses.forEach { lang ->
            val spaced = LanguageRules.of(lang).spaced
            lessons(lang).units.flatMap { it.lessons }.flatMap { l -> l.exercises.map { l.id to it } }.forEach { (id, e) ->
                when (e) {
                    is ContentExercise.Fill -> {
                        assertEquals("$id fill blank", 1, Regex("___").findAll(e.sentence).count())
                        assertTrue("$id fill", e.answer.isNotBlank())
                    }
                    is ContentExercise.Order -> {
                        val chips = e.chips ?: e.answer.split(" ")
                        assertTrue("$id order size", chips.size in 3..6)
                        if (!spaced) assertEquals("$id chips join", e.answer, chips.joinToString(""))
                    }
                    is ContentExercise.Listen -> assertTrue("$id listen", e.text.isNotBlank())
                    is ContentExercise.Translate -> assertTrue("$id translate", e.answers.isNotEmpty())
                    is ContentExercise.Mc -> Unit
                }
            }
        }
    }

    /** Builds every lesson in every main language and answers each exercise with its own solution. */
    @Test
    fun everyLessonBuildsAndItsOwnAnswersPass() {
        courses.forEach { lang ->
            val all = words(lang)
            mains(lang).forEach { main ->
                lessons(lang).units.forEach { unit ->
                    val unitWordIds = unit.lessons.flatMap { it.words }.toSet()
                    val cards = all.filter { it.id in unitWordIds }.map {
                        CardRow(it.id, it.text, it.example, it.translations.getValue(main).text, "", null, null, null, null, null, it.reading)
                    }
                    val builder = ExerciseBuilder(cards, main, Random(1), lang)
                    unit.lessons.forEach { l ->
                        val built = builder.build(l.exercises)
                        assertEquals("${l.id} all built", l.exercises.size, built.size)
                        built.forEach { ex ->
                            if (ex is Exercise.MultipleChoice) assertEquals("${l.id} $main mc options ${ex.options}", 4, ex.options.size)
                            val answer = when (ex) {
                                is Exercise.MultipleChoice -> Answer.Choice(ex.correctIndex)
                                is Exercise.WordOrder -> Answer.Chips(ex.answer)
                                else -> Answer.Text(ex.solution)
                            }
                            assertTrue("${l.id} ${ex.solution}", AnswerChecker.check(ex, answer, lang).isCorrect)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun entitiesRoundTrip() {
        val file = lessons("es")
        val entities = LessonParser.toEntities(file)
        assertEquals((0 until 20).toList(), entities.map { it.sortOrder })
        val first = entities.first()
        assertEquals(file.units.first().lessons.first().exercises, LessonParser.exercises(first))
        assertEquals("Greetings", LessonParser.text(first.unitTitleJson, "en"))
        // A main language with no text falls back to English.
        assertEquals("Greetings", LessonParser.text("""{"en":"Greetings"}""", "vi"))
    }
}
