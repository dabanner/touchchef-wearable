package com.touchchef.wearable.presentation

import TaskStatusScreen
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.touchchef.wearable.data.DevicePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.Manifest
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    private val webSocketClient = WebSocketClient()
    private lateinit var bpmService: BpmService
    private lateinit var qrCodeGenerator: QRCodeGenerator;
    private lateinit var handRaiseDetector: HandRaiseDetector
    private lateinit var taskHelpService: TaskHelpService
    private lateinit var cookManagementService: CookManagementService

    private val gson = Gson()
    private lateinit var deviceId: String
    private var devicePreferences: DevicePreferences? = null

    private val BODY_SENSOR_PERMISSION_CODE = 100
    private val NOTIFICATION_PERMISSION_CODE = 102


    override fun onCreate(savedInstanceState: Bundle?) {
        if (devicePreferences == null) {
            devicePreferences = DevicePreferences(baseContext)
        }
        lifecycleScope.launch {
            deviceId = DevicePreferences(baseContext).deviceId.first() ?: ""
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)

        installSplashScreen()
        qrCodeGenerator = QRCodeGenerator()
        qrCodeGenerator.initialize(baseContext)

        // Get deviceId and wait until we have one
        var deviceId = qrCodeGenerator.getCachedDeviceId()
        while (deviceId.isEmpty()) {
            Thread.sleep(100)  // Wait a bit
            deviceId = qrCodeGenerator.getCachedDeviceId()
        }

        // Now we definitely have a deviceId
        webSocketClient.initialize(deviceId)
        initializeServices()

        requestNotificationPermission()
        requestSensorPermission()
        setupNavigation()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun setupNavigation() {
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val navController = rememberNavController()
            // Initialize the detector
            handRaiseDetector = HandRaiseDetector(this, webSocketClient)

            // Start detection
            handRaiseDetector.startDetecting()

            NavHost(navController = navController, startDestination = "qrcodeScreen") {
                composable("qrcodeScreen") {
                    WebSocketQRCode(
                        webSocketClient = webSocketClient,
                        context = baseContext,
                        qrCodeGenerator = qrCodeGenerator,
                        navigateToConfirmationScreen = { name, avatar, deviceId, avatarColor ->
                            val avatarColorFix = avatarColor.replace("#", "")
                            Log.d("MainActivity", "Navigating to confirmation screen with name: $name, avatar: $avatar, deviceId: $deviceId, avatarColor: $avatarColorFix")
                            runOnUiThread {
                                navController.navigate("confirmationScreen/$name/$avatar/$deviceId/$avatarColorFix")
                            }
                        }
                    )
                }

                composable("confirmationScreen/{name}/{avatar}/{deviceId}/{avatarColor}") { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: "Inconnu"
                    val avatar = backStackEntry.arguments?.getString("avatar") ?: "one.png"
                    val deviceId = backStackEntry.arguments?.getString("deviceId") ?: "null"
                    val avatarColor = backStackEntry.arguments?.getString("avatarColor") ?: "ffffff"

                    ConfirmationScreen(
                        webSocketClient = webSocketClient,
                        deviceId = deviceId,
                        name = name,
                        avatar = avatar,
                        avatarColor = avatarColor,
                        navigateToGameScreen = {
                            Log.d("MainActivity", "Navigating to confirmation screen")
                            runOnUiThread {
                                navController.navigate("gameScreen/${deviceId}/${avatarColor}") {
                                    popUpTo("qrcodeScreen") { inclusive = true }
                                    popUpTo("confirmationScreen") { inclusive = true }
                                }
                            }

                        }
                    )
                }

                composable(
                    route = "gameScreen/{deviceId}/{avatarColor}",
                    arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                    val avatarColor = backStackEntry.arguments?.getString("avatarColor") ?: "ffffff"
                    val gameViewModel = remember {
                        GameViewModel(webSocketClient, deviceId = deviceId)
                    }
                    GameScreen(
                        webSocketClient = webSocketClient,
                        tasks = gameViewModel.tasks,
                        currentTaskIndex = gameViewModel.currentTaskIndex,
                        avatarColor = avatarColor,
                        deviceId=deviceId,
                        onTaskChange = { newIndex ->
                            gameViewModel.onTaskChange(newIndex)
                        },
                        onNavigateToTaskStatus = { deviceId, taskName,avatarColor ->
                            navController.navigate("taskStatusScreen/$deviceId/$taskName/$avatarColor") {
                                popUpTo("qrcodeScreen") { inclusive = true }
                                popUpTo("confirmationScreen") { inclusive = true }
                            }
                        }
                    )
                }


                composable("taskStatusScreen/{deviceId}/{taskName}/{avatarColor}") { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getString("deviceId") ?: "null"
                    val taskName = backStackEntry.arguments?.getString("taskName") ?: "null"
                    val avatarColor = backStackEntry.arguments?.getString("avatarColor") ?: "ffffff"
                    TaskStatusScreen(
                        avatarColor = avatarColor,
                        onCancelled = {
                            val message = mapOf("type" to "unactiveTask",
                                "from" to deviceId,
                                "to" to "unity"
                                )
                            webSocketClient.sendJson( message,  onResult = { success ->
                                if (success) {
                                    Log.d("WebSocket", "sent $message")
                                } else {
                                    Log.e("WebSocket", "Failed to send message")
                                }
                            })
                        },
                        onCompleted = {
                            val message = mapOf("type" to "taskFinished",
                                "from" to deviceId,
                                "to" to "unity"
                            )
                            webSocketClient.sendJson( message,  onResult = { success ->
                                if (success) {
                                    Log.d("WebSocket", "sent $message")
                                } else {
                                    Log.e("WebSocket", "Failed to send message")
                                }
                            })
                        },
                        onHelp = {
                            navController.navigate("taskScreen/$deviceId/$taskName") {
                                popUpTo("qrcodeScreen") { inclusive = true }
                                popUpTo("confirmationScreen") { inclusive = true }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }

                composable("taskScreen/{deviceId}/{taskName}") { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getString("deviceId") ?: "null"
                    val taskName = backStackEntry.arguments?.getString("taskName") ?: "null"
                    Log.d("Task Device id", "device id : $deviceId")
                    TaskScreen(
                        taskHelpService = taskHelpService,
                        cookManagementService = cookManagementService,
                        deviceId = deviceId,
                        taskName = taskName,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    private fun initializeServices() {
        initializeBasicServices()
        setupWebSocketConnection()
        //startBPMMonitoring()
    }

    private fun initializeBasicServices() {
        val deviceId = qrCodeGenerator.getCachedDeviceId()

        cookManagementService = CookManagementService(webSocketClient, deviceId)
        bpmService = BpmService(
            webSocketClient,
            getSystemService(Context.SENSOR_SERVICE) as SensorManager,
            baseContext
        )
        taskHelpService = TaskHelpService(webSocketClient, deviceId, baseContext)
    }

    private fun startBPMMonitoring() {
        lifecycleScope.launch {
            bpmService.bpmFlow.collect { bpm ->
                Log.d("MainActivity", "BPM Update: $bpm")
            }
        }
        bpmService.startMonitoring()
    }

    private fun setupWebSocketConnection() {
        webSocketClient.connect(
            onConnected = {
                Log.d("MainActivity", "WebSocket Connected")
            },
            onTaskMessage = { taskMessage ->
                Log.d("MainActivity", "Received task message: $taskMessage")
                taskHelpService.handleTaskMessage(taskMessage)
            },
            onMessage = { //message ->
                //Log.d("MainActivity", "Received non-task message: $message")
            },
            onError = { error ->
                Log.e("MainActivity", "WebSocket error: $error")
            }
        )
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

        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heartRateSensor == null) {
            Log.e("MainActivity", "No heart rate sensor found on this device")
            return
        } else {
            Log.d("MainActivity", "Heart rate sensor found: ${heartRateSensor.name}")
        }

        bpmService = BpmService(webSocketClient, sensorManager, baseContext)
        bpmService.startMonitoring()
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
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}