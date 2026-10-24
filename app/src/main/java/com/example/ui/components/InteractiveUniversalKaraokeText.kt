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
import kotlin.math.abs

private data class UniversalKaraokeWord(
    val wordIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val word: String,
    val isRevoked: Boolean
)

private data class UniversalTermMatch(
    val startChar: Int,
    val endChar: Int,
    val term: PageDictionaryTerm
)

private data class RevokedSpan(
    val startChar: Int,
    val endChar: Int
)

/**
 * Componente universal de leitura interativa com destaque sincronizado palavra por palavra (Karaokê):
 * - Destaque em tempo real da palavra ativa falada pela voz (TTS).
 * - Sincronização bidirecional: clicar em qualquer palavra (inclusive anterior) retorna a voz e o destaque
 *   imediatamente para aquele ponto.
 * - Suporte nativo a trechos revogados (apenas as palavras originalmente riscadas recebem strikethrough).
 * - Destaque inteligente de termos do dicionário com abertura instantânea ao tocar.
 */
@Composable
fun InteractiveUniversalKaraokeText(
    text: String,
    isSpeaking: Boolean,
    activeWordIndex: Int,
    fontSizeSp: Float = 14f,
    textColor: Color = Color(0xFFF4F4F5),
    activeWordColor: Color = Color(0xFFFCD34D),
    activeWordBgColor: Color = Color(0x66F59E0B),
    pastWordColor: Color = Color(0xFFCBD5E1),
    fontFamily: FontFamily = FontFamily.Default,
    textAlign: TextAlign = TextAlign.Justify,
    dictionaryTerms: List<PageDictionaryTerm> = emptyList(),
    modifier: Modifier = Modifier,
    onDictionaryTermClick: ((PageDictionaryTerm) -> Unit)? = null,
    onWordClick: (wordIndex: Int) -> Unit
) {
    // 1. Processa texto removendo as marcações ~~ mas gravando as faixas revogadas
    val (cleanText, revokedSpans) = remember(text) {
        val spans = mutableListOf<RevokedSpan>()
        val sb = StringBuilder()
        var currentIdx = 0
        val pattern = java.util.regex.Pattern.compile("~~(.*?)~~", java.util.regex.Pattern.DOTALL)
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            if (start > currentIdx) {
                sb.append(text.substring(currentIdx, start))
            }
            val revokedContent = matcher.group(1) ?: ""
            val spanStart = sb.length
            sb.append(revokedContent)
            val spanEnd = sb.length
            spans.add(RevokedSpan(spanStart, spanEnd))
            currentIdx = end
        }
        if (currentIdx < text.length) {
            sb.append(text.substring(currentIdx))
        }
        Pair(sb.toString(), spans)
    }

    // 2. Mapeamento de palavras
    val words = remember(cleanText, revokedSpans) {
        val list = mutableListOf<UniversalKaraokeWord>()
        val regex = Regex("[^ \\t\\n\\r\\u00A0]+")
        val matches = regex.findAll(cleanText)
        matches.forEachIndexed { index, m ->
            val wStart = m.range.first
            val wEnd = m.range.last + 1
            val isRev = revokedSpans.any { span -> wStart < span.endChar && wEnd > span.startChar }
            list.add(
                UniversalKaraokeWord(
                    wordIndex = index,
                    startChar = wStart,
                    endChar = wEnd,
                    word = m.value,
                    isRevoked = isRev
                )
            )
        }
        list
    }

    // 3. Mapeamento de termos do dicionário
    val termMatches = remember(cleanText, dictionaryTerms) {
        val matches = mutableListOf<UniversalTermMatch>()
        if (dictionaryTerms.isNotEmpty() && cleanText.isNotBlank()) {
            val lowerText = cleanText.lowercase()
            dictionaryTerms.forEach { dictTerm ->
                val termStr = dictTerm.term.trim()
                if (termStr.length >= 2) {
                    val pattern = Regex("\\b${Regex.escape(termStr.lowercase())}\\b")
                    pattern.findAll(lowerText).forEach { m ->
                        matches.add(
                            UniversalTermMatch(
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

    if (words.isEmpty()) {
        Text(
            text = text,
            fontSize = fontSizeSp.sp,
            color = textColor,
            modifier = modifier
        )
        return
    }

    // 4. Monta AnnotatedString sincronizada com Karaokê
    val annotatedString = remember(cleanText, words, isSpeaking, activeWordIndex, textColor, activeWordColor, activeWordBgColor, pastWordColor, termMatches) {
        buildAnnotatedString {
            var cursor = 0
            words.forEach { w ->
                if (w.startChar > cursor) {
                    val space = cleanText.substring(cursor, w.startChar)
                    withStyle(SpanStyle(color = textColor, fontSize = fontSizeSp.sp)) {
                        append(space)
                    }
                }

                val isCurrent = isSpeaking && w.wordIndex == activeWordIndex
                val isPast = isSpeaking && w.wordIndex < activeWordIndex
                val isDictMatch = termMatches.any { tm -> w.startChar < tm.endChar && w.endChar > tm.startChar }

                val style = when {
                    isCurrent -> SpanStyle(
                        color = activeWordColor,
                        background = activeWordBgColor,
                        fontWeight = FontWeight.ExtraBold,
                        textDecoration = if (w.isRevoked) TextDecoration.LineThrough else TextDecoration.None,
                        fontSize = fontSizeSp.sp
                    )
                    isPast -> SpanStyle(
                        color = if (w.isRevoked) Color(0xFF71717A) else pastWordColor,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (w.isRevoked) TextDecoration.LineThrough else TextDecoration.None,
                        fontSize = fontSizeSp.sp
                    )
                    isDictMatch -> SpanStyle(
                        color = Color(0xFF38BDF8),
                        background = Color(0x3338BDF8),
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (w.isRevoked) TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)) else TextDecoration.Underline,
                        fontSize = fontSizeSp.sp
                    )
                    w.isRevoked -> SpanStyle(
                        color = Color(0xFF71717A),
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = fontSizeSp.sp
                    )
                    else -> SpanStyle(
                        color = textColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = fontSizeSp.sp
                    )
                }

                withStyle(style) {
                    append(w.word)
                }

                cursor = w.endChar
            }

            if (cursor < cleanText.length) {
                val trailing = cleanText.substring(cursor)
                withStyle(SpanStyle(color = textColor, fontSize = fontSizeSp.sp)) {
                    append(trailing)
                }
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        textAlign = textAlign,
        fontFamily = fontFamily,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.58f).sp,
        letterSpacing = 0.2.sp,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(cleanText, words, termMatches, isSpeaking) {
                detectTapGestures { offset ->
                    layoutResult?.let { layout ->
                        val charOffset = layout.getOffsetForPosition(offset)

                        // 1. Verifica se clicou em um termo do dicionário
                        if (onDictionaryTermClick != null) {
                            val matched = termMatches.firstOrNull { charOffset >= it.startChar && charOffset <= it.endChar }
                            if (matched != null) {
                                onDictionaryTermClick(matched.term)
                                return@detectTapGestures
                            }
                        }

                        // 2. Localiza palavra tocada e salta a voz para ela imediatamente
                        val clickedWord = words.firstOrNull { charOffset in it.startChar until it.endChar }
                            ?: words.minByOrNull { abs(charOffset - it.startChar) }

                        if (clickedWord != null) {
                            onWordClick(clickedWord.wordIndex)
                        }
                    }
                }
            }
    )
}
