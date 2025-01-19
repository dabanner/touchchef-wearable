package com.touchchef.wearable.presentation

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.touchchef.wearable.utils.FeedbackManager

data class AssignedTask(
    val taskName: String,
    val cook: Cook,
    val taskId: String,
    val quantity: String,
    val workStation: String,
    val taskIcons: String? = null
)

data class Cook(
    val name: String,
    val deviceId: String,
    val avatar: String,
    val color: String
)

data class Task(
    val taskName: String,
    val cook: Cook,
    val taskId: String,
    val quantity: String,
    val workstation: String,
    val taskIcons: String? = null
)

class GameViewModel(
    private val webSocketClient: WebSocketClient,
    private val feedbackManager: FeedbackManager,
    private val deviceId: String,
) : ViewModel() {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    private val _currentTaskIndex = mutableStateOf(0)
    val currentTaskIndex: Int get() = _currentTaskIndex.value

    fun popActiveTask() {
        Log.d("GameViewModel", "Attempting to pop active task at index $currentTaskIndex. Total tasks: ${_tasks.size}")

        if (_tasks.isNotEmpty() && currentTaskIndex < _tasks.size) {
            // Remove the current task
            val removedTask = _tasks[currentTaskIndex]
            _tasks.removeAt(currentTaskIndex)
            Log.d("GameViewModel", "Removed task: ${removedTask.taskName}")

            // Adjust the current index if necessary
            if (currentTaskIndex >= _tasks.size) {
                val newIndex = maxOf(_tasks.size - 1, 0)
                Log.d("GameViewModel", "Adjusting currentTaskIndex from $currentTaskIndex to $newIndex")
                _currentTaskIndex.value = newIndex
            }

            // If there are remaining tasks, send activeTask message for the new current task
            if (_tasks.isNotEmpty()) {
                val currentTask = _tasks[currentTaskIndex]
                val message = mapOf(
                    "type" to "activeTask",
                    "from" to deviceId,
                    "to" to "table",
                    "assignedTask" to currentTask
                )
                webSocketClient.sendJson(message) { success ->
                    Log.d("GameViewModel", "Active task update after pop: ${if (success) "success" else "failed"} for task: ${currentTask.taskName}")
                }
            } else {
                Log.d("GameViewModel", "No tasks remaining after pop")
            }
        } else {
            Log.e("GameViewModel", "Cannot pop task: tasks empty or invalid index")
        }
    }

    private fun popTaskWithId(taskId: String) {
        Log.d("GameViewModel", "Attempting to pop task with ID: $taskId. Total tasks: ${_tasks.size}")

        // Find the index of the task with the given ID
        val taskIndex = _tasks.indexOfFirst { it.taskId == taskId }

        if (taskIndex != -1) {
            // Remove the task
            val removedTask = _tasks[taskIndex]
            _tasks.removeAt(taskIndex)
            Log.d("GameViewModel", "Removed task: ${removedTask.taskName}")

            // Adjust the current index if the removed task was before or at the current index
            if (taskIndex <= currentTaskIndex && currentTaskIndex > 0) {
                val newIndex = currentTaskIndex - 1
                Log.d("GameViewModel", "Adjusting currentTaskIndex from $currentTaskIndex to $newIndex")
                _currentTaskIndex.value = newIndex
            }

            // If there are remaining tasks, send activeTask message for the current task
            if (_tasks.isNotEmpty()) {
                val currentTask = _tasks[currentTaskIndex]
                val message = mapOf(
                    "type" to "activeTask",
                    "from" to deviceId,
                    "to" to "table",
                    "assignedTask" to currentTask
                )
                webSocketClient.sendJson(message) { success ->
                    Log.d("GameViewModel", "Active task update after pop: ${if (success) "success" else "failed"} for task: ${currentTask.taskName}")
                }
            } else {
                Log.d("GameViewModel", "No tasks remaining after pop")
            }
        } else {
            Log.e("GameViewModel", "Cannot pop task: task with ID $taskId not found")
        }
    }

    private fun hasTask(taskId: String?): Boolean {
        // Return false immediately if taskId is null
        if (taskId == null) {
            Log.d("GameViewModel", "Checking if null taskId exists: false")
            return false
        }

        val exists = _tasks.any { it.taskId == taskId }
        Log.d("GameViewModel", "Checking if task $taskId exists: $exists")
        return exists
    }

    fun onTaskChange(newIndex: Int) {
        Log.d("GameViewModel", "Task change requested: current index=${_currentTaskIndex.value}, new index=$newIndex, total tasks=${_tasks.size}")

        if (newIndex in 0 until _tasks.size && newIndex != _currentTaskIndex.value) {
            Log.d("GameViewModel", "Updating current task index to $newIndex")
            _currentTaskIndex.value = newIndex

            val currentTask = _tasks[newIndex]
            Log.d("GameViewModel", "Switching to task: ${currentTask.taskName}")

            feedbackManager.playTaskChangeFeedback()

            val message = mapOf(
                "type" to "activeTask",
                "from" to deviceId,
                "to" to "table",
                "assignedTask" to currentTask
            )

            webSocketClient.sendJson(message) { success ->
                if (success) {
                    Log.d("GameViewModel", "Task change successful: Index=$newIndex, Task=${currentTask.taskName}")
                } else {
                    Log.e("GameViewModel", "Failed to send task change notification")
                }
            }
        } else {
            Log.d("GameViewModel", "Invalid task change request - Index=$newIndex (current=${_currentTaskIndex.value}, total=${_tasks.size})")
        }
    }

    init {
        Log.d("GameViewModel", "Initializing GameViewModel for device: $deviceId")

        webSocketClient.setMessageListener { message ->
            when {
                message.type == "addTask" && message.to == deviceId -> {
                    message.assignedTask?.let { assignedTask ->
                        Log.d("GameViewModel", "Received new task: ${assignedTask.taskName}")

                        val newTask = Task(
                            taskName = assignedTask.taskName,
                            cook = assignedTask.cook,
                            taskId = assignedTask.taskId,
                            quantity = assignedTask.quantity,
                            workstation = assignedTask.workStation,
                            taskIcons = assignedTask.taskIcons
                        )

                        val taskExists = _tasks.any { it.taskId == newTask.taskId }
                        if (!taskExists) {
                            feedbackManager.onTaskFeedback()
                            _tasks.add(newTask)
                            Log.d("GameViewModel", "Added new task. Total tasks: ${_tasks.size}")

                            val isFirstTask = _tasks.size == 1
                            if (isFirstTask) {
                                val activeTaskMessage = mapOf(
                                    "type" to "activeTask",
                                    "from" to deviceId,
                                    "to" to "table",
                                    "assignedTask" to newTask
                                )
                                webSocketClient.sendJson(activeTaskMessage) { success ->
                                    val resultMessage = if (success) "sent" else "failed"
                                    Log.d(
                                        "GameViewModel",
                                        "Initial active task notification: $resultMessage"
                                    )
                                }
                            } else {
                                Log.d("GameViewModel", "Not the first task, no notification needed")
                            }
                        } else {
                            Log.d("GameViewModel", "Skipped duplicate task: ${newTask.taskId}")
                        }
                    }
                }
                message.type == "unactiveTask" && hasTask(message.taskID) -> {
                    message.taskID?.let {
                        popTaskWithId(it)
                        feedbackManager.onTaskRemoveFeedback()
                    }
                }
            }
        }
    }
}