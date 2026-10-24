package com.example.data.speech

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import android.os.Build
import com.example.data.model.ReadingLanguage
import com.example.data.model.StudyLanguage
import com.example.data.openai.OpenAiSpeechCache
import com.example.data.openai.OpenAiService
import com.example.ui.components.VoiceEngineChoice

data class TtsVoiceInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val locale: Locale,
    val isNeural: Boolean,
    val isPortuguese: Boolean,
    val isFrench: Boolean,
    val isEnglish: Boolean = false,
    val isSpanish: Boolean = false,
    val isMale: Boolean,
    val isFemale: Boolean,
    val quality: Int,
    val pitch: Float = 1.0f,
    val voice: Voice?
)

class PdfSpeechManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val speechScope = CoroutineScope(Dispatchers.Main)
    private var wordProgressJob: Job? = null

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isSpeakingCategory = MutableStateFlow(false)
    val isSpeakingCategory: StateFlow<Boolean> = _isSpeakingCategory.asStateFlow()

    private val _activeSentenceIndex = MutableStateFlow(0)
    val activeSentenceIndex: StateFlow<Int> = _activeSentenceIndex.asStateFlow()

    private val _activeWordIndex = MutableStateFlow(0)
    val activeWordIndex: StateFlow<Int> = _activeWordIndex.asStateFlow()

    private val _speechSpeed = MutableStateFlow(1.0f)
    val speechSpeed: StateFlow<Float> = _speechSpeed.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<TtsVoiceInfo?>(null)
    val selectedVoice: StateFlow<TtsVoiceInfo?> = _selectedVoice.asStateFlow()

    private val _activeReadingLanguage = MutableStateFlow(ReadingLanguage.PORTUGUESE)
    val activeReadingLanguage: StateFlow<ReadingLanguage> = _activeReadingLanguage.asStateFlow()

    private val _activeComparativeLineId = MutableStateFlow<String?>(null)
    val activeComparativeLineId: StateFlow<String?> = _activeComparativeLineId.asStateFlow()

    private val _activeTargetId = MutableStateFlow<String?>(null)
    val activeTargetId: StateFlow<String?> = _activeTargetId.asStateFlow()

    private val _isStudyPaused = MutableStateFlow<Boolean>(false)
    val isStudyPaused: StateFlow<Boolean> = _isStudyPaused.asStateFlow()

    private var lastStudyText: String = ""
    private var lastStudyLocale: Locale = Locale.US
    private var lastStudyLineId: String = ""

    private var comparativeFinishedCallback: (() -> Unit)? = null
    private var openAiMediaPlayer: MediaPlayer? = null
    private var openAiWordProgressJob: Job? = null

    private var currentSentences: List<String> = emptyList()
    private var currentSentenceTokens: List<WordToken> = emptyList()
    private var currentUtteranceStartChar: Int = 0
    private var onPageFinishedCallback: (() -> Unit)? = null
    private var pendingSpeakIndex: Int? = null
    private var pendingSpeakWordIndex: Int = 0
    private var hasReceivedRangeStart: Boolean = false

    data class WordToken(
        val index: Int,
        val text: String,
        val startChar: Int,
        val endChar: Int
    )

    companion object {
        val WORD_REGEX = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]|[^ \\t\\n\\r\\u00A0\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]+")

        fun tokenizeSentence(sentence: String): List<WordToken> {
            return WORD_REGEX.findAll(sentence).mapIndexed { idx, match ->
                WordToken(
                    index = idx,
                    text = match.value,
                    startChar = match.range.first,
                    endChar = match.range.last + 1
                )
            }.toList()
        }

        fun calculateWordDurationMs(word: String, speed: Float): Long {
            val clean = word.trim()
            val letterCount = clean.count { it.isLetterOrDigit() || it in '\u4E00'..'\u9FFF' || it in '\u3040'..'\u30FF' }.coerceAtLeast(1)

            // Baseline calibration for TTS prosody:
            // Normal speech rate at 1.0x is ~135-150 words per minute (~400-440ms for average 5-letter word).
            // Syllable base: ~140ms, phoneme duration: ~38ms per letter/digit
            var duration = 140L + (letterCount * 38L)

            // Punctuation pauses modeled after Android/Google TTS prosody:
            when {
                clean.endsWith("...") || clean.endsWith("…") -> duration += 450L
                clean.endsWith(".") || clean.endsWith("!") || clean.endsWith("?") -> duration += 400L
                clean.endsWith(",") || clean.endsWith(";") || clean.endsWith(":") -> duration += 260L
                clean.contains("—") || clean.contains("-") -> duration += 180L
            }

            // Scale by speech speed dynamically
            val scaled = (duration / speed.coerceIn(0.4f, 3.0f)).toLong()
            return scaled.coerceIn(70L, 2000L)
        }
    }

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    configureTts()
                    loadAvailableVoices()
                    // If a speech request arrived before TTS was initialized, execute it now
                    val pending = pendingSpeakIndex
                    if (pending != null && currentSentences.isNotEmpty()) {
                        pendingSpeakIndex = null
                        speakSentenceAt(pending, pendingSpeakWordIndex)
                    }
                } else {
                    Log.e("PdfSpeechManager", "TextToSpeech init failed with status $status")
                }
            }
        }
    }

    private fun loadAvailableVoices() {
        val t = tts ?: return
        try {
            val rawVoices = t.voices ?: emptySet()
            val list = mutableListOf<TtsVoiceInfo>()

            rawVoices.forEach { voice ->
                val isNeural = voice.name.contains("neural", ignoreCase = true) ||
                               voice.name.contains("network", ignoreCase = true) ||
                               voice.name.contains("wavenet", ignoreCase = true) ||
                               voice.quality >= Voice.QUALITY_VERY_HIGH

                val langLower = voice.locale.language.lowercase()
                val isPt = langLower == "pt" || voice.name.contains("pt-", ignoreCase = true) || voice.name.contains("pt_", ignoreCase = true) || voice.name.contains("por-", ignoreCase = true)
                val isFr = langLower == "fr" || langLower == "fra" || voice.locale == Locale.FRENCH || voice.locale == Locale.FRANCE ||
                           voice.name.contains("fr-", ignoreCase = true) || voice.name.contains("fr_", ignoreCase = true) ||
                           voice.name.contains("-fr-", ignoreCase = true) || voice.name.contains("fra-", ignoreCase = true) ||
                           voice.name.contains("french", ignoreCase = true)
                val isEn = langLower == "en" || langLower == "eng" || voice.name.contains("en-", ignoreCase = true) || voice.name.contains("en_", ignoreCase = true)
                val isEs = langLower == "es" || langLower == "spa" || voice.name.contains("es-", ignoreCase = true) || voice.name.contains("es_", ignoreCase = true)

                val nameLower = voice.name.lowercase()
                val isFemale = nameLower.contains("female") || nameLower.contains("fem") ||
                               nameLower.contains("-f-") || nameLower.contains("_f_") ||
                               nameLower.contains("#female")

                val isMale = nameLower.contains("male") || nameLower.contains("masc") ||
                             nameLower.contains("-m-") || nameLower.contains("_m_") ||
                             nameLower.contains("#male") || (!isFemale && (nameLower.contains("yfd") || nameLower.contains("ptd") || nameLower.contains("sm-local")))

                val prettyName = formatVoiceDisplayName(voice, isNeural, isMale, isFemale)

                list.add(
                    TtsVoiceInfo(
                        id = voice.name,
                        name = voice.name,
                        displayName = prettyName,
                        locale = voice.locale,
                        isNeural = isNeural,
                        isPortuguese = isPt,
                        isFrench = isFr,
                        isEnglish = isEn,
                        isSpanish = isEs,
                        isMale = isMale,
                        isFemale = isFemale,
                        quality = voice.quality,
                        pitch = 1.0f,
                        voice = voice
                    )
                )
            }

            // Brazilian Distinct Voice Presets with calibrated acoustic resonance
            val ptLocale = Locale("pt", "BR")
            val basePtVoice = rawVoices.firstOrNull { v ->
                val l = v.locale.language.lowercase()
                l == "pt" || v.name.contains("pt-", ignoreCase = true) || v.name.contains("pt_", ignoreCase = true)
            }

            // Always provide high-contrast Brazilian voices
            list.add(
                0,
                TtsVoiceInfo(
                    id = "preset_pt_br_male",
                    name = "pt-br-male-neural-preset",
                    displayName = "Português Brasil (Masculina) 🎙️ Encorpada",
                    locale = ptLocale,
                    isNeural = true,
                    isPortuguese = true,
                    isFrench = false,
                    isMale = true,
                    isFemale = false,
                    quality = Voice.QUALITY_VERY_HIGH,
                    pitch = 0.78f,
                    voice = rawVoices.firstOrNull { v ->
                        val n = v.name.lowercase()
                        (v.locale.language == "pt" || n.contains("pt")) && (n.contains("male") || n.contains("ptd") || n.contains("jab") || n.contains("masc"))
                    } ?: basePtVoice
                )
            )

            list.add(
                1,
                TtsVoiceInfo(
                    id = "preset_pt_br_female",
                    name = "pt-br-female-neural-preset",
                    displayName = "Português Brasil (Feminina) ✨ Suave",
                    locale = ptLocale,
                    isNeural = true,
                    isPortuguese = true,
                    isFrench = false,
                    isMale = false,
                    isFemale = true,
                    quality = Voice.QUALITY_VERY_HIGH,
                    pitch = 1.15f,
                    voice = rawVoices.firstOrNull { v ->
                        val n = v.name.lowercase()
                        (v.locale.language == "pt" || n.contains("pt")) && (n.contains("female") || n.contains("afs") || n.contains("yfd") || n.contains("fem"))
                    } ?: basePtVoice
                )
            )

            list.add(
                2,
                TtsVoiceInfo(
                    id = "preset_pt_br_narrator",
                    name = "pt-br-narrator-preset",
                    displayName = "Português Brasil (Narrador) 📖 Solene",
                    locale = ptLocale,
                    isNeural = true,
                    isPortuguese = true,
                    isFrench = false,
                    isMale = true,
                    isFemale = false,
                    quality = Voice.QUALITY_VERY_HIGH,
                    pitch = 0.88f,
                    voice = basePtVoice
                )
            )

            // Guarantee presence of French Voices
            val hasFrench = list.any { it.isFrench }
            if (!hasFrench) {
                list.add(
                    TtsVoiceInfo(
                        id = "preset_fr_france_standard",
                        name = "fr-fr-paris-preset",
                        displayName = "Français (France - Paris) ✨ Neural",
                        locale = Locale.FRANCE,
                        isNeural = true,
                        isPortuguese = false,
                        isFrench = true,
                        isMale = false,
                        isFemale = true,
                        quality = Voice.QUALITY_VERY_HIGH,
                        pitch = 1.05f,
                        voice = null
                    )
                )
                list.add(
                    TtsVoiceInfo(
                        id = "preset_fr_canada_male",
                        name = "fr-ca-montreal-male-preset",
                        displayName = "Français (Canada - Montreal) (Masculina)",
                        locale = Locale.CANADA_FRENCH,
                        isNeural = true,
                        isPortuguese = false,
                        isFrench = true,
                        isMale = true,
                        isFemale = false,
                        quality = Voice.QUALITY_HIGH,
                        pitch = 0.84f,
                        voice = null
                    )
                )
            }

            // Guarantee presence of English Voices presets
            val hasEnglish = list.any { it.isEnglish }
            if (!hasEnglish) {
                list.add(
                    TtsVoiceInfo(
                        id = "preset_en_us_female",
                        name = "en-us-neural-female-preset",
                        displayName = "English (United States) (Feminina) ✨ Neural",
                        locale = Locale.US,
                        isNeural = true,
                        isPortuguese = false,
                        isFrench = false,
                        isEnglish = true,
                        isSpanish = false,
                        isMale = false,
                        isFemale = true,
                        quality = Voice.QUALITY_VERY_HIGH,
                        pitch = 1.10f,
                        voice = null
                    )
                )
                list.add(
                    TtsVoiceInfo(
                        id = "preset_en_us_male",
                        name = "en-us-neural-male-preset",
                        displayName = "English (United States) (Masculina) 🎙️ Neural",
                        locale = Locale.US,
                        isNeural = true,
                        isPortuguese = false,
                        isFrench = false,
                        isEnglish = true,
                        isSpanish = false,
                        isMale = true,
                        isFemale = false,
                        quality = Voice.QUALITY_VERY_HIGH,
                        pitch = 0.82f,
                        voice = null
                    )
                )
            }

            // Guarantee presence of Spanish Voices presets
            val hasSpanish = list.any { it.isSpanish }
            if (!hasSpanish) {
                list.add(
                    TtsVoiceInfo(
                        id = "preset_es_spain_female",
                        name = "es-es-neural-female-preset",
                        displayName = "Español (España) (Feminina) ✨ Neural",
                        locale = Locale("es", "ES"),
                        isNeural = true,
                        isPortuguese = false,
                        isFrench = false,
                        isEnglish = false,
                        isSpanish = true,
                        isMale = false,
                        isFemale = true,
                        quality = Voice.QUALITY_VERY_HIGH,
                        pitch = 1.08f,
                        voice = null
                    )
                )
                list.add(
                    TtsVoiceInfo(
                        id = "preset_es_mx_male",
                        name = "es-mx-neural-male-preset",
                        displayName = "Español (México) (Masculina) 🎙️ Neural",
                        locale = Locale("es", "MX"),
                        isNeural = true,
                        isPortuguese = false,
                        isFrench = false,
                        isEnglish = false,
                        isSpanish = true,
                        isMale = true,
                        isFemale = false,
                        quality = Voice.QUALITY_VERY_HIGH,
                        pitch = 0.84f,
                        voice = null
                    )
                )
            }

            list.sortWith(
                compareByDescending<TtsVoiceInfo> { it.isPortuguese }
                    .thenByDescending { it.id.startsWith("preset_pt") }
                    .thenByDescending { it.isEnglish }
                    .thenByDescending { it.isSpanish }
                    .thenByDescending { it.isFrench }
                    .thenByDescending { it.isNeural }
                    .thenBy { it.displayName }
            )

            _availableVoices.value = list

            val prefs = context.getSharedPreferences("speech_prefs", Context.MODE_PRIVATE)
            val savedVoiceId = prefs.getString("selected_voice_id", null)
            val savedVoice = list.firstOrNull { it.id == savedVoiceId }

            val initialVoice = savedVoice
                ?: list.firstOrNull { it.id == "preset_pt_br_male" }
                ?: list.firstOrNull { it.isPortuguese }
                ?: list.firstOrNull()

            _selectedVoice.value = initialVoice
            if (initialVoice != null) {
                applySelectedVoice()
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error loading voices: ${e.message}")
        }
    }

    private fun formatVoiceDisplayName(voice: Voice, isNeural: Boolean, isMale: Boolean, isFemale: Boolean): String {
        val loc = voice.locale
        val country = if (loc.country.isNotBlank()) " (${loc.country})" else ""
        val lang = when (loc.language.lowercase()) {
            "pt" -> "Português"
            "fr" -> "Français"
            "en" -> "English"
            "es" -> "Español"
            "it" -> "Italiano"
            "de" -> "Deutsch"
            else -> loc.displayLanguage.replaceFirstChar { it.uppercase() }
        }
        val neuralBadge = if (isNeural) " ✨ Neural" else ""

        val voiceVariant = when {
            isMale -> " (Masculina)"
            isFemale -> " (Feminina)"
            else -> ""
        }

        return "$lang$country$voiceVariant$neuralBadge"
    }

    fun selectVoice(voiceInfo: TtsVoiceInfo) {
        val t = tts ?: return
        try {
            _selectedVoice.value = voiceInfo

            // 1. Persist the chosen voice across sessions
            try {
                context.getSharedPreferences("speech_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("selected_voice_id", voiceInfo.id)
                    .apply()
            } catch (e: Exception) {
                Log.w("PdfSpeechManager", "Error saving voice pref: ${e.message}")
            }

            // 2. Align active language if user picks a voice from another language
            when {
                voiceInfo.isEnglish -> _activeReadingLanguage.value = ReadingLanguage.ENGLISH
                voiceInfo.isFrench -> _activeReadingLanguage.value = ReadingLanguage.FRENCH
                voiceInfo.isSpanish -> _activeReadingLanguage.value = ReadingLanguage.SPANISH
                voiceInfo.isPortuguese -> _activeReadingLanguage.value = ReadingLanguage.PORTUGUESE
            }

            // 3. Instantly apply settings to the TTS engine
            applySelectedVoice()

            // 4. If currently reading, immediately restart current sentence with the new voice
            if (_isSpeaking.value && currentSentences.isNotEmpty()) {
                val currentIdx = _activeSentenceIndex.value
                val currentWord = _activeWordIndex.value
                speakSentenceAt(currentIdx, currentWord)
            } else {
                // Play immediate acoustic sample preview so the user feels the new voice
                testVoice(voiceInfo)
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error setting voice: ${e.message}")
        }
    }

    fun setReadingLanguage(language: ReadingLanguage) {
        _activeReadingLanguage.value = language
        if (!isInitialized) return
        applyLanguageToTts(language)
    }

    fun applySelectedVoice() {
        val t = tts ?: return
        val currentVoice = _selectedVoice.value ?: return
        try {
            // 1. Apply physical Voice object if present
            if (currentVoice.voice != null) {
                t.language = currentVoice.locale
                t.voice = currentVoice.voice
            } else {
                // Scan raw voices for best matching engine voice
                val rawVoices = t.voices ?: emptySet()
                val matched = rawVoices.firstOrNull { v ->
                    v.locale.language.equals(currentVoice.locale.language, ignoreCase = true) &&
                    (if (currentVoice.isMale) {
                        val n = v.name.lowercase()
                        n.contains("male") || n.contains("ptd") || n.contains("jab") || n.contains("masc")
                    } else if (currentVoice.isFemale) {
                        val n = v.name.lowercase()
                        n.contains("female") || n.contains("fem") || n.contains("afs") || n.contains("yfd")
                    } else true)
                } ?: rawVoices.firstOrNull { v ->
                    v.locale.language.equals(currentVoice.locale.language, ignoreCase = true)
                }

                if (matched != null) {
                    t.language = matched.locale
                    t.voice = matched
                } else {
                    t.language = currentVoice.locale
                }
            }

            // 2. Apply voice calibrated pitch multiplied by user pitch slider
            val effectivePitch = (currentVoice.pitch * _speechPitch.value).coerceIn(0.5f, 2.0f)
            t.setPitch(effectivePitch)

            // 3. Apply global speech rate
            t.setSpeechRate(_speechSpeed.value)
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error applying currentVoice: ${e.message}")
        }
    }

    private fun applyLanguageToTts(language: ReadingLanguage) {
        val t = tts ?: return
        try {
            val list = _availableVoices.value
            val targetLocale = when (language) {
                ReadingLanguage.PORTUGUESE -> Locale("pt", "BR")
                ReadingLanguage.ENGLISH -> Locale.US
                ReadingLanguage.FRENCH -> Locale.FRANCE
                ReadingLanguage.SPANISH -> Locale("es", "ES")
            }

            // 1. Prioritize real voice from loaded list
            val matchedFromList = when (language) {
                ReadingLanguage.PORTUGUESE -> list.firstOrNull { it.isPortuguese && it.voice != null && it.isNeural }
                    ?: list.firstOrNull { it.isPortuguese && it.voice != null }
                ReadingLanguage.ENGLISH -> list.firstOrNull { it.isEnglish && it.voice != null && it.isNeural }
                    ?: list.firstOrNull { it.isEnglish && it.voice != null }
                ReadingLanguage.FRENCH -> list.firstOrNull { it.isFrench && it.voice != null && it.isNeural }
                    ?: list.firstOrNull { it.isFrench && it.voice != null }
                ReadingLanguage.SPANISH -> list.firstOrNull { it.isSpanish && it.voice != null && it.isNeural }
                    ?: list.firstOrNull { it.isSpanish && it.voice != null }
            }

            // 2. If list doesn't contain a real voice instance, scan raw tts.voices directly
            val rawVoices = t.voices ?: emptySet()
            val matchedRawVoice: Voice? = matchedFromList?.voice ?: when (language) {
                ReadingLanguage.FRENCH -> {
                    rawVoices.firstOrNull { v ->
                        val l = v.locale.language.lowercase()
                        val n = v.name.lowercase()
                        (l == "fr" || l == "fra" || n.contains("fr-") || n.contains("fr_") || n.contains("french")) &&
                        (n.contains("neural") || v.quality >= Voice.QUALITY_HIGH)
                    } ?: rawVoices.firstOrNull { v ->
                        val l = v.locale.language.lowercase()
                        val n = v.name.lowercase()
                        l == "fr" || l == "fra" || n.contains("fr-") || n.contains("fr_") || n.contains("french")
                    }
                }
                ReadingLanguage.ENGLISH -> {
                    rawVoices.firstOrNull { v ->
                        val l = v.locale.language.lowercase()
                        val n = v.name.lowercase()
                        (l == "en" || n.contains("en-") || n.contains("en_")) &&
                        (n.contains("neural") || v.quality >= Voice.QUALITY_HIGH)
                    } ?: rawVoices.firstOrNull { v ->
                        v.locale.language.equals("en", ignoreCase = true) || v.name.contains("en-", ignoreCase = true)
                    }
                }
                ReadingLanguage.SPANISH -> {
                    rawVoices.firstOrNull { v ->
                        val l = v.locale.language.lowercase()
                        val n = v.name.lowercase()
                        (l == "es" || n.contains("es-") || n.contains("es_")) &&
                        (n.contains("neural") || v.quality >= Voice.QUALITY_HIGH)
                    } ?: rawVoices.firstOrNull { v ->
                        v.locale.language.equals("es", ignoreCase = true) || v.name.contains("es-", ignoreCase = true)
                    }
                }
                ReadingLanguage.PORTUGUESE -> {
                    rawVoices.firstOrNull { v ->
                        val l = v.locale.language.lowercase()
                        val n = v.name.lowercase()
                        (l == "pt" || n.contains("pt-") || n.contains("pt_")) &&
                        (n.contains("neural") || v.quality >= Voice.QUALITY_HIGH)
                    } ?: rawVoices.firstOrNull { v ->
                        v.locale.language.equals("pt", ignoreCase = true) || v.name.contains("pt-", ignoreCase = true)
                    }
                }
            }

            // 3. Apply to TextToSpeech engine
            if (matchedRawVoice != null) {
                t.language = matchedRawVoice.locale
                t.voice = matchedRawVoice
            } else {
                t.setLanguage(targetLocale)
                if (language == ReadingLanguage.FRENCH) {
                    val res = t.setLanguage(Locale.FRANCE)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        t.setLanguage(Locale.FRENCH)
                    }
                }
                try {
                    val defV = t.defaultVoice
                    if (defV != null && (defV.locale.language.equals(targetLocale.language, ignoreCase = true) ||
                                         (language == ReadingLanguage.FRENCH && defV.locale.language.startsWith("fr", ignoreCase = true)))) {
                        t.voice = defV
                    }
                } catch (_: Exception) {}
            }

            val fallbackPitch = (matchedFromList?.pitch ?: 1.0f) * _speechPitch.value
            t.setPitch(fallbackPitch.coerceIn(0.5f, 2.0f))
            t.setSpeechRate(_speechSpeed.value)
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error applying language $language to TTS: ${e.message}")
        }
    }

    fun applyLocaleToTts(targetLocale: Locale) {
        val t = tts ?: return
        try {
            val targetLangCode = targetLocale.language.lowercase()
            if (targetLangCode == "pt" || targetLangCode == "por") {
                applySelectedVoice()
                return
            }

            val altCodes = when (targetLangCode) {
                "he" -> listOf("iw", "heb")
                "iw" -> listOf("he", "heb")
                "zh" -> listOf("cmn", "zho")
                "ja" -> listOf("jpn")
                "ko" -> listOf("kor")
                "ar" -> listOf("ara")
                "ru" -> listOf("rus")
                "uk" -> listOf("ukr")
                "el" -> listOf("ell", "grc")
                "de" -> listOf("deu", "ger")
                "fr" -> listOf("fra", "fre")
                "es" -> listOf("spa")
                "it" -> listOf("ita")
                "en" -> listOf("eng")
                "nl" -> listOf("nld", "dut")
                "pl" -> listOf("pol")
                "tr" -> listOf("tur")
                "sv" -> listOf("swe")
                "hi" -> listOf("hin")
                "ro" -> listOf("ron", "rum")
                "id" -> listOf("ind")
                "vi" -> listOf("vie")
                "no" -> listOf("nor", "nob")
                "da" -> listOf("dan")
                "fi" -> listOf("fin")
                "cs" -> listOf("ces", "cze")
                else -> emptyList()
            }
            val allTargetCodes = listOf(targetLangCode) + altCodes

            val rawVoices = t.voices ?: emptySet()
            val matchedRawVoice: Voice? = rawVoices.firstOrNull { v ->
                val l = v.locale.language.lowercase()
                val n = v.name.lowercase()
                val matchesLang = allTargetCodes.any { code ->
                    l == code || n.contains("${code}-") || n.contains("${code}_") || n.contains("-${code}-")
                }
                matchesLang && (n.contains("neural") || v.quality >= Voice.QUALITY_HIGH)
            } ?: rawVoices.firstOrNull { v ->
                val l = v.locale.language.lowercase()
                val n = v.name.lowercase()
                allTargetCodes.any { code ->
                    l == code || n.contains("${code}-") || n.contains("${code}_") || n.contains("-${code}-")
                }
            }

            if (matchedRawVoice != null) {
                t.language = matchedRawVoice.locale
                t.voice = matchedRawVoice
            } else {
                val res = t.setLanguage(targetLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    for (alt in altCodes) {
                        val altLocale = Locale(alt)
                        val altRes = t.setLanguage(altLocale)
                        if (altRes != TextToSpeech.LANG_MISSING_DATA && altRes != TextToSpeech.LANG_NOT_SUPPORTED) {
                            break
                        }
                    }
                }
            }
            t.setPitch(_speechPitch.value.coerceIn(0.5f, 2.0f))
            t.setSpeechRate(_speechSpeed.value)
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error applying locale $targetLocale to TTS: ${e.message}")
        }
    }

    fun isLanguageAvailableOnDevice(targetLocale: Locale): Boolean {
        val t = tts ?: return false
        try {
            val targetLangCode = targetLocale.language.lowercase()
            if (targetLangCode == "pt" || targetLangCode == "por") return true

            val altCodes = when (targetLangCode) {
                "he" -> listOf("iw", "heb")
                "iw" -> listOf("he", "heb")
                "zh" -> listOf("cmn", "zho")
                "ja" -> listOf("jpn")
                "ko" -> listOf("kor")
                "ar" -> listOf("ara")
                "ru" -> listOf("rus")
                "uk" -> listOf("ukr")
                "el" -> listOf("ell", "grc")
                "de" -> listOf("deu", "ger")
                "fr" -> listOf("fra", "fre")
                "es" -> listOf("spa")
                "it" -> listOf("ita")
                "en" -> listOf("eng")
                "nl" -> listOf("nld", "dut")
                "pl" -> listOf("pol")
                "tr" -> listOf("tur")
                "sv" -> listOf("swe")
                "hi" -> listOf("hin")
                "ro" -> listOf("ron", "rum")
                "id" -> listOf("ind")
                "vi" -> listOf("vie")
                "no" -> listOf("nor", "nob")
                "da" -> listOf("dan")
                "fi" -> listOf("fin")
                "cs" -> listOf("ces", "cze")
                else -> emptyList()
            }
            val allTargetCodes = listOf(targetLangCode) + altCodes

            val rawVoices = t.voices ?: emptySet()
            val matched = rawVoices.any { v ->
                val l = v.locale.language.lowercase()
                val n = v.name.lowercase()
                allTargetCodes.any { c ->
                    l == c || n.contains("${c}-") || n.contains("${c}_") || n.contains("-${c}-")
                }
            }
            if (matched) return true

            val res = t.isLanguageAvailable(targetLocale)
            if (res >= TextToSpeech.LANG_AVAILABLE) return true
            for (alt in altCodes) {
                val altRes = t.isLanguageAvailable(Locale(alt))
                if (altRes >= TextToSpeech.LANG_AVAILABLE) return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun testVoice(voiceInfo: TtsVoiceInfo, customText: String? = null) {
        val t = tts ?: return
        try {
            if (voiceInfo.voice != null) {
                t.language = voiceInfo.locale
                t.voice = voiceInfo.voice
            } else {
                val rawVoices = t.voices ?: emptySet()
                val matched = rawVoices.firstOrNull { v ->
                    v.locale.language.equals(voiceInfo.locale.language, ignoreCase = true)
                }
                if (matched != null) {
                    t.language = matched.locale
                    t.voice = matched
                } else {
                    t.language = voiceInfo.locale
                }
            }
            val effectivePitch = (voiceInfo.pitch * _speechPitch.value).coerceIn(0.5f, 2.0f)
            t.setPitch(effectivePitch)
            t.setSpeechRate(_speechSpeed.value)

            val sampleText = customText ?: when {
                voiceInfo.isEnglish -> "Hello! This is a sample of your selected voice for reading."
                voiceInfo.isFrench -> "Bonjour ! Voici votre voix sélectionnée pour la lecture."
                voiceInfo.isSpanish -> "¡Hola! Esta es la voz seleccionada para su lectura."
                voiceInfo.isMale -> "Olá! Esta é a voz masculina brasileira selecionada para sua leitura."
                voiceInfo.id.contains("female") -> "Olá! Esta é a voz feminina brasileira selecionada para sua leitura."
                else -> "Olá! Esta é a voz neural selecionada para leitura de seus livros."
            }

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "test_voice")
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            t.speak(sampleText, TextToSpeech.QUEUE_FLUSH, params, "test_voice")
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error testing voice: ${e.message}")
        }
    }

    private fun configureTts() {
        val t = tts ?: return

        // 1. Configure Language with robust fallbacks
        try {
            var langResult = t.setLanguage(Locale("pt", "BR"))
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                langResult = t.setLanguage(Locale("pt"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    langResult = t.setLanguage(Locale.getDefault())
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        t.setLanguage(Locale.US)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Language setup error: ${e.message}")
        }

        // 2. Select high-quality voice if available
        try {
            val voices = t.voices
            val neuralVoice = voices?.firstOrNull { voice ->
                !voice.isNetworkConnectionRequired &&
                (voice.name.contains("neural", ignoreCase = true) ||
                 voice.quality >= Voice.QUALITY_VERY_HIGH ||
                 voice.name.contains("pt-br", ignoreCase = true) ||
                 voice.name.contains("pt_br", ignoreCase = true))
            }
            if (neuralVoice != null) {
                t.voice = neuralVoice
            }
        } catch (e: Exception) {
            Log.w("PdfSpeechManager", "Voice selection fallback: ${e.message}")
        }

        // 3. Set initial speech rate and pitch
        try {
            t.setSpeechRate(_speechSpeed.value)
            t.setPitch(1.0f)
        } catch (e: Exception) {
            // ignore
        }

        // 4. Progress Listener with Real-Time Word Synchronisation
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    if (utteranceId?.startsWith("category_") == true) {
                        _isSpeakingCategory.value = true
                        return@post
                    }
                    if (utteranceId?.startsWith("comp_") == true) {
                        _isSpeaking.value = true
                        hasReceivedRangeStart = false
                        val parts = utteranceId.removePrefix("comp_").split("_")
                        val wOffset = parts.lastOrNull()?.toIntOrNull() ?: 0
                        val lineId = parts.dropLast(1).joinToString("_")
                        _activeComparativeLineId.value = lineId
                        _activeWordIndex.value = wOffset
                        startSmoothWordProgress(currentSentenceTokens, wOffset)
                        return@post
                    }
                    _isSpeaking.value = true
                    hasReceivedRangeStart = false
                    val parts = utteranceId?.split("_")
                    val sIdx = parts?.getOrNull(0)?.toIntOrNull()
                    val wOffset = parts?.getOrNull(1)?.toIntOrNull() ?: 0
                    if (sIdx != null) {
                        _activeSentenceIndex.value = sIdx
                        _activeWordIndex.value = wOffset
                        startSmoothWordProgress(currentSentenceTokens, wOffset)
                    }
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                mainHandler.post {
                    if (utteranceId?.startsWith("category_") == true) {
                        return@post
                    }

                    val tokens = currentSentenceTokens
                    if (tokens.isNotEmpty()) {
                        val charInSentence = currentUtteranceStartChar + start
                        val matchedToken = tokens.find { token ->
                            charInSentence in token.startChar..token.endChar
                        } ?: tokens.filter { it.startChar <= charInSentence }.maxByOrNull { it.startChar }
                          ?: tokens.firstOrNull()

                        if (utteranceId?.startsWith("comp_") == true) {
                            val parts = utteranceId.removePrefix("comp_").split("_")
                            val lineId = parts.dropLast(1).joinToString("_")
                            _activeComparativeLineId.value = lineId
                        } else {
                            val parts = utteranceId?.split("_")
                            val sIdx = parts?.getOrNull(0)?.toIntOrNull() ?: _activeSentenceIndex.value
                            _activeSentenceIndex.value = sIdx
                        }

                        if (matchedToken != null) {
                            _activeWordIndex.value = matchedToken.index
                            // Continue smooth progression smoothly from matched word to prevent stalls
                            startSmoothWordProgress(tokens, matchedToken.index)
                        }
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (utteranceId?.startsWith("category_") == true) {
                        _isSpeakingCategory.value = false
                        applySelectedVoice()
                        return@post
                    }
                    if (utteranceId?.startsWith("comp_") == true) {
                        wordProgressJob?.cancel()
                        _isSpeaking.value = false
                        _isStudyPaused.value = false
                        _activeComparativeLineId.value = null
                        _activeWordIndex.value = 0
                        comparativeFinishedCallback?.invoke()
                        comparativeFinishedCallback = null
                        return@post
                    }
                    wordProgressJob?.cancel()
                    val parts = utteranceId?.split("_")
                    val sIdx = parts?.getOrNull(0)?.toIntOrNull()
                    if (sIdx != null) {
                        val nextIdx = sIdx + 1
                        if (nextIdx < currentSentences.size) {
                            _activeSentenceIndex.value = nextIdx
                            _activeWordIndex.value = 0
                            speakSentenceAt(nextIdx, startWordOffset = 0)
                        } else {
                            _isSpeaking.value = false
                            _activeWordIndex.value = 0
                            _activeTargetId.value = null
                            onPageFinishedCallback?.invoke()
                        }
                    } else {
                        _isSpeaking.value = false
                        _activeWordIndex.value = 0
                        _activeTargetId.value = null
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    _activeTargetId.value = null
                    if (utteranceId?.startsWith("category_") == true) {
                        _isSpeakingCategory.value = false
                        applySelectedVoice()
                        return@post
                    }
                    if (utteranceId?.startsWith("comp_") == true) {
                        wordProgressJob?.cancel()
                        _isSpeaking.value = false
                        _isStudyPaused.value = false
                        _activeComparativeLineId.value = null
                        _activeWordIndex.value = 0
                        comparativeFinishedCallback?.invoke()
                        comparativeFinishedCallback = null
                        return@post
                    }
                    wordProgressJob?.cancel()
                    Log.w("PdfSpeechManager", "TTS Utterance error for ID $utteranceId")
                    val parts = utteranceId?.split("_")
                    val sIdx = parts?.getOrNull(0)?.toIntOrNull()
                    if (sIdx != null && sIdx + 1 < currentSentences.size) {
                        speakSentenceAt(sIdx + 1, startWordOffset = 0)
                    } else {
                        _isSpeaking.value = false
                    }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    if (utteranceId?.startsWith("category_") == true) {
                        _isSpeakingCategory.value = false
                        applySelectedVoice()
                        return@post
                    }
                    if (utteranceId?.startsWith("comp_") == true) {
                        wordProgressJob?.cancel()
                        _isSpeaking.value = false
                        _activeComparativeLineId.value = null
                        _activeWordIndex.value = 0
                        comparativeFinishedCallback?.invoke()
                        comparativeFinishedCallback = null
                        return@post
                    }
                    wordProgressJob?.cancel()
                    Log.w("PdfSpeechManager", "TTS Utterance error for ID $utteranceId, code: $errorCode")
                    val parts = utteranceId?.split("_")
                    val sIdx = parts?.getOrNull(0)?.toIntOrNull()
                    if (sIdx != null && sIdx + 1 < currentSentences.size) {
                        speakSentenceAt(sIdx + 1, startWordOffset = 0)
                    } else {
                        _isSpeaking.value = false
                    }
                }
            }
        })
    }

    private fun startSmoothWordProgress(tokens: List<WordToken>, startWordOffset: Int) {
        wordProgressJob?.cancel()
        if (tokens.isEmpty()) return
        val safeStart = startWordOffset.coerceIn(0, tokens.size - 1)

        wordProgressJob = speechScope.launch {
            for (wIdx in safeStart until tokens.size) {
                if (!isActive || !_isSpeaking.value) break
                val token = tokens[wIdx]
                _activeWordIndex.value = token.index

                val currentSpeed = _speechSpeed.value.coerceIn(0.4f, 3.0f)
                val durationMs = calculateWordDurationMs(token.text, currentSpeed)
                delay(durationMs)
            }
        }
    }

    fun resetActiveIndex() {
        wordProgressJob?.cancel()
        _activeSentenceIndex.value = 0
        _activeWordIndex.value = 0
    }

    fun stop() {
        wordProgressJob?.cancel()
        openAiWordProgressJob?.cancel()
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            if (openAiMediaPlayer?.isPlaying == true) {
                openAiMediaPlayer?.stop()
            }
            openAiMediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        openAiMediaPlayer = null
        _isSpeaking.value = false
        _isSpeakingCategory.value = false
        _isStudyPaused.value = false
        _activeSentenceIndex.value = 0
        _activeWordIndex.value = 0
        _activeComparativeLineId.value = null
        _activeTargetId.value = null
        comparativeFinishedCallback = null
        currentSentences = emptyList()
        onPageFinishedCallback = null
        pendingSpeakIndex = null
        pendingSpeakWordIndex = 0
        applySelectedVoice()
    }

    /**
     * Speaks the category and optional subcategory in native English with high quality.
     * Stops previous speech immediately so covers transition smoothly without overlap.
     */
    fun speakCategoryInEnglish(category: String, subcategory: String? = null) {
        val t = tts ?: return
        if (!isInitialized) return

        val cleanCat = category.trim()
        if (cleanCat.isBlank()) return

        wordProgressJob?.cancel()
        try {
            t.stop()
        } catch (_: Exception) {}

        _isSpeaking.value = false

        val textToSpeak = if (!subcategory.isNullOrBlank() && !subcategory.trim().equals(cleanCat, ignoreCase = true)) {
            "$cleanCat. ${subcategory.trim()}."
        } else {
            "$cleanCat."
        }

        try {
            val rawVoices = t.voices ?: emptySet()
            val englishVoice = rawVoices.firstOrNull { voice ->
                voice.locale.language.equals("en", ignoreCase = true) &&
                !voice.isNetworkConnectionRequired &&
                (voice.name.contains("neural", ignoreCase = true) || voice.quality >= Voice.QUALITY_HIGH)
            } ?: rawVoices.firstOrNull { voice ->
                voice.locale.language.equals("en", ignoreCase = true)
            }

            if (englishVoice != null) {
                t.voice = englishVoice
                t.language = englishVoice.locale
            } else {
                t.language = Locale.US
            }

            t.setSpeechRate(1.0f)
            t.setPitch(1.0f)

            val utteranceId = "category_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }

            _isSpeakingCategory.value = true
            val res = t.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (res == TextToSpeech.ERROR) {
                _isSpeakingCategory.value = false
                applySelectedVoice()
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error speaking category in English: ${e.message}")
            _isSpeakingCategory.value = false
            applySelectedVoice()
        }
    }

    /**
     * Halts category speech immediately and restores default selected reading voice.
     */
    fun stopCategorySpeech() {
        if (_isSpeakingCategory.value) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
            _isSpeakingCategory.value = false
            applySelectedVoice()
        }
    }

    /**
     * Speaks arbitrary text in a specified target language (EN, FR, ES, PT).
     */
    fun speakTextInLanguage(text: String, languageCode: String, onComplete: () -> Unit = {}) {
        val targetLang = when (languageCode.lowercase().take(2)) {
            "fr" -> ReadingLanguage.FRENCH
            "en" -> ReadingLanguage.ENGLISH
            "es" -> ReadingLanguage.SPANISH
            else -> ReadingLanguage.PORTUGUESE
        }
        speakComparativeLine(
            text = text,
            language = targetLang,
            lineId = "standalone_${System.currentTimeMillis()}",
            startWordOffset = 0,
            onComplete = onComplete
        )
    }

    /**
     * Speaks a specific comparative line with authentic native voice (FR, EN, ES, PT)
     * and real-time word-by-word progress tracking.
     */
    fun speakComparativeLine(
        text: String,
        language: ReadingLanguage,
        lineId: String,
        startWordOffset: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        val t = tts ?: return
        if (!isInitialized || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        wordProgressJob?.cancel()
        try {
            t.stop()
        } catch (_: Exception) {}

        _activeComparativeLineId.value = lineId
        _isSpeakingCategory.value = false
        comparativeFinishedCallback = onComplete

        // 1. Instantly switch to the authentic native voice for the selected language
        applyLanguageToTts(language)

        val fullText = text.trim()
        val tokens = tokenizeSentence(fullText)
        currentSentenceTokens = tokens
        currentSentences = listOf(fullText)
        _activeSentenceIndex.value = 0

        val safeOffset = startWordOffset.coerceIn(0, (tokens.size - 1).coerceAtLeast(0))
        val targetToken = tokens.getOrNull(safeOffset)
        val charStart = if (safeOffset == 0) 0 else (targetToken?.startChar ?: 0)
        currentUtteranceStartChar = charStart

        val textToSpeak = if (charStart > 0 && charStart < fullText.length) {
            fullText.substring(charStart).trim()
        } else {
            fullText
        }

        _activeWordIndex.value = targetToken?.index ?: 0
        _isSpeaking.value = true

        val utteranceId = "comp_${lineId}_${safeOffset}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        try {
            tts?.setSpeechRate(_speechSpeed.value)
            val res = t.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (res == TextToSpeech.ERROR) {
                _isSpeaking.value = false
                _activeComparativeLineId.value = null
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error speaking comparative line: ${e.message}")
            _isSpeaking.value = false
            _activeComparativeLineId.value = null
            onComplete?.invoke()
        }
    }

    /**
     * Lê uma frase de estudo em qualquer idioma suportado (com Locale autêntico)
     * com rastreamento sincronizado palavra por palavra (Karaokê) em tempo real.
     */
    fun speakStudyLine(
        text: String,
        targetLocale: Locale,
        lineId: String,
        startWordOffset: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            onComplete?.invoke()
            return
        }

        if (seekOpenAiToWord(lineId, startWordOffset)) {
            return
        }

        val langCode = targetLocale.language.lowercase()
        val savedPref = VoiceEnginePreferences.getPreferredEngine(context, langCode)
        val isPhoneAvailable = isLanguageAvailableOnDevice(targetLocale)

        // Se o idioma não tiver voz pré-instalada no aparelho OU se a OpenAI foi escolhida como preferência:
        if (!isPhoneAvailable || savedPref == VoiceEngineChoice.OPENAI) {
            speechScope.launch {
                val apiKey = OpenAiService.getApiKey(context)
                val audioRes = OpenAiSpeechCache.getOrGenerateAudio(
                    context = context,
                    text = cleanText,
                    langCode = langCode,
                    blockId = lineId,
                    customApiKey = apiKey
                )
                if (audioRes.isSuccess) {
                    playOpenAiAudio(
                        audioFile = audioRes.getOrThrow(),
                        lineId = lineId,
                        text = cleanText,
                        startWordOffset = startWordOffset,
                        onComplete = onComplete
                    )
                } else {
                    speakStudyLineDirectTts(cleanText, targetLocale, lineId, startWordOffset, onComplete)
                }
            }
            return
        }

        speakStudyLineDirectTts(cleanText, targetLocale, lineId, startWordOffset, onComplete)
    }

    private fun speakStudyLineDirectTts(
        text: String,
        targetLocale: Locale,
        lineId: String,
        startWordOffset: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        val t = tts ?: return
        if (!isInitialized || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        wordProgressJob?.cancel()
        try {
            t.stop()
        } catch (_: Exception) {}

        _activeComparativeLineId.value = lineId
        _isSpeakingCategory.value = false
        _isStudyPaused.value = false
        lastStudyText = text
        lastStudyLocale = targetLocale
        lastStudyLineId = lineId
        comparativeFinishedCallback = onComplete

        // Aplica o Locale autêntico do idioma
        applyLocaleToTts(targetLocale)

        val fullText = text.trim()
        val tokens = tokenizeSentence(fullText)
        currentSentenceTokens = tokens
        currentSentences = listOf(fullText)
        _activeSentenceIndex.value = 0

        val safeOffset = startWordOffset.coerceIn(0, (tokens.size - 1).coerceAtLeast(0))
        val targetToken = tokens.getOrNull(safeOffset)
        val charStart = if (safeOffset == 0) 0 else (targetToken?.startChar ?: 0)
        currentUtteranceStartChar = charStart

        val textToSpeak = if (charStart > 0 && charStart < fullText.length) {
            fullText.substring(charStart).trim()
        } else {
            fullText
        }

        _activeWordIndex.value = targetToken?.index ?: 0
        _isSpeaking.value = true

        val utteranceId = "comp_${lineId}_${safeOffset}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        try {
            t.setSpeechRate(_speechSpeed.value)
            val res = t.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (res == TextToSpeech.ERROR) {
                _isSpeaking.value = false
                _activeComparativeLineId.value = null
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error speaking study line: ${e.message}")
            _isSpeaking.value = false
            _activeComparativeLineId.value = null
            onComplete?.invoke()
        }
    }

    fun speakStudyLine(
        text: String,
        language: StudyLanguage,
        lineId: String,
        startWordOffset: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        speakStudyLine(text, language.ttsLocale, lineId, startWordOffset, onComplete)
    }

    /**
     * Reproduz áudio neural gerado pela OpenAI com sincronização em tempo real palavra por palavra (karaokê)
     * e controle dinâmico da velocidade de fala (acelerar / desacelerar).
     */
    fun playOpenAiAudio(
        audioFile: File,
        lineId: String,
        text: String,
        startWordOffset: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        if (!audioFile.exists() || audioFile.length() == 0L || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        wordProgressJob?.cancel()
        try { tts?.stop() } catch (_: Exception) {}

        openAiWordProgressJob?.cancel()
        try {
            if (openAiMediaPlayer?.isPlaying == true) {
                openAiMediaPlayer?.stop()
            }
            openAiMediaPlayer?.release()
        } catch (_: Exception) {}
        _activeComparativeLineId.value = lineId
        _activeTargetId.value = lineId
        _isSpeaking.value = true
        _isSpeakingCategory.value = false
        _isStudyPaused.value = false

        val fullText = text.trim()
        val tokens = tokenizeSentence(fullText)
        currentSentenceTokens = tokens
        currentSentences = listOf(fullText)
        _activeSentenceIndex.value = 0

        val safeOffset = startWordOffset.coerceIn(0, (tokens.size - 1).coerceAtLeast(0))
        _activeWordIndex.value = safeOffset

        try {
            val mp = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                val currentSpeed = _speechSpeed.value.coerceIn(0.4f, 3.0f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                    try {
                        playbackParams = playbackParams.setSpeed(currentSpeed)
                    } catch (_: Exception) {}
                }
                if (safeOffset > 0 && tokens.isNotEmpty()) {
                    val ratio = safeOffset.toFloat() / tokens.size.toFloat()
                    val targetMs = (ratio * duration).toInt().coerceIn(0, duration)
                    seekTo(targetMs)
                }
                start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val currentSpeedAfter = _speechSpeed.value.coerceIn(0.4f, 3.0f)
                        if (currentSpeedAfter != 1.0f) {
                            playbackParams = playbackParams.setSpeed(currentSpeedAfter)
                        }
                    } catch (_: Exception) {}
                }
                setOnCompletionListener {
                    openAiWordProgressJob?.cancel()
                    _isSpeaking.value = false
                    _activeComparativeLineId.value = null
                    if (_activeTargetId.value == lineId) {
                        _activeTargetId.value = null
                    }
                    _activeWordIndex.value = 0
                    onComplete?.invoke()
                }
            }
            openAiMediaPlayer = mp

            openAiWordProgressJob = CoroutineScope(Dispatchers.Main).launch {
                val totalDuration = mp.duration.toFloat().coerceAtLeast(1f)
                val totalTokens = tokens.size.coerceAtLeast(1)
                while (isActive && mp.isPlaying) {
                    val currentPos = mp.currentPosition.toFloat()
                    val progressRatio = (currentPos / totalDuration).coerceIn(0f, 0.999f)
                    val targetTokenIdx = (progressRatio * totalTokens).toInt().coerceIn(0, totalTokens - 1)
                    if (_activeWordIndex.value != targetTokenIdx) {
                        _activeWordIndex.value = targetTokenIdx
                    }
                    delay(30)
                }
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Erro ao reproduzir áudio da OpenAI: ${e.message}", e)
            _isSpeaking.value = false
            _activeComparativeLineId.value = null
            if (_activeTargetId.value == lineId) {
                _activeTargetId.value = null
            }
            onComplete?.invoke()
        }
    }

    /**
     * Salta a reprodução da voz neural da OpenAI imediatamente para uma palavra específica,
     * sincronizando o destaque visual palavra por palavra instantaneamente.
     */
    fun seekOpenAiToWord(targetOrLineId: String, wordIndex: Int): Boolean {
        val mp = openAiMediaPlayer ?: return false
        if (_activeTargetId.value == targetOrLineId || _activeComparativeLineId.value == targetOrLineId) {
            val tokens = currentSentenceTokens
            if (tokens.isNotEmpty()) {
                val safeIdx = wordIndex.coerceIn(0, tokens.size - 1)
                val ratio = safeIdx.toFloat() / tokens.size.toFloat()
                val targetMs = (ratio * mp.duration).toInt().coerceIn(0, mp.duration)
                try {
                    mp.seekTo(targetMs)
                    _activeWordIndex.value = safeIdx
                    if (!mp.isPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val currentSpeed = _speechSpeed.value.coerceIn(0.4f, 3.0f)
                            mp.playbackParams = mp.playbackParams.setSpeed(currentSpeed)
                        }
                        mp.start()
                        _isSpeaking.value = true
                    }
                    return true
                } catch (e: Exception) {
                    Log.e("PdfSpeechManager", "Erro ao buscar palavra no OpenAI player: ${e.message}")
                }
            }
        }
        return false
    }

    fun speakStudyPortugueseLine(
        text: String,
        lineId: String,
        startWordOffset: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        speakStudyLine(text, Locale("pt", "BR"), lineId, startWordOffset, onComplete)
    }

    fun pauseStudyLine(lineId: String) {
        if (_activeComparativeLineId.value == lineId && _isSpeaking.value) {
            wordProgressJob?.cancel()
            try {
                tts?.stop()
            } catch (_: Exception) {}
            _isSpeaking.value = false
            _isStudyPaused.value = true
        }
    }

    fun resumeStudyLine(
        text: String,
        targetLocale: Locale,
        lineId: String,
        onComplete: (() -> Unit)? = null
    ) {
        val wordOffset = _activeWordIndex.value
        speakStudyLine(text, targetLocale, lineId, wordOffset, onComplete)
    }

    fun toggleStudyLine(
        text: String,
        targetLocale: Locale,
        lineId: String,
        onComplete: (() -> Unit)? = null
    ) {
        if (_activeComparativeLineId.value == lineId) {
            if (_isSpeaking.value) {
                pauseStudyLine(lineId)
            } else {
                resumeStudyLine(text, targetLocale, lineId, onComplete)
            }
        } else {
            speakStudyLine(text, targetLocale, lineId, 0, onComplete)
        }
    }

    fun toggleStudyLine(
        text: String,
        language: StudyLanguage,
        lineId: String,
        onComplete: (() -> Unit)? = null
    ) {
        toggleStudyLine(text, language.ttsLocale, lineId, onComplete)
    }

    fun toggleStudyPortugueseLine(
        text: String,
        lineId: String,
        onComplete: (() -> Unit)? = null
    ) {
        toggleStudyLine(text, Locale("pt", "BR"), lineId, onComplete)
    }

    fun pause() {
        wordProgressJob?.cancel()
        openAiWordProgressJob?.cancel()
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            if (openAiMediaPlayer?.isPlaying == true) {
                openAiMediaPlayer?.pause()
            }
        } catch (e: Exception) {
            // ignore
        }
        _isSpeaking.value = false
    }

    fun resume() {
        if (openAiMediaPlayer != null && _activeComparativeLineId.value != null) {
            try {
                openAiMediaPlayer?.let { mp ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val currentSpeed = _speechSpeed.value.coerceIn(0.4f, 3.0f)
                        mp.playbackParams = mp.playbackParams.setSpeed(currentSpeed)
                    }
                }
                openAiMediaPlayer?.start()
                _isSpeaking.value = true
                val mp = openAiMediaPlayer!!
                val tokens = currentSentenceTokens
                openAiWordProgressJob?.cancel()
                openAiWordProgressJob = CoroutineScope(Dispatchers.Main).launch {
                    val totalDuration = mp.duration.toFloat().coerceAtLeast(1f)
                    val totalTokens = tokens.size.coerceAtLeast(1)
                    while (isActive && mp.isPlaying) {
                        val currentPos = mp.currentPosition.toFloat()
                        val progressRatio = (currentPos / totalDuration).coerceIn(0f, 0.999f)
                        val targetTokenIdx = (progressRatio * totalTokens).toInt().coerceIn(0, totalTokens - 1)
                        if (_activeWordIndex.value != targetTokenIdx) {
                            _activeWordIndex.value = targetTokenIdx
                        }
                        delay(30)
                    }
                }
                return
            } catch (_: Exception) {}
        }
        if (currentSentences.isNotEmpty()) {
            speakSentenceAt(_activeSentenceIndex.value, _activeWordIndex.value)
        }
    }

    fun togglePlayPause() {
        if (_isSpeaking.value) {
            pause()
        } else {
            resume()
        }
    }

    fun setSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.4f, 3.0f)
        _speechSpeed.value = clampedSpeed
        try {
            tts?.setSpeechRate(clampedSpeed)
        } catch (e: Exception) {
            // ignore
        }

        // Aplica imediatamente a nova velocidade (aceleração/desaceleração) no player neural da OpenAI
        try {
            openAiMediaPlayer?.let { mp ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val wasPlaying = mp.isPlaying
                    mp.playbackParams = mp.playbackParams.setSpeed(clampedSpeed)
                    if (!wasPlaying && mp.isPlaying) {
                        mp.pause()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Erro ao ajustar velocidade OpenAI: ${e.message}")
        }

        // Seamlessly restart active speech so the rate change takes effect immediately
        // and word highlighting updates with the new pace in perfect sync (somente se não for OpenAI MediaPlayer já em execução)
        if (_isSpeaking.value && currentSentences.isNotEmpty() && _activeComparativeLineId.value == null && openAiMediaPlayer == null) {
            val currentSentence = _activeSentenceIndex.value
            val currentWord = _activeWordIndex.value
            speakSentenceAt(currentSentence, currentWord)
        } else if (_isSpeaking.value && _activeComparativeLineId.value != null && currentSentences.isNotEmpty() && openAiMediaPlayer == null) {
            val lineId = _activeComparativeLineId.value!!
            val text = currentSentences.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                val currentWord = _activeWordIndex.value
                speakComparativeLine(text, _activeReadingLanguage.value, lineId, currentWord, comparativeFinishedCallback)
            }
        }
    }

    fun increaseSpeed(step: Float = 0.25f) {
        setSpeed((_speechSpeed.value + step).coerceIn(0.5f, 3.0f))
    }

    fun decreaseSpeed(step: Float = 0.25f) {
        setSpeed((_speechSpeed.value - step).coerceIn(0.5f, 3.0f))
    }

    fun setPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        _speechPitch.value = clampedPitch
        applySelectedVoice()

        if (_isSpeaking.value && currentSentences.isNotEmpty() && _activeComparativeLineId.value == null) {
            val currentSentence = _activeSentenceIndex.value
            val currentWord = _activeWordIndex.value
            speakSentenceAt(currentSentence, currentWord)
        }
    }

    fun prepareSentences(
        sentences: List<String>,
        onPageFinished: (() -> Unit)? = null
    ) {
        currentSentences = sentences
        onPageFinishedCallback = onPageFinished
    }

    fun startReading(
        sentences: List<String>,
        startIndex: Int = 0,
        startWordIndex: Int = 0,
        onPageFinished: (() -> Unit)? = null
    ) {
        currentSentences = sentences
        onPageFinishedCallback = onPageFinished
        if (sentences.isEmpty()) {
            _isSpeaking.value = false
            return
        }

        val safeSentenceIndex = startIndex.coerceIn(0, (sentences.size - 1).coerceAtLeast(0))
        val sentence = sentences[safeSentenceIndex]
        val tokens = tokenizeSentence(sentence)
        val safeWordIdx = startWordIndex.coerceIn(0, (tokens.size - 1).coerceAtLeast(0))

        _activeSentenceIndex.value = safeSentenceIndex
        _activeWordIndex.value = safeWordIdx
        _isSpeaking.value = true

        if (!isInitialized) {
            pendingSpeakIndex = safeSentenceIndex
            pendingSpeakWordIndex = safeWordIdx
            return
        }

        speakSentenceAt(safeSentenceIndex, safeWordIdx)
    }

    fun speakFromWord(sentenceIndex: Int, wordIndex: Int) {
        if (sentenceIndex !in currentSentences.indices) return
        val sentence = currentSentences[sentenceIndex]
        val tokens = tokenizeSentence(sentence)
        val safeWordIdx = wordIndex.coerceIn(0, (tokens.size - 1).coerceAtLeast(0))

        _activeSentenceIndex.value = sentenceIndex
        _activeWordIndex.value = safeWordIdx
        _isSpeaking.value = true

        speakSentenceAt(sentenceIndex, safeWordIdx)
    }

    fun speakFromSentence(index: Int) {
        speakFromWord(index, 0)
    }

    fun jumpToSentence(index: Int) {
        speakFromWord(index, 0)
    }

    fun nextSentence() {
        val nextIdx = (_activeSentenceIndex.value + 1).coerceAtMost(currentSentences.size - 1)
        if (nextIdx != _activeSentenceIndex.value) {
            speakSentenceAt(nextIdx, 0)
        }
    }

    fun previousSentence() {
        val prevIdx = (_activeSentenceIndex.value - 1).coerceAtLeast(0)
        if (prevIdx != _activeSentenceIndex.value) {
            speakSentenceAt(prevIdx, 0)
        }
    }

    private fun speakSentenceAt(index: Int, startWordOffset: Int = 0) {
        wordProgressJob?.cancel()
        _isSpeakingCategory.value = false
        _activeComparativeLineId.value = null
        applySelectedVoice()

        if (index !in currentSentences.indices) {
            _isSpeaking.value = false
            _activeWordIndex.value = 0
            return
        }

        val fullSentence = currentSentences[index].trim()
        if (fullSentence.isBlank()) {
            val nextIdx = index + 1
            if (nextIdx < currentSentences.size) {
                speakSentenceAt(nextIdx, 0)
            } else {
                _isSpeaking.value = false
                _activeWordIndex.value = 0
                onPageFinishedCallback?.invoke()
            }
            return
        }

        val tokens = tokenizeSentence(fullSentence)
        if (tokens.isEmpty()) {
            val nextIdx = index + 1
            if (nextIdx < currentSentences.size) {
                speakSentenceAt(nextIdx, 0)
            } else {
                _isSpeaking.value = false
                _activeWordIndex.value = 0
                onPageFinishedCallback?.invoke()
            }
            return
        }

        currentSentenceTokens = tokens
        val safeOffset = startWordOffset.coerceIn(0, tokens.size - 1)
        val targetToken = tokens[safeOffset]

        val charStart = if (safeOffset == 0) 0 else targetToken.startChar
        currentUtteranceStartChar = charStart

        val textToSpeak = if (charStart > 0 && charStart < fullSentence.length) {
            fullSentence.substring(charStart).trim()
        } else {
            fullSentence
        }

        _activeSentenceIndex.value = index
        _activeWordIndex.value = targetToken.index
        _isSpeaking.value = true

        val utteranceId = "${index}_${safeOffset}"

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        try {
            val currentVoice = _selectedVoice.value
            val basePitch = currentVoice?.pitch ?: 1.0f
            tts?.setPitch(basePitch * _speechPitch.value)
            tts?.setSpeechRate(_speechSpeed.value)
            val result = tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result == TextToSpeech.ERROR) {
                @Suppress("DEPRECATION")
                val fallbackParams = HashMap<String, String>().apply {
                    put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                    put(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC.toString())
                }
                @Suppress("DEPRECATION")
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, fallbackParams)
            }
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error in speakSentenceAt: ${e.message}")
        }
    }

    fun speakTextImmediately(text: String) {
        val t = tts ?: return
        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "immediate_speak")
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            t.speak(text, TextToSpeech.QUEUE_FLUSH, params, "immediate_speak")
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error in speakTextImmediately: ${e.message}")
        }
    }

    fun speakTextInLanguage(text: String, language: ReadingLanguage) {
        setReadingLanguage(language)
        speakTextImmediately(text)
    }

    fun speakTextInStudyLanguage(text: String, language: com.example.data.model.StudyLanguage) {
        val t = tts ?: return
        try {
            t.setLanguage(language.ttsLocale)
            val rawVoices = t.voices ?: emptySet()
            val matchedVoice = rawVoices.firstOrNull { v ->
                v.locale.language.equals(language.ttsLocale.language, ignoreCase = true) &&
                (v.name.contains("neural", ignoreCase = true) || v.quality >= Voice.QUALITY_HIGH)
            } ?: rawVoices.firstOrNull { v ->
                v.locale.language.equals(language.ttsLocale.language, ignoreCase = true)
            }
            if (matchedVoice != null) {
                t.voice = matchedVoice
            }
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "immediate_study_speak")
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            t.speak(text, TextToSpeech.QUEUE_FLUSH, params, "immediate_study_speak")
        } catch (e: Exception) {
            Log.e("PdfSpeechManager", "Error in speakTextInStudyLanguage: ${e.message}")
            speakTextImmediately(text)
        }
    }

    /**
     * Detecta o idioma do texto analisando palavras-chave, acentuação e padrões fonéticos
     * para alternar a voz do TTS de forma 100% automática entre Português, Inglês, Francês e Espanhol.
     */
    fun detectLanguageLocale(text: String): Locale {
        val clean = text.trim().lowercase()
        if (clean.isBlank()) return Locale("pt", "BR")

        // Acentos característicos
        val ptAccents = clean.count { it in "áàâãéêíóôõúç" }
        val frAccents = clean.count { it in "œæèêëàùûçîï" }
        val esAccents = clean.count { it in "ñ¿¡" }

        val words = clean.split(Regex("[^\\p{L}0-9]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return Locale("pt", "BR")

        val ptStopwords = setOf(
            "de", "a", "o", "que", "e", "do", "da", "em", "um", "para", "é", "com", "não", "uma", "os", "no",
            "se", "na", "por", "mais", "as", "dos", "como", "mas", "foi", "ao", "ele", "das", "tem", "à", "seu",
            "sua", "ou", "ser", "quando", "muito", "há", "nos", "já", "está", "eu", "também", "só", "pelo",
            "pela", "até", "isso", "ela", "entre", "era", "depois", "sem", "mesmo", "aos", "ter", "seus", "quem",
            "nas", "me", "esse", "eles", "estão", "você", "tinha", "foram", "essa", "num", "nem", "suas", "meu",
            "animal", "penas", "asas", "voa", "milho", "semente", "vive", "natureza", "significa", "exemplo"
        )

        val enStopwords = setOf(
            "the", "of", "and", "a", "to", "in", "is", "you", "that", "it", "he", "was", "for", "on", "are",
            "as", "with", "his", "they", "i", "at", "be", "this", "have", "from", "or", "one", "had", "by",
            "word", "but", "not", "what", "all", "were", "we", "when", "your", "can", "said", "there", "use",
            "an", "each", "which", "she", "do", "how", "their", "if", "will", "up", "other", "about", "out",
            "many", "then", "them", "these", "so", "some", "her", "would", "make", "like", "him", "into",
            "time", "has", "look", "two", "more", "write", "go", "see", "number", "no", "way", "could", "people",
            "my", "than", "first", "water", "been", "call", "who", "oil", "its", "now", "find", "long", "down",
            "day", "did", "get", "come", "made", "may", "part", "bird", "feather", "wing", "fly", "seed",
            "sentence", "meaning", "definition", "verb", "phrasal", "example", "question"
        )

        val frStopwords = setOf(
            "le", "la", "les", "des", "un", "une", "du", "et", "est", "en", "que", "qui", "dans", "pour", "sur",
            "avec", "ce", "cette", "ces", "plus", "pas", "son", "sa", "ses", "au", "aux", "par", "mais", "nous",
            "vous", "ils", "elles", "comme", "ou", "si", "leur", "leurs", "être", "avoir"
        )

        val esStopwords = setOf(
            "el", "la", "de", "que", "y", "a", "en", "un", "ser", "se", "no", "haber", "por", "con", "su",
            "para", "como", "estar", "tener", "le", "lo", "todo", "pero", "más", "hacer", "o", "poder", "decir",
            "este", "ir", "otro", "ese", "si", "me", "ya", "ver", "porque", "dar", "cuando", "él", "muy", "sin",
            "vez", "mucho", "saber", "qué", "sobre", "mi", "alguno", "mismo", "yo", "también", "hasta"
        )

        var ptScore = ptAccents * 3 + words.count { it in ptStopwords } * 2
        var enScore = words.count { it in enStopwords } * 2
        var frScore = frAccents * 3 + words.count { it in frStopwords } * 2
        var esScore = esAccents * 3 + words.count { it in esStopwords } * 2

        if (words.any { it.endsWith("ing") || it.endsWith("tion") || it.endsWith("ed") || it.startsWith("th") || it.startsWith("wh") }) {
            enScore += 2
        }

        return when {
            enScore > ptScore && enScore > frScore && enScore > esScore -> Locale.ENGLISH
            frScore > ptScore && frScore > enScore && frScore > esScore -> Locale.FRENCH
            esScore > ptScore && esScore > enScore && esScore > frScore -> Locale("es", "ES")
            else -> Locale("pt", "BR")
        }
    }

    /**
     * Inicia a leitura falada por voz com rastreamento sincronizado palavra por palavra (Karaokê)
     * para qualquer bloco, artigo, explicação, questão ou verbete de dicionário,
     * detectando e aplicando automaticamente a voz do idioma correto.
     */
    fun startReadingTarget(
        targetId: String,
        text: String,
        startWordIndex: Int = 0,
        onFinished: (() -> Unit)? = null
    ) {
        val clean = text.replace("~~", "").trim()
        if (clean.isBlank()) return
        _activeTargetId.value = targetId
        val detectedLocale = detectLanguageLocale(clean)
        applyLocaleToTts(detectedLocale)
        startReading(
            sentences = listOf(clean),
            startIndex = 0,
            startWordIndex = startWordIndex,
            onPageFinished = {
                if (_activeTargetId.value == targetId) {
                    _activeTargetId.value = null
                }
                onFinished?.invoke()
            }
        )
    }

    /**
     * Salta a leitura da voz imediatamente para a palavra clicada (inclusive voltando para palavras anteriores),
     * mantendo a voz e o destaque visual perfeitamente sincronizados no idioma apropriado.
     */
    fun speakFromWordInTarget(targetId: String, text: String, wordIndex: Int) {
        if (seekOpenAiToWord(targetId, wordIndex)) {
            return
        }
        val clean = text.replace("~~", "").trim()
        if (clean.isBlank()) return
        _activeTargetId.value = targetId
        val detectedLocale = detectLanguageLocale(clean)
        applyLocaleToTts(detectedLocale)
        if (currentSentences.isEmpty() || currentSentences.firstOrNull() != clean) {
            startReading(
                sentences = listOf(clean),
                startIndex = 0,
                startWordIndex = wordIndex,
                onPageFinished = {
                    if (_activeTargetId.value == targetId) {
                        _activeTargetId.value = null
                    }
                }
            )
        } else {
            speakFromWord(sentenceIndex = 0, wordIndex = wordIndex)
        }
    }

    fun stopTarget(targetId: String? = null) {
        if (targetId == null || _activeTargetId.value == targetId) {
            stop()
        }
    }

    fun speakAutoLanguageLine(
        text: String,
        lineId: String,
        startWordOffset: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        val detected = detectLanguageLocale(text)
        speakStudyLine(text, detected, lineId, startWordOffset, onComplete)
    }

    fun toggleAutoLanguageLine(
        text: String,
        lineId: String,
        onComplete: (() -> Unit)? = null
    ) {
        val detected = detectLanguageLocale(text)
        toggleStudyLine(text, detected, lineId, onComplete)
    }

    fun release() {
        wordProgressJob?.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }

    fun shutdown() {
        release()
    }
}
