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

    init {
        setupWebSocketListener()
    }

    private fun setupWebSocketListener() {
        webSocketClient.connect(
            onConnected = {
                Log.d("CookManagementService", "WebSocket connected")
            },
            onMessage = { message ->
                try {
                    val messageMap = gson.fromJson(message, Map::class.java) as? Map<String, Any>
                    if (messageMap != null) {
                        val type = messageMap["type"] as? String
                        val from = messageMap["from"] as? String

                        if (type == "cooksList" && from == "angular") {
                            val cooksListData = messageMap["cooksList"] as? List<Map<String, Any>>
                            val cooksList = cooksListData?.mapNotNull { cookData ->
                                try {
                                    Cook(
                                        name = cookData["name"] as? String ?: "",
                                        deviceId = cookData["deviceId"] as? String ?: "",
                                        avatar = cookData["avatar"] as? String ?: ""
                                    )
                                } catch (e: Exception) {
                                    Log.e("CookManagementService", "Error parsing cook data", e)
                                    null
                                }
                            } ?: emptyList()

                            _cooksFlow.value = cooksList
                            Log.d("CookManagementService", "Updated cooks list: ${cooksList.size} cooks")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CookManagementService", "Error processing message", e)
                }
            },
            onError = { error ->
                Log.e("CookManagementService", "WebSocket error: $error")
            }
        )
    }

    fun getCookById(cookId: String): Cook? {
        return _cooksFlow.value.find { it.deviceId == cookId }
    }

    fun getAllCooks(): List<Cook> {
        return _cooksFlow.value
    }
}