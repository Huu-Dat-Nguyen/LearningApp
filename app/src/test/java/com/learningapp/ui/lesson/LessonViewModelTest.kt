package com.learningapp.ui.lesson

import com.learningapp.data.content.ContentExercise
import com.learningapp.data.db.CardRow
import com.learningapp.data.repo.LessonData
import com.learningapp.data.repo.LessonResult
import com.learningapp.data.repo.LessonStore
import com.learningapp.domain.lesson.Exercise
import com.learningapp.domain.lesson.Verdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {

    private class FakeStore(private val data: LessonData?) : LessonStore {
        var completed: Pair<LessonResult, List<String>>? = null
        override suspend fun lesson(id: String) = data
        override suspend fun completeLesson(result: LessonResult, lessonWordIds: List<String>) {
            completed = result to lessonWordIds
        }
    }

    private fun card(id: String, text: String, tr: String) = CardRow(id, text, "", tr, "", null, null, null, null, null)

    private val words = listOf(
        card("es_hola", "hola", "hello"), card("es_adios", "adiós", "goodbye"),
        card("es_gracias", "gracias", "thank you"), card("es_perdon", "perdón", "sorry"),
    )

    private fun lesson(vararg exercises: ContentExercise) =
        LessonData("l1", "Hello & goodbye", "u1", "es", "en", exercises.toList(), listOf("es_hola", "es_adios"), words)

    private fun vm(store: FakeStore) = LessonViewModel("l1", store, Random(7))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun missingLessonShowsNotFound() {
        val vm = vm(FakeStore(null))
        assertTrue(vm.state.value.notFound)
    }

    @Test
    fun multipleChoiceRightAnswerFinishesAndSaves() {
        val store = FakeStore(lesson(ContentExercise.Mc("es_hola")))
        val vm = vm(store)
        val mc = vm.state.value.exercise as Exercise.MultipleChoice
        assertFalse(vm.state.value.canCheck)
        vm.select(mc.correctIndex)
        assertTrue(vm.state.value.canCheck)
        vm.check()
        assertEquals(Verdict.Correct, vm.state.value.feedback?.verdict)
        vm.next()
        val result = vm.state.value.result
        assertNotNull(result)
        assertEquals(1.0, result!!.accuracy, 1e-9)
        assertEquals(11, result.xp)
        assertEquals(listOf("es_hola", "es_adios"), store.completed?.second)
    }

    @Test
    fun wrongAnswerIsRetriedAtTheEnd() {
        val store = FakeStore(
            lesson(
                ContentExercise.Translate(mapOf("en" to "Goodbye!"), listOf("Adiós")),
                ContentExercise.Listen("hola", mapOf("en" to "hello")),
            )
        )
        val vm = vm(store)
        vm.type("hola"); vm.check()
        assertEquals(Verdict.Wrong, vm.state.value.feedback?.verdict)
        vm.next()
        vm.type("Hola"); vm.check(); vm.next()
        assertTrue(vm.state.value.isRetry)
        assertTrue(vm.state.value.exercise is Exercise.Translate)
        assertEquals("", vm.state.value.text) // answer cleared for the retry
        vm.type("adios"); vm.check()
        assertEquals(Verdict.AccentSlip, vm.state.value.feedback?.verdict)
        vm.next()
        val result = vm.state.value.result!!
        assertEquals(0.5, result.accuracy, 1e-9)
        assertEquals(3, result.answered)
        assertEquals(2, result.correct)
        assertNotNull(store.completed)
    }

    @Test
    fun wordOrderChipsMoveBothWays() {
        val vm = vm(FakeStore(lesson(ContentExercise.Order("Quiero pan con queso", mapOf("en" to "I want bread with cheese")))))
        val ex = vm.state.value.exercise as Exercise.WordOrder
        assertEquals(4, vm.state.value.pool.size)
        // Place chips in the right order by text.
        ex.answer.forEach { word -> vm.place(vm.state.value.pool.first { it.text == word }) }
        assertTrue(vm.state.value.pool.isEmpty())
        val last = vm.state.value.placed.last()
        vm.unplace(last)
        assertEquals(1, vm.state.value.pool.size)
        vm.place(last)
        vm.check()
        assertEquals(Verdict.Correct, vm.state.value.feedback?.verdict)
    }

    @Test
    fun answerCannotChangeWhileFeedbackShows() {
        val vm = vm(FakeStore(lesson(ContentExercise.Listen("hola", mapOf("en" to "hello")))))
        vm.type("hola"); vm.check()
        vm.type("changed")
        assertEquals("hola", vm.state.value.text)
        vm.check() // second check does nothing
        assertEquals(Verdict.Correct, vm.state.value.feedback?.verdict)
    }

    @Test
    fun nextDoesNothingBeforeCheck() {
        val vm = vm(FakeStore(lesson(ContentExercise.Listen("hola", mapOf("en" to "hello")))))
        vm.next()
        assertNull(vm.state.value.result)
        assertNotNull(vm.state.value.exercise)
    }
}
