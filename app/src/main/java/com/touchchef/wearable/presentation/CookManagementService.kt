package com.touchchef.wearable.presentation

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CookManagementService(
    private val webSocketClient: WebSocketClient,
    private val deviceId: String
) {
    private val _cooksFlow = MutableStateFlow<List<Cook>>(emptyList())
    val cooksFlow: StateFlow<List<Cook>> = _cooksFlow

    companion object {
        private const val TAG = "CookManagementService"
        private const val MESSAGE_TYPE_COOKS_LIST = "cooksList"
        private const val MESSAGE_SOURCE_ANGULAR = "angular"
    }

    init {
        connectToWebSocket()
    }

    private fun connectToWebSocket() {

        webSocketClient.setMessageListener { webSocketMessage ->
            Log.d("WebSocket", "Received message: $webSocketMessage")

            if (webSocketMessage.type == MESSAGE_TYPE_COOKS_LIST && webSocketMessage.from == MESSAGE_SOURCE_ANGULAR) {
                updateCooksList(webSocketMessage)
            }
        }
    }

    private fun updateCooksList(messageMap: WebSocketMessage) {
        try {
            val cooksListData = messageMap.cooksList
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
                avatar = cookData["avatar"] as? String ?: return null,
                color = cookData["color"] as? String ?: return null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating cook from data", e)
            null
        }
    }

    fun getCookById(cookId: String): Cook? = _cooksFlow.value.find { it.deviceId == cookId }
    fun getAllCooks(): List<Cook> = _cooksFlow.value
}