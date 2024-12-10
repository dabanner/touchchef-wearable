package com.touchchef.wearable.presentation

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TaskHelpRequest(
    val taskId: String,
    val from: String,
    val to: String,
    val type: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TaskHelpService(
    private val webSocketClient: WebSocketClient,
    private val deviceId: String
) {
    private val _helpRequestFlow = MutableStateFlow<TaskHelpRequest?>(null)
    val helpRequestFlow: StateFlow<TaskHelpRequest?> = _helpRequestFlow

    fun requestHelp(taskId: String, targetCookId: String) {
        val helpRequest = TaskHelpRequest(
            taskId = taskId,
            from = deviceId,
            to = targetCookId,
            type = "help_request",
            message = "Besoin d'aide pour la tâche #$taskId"
        )

        webSocketClient.sendJson(helpRequest) { success ->
            if (success) {
                Log.d("TaskHelpService", "Help request sent to cook $targetCookId for task #$taskId by $deviceId")
                _helpRequestFlow.value = helpRequest
            } else {
                Log.e("TaskHelpService", "Failed to send help request for task #$taskId")
            }
        }
    }

    fun notifyAllParticipants(taskId: String) {
        val notification = mapOf(
            "from" to deviceId,
            "to" to "all",
            "type" to "task_notification",
            "taskId" to taskId,
            "message" to "Qui peut prendre en charge la tâche #$taskId ?",
            "timestamp" to System.currentTimeMillis()
        )

        webSocketClient.sendJson(notification) { success ->
            if (success) {
                Log.d("TaskHelpService", "Notification sent successfully for task #$taskId by $deviceId")
            } else {
                Log.e("TaskHelpService", "Failed to send notification for task #$taskId")
            }
        }
    }

    fun acceptTask(taskId: String, fromUserId: String) {
        val response = mapOf(
            "from" to deviceId,
            "to" to fromUserId,
            "type" to "task_acceptance",
            "taskId" to taskId,
            "message" to "Je peux prendre en charge la tâche #$taskId",
            "timestamp" to System.currentTimeMillis()
        )

        webSocketClient.sendJson(response) { success ->
            if (success) {
                Log.d("TaskHelpService", "Task acceptance sent successfully for task #$taskId by $deviceId to $fromUserId")
            } else {
                Log.e("TaskHelpService", "Failed to send task acceptance for task #$taskId")
            }
        }
    }
}