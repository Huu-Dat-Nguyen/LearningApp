package com.learningapp.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningapp.data.content.SUPPORTED_LANGUAGES
import com.learningapp.data.repo.UserPrefs
import com.learningapp.ui.components.AppCard
import com.learningapp.ui.components.GradientButton
import com.learningapp.ui.components.OutlineButton
import com.learningapp.ui.components.rememberNotificationPermission
import com.learningapp.ui.theme.BodyText
import com.learningapp.ui.theme.CardBg
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.TagBg
import com.learningapp.ui.theme.Track
import com.learningapp.ui.theme.Violet
import com.learningapp.ui.theme.VioletGradient
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.finished) { if (s.finished) onFinished() }
    BackHandler(enabled = s.step != OnboardingStep.Welcome) { viewModel.back() }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (s.step) {
            OnboardingStep.Welcome -> Welcome(viewModel::start)
            OnboardingStep.Languages -> Languages(s, viewModel)
            OnboardingStep.Placement -> PlacementQuiz(s, viewModel)
            OnboardingStep.Goal -> Goal(s, viewModel)
        }
    }
}

@Composable
private fun ColumnScope.Welcome(onStart: () -> Unit) {
    Spacer(Modifier.weight(1f))
    Box(
        Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp)).background(TagBg),
        contentAlignment = Alignment.Center,
    ) { Text("¡Hola!  Bonjour  こんにちは  Xin chào", style = MaterialTheme.typography.titleMedium, color = Violet, textAlign = TextAlign.Center) }
    Text("LearningApp", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(
        "Learn a language in a few minutes a day. Free, no ads.",
        style = MaterialTheme.typography.bodyLarge, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.weight(1f))
    GradientButton("Get started", onStart)
}

@Composable
private fun ColumnScope.Languages(s: OnboardingUiState, vm: OnboardingViewModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Choose languages", style = MaterialTheme.typography.titleLarge)
        SectionLabel("YOUR MAIN LANGUAGE")
        SUPPORTED_LANGUAGES.forEach { lang ->
            // A main language is usable if some target language has content for it.
            val usable = s.pairs.values.any { lang.code in it }
            LanguageOption(lang.englishName, lang.nativeName, s.mainLang == lang.code, enabled = usable) { vm.pickMain(lang.code) }
        }
        SectionLabel("LANGUAGE TO LEARN")
        SUPPORTED_LANGUAGES.filter { it.code != s.mainLang }.forEach { lang ->
            val usable = s.mainLang in s.pairs[lang.code].orEmpty()
            LanguageOption(lang.englishName, lang.nativeName, s.targetLang == lang.code, enabled = usable) { vm.pickTarget(lang.code) }
        }
    }
    if (s.pairAvailable) GradientButton("Continue", vm::confirmLanguages)
    else DisabledButton(if (s.mainLang == s.targetLang) "Pick two different languages" else "Pick a language to learn")
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp), modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun LanguageOption(name: String, native: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0xFFF6EEFE) else if (enabled) CardBg else Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) Violet else if (enabled) Color.Transparent else Track, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (enabled) Ink else Muted)
            if (native != name) Text(native, style = MaterialTheme.typography.labelMedium)
        }
        if (!enabled) Text("COMING SOON", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun ColumnScope.PlacementQuiz(s: OnboardingUiState, vm: OnboardingViewModel) {
    val total = s.questions.size.coerceAtLeast(1)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Track)) {
            Box(Modifier.fillMaxWidth((s.questionIndex.toFloat() / total).coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(VioletGradient))
        }
        Text("${(s.questionIndex + 1).coerceAtMost(total)} / $total", style = MaterialTheme.typography.labelMedium)
    }
    Text("Quick check", style = MaterialTheme.typography.titleLarge)
    Text("A few questions to find your starting point.", style = MaterialTheme.typography.bodyMedium, color = BodyText)
    val q = s.question
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (q != null) {
            Text("What does \"${q.exercise.prompt}\" mean?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
            q.exercise.options.forEachIndexed { i, option ->
                LanguageOption(option, option, s.selected == i, enabled = true) { vm.select(i) }
            }
        } else if (!s.loadingQuestions) {
            Text("No quiz for this language yet.", style = MaterialTheme.typography.bodyMedium)
        }
    }
    OutlineButton("I'm a beginner, skip", vm::skipPlacement)
    if (s.selected != null) GradientButton(if (s.questionIndex + 1 >= s.questions.size) "Finish quiz" else "Next", vm::nextQuestion)
    else DisabledButton("Next")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.Goal(s: OnboardingUiState, vm: OnboardingViewModel) {
    var picking by remember { mutableStateOf(false) }
    // Reminder on: ask for notification permission first; if refused, finish with the reminder off.
    val askNotifications = rememberNotificationPermission { granted ->
        if (!granted) vm.setReminder(false)
        vm.finish()
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Daily goal", style = MaterialTheme.typography.titleLarge)
        if (s.startUnitIndex > 0) {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Nice! You'll start at unit ${s.startUnitIndex + 1}.", style = MaterialTheme.typography.bodyLarge, color = Ink)
                Text("Earlier lessons are unlocked, and their words are in your flashcards.", style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("How much time a day?", style = MaterialTheme.typography.bodyMedium, color = BodyText)
        UserPrefs.GOALS.forEach { minutes ->
            val label = when (minutes) { 5 -> "Casual"; 10 -> "Regular"; else -> "Serious" }
            LanguageOption("$minutes min a day", label, s.goalMinutes == minutes, enabled = true) { vm.pickGoal(minutes) }
        }
        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminder", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Text(if (s.reminderEnabled) "At ${formatTime(s.reminderMinuteOfDay)}" else "Off", style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = s.reminderEnabled,
                    onCheckedChange = vm::setReminder,
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet),
                )
            }
            if (s.reminderEnabled) {
                Text(
                    "Change time",
                    style = MaterialTheme.typography.labelLarge,
                    color = Violet,
                    modifier = Modifier.clip(CircleShape).clickable { picking = true }.padding(vertical = 6.dp),
                )
            }
        }
    }
    GradientButton(if (s.finishing) "Setting up…" else "Start learning", { if (s.reminderEnabled) askNotifications() else vm.finish() })

    if (picking) {
        val state = rememberTimePickerState(s.reminderMinuteOfDay / 60, s.reminderMinuteOfDay % 60)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton({ vm.setReminderTime(state.hour * 60 + state.minute); picking = false }) { Text("OK") } },
            dismissButton = { TextButton({ picking = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
internal fun DisabledButton(text: String) {
    Box(Modifier.fillMaxWidth().clip(CircleShape).background(Track).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = Muted)
    }
}

/** "7:00 PM" or "19:00", following the phone's locale. */
fun formatTime(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
