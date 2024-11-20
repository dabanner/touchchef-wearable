package com.touchchef.wearable.presentation

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.os.Build
import android.provider.Settings
import com.touchchef.wearable.DevicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QRCodeGenerator {
    private var devicePreferences: DevicePreferences? = null
    private var cachedDeviceId: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: android.content.Context) {
        if (devicePreferences == null) {
            devicePreferences = DevicePreferences(context)
            coroutineScope.launch {
                val savedId = devicePreferences?.deviceId?.first()
                cachedDeviceId = savedId ?: getUniqueDeviceIdentifier(context).also { newId ->
                    devicePreferences?.saveDeviceId(newId)
                }
            }
        }
    }

    fun generateDeviceQRCode(context: android.content.Context, size: Int): Bitmap {
        // Utiliser l'ID en cache ou le générer si nécessaire
        val deviceName = cachedDeviceId ?: getUniqueDeviceIdentifier(context)

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(deviceName, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        // Sauvegarder l'ID si ce n'est pas déjà fait
        if (cachedDeviceId == null) {
            coroutineScope.launch {
                devicePreferences?.saveDeviceId(deviceName)
                cachedDeviceId = deviceName
            }
        }

        return bitmap
    }

    private fun getUniqueDeviceIdentifier(context: android.content.Context): String {
        val deviceModel = Build.MODEL
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return "$deviceModel-$androidId"
    }

    // Méthode pour accéder au deviceId depuis d'autres classes
    suspend fun getDeviceId(context: android.content.Context): String {
        return cachedDeviceId ?: devicePreferences?.deviceId?.first() ?: getUniqueDeviceIdentifier(context).also { newId ->
            devicePreferences?.saveDeviceId(newId)
            cachedDeviceId = newId
        }
    }
}