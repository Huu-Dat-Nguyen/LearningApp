package com.learningapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.learningapp.ui.theme.BodyText

/** Centered card with a title, a line of text and one main button (empty and finished states). */
@Composable
fun MessageCard(title: String, body: String, button: String, onClick: () -> Unit, extra: @Composable () -> Unit = {}) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = BodyText, textAlign = TextAlign.Center)
            extra()
            Spacer(Modifier.size(6.dp))
            GradientButton(button, onClick)
        }
    }
}
