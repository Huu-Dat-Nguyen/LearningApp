package com.learningapp.ui.speak

import com.learningapp.data.repo.PracticeSentence
import com.learningapp.data.repo.SpeakStore
import com.learningapp.domain.speech.SpeechError
import com.learningapp.domain.speech.SpeechEvent
import com.learningapp.domain.speech.SpeechInput
import com.learningapp.domain.speech.WordMark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeakViewModelTest {

    private class FakeSpeech(override val isAvailable: Boolean = true) : SpeechInput {
        var lang: String? = null
        var listener: ((SpeechEvent) -> Unit)? = null
        var stopped = 0
        var released = false
        override fun start(langTag: String, onEvent: (SpeechEvent) -> Unit) { lang = langTag; listener = onEvent }
        override fun stop() { stopped++ }
        override fun cancel() = Unit
        override fun release() { released = true }
        fun send(e: SpeechEvent) = listener!!.invoke(e)
    }

    private class FakeStore(val list: List<PracticeSentence>) : SpeakStore {
        val attempts = mutableListOf<Pair<String, Int>>()
        override suspend fun targetLang() = "es"
        override suspend fun sentences() = list
        override suspend fun recordAttempt(sentence: String, score: Int): Int {
            attempts += sentence to score
            return if (score >= 80) 2 else if (score >= 50) 1 else 0
        }
    }

    private val sentences = listOf(PracticeSentence("Buenas noches", "Good night"), PracticeSentence("Me gusta el queso", "I like cheese"))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsWithFirstSentence() {
        val vm = SpeakViewModel(FakeStore(sentences), FakeSpeech())
        val s = vm.state.value
        assertEquals("Buenas noches", s.sentence?.text)
        assertEquals(1, s.position)
        assertEquals(2, s.total)
        assertEquals(SpeakPhase.Idle, s.phase)
    }

    @Test
    fun goodAttemptIsScoredAndGivesXp() {
        val speech = FakeSpeech()
        val store = FakeStore(sentences)
        val vm = SpeakViewModel(store, speech)
        vm.micTapped()
        assertEquals("es-ES", speech.lang)
        assertEquals(SpeakPhase.Listening, vm.state.value.phase)
        speech.send(SpeechEvent.Partial("buenas"))
        assertEquals("buenas", vm.state.value.partial)
        speech.send(SpeechEvent.Results(listOf("buenas notches", "buenas noches")))
        val s = vm.state.value
        assertEquals(SpeakPhase.Result, s.phase)
        assertEquals(100, s.score?.score)
        assertEquals(2, s.xpGained)
        assertEquals(listOf("Buenas noches" to 100), store.attempts)
    }

    @Test
    fun tappingMicWhileListeningStops() {
        val speech = FakeSpeech()
        val vm = SpeakViewModel(FakeStore(sentences), speech)
        vm.micTapped(); vm.micTapped()
        assertEquals(1, speech.stopped)
    }

    @Test
    fun partialMatchMarksWords() {
        val speech = FakeSpeech()
        val vm = SpeakViewModel(FakeStore(sentences), speech)
        vm.next()
        vm.micTapped()
        speech.send(SpeechEvent.Results(listOf("me gusta queso")))
        val s = vm.state.value.score!!
        assertEquals(listOf(WordMark.Right, WordMark.Right, WordMark.Missed, WordMark.Right), s.words.map { it.mark })
        assertEquals(75, s.score)
    }

    @Test
    fun errorsAndTryAgain() {
        val speech = FakeSpeech()
        val vm = SpeakViewModel(FakeStore(sentences), speech)
        vm.micTapped()
        speech.send(SpeechEvent.Error(SpeechError.NoMatch))
        assertEquals(SpeakPhase.Error, vm.state.value.phase)
        assertEquals(SpeechError.NoMatch, vm.state.value.error)
        vm.tryAgain()
        assertEquals(SpeakPhase.Idle, vm.state.value.phase)
        assertNull(vm.state.value.error)
    }

    @Test
    fun permissionDeniedShowsError() {
        val vm = SpeakViewModel(FakeStore(sentences), FakeSpeech())
        vm.permissionDenied()
        assertEquals(SpeechError.Permission, vm.state.value.error)
    }

    @Test
    fun nextWrapsAround() {
        val vm = SpeakViewModel(FakeStore(sentences), FakeSpeech())
        vm.next(); vm.next()
        assertEquals(1, vm.state.value.position)
    }

    @Test
    fun noRecognitionMeansListenOnly() {
        val vm = SpeakViewModel(FakeStore(sentences), FakeSpeech(isAvailable = false))
        assertFalse(vm.state.value.recognitionAvailable)
        assertTrue(vm.state.value.sentence != null)
    }
}
