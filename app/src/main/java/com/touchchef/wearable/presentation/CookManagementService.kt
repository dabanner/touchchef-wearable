package com.touchchef.wearable.presentation

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Cook(
    val name: String,
    val deviceId: String,
    val avatar: String
)

class CookManagementService(
    private val webSocketClient: WebSocketClient,
    private val deviceId: String
) {
    private val _cooksFlow = MutableStateFlow<List<Cook>>(emptyList())
    val cooksFlow: StateFlow<List<Cook>> = _cooksFlow
    private val gson = Gson()

    companion object {
        private const val TAG = "CookManagementService"
        private const val MESSAGE_TYPE_COOKS_LIST = "cooksList"
        private const val MESSAGE_SOURCE_ANGULAR = "angular"
    }

    init {
        connectToWebSocket()
    }

    private fun connectToWebSocket() {
        webSocketClient.connect(
            onConnected = {
                Log.d(TAG, "WebSocket connected")
            },
            onTaskMessage = { taskMessage ->
                Log.d(TAG, "Received task message: $taskMessage")
            },
            onMessage = { message ->
                handleWebSocketMessage(message)
            },
            onError = { error ->
                Log.e(TAG, "WebSocket error: $error")
            }
        )
    }

    private fun handleWebSocketMessage(message: String) {
        try {
            val messageMap = gson.fromJson(message, Map::class.java) as? Map<String, Any>
            val type = messageMap?.get("type") as? String
            val from = messageMap?.get("from") as? String

            if (type == MESSAGE_TYPE_COOKS_LIST && from == MESSAGE_SOURCE_ANGULAR) {
                updateCooksList(messageMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
        }
    }

    private fun updateCooksList(messageMap: Map<String, Any>) {
        try {
            val cooksListData = messageMap["cooksList"] as? List<Map<String, Any>>
            val cooksList = cooksListData?.mapNotNull { cookData -> createCookFromData(cookData) }
                ?.filterNot { it.deviceId == deviceId } // Filter out current device
                ?: emptyList()

            _cooksFlow.value = cooksList
            Log.d(TAG, "Updated cooks list: ${cooksList.size} cooks")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cooks list", e)
        }
    }

    private fun createCookFromData(cookData: Map<String, Any>): Cook? {
        return try {
            Cook(
                name = cookData["name"] as? String ?: return null,
                deviceId = cookData["deviceId"] as? String ?: return null,
                avatar = cookData["avatar"] as? String ?: return null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating cook from data", e)
            null
        }
    }

    fun getCookById(cookId: String): Cook? = _cooksFlow.value.find { it.deviceId == cookId }
    fun getAllCooks(): List<Cook> = _cooksFlow.value
}