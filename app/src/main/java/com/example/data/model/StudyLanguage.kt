package com.example.data.model

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Idiomas de suporte para aprendizagem profunda nível C2 / Poliglota:
 * Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico e Inglês.
 */
enum class StudyLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean,
    val scriptFamily: String,
    val ttsLocale: Locale,
    val badgeColorHex: Long
) {
    RUSSIAN(
        code = "ru",
        displayName = "Russo",
        nativeName = "Русский",
        flag = "🇷🇺",
        isRtl = false,
        scriptFamily = "Cirílico",
        ttsLocale = Locale("ru", "RU"),
        badgeColorHex = 0xFFEF4444
    ),
    GERMAN(
        code = "de",
        displayName = "Alemão",
        nativeName = "Deutsch",
        flag = "🇩🇪",
        isRtl = false,
        scriptFamily = "Latino / Germânico",
        ttsLocale = Locale.GERMANY,
        badgeColorHex = 0xFFF59E0B
    ),
    GREEK(
        code = "el",
        displayName = "Grego",
        nativeName = "Ελληνικά",
        flag = "🇬🇷",
        isRtl = false,
        scriptFamily = "Grego",
        ttsLocale = Locale("el", "GR"),
        badgeColorHex = 0xFF06B6D4
    ),
    CHINESE(
        code = "zh",
        displayName = "Chinês (Mandarim)",
        nativeName = "中文 (普通话)",
        flag = "🇨🇳",
        isRtl = false,
        scriptFamily = "Hanzi / Pinyin",
        ttsLocale = Locale.CHINA,
        badgeColorHex = 0xFFDC2626
    ),
    JAPANESE(
        code = "ja",
        displayName = "Japonês",
        nativeName = "日本語",
        flag = "🇯🇵",
        isRtl = false,
        scriptFamily = "Kanji / Furigana",
        ttsLocale = Locale.JAPAN,
        badgeColorHex = 0xFFEC4899
    ),
    UKRAINIAN(
        code = "uk",
        displayName = "Ucraniano",
        nativeName = "Українська",
        flag = "🇺🇦",
        isRtl = false,
        scriptFamily = "Cirílico",
        ttsLocale = Locale("uk", "UA"),
        badgeColorHex = 0xFF3B82F6
    ),
    ARABIC(
        code = "ar",
        displayName = "Árabe",
        nativeName = "العربية",
        flag = "🇸🇦",
        isRtl = true,
        scriptFamily = "Árabe / Tashkeel",
        ttsLocale = Locale("ar", "SA"),
        badgeColorHex = 0xFF10B981
    ),
    HEBREW(
        code = "he",
        displayName = "Hebraico",
        nativeName = "עברית",
        flag = "🇮🇱",
        isRtl = true,
        scriptFamily = "Hebraico / Niqqud",
        ttsLocale = Locale("iw", "IL"),
        badgeColorHex = 0xFF8B5CF6
    ),
    ENGLISH(
        code = "en",
        displayName = "Inglês",
        nativeName = "English",
        flag = "🇺🇸",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale.US,
        badgeColorHex = 0xFF1D4ED8
    ),
    SPANISH(
        code = "es",
        displayName = "Espanhol",
        nativeName = "Español",
        flag = "🇪🇸",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("es", "ES"),
        badgeColorHex = 0xFFDC2626
    ),
    FRENCH(
        code = "fr",
        displayName = "Francês",
        nativeName = "Français",
        flag = "🇫🇷",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale.FRANCE,
        badgeColorHex = 0xFF2563EB
    ),
    ITALIAN(
        code = "it",
        displayName = "Italiano",
        nativeName = "Italiano",
        flag = "🇮🇹",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale.ITALY,
        badgeColorHex = 0xFF16A34A
    ),
    PORTUGUESE(
        code = "pt",
        displayName = "Português",
        nativeName = "Português",
        flag = "🇧🇷",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("pt", "BR"),
        badgeColorHex = 0xFF059669
    ),
    KOREAN(
        code = "ko",
        displayName = "Coreano",
        nativeName = "한국어",
        flag = "🇰🇷",
        isRtl = false,
        scriptFamily = "Hangul",
        ttsLocale = Locale.KOREA,
        badgeColorHex = 0xFF4F46E5
    ),
    DUTCH(
        code = "nl",
        displayName = "Holandês",
        nativeName = "Nederlands",
        flag = "🇳🇱",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("nl", "NL"),
        badgeColorHex = 0xFFEA580C
    ),
    POLISH(
        code = "pl",
        displayName = "Polonês",
        nativeName = "Polski",
        flag = "🇵🇱",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("pl", "PL"),
        badgeColorHex = 0xFFE11D48
    ),
    TURKISH(
        code = "tr",
        displayName = "Turco",
        nativeName = "Türkçe",
        flag = "🇹🇷",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("tr", "TR"),
        badgeColorHex = 0xFFDC2626
    ),
    SWEDISH(
        code = "sv",
        displayName = "Sueco",
        nativeName = "Svenska",
        flag = "🇸🇪",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("sv", "SE"),
        badgeColorHex = 0xFF0284C7
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        flag = "🇮🇳",
        isRtl = false,
        scriptFamily = "Devanagari",
        ttsLocale = Locale("hi", "IN"),
        badgeColorHex = 0xFFD97706
    ),
    ROMANIAN(
        code = "ro",
        displayName = "Romeno",
        nativeName = "Română",
        flag = "🇷🇴",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("ro", "RO"),
        badgeColorHex = 0xFF2563EB
    ),
    INDONESIAN(
        code = "id",
        displayName = "Indonésio",
        nativeName = "Bahasa Indonesia",
        flag = "🇮🇩",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("id", "ID"),
        badgeColorHex = 0xFFDC2626
    ),
    VIETNAMESE(
        code = "vi",
        displayName = "Vietnamita",
        nativeName = "Tiếng Việt",
        flag = "🇻🇳",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("vi", "VN"),
        badgeColorHex = 0xFFEAB308
    ),
    NORWEGIAN(
        code = "no",
        displayName = "Norueguês",
        nativeName = "Norsk",
        flag = "🇳🇴",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("no", "NO"),
        badgeColorHex = 0xFF0284C7
    ),
    DANISH(
        code = "da",
        displayName = "Dinamarquês",
        nativeName = "Dansk",
        flag = "🇩🇰",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("da", "DK"),
        badgeColorHex = 0xFFBE123C
    ),
    FINNISH(
        code = "fi",
        displayName = "Finlandês",
        nativeName = "Suomi",
        flag = "🇫🇮",
        isRtl = false,
        scriptFamily = "Uralic",
        ttsLocale = Locale("fi", "FI"),
        badgeColorHex = 0xFF3B82F6
    ),
    CZECH(
        code = "cs",
        displayName = "Tcheco",
        nativeName = "Čeština",
        flag = "🇨🇿",
        isRtl = false,
        scriptFamily = "Latino",
        ttsLocale = Locale("cs", "CZ"),
        badgeColorHex = 0xFF1D4ED8
    );

    val badgeColor: Color
        get() = Color(badgeColorHex)

    companion object {
        val allSupportedLanguages: List<StudyLanguage> = listOf(
            PORTUGUESE,
            ENGLISH,
            SPANISH,
            FRENCH,
            GERMAN,
            ITALIAN,
            RUSSIAN,
            CHINESE,
            JAPANESE,
            KOREAN,
            UKRAINIAN,
            GREEK,
            ARABIC,
            HEBREW,
            DUTCH,
            POLISH,
            TURKISH,
            SWEDISH,
            HINDI,
            ROMANIAN,
            INDONESIAN,
            VIETNAMESE,
            NORWEGIAN,
            DANISH,
            FINNISH,
            CZECH
        )

        fun fromCode(code: String): StudyLanguage {
            val clean = code.trim().lowercase()
            return entries.firstOrNull { it.code == clean } ?: ENGLISH
        }

        /**
         * Detecta o idioma a partir do texto ou scripts predominantes.
         */
        fun detectFromText(text: String): StudyLanguage {
            if (text.isBlank()) return ENGLISH

            var cyrillicCount = 0
            var ukrainianSpecificCount = 0 // і, ї, є, ґ
            var russianSpecificCount = 0   // ы, э, ъ, ё
            var arabicCount = 0
            var hebrewCount = 0
            var cjkCount = 0
            var kanaCount = 0
            var greekCount = 0
            var germanSpecificCount = 0   // ä, ö, ü, ß

            for (ch in text) {
                when {
                    // Ucraniano vs Russo
                    ch == 'і' || ch == 'І' || ch == 'ї' || ch == 'Ї' || ch == 'є' || ch == 'Є' || ch == 'ґ' || ch == 'Ґ' -> {
                        ukrainianSpecificCount++
                        cyrillicCount++
                    }
                    ch == 'ы' || ch == 'Ы' || ch == 'э' || ch == 'Э' || ch == 'ъ' || ch == 'Ъ' || ch == 'ё' || ch == 'Ё' -> {
                        russianSpecificCount++
                        cyrillicCount++
                    }
                    ch in '\u0400'..'\u04FF' -> cyrillicCount++
                    ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' -> arabicCount++
                    ch in '\u0590'..'\u05FF' -> hebrewCount++
                    ch in '\u3040'..'\u309F' || ch in '\u30A0'..'\u30FF' -> kanaCount++
                    ch in '\u4E00'..'\u9FFF' || ch in '\u3400'..'\u4DBF' -> cjkCount++
                    ch in '\u0370'..'\u03FF' || ch in '\u1F00'..'\u1FFF' -> greekCount++
                    ch == 'ä' || ch == 'Ä' || ch == 'ö' || ch == 'Ö' || ch == 'ü' || ch == 'Ü' || ch == 'ß' -> germanSpecificCount++
                }
            }

            val total = text.length.coerceAtLeast(1)

            if (arabicCount > 5) return ARABIC
            if (hebrewCount > 5) return HEBREW
            if (greekCount > 5) return GREEK
            if (kanaCount > 2) return JAPANESE
            if (cjkCount > 5) return if (kanaCount > 0) JAPANESE else CHINESE

            if (cyrillicCount > 5) {
                return if (ukrainianSpecificCount > russianSpecificCount) UKRAINIAN else RUSSIAN
            }

            if (germanSpecificCount > 2 || text.contains(" der ", ignoreCase = true) || text.contains(" die ", ignoreCase = true) || text.contains(" das ", ignoreCase = true) || text.contains(" und ", ignoreCase = true)) {
                return GERMAN
            }

            return ENGLISH
        }
    }
}
