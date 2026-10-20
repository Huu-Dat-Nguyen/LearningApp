package com.learningapp.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonDaoTest {
    private lateinit var db: AppDatabase

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun lesson(id: String, unit: String, order: Int) =
        LessonEntity(id, "es", unit, """{"en":"Unit"}""", "food", "A1", """{"en":"$id"}""", order, "[]", "[]")

    @Test
    fun lessonsJoinProgress() = runBlocking {
        val dao = db.lessonDao()
        dao.upsertLessons(listOf(lesson("l2", "u1", 1), lesson("l1", "u1", 0)))
        dao.upsertProgress(LessonProgressEntity("l1", 1, 1, 0.8, 1, 15))
        val rows = dao.lessons("es").first()
        assertEquals(listOf("l1", "l2"), rows.map { it.id })
        assertEquals(listOf(true, false), rows.map { it.completed })
        assertEquals(0.8, rows.first().bestAccuracy!!, 1e-9)
    }

    @Test
    fun activityAddsUp() = runBlocking {
        val dao = db.activityDao()
        dao.add(100, "es", xp = 10, seconds = 60, answered = 5, correct = 4)
        dao.add(100, "es", xp = 3, seconds = 30, answered = 3, correct = 3)
        dao.add(99, "es", xp = 0, seconds = 20)
        val day = dao.day(100, "es")!!
        assertEquals(13, day.xp)
        assertEquals(90, day.activeSeconds)
        assertEquals(7, day.correct)
        assertEquals(13, dao.totalXp("es").first())
        assertEquals(listOf(100L), dao.activeDays("es").first()) // day 99 had no XP
    }

    @Test
    fun migrationFrom1KeepsReviewProgress() {
        helper.createDatabase("migration-test", 1).use { v1 ->
            v1.execSQL("INSERT INTO words (id, lang, text, example, topic, level, sortOrder) VALUES ('es_hola','es','hola','¡Hola!','greetings','A1',0)")
            v1.execSQL("INSERT INTO user_words (wordId, targetLang, easeFactor, intervalDays, repetitions, lapses, dueEpochDay, lastReviewedAtMs) VALUES ('es_hola','es',2.5,6,2,0,100,0)")
        }
        helper.runMigrationsAndValidate("migration-test", 2, true).use { v2 ->
            v2.query("SELECT intervalDays FROM user_words WHERE wordId = 'es_hola'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(6, c.getInt(0))
            }
            v2.query("SELECT COUNT(*) FROM lessons").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }
}
