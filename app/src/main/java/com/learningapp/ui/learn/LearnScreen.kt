package com.learningapp.ui.learn

import com.learningapp.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningapp.data.repo.LessonItem
import com.learningapp.data.repo.LessonState
import com.learningapp.ui.theme.CardBg
import com.learningapp.ui.theme.appColors
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.TagBg
import com.learningapp.ui.theme.Violet
import kotlin.math.roundToInt

@Composable
fun LearnScreen(onOpenLesson: (String) -> Unit, viewModel: LearnViewModel = hiltViewModel()) {
    val units by viewModel.units.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text(stringResource(R.string.learn_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) }
        units?.forEach { unit ->
            item(key = unit.id) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Text(unit.title.uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        unit.level,
                        style = MaterialTheme.typography.labelSmall,
                        color = Violet,
                        modifier = Modifier.clip(CircleShape).background(TagBg).padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }
            items(unit.lessons, key = { it.id }) { lesson -> LessonRow(lesson) { onOpenLesson(lesson.id) } }
        }
        item { Spacer(Modifier.padding(bottom = 18.dp)) }
    }
}

@Composable
private fun LessonRow(lesson: LessonItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val locked = lesson.state == LessonState.Locked
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (locked) Modifier.border(1.dp, appColors.lockedBorder, shape)
                else Modifier.shadow(2.dp, shape, ambientColor = Ink.copy(alpha = 0.1f), spotColor = Ink.copy(alpha = 0.1f)).background(CardBg, shape)
            )
            .clip(shape)
            .clickable(enabled = !locked, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            lesson.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (locked) appColors.lockedText else Ink,
            modifier = Modifier.weight(1f),
        )
        val (label, color) = when (lesson.state) {
            LessonState.Done -> (
                if (lesson.testedOut) stringResource(R.string.learn_tested_out)
                else stringResource(R.string.learn_done_accuracy, ((lesson.bestAccuracy ?: 0.0) * 100).roundToInt())
            ) to Muted
            LessonState.Next -> stringResource(R.string.learn_start) to Violet
            LessonState.Locked -> stringResource(R.string.learn_locked) to appColors.lockedText
        }
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), color = color)
    }
}
