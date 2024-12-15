/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.touchchef.wearable.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val webSocketClient = WebSocketClient()
    private lateinit var bpmService: BpmService
    private lateinit var qrCodeGenerator: QRCodeGenerator;
    private lateinit var handRaiseDetector: HandRaiseDetector

    private val BODY_SENSOR_PERMISSION_CODE = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        qrCodeGenerator = QRCodeGenerator()
        qrCodeGenerator.initialize(baseContext)
        super.onCreate(savedInstanceState)
        requestSensorPermission()
        // Initialize BPM service
        bpmService = BpmService(
            webSocketClient,
            getSystemService(Context.SENSOR_SERVICE) as SensorManager,
            baseContext
        )

        // Start monitoring BPM
        bpmService.startMonitoring()

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val navController = rememberNavController()
            // Initialize the detector
            handRaiseDetector = HandRaiseDetector(this)

            // Start detection
            handRaiseDetector.startDetecting()

            NavHost(navController = navController, startDestination = "qrcodeScreen") {
                composable("qrcodeScreen") {
                    WebSocketQRCode(
                        webSocketClient = webSocketClient,
                        context = baseContext,
                        qrCodeGenerator,
                        navigateToConfirmationScreen = { name, avatar, deviceId ->

                            Log.d("MainActivity", "Navigating to confirmation screen")
                            runOnUiThread {
                                navController.navigate("confirmationScreen/$name/$avatar/$deviceId")
                            }
                        }
                    )
                }

                composable(
                    "confirmationScreen/{name}/{avatar}/{deviceId}",
                ) {
                    ConfirmationScreen(
                        webSocketClient = webSocketClient,
                        deviceId = it.arguments?.getString("deviceId") ?: "null",
                        name = it.arguments?.getString("name") ?: "Inconnu",
                        avatar = it.arguments?.getString("avatar") ?: "default_avatar.png"
                    )
                }

            }
        }
    }

    private fun requestSensorPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                checkSelfPermission(android.Manifest.permission.BODY_SENSORS)
                        == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Body sensor permission already granted")
                    initializeBpmService()
                }
                else -> {
                    Log.d("MainActivity", "Requesting body sensor permission")
                    requestPermissions(
                        arrayOf(android.Manifest.permission.BODY_SENSORS),
                        BODY_SENSOR_PERMISSION_CODE
                    )
                }
            }
        } else {
            initializeBpmService()
        }
    }

    private fun initializeBpmService() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Check if the heart rate sensor exists
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heartRateSensor == null) {
            Log.e("MainActivity", "No heart rate sensor found on this device")
            return
        } else {
            Log.d("MainActivity", "Heart rate sensor found: ${heartRateSensor.name}")
        }

        bpmService = BpmService(webSocketClient, sensorManager, baseContext)
        bpmService.startMonitoring()

        // Add a coroutine to monitor BPM values
        lifecycleScope.launch {
            bpmService.bpmFlow.collect { bpm ->
                Log.d("MainActivity", "BPM Update: $bpm")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BODY_SENSOR_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("MainActivity", "Body sensor permission granted by user")
                    initializeBpmService()
                } else {
                    Log.e("MainActivity", "Body sensor permission denied by user")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bpmService.isInitialized) {
            bpmService.stopMonitoring()
        }
        handRaiseDetector.stopDetecting()
    }
}