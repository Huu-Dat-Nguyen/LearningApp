package com.learningapp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A word in the language being learned. Loaded from assets/content/<lang>/words.json. */
@Entity(tableName = "words", indices = [Index("lang")])
data class WordEntity(
    @PrimaryKey val id: String,
    val lang: String,
    val text: String,
    val example: String,
    val topic: String,
    val level: String,
    /** Position in the content file; new cards are introduced in this order. */
    val sortOrder: Int,
)

/** Translation of a word into one main (native) language. */
@Entity(
    tableName = "word_translations",
    primaryKeys = ["wordId", "lang"],
    foreignKeys = [ForeignKey(WordEntity::class, ["id"], ["wordId"], onDelete = ForeignKey.CASCADE)],
)
data class WordTranslationEntity(
    val wordId: String,
    val lang: String,
    val text: String,
    val example: String,
)

/** Spaced-repetition state for a word the user has started. No row = new card. */
@Entity(
    tableName = "user_words",
    foreignKeys = [ForeignKey(WordEntity::class, ["id"], ["wordId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["targetLang", "dueEpochDay"])],
)
data class UserWordEntity(
    @PrimaryKey val wordId: String,
    val targetLang: String,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    /** Next review date as LocalDate.toEpochDay(). */
    val dueEpochDay: Long,
    val lastReviewedAtMs: Long,
)

/** One review answer, kept for stats and debugging. */
@Entity(tableName = "review_log", indices = [Index("wordId")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: String,
    /** 0 = Again, 1 = Hard, 2 = Good, 3 = Easy. */
    val grade: Int,
    val reviewedAtMs: Long,
    val previousIntervalDays: Int,
    val newIntervalDays: Int,
)

/** A card ready to show: word + translation in the main language + review state (null when new). */
data class CardRow(
    val wordId: String,
    val text: String,
    val example: String,
    val translation: String,
    val translationExample: String,
    val easeFactor: Double?,
    val intervalDays: Int?,
    val repetitions: Int?,
    val lapses: Int?,
    val dueEpochDay: Long?,
) {
    val isNew: Boolean get() = dueEpochDay == null
}
