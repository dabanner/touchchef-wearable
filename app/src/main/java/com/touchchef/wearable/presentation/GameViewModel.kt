package com.touchchef.wearable.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.touchchef.wearable.data.Task

class GameViewModel(private val webSocketClient: WebSocketClient) : ViewModel() {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    private val _currentTaskIndex = mutableStateOf(0)
    val currentTaskIndex: Int get() = _currentTaskIndex.value

    init {
        webSocketClient.addMessageListener { message ->
            when (message.type) {
                "startGame" -> {
                    // Assuming the message contains a tasks array
                    message.tasks?.forEach { task ->
                        _tasks.add(Task(task.type, task.quantity))
                    }
                }
                "nextTask" -> {
                    if (_currentTaskIndex.value < _tasks.size - 1) {
                        _currentTaskIndex.value++
                    }
                }
                // Handle other message types
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the listener when ViewModel is cleared
        webSocketClient.removeMessageListener { /* reference to the same listener */ }
    }
}
