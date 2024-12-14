package com.touchchef.wearable.presentation

import android.content.Context
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

// GameViewModel to handle tasks
class GameViewModel(
    private val webSocketClient: WebSocketClient,
    context: Context
) : ViewModel() {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks
    private var cachedDeviceId: String = ""
    private var devicePreferences: DevicePreferences? = null

    private val _currentTaskIndex = mutableStateOf(0)
    val currentTaskIndex: Int get() = _currentTaskIndex.value


    init {
        viewModelScope.launch {
            if (devicePreferences == null) {
                devicePreferences = DevicePreferences(context)
                cachedDeviceId = devicePreferences!!.deviceId.first() ?: ""
            }
            webSocketClient.addMessageListener { message ->
                when {
                    message.type == "addTask" && message.to == cachedDeviceId -> {
                        message.assignedTask?.let { assignedTask ->
                            val newTask = Task(
                                taskName = assignedTask.taskName,
                                cook = assignedTask.cook
                            )
                            _tasks.add(newTask)
                        }
                    }
                    // Handle other message types if needed
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient.removeMessageListener { /* reference to the same listener */ }
    }
}
