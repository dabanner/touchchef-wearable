package com.touchchef.wearable.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import android.content.Context
import android.util.Log
import com.touchchef.wearable.data.DevicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HandRaiseDetector(private val context: Context, private val webSocketClient: WebSocketClient) {
    private var isHandRaised = false
    private var lastHandRaiseState = false
    private var devicePreferences: DevicePreferences? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Get the sensor manager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Get the accelerometer sensor
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Create sensor event listener
    private val sensorEventListener = object : SensorEventListener {
        private val Y_THRESHOLD = -7.0f
        private val MOVEMENT_THRESHOLD = 2.0f
        private var lastX = 0f
        private var lastY = 0f
        private var lastZ = 0f
        private var stableCount = 0
        private val STABLE_THRESHOLD = 5

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let { sensorEvent ->
                val x = sensorEvent.values[0]
                val y = sensorEvent.values[1]
                val z = sensorEvent.values[2]

                // Calculate movement delta
                val deltaX = kotlin.math.abs(lastX - x)
                val deltaY = kotlin.math.abs(lastY - y)
                val deltaZ = kotlin.math.abs(lastZ - z)

                // Update last values
                lastX = x
                lastY = y
                lastZ = z

                // Check if hand is raised vertically overhead
                val isMoving = deltaX > MOVEMENT_THRESHOLD ||
                        deltaY > MOVEMENT_THRESHOLD ||
                        deltaZ > MOVEMENT_THRESHOLD

                // Check if hand is raised overhead (watch face down, arm up)
                val isOverhead = y < Y_THRESHOLD

                // Implement stability counter
                if (isOverhead && !isMoving) {
                    stableCount++
                    if (stableCount >= STABLE_THRESHOLD) {
                        isHandRaised = true
                    }
                } else {
                    stableCount = 0
                    isHandRaised = false
                }

                // Send WebSocket message only when state changes
                if (isHandRaised != lastHandRaiseState) {
                    lastHandRaiseState = isHandRaised
                    sendHandRaiseEvent(isHandRaised)
                }
            }
        }
    }

    private fun sendHandRaiseEvent(isHandRaised: Boolean) {
        coroutineScope.launch {
            if (devicePreferences == null) {
                devicePreferences = DevicePreferences(context)
            }

            val deviceId = devicePreferences!!.deviceId.first().toString()

            val handRaiseEvent = mapOf(
                "from" to deviceId,
                "to" to "angular",
                "type" to "handRaise",
                "raised" to isHandRaised,
                "timestamp" to System.currentTimeMillis()
            )

            webSocketClient.sendJson(handRaiseEvent) { success ->
                if (success) {
                    Log.d("HandRaiseDetector", "Hand raise event sent successfully")
                } else {
                    Log.e("HandRaiseDetector", "Failed to send hand raise event")
                }
            }
        }
    }

    fun startDetecting() {
        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stopDetecting() {
        sensorManager.unregisterListener(sensorEventListener)
    }
}
