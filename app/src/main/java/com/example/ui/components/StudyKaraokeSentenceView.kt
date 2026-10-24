package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LanguageFlagTheme

data class KaraokeWordToken(
    val wordIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val word: String
)

/**
 * Componente profissional de leitura por voz com Karaokê em tempo real palavra por palavra:
 * - Destaque sincronizado de cada palavra no exato momento da fala.
 * - Sincronização interativa: clicar em qualquer palavra salta a voz para aquele ponto.
 * - Mini-botão de controle: pausar e despausar (retomar) no mesmo botão compacto.
 * - Estilização refinada temática nas cores da bandeira do idioma.
 */
@Composable
fun StudyKaraokeSentenceView(
    text: String,
    lineId: String,
    isSpeaking: Boolean,
    activeLineId: String?,
    activeWordIndex: Int,
    isPaused: Boolean,
    flagTheme: LanguageFlagTheme,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontFamily: FontFamily = FontFamily.Default,
    lineHeight: TextUnit = 20.sp,
    baseTextColor: Color = flagTheme.textPrimary,
    containerColor: Color = Color.Transparent,
    showBorderWhenActive: Boolean = true,
    showPrefix: String? = null,
    onTogglePlayPause: () -> Unit,
    onWordClick: (wordOffset: Int) -> Unit
) {
    if (text.isBlank()) return

    val isActiveLine = (activeLineId == lineId)
    val isActuallyPlaying = isActiveLine && isSpeaking
    val isActuallyPaused = isActiveLine && isPaused

    // 1. Mapeamento de palavras e offsets de caracteres
    val (wordTokens, cleanText) = remember(text) {
        val trimmed = text.trim()
        val tokens = mutableListOf<KaraokeWordToken>()
        val regex = Regex("[^ \\t\\n\\r\\u00A0]+")
        regex.findAll(trimmed).forEachIndexed { idx, match ->
            tokens.add(
                KaraokeWordToken(
                    wordIndex = idx,
                    startChar = match.range.first,
                    endChar = match.range.last + 1,
                    word = match.value
                )
            )
        }
        Pair(tokens, trimmed)
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Pulsação sutil quando a linha está ativamente falando
    val infiniteTransition = rememberInfiniteTransition(label = "karaokePulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeCardBorder = when {
        isActuallyPlaying -> flagTheme.secondaryAccent.copy(alpha = pulseGlow)
        isActuallyPaused -> Color(0xFFF59E0B).copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val activeCardBg = when {
        isActuallyPlaying -> flagTheme.surfaceCard.copy(alpha = 0.9f)
        isActuallyPaused -> flagTheme.surfaceCard.copy(alpha = 0.8f)
        else -> containerColor
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = activeCardBg,
        border = if (showBorderWhenActive && (isActuallyPlaying || isActuallyPaused)) {
            androidx.compose.foundation.BorderStroke(1.2.dp, activeCardBorder)
        } else {
            null
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // MINI BOTÃO DE CONTROLE (PAUSAR / DESPAUSAR / TOCAR)
            // "um simbolozinho para eu poder pausar e despausar, o mesmo botão pequenininho"
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, end = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActuallyPlaying -> flagTheme.secondaryAccent
                            isActuallyPaused -> Color(0xFFF59E0B)
                            else -> flagTheme.primaryAccent.copy(alpha = 0.2f)
                        }
                    )
                    .clickable { onTogglePlayPause() }
                    .testTag("karaoke_play_pause_$lineId"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isActuallyPlaying -> Icons.Default.Pause
                        isActuallyPaused -> Icons.Default.PlayArrow
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = when {
                        isActuallyPlaying -> "Pausar Leitura"
                        isActuallyPaused -> "Despausar Leitura"
                        else -> "Ouvir Leitura"
                    },
                    tint = when {
                        isActuallyPlaying -> flagTheme.containerBackground
                        isActuallyPaused -> Color.Black
                        else -> flagTheme.secondaryAccent
                    },
                    modifier = Modifier.size(16.dp)
                )
            }

            // TEXTO COM KARAOKÊ SINCRONIZADO E CLIQUES NAS PALAVRAS
            Column(modifier = Modifier.weight(1f)) {
                if (showPrefix != null) {
                    Text(
                        text = showPrefix,
                        fontSize = (fontSize.value * 0.8f).sp,
                        fontWeight = FontWeight.Bold,
                        color = flagTheme.secondaryAccent,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Construção da string anotada com destaque do Karaokê
                val annotatedString = buildAnnotatedString {
                    if (isActiveLine && (isActuallyPlaying || isActuallyPaused)) {
                        wordTokens.forEachIndexed { idx, token ->
                            val isCurrentWord = (token.wordIndex == activeWordIndex)
                            val isPastWord = (token.wordIndex < activeWordIndex)

                            when {
                                isCurrentWord -> {
                                    // PALAVRA ATUAL FALADA PELA VOZ: Destaque máximo vibrante
                                    withStyle(
                                        SpanStyle(
                                            color = flagTheme.containerBackground,
                                            background = flagTheme.secondaryAccent,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.3.sp
                                        )
                                    ) {
                                        append(" ${token.word} ")
                                    }
                                }
                                isPastWord -> {
                                    // Palavras já lidas com tom contrastante
                                    withStyle(
                                        SpanStyle(
                                            color = baseTextColor.copy(alpha = 0.95f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(token.word)
                                    }
                                    append(" ")
                                }
                                else -> {
                                    // Próximas palavras
                                    withStyle(
                                        SpanStyle(
                                            color = baseTextColor.copy(alpha = 0.7f),
                                            fontWeight = fontWeight
                                        )
                                    ) {
                                        append(token.word)
                                    }
                                    append(" ")
                                }
                            }
                        }
                    } else {
                        // Linha em estado normal
                        withStyle(
                            SpanStyle(
                                color = baseTextColor,
                                fontWeight = fontWeight
                            )
                        ) {
                            append(cleanText)
                        }
                    }
                }

                Text(
                    text = annotatedString,
                    fontSize = fontSize,
                    fontFamily = fontFamily,
                    lineHeight = lineHeight,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(cleanText) {
                            detectTapGestures { offset ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                val charIndex = layout.getOffsetForPosition(offset)
                                val clickedToken = wordTokens.find { token ->
                                    charIndex in token.startChar..token.endChar
                                } ?: wordTokens.minByOrNull { kotlin.math.abs(it.startChar - charIndex) }

                                clickedToken?.let { token ->
                                    onWordClick(token.wordIndex)
                                }
                            }
                        },
                    onTextLayout = { textLayoutResult = it }
                )

                // Indicação de pausa
                if (isActuallyPaused) {
                    Text(
                        text = "⏸️ Leitura pausada • Toque no botão para continuar ou em qualquer palavra para saltar",
                        fontSize = 10.sp,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
