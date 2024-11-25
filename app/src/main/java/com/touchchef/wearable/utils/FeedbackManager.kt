package com.touchchef.wearable.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.touchchef.wearable.R

class FeedbackManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun playSuccessFeedback() {
        val pattern = longArrayOf(0, 100, 100, 100)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))


        // Jouer le son
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, R.raw.success_sound)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer?.start()
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}