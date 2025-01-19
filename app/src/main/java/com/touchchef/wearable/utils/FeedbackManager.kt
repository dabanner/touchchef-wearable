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

    fun playTaskChangeFeedback() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun playLongPressFeedback(){
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun playButtonPressFeedback(){
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun onTaskFeedback() {
        val timings = longArrayOf(0, 50, 50, 200)
        val amplitudes = intArrayOf(
            0,  // initial delay amplitude
            VibrationEffect.DEFAULT_AMPLITUDE,  // short vibration
            0,  // pause
            VibrationEffect.DEFAULT_AMPLITUDE   // longer vibration
        )
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun onEndGameFeedback() {
        val timings = longArrayOf(0, 100, 400, 100, 400, 600)
        val amplitudes = intArrayOf(
            0,  // initial delay amplitude
            VibrationEffect.DEFAULT_AMPLITUDE,  // first short whistle
            0,  // first pause
            VibrationEffect.DEFAULT_AMPLITUDE,  // second short whistle
            0,  // second pause
            VibrationEffect.DEFAULT_AMPLITUDE   // final long whistle
        )
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun onTaskSuccessFeedback() {
        val timings = longArrayOf(0, 200)
        val amplitudes = intArrayOf(
            0,  // initial delay amplitude
            VibrationEffect.DEFAULT_AMPLITUDE,  // short vibration
        )
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun onTaskRemoveFeedback() {
        val timings = longArrayOf(0, 50, 50, 50, 50, 50)
        val amplitudes = intArrayOf(
            0,  // initial delay amplitude
            VibrationEffect.DEFAULT_AMPLITUDE,  // short vibration
            0,  // pause
            VibrationEffect.DEFAULT_AMPLITUDE,   // longer vibration
            0,  // pause
            VibrationEffect.DEFAULT_AMPLITUDE   // longer vibration
        )
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun playStartFeedback() {
        // timings array: wait time before first vibration, first vibration duration, wait time, second vibration duration...
        // [0, 100, 100, 100, 100, 1000] means:
        // - wait 0ms
        // - vibrate 100ms
        // - wait 100ms
        // - vibrate 100ms
        // - wait 100ms
        // - vibrate 1000ms
        val timings = longArrayOf(100, 100, 100, 100, 1000)

        // amplitudes array must match the duration array length (except initial delay)
        // -1 would be default amplitude
        val amplitudes = intArrayOf(
            VibrationEffect.DEFAULT_AMPLITUDE,  // first short vibration
            0,  // pause
            VibrationEffect.DEFAULT_AMPLITUDE,  // second short vibration
            0,  // pause
            VibrationEffect.DEFAULT_AMPLITUDE   // final long vibration
        )

        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

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