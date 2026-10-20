package com.learningapp.domain.lesson

/** One exercise ready to show. Built from lesson content by [ExerciseBuilder]. */
sealed interface Exercise {
    /** Short instruction shown above the exercise. */
    val instruction: String

    /** The right answer, shown in the feedback panel. */
    val solution: String

    /** Show a target-language word, pick its translation. */
    data class MultipleChoice(
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int,
        override val instruction: String = "Choose the correct translation",
    ) : Exercise {
        override val solution: String get() = options[correctIndex]
    }

    /** Type the missing word; [before]/[after] are the sentence parts around the blank. */
    data class FillBlank(
        val before: String,
        val after: String,
        val answer: String,
        val hint: String,
        override val instruction: String = "Fill in the blank",
    ) : Exercise {
        override val solution: String get() = answer
    }

    /** Tap the word chips into the right order. */
    data class WordOrder(
        val chips: List<String>,
        val answer: List<String>,
        val translation: String,
        override val instruction: String = "Put the words in order",
    ) : Exercise {
        override val solution: String get() = answer.joinToString(" ")
    }

    /** Listen (text-to-speech) and type what you hear. */
    data class Listen(
        val text: String,
        val translation: String,
        override val instruction: String = "Type what you hear",
    ) : Exercise {
        override val solution: String get() = text
    }

    /** Translate from the main language into the target language. */
    data class Translate(
        val source: String,
        val answers: List<String>,
        override val instruction: String = "Translate this sentence",
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
