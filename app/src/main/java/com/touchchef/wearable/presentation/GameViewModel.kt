package com.touchchef.wearable.presentation

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchchef.wearable.data.DevicePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Updated data classes for the message format

data class AssignedTask(
    val taskName: String,
    val cook: Cook
)

data class Cook(
    val name: String,
    val deviceId: String,
    val avatar: String,
    val color: String
)

// Updated Task class to store all necessary information
data class Task(
    val taskName: String,
    val cook: Cook,
)

class GameViewModel(
    private val webSocketClient: WebSocketClient,
    private val deviceId: String
) : ViewModel() {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    private val _currentTaskIndex = mutableStateOf(0)
    val currentTaskIndex: Int get() = _currentTaskIndex.value

    fun onTaskChange(newIndex: Int) {
        if (newIndex in 0 until _tasks.size) {
            _currentTaskIndex.value = newIndex
            val message = mapOf("type" to "activeTask",
                "from" to deviceId,
                "to" to "table",
                "assignedTask" to tasks[currentTaskIndex]
            )
            webSocketClient.sendJson(message) { success ->
                if (success) {
                    Log.d("HandRaiseDetector", "Hand raise event sent successfully")
                } else {
                    Log.e("HandRaiseDetector", "Failed to send hand raise event")
                }
            }
        }
    }

    init {
        webSocketClient.setMessageListener { message ->
            when {
                message.type == "addTask" && message.to == deviceId -> {
                    message.assignedTask?.let { assignedTask ->
                        val newTask = Task(
                            taskName = assignedTask.taskName,
                            cook = assignedTask.cook
                        )
                        // Check if task already exists
                        if (!_tasks.any { existingTask ->
                                existingTask.taskName == newTask.taskName &&
                                        existingTask.cook.deviceId == newTask.cook.deviceId
                            }) {
                            _tasks.add(newTask)
                            Log.d("addTask", "Added new task: $newTask")
                        } else {
                            Log.d("addTask", "Task already exists, skipping: $newTask")
                        }
                    }
                }
            }
        }
    }
}