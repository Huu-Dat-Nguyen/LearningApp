package com.learningapp.domain.lesson

/**
 * How a target language is written, for answer checking and speech scoring.
 * [spaced]: words are separated by spaces (false for Japanese).
 * [accentSlips]: a missing accent counts as right with a note (Spanish, French);
 *   in Vietnamese tone marks change the word, so they must be typed.
 * [keepLetters]: letters that are not "an accent on another letter" (Spanish ñ).
 */
data class LanguageRules(
    val spaced: Boolean = true,
    val accentSlips: Boolean = true,
    val keepLetters: Set<Char> = emptySet(),
) {
    companion object {
        fun of(lang: String): LanguageRules = when (lang) {
            "es" -> LanguageRules(keepLetters = setOf('ñ'))
            "fr" -> LanguageRules()
            "en" -> LanguageRules()
            "vi" -> LanguageRules(accentSlips = false)
            "ja" -> LanguageRules(spaced = false, accentSlips = false)
            else -> LanguageRules()
        }
    }
}
