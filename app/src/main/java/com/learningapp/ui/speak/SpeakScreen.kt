package com.learningapp.ui.speak

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningapp.R
import com.learningapp.domain.speech.SpeechError
import com.learningapp.domain.speech.SpeechScore
import com.learningapp.domain.speech.WordMark
import com.learningapp.ui.components.AppCard
import com.learningapp.ui.components.GradientButton
import com.learningapp.ui.components.OutlineButton
import com.learningapp.ui.components.Speaker
import com.learningapp.ui.components.TrackStudyTime
import com.learningapp.ui.components.rememberSpeaker
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.appColors
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.TagBg
import com.learningapp.ui.theme.Violet
import com.learningapp.ui.theme.VioletGradient


@Composable
fun SpeakScreen(viewModel: SpeakViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker(state.targetLang)
    val context = LocalContext.current
    TrackStudyTime()

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.micTapped() else viewModel.permissionDenied()
    }
    fun onMic() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.micTapped() else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Speak", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (state.total > 0) Text("${state.position} / ${state.total}", style = MaterialTheme.typography.labelMedium)
        }
        val sentence = state.sentence
        if (state.loading) return@Column
        if (sentence == null) {
            AppCard(Modifier.fillMaxWidth()) { Text("Nothing to practise yet. Start a lesson first.", style = MaterialTheme.typography.bodyMedium) }
            return@Column
        }

        AppCard(Modifier.fillMaxWidth()) {
            Text("SAY THIS SENTENCE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            Text(sentence.text, style = MaterialTheme.typography.headlineSmall)
            Text(sentence.translation, style = MaterialTheme.typography.bodyMedium, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                SmallSpeaker(speaker, "Listen") { speaker.speak(sentence.text) }
                SmallSpeaker(speaker, "Listen slowly") { speaker.speak(sentence.text, slow = true) }
                Text("Listen · slow", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (state.recognitionAvailable) {
            MicButton(state.phase == SpeakPhase.Listening, state.level, ::onMic)
            Text(
                when (state.phase) {
                    SpeakPhase.Listening -> "Listening… tap to stop"
                    else -> "Tap the mic and read the sentence aloud"
                },
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AppCard(Modifier.fillMaxWidth()) {
                Text(
                    "This phone has no speech recognition, so you can listen and repeat, but the app can't score you.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.phase == SpeakPhase.Listening && state.partial.isNotBlank()) {
            Text("\"${state.partial}\"", style = MaterialTheme.typography.bodyLarge, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        state.score?.let { ResultCard(it, state.xpGained) }
        state.error?.let { ErrorCard(it, onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }) }

        if (state.phase == SpeakPhase.Result || state.phase == SpeakPhase.Error) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton("Try again", viewModel::tryAgain, Modifier.weight(1f))
                GradientButton("Next", viewModel::next, Modifier.weight(1f))
            }
        } else if (state.phase == SpeakPhase.Idle) {
            OutlineButton("Skip sentence", viewModel::next)
        }
        Text(
            "The score shows how well the phone understood you, not a full accent check.",
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SmallSpeaker(speaker: Speaker, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(TagBg).clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_volume), contentDescription = null, tint = if (speaker.ready) Violet else Muted)
    }
}

@Composable
private fun MicButton(listening: Boolean, level: Float, onClick: () -> Unit) {
    val pulse by animateFloatAsState(if (listening) 1f + level * 0.25f else 1f, label = "mic")
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        if (listening) {
            Box(Modifier.size(132.dp).scale(pulse).clip(CircleShape).background(appColors.recording.copy(alpha = 0.15f)))
        }
        Box(
            Modifier
                .size(110.dp)
                .clip(CircleShape)
                .then(if (listening) Modifier.background(appColors.recording) else Modifier.background(VioletGradient))
                .clickable(onClick = onClick)
                .semantics { contentDescription = if (listening) "Stop recording" else "Start recording" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(score: SpeechScore, xp: Int) {
    AppCard(Modifier.fillMaxWidth()) {
        Text("WHAT YOU SAID", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Text("\"${score.heard}\"", style = MaterialTheme.typography.bodyLarge, color = Ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            score.words.forEach { w ->
                Text(
                    w.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = when (w.mark) { WordMark.Right -> appColors.rightInk; WordMark.Close -> appColors.closeInk; WordMark.Missed -> appColors.wrongInk },
                )
            }
        }
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${score.score}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(" / 100 · ${score.label}", style = MaterialTheme.typography.bodyLarge, color = Muted, modifier = Modifier.padding(bottom = 5.dp))
            Spacer(Modifier.weight(1f))
            if (xp > 0) Text("+$xp XP", style = MaterialTheme.typography.labelLarge, color = Violet)
        }
        Text("Green = right · amber = close · red = missed", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun ErrorCard(error: SpeechError, onOpenSettings: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Text(
            when (error) {
                SpeechError.NoMatch -> "Didn't catch that. Try again a little closer to the phone."
                SpeechError.NoSpeech -> "No speech heard. Tap the mic and start speaking."
                SpeechError.Network -> "Speech recognition needs a connection, or download the Spanish offline speech pack in the phone's settings."
                SpeechError.Busy -> "The speech service is busy. Try again in a moment."
                SpeechError.Permission -> "The app needs the microphone to hear you. You can allow it in the app's settings."
                SpeechError.NotAvailable -> "Speech recognition for this language isn't available on this phone. Try adding Spanish in the phone's speech or voice-typing settings."
                SpeechError.Other -> "Something went wrong with speech recognition. Try again."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (error == SpeechError.Permission) {
            Text(
                "Open settings",
                style = MaterialTheme.typography.labelLarge,
                color = Violet,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenSettings).padding(vertical = 6.dp),
            )
        }
    }
}
