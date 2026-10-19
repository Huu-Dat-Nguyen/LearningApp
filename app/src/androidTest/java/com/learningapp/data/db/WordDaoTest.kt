package com.learningapp.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: WordDao
    private val today = 20_000L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.wordDao()
        val words = (0 until 5).map { WordEntity("es_w$it", "es", "w$it", "ej$it", "food", "A1", it) }
        val tr = words.map { WordTranslationEntity(it.id, "en", "t-${it.text}", "ex-${it.text}") }
        dao.upsertContent(words, tr)
    }

    @After
    fun tearDown() = db.close()

    private fun state(id: String, due: Long, interval: Int = 1) =
        UserWordEntity(id, "es", intervalDays = interval, repetitions = 1, dueEpochDay = due, lastReviewedAtMs = 0)

    @Test
    fun newCardsAreUnreviewedWordsInOrder() = runBlocking {
        dao.upsertUserWord(state("es_w1", today + 3))
        val new = dao.newCards("es", "en", 10)
        assertEquals(listOf("es_w0", "es_w2", "es_w3", "es_w4"), new.map { it.wordId })
        assertTrue(new.all { it.isNew })
        assertEquals("t-w0", new.first().translation)
    }

    @Test
    fun dueCardsOnlyIncludeTodayOrEarlier() = runBlocking {
        dao.upsertUserWord(state("es_w0", today))
        dao.upsertUserWord(state("es_w1", today - 2))
        dao.upsertUserWord(state("es_w2", today + 1))
        val due = dao.dueCards("es", "en", today, 10)
        assertEquals(listOf("es_w1", "es_w0"), due.map { it.wordId }) // oldest first
        assertEquals(2, dao.dueCount("es", today).first())
    }

    @Test
    fun saveReviewWritesStateAndLog() = runBlocking {
        assertNull(dao.userWord("es_w0"))
        dao.saveReview(state("es_w0", today + 6, interval = 6), ReviewLogEntity(wordId = "es_w0", grade = 2, reviewedAtMs = 1000, previousIntervalDays = 0, newIntervalDays = 6))
        assertEquals(6, dao.userWord("es_w0")?.intervalDays)
        assertEquals(1, dao.newStartedBetween("es", 0, 2000))
        assertEquals(0, dao.newStartedBetween("es", 2000, 3000))
    }

    @Test
    fun contentReimportKeepsProgress() = runBlocking {
        dao.upsertUserWord(state("es_w0", today + 6, interval = 6))
        dao.upsertContent(listOf(WordEntity("es_w0", "es", "w0-new", "ej0", "food", "A1", 0)), emptyList())
        assertEquals(6, dao.userWord("es_w0")?.intervalDays)
    }
}
