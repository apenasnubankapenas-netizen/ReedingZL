package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageSegmentExplanation
import com.example.ui.theme.ReadingThemeManager
import kotlin.math.abs

private data class ExplanationWord(
    val wordIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val word: String
)

/**
 * Modern, sophisticated in-place explanation card.
 *
 * Features:
 * 1. Clean, minimalist typography and subtle theme integration (OLED Dark, Sepia, Terminal Green, Cyberpunk, Light).
 * 2. 4 languages: PT (Português), ES (Español), EN (English), FR (Français).
 * 3. Interactive word tapping: tap ANY word inside the explanation to immediately start/resume voice reading from that exact word.
 * 4. Real-time karaoke highlight while reading the explanation aloud.
 * 5. Minimal, elegant controls: simple "Explicação", "Ver explicação", "Ouvir", "Ocultar".
 */
@Composable
fun SegmentAiExplanationCard(
    explanation: PageSegmentExplanation?,
    isLoading: Boolean,
    errorMessage: String? = null,
    readingTheme: String,
    category: String? = null,
    categoryColor: String? = null,
    isSpeaking: Boolean = false,
    activeComparativeLineId: String? = null,
    activeWordIndex: Int = 0,
    onLanguageChangeAndSpeak: (langCode: String, textToSpeak: String) -> Unit,
    onSpeakExplanation: (langCode: String, text: String) -> Unit,
    onSpeakExplanationWord: (langCode: String, fullText: String, wordOffset: Int) -> Unit = { _, _, _ -> },
    onStopSpeaking: () -> Unit = {},
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onOpenChat: (() -> Unit)? = null,
    textFontSizeSp: Float = 14f,
    modifier: Modifier = Modifier
) {
    // Current active language (Default to PT)
    var currentLangCode by remember { mutableStateOf("PT") }

    val languages = listOf(
        "PT" to "PT",
        "ES" to "ES",
        "EN" to "EN",
        "FR" to "FR"
    )

    val currentExplanationText = remember(explanation, currentLangCode) {
        val raw = explanation?.getExplanationForLanguage(currentLangCode) ?: ""
        // Non-breaking spaces for typographic polish
        raw.replace(Regex("(\\b[a-zA-Z0-9]\\b) "), "$1\u00A0")
    }

    // Map explanation text words for interactive tapping & karaoke
    val (explanationWords, formattedWordsText) = remember(currentExplanationText) {
        val words = mutableListOf<ExplanationWord>()
        val wordRegex = Regex("[^ \\t\\n\\r\\u00A0]+")
        val matches = wordRegex.findAll(currentExplanationText)
        matches.forEachIndexed { wIdx, match ->
            words.add(
                ExplanationWord(
                    wordIndex = wIdx,
                    startChar = match.range.first,
                    endChar = match.range.last + 1,
                    word = match.value
                )
            )
        }
        words to currentExplanationText
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val explanationLineId = "expl_${explanation?.id ?: 0}_${currentLangCode.uppercase()}"
    val isSpeakingThisExplanation = isSpeaking && activeComparativeLineId == explanationLineId

    // Harmonious theme styling resolving exact palette from ReadingThemeManager
    val palette = remember(readingTheme, category, categoryColor) {
        ReadingThemeManager.getPalette(readingTheme, category, categoryColor)
    }

    val cardBg = palette.surfaceVariant
    val borderColor = palette.cardBorder.takeIf { it != Color.Transparent } ?: palette.accent.copy(alpha = 0.45f)
    val textColor = palette.textPrimary
    val accentColor = palette.accent
    val karaokeHighlightBg = palette.activeWordBg.copy(alpha = if (palette.isLight) 0.55f else 0.40f)

    // Build annotated string with real-time karaoke highlight if playing
    val annotatedExplanation = remember(currentExplanationText, isSpeakingThisExplanation, activeWordIndex, textColor, accentColor, karaokeHighlightBg) {
        buildAnnotatedString {
            append(currentExplanationText)
            if (isSpeakingThisExplanation && activeWordIndex in explanationWords.indices) {
                val activeWord = explanationWords[activeWordIndex]
                addStyle(
                    style = SpanStyle(
                        background = karaokeHighlightBg,
                        fontWeight = FontWeight.Bold,
                        color = palette.activeWordText
                    ),
                    start = activeWord.startChar,
                    end = activeWord.endChar
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.45f)),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Sleek Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = "Explicação",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.3.sp
                    )
                }

                // Center: Language Selector Pills (PT, ES, EN, FR)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    languages.forEach { (code, label) ->
                        val isSelected = currentLangCode.equals(code, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) accentColor else accentColor.copy(alpha = 0.10f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accentColor else accentColor.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .height(26.dp)
                                .clickable {
                                    currentLangCode = code
                                    val text = explanation?.getExplanationForLanguage(code) ?: ""
                                    onLanguageChangeAndSpeak(code, text)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) palette.buttonContent else textColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Actions: Chat / Listen / Refresh / Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Chat Button
                    if (onOpenChat != null) {
                        IconButton(
                            onClick = onOpenChat,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Abrir Chat da etapa",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Audio Listen/Pause Toggle
                    IconButton(
                        onClick = {
                            if (isSpeakingThisExplanation) {
                                onStopSpeaking()
                            } else {
                                onSpeakExplanationWord(currentLangCode, currentExplanationText, 0)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeakingThisExplanation) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeakingThisExplanation) "Pausar" else "Ouvir",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refazer",
                            tint = textColor.copy(alpha = 0.70f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Delete button
                    if (explanation != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Apagar",
                                tint = Color(0xFFEF4444).copy(alpha = 0.75f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = textColor.copy(alpha = 0.70f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Content
            when {
                isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Carregando explicação...",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = Color(0xFFF87171),
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { onRegenerate() }
                        ) {
                            Text(
                                text = "Tentar novamente",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                currentExplanationText.isNotBlank() -> {
                    val explanationScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(explanationScrollState)
                    ) {
                        // Interactive text: Tapping ANY word starts voice reading from that exact word!
                        Text(
                            text = annotatedExplanation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Justify,
                                lineHeight = (textFontSizeSp * 1.48f).coerceIn(16f, 48f).sp,
                                fontSize = (textFontSizeSp * 0.95f).coerceIn(11f, 32f).sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = textColor,
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(explanationWords, currentExplanationText, currentLangCode) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val layout = textLayoutResult ?: return@detectTapGestures
                                            val charOffset = layout.getOffsetForPosition(offset)

                                            // Find clicked word in explanation
                                            val clicked = explanationWords.firstOrNull { charOffset in it.startChar until it.endChar }
                                                ?: explanationWords.minByOrNull { abs(charOffset - it.startChar) }

                                            if (clicked != null) {
                                                // Start speech from this exact clicked word in the explanation!
                                                onSpeakExplanationWord(currentLangCode, currentExplanationText, clicked.wordIndex)
                                            } else {
                                                // If tapped outside words, toggle/play whole explanation
                                                onSpeakExplanationWord(currentLangCode, currentExplanationText, 0)
                                            }
                                        }
                                    )
                                }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Minimal, elegant Hide / Minimize pill button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable { onDismiss() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Ocultar",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ocultar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Toque em 'Refazer' para gerar a explicação detalhada.",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = textColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }
        }
    }
}
