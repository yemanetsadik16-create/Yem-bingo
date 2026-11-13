package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.AuthManager
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

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

enum class CallerEvent(val key: String, val amharicPhrase: String) {
    GAME_START("event_game_start", "ጨዋታው ተጀምሯል! መልካም ዕድል!"),
    BINGO_WIN("event_bingo_win", "ቢንጎ! እንኳን ደስ አለዎት፣ አሸናፊ አግኝተናል!")
}

private data class CachedAudio(
    val bytes: ByteArray,
    val mimeType: String,
    val phrase: String
)

class AmharicCaller(
    private val context: Context,
    @Volatile var voiceName: String = "Iapetus"
) {

    companion object {
        private const val TAG = "AmharicCaller"
        private const val TTS_MODEL_NAME = "gemini-3.1-flash-tts-preview"
        private const val AI_TIMEOUT_MS = 30_000L
        private const val DEFAULT_SAMPLE_RATE = 24_000

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
                amharicDisplayText = "$amharicLetter $amharicNumber! ($letter-$ball)",
                spokenAmharicText = "$amharicLetter $amharicNumber!",
                phoneticSpokenText = "$phoneticLetter, $phoneticNumber"
            )
        }

        fun buildExpressiveSpokenPhrase(ball: Int, callIndex: Int): String {
            val core = getCallout(ball).spokenAmharicText
            return when {
                callIndex > 1 && callIndex % 4 == 2 -> "ቀጣዩ ቁጥር... $core"
                callIndex in 5..9 && callIndex % 4 == 0 -> "$core አሸናፊ ማን ይሆን?"
                callIndex >= 10 && callIndex % 4 == 0 -> "$core ቢንጎ ተቃርቧል!"
                else -> core
            }
        }

        /**
         * Decomposes any 75-ball Bingo callout into a tiny 22-word reusable Amharic vocabulary:
         * 5 letters (ቢ, አይ, ኤን, ጂ, ኦ) + 10 units (1..10) + "አስራ" + 6 tens (20..70).
         */
        fun getAtomicComponentsForBall(ball: Int): List<Pair<String, String>> {
            val letter = getLetterForBall(ball)
            val amharicLetter = getAmharicLetter(letter)
            val components = mutableListOf("atom_letter_$letter" to amharicLetter)

            when {
                ball in 1..10 -> {
                    components.add("atom_unit_$ball" to getAmharicNumberWords(ball))
                }
                ball in 11..19 -> {
                    val unit = ball % 10
                    components.add("atom_prefix_asra" to "አስራ")
                    components.add("atom_unit_$unit" to getAmharicNumberWords(unit))
                }
                ball in 20..75 -> {
                    val tens = (ball / 10) * 10
                    val unit = ball % 10
                    val tensWord = when (tens) {
                        20 -> "ሃያ"
                        30 -> "ሰላሳ"
                        40 -> "አርባ"
                        50 -> "ሃምሳ"
                        60 -> "ስልሳ"
                        70 -> "ሰባ"
                        else -> ""
                    }
                    if (tensWord.isNotEmpty()) {
                        components.add("atom_tens_$tens" to tensWord)
                    }
                    if (unit > 0) {
                        components.add("atom_unit_$unit" to getAmharicNumberWords(unit))
                    }
                }
            }
            return components
        }

        fun getAllAtomicVocabulary(): List<Pair<String, String>> {
            val list = mutableListOf<Pair<String, String>>()
            list.add("atom_letter_B" to "ቢ")
            list.add("atom_letter_I" to "አይ")
            list.add("atom_letter_N" to "ኤን")
            list.add("atom_letter_G" to "ጂ")
            list.add("atom_letter_O" to "ኦ")
            for (u in 1..10) {
                list.add("atom_unit_$u" to getAmharicNumberWords(u))
            }
            list.add("atom_prefix_asra" to "አስራ")
            list.add("atom_tens_20" to "ሃያ")
            list.add("atom_tens_30" to "ሰላሳ")
            list.add("atom_tens_40" to "አርባ")
            list.add("atom_tens_50" to "ሃምሳ")
            list.add("atom_tens_60" to "ስልሳ")
            list.add("atom_tens_70" to "ሰባ")
            return list
        }

        internal fun trimSilence16BitMono(pcm: ByteArray, sampleRate: Int): ByteArray {
            val alignedLen = pcm.size - (pcm.size % 2)
            if (alignedLen < 4) return pcm
            val sampleCount = alignedLen / 2
            val threshold = 220

            var firstActive = 0
            while (firstActive < sampleCount) {
                val lo = pcm[firstActive * 2].toInt() and 0xFF
                val hi = pcm[firstActive * 2 + 1].toInt()
                val sample = (hi shl 8) or lo
                if (kotlin.math.abs(sample) >= threshold) break
                firstActive++
            }
            if (firstActive >= sampleCount) return pcm

            var lastActive = sampleCount - 1
            while (lastActive > firstActive) {
                val lo = pcm[lastActive * 2].toInt() and 0xFF
                val hi = pcm[lastActive * 2 + 1].toInt()
                val sample = (hi shl 8) or lo
                if (kotlin.math.abs(sample) >= threshold) break
                lastActive--
            }

            val padBefore = (sampleRate * 35) / 1000 // 35ms lead protects soft initial consonants
            val padAfter = (sampleRate * 55) / 1000  // 55ms tail preserves natural breath release
            val startSample = maxOf(0, firstActive - padBefore)
            val endSample = minOf(sampleCount, lastActive + 1 + padAfter)
            if (endSample <= startSample) return pcm

            return pcm.copyOfRange(startSample * 2, endSample * 2)
        }

        internal fun applyFadeEnvelope16BitMono(
            pcm: ByteArray,
            sampleRate: Int,
            fadeInMs: Int = 10,
            fadeOutMs: Int = 16
        ): ByteArray {
            val alignedLen = pcm.size - (pcm.size % 2)
            if (alignedLen < 4) return pcm
            val out = pcm.copyOf(alignedLen)
            val totalSamples = alignedLen / 2
            val fadeInSamples = minOf((sampleRate * fadeInMs) / 1000, totalSamples / 2)
            val fadeOutSamples = minOf((sampleRate * fadeOutMs) / 1000, totalSamples / 2)

            for (i in 0 until fadeInSamples) {
                val gain = 0.5 * (1.0 - kotlin.math.cos(PI * i / fadeInSamples.coerceAtLeast(1)))
                val lo = out[i * 2].toInt() and 0xFF
                val hi = out[i * 2 + 1].toInt()
                val sample = (hi shl 8) or lo
                val scaled = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[i * 2] = (scaled and 0xFF).toByte()
                out[i * 2 + 1] = ((scaled shr 8) and 0xFF).toByte()
            }

            for (i in 0 until fadeOutSamples) {
                val idx = totalSamples - 1 - i
                val gain = 0.5 * (1.0 - kotlin.math.cos(PI * i / fadeOutSamples.coerceAtLeast(1)))
                val lo = out[idx * 2].toInt() and 0xFF
                val hi = out[idx * 2 + 1].toInt()
                val sample = (hi shl 8) or lo
                val scaled = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[idx * 2] = (scaled and 0xFF).toByte()
                out[idx * 2 + 1] = ((scaled shr 8) and 0xFF).toByte()
            }
            return out
        }

        internal fun stitchPcmClips(clips: List<ByteArray>, sampleRate: Int, gapMs: Int = 22): ByteArray {
            if (clips.isEmpty()) return ByteArray(0)
            val smoothed = clips.map { clip ->
                applyFadeEnvelope16BitMono(
                    pcm = trimSilence16BitMono(clip, sampleRate),
                    sampleRate = sampleRate
                )
            }
            val gapBytes = ((sampleRate * gapMs) / 1000) * 2
            val out = ByteArrayOutputStream()
            val silence = ByteArray(gapBytes)
            smoothed.forEachIndexed { index, clip ->
                if (index > 0 && gapBytes > 0) {
                    out.write(silence)
                }
                out.write(clip)
            }
            return out.toByteArray()
        }
    }

    private val callerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playbackExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AmharicAudioPlayback").apply { isDaemon = true }
    }
    private val playbackDispatcher = playbackExecutor.asCoroutineDispatcher()
    // Max 2 concurrent Gemini TTS requests; live ball calls cancel/pause background warming immediately.
    private val fetchSemaphore = Semaphore(2)
    private val callGeneration = AtomicLong(0L)
    @Volatile
    private var atomicWarmJob: kotlinx.coroutines.Job? = null
    @Volatile
    private var geminiBlockedUntilMs: Long = 0L
    @Volatile
    private var geminiBlockedMessage: String? = null
    private fun diskCacheDirForCurrentVoice(): File? {
        return try {
            // Remove older voice cache directories once so updated clear male Amharic voice clips generate cleanly
            listOf(
                "amharic_voice_cache_puck",
                "amharic_voice_cache_fenrir",
                "amharic_voice_cache_v3_charon",
                "amharic_voice_cache_v3_fenrir",
                "amharic_voice_cache_v4_puck",
                "amharic_voice_cache_v5_puck",
                "amharic_voice_cache_v5_charon",
                "amharic_voice_cache_v5_aoede"
            ).forEach { oldName ->
                val oldDir = File(context.filesDir, oldName)
                if (oldDir.exists()) oldDir.deleteRecursively()
            }
            File(context.filesDir, "amharic_voice_cache_v6_${voiceName.lowercase()}").apply {
                if (!exists()) mkdirs()
            }
        } catch (_: Exception) {
            null
        }
    }

    // Lazy/reconfigurable Gemini TTS model configured natively for Ethiopian Amharic (am-ET).
    @OptIn(PublicPreviewAPI::class)
    @Volatile
    private var cachedTtsModelVoice: String? = null
    @Volatile
    private var cachedTtsModel: GenerativeModel? = null
    private val modelLock = Any()

    @OptIn(PublicPreviewAPI::class)
    private fun getOrCreateTtsModel(): GenerativeModel {
        val currentVoice = voiceName
        val existing = cachedTtsModel
        if (existing != null && cachedTtsModelVoice == currentVoice) {
            return existing
        }
        return synchronized(modelLock) {
            val rechecked = cachedTtsModel
            if (rechecked != null && cachedTtsModelVoice == currentVoice) {
                rechecked
            } else {
                Firebase.ai.generativeModel(
                    modelName = TTS_MODEL_NAME,
                    generationConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.AUDIO)
                        speechConfig = SpeechConfig(
                            voice = Voice(currentVoice),
                            languageCode = "am-ET"
                        )
                    },
                    systemInstruction = content {
                        text(
                            "You are a clear, articulate, confident Ethiopian male Bingo announcer in Addis Ababa speaking fluent, standard Amharic. Enunciate every Amharic letter and number crisply, naturally, and warmly with zero English accent or English words."
                        )
                    }
                ).also {
                    cachedTtsModelVoice = currentVoice
                    cachedTtsModel = it
                }
            }
        }
    }

    private val audioCache = ConcurrentHashMap<String, CachedAudio>()
    private val inflightFetches = ConcurrentHashMap<String, Deferred<CachedAudio?>>()
    private val playbackLock = Any()

    private var activeAudioTrack: AudioTrack? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    @Volatile
    private var fallbackTts: TextToSpeech? = null
    @Volatile
    private var isFallbackTtsReady: Boolean = false
    @Volatile
    private var isAmharicTtsSupported: Boolean = false

    @Volatile
    var isVoiceEnabled: Boolean = true

    @Volatile
    var isFastSpeed: Boolean = true

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            field = value
            updateLiveVolume()
        }

    @Volatile
    var volume: Float = 0.9f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateLiveVolume()
        }

    var onCallerStatusChanged: ((status: String, errorMessage: String?) -> Unit)? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Exception) {}

        initFallbackTtsSafely()
    }

    private fun initFallbackTtsSafely() {
        try {
            fallbackTts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = fallbackTts ?: return@TextToSpeech
                    try {
                        val amharicLocale = Locale.forLanguageTag("am-ET")
                        val amResult = engine.setLanguage(amharicLocale)
                        if (amResult == TextToSpeech.LANG_MISSING_DATA || amResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            isAmharicTtsSupported = false
                            engine.setLanguage(Locale.US)
                        } else {
                            isAmharicTtsSupported = true
                        }
                        selectMaleTtsVoice(engine, isAmharicTtsSupported)
                        engine.setPitch(0.96f)
                        engine.setSpeechRate(if (isFastSpeed) 1.06f else 0.96f)
                        isFallbackTtsReady = true
                    } catch (_: Exception) {
                        isFallbackTtsReady = false
                        isAmharicTtsSupported = false
                    }
                }
            }
        } catch (_: Exception) {
            fallbackTts = null
            isFallbackTtsReady = false
        }
    }

    private fun selectMaleTtsVoice(engine: TextToSpeech, preferAmharic: Boolean) {
        try {
            val voices = engine.voices ?: return
            val targetLang = if (preferAmharic) "am" else "en"
            val candidates = voices.filter {
                !it.isNetworkConnectionRequired &&
                    it.locale?.language.equals(targetLang, ignoreCase = true)
            }.ifEmpty {
                voices.filter { it.locale?.language.equals(targetLang, ignoreCase = true) }
            }.sortedByDescending { it.quality }
            val maleVoice = candidates.firstOrNull { v ->
                val n = v.name.lowercase()
                n.contains("male") ||
                    n.contains("-m-") ||
                    n.contains("-b-") ||
                    n.contains("-d-") ||
                    n.contains("iom") ||
                    n.contains("tpd") ||
                    n.contains("yol")
            } ?: candidates.firstOrNull { v ->
                val n = v.name.lowercase()
                !n.contains("female") && !n.contains("-f-") && !n.contains("sfg")
            }
            if (maleVoice != null) {
                engine.voice = maleVoice
            }
        } catch (_: Exception) {}
    }

    fun applyVoiceProfile() {
        updateLiveVolume()
        try {
            fallbackTts?.setPitch(0.96f)
            fallbackTts?.setSpeechRate(if (isFastSpeed) 1.06f else 0.96f)
        } catch (_: Exception) {}
    }

    fun effectiveGain(): Float {
        return if (isMuted || !isVoiceEnabled) 0f else volume.coerceIn(0f, 1f)
    }

    private fun updateLiveVolume() {
        val gain = effectiveGain()
        synchronized(playbackLock) {
            try {
                activeAudioTrack?.setVolume(gain)
            } catch (_: Exception) {}
            try {
                activeMediaPlayer?.setVolume(gain, gain)
            } catch (_: Exception) {}
            if (gain <= 0f) {
                try {
                    fallbackTts?.stop()
                } catch (_: Exception) {}
            }
        }
    }

    fun warmAtomicVocabulary() {
        if (!isVoiceEnabled || !AuthManager.isFirebaseInitialized(context)) return
        if (android.os.SystemClock.elapsedRealtime() < geminiBlockedUntilMs) return
        if (atomicWarmJob?.isActive == true) return
        atomicWarmJob = callerScope.launch {
            for ((key, phrase) in getAllAtomicVocabulary()) {
                if (!isVoiceEnabled || android.os.SystemClock.elapsedRealtime() < geminiBlockedUntilMs) break
                if (loadFromMemoryOrDisk(key) != null) continue
                // Yield briefly if live ball calls are waiting so upcoming round numbers always get priority
                while (inflightFetches.keys.any { it.startsWith("ball_") }) {
                    delay(300L)
                }
                try {
                    val job = launchPrefetch(key, phrase, phrase)
                    job?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
                delay(200L)
            }
        }
    }

    fun setVoiceProfile(newVoiceName: String) {
        if (newVoiceName.isBlank() || newVoiceName == voiceName) return
        voiceName = newVoiceName
        clearSessionCache()
        audioCache.clear()
        onCallerStatusChanged?.invoke("Amharic Caller ($voiceName)", null)
    }

    fun previewAmharicVoice() {
        if (!isVoiceEnabled || isMuted) return
        callRoundStart()
    }

    private fun scopedCacheKey(rawKey: String): String = "${voiceName.lowercase()}_$rawKey"

    private fun loadFromMemoryOrDisk(cacheKey: String): CachedAudio? {
        val scopedKey = scopedCacheKey(cacheKey)
        audioCache[scopedKey]?.let { return it }
        val dir = diskCacheDirForCurrentVoice() ?: return null
        val dataFile = File(dir, "$cacheKey.bin")
        val metaFile = File(dir, "$cacheKey.mime")
        if (!dataFile.exists() || dataFile.length() <= 0L) return null
        return try {
            val bytes = dataFile.readBytes()
            val mime = if (metaFile.exists()) metaFile.readText().trim() else "audio/L16;codec=pcm;rate=$DEFAULT_SAMPLE_RATE"
            if (bytes.isNotEmpty()) {
                CachedAudio(bytes = bytes, mimeType = mime, phrase = cacheKey).also {
                    audioCache[scopedKey] = it
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToDisk(cacheKey: String, clip: CachedAudio) {
        val dir = diskCacheDirForCurrentVoice() ?: return
        try {
            File(dir, "$cacheKey.mime").writeText(clip.mimeType)
            File(dir, "$cacheKey.bin").writeBytes(clip.bytes)
        } catch (_: Exception) {}
    }

    private fun extractPcmFromCached(clip: CachedAudio): Pair<ByteArray, Int>? {
        val lowerMime = clip.mimeType.lowercase()
        val bytes = clip.bytes
        val isWavHeader = bytes.size > 44 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()
        if (isWavHeader) {
            val info = parseWavHeader(bytes) ?: return null
            if (info.channelMask != AudioFormat.CHANNEL_OUT_MONO) return null
            val end = minOf(bytes.size, info.pcmOffset + info.dataLength)
            if (end <= info.pcmOffset) return null
            return bytes.copyOfRange(info.pcmOffset, end) to info.sampleRate
        }
        if (lowerMime.contains("pcm") || lowerMime.contains("l16") || lowerMime.isBlank()) {
            val rate = parseSampleRateFromMime(lowerMime) ?: DEFAULT_SAMPLE_RATE
            return bytes to rate
        }
        return null
    }

    private fun tryAssembleAmharicBallFromAtoms(ball: Int): CachedAudio? {
        val components = getAtomicComponentsForBall(ball)
        val pcmParts = mutableListOf<ByteArray>()
        var commonSampleRate: Int? = null
        for ((atomKey, _) in components) {
            val cachedAtom = loadFromMemoryOrDisk(atomKey) ?: return null
            val (pcm, rate) = extractPcmFromCached(cachedAtom) ?: return null
            if (commonSampleRate == null) {
                commonSampleRate = rate
            } else if (commonSampleRate != rate) {
                return null
            }
            pcmParts.add(pcm)
        }
        val rate = commonSampleRate ?: DEFAULT_SAMPLE_RATE
        val stitched = stitchPcmClips(pcmParts, rate, gapMs = 22)
        if (stitched.size < 4) return null
        return CachedAudio(
            bytes = stitched,
            mimeType = "audio/L16;codec=pcm;rate=$rate",
            phrase = getCallout(ball).spokenAmharicText
        )
    }

    fun pauseBackgroundWarming() {
        atomicWarmJob?.cancel()
    }

    fun prefetchBall(ball: Int, callIndex: Int) {
        if (!isVoiceEnabled) return
        val key = "ball_$ball"
        if (loadFromMemoryOrDisk(key) != null) return
        val expressivePhrase = buildExpressiveSpokenPhrase(ball, callIndex)
        val corePhrase = getCallout(ball).spokenAmharicText
        launchPrefetch(key, expressivePhrase, corePhrase)
    }

    fun prefetchEvent(event: CallerEvent) {
        if (!isVoiceEnabled) return
        if (loadFromMemoryOrDisk(event.key) != null) return
        launchPrefetch(event.key, event.amharicPhrase, event.amharicPhrase)
    }

    private fun isAttestationFailure(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val lower = message.lowercase()
        return lower.contains("403") ||
            lower.contains("app check") ||
            lower.contains("appcheck") ||
            lower.contains("attestation") ||
            lower.contains("permission_denied") ||
            lower.contains("unauthenticated")
    }

    private fun launchPrefetch(cacheKey: String, primaryPhrase: String, fallbackPhrase: String): Deferred<CachedAudio?>? {
        if (!AuthManager.isFirebaseInitialized(context)) {
            onCallerStatusChanged?.invoke(
                "Voice Backup Mode",
                "Add google-services.json for Gemini Amharic audio"
            )
            return null
        }

        if (android.os.SystemClock.elapsedRealtime() < geminiBlockedUntilMs) {
            onCallerStatusChanged?.invoke("Voice Backup Mode", geminiBlockedMessage)
            return null
        }

        return inflightFetches.computeIfAbsent(cacheKey) {
            callerScope.async {
                try {
                    fetchSemaphore.withPermit {
                        loadFromMemoryOrDisk(cacheKey)?.let { return@withPermit it }

                        var lastError: String? = null
                        val isAtomicClip = cacheKey.startsWith("atom_")
                        val attempts = if (primaryPhrase == fallbackPhrase) {
                            listOf(
                                buildHostSpeechPrompt(primaryPhrase, isAtomicClip = isAtomicClip, structured = true),
                                buildHostSpeechPrompt(primaryPhrase, isAtomicClip = isAtomicClip, structured = false)
                            )
                        } else {
                            listOf(
                                buildHostSpeechPrompt(primaryPhrase, isAtomicClip = isAtomicClip, structured = true),
                                buildHostSpeechPrompt(fallbackPhrase, isAtomicClip = isAtomicClip, structured = false)
                            )
                        }
                        val model = getOrCreateTtsModel()
                        for ((attemptIdx, prompt) in attempts.withIndex()) {
                            if (attemptIdx > 0) {
                                delay(250L)
                            }
                            try {
                                val response = withTimeout(9_000L) {
                                    model.generateContent(prompt)
                                }
                                val audioPart = response.inlineDataParts.firstOrNull {
                                    it.mimeType.startsWith("audio/", ignoreCase = true)
                                } ?: response.inlineDataParts.firstOrNull()

                                if (audioPart != null && audioPart.inlineData.isNotEmpty()) {
                                    geminiBlockedUntilMs = 0L
                                    geminiBlockedMessage = null
                                    val clip = CachedAudio(
                                        bytes = audioPart.inlineData,
                                        mimeType = audioPart.mimeType,
                                        phrase = if (attemptIdx == 0) primaryPhrase else fallbackPhrase
                                    )
                                    audioCache[scopedCacheKey(cacheKey)] = clip
                                    saveToDisk(cacheKey, clip)
                                    onCallerStatusChanged?.invoke("Gemini Amharic ($voiceName) Ready", null)
                                    return@withPermit clip
                                } else {
                                    lastError = "Gemini audio model returned no audio data."
                                    Log.w(TAG, "Empty audio part on attempt ${attemptIdx + 1} for $cacheKey")
                                }
                            } catch (e: TimeoutCancellationException) {
                                lastError = "Amharic voice generation timed out."
                                Log.w(TAG, "Gemini TTS attempt ${attemptIdx + 1} timed out for $cacheKey")
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                lastError = e.localizedMessage ?: "Gemini Amharic voice unavailable."
                                Log.w(TAG, "Gemini TTS attempt ${attemptIdx + 1} failed for $cacheKey: $lastError")
                                if (isAttestationFailure(lastError)) {
                                    val readableError = "Gemini voice blocked (App Check 403 on unregistered debug build). Playing audio backup."
                                    geminiBlockedUntilMs = android.os.SystemClock.elapsedRealtime() + 60_000L
                                    geminiBlockedMessage = readableError
                                    onCallerStatusChanged?.invoke("Voice Backup Mode", readableError)
                                    return@withPermit null
                                }
                            }
                        }

                        onCallerStatusChanged?.invoke(
                            "Gemini Amharic • Atom Fallback",
                            lastError
                        )
                        null
                    }
                } finally {
                    inflightFetches.remove(cacheKey)
                }
            }
        }
    }

    private fun buildHostSpeechPrompt(
        amharicPhrase: String,
        isAtomicClip: Boolean = false,
        structured: Boolean = true
    ): String {
        if (!structured) {
            return amharicPhrase
        }
        if (isAtomicClip) {
            return """
                # DIRECTOR'S NOTES
                Language: Amharic (Ethiopia, Addis Ababa)
                Delivery: Crisp, clear, articulate male announcer speaking a single word naturally without rushing.

                #### TRANSCRIPT
                $amharicPhrase
            """.trimIndent()
        }
        val tag = if (isFastSpeed) "[clear, energetic]" else "[clear, steady]"
        return """
            # AUDIO PROFILE: Addis Ababa Male Bingo Caller
            Clear, confident, articulate adult Ethiopian male game-show announcer speaking standard Amharic.

            # DIRECTOR'S NOTES
            Language: Standard Addis Ababa Amharic (am-ET)
            Style: Crisp, warm, natural male broadcast voice with clear syllable pronunciation under 3.5 seconds. Speak ONLY the transcript text in Amharic.

            #### TRANSCRIPT
            $tag $amharicPhrase
        """.trimIndent()
    }

    fun callBall(ball: Int, callIndex: Int = 1) {
        if (!isVoiceEnabled || isMuted) return
        val myGen = callGeneration.incrementAndGet()
        val key = "ball_$ball"

        // 1. Zero-latency hit from memory or persistent disk cache
        val cached = loadFromMemoryOrDisk(key)
        if (cached != null) {
            callerScope.launch(playbackDispatcher) {
                if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                    playAudioBufferBlocking(cached.bytes, cached.mimeType, myGen)
                }
            }
            onCallerStatusChanged?.invoke("Gemini Amharic ($voiceName) • Live", null)
            return
        }

        // 2. If Gemini is temporarily blocked by App Check/offline, speak immediately without stalling
        if (android.os.SystemClock.elapsedRealtime() < geminiBlockedUntilMs) {
            onCallerStatusChanged?.invoke("Voice Backup Mode", geminiBlockedMessage)
            speakFallbackBall(ball, myGen)
            return
        }

        // 3. Start or join in-flight Gemini TTS fetch
        if (inflightFetches[key] == null) {
            prefetchBall(ball, callIndex)
        }
        onCallerStatusChanged?.invoke("Buffering Amharic Voice...", null)

        callerScope.launch {
            // Wait up to 2.6s for natural full-sentence Gemini clip
            val fastClip = try {
                withTimeoutOrNull(2_600L) {
                    inflightFetches[key]?.await() ?: loadFromMemoryOrDisk(key)
                } ?: loadFromMemoryOrDisk(key)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                loadFromMemoryOrDisk(key)
            }

            if (callGeneration.get() != myGen || isMuted || !isVoiceEnabled) return@launch

            if (fastClip != null) {
                onCallerStatusChanged?.invoke("Gemini Amharic ($voiceName) • Live", null)
                callerScope.launch(playbackDispatcher) {
                    if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                        playAudioBufferBlocking(fastClip.bytes, fastClip.mimeType, myGen)
                    }
                }
                return@launch
            }

            // 4. Transient stitched Amharic fallback from cached atomic Gemini voice pieces
            // (Never overwrite ball_$ball on disk so the full natural Gemini clip still caches cleanly)
            val assembledAmharic = tryAssembleAmharicBallFromAtoms(ball)
            if (assembledAmharic != null) {
                onCallerStatusChanged?.invoke("Gemini Amharic ($voiceName) • Live", null)
                callerScope.launch(playbackDispatcher) {
                    if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                        playAudioBufferBlocking(assembledAmharic.bytes, assembledAmharic.mimeType, myGen)
                    }
                }
                return@launch
            }

            // 5. Otherwise wait up to 2.0s more for full Gemini fetch
            val lateClip = try {
                withTimeoutOrNull(2_000L) {
                    inflightFetches[key]?.await() ?: loadFromMemoryOrDisk(key)
                } ?: loadFromMemoryOrDisk(key)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                loadFromMemoryOrDisk(key)
            }

            if (callGeneration.get() != myGen || isMuted || !isVoiceEnabled) return@launch

            if (lateClip != null) {
                onCallerStatusChanged?.invoke("Gemini Amharic ($voiceName) • Live", null)
                callerScope.launch(playbackDispatcher) {
                    if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                        playAudioBufferBlocking(lateClip.bytes, lateClip.mimeType, myGen)
                    }
                }
            } else {
                // 6. Last-resort local TTS + chime fallback if network/quota is completely offline
                speakFallbackBall(ball, myGen)
            }
        }
    }

    fun callRoundStart() {
        if (!isVoiceEnabled || isMuted) return
        val myGen = callGeneration.incrementAndGet()
        val cached = loadFromMemoryOrDisk(CallerEvent.GAME_START.key)
        if (cached != null) {
            callerScope.launch(playbackDispatcher) {
                if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                    playAudioBufferBlocking(cached.bytes, cached.mimeType, myGen)
                }
            }
            return
        }

        callerScope.launch {
            if (inflightFetches[CallerEvent.GAME_START.key] == null) {
                prefetchEvent(CallerEvent.GAME_START)
            }
            val clip = try {
                withTimeoutOrNull(4_800L) {
                    inflightFetches[CallerEvent.GAME_START.key]?.await() ?: loadFromMemoryOrDisk(CallerEvent.GAME_START.key)
                } ?: loadFromMemoryOrDisk(CallerEvent.GAME_START.key)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

            if (callGeneration.get() != myGen || isMuted || !isVoiceEnabled) return@launch
            if (clip != null) {
                callerScope.launch(playbackDispatcher) {
                    if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                        playAudioBufferBlocking(clip.bytes, clip.mimeType, myGen)
                    }
                }
            } else {
                playSubtleChimeFallback(isVictory = false, generation = myGen)
            }
        }
    }

    fun callWin() {
        if (!isVoiceEnabled || isMuted) return
        val myGen = callGeneration.incrementAndGet()
        val cached = loadFromMemoryOrDisk(CallerEvent.BINGO_WIN.key)
        if (cached != null) {
            callerScope.launch(playbackDispatcher) {
                if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                    playAudioBufferBlocking(cached.bytes, cached.mimeType, myGen)
                }
            }
            return
        }

        if (!AuthManager.isFirebaseInitialized(context) || android.os.SystemClock.elapsedRealtime() < geminiBlockedUntilMs) {
            playSubtleChimeFallback(isVictory = true, generation = myGen)
            return
        }

        callerScope.launch {
            if (inflightFetches[CallerEvent.BINGO_WIN.key] == null) {
                prefetchEvent(CallerEvent.BINGO_WIN)
            }
            val clip = try {
                withTimeoutOrNull(3_200L) {
                    inflightFetches[CallerEvent.BINGO_WIN.key]?.await() ?: loadFromMemoryOrDisk(CallerEvent.BINGO_WIN.key)
                } ?: loadFromMemoryOrDisk(CallerEvent.BINGO_WIN.key)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (callGeneration.get() != myGen || isMuted || !isVoiceEnabled) return@launch
            if (clip != null) {
                callerScope.launch(playbackDispatcher) {
                    if (callGeneration.get() == myGen && !isMuted && isVoiceEnabled) {
                        playAudioBufferBlocking(clip.bytes, clip.mimeType, myGen)
                    }
                }
            } else {
                playSubtleChimeFallback(isVictory = true, generation = myGen)
            }
        }
    }

    private fun speakFallbackBall(ball: Int, generation: Long) {
        if (callGeneration.get() != generation || isMuted || !isVoiceEnabled) return
        val engine = fallbackTts
        // Never use robotic English speechSynthesis when device lacks Amharic support;
        // fall back cleanly to the subtle audio chime instead.
        if (engine != null && isFallbackTtsReady && isAmharicTtsSupported) {
            try {
                val callout = getCallout(ball)
                val bundle = android.os.Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, effectiveGain())
                }
                synchronized(playbackLock) {
                    stopActivePlaybackLocked()
                    engine.setPitch(0.96f)
                    engine.setSpeechRate(if (isFastSpeed) 1.06f else 0.96f)
                    engine.speak(callout.spokenAmharicText, TextToSpeech.QUEUE_FLUSH, bundle, "ball_fb_$ball")
                }
                return
            } catch (_: Exception) {}
        }
        playSubtleChimeFallback(isVictory = false, generation = generation)
    }

    fun playSubtleChimeFallback(isVictory: Boolean = false, generation: Long = callGeneration.get()) {
        val gain = effectiveGain()
        if (gain <= 0f) return

        callerScope.launch(playbackDispatcher) {
            if (callGeneration.get() != generation || isMuted || !isVoiceEnabled) return@launch
            try {
                val sampleRate = 22050
                val notes = if (isVictory) {
                    listOf(523.25 to 0.12, 659.25 to 0.12, 783.99 to 0.24)
                } else {
                    listOf(587.33 to 0.10, 880.00 to 0.20)
                }
                val totalSamples = notes.sumOf { (it.second * sampleRate).toInt() }
                val pcmBytes = ByteArray(totalSamples * 2)
                var sampleOffset = 0

                for ((freq, durationSec) in notes) {
                    val count = (durationSec * sampleRate).toInt()
                    for (i in 0 until count) {
                        val t = i.toDouble() / sampleRate
                        val env = exp(-4.2 * (i.toDouble() / count)) * minOf(1.0, i / 90.0)
                        val fundamental = sin(2.0 * PI * freq * t)
                        val overtone = 0.25 * sin(2.0 * PI * (freq * 2.0) * t)
                        val sample = ((fundamental + overtone) * 0.55 * env * Short.MAX_VALUE)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        val byteIdx = (sampleOffset + i) * 2
                        pcmBytes[byteIdx] = (sample and 0xFF).toByte()
                        pcmBytes[byteIdx + 1] = ((sample shr 8) and 0xFF).toByte()
                    }
                    sampleOffset += count
                }

                val played = playPcmViaAudioTrackBlocking(
                    pcmBytes = pcmBytes,
                    sampleRate = sampleRate,
                    channelMask = AudioFormat.CHANNEL_OUT_MONO,
                    gain = gain,
                    generation = generation,
                    isChime = true
                )
                if (!played && callGeneration.get() == generation && !isMuted && isVoiceEnabled) {
                    val wavBytes = wrapPcmAsWav(pcmBytes, sampleRate, channels = 1)
                    if (!playEncodedViaMediaPlayerBlocking(wavBytes, "audio/wav", gain, generation)) {
                        try {
                            toneGenerator?.startTone(
                                if (isVictory) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP2,
                                180
                            )
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
                } catch (_: Exception) {}
            }
        }
    }

    private fun playAudioBufferBlocking(bytes: ByteArray, mimeType: String, generation: Long) {
        val gain = effectiveGain()
        if (gain <= 0f || bytes.isEmpty() || callGeneration.get() != generation) return

        val lowerMime = mimeType.lowercase()
        val isWavHeader = bytes.size > 44 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()

        if (isWavHeader) {
            val wavInfo = parseWavHeader(bytes)
            if (wavInfo != null) {
                val (pcmOffset, dataLength, sampleRate, channelMask) = wavInfo
                val endOffset = minOf(bytes.size, pcmOffset + dataLength)
                if (endOffset > pcmOffset) {
                    val pcmData = bytes.copyOfRange(pcmOffset, endOffset)
                    if (playPcmViaAudioTrackBlocking(pcmData, sampleRate, channelMask, gain, generation)) {
                        return
                    }
                }
            }
            // Backup: let MediaPlayer decode the WAV container directly
            playEncodedViaMediaPlayerBlocking(bytes, "audio/wav", gain, generation)
            return
        }

        if (lowerMime.contains("pcm") || lowerMime.contains("l16") || lowerMime.isBlank()) {
            val sampleRate = parseSampleRateFromMime(lowerMime) ?: DEFAULT_SAMPLE_RATE
            if (!playPcmViaAudioTrackBlocking(bytes, sampleRate, AudioFormat.CHANNEL_OUT_MONO, gain, generation)) {
                // Backup: wrap raw PCM in a WAV header and play via MediaPlayer
                val wavBytes = wrapPcmAsWav(bytes, sampleRate, channels = 1)
                playEncodedViaMediaPlayerBlocking(wavBytes, "audio/wav", gain, generation)
            }
            return
        }

        // Compressed container format (MP3/OGG/AAC/etc.)
        playEncodedViaMediaPlayerBlocking(bytes, lowerMime, gain, generation)
    }

    private fun parseSampleRateFromMime(mimeType: String): Int? {
        val match = Regex("rate\\s*=\\s*(\\d+)").find(mimeType) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    private data class WavHeaderInfo(
        val pcmOffset: Int,
        val dataLength: Int,
        val sampleRate: Int,
        val channelMask: Int
    )

    private fun parseWavHeader(bytes: ByteArray): WavHeaderInfo? {
        if (bytes.size < 44) return null
        return try {
            val channels = ((bytes[23].toInt() and 0xFF) shl 8) or (bytes[22].toInt() and 0xFF)
            val sampleRate = ((bytes[27].toInt() and 0xFF) shl 24) or
                ((bytes[26].toInt() and 0xFF) shl 16) or
                ((bytes[25].toInt() and 0xFF) shl 8) or
                (bytes[24].toInt() and 0xFF)
            val validRate = if (sampleRate in 8000..192000) sampleRate else DEFAULT_SAMPLE_RATE
            val channelMask = if (channels >= 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO

            var offset = 12
            while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
                val chunkSize = ((bytes[offset + 7].toInt() and 0xFF) shl 24) or
                    ((bytes[offset + 6].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 5].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 4].toInt() and 0xFF)
                val safeChunkSize = maxOf(0, chunkSize)
                if (chunkId == "data") {
                    val dataStart = offset + 8
                    val dataLen = if (safeChunkSize > 0) minOf(safeChunkSize, bytes.size - dataStart) else (bytes.size - dataStart)
                    return WavHeaderInfo(dataStart, dataLen, validRate, channelMask)
                }
                // RIFF chunks are word-aligned (padded to even byte boundaries)
                offset += 8 + safeChunkSize + (safeChunkSize and 1)
            }
            WavHeaderInfo(44, bytes.size - 44, validRate, channelMask)
        } catch (_: Exception) {
            null
        }
    }

    private fun wrapPcmAsWav(pcm: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.size
        val totalDataLen = dataSize + 36
        val out = ByteArrayOutputStream(44 + dataSize)
        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        out.write(intToLittleEndian(totalDataLen))
        out.write("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        out.write(intToLittleEndian(16))
        out.write(shortToLittleEndian(1))
        out.write(shortToLittleEndian(channels))
        out.write(intToLittleEndian(sampleRate))
        out.write(intToLittleEndian(byteRate))
        out.write(shortToLittleEndian(blockAlign))
        out.write(shortToLittleEndian(bitsPerSample))
        out.write("data".toByteArray(Charsets.US_ASCII))
        out.write(intToLittleEndian(dataSize))
        out.write(pcm)
        return out.toByteArray()
    }

    private fun intToLittleEndian(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )

    private fun shortToLittleEndian(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte()
    )

    private fun playPcmViaAudioTrackBlocking(
        pcmBytes: ByteArray,
        sampleRate: Int,
        channelMask: Int,
        gain: Float,
        generation: Long,
        isChime: Boolean = false
    ): Boolean {
        val frameSize = if (channelMask == AudioFormat.CHANNEL_OUT_STEREO) 4 else 2
        val alignedLen = pcmBytes.size - (pcmBytes.size % frameSize)
        if (alignedLen < frameSize || callGeneration.get() != generation) return false

        val totalFrames = alignedLen / frameSize
        val useStaticMode = alignedLen <= 1_048_576
        var track: AudioTrack? = null
        return try {
            synchronized(playbackLock) {
                stopActivePlaybackLocked()
            }

            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                channelMask,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (useStaticMode) {
                alignedLen
            } else {
                maxOf(if (minBuffer > 0) minBuffer * 2 else 8192, 8192)
            }

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(
                            if (isChime) AudioAttributes.CONTENT_TYPE_SONIFICATION else AudioAttributes.CONTENT_TYPE_SPEECH
                        )
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setTransferMode(if (useStaticMode) AudioTrack.MODE_STATIC else AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                Log.w(TAG, "AudioTrack failed to initialize (state=${track.state})")
                track.release()
                return false
            }

            var written = 0
            if (useStaticMode) {
                while (written < alignedLen && callGeneration.get() == generation && !isMuted && isVoiceEnabled) {
                    val result = track.write(pcmBytes, written, alignedLen - written)
                    if (result <= 0) {
                        Log.w(TAG, "AudioTrack static write returned $result")
                        break
                    }
                    written += result
                }
                if (written <= 0) return false
            }

            synchronized(playbackLock) {
                if (callGeneration.get() != generation || isMuted || !isVoiceEnabled) {
                    track.release()
                    return false
                }
                activeAudioTrack = track
                track.setVolume(gain)
                track.play()
            }

            if (!useStaticMode) {
                val chunkSize = minOf(bufferSize, 4096)
                while (written < alignedLen && callGeneration.get() == generation && !isMuted && isVoiceEnabled) {
                    val toWrite = minOf(chunkSize, alignedLen - written)
                    val result = track.write(pcmBytes, written, toWrite, AudioTrack.WRITE_BLOCKING)
                    if (result <= 0) {
                        Log.w(TAG, "AudioTrack stream write returned $result")
                        break
                    }
                    written += result
                }
            }

            val writtenFrames = written / frameSize
            if (callGeneration.get() == generation && writtenFrames > 0) {
                val clipDurationMs = (writtenFrames.toLong() * 1000L) / sampleRate.coerceAtLeast(8000)
                val minWaitMs = (clipDurationMs * 85L) / 100L
                val maxWaitMs = minOf(5_200L, clipDurationMs + 500L)
                val startMs = android.os.SystemClock.elapsedRealtime()
                val deadlineMs = startMs + maxWaitMs
                while (callGeneration.get() == generation &&
                    !isMuted &&
                    isVoiceEnabled &&
                    android.os.SystemClock.elapsedRealtime() < deadlineMs
                ) {
                    val elapsedMs = android.os.SystemClock.elapsedRealtime() - startMs
                    val head = try { track.playbackHeadPosition } catch (_: Exception) { writtenFrames }
                    if (elapsedMs >= minWaitMs && head >= writtenFrames) break
                    Thread.sleep(25L)
                }
            }
            written > 0
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack playback failed: ${e.message}")
            false
        } finally {
            synchronized(playbackLock) {
                if (activeAudioTrack === track) {
                    activeAudioTrack = null
                }
                try {
                    track?.let {
                        if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            it.pause()
                            if (callGeneration.get() != generation || isMuted || !isVoiceEnabled) {
                                it.flush()
                            }
                            it.stop()
                        }
                        it.release()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun playEncodedViaMediaPlayerBlocking(
        bytes: ByteArray,
        lowerMime: String,
        gain: Float,
        generation: Long
    ): Boolean {
        if (callGeneration.get() != generation) return false
        val ext = when {
            lowerMime.contains("mp3") || lowerMime.contains("mpeg") -> "mp3"
            lowerMime.contains("ogg") -> "ogg"
            lowerMime.contains("aac") -> "aac"
            else -> "wav"
        }
        val tempFile = try {
            File.createTempFile("caller_", ".$ext", context.cacheDir).apply {
                writeBytes(bytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to buffer audio clip", e)
            return false
        }

        val doneLatch = java.util.concurrent.CountDownLatch(1)
        var started = false
        synchronized(playbackLock) {
            stopActivePlaybackLocked()
            if (callGeneration.get() != generation) {
                tempFile.delete()
                return false
            }
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    setVolume(gain, gain)
                    setOnCompletionListener { mp ->
                        try {
                            mp.release()
                        } catch (_: Exception) {}
                        tempFile.delete()
                        doneLatch.countDown()
                    }
                    setOnErrorListener { mp, _, _ ->
                        try {
                            mp.release()
                        } catch (_: Exception) {}
                        tempFile.delete()
                        doneLatch.countDown()
                        true
                    }
                    prepare()
                    start()
                }
                activeMediaPlayer = player
                started = true
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer playback failed: ${e.message}")
                tempFile.delete()
                doneLatch.countDown()
            }
        }
        if (started) {
            val deadlineMs = android.os.SystemClock.elapsedRealtime() + 4_500L
            while (callGeneration.get() == generation &&
                !isMuted &&
                isVoiceEnabled &&
                android.os.SystemClock.elapsedRealtime() < deadlineMs
            ) {
                if (doneLatch.await(40L, java.util.concurrent.TimeUnit.MILLISECONDS)) break
            }
        }
        return started
    }

    private fun stopActivePlaybackLocked() {
        try {
            fallbackTts?.stop()
        } catch (_: Exception) {}

        try {
            activeAudioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                    track.stop()
                }
                track.release()
            }
        } catch (_: Exception) {}
        activeAudioTrack = null

        try {
            activeMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {}
        activeMediaPlayer = null
    }

    fun clearSessionCache() {
        callGeneration.incrementAndGet()
        inflightFetches.values.forEach { it.cancel() }
        inflightFetches.clear()
        // Keep downloaded Amharic audio clips in memory + disk across rounds
        // so repeat numbers play instantaneously without re-hitting rate limits.
        synchronized(playbackLock) {
            stopActivePlaybackLocked()
        }
    }

    fun release() {
        clearSessionCache()
        atomicWarmJob?.cancel()
        audioCache.clear()
        callerScope.cancel()
        playbackDispatcher.close()
        try {
            fallbackTts?.shutdown()
            fallbackTts = null
        } catch (_: Exception) {}
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
