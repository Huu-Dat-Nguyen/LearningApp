package com.learningapp.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the bundled word lists so a bad content edit fails the build, not the app. */
class ContentFileTest {

    private fun load(lang: String) =
        ContentParser.parse(File("src/main/assets/content/$lang/words.json").readText(Charsets.UTF_8))

    @Test
    fun spanishListParsesWithUniqueIds() {
        val file = load("es")
        assertEquals("es", file.lang)
        assertEquals(100, file.words.size)
        assertEquals(file.words.size, file.words.map { it.id }.toSet().size)
    }

    @Test
    fun everyWordHasEnglishTranslationAndExample() {
        load("es").words.forEach { w ->
            val en = w.translations["en"]
            assertTrue("${w.id} missing en", en != null && en.text.isNotBlank() && en.example.isNotBlank())
            assertTrue("${w.id} missing example", w.example.isNotBlank())
            assertTrue("${w.id} id prefix", w.id.startsWith("es_"))
        }
    }

    @Test
    fun entitiesKeepContentOrder() {
        val (words, translations) = ContentParser.toEntities(load("es"))
        assertEquals((0 until 100).toList(), words.map { it.sortOrder })
        assertEquals(100, translations.size)
    }
}
