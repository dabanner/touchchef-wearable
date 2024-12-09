package com.touchchef.wearable.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.touchchef.wearable.data.DevicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BpmMessage(
    val bpm: Int,
    val timestamp: Long
)

class BpmService(
    private val webSocketClient: WebSocketClient,
    private val sensorManager: SensorManager,
    private val context: Context
) : SensorEventListener {
    private var devicePreferences: DevicePreferences? = null

    fun initialize(context: android.content.Context) {
        if (devicePreferences == null) {
            devicePreferences = DevicePreferences(context)
        }
    }
    private var monitoringJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _bpmFlow = MutableStateFlow<Int?>(null)
    val bpmFlow: StateFlow<Int?> = _bpmFlow

    private val heartRateSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        sensorManager.unregisterListener(this)
        _bpmFlow.value = null
    }
    fun startMonitoring() {
        val sensor = heartRateSensor
        if (sensor == null) {
            Log.e("BpmService", "Heart rate sensor not found")
            return
        }

        Log.d("BpmService", "Starting heart rate monitoring with sensor: ${sensor.name}")

        val registered = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            Log.e("BpmService", "Failed to register sensor listener")
            return
        }

        Log.d("BpmService", "Successfully registered sensor listener")

        if (devicePreferences == null) {
            devicePreferences = DevicePreferences(context)
        }

        // Start periodic sending of BPM data
        monitoringJob = coroutineScope.launch {
            while (isActive) {
                _bpmFlow.value?.let { bpm ->
                    Log.d("BpmService", "Preparing to send BPM: $bpm")
                    //
                    //val message = mapOf("type" to "confirmation", "to" to "angular", "from" to deviceId)
                    val bpmMessage = mapOf(
                        "from" to devicePreferences!!.deviceId.first(),
                        "to" to "angular",
                        "type" to "heartrate",
                        "bpm" to bpm,
                        "timestamp" to System.currentTimeMillis()
                    )

                    webSocketClient.sendJson(bpmMessage) { success ->
                        if (success) {
                            Log.d("BpmService", "Successfully sent BPM: $bpm")
                        } else {
                            Log.e("BpmService", "Failed to send BPM data")
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val bpm = event.values[0].toInt()
            Log.d("BpmService", "Raw sensor reading: ${event.values[0]}")
            Log.d("BpmService", "Converted BPM: $bpm")
            _bpmFlow.value = bpm
        } else {
            Log.d("BpmService", "Received sensor event from unexpected sensor: ${event?.sensor?.name}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("BpmService", "Sensor accuracy changed: $accuracy for sensor: ${sensor?.name}")
    }
}
