package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReadingLanguage

private data class LineWord(
    val wordIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val word: String
)

/**
 * Renders a single comparative sentence with real-time word-by-word Karaoke highlighting
 * and direct word-tap speech initiation.
 */
@Composable
fun ComparativeKaraokeLine(
    text: String,
    language: ReadingLanguage,
    lineId: String,
    isSpeaking: Boolean,
    activeComparativeLineId: String?,
    activeWordIndex: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
    onSpeakLine: (startWordOffset: Int) -> Unit = {}
) {
    if (text.isBlank()) return

    val isActiveLine = isSpeaking && activeComparativeLineId == lineId

    // 1. Map words and their character offsets in the line
    val (lineWords, cleanText) = remember(text) {
        val trimmed = text.trim()
        val words = mutableListOf<LineWord>()
        val regex = Regex("[^ \\t\\n\\r\\u00A0]+")
        regex.findAll(trimmed).forEachIndexed { idx, match ->
            words.add(
                LineWord(
                    wordIndex = idx,
                    startChar = match.range.first,
                    endChar = match.range.last + 1,
                    word = match.value
                )
            )
        }
        Pair(words, trimmed)
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // 2. Visual styling per language
    val (accentColor, containerBg, activeBorderColor) = when (language) {
        ReadingLanguage.ENGLISH -> Triple(
            Color(0xFF60A5FA),
            Color(0xFF1E293B).copy(alpha = 0.85f),
            Color(0xFF3B82F6)
        )
        ReadingLanguage.FRENCH -> Triple(
            Color(0xFFF472B6),
            Color(0xFF331D2C).copy(alpha = 0.85f),
            Color(0xFFEC4899)
        )
        ReadingLanguage.SPANISH -> Triple(
            Color(0xFF34D399),
            Color(0xFF132E27).copy(alpha = 0.85f),
            Color(0xFF10B981)
        )
        ReadingLanguage.PORTUGUESE -> Triple(
            Color(0xFFFBBF24),
            Color(0xFF2C2416).copy(alpha = 0.85f),
            Color(0xFFF59E0B)
        )
    }

    val currentWordBg = when (language) {
        ReadingLanguage.FRENCH -> Color(0xFFF43F5E)
        ReadingLanguage.ENGLISH -> Color(0xFF2563EB)
        ReadingLanguage.SPANISH -> Color(0xFF059669)
        ReadingLanguage.PORTUGUESE -> Color(0xFFD97706)
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isActiveLine) activeBorderColor else accentColor.copy(alpha = 0.25f),
        label = "lineBorder"
    )

    // 3. Build AnnotatedString with real-time Karaoke highlight
    val annotatedString: AnnotatedString = remember(
        cleanText,
        lineWords,
        isActiveLine,
        activeWordIndex,
        textColor,
        accentColor,
        currentWordBg
    ) {
        buildAnnotatedString {
            append(cleanText)

            if (isActiveLine && lineWords.isNotEmpty()) {
                lineWords.forEach { word ->
                    when {
                        word.wordIndex == activeWordIndex -> {
                            // Active speaking word: vibrant highlight pill
                            addStyle(
                                style = SpanStyle(
                                    color = Color.White,
                                    background = currentWordBg,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                start = word.startChar,
                                end = word.endChar
                            )
                        }
                        word.wordIndex < activeWordIndex -> {
                            // Already spoken word: slightly brighter accent
                            addStyle(
                                style = SpanStyle(
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                start = word.startChar,
                                end = word.endChar
                            )
                        }
                        else -> {
                            // Upcoming word
                            addStyle(
                                style = SpanStyle(
                                    color = textColor.copy(alpha = 0.90f)
                                ),
                                start = word.startChar,
                                end = word.endChar
                            )
                        }
                    }
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActiveLine) containerBg else containerBg.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            if (isActiveLine) 1.2.dp else 0.6.dp,
            animatedBorderColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Flag indicator badge
            Text(
                text = language.flag,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { onSpeakLine(0) }
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Text with interactive tap per word & Karaoke highlight
            Box(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(cleanText, lineWords) {
                        detectTapGestures { offset ->
                            val layout = layoutResult ?: return@detectTapGestures
                            val charOffset = layout.getOffsetForPosition(offset)
                            val clickedWord = lineWords.firstOrNull { charOffset in it.startChar..it.endChar }
                            if (clickedWord != null) {
                                onSpeakLine(clickedWord.wordIndex)
                            } else {
                                onSpeakLine(0)
                            }
                        }
                    }
            ) {
                Text(
                    text = annotatedString,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Serif,
                    color = textColor,
                    onTextLayout = { layoutResult = it }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Audio button (animated graphic equalizer if speaking this line)
            Surface(
                shape = CircleShape,
                color = if (isActiveLine) activeBorderColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSpeakLine(0) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isActiveLine) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                        contentDescription = "Ouvir em ${language.label}",
                        tint = if (isActiveLine) activeBorderColor else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
