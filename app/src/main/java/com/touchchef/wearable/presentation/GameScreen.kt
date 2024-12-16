package com.touchchef.wearable.presentation

import TaskStatusScreen
import android.util.Log
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavHostController
import com.touchchef.wearable.presentation.theme.TouchChefTypography
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


@Composable
private fun EmptyTaskState(
    avatarColor: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(parseColor("#$avatarColor")),
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
                style = TextStyle(fontSize = 16.sp),
                fontFamily = TouchChefTypography.bricolageGrotesque
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🤷",
                style = TextStyle(fontSize = 24.sp)
            )
        }
    }
}

// Component for showing task progress indicators
@Composable
private fun TaskProgressIndicator(
    totalTasks: Int,
    currentTaskIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        repeat(totalTasks) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index == currentTaskIndex)
                            Color.White
                        else
                            Color.White.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .padding(vertical = 6.dp)
            )
        }
    }
}

// Component for displaying task content
@Composable
private fun TaskContent(
    task: Task,
    currentTaskIndex: Int,
    totalTasks: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tâche en cours",
            color = Color.White,
            style = TextStyle(fontSize = 16.sp),
            fontFamily = TouchChefTypography.bricolageGrotesque
        )

        Spacer(modifier = Modifier.height(24.dp))

        task.taskIcons?.let { icons ->
            Text(
                text = icons,
                color = Color.White,
                style = TextStyle(fontSize = 24.sp),
                textAlign = TextAlign.Center,
                fontFamily = TouchChefTypography.bricolageGrotesque
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = task.taskName,
            color = Color.White,
            style = TextStyle(fontSize = 20.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontFamily = TouchChefTypography.bricolageGrotesque
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${currentTaskIndex + 1}/$totalTasks",
            color = Color.White.copy(alpha = 0.7f),
            style = TextStyle(fontSize = 14.sp),
            fontFamily = TouchChefTypography.bricolageGrotesque
        )
    }
}
@Composable
private fun HandleGesturesBox(
    currentTask: Task,
    currentTaskIndex: Int,  // This is the actual current index
    tasks: List<Task>,
    onTaskChange: (Int) -> Unit,
    showTaskStatus: MutableState<Boolean>,
    content: @Composable BoxScope.() -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(parseColor(currentTask.cook.color))
            .pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        awaitPointerEventScope {
                            awaitFirstDown().also { down ->
                                Log.d("GameScreen", "Touch Down Detected for task index: $currentTaskIndex")
                                longPressJob?.cancel()
                                longPressJob = launch {
                                    delay(500)
                                    if (dragOffset == 0f) {
                                        Log.d("GameScreen", "Long Press Detected at index $currentTaskIndex - Showing Task Status")
                                        showTaskStatus.value = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(currentTaskIndex) { // Add currentTaskIndex as a key
                detectVerticalDragGestures(
                    onDragStart = {
                        Log.d("GameScreen", "Drag Started at index $currentTaskIndex - Resetting offset")
                        dragOffset = 0f
                        longPressJob?.cancel()
                    },
                    onDragEnd = {
                        Log.d("GameScreen", "Drag Ended - Final Offset: $dragOffset")
                        Log.d("GameScreen", "Current Task Index before navigation: $currentTaskIndex")

                        val minDragThreshold = 30f
                        if (abs(dragOffset) > minDragThreshold) {
                            when {
                                dragOffset > 0 && currentTaskIndex > 0 -> {
                                    val newIndex = currentTaskIndex - 1
                                    Log.d("GameScreen", "Navigating to Previous Task: $newIndex from $currentTaskIndex")
                                    onTaskChange(newIndex)
                                }
                                dragOffset < 0 && currentTaskIndex < tasks.size - 1 -> {
                                    val newIndex = currentTaskIndex + 1
                                    Log.d("GameScreen", "Navigating to Next Task: $newIndex from $currentTaskIndex")
                                    onTaskChange(newIndex)
                                }
                                else -> {
                                    Log.d("GameScreen", "Navigation not possible - At boundary: index=$currentTaskIndex, total=${tasks.size}")
                                }
                            }
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        Log.d("GameScreen", "Drag Cancelled at index $currentTaskIndex")
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        Log.d("GameScreen", "Dragging - Current Offset: $dragOffset, Current Index: $currentTaskIndex")
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
private fun TaskStatusOverlay(
    currentTask: Task,
    deviceId: String,
    avatarColor: String,
    showTaskStatus: MutableState<Boolean>,
    onPopTask: () -> Unit,
    webSocketClient: WebSocketClient,
    navController: NavHostController
) {
    Log.d("GameScreen", "Showing Task Status Screen")
    TaskStatusScreen(
        avatarColor = avatarColor,
        onCompleted = {
            Log.d("GameScreen", "Task Completed - Sending finished message")
            val message = mapOf(
                "type" to "taskFinished",
                "from" to deviceId,
                "to" to "unity"
            )
            webSocketClient.sendJson(message) { success ->
                Log.d("GameScreen", "Task finished message sent: $success")
            }
            onPopTask()
            showTaskStatus.value = false
        },
        onCancelled = {
            Log.d("GameScreen", "Task Cancelled - Sending unactive message")
            val message = mapOf(
                "type" to "unactiveTask",
                "from" to deviceId,
                "to" to "unity"
            )
            webSocketClient.sendJson(message) { success ->
                Log.d("GameScreen", "Task unactive message sent: $success")
            }
            onPopTask()
            showTaskStatus.value = false
        },
        onHelp = {
            Log.d("GameScreen", "Help Requested - Navigating to task screen")
            navController.navigate("taskScreen/$deviceId/${currentTask.taskName}") {
                popUpTo("qrcodeScreen") { inclusive = true }
                popUpTo("confirmationScreen") { inclusive = true }
            }
            showTaskStatus.value = false
        },
        onBack = {
            Log.d("GameScreen", "Task Status Screen Closed")
            showTaskStatus.value = false
        }
    )
}

@Composable
fun GameScreen(
    taskHelpService: TaskHelpService,
    cookManagementService: CookManagementService,
    webSocketClient: WebSocketClient,
    tasks: List<Task>,
    currentTaskIndex: Int,
    onTaskChange: (Int) -> Unit,
    onPopTask: () -> Unit,
    deviceId: String,
    avatarColor: String,
    navController: NavHostController
) {
    var showTaskStatus = remember { mutableStateOf(false) }

    LaunchedEffect(tasks, currentTaskIndex) {
        Log.d("GameScreen", "Tasks State Updated - Count: ${tasks.size}, Current Index: $currentTaskIndex")
        tasks.forEachIndexed { index, task ->
            Log.d("GameScreen", "Task[$index]: ${task.taskName}, Icons: ${task.taskIcons}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) {
            Log.d("GameScreen", "No tasks available")
            EmptyTaskState(avatarColor = avatarColor)
        } else {
            val currentTask = tasks[currentTaskIndex]
            Log.d("GameScreen", "Displaying Current Task: ${currentTask.taskName}, Index: $currentTaskIndex")

            HandleGesturesBox(
                currentTaskIndex = currentTaskIndex,
                tasks = tasks,
                currentTask = currentTask,
                onTaskChange = onTaskChange,
                showTaskStatus = showTaskStatus
            ) {
                TaskProgressIndicator(
                    totalTasks = tasks.size,
                    currentTaskIndex = currentTaskIndex,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )

                TaskContent(
                    task = currentTask,
                    currentTaskIndex = currentTaskIndex,
                    totalTasks = tasks.size,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                )
            }
        }

        if (showTaskStatus.value && tasks.isNotEmpty()) {
            TaskStatusOverlay(
                currentTask = tasks[currentTaskIndex],
                deviceId = deviceId,
                avatarColor = avatarColor,
                showTaskStatus = showTaskStatus,
                onPopTask = onPopTask,
                webSocketClient = webSocketClient,
                navController = navController
            )
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
