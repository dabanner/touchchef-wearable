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
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.touchchef.wearable.presentation.theme.TouchChefTheme
import com.touchchef.wearable.utils.FeedbackManager
import kotlin.math.log

class MainActivity : ComponentActivity() {
    private val webSocketClient = WebSocketClient()
    private lateinit var bpmService: BpmService
    private lateinit var qrCodeGenerator: QRCodeGenerator;
    private lateinit var handRaiseDetector: HandRaiseDetector
    private lateinit var taskHelpService: TaskHelpService
    private lateinit var cookManagementService: CookManagementService
    private lateinit var gameViewModel: GameViewModel
    private lateinit var feedbackManager: FeedbackManager
    private lateinit var navController: NavHostController

    private var activeTimer by mutableStateOf<Timer?>(null)

    private var showFullScreenTimer by mutableStateOf(false)

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

        feedbackManager = FeedbackManager(findViewById<View>(android.R.id.content))

        // Now we definitely have a deviceId
        webSocketClient.initialize(deviceId)
        initializeServices()

        webSocketClient.addMessageListener { message ->
            if (message.to == deviceId && message.type == "addTimer" && message.timer != null) {
                val duration = message.timer.timerDuration.toIntOrNull()
                if (duration != null) {
                    runOnUiThread {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        activeTimer = message.timer
                        showFullScreenTimer = true
                    }
                }
            }

            if (message.type == "endGame" && ::navController.isInitialized) {
                runOnUiThread {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    val currentBackStack = navController.currentBackStackEntry

                    // Log the entire backstack arguments
                    Log.d("Navigation", "Current backstack arguments: ${currentBackStack?.arguments}")

                    val color = currentBackStack?.arguments?.getString("avatarColor") ?: "FFFC403"
                    val name = currentBackStack?.arguments?.getString("name") ?: "Bravo!"
                    val avatar = currentBackStack?.arguments?.getString("avatar") ?: "1"

                    // Log individual values
                    Log.d("Navigation", "Retrieved values - color: $color, name: $name, avatar: $avatar")
                    Log.d("Navigation", "Navigating to: raiseHandScreen/$name/$avatar/$color")

                    navController.navigate("raiseHandScreen/$name/$avatar/$color")
                }
            }

            if(message.type=="stop_game" && ::navController.isInitialized){
                runOnUiThread{
                    val currentBackStack = navController.currentBackStackEntry
                    val color = currentBackStack?.arguments?.getString("avatarColor") ?: "FFFC403"
                    feedbackManager.onEndGameFeedback()

                    navController.navigate("endGameScreen/$color")

                }
            }
        }

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

    private fun performHapticFeedback(feedbackType: Int) {
        val view = window.decorView
        view.performHapticFeedback( feedbackType, )
    }

