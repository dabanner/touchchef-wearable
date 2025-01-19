package com.touchchef.wearable.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

    private val TAG = "HandRaiseDetector"

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val sensorEventListener = object : SensorEventListener {
        // Detection thresholds
        private val RAISE_X_THRESHOLD = 8.0f      // X threshold for raising
        private val LOWER_X_THRESHOLD = 7.0f      // X threshold for lowering (hysteresis)
        private val Y_TOLERANCE = 3.5f            // Y tolerance increased slightly
        private val Z_TOLERANCE = 3.5f            // Z tolerance increased slightly

        private val MOVEMENT_THRESHOLD = 3.0f      // Increased movement tolerance
        private var lastX = 0f
        private var lastY = 0f
        private var lastZ = 0f

        // Stability counters
        private var stableCount = 0
        private val RAISE_STABLE_THRESHOLD = 3     // Need more samples to trigger raise
        private val LOWER_STABLE_THRESHOLD = 10    // Need even more samples to trigger lower

        // Moving average for X axis
        private val xValues = ArrayDeque<Float>(5)
        private var xAverage = 0f

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Log.d(TAG, "Sensor accuracy changed: $accuracy")
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let { sensorEvent ->
                val x = sensorEvent.values[0]
                val y = sensorEvent.values[1]
                val z = sensorEvent.values[2]

                // Update moving average for X
                xValues.addLast(x)
                if (xValues.size > 5) {
                    xValues.removeFirst()
                }
                xAverage = xValues.average().toFloat()

                // Calculate movement delta
                val deltaX = kotlin.math.abs(lastX - x)
                val deltaY = kotlin.math.abs(lastY - y)
                val deltaZ = kotlin.math.abs(lastZ - z)

                // Update last values
                lastX = x
                lastY = y
                lastZ = z

                val isMoving = deltaX > MOVEMENT_THRESHOLD ||
                        deltaY > MOVEMENT_THRESHOLD ||
                        deltaZ > MOVEMENT_THRESHOLD

                // Check if hand is raised, using different thresholds based on current state
                val threshold = if (!isHandRaised) RAISE_X_THRESHOLD else LOWER_X_THRESHOLD
                val isRaisedProperly = xAverage > threshold &&
                        kotlin.math.abs(y) < Y_TOLERANCE &&
                        kotlin.math.abs(z) < Z_TOLERANCE

                Log.d(TAG, "Orientation - X: $x (avg: $xAverage), Y: $y, Z: $z, Moving: $isMoving, RaisedProperly: $isRaisedProperly")

                if (isRaisedProperly && !isMoving) {
                    stableCount++
                    Log.d(TAG, "Position stable, count: $stableCount")
                    // Use different thresholds for raising vs lowering
                    if (!isHandRaised && stableCount >= RAISE_STABLE_THRESHOLD) {
                        Log.d(TAG, "Stability threshold reached! Hand raise detected!")
                        isHandRaised = true
                        stableCount = 0  // Reset counter after state change
                    }
                } else if (!isRaisedProperly && !isMoving) {
                    stableCount++
                    if (isHandRaised && stableCount >= LOWER_STABLE_THRESHOLD) {
                        Log.d(TAG, "Lower threshold reached! Hand lowered!")
                        isHandRaised = false
                        stableCount = 0  // Reset counter after state change
                    }
                } else {
                    if (stableCount > 0) {
                        Log.d(TAG, "Reset stability counter. Was: $stableCount")
                    }
                    stableCount = 0
                }

                if (isHandRaised != lastHandRaiseState) {
                    Log.d(TAG, "Hand raise state changed from $lastHandRaiseState to $isHandRaised")
                    lastHandRaiseState = isHandRaised
                    sendHandRaiseEvent(isHandRaised)
                }
            }
        }
    }

    private fun sendHandRaiseEvent(isHandRaised: Boolean) {
        Log.d(TAG, "Attempting to send hand raise event: $isHandRaised")
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
                    Log.d(TAG, "Hand raise event sent successfully")
                } else {
                    Log.e(TAG, "Failed to send hand raise event")
                }
            }
        }
    }

    fun startDetecting() {
        Log.d(TAG, "Starting hand raise detection")
        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stopDetecting() {
        Log.d(TAG, "Stopping hand raise detection")
        sensorManager.unregisterListener(sensorEventListener)
    }
}