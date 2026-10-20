package com.learningapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learningapp.data.repo.ProgressSummary
import com.learningapp.ui.components.AppCard
import com.learningapp.ui.components.GradientButton
import com.learningapp.ui.components.StatCard
import com.learningapp.ui.components.WeekStrip
import com.learningapp.ui.theme.LearningAppTheme
import com.learningapp.ui.theme.Pink
import com.learningapp.ui.theme.TagBg
import com.learningapp.ui.theme.Teal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// All numbers come from HomeViewModel (Room); defaults are for the preview.
@Composable
fun HomeScreen(
    onContinueLesson: () -> Unit,
    onReview: () -> Unit,
    onSpeak: () -> Unit,
    cardsDue: Int = 12,
    /** Title of the next lesson; null when all lessons are done. */
    nextLessonTitle: String? = "Greetings 2",
    summary: ProgressSummary? = null,
) {
    val today = summary?.today ?: LocalDate.now()
    val monday = today.with(DayOfWeek.MONDAY)
    val week = (0..6).map { monday.plusDays(it.toLong()) }
        .map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to it.dayOfMonth }
    val practiced = summary?.week.orEmpty().mapIndexedNotNull { i, d -> if (d.active) i else null }.toSet()
    val streak = summary?.streak ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hi there", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).background(TagBg)) // profile picture placeholder
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                "Daily goal", "${summary?.todayMinutes ?: 0}", "/${summary?.dailyGoalMinutes ?: 10} min",
                summary?.goalProgress ?: 0f, Pink, Modifier.weight(1f),
            )
            // Ring fills over a week of streak.
            StatCard(
                "Day streak", "$streak", "${summary?.totalXp ?: 0} XP",
                (streak / 7f).coerceAtMost(1f), Teal, Modifier.weight(1f),
            )
        }
        WeekStrip(week, todayIndex = today.dayOfWeek.value - 1, practiced = practiced)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("READY TO LEARN?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                if (nextLessonTitle != null) "Next lesson: $nextLessonTitle" else "All lessons done. Keep reviewing!",
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.size(4.dp))
            GradientButton(if (nextLessonTitle != null) "Continue lesson" else "See lessons", onContinueLesson)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppCard(Modifier.weight(1f), onClick = onReview) {
                Text("$cardsDue", style = MaterialTheme.typography.titleMedium)
                Text("Cards due", style = MaterialTheme.typography.labelMedium)
            }
            AppCard(Modifier.weight(1f), onClick = onSpeak) {
                Text("Speak", style = MaterialTheme.typography.titleMedium)
                Text("Pronunciation", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFEEF3)
@Composable
private fun HomePreview() {
    LearningAppTheme { HomeScreen({}, {}, {}) }
}
