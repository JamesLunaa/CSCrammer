package com.example.civora.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civora.data.db.Accuracy
import com.example.civora.domain.Scoring
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.TotalNumber

@Composable
fun HomeScreen(
    overall: Accuracy?,
    onQuickReview: () -> Unit,
    onPractice: () -> Unit,
    onMock: () -> Unit,
    onReview: () -> Unit,
    onStats: () -> Unit,
) {
    val extras = CivoraTheme.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(extras.field)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Civora", style = MaterialTheme.typography.displayLarge, color = extras.onField)
            Text("Civil Service Exam reviewer", style = MaterialTheme.typography.bodyLarge, color = extras.fieldLilac)
            val answered = overall?.answered ?: 0
            Text(
                if (answered > 0) {
                    "$answered answered · ${Scoring.percent(overall!!.correct, answered)}% correct"
                } else {
                    "2,500 questions ready. No setup."
                },
                style = TotalNumber.copy(fontSize = 18.sp),
                color = extras.onField,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Column(
            Modifier.navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The single lamp action on Home.
            Button(
                onClick = onQuickReview,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Quick review · 10 random questions", style = MaterialTheme.typography.labelLarge) }
            ModeCard("Practice", "Pick a subject, topics and difficulty.", onPractice)
            ModeCard("Mock exam", "Full 170 items in 3h 10m, or a 50-item mini.", onMock)
            ModeCard("Review", "Re-drill your mistakes and bookmarked questions.", onReview)
            ModeCard("Progress", "Accuracy by subject, weakest topics, streak and history.", onStats)
        }
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
