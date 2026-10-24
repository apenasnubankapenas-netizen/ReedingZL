package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.BookCategoryTaxonomy

data class ReadingThemePalette(
    val themeId: String,
    val displayName: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val border: Color,
    val cardBorder: Color,
    val divider: Color,
    val buttonContainer: Color,
    val buttonContent: Color,
    val buttonSecondaryContainer: Color,
    val buttonSecondaryContent: Color,
    val hudDockBg: Color,
    val hudActiveBtn: Color,
    val hudActiveIcon: Color,
    val hudInactiveBtn: Color,
    val hudInactiveIcon: Color,
    val hudLabelColor: Color,
    val badgeContainer: Color,
    val badgeContent: Color,
    val activeSentenceBg: Color,
    val activeWordBg: Color,
    val activeWordText: Color,
    val isLight: Boolean = false
) {
    val karaokeActiveText: Color get() = activeWordText
    val karaokeActiveBg: Color get() = activeWordBg
}

object ReadingThemeManager {

    data class ThemeOption(
        val id: String,
        val label: String,
        val subtitle: String,
        val iconEmoji: String
    )

    val THEMES = listOf(
        ThemeOption("DARK_OLED", "Dark OLED Profundo", "Preto absoluto com toques violeta", "⚫"),
        ThemeOption("CYBER_NEON_NIGHT", "Ciber Noturno Alta Concentração", "10 cores neon: fundo azul escuro, ciano, magenta e rosa", "🌌"),
        ThemeOption("MONOCHROME_BW", "Preto e Branco Alto Contraste", "E-Ink minimalista e descansado", "⚪"),
        ThemeOption("PAPER_SEPIA", "Papel Sépiado E-Ink", "Tons quentes aconchegantes", "📜"),
        ThemeOption("CYBERPUNK_NEON", "Cyberpunk Neon", "Visual futurista ciano e magenta", "⚡"),
        ThemeOption("CINEMA_YELLOW", "Legenda Cinema Amarelo", "Alto contraste com realce dourado", "🎬"),
        ThemeOption("TERMINAL_GREEN", "Terminal Matrix Retro", "Verde fosforoso hacker clássico", "📟")
    )

    fun getPaletteForCategory(
        category: String?,
        themeId: String? = "DARK_OLED",
        categoryColor: Any? = null
    ): ReadingThemePalette {
        return getPalette(themeId, category, categoryColor)
    }

