package com.learningapp.ui.review

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Android TextToSpeech for one language. [missingVoice] is true when the phone has no voice for it. */
@Stable
class Speaker {
    internal var tts: TextToSpeech? = null
    var ready by mutableStateOf(false)
        internal set
    var missingVoice by mutableStateOf(false)
        internal set

    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }
}

@Composable
fun rememberSpeaker(langTag: String): Speaker {
    val context = LocalContext.current
    val speaker = remember(langTag) { Speaker() }
    DisposableEffect(langTag) {
        if (langTag.isNotEmpty()) {
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                val result = if (status == TextToSpeech.SUCCESS) {
                    engine?.setLanguage(Locale.forLanguageTag(langTag))
                } else {
                    TextToSpeech.LANG_NOT_SUPPORTED
                }
                val ok = result != null && result >= TextToSpeech.LANG_AVAILABLE
                speaker.ready = ok
                speaker.missingVoice = !ok
            }
            speaker.tts = engine
        }
        onDispose {
            speaker.tts?.shutdown()
            speaker.tts = null
            speaker.ready = false
        }
    }
    return speaker
}
