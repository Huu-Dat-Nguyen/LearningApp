package com.learningapp.data.repo

import com.learningapp.data.db.CardRow
import com.learningapp.data.db.UserWordEntity

/** What the review screen needs from storage. [WordRepository] implements it; tests use a fake. */
interface ReviewRepository {
    suspend fun targetLang(): String
    suspend fun dueCards(): List<CardRow>
    suspend fun newCards(limit: Int): List<CardRow>
    suspend fun newCardsLeftToday(dailyLimit: Int): Int
    suspend fun saveReview(newState: UserWordEntity, grade: Int, previousIntervalDays: Int)
}
