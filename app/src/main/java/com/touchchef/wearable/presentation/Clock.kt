package com.touchchef.wearable.presentation


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

@Composable
fun CallStyleTimer(
    onAccept: () -> Unit = {},
    onRefuse: () -> Unit = {}
) {
    var remainingSeconds by remember { mutableStateOf(45) } // 45 seconds timer

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Main circular timer background
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFF87CEEB)), // Light blue background
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Minuteur de",
                    color = Color.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = String.format("%02d:%02ds", remainingSeconds / 60, remainingSeconds % 60),
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Accept/Refuse buttons at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Accept (Green) button
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Demarrer",
                    tint = Color.White
                )
            }

            // Refuse (Red) button
            Button(
                onClick = onRefuse,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Refuse",
                    tint = Color.White
                )
            }
        }
    }
}

// Preview
@Preview(
    widthDp = 192,
    heightDp = 192,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun CallStyleTimerPreview() {
    CallStyleTimer()
}