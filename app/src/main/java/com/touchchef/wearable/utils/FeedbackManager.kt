package com.touchchef.wearable.utils

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import com.touchchef.wearable.R

class FeedbackManager(private val view: View) {
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator = view.context.getSystemService(Vibrator::class.java)

    fun playSuccessFeedback() {
        val pattern = longArrayOf(0, 100, 100, 100)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))

        releaseMediaPlayer()

        try {
            MediaPlayer.create(view.context, R.raw.success_sound)?.apply {
                mediaPlayer = this
                setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "MediaPlayer in illegal state", e)
        }
    }

    fun release() {
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "FeedbackManager"
    }
}