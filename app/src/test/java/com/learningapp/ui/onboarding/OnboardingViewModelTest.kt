package com.learningapp.ui.onboarding

import com.learningapp.data.content.ContentCatalog
import com.learningapp.data.content.ContentParser
import com.learningapp.data.repo.OnboardingChoices
import com.learningapp.data.repo.OnboardingStore
import com.learningapp.data.repo.Placement
import com.learningapp.data.repo.PlacementQuestion
import com.learningapp.domain.lesson.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private class FakeStore : OnboardingStore {
        var finished: OnboardingChoices? = null
        override suspend fun needsOnboarding() = true
        override fun pairs() = mapOf("es" to setOf("en"))
        override suspend fun placementQuestions(main: String, target: String) =
            (0 until 6).map { PlacementQuestion(it / 2, Exercise.MultipleChoice("w$it", listOf("a", "b", "c", "d"), correctIndex = 1)) }
        override suspend fun finish(c: OnboardingChoices) { finished = c }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun answer(vm: OnboardingViewModel, rightCount: Int) {
        repeat(6) { i ->
            vm.select(if (i < rightCount) 1 else 0)
            vm.nextQuestion()
        }
    }

    @Test
    fun onlyPairsWithContentCanContinue() {
        val vm = OnboardingViewModel(FakeStore())
        vm.start()
        assertTrue(vm.state.value.pairAvailable) // default en -> es
        vm.pickTarget("fr")
        assertFalse(vm.state.value.pairAvailable)
        vm.confirmLanguages()
        assertEquals(OnboardingStep.Languages, vm.state.value.step)
        vm.pickTarget("en")
        assertFalse(vm.state.value.pairAvailable) // same as main
    }

    @Test
    fun placementPicksStartUnitAndFinishSavesChoices() {
        val store = FakeStore()
        val vm = OnboardingViewModel(store)
        vm.start(); vm.confirmLanguages()
        assertEquals(6, vm.state.value.questions.size)
        answer(vm, rightCount = 4)
        assertEquals(OnboardingStep.Goal, vm.state.value.step)
        assertEquals(1, vm.state.value.startUnitIndex)
        vm.pickGoal(20); vm.setReminder(true); vm.setReminderTime(8 * 60 + 30)
        vm.finish()
        assertEquals(OnboardingChoices("en", "es", 1, 20, true, 510), store.finished)
        assertTrue(vm.state.value.finished)
    }

    @Test
    fun skipStartsAtUnitOne() {
        val vm = OnboardingViewModel(FakeStore())
        vm.start(); vm.confirmLanguages(); vm.skipPlacement()
        assertEquals(0, vm.state.value.startUnitIndex)
        assertEquals(OnboardingStep.Goal, vm.state.value.step)
    }

    @Test
    fun nextNeedsAnAnswer() {
        val vm = OnboardingViewModel(FakeStore())
        vm.start(); vm.confirmLanguages()
        vm.nextQuestion()
        assertEquals(0, vm.state.value.questionIndex)
    }

    @Test
    fun backGoesToPreviousStep() {
        val vm = OnboardingViewModel(FakeStore())
        vm.start(); vm.confirmLanguages(); vm.back()
        assertEquals(OnboardingStep.Languages, vm.state.value.step)
        vm.back()
        assertEquals(OnboardingStep.Welcome, vm.state.value.step)
    }

    @Test
    fun startUnitThresholds() {
        assertEquals(listOf(0, 0, 0, 1, 1, 2, 2), (0..6).map { Placement.startUnit(it) })
    }

    @Test
    fun spanishContentIsLearnableFromEnglish() {
        val file = ContentParser.parse(File("src/main/assets/content/es/words.json").readText(Charsets.UTF_8))
        assertEquals(setOf("en", "fr", "ja", "vi"), ContentCatalog.mainLanguagesOf(file))
    }
}
