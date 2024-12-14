package com.touchchef.wearable.presentation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import com.touchchef.wearable.data.Task
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen(
    webSocketClient: WebSocketClient,
    tasks: List<Task>,
    currentTaskIndex: Int,
    onGameComplete: () -> Unit
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF87CEEB)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Pas de tâche\nattribuée",
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🤷",
                    style = TextStyle(fontSize = 24.sp)
                )
            }
        }
    } else {
        val currentTask = tasks[currentTaskIndex]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF87CEEB)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tâche en cours",
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    when (currentTask.type) {
                        "COUPER" -> {
                            Text("🍅", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔪", fontSize = 24.sp)
                        }
                        // Add other task types here
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${currentTask.type} ${currentTask.quantity}x 🍅",
                    color = Color.White,
                    style = TextStyle(fontSize = 14.sp)
                )
                // Add timer here if needed
            }
        }
    }
}

