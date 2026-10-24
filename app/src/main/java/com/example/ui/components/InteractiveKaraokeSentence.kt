package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.data.model.PageDictionaryTerm

data class WordRange(
    val wordIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val word: String
)

data class TermMatch(
    val startChar: Int,
    val endChar: Int,
    val term: PageDictionaryTerm
)

/**
 * Interactive Karaoke Sentence Component:
 * - Native Android full text justification (TextAlign.Justify)
 * - Word-by-word real-time highlight & subtle background pill during TTS
 * - Universal Dictionary Term highlighting: words/expressions in dictionary are highlighted with a distinct standout color
 * - Click on ANY dictionary term to instantly open the dictionary directly for that term!
 * - Click on normal words to trigger speech from that point
 */
@Composable
fun InteractiveKaraokeSentence(
    sentence: String,
    sentenceIndex: Int,
    isSentenceActive: Boolean,
    activeWordIndex: Int,
    textFontSizeSp: Float = 16f,
    fontFamily: FontFamily = FontFamily.Default,
    textColor: Color = Color.White,
    readingTheme: String = "DARK_OLED",
    dictionaryTerms: List<PageDictionaryTerm> = emptyList(),
    modifier: Modifier = Modifier,
    onDictionaryTermClick: (PageDictionaryTerm) -> Unit = {},
    onWordClick: (wordIndex: Int) -> Unit
) {
    val wordRanges = remember(sentence) {
        val ranges = mutableListOf<WordRange>()
        val regex = Regex("[^ \\t\\n\\r\\u00A0]+")
        val matches = regex.findAll(sentence)
        matches.forEachIndexed { index, matchResult ->
            ranges.add(
                WordRange(
                    wordIndex = index,
                    startChar = matchResult.range.first,
                    endChar = matchResult.range.last + 1,
                    word = matchResult.value
                )
            )
        }
        ranges
    }

    // Find all occurrences of dictionary terms within this sentence
    val termMatches = remember(sentence, dictionaryTerms) {
        val matches = mutableListOf<TermMatch>()
        if (dictionaryTerms.isNotEmpty()) {
            val cleanSentence = sentence.lowercase()
            dictionaryTerms.forEach { dictTerm ->
                val termStr = dictTerm.term.trim()
                if (termStr.isNotBlank() && termStr.length >= 2) {
                    val pattern = Regex("\\b${Regex.escape(termStr.lowercase())}\\b")
                    pattern.findAll(cleanSentence).forEach { m ->
                        matches.add(
                            TermMatch(
                                startChar = m.range.first,
                                endChar = m.range.last + 1,
                                term = dictTerm
                            )
                        )
                    }
                }
            }
        }
        matches.sortedBy { it.startChar }
    }

    if (wordRanges.isEmpty()) return

    // Standout dictionary highlighting color (distinct from any reading theme)
    val dictHighlightColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFF0066CC) // Deep Sapphire Blue against sepia
        "CINEMA_YELLOW" -> Color(0xFF00E5FF) // Vibrant Cyan against cinema yellow
        "CYBERPUNK_NEON" -> Color(0xFFFFD700) // Golden Amber against neon
        "TERMINAL_GREEN" -> Color(0xFFFF7700) // Bright Orange against terminal green
        "STREAMING_BOX" -> Color(0xFF38BDF8) // Sky Blue against streaming
        else -> Color(0xFF38BDF8) // Electric Sky Blue against dark OLED
    }

    val dictBgColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0x220066CC)
        "CINEMA_YELLOW" -> Color(0x3300E5FF)
        "CYBERPUNK_NEON" -> Color(0x33FFD700)
        "TERMINAL_GREEN" -> Color(0x33FF7700)
        "STREAMING_BOX" -> Color(0x3338BDF8)
        else -> Color(0x3338BDF8)
    }

    val currentActiveBg = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0x338E2DE2)
        "CINEMA_YELLOW" -> Color(0x66B89400)
        "CYBERPUNK_NEON" -> Color(0x66FF007F)
        "TERMINAL_GREEN" -> Color(0x55005522)
        "STREAMING_BOX" -> Color(0x664F46E5)
        else -> Color(0x4D8E2DE2)
    }

    val currentWordColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFF4A148C)
        "CINEMA_YELLOW" -> Color(0xFFFFF9A6)
        "CYBERPUNK_NEON" -> Color(0xFFFFFFFF)
        "TERMINAL_GREEN" -> Color(0xFFB8FFD2)
        "STREAMING_BOX" -> Color(0xFFFFE082)
        else -> Color.White
    }

    val pastWordColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFF5A3D28)
        "CINEMA_YELLOW" -> Color(0xFFB8A500)
        "CYBERPUNK_NEON" -> Color(0xFF00B2C2)
        "TERMINAL_GREEN" -> Color(0xFF00A843)
        "STREAMING_BOX" -> Color(0xFF9E9EAF)
        else -> Color(0xFFE9D5FF)
    }

    val annotatedString = remember(sentence, isSentenceActive, activeWordIndex, readingTheme, textColor, termMatches) {
        buildAnnotatedString {
            var currentIndex = 0
            wordRanges.forEach { range ->
                // Whitespace / punctuation before this word
                if (range.startChar > currentIndex) {
                    val space = sentence.substring(currentIndex, range.startChar)
                    withStyle(SpanStyle(color = textColor, fontSize = textFontSizeSp.sp)) {
                        append(space)
                    }
                }

                val isCurrent = isSentenceActive && range.wordIndex == activeWordIndex
                val isPast = isSentenceActive && range.wordIndex < activeWordIndex
                val hasDictMatch = termMatches.any { tm ->
                    range.startChar < tm.endChar && range.endChar > tm.startChar
                }

                val style = when {
                    isCurrent -> SpanStyle(
                        color = currentWordColor,
                        background = currentActiveBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = textFontSizeSp.sp
                    )
                    isPast -> SpanStyle(
                        color = pastWordColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = textFontSizeSp.sp
                    )
                    hasDictMatch -> SpanStyle(
                        color = dictHighlightColor,
                        background = dictBgColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        fontSize = textFontSizeSp.sp
                    )
                    else -> SpanStyle(
                        color = if (isSentenceActive) textColor.copy(alpha = 0.85f) else textColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = textFontSizeSp.sp
                    )
                }

                withStyle(style) {
                    append(range.word)
                }

                currentIndex = range.endChar
            }

            // Append any trailing characters
            if (currentIndex < sentence.length) {
                val trailing = sentence.substring(currentIndex)
                withStyle(SpanStyle(color = textColor, fontSize = textFontSizeSp.sp)) {
                    append(trailing)
                }
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        textAlign = TextAlign.Justify,
        fontFamily = fontFamily,
        fontSize = textFontSizeSp.sp,
        lineHeight = (textFontSizeSp * 1.65f).sp,
        letterSpacing = 0.35.sp,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(sentence, termMatches) {
                detectTapGestures { offset ->
                    layoutResult?.let { layout ->
                        val charOffset = layout.getOffsetForPosition(offset)
                        // Check if tapped on a dictionary term first!
                        val matchedTerm = termMatches.firstOrNull { charOffset >= it.startChar && charOffset <= it.endChar }
                        if (matchedTerm != null) {
                            onDictionaryTermClick(matchedTerm.term)
                            return@detectTapGestures
                        }

                        // Otherwise trigger word TTS
                        val clickedRange = wordRanges.firstOrNull { charOffset in it.startChar until it.endChar }
                            ?: wordRanges.minByOrNull { Math.abs(charOffset - it.startChar) }
                        if (clickedRange != null) {
                            onWordClick(clickedRange.wordIndex)
                        }
                    }
                }
            }
    )
}

