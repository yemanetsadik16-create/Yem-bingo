package com.example.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

data class AmharicCallout(
    val ball: Int,
    val letter: String,
    val amharicLetter: String,
    val amharicNumberWord: String,
    val phoneticNumberWord: String,
    val amharicDisplayText: String,
    val spokenAmharicText: String,
    val phoneticSpokenText: String
)

class AmharicCaller(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false
    private var isAmharicLocaleSupported: Boolean = false
    private var toneGenerator: ToneGenerator? = null

    var isVoiceEnabled: Boolean = true
    var isFastSpeed: Boolean = true // Fast, energetic caller cadence
    var malePitch: Float = 0.85f   // Clear, resonant male caller tone

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (_: Exception) {}

        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("AmharicCaller", "Error initializing TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val amharicLocale = Locale.Builder().setLanguage("am").setRegion("ET").build()
            val availability = engine.isLanguageAvailable(amharicLocale)

            val targetLocale = if (availability >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = amharicLocale
                isAmharicLocaleSupported = true
                amharicLocale
            } else {
                engine.language = Locale.US
                isAmharicLocaleSupported = false
                Locale.US
            }

            selectMaleVoice(engine, targetLocale)
            applyVoiceProfile()
            isTtsReady = true

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })
        }
    }

    private fun selectMaleVoice(engine: TextToSpeech, targetLocale: Locale) {
        try {
            val voices = engine.voices ?: return
            val matchingVoices = voices.filter {
                it.locale.language.equals(targetLocale.language, ignoreCase = true)
            }
            if (matchingVoices.isEmpty()) return

            // Prefer offline/local clear male voices when labeled
            val preferredMaleVoice = matchingVoices
                .sortedBy { it.isNetworkConnectionRequired }
                .firstOrNull { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    val features = voice.features?.joinToString(",")?.lowercase(Locale.ROOT).orEmpty()
                    (name.contains("male") && !name.contains("female")) ||
                        features.contains("male") ||
                        name.contains("-iom-") ||
                        name.contains("-tpd-") ||
                        name.contains("-ame-") ||
                        name.contains("-gba-")
                } ?: matchingVoices
                .sortedBy { it.isNetworkConnectionRequired }
                .firstOrNull { !it.name.lowercase(Locale.ROOT).contains("female") }

            if (preferredMaleVoice != null) {
                engine.voice = preferredMaleVoice
            }
        } catch (e: Exception) {
            Log.d("AmharicCaller", "Voice selection fallback to default: ${e.message}")
        }
    }

    fun applyVoiceProfile() {
        val engine = tts ?: return
        try {
            // Clear male caller pitch and fast, crisp speech rate
            engine.setPitch(malePitch)
            val rate = if (isFastSpeed) 1.45f else 1.05f
            engine.setSpeechRate(rate)
        } catch (e: Exception) {
            Log.e("AmharicCaller", "Failed to apply voice profile: ${e.message}")
        }
    }

    fun callBall(ball: Int) {
        if (!isVoiceEnabled || !isTtsReady) return
        val callout = getCallout(ball)

        // Short crisp pre-alert tone
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
        } catch (_: Exception) {}

        applyVoiceProfile()

        val textToSpeak = if (isAmharicLocaleSupported) {
            callout.spokenAmharicText
        } else {
            // Crisp phonetic fallback when native Amharic TTS data is absent on the device
            callout.phoneticSpokenText
        }

        try {
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "call_$ball")
        } catch (e: Exception) {
            Log.e("AmharicCaller", "Failed to call ball $ball: ${e.message}")
        }
    }

    fun callWin() {
        if (!isVoiceEnabled || !isTtsReady) return
        applyVoiceProfile()

        val winPhrase = if (isAmharicLocaleSupported) {
            "ቢንጎ! አሸናፊ! እንኳን ደስ አለዎት!"
        } else {
            "Bingo! Ashenafi! Enkwan Des Alewot!"
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(winPhrase, TextToSpeech.QUEUE_FLUSH, null, "win_call")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(winPhrase, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (_: Exception) {}
    }

    fun callRoundStart() {
        if (!isVoiceEnabled || !isTtsReady) return
        applyVoiceProfile()

        val startPhrase = if (isAmharicLocaleSupported) {
            "አዲስ ዙር ተጀመረ! መልካም ዕድል!"
        } else {
            "Adis zur tejemere! Melkam idil!"
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(startPhrase, TextToSpeech.QUEUE_FLUSH, null, "start_call")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(startPhrase, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }

    companion object {
        fun getLetterForBall(number: Int): String {
            return when (number) {
                in 1..15 -> "B"
                in 16..30 -> "I"
                in 31..45 -> "N"
                in 46..60 -> "G"
                in 61..75 -> "O"
                else -> ""
            }
        }

        fun getAmharicLetter(letter: String): String {
            return when (letter.uppercase()) {
                "B" -> "ቢ"
                "I" -> "አይ"
                "N" -> "ኤን"
                "G" -> "ጂ"
                "O" -> "ኦ"
                else -> letter
            }
        }

        fun getPhoneticLetter(letter: String): String {
            return when (letter.uppercase()) {
                "B" -> "Bee"
                "I" -> "Eye"
                "N" -> "En"
                "G" -> "Jee"
                "O" -> "Oh"
                else -> letter
            }
        }

        fun getAmharicNumberWords(number: Int): String {
            if (number in 1..10) {
                return when (number) {
                    1 -> "አንድ"
                    2 -> "ሁለት"
                    3 -> "ሦስት"
                    4 -> "አራት"
                    5 -> "አምስት"
                    6 -> "ስድስት"
                    7 -> "ሰባት"
                    8 -> "ስምንት"
                    9 -> "ዘጠኝ"
                    10 -> "አስር"
                    else -> ""
                }
            }
            if (number in 11..19) {
                val ones = when (number % 10) {
                    1 -> "አንድ"
                    2 -> "ሁለት"
                    3 -> "ሦስት"
                    4 -> "አራት"
                    5 -> "አምስት"
                    6 -> "ስድስት"
                    7 -> "ሰባት"
                    8 -> "ስምንት"
                    9 -> "ዘጠኝ"
                    else -> ""
                }
                return "አስራ $ones"
            }

            val tens = number / 10
            val ones = number % 10

            val tensWord = when (tens) {
                2 -> "ሃያ"
                3 -> "ሰላሳ"
                4 -> "አርባ"
                5 -> "ሃምሳ"
                6 -> "ስልሳ"
                7 -> "ሰባ"
                else -> ""
            }

            if (ones == 0) return tensWord

            val onesWord = when (ones) {
                1 -> "አንድ"
                2 -> "ሁለት"
                3 -> "ሦስት"
                4 -> "አራት"
                5 -> "አምስት"
                6 -> "ስድስት"
                7 -> "ሰባት"
                8 -> "ስምንት"
                9 -> "ዘጠኝ"
                else -> ""
            }

            return "$tensWord $onesWord"
        }

        fun getPhoneticNumberWords(number: Int): String {
            if (number in 1..10) {
                return when (number) {
                    1 -> "And"
                    2 -> "Hulet"
                    3 -> "Sost"
                    4 -> "Arat"
                    5 -> "Amist"
                    6 -> "Sidist"
                    7 -> "Sebat"
                    8 -> "Simint"
                    9 -> "Zetegn"
                    10 -> "Asir"
                    else -> ""
                }
            }
            if (number in 11..19) {
                val ones = when (number % 10) {
                    1 -> "And"
                    2 -> "Hulet"
                    3 -> "Sost"
                    4 -> "Arat"
                    5 -> "Amist"
                    6 -> "Sidist"
                    7 -> "Sebat"
                    8 -> "Simint"
                    9 -> "Zetegn"
                    else -> ""
                }
                return "Asra $ones"
            }

            val tens = number / 10
            val ones = number % 10

            val tensWord = when (tens) {
                2 -> "Haya"
                3 -> "Selasa"
                4 -> "Arba"
                5 -> "Hamsa"
                6 -> "Silsa"
                7 -> "Seba"
                else -> ""
            }

            if (ones == 0) return tensWord

            val onesWord = when (ones) {
                1 -> "And"
                2 -> "Hulet"
                3 -> "Sost"
                4 -> "Arat"
                5 -> "Amist"
                6 -> "Sidist"
                7 -> "Sebat"
                8 -> "Simint"
                9 -> "Zetegn"
                else -> ""
            }

            return "$tensWord $onesWord"
        }

        fun getCallout(ball: Int): AmharicCallout {
            val letter = getLetterForBall(ball)
            val amharicLetter = getAmharicLetter(letter)
            val amharicNumber = getAmharicNumberWords(ball)
            val phoneticNumber = getPhoneticNumberWords(ball)
            val phoneticLetter = getPhoneticLetter(letter)

            return AmharicCallout(
                ball = ball,
                letter = letter,
                amharicLetter = amharicLetter,
                amharicNumberWord = amharicNumber,
                phoneticNumberWord = phoneticNumber,
                amharicDisplayText = "$amharicLetter - $ball ($amharicNumber)",
                spokenAmharicText = "$amharicLetter, $amharicNumber",
                phoneticSpokenText = "$phoneticLetter, $phoneticNumber"
            )
        }
    }
}
