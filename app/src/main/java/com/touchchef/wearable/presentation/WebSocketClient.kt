package com.touchchef.wearable.presentation

import android.util.Log
import okhttp3.*
class WebSocketClient {
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient()

    fun connect() {
        val request = Request.Builder()
            .url("ws://10.192.42.141:8080")  // Use correct IP, for localhost : ws://localhost:8080
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected successfully")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection error", t)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
            }
        })
    }
}
