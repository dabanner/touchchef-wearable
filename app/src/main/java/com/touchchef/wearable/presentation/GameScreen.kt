package com.touchchef.wearable.presentation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment.Companion.TopEnd

@Composable
fun GameScreen(
    webSocketClient: WebSocketClient,
    tasks: List<Task>,
    currentTaskIndex: Int,
    onTaskChange: (Int) -> Unit,
    deviceId: String,
    onNavigateToTaskStatus: (String, String) -> Unit,
    avatarColor: String,
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    parseColor("#$avatarColor")),
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
        var offsetY by remember { mutableStateOf(0f) }
        var isScrollInProgress by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(parseColor(currentTask.cook.color))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isScrollInProgress = true },
                        onDragEnd = {
                            if (abs(offsetY) > 30 && isScrollInProgress) {
                                if (offsetY > 0 && currentTaskIndex > 0) {
                                    onTaskChange(currentTaskIndex - 1)
                                } else if (offsetY < 0 && currentTaskIndex < tasks.size - 1) {
                                    onTaskChange(currentTaskIndex + 1)
                                }
                            }
                            offsetY = 0f
                            isScrollInProgress = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (isScrollInProgress) {
                                offsetY += dragAmount
                                change.consume()
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = "*️⃣",
                        modifier = Modifier
                            .clickable {
                                onNavigateToTaskStatus(deviceId, currentTask.taskName)
                            }
                            .padding(8.dp),
                        style = TextStyle(fontSize = 20.sp),
                        color = Color.White
                    )
                }

                Text(
                    text = "Tâche en cours",
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )

                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    repeat(tasks.size) { index ->
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(
                                    color = if (index == currentTaskIndex)
                                        Color.White
                                    else
                                        Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                        if (index < tasks.size - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                // Center content with more spacing
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = currentTask.taskName,
                    color = Color.White,
                    style = TextStyle(fontSize = 20.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chef: ${currentTask.cook.name}",
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "${currentTaskIndex + 1}/${tasks.size}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = TextStyle(fontSize = 14.sp)
                )
            }
        }
    }
}

fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF87CEEB) // Default color if parsing fails
    }
}
