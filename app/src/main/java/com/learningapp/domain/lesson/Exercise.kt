package com.learningapp.domain.lesson

/** One exercise ready to show. Built from lesson content by [ExerciseBuilder]. */
sealed interface Exercise {
    /** The right answer, shown in the feedback panel. */
    val solution: String

    /** Show a target-language word, pick its translation. */
    data class MultipleChoice(
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int,
        /** How to read [prompt] (Japanese: "みず (mizu)"), shown under it. */
        val reading: String? = null,
    ) : Exercise {
        override val solution: String get() = options[correctIndex]
    }

    /** Type the missing word; [before]/[after] are the sentence parts around the blank. */
    data class FillBlank(
        val before: String,
        val after: String,
        val answer: String,
        val hint: String,
        /** Other accepted spellings (e.g. kana for a kanji answer). */
        val accept: List<String> = emptyList(),
    ) : Exercise {
        override val solution: String get() = answer
    }

    /** Tap the word chips into the right order. */
    data class WordOrder(
        val chips: List<String>,
        val answer: List<String>,
        val translation: String,
        /** How chips join into a sentence: a space, or nothing for Japanese. */
        val joiner: String = " ",
    ) : Exercise {
        override val solution: String get() = answer.joinToString(joiner)
    }

    /** Listen (text-to-speech) and type what you hear. */
    data class Listen(
        val text: String,
        val translation: String,
        /** Other accepted spellings (e.g. kana for kanji). */
        val accept: List<String> = emptyList(),
    ) : Exercise {
        override val solution: String get() = text
    }

    /** Translate from the main language into the target language. */
    data class Translate(
        val source: String,
        val answers: List<String>,
    ) : Exercise {
        override val solution: String get() = answers.first()
    }
}

/** What the user gave: an option index, typed text, or chips in order. */
sealed interface Answer {
    data class Choice(val index: Int) : Answer
    data class Text(val text: String) : Answer
    data class Chips(val chips: List<String>) : Answer
}
