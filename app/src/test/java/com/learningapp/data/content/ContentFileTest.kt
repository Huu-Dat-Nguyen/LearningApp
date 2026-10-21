package com.learningapp.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks every bundled word list so a bad content edit fails the build, not the app. */
class ContentFileTest {

    /** Every course folder under assets/content. */
    private val courses: List<String> =
        File("src/main/assets/content").listFiles().orEmpty().filter { File(it, "words.json").exists() }.map { it.name }.sorted()

    private fun load(lang: String) =
        ContentParser.parse(File("src/main/assets/content/$lang/words.json").readText(Charsets.UTF_8))

    private fun mains(target: String) = SUPPORTED_LANGUAGES.map { it.code }.filter { it != target }.toSet()

    @Test
    fun allFiveCoursesExist() {
        assertEquals(listOf("en", "es", "fr", "ja", "vi"), courses)
    }

    @Test
    fun eachCourseHas100UniqueWords() {
        courses.forEach { lang ->
            val file = load(lang)
            assertEquals(lang, file.lang)
            assertEquals("$lang words", 100, file.words.size)
            assertEquals("$lang ids", file.words.size, file.words.map { it.id }.toSet().size)
            assertTrue("$lang id prefix", file.words.all { it.id.startsWith("${lang}_") })
        }
    }

    @Test
    fun everyWordHasMeaningsInAllFourMainLanguages() {
        courses.forEach { lang ->
            load(lang).words.forEach { w ->
                assertEquals("${w.id} main languages", mains(lang), w.translations.keys)
                w.translations.forEach { (m, t) ->
                    assertTrue("${w.id} $m blank", t.text.isNotBlank() && t.example.isNotBlank())
                }
                assertTrue("${w.id} missing example", w.example.isNotBlank())
            }
            assertEquals("$lang learnable from", mains(lang), ContentCatalog.mainLanguagesOf(load(lang)))
        }
    }

    @Test
    fun japaneseWordsHaveReadings() {
        load("ja").words.forEach { assertTrue("${it.id} reading", !it.reading.isNullOrBlank()) }
    }

    @Test
    fun entitiesKeepContentOrder() {
        courses.forEach { lang ->
            val (words, translations) = ContentParser.toEntities(load(lang))
            assertEquals((0 until 100).toList(), words.map { it.sortOrder })
            assertEquals(400, translations.size)
        }
    }
}
