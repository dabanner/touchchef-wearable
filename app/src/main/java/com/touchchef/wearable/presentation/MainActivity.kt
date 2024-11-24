/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.touchchef.wearable.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    private val webSocketClient = WebSocketClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "qrcodeScreen") {
                composable("qrcodeScreen") {
                    WebSocketQRCode(
                        webSocketClient = webSocketClient,
                        context = baseContext,
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
                    Log.d("MainActivity", "Navigating to confirmation screen")
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
}