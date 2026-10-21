package com.learningapp.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningapp.R
import com.learningapp.data.repo.LessonResult
import com.learningapp.domain.lesson.CheckResult
import com.learningapp.domain.lesson.Exercise
import com.learningapp.ui.components.AppCard
import com.learningapp.ui.components.GradientButton
import com.learningapp.ui.components.MessageCard
import com.learningapp.ui.components.Speaker
import com.learningapp.ui.components.TrackStudyTime
import com.learningapp.ui.components.rememberSpeaker
import com.learningapp.ui.theme.BodyText
import com.learningapp.ui.theme.appColors
import com.learningapp.ui.theme.CardBg
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.Line
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.TagBg
import com.learningapp.ui.theme.Track
import com.learningapp.ui.theme.Violet
import com.learningapp.ui.theme.VioletGradient
import kotlin.math.roundToInt


@Composable
fun LessonScreen(onClose: () -> Unit, viewModel: LessonViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker(state.targetLang)
    TrackStudyTime()
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar(state.progress, onClose)
        when {
            state.loading -> Unit
            state.notFound -> MessageCard("Lesson not found", "This lesson is not available.", "Back", onClose)
            state.result != null -> LessonDone(state.title, state.result!!, onClose)
            state.exercise != null -> ExerciseArea(state, speaker, viewModel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TopBar(progress: Float, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose).semantics { contentDescription = "Close lesson" },
            contentAlignment = Alignment.Center,
        ) { Text("✕", fontSize = 18.sp, color = Muted, fontWeight = FontWeight.Bold) }
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Track)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(VioletGradient))
        }
    }
}

@Composable
private fun ExerciseArea(state: LessonUiState, speaker: Speaker, vm: LessonViewModel, modifier: Modifier) {
    val ex = state.exercise ?: return
    val locked = state.feedback != null
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            (if (state.isRetry) "Try again · " else "") + ex.instruction.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
        )
        when (ex) {
            is Exercise.MultipleChoice -> {
                Text("\"${ex.prompt}\"", style = MaterialTheme.typography.headlineSmall)
                ex.options.forEachIndexed { i, option ->
                    OptionCard(option, selected = state.selected == i, enabled = !locked) { vm.select(i) }
                }
            }
            is Exercise.FillBlank -> {
                Text("${ex.before}____${ex.after}", style = MaterialTheme.typography.headlineSmall)
                Text(ex.hint, style = MaterialTheme.typography.bodyMedium, color = Muted)
                AnswerField(state.text, !locked, "Missing word", vm::type, vm::check)
            }
            is Exercise.WordOrder -> {
                Text(ex.translation, style = MaterialTheme.typography.headlineSmall)
                ChipLine(state.placed, placeholder = "Tap the words below", enabled = !locked, onTap = vm::unplace, answerLine = true)
                ChipLine(state.pool, placeholder = "", enabled = !locked, onTap = vm::place, answerLine = false)
            }
            is Exercise.Listen -> {
                // Play once when the exercise appears.
                LaunchedEffect(ex, speaker.ready) { if (speaker.ready) speaker.speak(ex.text) }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayButton(80, speaker, "Play") { speaker.speak(ex.text) }
                    PlayButton(52, speaker, "Play slowly") { speaker.speak(ex.text, slow = true) }
                    Text("Slow", style = MaterialTheme.typography.labelMedium)
                }
                if (speaker.missingVoice) {
                    Text("No voice for this language on the phone. Add one in Settings › Text-to-speech.", style = MaterialTheme.typography.labelMedium)
                }
                AnswerField(state.text, !locked, "Type what you hear", vm::type, vm::check)
            }
            is Exercise.Translate -> {
                Text(ex.source, style = MaterialTheme.typography.headlineSmall)
                val language = java.util.Locale.forLanguageTag(state.targetLang).getDisplayLanguage(java.util.Locale.ENGLISH)
                AnswerField(state.text, !locked, "In $language", vm::type, vm::check)
            }
        }
    }
    val feedback = state.feedback
    if (feedback != null) {
        FeedbackPanel(feedback, ex)
        GradientButton("Continue", vm::next)
    } else {
        CheckButton(enabled = state.canCheck, onClick = vm::check)
    }
}

@Composable
private fun OptionCard(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) appColors.selectedBg else CardBg)
            .border(2.dp, if (selected) Violet else Color.Transparent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = if (selected) Ink else BodyText, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}

@Composable
private fun AnswerField(text: String, enabled: Boolean, label: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(label) },
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violet, unfocusedBorderColor = Line,
            focusedContainerColor = CardBg, unfocusedContainerColor = CardBg, disabledContainerColor = CardBg,
            disabledTextColor = Ink,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipLine(chips: List<Chip>, placeholder: String, enabled: Boolean, onTap: (Chip) -> Unit, answerLine: Boolean) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (chips.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 10.dp))
        }
        chips.forEach { chip ->
            Text(
                chip.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (answerLine) Color.White else Ink,
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (answerLine) Modifier.background(VioletGradient) else Modifier.background(CardBg).border(1.dp, Line, CircleShape))
                    .clickable(enabled = enabled) { onTap(chip) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
    if (answerLine) Box(Modifier.fillMaxWidth().height(2.dp).background(Line))
}

@Composable
private fun PlayButton(sizeDp: Int, speaker: Speaker, description: String, onClick: () -> Unit) {
    val big = sizeDp > 60
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape)
            .then(if (big) Modifier.background(VioletGradient) else Modifier.background(TagBg))
            .clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_volume), contentDescription = null, tint = if (big) Color.White else if (speaker.ready) Violet else Muted)
    }
}

@Composable
private fun CheckButton(enabled: Boolean, onClick: () -> Unit) {
    if (enabled) {
        GradientButton("Check", onClick)
    } else {
        Box(Modifier.fillMaxWidth().clip(CircleShape).background(Track).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text("CHECK", style = MaterialTheme.typography.labelLarge, color = Muted)
        }
    }
}

@Composable
private fun FeedbackPanel(result: CheckResult, ex: Exercise) {
    val right = result.isCorrect
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (right) appColors.rightBg else appColors.wrongBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(if (right) "Correct!" else "Not quite", style = MaterialTheme.typography.titleMedium, color = if (right) appColors.rightInk else appColors.wrongInk)
        result.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = appColors.rightInk) }
        if (!right) Text("Answer: ${ex.solution}", style = MaterialTheme.typography.bodyMedium, color = appColors.wrongInk)
        if (!right) Text("You'll see this one again at the end.", style = MaterialTheme.typography.labelMedium, color = appColors.wrongInk.copy(alpha = 0.8f))
    }
}

@Composable
private fun LessonDone(title: String, result: LessonResult, onClose: () -> Unit) {
    MessageCard(
        title = "Lesson complete!",
        body = title,
        button = "Continue",
        onClick = onClose,
        extra = {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("+${result.xp}", "XP", Modifier.weight(1f))
                Stat("${(result.accuracy * 100).roundToInt()}%", "Accuracy", Modifier.weight(1f))
            }
            Text("New words added to your flashcards.", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        },
    )
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(TagBg).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
