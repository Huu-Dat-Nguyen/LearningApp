package com.learningapp.ui.settings

import com.learningapp.data.content.languageNameRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.learningapp.R
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningapp.data.content.SUPPORTED_LANGUAGES
import com.learningapp.data.repo.UserPrefs
import com.learningapp.ui.components.AppCard
import com.learningapp.ui.components.rememberNotificationPermission
import com.learningapp.ui.onboarding.formatTime
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.Line
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.Violet

const val REPO_URL = "https://github.com/Huu-Dat-Nguyen/LearningApp"

private enum class Dialog { Main, Target, Goal, Time, Licenses }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backLabel = stringResource(R.string.common_back)
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    var notificationsBlocked by remember { mutableStateOf(false) }
    val askNotifications = rememberNotificationPermission { granted ->
        notificationsBlocked = !granted
        s?.let { if (granted) viewModel.setReminder(true, it.reminderMinuteOfDay) }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onBack).semantics { contentDescription = backLabel },
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = Ink) }
            Text(stringResource(R.string.common_settings), style = MaterialTheme.typography.titleLarge)
        }
        val st = s ?: return@Column

        Section(stringResource(R.string.settings_languages)) {
            Item(stringResource(R.string.common_main_language), stringResource(languageNameRes(st.mainLang))) { dialog = Dialog.Main }
            Divider()
            Item(stringResource(R.string.settings_learning), stringResource(languageNameRes(st.targetLang))) { dialog = Dialog.Target }
            Text(stringResource(R.string.settings_progress_kept), style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        Section(stringResource(R.string.settings_practice)) {
            Item(stringResource(R.string.common_daily_goal), pluralStringResource(R.plurals.common_minutes_per_day, st.goalMinutes, st.goalMinutes)) { dialog = Dialog.Goal }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_daily_reminder), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Text(if (st.reminderEnabled) stringResource(R.string.common_at_time, formatTime(st.reminderMinuteOfDay)) else stringResource(R.string.common_off), style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = st.reminderEnabled,
                    onCheckedChange = { on -> if (on) askNotifications() else viewModel.setReminder(false, st.reminderMinuteOfDay) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet),
                )
            }
            if (notificationsBlocked && !st.reminderEnabled) {
                Text(
                    stringResource(R.string.settings_notifications_blocked),
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                )
            }
            if (st.reminderEnabled) {
                Divider()
                Item(stringResource(R.string.settings_reminder_time), formatTime(st.reminderMinuteOfDay)) { dialog = Dialog.Time }
            }
        }
        Section(stringResource(R.string.settings_sound)) {
            Item(stringResource(R.string.settings_tts_voices), stringResource(R.string.settings_tts_voices_hint)) { openTtsSettings(context) }
        }
        Section(stringResource(R.string.settings_about)) {
            Item(stringResource(R.string.settings_version), appVersion(context)) {}
            Divider()
            Item(stringResource(R.string.settings_licences), stringResource(R.string.settings_licences_hint)) { dialog = Dialog.Licenses }
            Divider()
            Item(stringResource(R.string.settings_source_code), "github.com/Huu-Dat-Nguyen/LearningApp") { open(context, Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }
        }
    }

    val st = s ?: return
    when (dialog) {
        Dialog.Main -> Picker(
            title = stringResource(R.string.common_main_language),
            options = SUPPORTED_LANGUAGES.map { it.code to stringResource(languageNameRes(it.code)) },
            selected = st.mainLang,
            enabled = { code -> st.pairs[st.targetLang].orEmpty().contains(code) && code != st.targetLang },
            onPick = { viewModel.setLanguages(it, st.targetLang) },
            onDismiss = { dialog = null },
        )
        Dialog.Target -> Picker(
            title = stringResource(R.string.settings_language_to_learn),
            options = SUPPORTED_LANGUAGES.filter { it.code != st.mainLang }.map { it.code to stringResource(languageNameRes(it.code)) },
            selected = st.targetLang,
            enabled = { code -> st.mainLang in st.pairs[code].orEmpty() },
            onPick = { viewModel.setLanguages(st.mainLang, it) },
            onDismiss = { dialog = null },
        )
        Dialog.Goal -> Picker(
            title = stringResource(R.string.common_daily_goal),
            options = UserPrefs.GOALS.map { "$it" to pluralStringResource(R.plurals.common_minutes_per_day, it, it) },
            selected = "${st.goalMinutes}",
            enabled = { true },
            onPick = { viewModel.setGoal(it.toInt()) },
            onDismiss = { dialog = null },
        )
        Dialog.Time -> {
            val state = rememberTimePickerState(st.reminderMinuteOfDay / 60, st.reminderMinuteOfDay % 60)
            AlertDialog(
                onDismissRequest = { dialog = null },
                confirmButton = { TextButton({ viewModel.setReminder(true, state.hour * 60 + state.minute); dialog = null }) { Text(stringResource(R.string.common_ok)) } },
                dismissButton = { TextButton({ dialog = null }) { Text(stringResource(R.string.common_cancel)) } },
                text = { TimePicker(state) },
            )
        }
        Dialog.Licenses -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton({ dialog = null }) { Text(stringResource(R.string.common_close)) } },
            title = { Text(stringResource(R.string.settings_licences)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LICENSES.forEach { (name, licence) ->
                        Text(stringResource(R.string.settings_license_entry, stringResource(name), stringResource(licence)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
        )
        null -> Unit
    }
}

private val LICENSES = listOf(
    R.string.settings_license_androidx to R.string.settings_license_apache,
    R.string.settings_license_dagger to R.string.settings_license_apache,
    R.string.settings_license_kotlin to R.string.settings_license_apache,
    R.string.settings_license_icons to R.string.settings_license_apache,
    R.string.settings_license_font to R.string.settings_license_ofl,
)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp))
    AppCard(Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun Item(title: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = Line)

@Composable
private fun Picker(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    enabled: (String) -> Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.common_close)) } },
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (code, label) ->
                    val on = enabled(code)
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                            .clickable(enabled = on) { onPick(code); onDismiss() }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = code == selected, onClick = null, enabled = on, colors = RadioButtonDefaults.colors(selectedColor = Violet))
                        Text(if (on || code == selected) label else stringResource(R.string.common_coming_soon_suffix, label), color = if (on) Ink else Muted, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
    )
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"

private fun openTtsSettings(context: Context) {
    // Not every phone has the direct text-to-speech page; fall back to general settings.
    if (!open(context, Intent("com.android.settings.TTS_SETTINGS"))) open(context, Intent(Settings.ACTION_SETTINGS))
}

private fun open(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
} catch (_: ActivityNotFoundException) {
    false
}
