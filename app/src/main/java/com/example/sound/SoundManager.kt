package com.example.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    val amharicCaller: AmharicCaller = AmharicCaller(context)

    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true
    var isAmharicVoiceEnabled: Boolean
        get() = amharicCaller.isVoiceEnabled
        set(value) {
            amharicCaller.isVoiceEnabled = value
        }

    var isFastCallerSpeed: Boolean
        get() = amharicCaller.isFastSpeed
        set(value) {
            amharicCaller.isFastSpeed = value
            amharicCaller.applyVoiceProfile()
        }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing vibrator: ${e.message}")
            null
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing ToneGenerator: ${e.message}")
        }
    }

    fun playBallCall(ball: Int? = null) {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        } catch (_: Exception) {}

        if (ball != null && isAmharicVoiceEnabled) {
            amharicCaller.callBall(ball)
        }
    }

    fun playDaub() {
        if (isVibrationEnabled) {
            vibrate(30)
        }
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
        } catch (_: Exception) {}
    }

    fun playWin() {
        if (isVibrationEnabled) {
            vibratePattern(longArrayOf(0, 100, 80, 200, 100, 400))
        }
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
        } catch (_: Exception) {}

        if (isAmharicVoiceEnabled) {
            amharicCaller.callWin()
        }
    }

    fun playRoundStart() {
        if (isAmharicVoiceEnabled) {
            amharicCaller.callRoundStart()
        }
    }

    fun playButtonTap() {
        if (isVibrationEnabled) {
            vibrate(15)
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
            amharicCaller.release()
        } catch (_: Exception) {}
    }
}
