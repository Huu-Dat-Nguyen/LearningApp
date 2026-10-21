package com.learningapp.ui.onboarding

import com.learningapp.data.content.languageNameRes
import com.learningapp.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.learningapp.ui.theme.appColors
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
    ) { Text(stringResource(R.string.onboarding_greetings), style = MaterialTheme.typography.titleMedium, color = Violet, textAlign = TextAlign.Center) }
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(
        stringResource(R.string.onboarding_tagline),
        style = MaterialTheme.typography.bodyLarge, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.weight(1f))
    GradientButton(stringResource(R.string.onboarding_get_started), onStart)
}

@Composable
private fun ColumnScope.Languages(s: OnboardingUiState, vm: OnboardingViewModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.onboarding_choose_languages), style = MaterialTheme.typography.titleLarge)
        SectionLabel(stringResource(R.string.onboarding_your_main_language))
        SUPPORTED_LANGUAGES.forEach { lang ->
            // A main language is usable if some target language has content for it.
            val usable = s.pairs.values.any { lang.code in it }
            LanguageOption(stringResource(languageNameRes(lang.code)), lang.nativeName, s.mainLang == lang.code, enabled = usable) { vm.pickMain(lang.code) }
        }
        SectionLabel(stringResource(R.string.onboarding_language_to_learn))
        SUPPORTED_LANGUAGES.filter { it.code != s.mainLang }.forEach { lang ->
            val usable = s.mainLang in s.pairs[lang.code].orEmpty()
            LanguageOption(stringResource(languageNameRes(lang.code)), lang.nativeName, s.targetLang == lang.code, enabled = usable) { vm.pickTarget(lang.code) }
        }
    }
    if (s.pairAvailable) GradientButton(stringResource(R.string.common_continue), vm::confirmLanguages)
    else DisabledButton(stringResource(if (s.mainLang == s.targetLang) R.string.onboarding_pick_two_different else R.string.onboarding_pick_language))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp), modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun LanguageOption(name: String, native: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) appColors.selectedBg else if (enabled) CardBg else Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) Violet else if (enabled) Color.Transparent else Track, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (enabled) Ink else Muted)
            if (native != name) Text(native, style = MaterialTheme.typography.labelMedium)
        }
        if (!enabled) Text(stringResource(R.string.onboarding_coming_soon).uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
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
    Text(stringResource(R.string.onboarding_quick_check), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.onboarding_quick_check_body), style = MaterialTheme.typography.bodyMedium, color = BodyText)
    val q = s.question
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (q != null) {
            Text(stringResource(R.string.onboarding_what_does_mean, q.exercise.prompt), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
            q.exercise.reading?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Muted) }
            q.exercise.options.forEachIndexed { i, option ->
                LanguageOption(option, option, s.selected == i, enabled = true) { vm.select(i) }
            }
        } else if (!s.loadingQuestions) {
            Text(stringResource(R.string.onboarding_no_quiz), style = MaterialTheme.typography.bodyMedium)
        }
    }
    OutlineButton(stringResource(R.string.onboarding_skip_beginner), vm::skipPlacement)
    if (s.selected != null) GradientButton(stringResource(if (s.questionIndex + 1 >= s.questions.size) R.string.onboarding_finish_quiz else R.string.common_next), vm::nextQuestion)
    else DisabledButton(stringResource(R.string.common_next))
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
        Text(stringResource(R.string.common_daily_goal), style = MaterialTheme.typography.titleLarge)
        if (s.startUnitIndex > 0) {
            AppCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_start_at_unit, s.startUnitIndex + 1), style = MaterialTheme.typography.bodyLarge, color = Ink)
                Text(stringResource(R.string.onboarding_earlier_unlocked), style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(stringResource(R.string.onboarding_how_much_time), style = MaterialTheme.typography.bodyMedium, color = BodyText)
        UserPrefs.GOALS.forEach { minutes ->
            val label = stringResource(
                when (minutes) { 5 -> R.string.onboarding_goal_casual; 10 -> R.string.onboarding_goal_regular; else -> R.string.onboarding_goal_serious },
            )
            LanguageOption(pluralStringResource(R.plurals.common_minutes_per_day, minutes, minutes), label, s.goalMinutes == minutes, enabled = true) { vm.pickGoal(minutes) }
        }
        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_daily_reminder), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Text(if (s.reminderEnabled) stringResource(R.string.common_at_time, formatTime(s.reminderMinuteOfDay)) else stringResource(R.string.common_off), style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = s.reminderEnabled,
                    onCheckedChange = vm::setReminder,
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet),
                )
            }
            if (s.reminderEnabled) {
                Text(
                    stringResource(R.string.onboarding_change_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = Violet,
                    modifier = Modifier.clip(CircleShape).clickable { picking = true }.padding(vertical = 6.dp),
                )
            }
        }
    }
    GradientButton(stringResource(if (s.finishing) R.string.onboarding_setting_up else R.string.onboarding_start_learning), { if (s.reminderEnabled) askNotifications() else vm.finish() })

    if (picking) {
        val state = rememberTimePickerState(s.reminderMinuteOfDay / 60, s.reminderMinuteOfDay % 60)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton({ vm.setReminderTime(state.hour * 60 + state.minute); picking = false }) { Text(stringResource(R.string.common_ok)) } },
            dismissButton = { TextButton({ picking = false }) { Text(stringResource(R.string.common_cancel)) } },
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
