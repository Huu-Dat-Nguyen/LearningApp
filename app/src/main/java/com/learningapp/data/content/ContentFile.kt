package com.learningapp.data.content

import com.learningapp.data.db.WordEntity
import com.learningapp.data.db.WordTranslationEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shape of assets/content/<lang>/words.json. */
@Serializable
data class ContentFile(val lang: String, val version: Int, val words: List<ContentWord>)

@Serializable
data class ContentWord(
    val id: String,
    val text: String,
    val topic: String,
    val level: String,
    val example: String,
    /** Keyed by main (native) language code. */
    val translations: Map<String, ContentTranslation>,
    /** Japanese only: "みず (mizu)". */
    val reading: String? = null,
)

@Serializable
data class ContentTranslation(val text: String, val example: String)

object ContentParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): ContentFile = json.decodeFromString(text)

    fun toEntities(file: ContentFile): Pair<List<WordEntity>, List<WordTranslationEntity>> {
        val words = file.words.mapIndexed { i, w ->
            WordEntity(w.id, file.lang, w.text, w.example, w.topic, w.level, sortOrder = i, reading = w.reading)
        }
        val translations = file.words.flatMap { w ->
            w.translations.map { (lang, t) -> WordTranslationEntity(w.id, lang, t.text, t.example) }
        }
        return words to translations
    }
}
