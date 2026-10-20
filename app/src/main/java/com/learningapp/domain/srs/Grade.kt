package com.learningapp.domain.srs

/** Answer buttons on the review screen. [value] is stored in review_log.grade; [quality] is the SM-2 q. */
enum class Grade(val value: Int, val quality: Int) {
    Again(0, 1),
    Hard(1, 3),
    Good(2, 4),
    Easy(3, 5);

    companion object {
        fun fromValue(value: Int): Grade = entries.first { it.value == value }
    }
}
