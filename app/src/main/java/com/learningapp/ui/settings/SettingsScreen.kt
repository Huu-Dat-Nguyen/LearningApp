package com.learningapp.ui.settings

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
import com.learningapp.data.content.languageName
import com.learningapp.data.repo.UserPrefs
import com.learningapp.ui.components.AppCard
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
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onBack).semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, color = Ink, fontWeight = FontWeight.Bold) }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }
        val st = s ?: return@Column

        Section("LANGUAGES") {
            Item("Main language", languageName(st.mainLang)) { dialog = Dialog.Main }
            Divider()
            Item("Learning", languageName(st.targetLang)) { dialog = Dialog.Target }
            Text("Progress is kept for each language you learn.", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        Section("PRACTICE") {
            Item("Daily goal", "${st.goalMinutes} min a day") { dialog = Dialog.Goal }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminder", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Text(if (st.reminderEnabled) "At ${formatTime(st.reminderMinuteOfDay)}" else "Off", style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = st.reminderEnabled,
                    onCheckedChange = { viewModel.setReminder(it, st.reminderMinuteOfDay) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet),
                )
            }
            if (st.reminderEnabled) {
                Divider()
                Item("Reminder time", formatTime(st.reminderMinuteOfDay)) { dialog = Dialog.Time }
            }
        }
        Section("SOUND") {
            Item("Text-to-speech voices", "Add or change voices in the phone's settings") { openTtsSettings(context) }
        }
        Section("ABOUT") {
            Item("Version", appVersion(context)) {}
            Divider()
            Item("Open-source licences", "Libraries used by this app") { dialog = Dialog.Licenses }
            Divider()
            Item("Source code", "github.com/Huu-Dat-Nguyen/LearningApp") { open(context, Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }
        }
    }

    val st = s ?: return
    when (dialog) {
        Dialog.Main -> Picker(
            title = "Main language",
            options = SUPPORTED_LANGUAGES.map { it.code to it.englishName },
            selected = st.mainLang,
            enabled = { code -> st.pairs[st.targetLang].orEmpty().contains(code) && code != st.targetLang },
            onPick = { viewModel.setLanguages(it, st.targetLang) },
            onDismiss = { dialog = null },
        )
        Dialog.Target -> Picker(
            title = "Language to learn",
            options = SUPPORTED_LANGUAGES.filter { it.code != st.mainLang }.map { it.code to it.englishName },
            selected = st.targetLang,
            enabled = { code -> st.mainLang in st.pairs[code].orEmpty() },
            onPick = { viewModel.setLanguages(st.mainLang, it) },
            onDismiss = { dialog = null },
        )
        Dialog.Goal -> Picker(
            title = "Daily goal",
            options = UserPrefs.GOALS.map { "$it" to "$it min a day" },
            selected = "${st.goalMinutes}",
            enabled = { true },
            onPick = { viewModel.setGoal(it.toInt()) },
            onDismiss = { dialog = null },
        )
        Dialog.Time -> {
            val state = rememberTimePickerState(st.reminderMinuteOfDay / 60, st.reminderMinuteOfDay % 60)
            AlertDialog(
                onDismissRequest = { dialog = null },
                confirmButton = { TextButton({ viewModel.setReminder(true, state.hour * 60 + state.minute); dialog = null }) { Text("OK") } },
                dismissButton = { TextButton({ dialog = null }) { Text("Cancel") } },
                text = { TimePicker(state) },
            )
        }
        Dialog.Licenses -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton({ dialog = null }) { Text("Close") } },
            title = { Text("Open-source licences") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LICENSES.forEach { (name, licence) ->
                        Text("$name — $licence", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
        )
        null -> Unit
    }
}

private val LICENSES = listOf(
    "AndroidX, Jetpack Compose, Room, Navigation, DataStore, WorkManager" to "Apache 2.0",
    "Dagger / Hilt" to "Apache 2.0",
    "Kotlin, kotlinx.coroutines, kotlinx.serialization" to "Apache 2.0",
    "Material icons" to "Apache 2.0",
)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp))
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
        confirmButton = { TextButton(onDismiss) { Text("Close") } },
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
                        Text(if (on || code == selected) label else "$label · coming soon", color = if (on) Ink else Muted, modifier = Modifier.padding(start = 8.dp))
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
