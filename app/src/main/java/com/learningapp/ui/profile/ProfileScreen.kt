package com.learningapp.ui.profile

import androidx.compose.ui.res.stringResource
import com.learningapp.R
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.learningapp.ui.theme.TagBg
import com.learningapp.ui.theme.Violet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.learningapp.data.repo.ProgressRepository
import com.learningapp.data.repo.ProgressSummary
import com.learningapp.ui.components.AppCard
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.Track
import com.learningapp.ui.theme.VioletGradient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ProgressViewModel @Inject constructor(progress: ProgressRepository) : ViewModel() {
    val summary: StateFlow<ProgressSummary?> = progress.summary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProfileScreen(onOpenSettings: () -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val settingsLabel = stringResource(R.string.common_settings)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(TagBg).clickable(onClick = onOpenSettings)
                    .semantics { contentDescription = settingsLabel },
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_settings), contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp)) }
        }
        val s = summary ?: return@Column
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${s.streak}", stringResource(R.string.profile_day_streak), Modifier.weight(1f))
            Stat("${s.totalXp}", stringResource(R.string.profile_total_xp), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${s.wordsStarted}", stringResource(R.string.profile_words_learned), Modifier.weight(1f))
            Stat(s.accuracy7d?.let { "${(it * 100).roundToInt()}%" } ?: "–", stringResource(R.string.profile_accuracy_7d), Modifier.weight(1f))
        }
        AppCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_xp_last_7), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            XpChart(s)
        }
        AppCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_today), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.profile_minutes_studied, s.todayMinutes, s.dailyGoalMinutes), style = MaterialTheme.typography.bodyLarge, color = Ink)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(Track)) {
                Box(Modifier.fillMaxWidth(s.goalProgress).height(8.dp).clip(RoundedCornerShape(99.dp)).background(VioletGradient))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    AppCard(modifier) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Bars scaled to the best day; a small stub shows days with no XP. */
@Composable
private fun XpChart(s: ProgressSummary) {
    val max = (s.last7.maxOfOrNull { it.xp } ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        s.last7.forEach { day ->
            val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            val description = stringResource(R.string.profile_day_xp_description, label, day.xp)
            Column(
                Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (day.xp > 0) Text("${day.xp}", style = MaterialTheme.typography.labelSmall, color = Muted)
                val fraction = (day.xp.toFloat() / max).coerceIn(0.04f, 1f)
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(fraction * 0.75f).clip(RoundedCornerShape(8.dp))
                        .then(if (day.xp > 0) Modifier.background(VioletGradient) else Modifier.background(Track)),
                )
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (day.date == s.today) Ink else Muted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
