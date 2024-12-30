package com.touchchef.wearable.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

@Composable
fun CircularTimerOverlay(
    seconds: Int,
    onTimerComplete: () -> Unit = {}
) {
    var remainingSeconds by remember { mutableStateOf(seconds) }
    var progress by remember { mutableStateOf(0f) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            progress = 1f - (remainingSeconds.toFloat() / seconds.toFloat())

            if (remainingSeconds == 0) {
                isRunning = false
                onTimerComplete()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {

            // Draw progress arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.7f),
                startAngle = -90f,
                sweepAngle = -progress * 360f,
                useCenter = false,
                style = Stroke(
                    width = 16.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
