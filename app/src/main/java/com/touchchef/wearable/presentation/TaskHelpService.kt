package com.touchchef.wearable.presentation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Message Types
private object MessageType {
    const val HELP_REQUEST = "help_request"
    const val TASK_ACCEPTANCE = "task_acceptance"
    const val TASK_NOTIFICATION = "task_notification"
}

enum class TaskRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class TaskHelpRequest(
    val taskId: String,
    val from: String,
    val to: String,
    val type: String,
    var status: TaskRequestStatus = TaskRequestStatus.PENDING,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "taskId" to taskId,
        "from" to from,
        "to" to to,
        "type" to type,
        "message" to message,
        "timestamp" to timestamp
    )
}

class TaskHelpService(
    private val webSocketClient: WebSocketClient,
    private val deviceId: String,
    private val context: Context
) {
    companion object {
        private const val TAG = "TaskHelpService"
    }

    // State Management
    private val _activeRequestFlow = MutableStateFlow<TaskHelpRequest?>(null)
    val activeRequestFlow: StateFlow<TaskHelpRequest?> = _activeRequestFlow

    private val _pendingRequestsFlow = MutableStateFlow<List<TaskHelpRequest>>(emptyList())
    val pendingRequestsFlow: StateFlow<List<TaskHelpRequest>> = _pendingRequestsFlow

    init {
        webSocketClient.initialize(deviceId)
    }


    fun requestHelp(taskId: String, targetCookId: String) {
        if (!isValidTarget(targetCookId)) return

        val helpRequest = createHelpRequest(taskId, targetCookId)
        sendHelpRequest(helpRequest)
    }

    fun acceptTask(taskId: String, fromUserId: String) {
        updateRequestStatuses(taskId, fromUserId)
        sendTaskAcceptance(taskId, fromUserId)
    }

    fun notifyAllParticipants(taskId: String) {
        val notification = createNotificationMessage(taskId)
        sendNotification(notification, taskId)
    }

    fun clearRequest(taskId: String) {
        _pendingRequestsFlow.value = _pendingRequestsFlow.value.filterNot { it.taskId == taskId }
        if (_activeRequestFlow.value?.taskId == taskId) {
            _activeRequestFlow.value = null
        }
        Log.d(TAG, "Cleared request for task: $taskId")
    }

    // Message Handling
    fun handleTaskMessage(taskMessage: Map<String, Any>) {
        Log.d(TAG, "Starting to handle task message on device $deviceId: $taskMessage")

        val messageType = taskMessage["type"] as? String
        val from = taskMessage["from"] as? String
        val to = taskMessage["to"] as? String
        val taskId = taskMessage["taskId"] as? String

        Log.d(TAG, "Message details - Type: $messageType, From: $from, To: $to, TaskId: $taskId, DeviceId: $deviceId")

        if (from.isNullOrEmpty() ||
            to.isNullOrEmpty() || taskId.isNullOrEmpty() ) {
            Log.w(TAG, "Invalid task message - missing fields")
            return

        }

        when (messageType) {

            "help_request" -> {
                Log.d(TAG, "Processing help request")
                handleHelpRequest(taskMessage)
            }
            "task_acceptance" -> handleTaskAcceptance(taskMessage)
            "task_notification" -> handleTaskNotification(taskMessage)
            else -> Log.w(TAG, "Unknown message type: $messageType")
        }
    }

    // Private Helper Methods
    private fun isValidTarget(targetCookId: String): Boolean {
        return if (targetCookId.isBlank() || targetCookId == deviceId) {
            Log.d(TAG, "Invalid help request target")
            false
        } else true
    }

    private fun createHelpRequest(taskId: String, targetCookId: String) = TaskHelpRequest(
        taskId = taskId,
        from = deviceId,
        to = targetCookId,
        type = MessageType.HELP_REQUEST,
        message = "Besoin d'aide pour la tâche #$taskId"
    )

    private fun extractMessageDetails(taskMessage: Map<String, Any>): MessageDetails? {
        val messageType = taskMessage["type"] as? String
        val from = taskMessage["from"] as? String
        val to = taskMessage["to"] as? String
        val taskId = taskMessage["taskId"] as? String

        return if (messageType.isNullOrEmpty() || from.isNullOrEmpty() ||
            to.isNullOrEmpty() || taskId.isNullOrEmpty()) {
            Log.w(TAG, "Invalid message - missing fields: type=$messageType, from=$from, to=$to, taskId=$taskId")
            null
        } else {
            MessageDetails(messageType, from, to, taskId)
        }
    }

    private fun handleHelpRequest(taskMessage: Map<String, Any>) {
        val to = taskMessage["to"] as String
        val from = taskMessage["from"] as String

        Log.d(TAG, "Handling help request - To: $to, From: $from, ThisDevice: $deviceId")

        // Only process if we're the target
        if (to == deviceId) {
            Log.d(TAG, "This device is the target, creating request")
            val request = createTaskHelpRequestFromMessage(taskMessage)
            updatePendingRequests(request)
            updateActiveRequest(request)
            Log.d(TAG, "Help request processed and stored")
        } else {
            Log.d(TAG, "This device is not the target, ignoring request")
        }
    }

    private fun handleTaskNotification(taskMessage: Map<String, Any>) {
        val to = taskMessage["to"] as String
        if (to == "all" || to == deviceId) {
            updatePendingRequests(createTaskHelpRequestFromMessage(taskMessage))
        }
    }

    private fun handleTaskAcceptance(taskMessage: Map<String, Any>) {
        val to = taskMessage["to"] as? String
        val taskId = taskMessage["taskId"] as? String
        val from = taskMessage["from"] as? String

        // Validate the message
        if (to.isNullOrEmpty() || taskId.isNullOrEmpty() || from.isNullOrEmpty()) {
            Log.w(TAG, "Invalid task acceptance message - missing fields")
            return
        }

        // Only process if this device is the target
        if (to == deviceId) {
            val updatedRequests = _pendingRequestsFlow.value.map { request ->
                if (request.taskId == taskId && request.from == deviceId) {
                    request.copy(status = TaskRequestStatus.ACCEPTED)
                } else {
                    request
                }
            }

            // Update pending requests
            _pendingRequestsFlow.value = updatedRequests

            // Update active request if it matches
            _activeRequestFlow.value = _activeRequestFlow.value?.let { activeRequest ->
                if (activeRequest.taskId == taskId) {
                    activeRequest.copy(status = TaskRequestStatus.ACCEPTED)
                } else {
                    activeRequest
                }
            }

            Log.d(TAG, "Task #$taskId accepted by $from")
        }
    }

    private fun updateRequestStatuses(taskId: String, fromUserId: String) {
        updatePendingRequestsStatus(taskId, fromUserId)
        updateActiveRequestStatus(taskId)
    }

    private fun updatePendingRequestsStatus(taskId: String, fromUserId: String) {
        _pendingRequestsFlow.value = _pendingRequestsFlow.value.map { request ->
            if (request.taskId == taskId && request.from == fromUserId) {
                request.copy(status = TaskRequestStatus.ACCEPTED)
            } else request
        }
    }

    private fun updateActiveRequestStatus(taskId: String) {
        _activeRequestFlow.value = _activeRequestFlow.value?.let { activeRequest ->
            if (activeRequest.taskId == taskId) {
                activeRequest.copy(status = TaskRequestStatus.ACCEPTED)
            } else activeRequest
        }
    }

    private fun createNotificationMessage(taskId: String) = mapOf(
        "from" to deviceId,
        "to" to "all",
        "type" to MessageType.TASK_NOTIFICATION,
        "taskId" to taskId,
        "message" to "Qui peut prendre en charge la tâche #$taskId ?",
        "timestamp" to System.currentTimeMillis()
    )

    private fun sendNotification(notification: Map<String, Any>, taskId: String) {
        webSocketClient.sendJson(notification) { success ->
            if (success) {
                Log.d(TAG, "Notification sent successfully for task #$taskId")
                updateActiveRequest(
                    TaskHelpRequest(
                        taskId = taskId,
                        from = deviceId,
                        to = "all",
                        type = MessageType.TASK_NOTIFICATION,
                        message = "Qui peut prendre en charge la tâche #$taskId ?"
                    )
                )
            } else {
                Log.e(TAG, "Failed to send notification for task #$taskId")
            }
        }
    }

    private fun updatePendingRequests(request: TaskHelpRequest) {
        val currentRequests = _pendingRequestsFlow.value.toMutableList()
        Log.d(TAG, "Current pending requests before update: ${currentRequests.size}")

        // Remove existing request with same taskId and from
        currentRequests.removeAll { it.taskId == request.taskId && it.from == request.from }
        currentRequests.add(request)

        _pendingRequestsFlow.value = currentRequests
        Log.d(TAG, "Updated pending requests. New count: ${currentRequests.size}")
        Log.d(TAG, "All pending requests: ${_pendingRequestsFlow.value}")
    }

    private fun updateActiveRequest(request: TaskHelpRequest) {
        _activeRequestFlow.value = request
        Log.d(TAG, "Updated active request: $request")
    }

    private data class MessageDetails(
        val type: String,
        val from: String,
        val to: String,
        val taskId: String
    )

    // WebSocket Communication
    private fun sendHelpRequest(helpRequest: TaskHelpRequest) {
        webSocketClient.sendJson(helpRequest.toMap()) { success ->
            if (success) {
                Log.d(TAG, "Help request sent to cook ${helpRequest.to} for task #${helpRequest.taskId}")
                updateActiveRequest(helpRequest)
                updatePendingRequests(helpRequest)
            } else {
                Log.e(TAG, "Failed to send help request for task #${helpRequest.taskId}")
            }
        }
    }

    private fun sendTaskAcceptance(taskId: String, fromUserId: String) {
        val response = mapOf(
            "from" to deviceId,
            "to" to fromUserId,
            "type" to MessageType.TASK_ACCEPTANCE,
            "taskId" to taskId,
            "message" to "Je peux prendre en charge la tâche #$taskId",
            "timestamp" to System.currentTimeMillis()
        )

        webSocketClient.sendJson(response) { success ->
            if (success) {
                Log.d(TAG, "Task acceptance sent for task #$taskId")
            } else {
                Log.e(TAG, "Failed to send task acceptance for task #$taskId")
            }
        }
    }

    private fun createTaskHelpRequestFromMessage(message: Map<String, Any>) = TaskHelpRequest(
        taskId = message["taskId"] as String,
        from = message["from"] as String,
        to = message["to"] as String,
        type = message["type"] as String,
        message = message["message"] as String,
        status = TaskRequestStatus.PENDING,
        timestamp = (message["timestamp"] as Number).toLong()
    )

    fun cancelTask(taskId: String, helpRequest: TaskHelpRequest) {
        val cancelMessage = mapOf(
            "type" to "task_cancelled",
            "taskId" to taskId,
            "from" to deviceId,
            "to" to if (helpRequest.from == deviceId) helpRequest.to else helpRequest.from,
            "message" to "Aide annulée pour la tâche #$taskId",
            "timestamp" to System.currentTimeMillis()
        )

        webSocketClient.sendJson(cancelMessage) { success ->
            if (success) {
                clearRequest(taskId)
                Log.d(TAG, "Task cancellation sent for task #$taskId")
            } else {
                Log.e(TAG, "Failed to send task cancellation for task #$taskId")
            }
        }
    }

    fun completeTask(taskId: String, helpRequest: TaskHelpRequest) {
        val completeMessage = mapOf(
            "type" to "task_completed",
            "taskId" to taskId,
            "from" to deviceId,
            "to" to if (helpRequest.from == deviceId) helpRequest.to else helpRequest.from,
            "message" to "Tâche #$taskId terminée",
            "timestamp" to System.currentTimeMillis()
        )

        webSocketClient.sendJson(completeMessage) { success ->
            if (success) {
                clearRequest(taskId)
                Log.d(TAG, "Task completion sent for task #$taskId")
            } else {
                Log.e(TAG, "Failed to send task completion for task #$taskId")
            }
        }
    }
}