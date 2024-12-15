package com.touchchef.wearable.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

@Composable
fun CallStyleTimer(numOfSeconds: Int) {
    var remainingSeconds by remember { mutableStateOf(numOfSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Minuteur de",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            // Timer display
            Text(
                text = String.format("%02d:%02ds", remainingSeconds / 60, remainingSeconds % 60),
                color = Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Clock emoji
            Text(
                text = "⏰",
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            // Start button
            Button(
                onClick = { isRunning = true },
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF90EE90),  // Light green
                    disabledBackgroundColor = Color(0xFF90EE90).copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Démarré",
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                    Text(text = "✓", fontSize = 16.sp)
                }
            }

            // Refuse button
            Button(
                onClick = {
                    isRunning = false
                    remainingSeconds = 43
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFF6961)  // Light red
                ),
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Refusé",
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                    Text(text = "❌", fontSize = 16.sp)
                }
            }
        }
    }
}