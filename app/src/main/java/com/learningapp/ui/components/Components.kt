package com.learningapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learningapp.ui.theme.CardBg
import com.learningapp.ui.theme.appColors
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.Track
import com.learningapp.ui.theme.Violet
import com.learningapp.ui.theme.VioletGradient

/** Main call-to-action: violet gradient pill with a soft glow. */
@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, CircleShape, ambientColor = Violet, spotColor = Violet)
            .clip(CircleShape)
            .background(VioletGradient)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

/** Secondary pill button. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(CardBg)
            .border(1.dp, Track, CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.3.sp), color = Ink)
    }
}

/** White rounded card with a very light shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .shadow(2.dp, shape, ambientColor = Ink.copy(alpha = 0.1f), spotColor = Ink.copy(alpha = 0.1f))
            .clip(shape)
            .background(CardBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Thin progress ring, like the stat cards in the reference design. */
@Composable
fun ProgressRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val track = Track
    Canvas(modifier.size(30.dp)) {
        val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        drawArc(track, 0f, 360f, useCenter = false, style = stroke)
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), useCenter = false, style = stroke)
    }
}

/** Stat card: label, big number, small unit, ring on the right. */
@Composable
fun StatCard(label: String, value: String, unit: String, progress: Float, ringColor: Color, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            ProgressRing(progress, ringColor)
        }
    }
}

/** Mon–Sun strip; today is circled, practiced days get a dot. */
@Composable
fun WeekStrip(days: List<Pair<String, Int>>, todayIndex: Int, practiced: Set<Int>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { i, (name, date) ->
            val isToday = i == todayIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .then(if (isToday) Modifier.border(1.5.dp, appColors.todayBorder, CircleShape) else Modifier)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(name.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (isToday) Ink else Muted)
                Text("$date", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (isToday) Ink else appColors.dateMuted)
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (i in practiced) Violet else Color.Transparent))
            }
        }
    }
}
