package com.learningapp.ui.review

import com.learningapp.data.content.languageNameRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningapp.R
import com.learningapp.data.db.CardRow
import com.learningapp.domain.srs.Grade
import com.learningapp.domain.srs.Sm2Scheduler
import com.learningapp.ui.components.AppCard
import com.learningapp.ui.components.GradientButton
import com.learningapp.ui.components.MessageCard
import com.learningapp.ui.components.Speaker
import com.learningapp.ui.components.TrackStudyTime
import com.learningapp.ui.components.rememberSpeaker
import com.learningapp.ui.theme.BodyText
import com.learningapp.ui.theme.CardBg
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.Line
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.TagBg
import com.learningapp.ui.theme.Track
import com.learningapp.ui.theme.Violet
import com.learningapp.ui.theme.VioletGradient

@Composable
fun ReviewScreen(onDone: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker(state.targetLang)
    TrackStudyTime()
    ReviewContent(state, speaker, onFlip = viewModel::flip, onGrade = viewModel::grade, onDone = onDone)
}

@Composable
private fun ReviewContent(
    state: ReviewUiState,
    speaker: Speaker,
    onFlip: () -> Unit,
    onGrade: (Grade) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.review_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (state.card != null) Text(state.positionLabel, style = MaterialTheme.typography.labelMedium)
        }
        val card = state.card
        when {
            state.loading -> Unit
            state.isEmpty -> MessageCard(
                title = stringResource(R.string.review_nothing_title),
                body = stringResource(R.string.review_nothing_body),
                button = stringResource(R.string.common_back_to_home),
                onClick = onDone,
            )
            state.finished -> MessageCard(
                title = stringResource(R.string.review_all_done_title),
                body = pluralStringResource(R.plurals.review_all_done_body, state.reviewed, state.reviewed),
                button = stringResource(R.string.common_back_to_home),
                onClick = onDone,
            )
            card != null -> {
                // New key per card, so the next card starts face up instead of flipping back.
                key(state.position, card.wordId) {
                    FlashCard(card, state.isFlipped, state.targetLang, speaker, onFlip, Modifier.weight(1f))
                }
                if (state.isFlipped) {
                    GradeRow(state.intervals, onGrade)
                } else {
                    GradientButton(stringResource(R.string.review_show_answer), onFlip)
                }
            }
        }
    }
}

@Composable
private fun FlashCard(
    card: CardRow,
    flipped: Boolean,
    langTag: String,
    speaker: Speaker,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(400), label = "flip")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .background(CardBg)
            .border(1.dp, Line, shape)
            .clickable(onClick = onFlip)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (rotation <= 90f) {
            CardFront(card)
        } else {
            // Turn the back face around so its text is not mirrored.
            Column(
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CardBack(card, langTag, speaker) }
        }
    }
}

@Composable
private fun CardFront(card: CardRow) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (card.isNew) Tag(stringResource(R.string.review_new).uppercase())
        Text(card.text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
        card.reading?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = Muted, textAlign = TextAlign.Center) }
        Text(stringResource(R.string.review_tap_to_flip), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ColumnScope.CardBack(card: CardRow, langTag: String, speaker: Speaker) {
    val playLabel = stringResource(R.string.review_play_audio)
    Text(card.text + (card.reading?.let { "  ·  $it" } ?: ""), style = MaterialTheme.typography.labelLarge, color = Muted)
    Spacer(Modifier.size(6.dp))
    Text(card.translation, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
    HorizontalDivider(Modifier.padding(vertical = 18.dp).fillMaxWidth(0.4f), color = Line)
    Text(card.example, style = MaterialTheme.typography.bodyLarge, color = BodyText, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
    Text(card.translationExample, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
    Spacer(Modifier.size(20.dp))
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(TagBg)
            .clickable { speaker.speak("${card.text}. ${card.example}") }
            .semantics { contentDescription = playLabel },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_volume), contentDescription = null, tint = if (speaker.ready) Violet else Muted)
    }
    if (speaker.missingVoice) {
        val language = stringResource(languageNameRes(langTag))
        Text(
            stringResource(R.string.common_voice_missing, language),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Violet,
        modifier = Modifier.clip(CircleShape).background(TagBg).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun GradeRow(intervals: Map<Grade, Int>, onGrade: (Grade) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Grade.entries.forEach { grade ->
            GradeButton(grade, intervals[grade] ?: 0, onGrade)
        }
    }
}

@Composable
private fun RowScope.GradeButton(grade: Grade, days: Int, onGrade: (Grade) -> Unit) {
    val gradeLabel = stringResource(
        when (grade) {
            Grade.Again -> R.string.review_grade_again
            Grade.Hard -> R.string.review_grade_hard
            Grade.Good -> R.string.review_grade_good
            Grade.Easy -> R.string.review_grade_easy
        },
    )
    val primary = grade == Grade.Good
    val shape = RoundedCornerShape(16.dp)
    val interval = intervalLabel(days)
    val description = stringResource(R.string.review_grade_description, gradeLabel, interval)
    Column(
        Modifier
            .weight(1f)
            .clip(shape)
            .then(if (primary) Modifier.background(VioletGradient) else Modifier.background(CardBg).border(1.dp, Track, shape))
            .clickable { onGrade(grade) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(gradeLabel, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.3.sp), color = if (primary) Color.White else Ink)
        Text(interval, style = MaterialTheme.typography.labelMedium, color = if (primary) Color.White.copy(alpha = 0.85f) else Muted)
    }
}

/** "6d", "3mo", "1.5y" in the app language (same thresholds as Sm2Scheduler.formatInterval). */
@Composable
private fun intervalLabel(days: Int): String = when {
    days < 30 -> stringResource(R.string.review_interval_days, days)
    days < 365 -> stringResource(R.string.review_interval_months, kotlin.math.round(days / 30.0).toInt())
    else -> stringResource(R.string.review_interval_years, "%.1f".format(java.util.Locale.ROOT, days / 365.0).removeSuffix(".0"))
}
