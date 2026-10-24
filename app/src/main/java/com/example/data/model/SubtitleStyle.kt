package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

enum class SubtitleStylePreset(
    val title: String,
    val textColor: Color,
    val activeBackgroundColor: Color,
    val cardBackgroundColor: Color,
    val borderColor: Color,
    val inactiveTextColor: Color
) {
    CLASSIC_WHITE(
        title = "Clássico Branco",
        textColor = Color.White,
        activeBackgroundColor = Color(0xFF2A2A2A),
        cardBackgroundColor = Color(0xFF121212),
        borderColor = Color(0xFF333333),
        inactiveTextColor = Color(0xFF888888)
    ),
    CINEMA_GOLD(
        title = "Cinema Dourado",
        textColor = Color(0xFFFFD700),
        activeBackgroundColor = Color(0xFF261D00),
        cardBackgroundColor = Color(0xFF0F0E0B),
        borderColor = Color(0xFFFFC107),
        inactiveTextColor = Color(0xFF9E8B4D)
    ),
    NEON_CYAN(
        title = "Neon Cyan",
        textColor = Color(0xFF00E5FF),
        activeBackgroundColor = Color(0xFF00363D),
        cardBackgroundColor = Color(0xFF081318),
        borderColor = Color(0xFF00B0FF),
        inactiveTextColor = Color(0xFF538A96)
    ),
    HIGH_CONTRAST(
        title = "Alto Contraste",
        textColor = Color.Yellow,
        activeBackgroundColor = Color.Black,
        cardBackgroundColor = Color(0xFF000000),
        borderColor = Color.White,
        inactiveTextColor = Color(0xFFAAAAAA)
    ),
    AMBER_WARM(
        title = "Âmbar Suave",
        textColor = Color(0xFFFFB74D),
        activeBackgroundColor = Color(0xFF33200A),
        cardBackgroundColor = Color(0xFF140F0A),
        borderColor = Color(0xFFFF9800),
        inactiveTextColor = Color(0xFFA67C52)
    )
}

enum class FullscreenDisplayMode(val label: String) {
    CONTINUOUS_3PARTS("Fluxo 3 Partes"),
    BIG_CINEMA("Cinema Grande"),
    PAPER_SHEET("Folha de Papel"),
    TRANSCRIPT_SCROLL("Transcrição Interativa"),
    BOOK_MODE("Modo Livro")
}

enum class SubtitleAlignment(val label: String, val align: TextAlign) {
    CENTER("Centro", TextAlign.Center),
    LEFT("Esquerda", TextAlign.Left),
    RIGHT("Direita", TextAlign.Right),
    JUSTIFY("Justificado", TextAlign.Justify)
}