    fun getPalette(
        themeId: String?,
        category: Any? = null,
        categoryColor: Any? = null
    ): ReadingThemePalette {
        if (themeId == "CATEGORY_COLOR") {
            val catName = category as? String
            val colHex = categoryColor as? String
            val catPal = BookCategoryTaxonomy.getPaletteForCategory(catName, colHex)
            return ReadingThemePalette(
                themeId = "CATEGORY_COLOR",
                displayName = catName ?: "Categoria",
                background = catPal.headerBg,
                surface = catPal.surfaceColor,
                surfaceVariant = catPal.cardBg,
                surfaceElevated = catPal.containerColor,
                textPrimary = catPal.textColor,
                textSecondary = catPal.textColor.copy(alpha = 0.75f),
                textTertiary = catPal.textColor.copy(alpha = 0.5f),
                accent = catPal.accentColor,
                accentSecondary = catPal.glowColor,
                border = catPal.accentColor.copy(alpha = 0.4f),
                cardBorder = catPal.accentColor.copy(alpha = 0.5f),
                divider = catPal.accentColor.copy(alpha = 0.25f),
                buttonContainer = catPal.primaryColor,
                buttonContent = Color.White,
                buttonSecondaryContainer = catPal.badgeBg,
                buttonSecondaryContent = catPal.accentColor,
                hudDockBg = catPal.headerBg.copy(alpha = 0.92f),
                hudActiveBtn = catPal.primaryColor,
                hudActiveIcon = Color.White,
                hudInactiveBtn = catPal.badgeBg,
                hudInactiveIcon = catPal.textColor.copy(alpha = 0.7f),
                hudLabelColor = catPal.textColor.copy(alpha = 0.7f),
                badgeContainer = catPal.badgeBg,
                badgeContent = catPal.accentColor,
                activeSentenceBg = catPal.activeSentenceBg,
                activeWordBg = catPal.accentColor,
                activeWordText = Color.White,
                isLight = false
            )
        }
        if (themeId == "STREAMING_BOX") {
            return ReadingThemePalette(
                themeId = "STREAMING_BOX",
                displayName = "Closed Captions",
                background = Color(0xFF101115),
                surface = Color(0xFF16171E),
                surfaceVariant = Color(0xFF212538),
                surfaceElevated = Color(0xFF2B3047),
                textPrimary = Color(0xFFF3F4F6),
                textSecondary = Color(0xFF9CA3AF),
                textTertiary = Color(0xFF6B7280),
                accent = Color(0xFF818CF8),
                accentSecondary = Color(0xFFA5B4FC),
                border = Color(0xFF374151),
                cardBorder = Color(0xFF4B5563),
                divider = Color(0xFF1F2937),
                buttonContainer = Color(0xFF6366F1),
                buttonContent = Color.White,
                buttonSecondaryContainer = Color(0xFF374151),
                buttonSecondaryContent = Color(0xFFE0E7FF),
                hudDockBg = Color(0xEE16171E),
                hudActiveBtn = Color(0xFF6366F1),
                hudActiveIcon = Color.White,
                hudInactiveBtn = Color(0x336366F1),
                hudInactiveIcon = Color(0xFF9CA3AF),
                hudLabelColor = Color(0xFF9CA3AF),
                badgeContainer = Color(0xFF1F2937),
                badgeContent = Color(0xFF818CF8),
                activeSentenceBg = Color(0x33818CF8),
                activeWordBg = Color(0xFF818CF8),
                activeWordText = Color.White,
                isLight = false
            )
        }
        return when (themeId) {
            "MONOCHROME_BW" -> ReadingThemePalette(
                themeId = "MONOCHROME_BW",
                displayName = "Preto e Branco",
                background = Color(0xFF000000),
                surface = Color(0xFF121212),
                surfaceVariant = Color(0xFF1E1E1E),
                surfaceElevated = Color(0xFF282828),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFB0B0B0),
                textTertiary = Color(0xFF707070),
                accent = Color(0xFFFFFFFF),
                accentSecondary = Color(0xFFD0D0D0),
                border = Color(0xFF333333),
                cardBorder = Color(0xFF2E2E2E),
                divider = Color(0xFF222222),
                buttonContainer = Color(0xFFFFFFFF),
                buttonContent = Color(0xFF000000),
                buttonSecondaryContainer = Color(0xFF222222),
                buttonSecondaryContent = Color(0xFFFFFFFF),
                hudDockBg = Color(0xEE121212),
                hudActiveBtn = Color(0xFFFFFFFF),
                hudActiveIcon = Color(0xFF000000),
                hudInactiveBtn = Color(0x33FFFFFF),
                hudInactiveIcon = Color(0xFFB0B0B0),
                hudLabelColor = Color(0xFFB0B0B0),
                badgeContainer = Color(0xFF262626),
                badgeContent = Color(0xFFFFFFFF),
                activeSentenceBg = Color(0x33FFFFFF),
                activeWordBg = Color(0xFFFFFFFF),
                activeWordText = Color(0xFF000000),
                isLight = false
            )
            "PAPER_SEPIA" -> ReadingThemePalette(
                themeId = "PAPER_SEPIA",
                displayName = "Papel Sépia",
                background = Color(0xFFFBF0D9),
                surface = Color(0xFFF4E4C1),
                surfaceVariant = Color(0xFFEADBBE),
                surfaceElevated = Color(0xFFDFCFAF),
                textPrimary = Color(0xFF3A2E1A),
                textSecondary = Color(0xFF6B5838),
                textTertiary = Color(0xFF9E8A68),
                accent = Color(0xFF8C4A1A),
                accentSecondary = Color(0xFFB86A2E),
                border = Color(0xFFD6C29E),
                cardBorder = Color(0xFFD0BA94),
                divider = Color(0xFFDECDB0),
                buttonContainer = Color(0xFF8C4A1A),
                buttonContent = Color(0xFFFBF0D9),
                buttonSecondaryContainer = Color(0xFFEADBBE),
                buttonSecondaryContent = Color(0xFF3A2E1A),
                hudDockBg = Color(0xEEF4E4C1),
                hudActiveBtn = Color(0xFF8C4A1A),
                hudActiveIcon = Color(0xFFFBF0D9),
                hudInactiveBtn = Color(0x338C4A1A),
                hudInactiveIcon = Color(0xFF6B5838),
                hudLabelColor = Color(0xFF6B5838),
                badgeContainer = Color(0xFFEADBBE),
                badgeContent = Color(0xFF3A2E1A),
                activeSentenceBg = Color(0x448C4A1A),
                activeWordBg = Color(0xFF8C4A1A),
                activeWordText = Color(0xFFFBF0D9),
                isLight = true
            )
            "CYBERPUNK_NEON" -> ReadingThemePalette(
                themeId = "CYBERPUNK_NEON",
                displayName = "Cyberpunk",
                background = Color(0xFF070714),
                surface = Color(0xFF0E0E25),
                surfaceVariant = Color(0xFF171738),
                surfaceElevated = Color(0xFF202048),
                textPrimary = Color(0xFF00FFFF),
                textSecondary = Color(0xFFFF007F),
                textTertiary = Color(0xFF7A7AA8),
                accent = Color(0xFF00FFFF),
                accentSecondary = Color(0xFFFF007F),
                border = Color(0xFF2A2A60),
                cardBorder = Color(0xFF3A3A80),
                divider = Color(0xFF1E1E45),
                buttonContainer = Color(0xFF00FFFF),
                buttonContent = Color(0xFF070714),
                buttonSecondaryContainer = Color(0xFF1E1E45),
                buttonSecondaryContent = Color(0xFF00FFFF),
                hudDockBg = Color(0xEE0E0E25),
                hudActiveBtn = Color(0xFF00FFFF),
                hudActiveIcon = Color(0xFF070714),
                hudInactiveBtn = Color(0x3300FFFF),
                hudInactiveIcon = Color(0xFF7A7AA8),
                hudLabelColor = Color(0xFFFF007F),
                badgeContainer = Color(0xFF171738),
                badgeContent = Color(0xFF00FFFF),
                activeSentenceBg = Color(0x3300FFFF),
                activeWordBg = Color(0xFF00FFFF),
                activeWordText = Color(0xFF070714),
                isLight = false
            )
            "CYBER_NEON_NIGHT" -> ReadingThemePalette(
                themeId = "CYBER_NEON_NIGHT",
                displayName = "Ciber Noturno 10 Cores",
                background = Color(0xFF030D22), // Fundo #030D22
                surface = Color(0xFF081430),
                surfaceVariant = Color(0xFF1C1347), // Linha selecionada #1C1347
                surfaceElevated = Color(0xFF0B1F48),
                textPrimary = Color(0xFFFDFEFF), // Branco frio #FDFEFF
                textSecondary = Color(0xFF0EE5C6), // Verde-água #0EE5C6
                textTertiary = Color(0xFF0CB886), // Verde esmeralda #0CB886
                accent = Color(0xFF0EF3FF), // Ciano neon #0EF3FF
                accentSecondary = Color(0xFFFF2CF1), // Magenta neon #FF2CF1
                border = Color(0xFF0098DF), // Azul-ciano #0098DF
                cardBorder = Color(0xFF0EF3FF), // Ciano neon
                divider = Color(0xFF1C1347), // Linha selecionada
                buttonContainer = Color(0xFFFF2CF1), // Magenta neon #FF2CF1
                buttonContent = Color(0xFF030D22), // Fundo
                buttonSecondaryContainer = Color(0xFF0CB886), // Verde esmeralda #0CB886
                buttonSecondaryContent = Color(0xFFFDFEFF), // Branco frio
                hudDockBg = Color(0xF2030D22), // Fundo
                hudActiveBtn = Color(0xFFFF2E97), // Rosa neon #FF2E97
                hudActiveIcon = Color(0xFF0EE5C6), // Verde-água #0EE5C6
                hudInactiveBtn = Color(0x331C1347), // Linha selecionada
                hudInactiveIcon = Color(0xFF0098DF), // Azul-ciano #0098DF
                hudLabelColor = Color(0xFFFFCC00), // Amarelo de referência #FFCC00
                badgeContainer = Color(0xFFFF2E97), // Rosa neon #FF2E97
                badgeContent = Color(0xFFFDFEFF), // Branco frio
                activeSentenceBg = Color(0xFF1C1347), // Linha selecionada #1C1347
                activeWordBg = Color(0xFF0EF3FF), // Ciano neon #0EF3FF
                activeWordText = Color(0xFF030D22), // Fundo
                isLight = false
            )
            "CINEMA_YELLOW" -> ReadingThemePalette(
                themeId = "CINEMA_YELLOW",
                displayName = "Cinema Amarelo",
                background = Color(0xFF080808),
                surface = Color(0xFF141414),
                surfaceVariant = Color(0xFF202020),
                surfaceElevated = Color(0xFF2C2C2C),
                textPrimary = Color(0xFFFFE600),
                textSecondary = Color(0xFFCCCCCC),
                textTertiary = Color(0xFF888888),
                accent = Color(0xFFFFE600),
                accentSecondary = Color(0xFFFFB703),
                border = Color(0xFF333333),
                cardBorder = Color(0xFF2B2B2B),
                divider = Color(0xFF1E1E1E),
                buttonContainer = Color(0xFFFFE600),
                buttonContent = Color(0xFF080808),
                buttonSecondaryContainer = Color(0xFF222222),
                buttonSecondaryContent = Color(0xFFFFE600),
                hudDockBg = Color(0xEE141414),
                hudActiveBtn = Color(0xFFFFE600),
                hudActiveIcon = Color(0xFF080808),
                hudInactiveBtn = Color(0x33FFE600),
                hudInactiveIcon = Color(0xFFCCCCCC),
                hudLabelColor = Color(0xFFCCCCCC),
                badgeContainer = Color(0xFF222222),
                badgeContent = Color(0xFFFFE600),
                activeSentenceBg = Color(0x33FFE600),
                activeWordBg = Color(0xFFFFE600),
                activeWordText = Color(0xFF080808),
                isLight = false
            )
            "TERMINAL_GREEN" -> ReadingThemePalette(
                themeId = "TERMINAL_GREEN",
                displayName = "Terminal Retro",
                background = Color(0xFF030D04),
                surface = Color(0xFF071B08),
                surfaceVariant = Color(0xFF0E2C10),
                surfaceElevated = Color(0xFF143B17),
                textPrimary = Color(0xFF22C55E),
                textSecondary = Color(0xFF16A34A),
                textTertiary = Color(0xFF15803D),
                accent = Color(0xFF4ADE80),
                accentSecondary = Color(0xFF22C55E),
                border = Color(0xFF14532D),
                cardBorder = Color(0xFF166534),
                divider = Color(0xFF0E381A),
                buttonContainer = Color(0xFF22C55E),
                buttonContent = Color(0xFF030D04),
                buttonSecondaryContainer = Color(0xFF0E2C10),
                buttonSecondaryContent = Color(0xFF4ADE80),
                hudDockBg = Color(0xEE071B08),
                hudActiveBtn = Color(0xFF4ADE80),
                hudActiveIcon = Color(0xFF030D04),
                hudInactiveBtn = Color(0x334ADE80),
                hudInactiveIcon = Color(0xFF16A34A),
                hudLabelColor = Color(0xFF16A34A),
                badgeContainer = Color(0xFF0E2C10),
                badgeContent = Color(0xFF4ADE80),
                activeSentenceBg = Color(0x3322C55E),
                activeWordBg = Color(0xFF4ADE80),
                activeWordText = Color(0xFF030D04),
                isLight = false
            )
            else -> ReadingThemePalette( // DARK_OLED default
                themeId = "DARK_OLED",
                displayName = "Dark OLED",
                background = Color(0xFF000000),
                surface = Color(0xFF0F0F1A),
                surfaceVariant = Color(0xFF19192C),
                surfaceElevated = Color(0xFF24243E),
                textPrimary = Color(0xFFF1F5F9),
                textSecondary = Color(0xFF94A3B8),
                textTertiary = Color(0xFF64748B),
                accent = Color(0xFF818CF8),
                accentSecondary = Color(0xFFA78BFA),
                border = Color(0xFF2D2D4E),
                cardBorder = Color(0xFF383860),
                divider = Color(0xFF1F1F35),
                buttonContainer = Color(0xFF6366F1),
                buttonContent = Color(0xFFFFFFFF),
                buttonSecondaryContainer = Color(0xFF1E1E34),
                buttonSecondaryContent = Color(0xFFC7D2FE),
                hudDockBg = Color(0xEE0F0F1A),
                hudActiveBtn = Color(0xFF6366F1),
                hudActiveIcon = Color(0xFFFFFFFF),
                hudInactiveBtn = Color(0x336366F1),
                hudInactiveIcon = Color(0xFF94A3B8),
                hudLabelColor = Color(0xFF94A3B8),
                badgeContainer = Color(0xFF1E1E34),
                badgeContent = Color(0xFF818CF8),
                activeSentenceBg = Color(0x2E6366F1),
                activeWordBg = Color(0xFF6366F1),
                activeWordText = Color(0xFFFFFFFF),
                isLight = false
            )
        }
    }
}