    private fun setupNavigation() {
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            TouchChefTheme {

                navController = rememberNavController()
                // Initialize the detector
                handRaiseDetector = HandRaiseDetector(this, webSocketClient)

                // Start detection
                handRaiseDetector.startDetecting()

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "qrcodeScreen") {
                        composable("qrcodeScreen") {
                            WebSocketQRCode(
                                webSocketClient = webSocketClient,
                                context = baseContext,
                                qrCodeGenerator = qrCodeGenerator,
                                navigateToConfirmationScreen = { name, avatar, deviceId, avatarColor ->
                                    val avatarColorFix = avatarColor.replace("#", "")
                                    Log.d(
                                        "MainActivity",
                                        "Navigating to confirmation screen with name: $name, avatar: $avatar, deviceId: $deviceId, avatarColor: $avatarColorFix"
                                    )
                                    runOnUiThread {
                                        navController.navigate("confirmationScreen/$name/$avatar/$deviceId/$avatarColorFix")
                                    }
                                }
                            )
                        }

                        composable(
                            route = "confirmationScreen/{name}/{avatar}/{deviceId}/{avatarColor}",
                            arguments = listOf(
                                navArgument("name") { type = NavType.StringType },
                                navArgument("avatar") { type = NavType.StringType },
                                navArgument("deviceId") { type = NavType.StringType },
                                navArgument("avatarColor") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: "Inconnu"
                            val avatar = backStackEntry.arguments?.getString("avatar") ?: "one.png"
                            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: "null"
                            val avatarColor =
                                backStackEntry.arguments?.getString("avatarColor") ?: "ffffff"

                            ConfirmationScreen(
                                webSocketClient,
                                feedbackManager,
                                deviceId = deviceId,
                                name = name,
                                avatar = avatar,
                                avatarColor = avatarColor,
                                navigateToGameScreen = {
                                    Log.d("MainActivity", "Navigating to confirmation screen")
                                    feedbackManager.playStartFeedback()
                                    runOnUiThread {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("name", name)
                                        navController.currentBackStackEntry?.savedStateHandle?.set("avatar", avatar)
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
                            arguments = listOf(
                                navArgument("deviceId") {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                            val avatarColor =
                                backStackEntry.arguments?.getString("avatarColor") ?: "ffffff"
                            if (!::gameViewModel.isInitialized) {
                                gameViewModel = GameViewModel(webSocketClient, feedbackManager, deviceId)
                            }
                            GameScreen(
                                webSocketClient = webSocketClient,
                                tasks = gameViewModel.tasks,
                                currentTaskIndex = gameViewModel.currentTaskIndex,
                                avatarColor = avatarColor,
                                deviceId = deviceId,
                                navController = navController,
                                onTaskChange = { newIndex ->
                                    gameViewModel.onTaskChange(newIndex)
                                },
                                onPopTask = {
                                    gameViewModel.popActiveTask()
                                },
                                feedbackManager = feedbackManager
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

                        composable(
                            route = "raiseHandScreen/{name}/{avatar}/{color}",
                            arguments = listOf(
                                navArgument("name") { type = NavType.StringType },
                                navArgument("avatar") { type = NavType.StringType },
                                navArgument("color") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: "Bravo !"
                            val avatar = backStackEntry.arguments?.getString("avatar") ?: "1"
                            val color = backStackEntry.arguments?.getString("color") ?: "FFFC403"

                            RaiseHandScreen(
                                name = name,
                                avatar = avatar,
                                backgroundColor = color,
                                onDismiss = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = "endGameScreen/{color}",
                        ){ backStackEntry ->
                            val color = backStackEntry.arguments?.getString("color") ?: "FFFC403"
                            EndGameScreen(
                                backgroundColor = color,
                            )
                        }
                    }

                    activeTimer?.let { seconds ->
                        if (showFullScreenTimer) {
                            // Show full screen timer for initial acceptance
                                CallStyleTimer(
                                    numOfSeconds = seconds.timerDuration.toInt(),
                                    onTimerStart = {
                                        webSocketClient.sendJson(mapOf(
                                            "type" to "timerStart",
                                            "to" to "angular",
                                            "timerId" to seconds.timerId
                                        )) { success ->
                                            Log.d("Timer", "Timer start message sent: $success")
                                        }
                                        showFullScreenTimer = false
                                    },
                                    onTimerEnd = {
                                        webSocketClient.sendJson(mapOf(
                                            "type" to "timerRefuse",
                                            "to" to "angular",
                                            "timerId" to seconds.timerId
                                        )) { success ->
                                            Log.d("Timer", "Timer start message sent: $success")
                                        }
                                        showFullScreenTimer = false
                                        activeTimer = null
                                    }
                                )
                        } else {
                            // Show circular overlay timer
                            CircularTimerOverlay(
                                seconds = seconds.timerDuration.toInt(),
                                onTimerComplete = {
                                    webSocketClient.sendJson(mapOf(
                                        "type" to "timerFinish",
                                        "to" to "angular",
                                        "timerId" to seconds.timerId
                                    )) { success ->
                                        Log.d("Timer", "Timer finish message sent: $success")
                                    }
                                    activeTimer = null
                                }
                            )
                        }
                    }


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