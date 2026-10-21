package com.learningapp.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A language the app knows about, with its name in English and in itself. */
data class Language(val code: String, val englishName: String, val nativeName: String)

val SUPPORTED_LANGUAGES = listOf(
    Language("en", "English", "English"),
    Language("es", "Spanish", "Español"),
    Language("fr", "French", "Français"),
    Language("ja", "Japanese", "日本語"),
    Language("vi", "Vietnamese", "Tiếng Việt"),
)

fun languageName(code: String): String = SUPPORTED_LANGUAGES.firstOrNull { it.code == code }?.englishName ?: code

/**
 * Which language pairs have content: a target language needs assets/content/<target>/words.json,
 * and a main language is usable for it when every word has a translation in that language.
 */
@Singleton
class ContentCatalog @Inject constructor(@ApplicationContext private val context: Context) {

    /** target code -> main language codes it can be learned from. Read once. */
    val pairs: Map<String, Set<String>> by lazy { scan() }

    fun isAvailable(main: String, target: String): Boolean = main != target && main in pairs[target].orEmpty()

    private fun scan(): Map<String, Set<String>> {
        val dirs = context.assets.list("content").orEmpty()
        return dirs.mapNotNull { target ->
            val text = runCatching {
                context.assets.open("content/$target/words.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: return@mapNotNull null
            target to mainLanguagesOf(ContentParser.parse(text))
        }.toMap()
    }

    companion object {
        /** Main languages every word in [file] has a translation for. */
        fun mainLanguagesOf(file: ContentFile): Set<String> =
            file.words.map { it.translations.keys }.reduceOrNull { a, b -> a intersect b }.orEmpty()
    }
}
