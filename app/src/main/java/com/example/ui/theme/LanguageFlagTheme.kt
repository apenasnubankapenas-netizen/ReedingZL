package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.StudyLanguage

/**
 * Paleta de cores temática inspirada estritamente na bandeira de cada país para o estudo
 * exclusivo e profissional de cada idioma na seção de dicionário.
 * Conforme exigido: a estilização nas cores da bandeira só afeta esta seção específica de estudos.
 */
data class LanguageFlagTheme(
    val language: StudyLanguage,
    val countryName: String,
    val flagEmoji: String,
    val containerBackground: Color,
    val surfaceCard: Color,
    val cardBorder: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryAccent: Color,
    val headerGradient: Brush,
    val textPrimary: Color,
    val textSecondary: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val quadradinhoBg: Color,
    val quadradinhoBorder: Color,
    val quadradinhoSelectedBorder: Color,
    val pillPrepositionBg: Color,
    val pillPrepositionText: Color
) {
    companion object {
        fun getThemeFor(language: StudyLanguage): LanguageFlagTheme {
            return when (language) {
                StudyLanguage.ENGLISH -> LanguageFlagTheme(
                    language = StudyLanguage.ENGLISH,
                    countryName = "Estados Unidos",
                    flagEmoji = "🇺🇸",
                    // Cores da bandeira americana: Deep Navy Blue, Bright Crimson Red, Crisp White
                    containerBackground = Color(0xFF091224),
                    surfaceCard = Color(0xFF0F1E3D),
                    cardBorder = Color(0xFF2563EB).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF2563EB),    // Azul Real
                    secondaryAccent = Color(0xFFEF4444),  // Vermelho Rubro
                    tertiaryAccent = Color(0xFFFFFFFF),   // Branco
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF1E3A8A), Color(0xFF991B1B), Color(0xFF1E3A8A))
                    ),
                    textPrimary = Color(0xFFF8FAFC),
                    textSecondary = Color(0xFF93C5FD),
                    badgeBg = Color(0xFFEF4444).copy(alpha = 0.2f),
                    badgeText = Color(0xFFFCA5A5),
                    quadradinhoBg = Color(0xFF0E1A34),
                    quadradinhoBorder = Color(0xFF1D4ED8).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFEF4444),
                    pillPrepositionBg = Color(0xFFEF4444).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFECACA)
                )

                StudyLanguage.SPANISH -> LanguageFlagTheme(
                    language = StudyLanguage.SPANISH,
                    countryName = "Espanha",
                    flagEmoji = "🇪🇸",
                    // Cores da bandeira espanhola: Vermelho Rojigualda e Ouro/Amarelo Solar
                    containerBackground = Color(0xFF1C0A0C),
                    surfaceCard = Color(0xFF2B1014),
                    cardBorder = Color(0xFFEAB308).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFFDC2626),    // Vermelho Espanha
                    secondaryAccent = Color(0xFFFACC15),  // Amarelo Ouro
                    tertiaryAccent = Color(0xFFFEF08A),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF991B1B), Color(0xFFCA8A04), Color(0xFF991B1B))
                    ),
                    textPrimary = Color(0xFFFEF3C7),
                    textSecondary = Color(0xFFFDE047),
                    badgeBg = Color(0xFFDC2626).copy(alpha = 0.25f),
                    badgeText = Color(0xFFFECACA),
                    quadradinhoBg = Color(0xFF240D10),
                    quadradinhoBorder = Color(0xFFCA8A04).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFFACC15),
                    pillPrepositionBg = Color(0xFFEAB308).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFEF08A)
                )

                StudyLanguage.FRENCH -> LanguageFlagTheme(
                    language = StudyLanguage.FRENCH,
                    countryName = "França",
                    flagEmoji = "🇫🇷",
                    // Cores da bandeira francesa: Bleu, Blanc, Rouge
                    containerBackground = Color(0xFF081226),
                    surfaceCard = Color(0xFF101F3D),
                    cardBorder = Color(0xFF3B82F6).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF2563EB),
                    secondaryAccent = Color(0xFFEF4444),
                    tertiaryAccent = Color(0xFFFFFFFF),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF1D4ED8), Color(0xFF334155), Color(0xFFDC2626))
                    ),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFBFDBFE),
                    badgeBg = Color(0xFF3B82F6).copy(alpha = 0.2f),
                    badgeText = Color(0xFF93C5FD),
                    quadradinhoBg = Color(0xFF0D1830),
                    quadradinhoBorder = Color(0xFF3B82F6).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFEF4444),
                    pillPrepositionBg = Color(0xFFEF4444).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFECACA)
                )

                StudyLanguage.GERMAN -> LanguageFlagTheme(
                    language = StudyLanguage.GERMAN,
                    countryName = "Alemanha",
                    flagEmoji = "🇩🇪",
                    // Cores da bandeira alemã: Schwarz, Rot, Gold
                    containerBackground = Color(0xFF121214),
                    surfaceCard = Color(0xFF1E1E22),
                    cardBorder = Color(0xFFEAB308).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFFEF4444),
                    secondaryAccent = Color(0xFFFACC15),
                    tertiaryAccent = Color(0xFF3F3F46),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF18181B), Color(0xFFDC2626), Color(0xFFCA8A04))
                    ),
                    textPrimary = Color(0xFFFEF9C3),
                    textSecondary = Color(0xFFE4E4E7),
                    badgeBg = Color(0xFFEF4444).copy(alpha = 0.25f),
                    badgeText = Color(0xFFFCA5A5),
                    quadradinhoBg = Color(0xFF18181C),
                    quadradinhoBorder = Color(0xFFCA8A04).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFFACC15),
                    pillPrepositionBg = Color(0xFFEAB308).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFEF08A)
                )

                StudyLanguage.RUSSIAN -> LanguageFlagTheme(
                    language = StudyLanguage.RUSSIAN,
                    countryName = "Rússia",
                    flagEmoji = "🇷🇺",
                    // Cores da bandeira russa: Branco, Azul Imperial, Vermelho
                    containerBackground = Color(0xFF0B1428),
                    surfaceCard = Color(0xFF122040),
                    cardBorder = Color(0xFF3B82F6).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF2563EB),
                    secondaryAccent = Color(0xFFEF4444),
                    tertiaryAccent = Color(0xFFFFFFFF),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF1E3A8A), Color(0xFFDC2626))
                    ),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFBFDBFE),
                    badgeBg = Color(0xFFDC2626).copy(alpha = 0.25f),
                    badgeText = Color(0xFFFCA5A5),
                    quadradinhoBg = Color(0xFF0F1A33),
                    quadradinhoBorder = Color(0xFF3B82F6).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFEF4444),
                    pillPrepositionBg = Color(0xFF2563EB).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFBFDBFE)
                )

                StudyLanguage.JAPANESE -> LanguageFlagTheme(
                    language = StudyLanguage.JAPANESE,
                    countryName = "Japão",
                    flagEmoji = "🇯🇵",
                    // Cores da bandeira japonesa: Fundo Branco Puro e Sol Vermelho Carmesim
                    containerBackground = Color(0xFF151419),
                    surfaceCard = Color(0xFF221F28),
                    cardBorder = Color(0xFFE11D48).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFFE11D48),
                    secondaryAccent = Color(0xFFFFFFFF),
                    tertiaryAccent = Color(0xFFFDA4AF),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF4C0519), Color(0xFFBE123C), Color(0xFF4C0519))
                    ),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFFECDD3),
                    badgeBg = Color(0xFFE11D48).copy(alpha = 0.25f),
                    badgeText = Color(0xFFFDA4AF),
                    quadradinhoBg = Color(0xFF1B1922),
                    quadradinhoBorder = Color(0xFFE11D48).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFFB7185),
                    pillPrepositionBg = Color(0xFFE11D48).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFFE4E6)
                )

                StudyLanguage.ITALIAN -> LanguageFlagTheme(
                    language = StudyLanguage.ITALIAN,
                    countryName = "Itália",
                    flagEmoji = "🇮🇹",
                    // Cores da bandeira italiana: Verde Esmeralda, Branco, Vermelho
                    containerBackground = Color(0xFF091711),
                    surfaceCard = Color(0xFF10261D),
                    cardBorder = Color(0xFF16A34A).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF16A34A),
                    secondaryAccent = Color(0xFFDC2626),
                    tertiaryAccent = Color(0xFFFFFFFF),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF15803D), Color(0xFF1E293B), Color(0xFFB91C1C))
                    ),
                    textPrimary = Color(0xFFF0FDF4),
                    textSecondary = Color(0xFF86EFAC),
                    badgeBg = Color(0xFF16A34A).copy(alpha = 0.25f),
                    badgeText = Color(0xFF86EFAC),
                    quadradinhoBg = Color(0xFF0C1F17),
                    quadradinhoBorder = Color(0xFF16A34A).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFDC2626),
                    pillPrepositionBg = Color(0xFFDC2626).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFECACA)
                )

                StudyLanguage.CHINESE -> LanguageFlagTheme(
                    language = StudyLanguage.CHINESE,
                    countryName = "China",
                    flagEmoji = "🇨🇳",
                    // Cores da bandeira chinesa: Vermelho Imperial e Estrelas Douradas
                    containerBackground = Color(0xFF1B0909),
                    surfaceCard = Color(0xFF2C1010),
                    cardBorder = Color(0xFFEAB308).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFFDC2626),
                    secondaryAccent = Color(0xFFFACC15),
                    tertiaryAccent = Color(0xFFFEF08A),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF991B1B), Color(0xFFB45309), Color(0xFF991B1B))
                    ),
                    textPrimary = Color(0xFFFEF9C3),
                    textSecondary = Color(0xFFFDE047),
                    badgeBg = Color(0xFFDC2626).copy(alpha = 0.25f),
                    badgeText = Color(0xFFFECACA),
                    quadradinhoBg = Color(0xFF220C0C),
                    quadradinhoBorder = Color(0xFFCA8A04).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFFACC15),
                    pillPrepositionBg = Color(0xFFEAB308).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFEF08A)
                )

                StudyLanguage.UKRAINIAN -> LanguageFlagTheme(
                    language = StudyLanguage.UKRAINIAN,
                    countryName = "Ucrânia",
                    flagEmoji = "🇺🇦",
                    // Cores da bandeira ucraniana: Azul Celeste e Amarelo Trigo
                    containerBackground = Color(0xFF081326),
                    surfaceCard = Color(0xFF102140),
                    cardBorder = Color(0xFFEAB308).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF2563EB),
                    secondaryAccent = Color(0xFFFACC15),
                    tertiaryAccent = Color(0xFF93C5FD),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF1D4ED8), Color(0xFFCA8A04))
                    ),
                    textPrimary = Color(0xFFFEF9C3),
                    textSecondary = Color(0xFF93C5FD),
                    badgeBg = Color(0xFF2563EB).copy(alpha = 0.25f),
                    badgeText = Color(0xFFBFDBFE),
                    quadradinhoBg = Color(0xFF0D1B33),
                    quadradinhoBorder = Color(0xFFCA8A04).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFFACC15),
                    pillPrepositionBg = Color(0xFFEAB308).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFFEF08A)
                )

                StudyLanguage.GREEK -> LanguageFlagTheme(
                    language = StudyLanguage.GREEK,
                    countryName = "Grécia",
                    flagEmoji = "🇬🇷",
                    // Cores da bandeira grega: Azul Marinho Egeu e Branco
                    containerBackground = Color(0xFF08162B),
                    surfaceCard = Color(0xFF102444),
                    cardBorder = Color(0xFF06B6D4).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF0284C7),
                    secondaryAccent = Color(0xFFFFFFFF),
                    tertiaryAccent = Color(0xFF67E8F9),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF0369A1), Color(0xFF0891B2))
                    ),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFA5F3FC),
                    badgeBg = Color(0xFF0284C7).copy(alpha = 0.25f),
                    badgeText = Color(0xFFBAE6FD),
                    quadradinhoBg = Color(0xFF0C1E3A),
                    quadradinhoBorder = Color(0xFF0284C7).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFF67E8F9),
                    pillPrepositionBg = Color(0xFF0284C7).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFBAE6FD)
                )

                StudyLanguage.ARABIC -> LanguageFlagTheme(
                    language = StudyLanguage.ARABIC,
                    countryName = "Arábia",
                    flagEmoji = "🇸🇦",
                    // Cores da bandeira: Verde Árabe, Branco e Ouro
                    containerBackground = Color(0xFF081810),
                    surfaceCard = Color(0xFF0F2C1E),
                    cardBorder = Color(0xFF10B981).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF059669),
                    secondaryAccent = Color(0xFFFACC15),
                    tertiaryAccent = Color(0xFFFFFFFF),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF047857), Color(0xFF15803D))
                    ),
                    textPrimary = Color(0xFFECFDF5),
                    textSecondary = Color(0xFF6EE7B7),
                    badgeBg = Color(0xFF059669).copy(alpha = 0.25f),
                    badgeText = Color(0xFFA7F3D0),
                    quadradinhoBg = Color(0xFF0B2117),
                    quadradinhoBorder = Color(0xFF10B981).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFFACC15),
                    pillPrepositionBg = Color(0xFF059669).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFA7F3D0)
                )

                StudyLanguage.HEBREW -> LanguageFlagTheme(
                    language = StudyLanguage.HEBREW,
                    countryName = "Israel",
                    flagEmoji = "🇮🇱",
                    // Cores da bandeira de Israel: Azul Estrela de Davi e Branco
                    containerBackground = Color(0xFF091428),
                    surfaceCard = Color(0xFF122244),
                    cardBorder = Color(0xFF38BDF8).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF2563EB),
                    secondaryAccent = Color(0xFFFFFFFF),
                    tertiaryAccent = Color(0xFF38BDF8),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF1E40AF), Color(0xFF0284C7))
                    ),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFBAE6FD),
                    badgeBg = Color(0xFF2563EB).copy(alpha = 0.25f),
                    badgeText = Color(0xFFBFDBFE),
                    quadradinhoBg = Color(0xFF0D1C3B),
                    quadradinhoBorder = Color(0xFF38BDF8).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFF60A5FA),
                    pillPrepositionBg = Color(0xFF2563EB).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFBFDBFE)
                )
                StudyLanguage.PORTUGUESE -> LanguageFlagTheme(
                    language = StudyLanguage.PORTUGUESE,
                    countryName = "Brasil",
                    flagEmoji = "🇧🇷",
                    containerBackground = Color(0xFF031A0D),
                    surfaceCard = Color(0xFF082818),
                    cardBorder = Color(0xFF10B981).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF10B981),
                    secondaryAccent = Color(0xFFFBBF24),
                    tertiaryAccent = Color(0xFF38BDF8),
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF047857), Color(0xFFD97706), Color(0xFF1D4ED8))
                    ),
                    textPrimary = Color(0xFFF0FDF4),
                    textSecondary = Color(0xFFBBF7D0),
                    badgeBg = Color(0xFF10B981).copy(alpha = 0.25f),
                    badgeText = Color(0xFF6EE7B7),
                    quadradinhoBg = Color(0xFF062013),
                    quadradinhoBorder = Color(0xFF059669).copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFFFBBF24),
                    pillPrepositionBg = Color(0xFF047857).copy(alpha = 0.25f),
                    pillPrepositionText = Color(0xFFA7F3D0)
                )
                else -> LanguageFlagTheme(
                    language = language,
                    countryName = language.displayName,
                    flagEmoji = language.flag,
                    containerBackground = Color(0xFF0F172A),
                    surfaceCard = Color(0xFF1E293B),
                    cardBorder = language.badgeColor.copy(alpha = 0.6f),
                    primaryAccent = language.badgeColor,
                    secondaryAccent = Color(0xFF38BDF8),
                    tertiaryAccent = Color.White,
                    headerGradient = Brush.horizontalGradient(
                        listOf(Color(0xFF0F172A), language.badgeColor.copy(alpha = 0.7f), Color(0xFF0F172A))
                    ),
                    textPrimary = Color.White,
                    textSecondary = Color(0xFF94A3B8),
                    badgeBg = language.badgeColor.copy(alpha = 0.25f),
                    badgeText = Color.White,
                    quadradinhoBg = Color(0xFF1E293B),
                    quadradinhoBorder = language.badgeColor.copy(alpha = 0.5f),
                    quadradinhoSelectedBorder = Color(0xFF38BDF8),
                    pillPrepositionBg = language.badgeColor.copy(alpha = 0.25f),
                    pillPrepositionText = Color.White
                )
            }
        }
    }
}
