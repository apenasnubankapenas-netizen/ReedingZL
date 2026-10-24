package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Add
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PageSegmentExplanation
import com.example.data.model.SegmentChatMessage
import com.example.data.model.StudyLanguage
import com.example.data.pdf.BookParagraph
import com.example.data.speech.PdfSpeechManager
import com.example.data.speech.VoiceEnginePreferences
import com.example.data.openai.OpenAiSpeechCache
import com.example.data.openai.OpenAiService
import com.example.data.translator.TermsNotesTranslator
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.io.File
import java.util.Locale
import android.widget.Toast

private data class ParagraphWord(
    val globalSentenceIndex: Int,
    val sentenceIndexInParagraph: Int,
    val wordIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val word: String
)

private data class ParagraphTermMatch(
    val startChar: Int,
    val endChar: Int,
    val term: PageDictionaryTerm
)

/**
 * Renders an entire book paragraph as a single continuous, fully justified text block.
 * Eliminates abrupt sentence breaks and line jumping.
 * Applies non-breaking spaces to guarantee no isolated letters or numbers.
 * Provides real-time word-level karaoke synchronization and dictionary term interactions.
 *
 * Tapping any word resumes voice speech directly from that word.
 * Clean, modern "Ver explicação" / "Ocultar" button expands the local in-place explanation.
 */
@Composable
fun InteractiveKaraokeParagraph(
    paragraph: BookParagraph,
    activeSentenceIndex: Int,
    activeWordIndex: Int,
    isSpeaking: Boolean,
    activeComparativeLineId: String? = null,
    textFontSizeSp: Float = 16f,
    fontFamily: FontFamily = FontFamily.Serif,
    textColor: Color = Color.White,
    readingTheme: String = "DARK_OLED",
    category: String? = null,
    categoryColor: String? = null,
    dictionaryTerms: List<PageDictionaryTerm> = emptyList(),
    pageSegmentExplanations: List<PageSegmentExplanation> = emptyList(),
    activeExpandedSegmentIndex: Int? = null,
    isGeneratingExplanation: Boolean = false,
    explanationError: String? = null,
    segmentChatMessages: List<SegmentChatMessage> = emptyList(),
    isSegmentChatLoading: Boolean = false,
    segmentChatError: String? = null,
    isExtractingBlockTerms: Boolean = false,
    modifier: Modifier = Modifier,
    onWordClick: (globalSentenceIndex: Int, wordIndex: Int) -> Unit = { _, _ -> },
    onWordLongPress: (word: String, fullSentence: String) -> Unit = { _, _ -> },
    onDictionaryTermClick: (PageDictionaryTerm) -> Unit = {},
    onExtractBlockTerms: ((segmentIndex: Int, blockText: String) -> Unit)? = null,
    onSegmentClickForExplanation: (segmentIndex: Int, segmentText: String) -> Unit = { _, _ -> },
    onLanguageChangeAndSpeak: (langCode: String, textToSpeak: String) -> Unit = { _, _ -> },
    onSpeakExplanation: (langCode: String, text: String) -> Unit = { _, _ -> },
    onSpeakExplanationWord: (langCode: String, fullText: String, wordOffset: Int) -> Unit = { _, _, _ -> },
    onSpeakTranslatedBlock: (language: StudyLanguage, text: String, startWordOffset: Int, lineId: String) -> Unit = { _, _, _, _ -> },
    onPlayOpenAiAudio: ((audioFile: File, lineId: String, text: String, startWordOffset: Int) -> Unit)? = null,
    isLanguageAvailableOnDevice: ((Locale) -> Boolean)? = null,
    onStopSpeaking: () -> Unit = {},
    onToggleSpeech: () -> Unit = {},
    onRegenerateExplanation: (segmentIndex: Int, segmentText: String) -> Unit = { _, _ -> },
    onDeleteExplanation: (PageSegmentExplanation) -> Unit = {},
    onDismissExplanation: () -> Unit = {},
    onSendChatMessage: (segmentIndex: Int, segmentText: String, question: String, isAudio: Boolean) -> Unit = { _, _, _, _ -> },
    onClearSegmentChat: (segmentIndex: Int) -> Unit = {},
    onDeleteChatMessage: (SegmentChatMessage) -> Unit = {},
    onAnalyzeSentence: ((sentenceText: String) -> Unit)? = null,
    speechManager: PdfSpeechManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBlockLanguage by remember(paragraph.paragraphIndex) {
        mutableStateOf(StudyLanguage.PORTUGUESE)
    }
    var blockTranslationText by remember(paragraph.paragraphIndex) {
        mutableStateOf<String?>(null)
    }
    var isTranslatingBlock by remember { mutableStateOf(false) }

    var showAllLanguagesDialog by remember { mutableStateOf(false) }
    var isGeneratingOpenAiAudio by remember { mutableStateOf(false) }
    var pendingVoiceEngineRequest by remember {
        mutableStateOf<Triple<StudyLanguage, String, Pair<Int, String>>?>(null)
    }

    var isChatOpen by remember { mutableStateOf(false) }
    var isChatMinimized by remember { mutableStateOf(false) }

    LaunchedEffect(segmentChatMessages.size, isSegmentChatLoading) {
        if (segmentChatMessages.isNotEmpty() || isSegmentChatLoading) {
            isChatOpen = true
        }
    }

    fun triggerOpenAiSpeech(
        lang: StudyLanguage,
        text: String,
        wordOffset: Int,
        lineId: String
    ) {
        isGeneratingOpenAiAudio = true
        scope.launch {
            val apiKey = OpenAiService.getApiKey(context)
            val audioResult = OpenAiSpeechCache.getOrGenerateAudio(
                context = context,
                text = text,
                langCode = lang.code,
                blockId = lineId,
                customApiKey = apiKey
            )
            isGeneratingOpenAiAudio = false
            if (audioResult.isSuccess) {
                val file = audioResult.getOrThrow()
                if (onPlayOpenAiAudio != null) {
                    onPlayOpenAiAudio(file, lineId, text, wordOffset)
                } else {
                    onSpeakTranslatedBlock(lang, text, wordOffset, lineId)
                }
            } else {
                val err = audioResult.exceptionOrNull()?.message ?: "Erro ao gerar áudio OpenAI"
                Toast.makeText(context, "$err. Usando voz do sistema.", Toast.LENGTH_SHORT).show()
                onSpeakTranslatedBlock(lang, text, wordOffset, lineId)
            }
        }
    }

    fun handleTranslatedTextPlay(
        lang: StudyLanguage,
        text: String,
        wordOffset: Int,
        lineId: String
    ) {
        if (text.isBlank()) return

        val phoneHasVoice = isLanguageAvailableOnDevice?.invoke(lang.ttsLocale) ?: true
        val savedPref = VoiceEnginePreferences.getPreferredEngine(context, lang.code)

        if (!phoneHasVoice) {
            // Se o telefone não tiver a voz do idioma instalada -> Usa voz da OpenAI automaticamente!
            triggerOpenAiSpeech(lang, text, wordOffset, lineId)
            return
        }

        if (savedPref == VoiceEngineChoice.OPENAI) {
            triggerOpenAiSpeech(lang, text, wordOffset, lineId)
        } else if (savedPref == VoiceEngineChoice.PHONE) {
            onSpeakTranslatedBlock(lang, text, wordOffset, lineId)
        } else {
            // Bloquinho flutuante: pergunta se quer voz do telefone ou da OpenAI
            pendingVoiceEngineRequest = Triple(lang, text, Pair(wordOffset, lineId))
        }
    }

    // 1. Build continuous text and map word positions
    val (combinedText, paragraphWords) = remember(paragraph) {
        val words = mutableListOf<ParagraphWord>()
        val sb = StringBuilder()
        val wordRegex = Regex("[^ \\t\\n\\r\\u00A0]+")

        paragraph.sentences.forEachIndexed { sLocalIdx, sentenceText ->
            val globalSentenceIdx = paragraph.globalStartSentenceIndex + sLocalIdx
            if (sLocalIdx > 0) {
                sb.append(" ")
            }
            val sentenceStartInCombined = sb.length
            val matches = wordRegex.findAll(sentenceText)
            matches.forEachIndexed { wIdx, match ->
                words.add(
                    ParagraphWord(
                        globalSentenceIndex = globalSentenceIdx,
                        sentenceIndexInParagraph = sLocalIdx,
                        wordIndex = wIdx,
                        startChar = sentenceStartInCombined + match.range.first,
                        endChar = sentenceStartInCombined + match.range.last + 1,
                        word = match.value
                    )
                )
            }
            sb.append(sentenceText)
        }

        val rawText = sb.toString()
        // Replace single character space breaks with non-breaking spaces
        val polished = rawText.replace(Regex("(\\b[a-zA-Z0-9]\\b) "), "$1\u00A0")
        polished to words
    }

    // 2. Identify dictionary terms in paragraph
    val termMatches = remember(combinedText, dictionaryTerms) {
        if (dictionaryTerms.isEmpty() || combinedText.isBlank()) {
            emptyList()
        } else {
            val matches = mutableListOf<ParagraphTermMatch>()
            dictionaryTerms.forEach { dt ->
                val term = dt.term.trim()
                if (term.isNotBlank()) {
                    var startIndex = 0
                    while (startIndex < combinedText.length) {
                        val foundIndex = combinedText.indexOf(term, startIndex, ignoreCase = true)
                        if (foundIndex == -1) break
                        matches.add(
                            ParagraphTermMatch(
                                startChar = foundIndex,
                                endChar = foundIndex + term.length,
                                term = dt
                            )
                        )
                        startIndex = foundIndex + term.length
                    }
                }
            }
            matches
        }
    }

    // Paragraph sentences range
    val paragraphSegmentIndices = remember(paragraph) {
        (paragraph.globalStartSentenceIndex until (paragraph.globalStartSentenceIndex + paragraph.sentences.size)).toSet()
    }

    val paragraphExplanation = remember(pageSegmentExplanations, paragraphSegmentIndices) {
        pageSegmentExplanations.firstOrNull { it.segmentIndex in paragraphSegmentIndices }
    }

    val themePalette = remember(readingTheme, category, categoryColor) {
        ReadingThemeManager.getPalette(readingTheme, category, categoryColor)
    }

    val karaokeSentenceBg = themePalette.activeSentenceBg
    val karaokeWordBg = themePalette.activeWordBg
    val karaokeWordText = themePalette.activeWordText

    val dictionaryHighlightColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFFB87800)
        "TERMINAL_GREEN" -> Color(0xFF00FF99)
        "CYBERPUNK_NEON" -> Color(0xFFFF00AA)
        "LIGHT" -> Color(0xFF0284C7)
        "MONOCHROME_BW" -> Color(0xFFE5E5E5)
        "CINEMA_YELLOW" -> Color(0xFFFFB703)
        else -> themePalette.accentSecondary.takeIf { it != Color.Unspecified } ?: themePalette.accent
    }

    val buttonAccentColor = themePalette.accent
    val chatBrandColor = themePalette.accentSecondary.takeIf { it != Color.Unspecified && it != themePalette.accent } ?: themePalette.accent

    // 3. Build AnnotatedString for the entire justified paragraph
    val isParagraphActiveInSpeech = isSpeaking && activeSentenceIndex in paragraphSegmentIndices
    val isUsingOriginalPortuguese = selectedBlockLanguage == StudyLanguage.PORTUGUESE

    val blockLineId = remember(paragraph.paragraphIndex, selectedBlockLanguage) {
        "trans_${paragraph.paragraphIndex}_${selectedBlockLanguage.code}"
    }
    val isThisTranslatedBlockSpeaking = isSpeaking && activeComparativeLineId == blockLineId

    val activeDisplayText = remember(selectedBlockLanguage, blockTranslationText, combinedText) {
        if (selectedBlockLanguage == StudyLanguage.PORTUGUESE) {
            combinedText
        } else {
            blockTranslationText ?: TermsNotesTranslator.getQuickTextBlockTranslation(combinedText, selectedBlockLanguage)
        }
    }

    val translatedWords = remember(activeDisplayText) {
        val words = mutableListOf<ParagraphWord>()
        val matches = PdfSpeechManager.WORD_REGEX.findAll(activeDisplayText)
        matches.forEachIndexed { wIdx, match ->
            words.add(
                ParagraphWord(
                    globalSentenceIndex = paragraph.globalStartSentenceIndex,
                    sentenceIndexInParagraph = 0,
                    wordIndex = wIdx,
                    startChar = match.range.first,
                    endChar = match.range.last + 1,
                    word = match.value
                )
            )
        }
        words
    }

    val annotatedString: AnnotatedString = remember(
        combinedText,
        activeDisplayText,
        isUsingOriginalPortuguese,
        paragraphWords,
        translatedWords,
        termMatches,
        isParagraphActiveInSpeech,
        isThisTranslatedBlockSpeaking,
        activeSentenceIndex,
        activeWordIndex,
        textColor,
        karaokeSentenceBg,
        karaokeWordBg,
        karaokeWordText,
        dictionaryHighlightColor
    ) {
        buildAnnotatedString {
            if (isUsingOriginalPortuguese) {
                append(combinedText)

                // Sentence highlight (strictly synchronized and closed by period .)
                if (isParagraphActiveInSpeech) {
                    val sentenceWords = paragraphWords.filter { it.globalSentenceIndex == activeSentenceIndex }
                    if (sentenceWords.isNotEmpty()) {
                        val sStart = sentenceWords.minOf { it.startChar }
                        val sEnd = sentenceWords.maxOf { it.endChar }
                        val closingPeriodIdx = combinedText.indexOf('.', startIndex = sEnd.coerceAtMost(combinedText.length))
                        val effectiveEnd = if (closingPeriodIdx != -1 && closingPeriodIdx - sEnd <= 3) {
                            closingPeriodIdx + 1
                        } else {
                            sEnd
                        }
                        addStyle(
                            style = SpanStyle(background = karaokeSentenceBg),
                            start = sStart,
                            end = effectiveEnd.coerceAtMost(combinedText.length)
                        )
                    }

                    // Active word highlight
                    val activeWord = sentenceWords.firstOrNull { it.wordIndex == activeWordIndex }
                    if (activeWord != null) {
                        addStyle(
                            style = SpanStyle(
                                background = karaokeWordBg,
                                fontWeight = FontWeight.Bold,
                                color = karaokeWordText
                            ),
                            start = activeWord.startChar,
                            end = activeWord.endChar
                        )
                    }
                }

                // Dictionary terms underline / highlight
                termMatches.forEach { match ->
                    addStyle(
                        style = SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold,
                            color = dictionaryHighlightColor
                        ),
                        start = match.startChar,
                        end = match.endChar
                    )
                }
            } else {
                append(activeDisplayText)

                // Destaque do texto traduzido: oração por oração e palavra por palavra perfeitamente sincronizado com a voz
                if (isThisTranslatedBlockSpeaking && translatedWords.isNotEmpty()) {
                    val activeWord = translatedWords.firstOrNull { it.wordIndex == activeWordIndex }
                        ?: translatedWords.getOrNull(activeWordIndex)

                    if (activeWord != null) {
                        // Limite da oração na tradução (fechada por pontuação final ., !, ?, 。, ؟)
                        val textBefore = activeDisplayText.substring(0, activeWord.startChar)
                        val lastSentenceBoundary = maxOf(
                            textBefore.lastIndexOf('.'),
                            textBefore.lastIndexOf('!'),
                            textBefore.lastIndexOf('?'),
                            textBefore.lastIndexOf('。'),
                            textBefore.lastIndexOf('؟')
                        )
                        val sentenceStart = if (lastSentenceBoundary == -1) 0 else (lastSentenceBoundary + 1).coerceAtMost(activeWord.startChar)

                        val textAfter = activeDisplayText.substring(activeWord.endChar)
                        val nextBoundaryRel = listOf(
                            textAfter.indexOf('.'),
                            textAfter.indexOf('!'),
                            textAfter.indexOf('?'),
                            textAfter.indexOf('。'),
                            textAfter.indexOf('؟')
                        ).filter { it != -1 }.minOrNull()

                        val sentenceEnd = if (nextBoundaryRel == null) {
                            activeDisplayText.length
                        } else {
                            (activeWord.endChar + nextBoundaryRel + 1).coerceAtMost(activeDisplayText.length)
                        }

                        if (sentenceStart < sentenceEnd) {
                            addStyle(
                                style = SpanStyle(background = karaokeSentenceBg),
                                start = sentenceStart,
                                end = sentenceEnd
                            )
                        }

                        // Destaque visual da palavra ativa
                        addStyle(
                            style = SpanStyle(
                                background = karaokeWordBg,
                                fontWeight = FontWeight.Bold,
                                color = karaokeWordText
                            ),
                            start = activeWord.startChar,
                            end = activeWord.endChar
                        )
                    }
                }
            }
        }
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val isThisParagraphExpanded = activeExpandedSegmentIndex != null && activeExpandedSegmentIndex in paragraphSegmentIndices

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val isRtl = selectedBlockLanguage.isRtl
        val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

        // Continuous text block with RTL support for Hebrew and Arabic
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Text(
                text = annotatedString,
                textAlign = if (isRtl) TextAlign.Start else TextAlign.Justify,
                fontFamily = fontFamily,
                fontSize = textFontSizeSp.sp,
                lineHeight = (textFontSizeSp * 1.62f).sp,
                letterSpacing = if (selectedBlockLanguage == StudyLanguage.CHINESE || selectedBlockLanguage == StudyLanguage.JAPANESE) 0.8.sp else 0.2.sp,
                color = textColor,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(paragraphWords, translatedWords, termMatches, combinedText, activeDisplayText, isUsingOriginalPortuguese, selectedBlockLanguage, blockLineId) {
                        detectTapGestures(
                            onTap = { offset ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                val charOffset = layout.getOffsetForPosition(offset)

                                if (isUsingOriginalPortuguese) {
                                    // 1. Check if user tapped a dictionary term
                                    val clickedTermMatch = termMatches.firstOrNull { charOffset in it.startChar until it.endChar }
                                    if (clickedTermMatch != null) {
                                        onDictionaryTermClick(clickedTermMatch.term)
                                        return@detectTapGestures
                                    }

                                    // 2. Check if user tapped a word -> Start reading immediately from that word in Portuguese!
                                    val clickedWord = paragraphWords.firstOrNull { charOffset in it.startChar until it.endChar }
                                        ?: paragraphWords.minByOrNull { abs(charOffset - it.startChar) }

                                    if (clickedWord != null) {
                                        onWordClick(clickedWord.globalSentenceIndex, clickedWord.wordIndex)
                                    }
                                } else {
                                    // 3. User tapped on the translated text!
                                    // Automatically switch voice to the translated language and start reading from this word!
                                    val clickedWord = translatedWords.firstOrNull { charOffset in it.startChar until it.endChar }
                                        ?: translatedWords.minByOrNull { abs(charOffset - it.startChar) }
                                    val startWordOffset = clickedWord?.wordIndex ?: 0
                                    handleTranslatedTextPlay(selectedBlockLanguage, activeDisplayText, startWordOffset, blockLineId)
                                }
                            },
                            onLongPress = { offset ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                val charOffset = layout.getOffsetForPosition(offset)

                                if (isUsingOriginalPortuguese) {
                                    val clickedWord = paragraphWords.firstOrNull { charOffset in it.startChar until it.endChar }
                                        ?: paragraphWords.minByOrNull { abs(charOffset - it.startChar) }

                                    if (clickedWord != null) {
                                        val clean = clickedWord.word.trim().trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
                                        if (clean.isNotBlank()) {
                                            val fullSentence = paragraph.sentences.getOrNull(clickedWord.globalSentenceIndex - paragraph.globalStartSentenceIndex)
                                                ?: combinedText
                                            onWordLongPress(clean, fullSentence)
                                        }
                                    }
                                } else {
                                    val clickedWord = translatedWords.firstOrNull { charOffset in it.startChar until it.endChar }
                                        ?: translatedWords.minByOrNull { abs(charOffset - it.startChar) }

                                    if (clickedWord != null) {
                                        val clean = clickedWord.word.trim().trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
                                        if (clean.isNotBlank()) {
                                            onWordLongPress(clean, activeDisplayText)
                                        }
                                    }
                                }
                            }
                        )
                    }
            )
        }

        // =========================================================================
        // NOTAS DE RODAPÉ (CONCEITOS & DICIONÁRIO OFFLINE DO TEXTO)
        // Exibe nota de rodapé estilizada com fonte menor abaixo do bloco com os termos destacados
        // =========================================================================
        val matchingTerms = remember(termMatches) {
            termMatches.map { it.term }.distinctBy { it.term.lowercase().trim() }
        }

        var areFootnotesExpanded by remember { mutableStateOf(true) }

        if (matchingTerms.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.32f),
                border = BorderStroke(0.8.dp, dictionaryHighlightColor.copy(alpha = 0.38f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    // Cabeçalho das Notas de Rodapé (Minimalista: NOTAS)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { areFootnotesExpanded = !areFootnotesExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = dictionaryHighlightColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NOTAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                color = dictionaryHighlightColor
                            )
                        }
                        Icon(
                            imageVector = if (areFootnotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = dictionaryHighlightColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (areFootnotesExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        matchingTerms.forEachIndexed { fIndex, fTerm ->
                            if (fIndex > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    thickness = 0.5.dp
                                )
                            }
                            MultilingualTermItem(
                                term = fTerm,
                                speechManager = speechManager,
                                termIndex = fIndex + 1,
                                accentColor = dictionaryHighlightColor,
                                textColor = textColor,
                                titleFontSize = (textFontSizeSp * 0.75f).coerceIn(11f, 13.sp.value).sp,
                                bodyFontSize = (textFontSizeSp * 0.70f).coerceIn(10f, 12.sp.value).sp,
                                bodyLineHeight = ((textFontSizeSp * 0.70f) * 1.40f).coerceIn(14f, 17.sp.value).sp,
                                highlightBgColor = Color(0xFFFBBF24).copy(alpha = 0.45f),
                                highlightTextColor = Color(0xFFFEF08A),
                                onTermClick = { onDictionaryTermClick(fTerm) },
                                onFallbackSpeak = { langCode, text ->
                                    onSpeakExplanation(langCode, text)
                                },
                                scopeId = "paragraph_term_${paragraph.paragraphIndex}_${fTerm.id}"
                            )
                        }
                    }
                }
            }
        }

        // Seletor de Idiomas de Tradução do Bloco de Texto (OpenAI API com Bandeiras para passar com o dedo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudyLanguage.allSupportedLanguages.forEach { lang ->
                val isSelected = selectedBlockLanguage == lang
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) buttonAccentColor.copy(alpha = 0.28f) else Color.Transparent,
                    border = BorderStroke(
                        0.7.dp,
                        if (isSelected) buttonAccentColor else textColor.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.clickable {
                        val prevLang = selectedBlockLanguage
                        if (prevLang != lang && (isSpeaking || isThisTranslatedBlockSpeaking)) {
                            onStopSpeaking()
                        }
                        selectedBlockLanguage = lang
                        if (lang == StudyLanguage.PORTUGUESE) {
                            blockTranslationText = null
                        } else {
                            isTranslatingBlock = true
                            scope.launch {
                                val openAiKey = com.example.data.openai.OpenAiService.getApiKey(context)
                                val res = TermsNotesTranslator.translateTextBlock(
                                    text = combinedText,
                                    targetLanguage = lang,
                                    customApiKey = openAiKey,
                                    context = context
                                )
                                blockTranslationText = res
                                isTranslatingBlock = false
                            }
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = lang.flag,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = lang.code.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) buttonAccentColor else textColor.copy(alpha = 0.72f)
                        )
                        if (isSelected && isTranslatingBlock) {
                            Spacer(modifier = Modifier.width(4.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(8.dp),
                                strokeWidth = 1.dp,
                                color = buttonAccentColor
                            )
                        }
                    }
                }
            }

            // Botão "+ MAIS" para explorar todos os idiomas disponíveis
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = buttonAccentColor.copy(alpha = 0.12f),
                border = BorderStroke(0.7.dp, buttonAccentColor.copy(alpha = 0.40f)),
                modifier = Modifier.clickable {
                    showAllLanguagesDialog = true
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Mais Idiomas",
                        tint = buttonAccentColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "MAIS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = buttonAccentColor
                    )
                }
            }
        }

        // Indicador em tempo real quando a síntese da OpenAI com voz aleatória estiver processando
        if (isGeneratingOpenAiAudio) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.18f),
                border = BorderStroke(0.8.dp, Color(0xFF34D399).copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFF34D399)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ Gerando narração com voz neural OpenAI...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF34D399)
                    )
                }
            }
        }

        // Clean & Minimalist Button Row directly below each paragraph
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Button: "termos" (minimalista)
            if (onExtractBlockTerms != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = dictionaryHighlightColor.copy(alpha = if (matchingTerms.isNotEmpty()) 0.16f else 0.10f),
                    border = BorderStroke(
                        0.8.dp,
                        dictionaryHighlightColor.copy(alpha = if (matchingTerms.isNotEmpty()) 0.45f else 0.25f)
                    ),
                    modifier = Modifier.clickable {
                        onExtractBlockTerms(paragraph.globalStartSentenceIndex, combinedText)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExtractingBlockTerms) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.2.dp,
                                color = dictionaryHighlightColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "termos",
                                tint = dictionaryHighlightColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isExtractingBlockTerms) "..." else "termos",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = dictionaryHighlightColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))
            }

            // Button: "explicação" (minimalista)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = buttonAccentColor.copy(alpha = if (isThisParagraphExpanded) 0.18f else 0.08f),
                border = BorderStroke(
                    0.8.dp,
                    buttonAccentColor.copy(alpha = if (isThisParagraphExpanded) 0.50f else 0.25f)
                ),
                modifier = Modifier.clickable {
                    if (isThisParagraphExpanded) {
                        onDismissExplanation()
                    } else {
                        val primarySentence = paragraph.sentences.firstOrNull() ?: combinedText
                        onSegmentClickForExplanation(paragraph.globalStartSentenceIndex, primarySentence)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isThisParagraphExpanded) Icons.Default.KeyboardArrowUp else Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "explicação",
                        tint = buttonAccentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "explicação",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = buttonAccentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(5.dp))

            // Button: "chat" (minimalista)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isChatOpen) chatBrandColor.copy(alpha = 0.20f) else chatBrandColor.copy(alpha = 0.08f),
                border = BorderStroke(
                    0.8.dp,
                    if (isChatOpen) chatBrandColor else chatBrandColor.copy(alpha = 0.35f)
                ),
                modifier = Modifier.clickable {
                    if (!isChatOpen) {
                        isChatOpen = true
                        isChatMinimized = false
                    } else {
                        isChatMinimized = !isChatMinimized
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "chat",
                        tint = chatBrandColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "chat",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = chatBrandColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(5.dp))

            // Button: "sintaxe" (Análise estrutural e gramatical da frase nos 8 idiomas)
            if (onAnalyzeSentence != null) {
                val syntaxBrandColor = themePalette.accentSecondary.takeIf { it != Color.Unspecified } ?: themePalette.accent
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = syntaxBrandColor.copy(alpha = 0.09f),
                    border = BorderStroke(0.8.dp, syntaxBrandColor.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable {
                        val targetSentence = paragraph.sentences.firstOrNull() ?: combinedText
                        onAnalyzeSentence(targetSentence)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "sintaxe",
                            tint = syntaxBrandColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "sintaxe",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = syntaxBrandColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))
            }

            // Button: Player Pausar / Despausar (SOMENTE SÍMBOLOS, SEM PALAVRAS)
            val playerBrandColor = themePalette.accent
            val isCurrentSpeaking = if (isUsingOriginalPortuguese) (isSpeaking && activeSentenceIndex in paragraphSegmentIndices) else isThisTranslatedBlockSpeaking
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isCurrentSpeaking) playerBrandColor.copy(alpha = 0.22f) else playerBrandColor.copy(alpha = 0.08f),
                border = BorderStroke(
                    0.8.dp,
                    if (isCurrentSpeaking) playerBrandColor else playerBrandColor.copy(alpha = 0.35f)
                ),
                modifier = Modifier.clickable {
                    if (isUsingOriginalPortuguese) {
                        onToggleSpeech()
                    } else {
                        if (isThisTranslatedBlockSpeaking) {
                            onStopSpeaking()
                        } else {
                            handleTranslatedTextPlay(selectedBlockLanguage, activeDisplayText, 0, blockLineId)
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentSpeaking) "Pausar" else "Ouvir",
                        tint = if (isCurrentSpeaking) (themePalette.accentSecondary.takeIf { it != Color.Unspecified } ?: playerBrandColor) else playerBrandColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // In-place AI Explanation Card (Visible when this paragraph is expanded)
        if (isThisParagraphExpanded) {
            val exp = pageSegmentExplanations.firstOrNull { it.segmentIndex == activeExpandedSegmentIndex }
                ?: paragraphExplanation

            val targetSegmentText = remember(activeExpandedSegmentIndex, paragraph) {
                val localIdx = (activeExpandedSegmentIndex ?: 0) - paragraph.globalStartSentenceIndex
                paragraph.sentences.getOrNull(localIdx) ?: combinedText
            }

            SegmentAiExplanationCard(
                explanation = exp,
                isLoading = isGeneratingExplanation,
                errorMessage = explanationError,
                readingTheme = readingTheme,
                category = category,
                categoryColor = categoryColor,
                isSpeaking = isSpeaking,
                activeComparativeLineId = activeComparativeLineId,
                activeWordIndex = activeWordIndex,
                onLanguageChangeAndSpeak = onLanguageChangeAndSpeak,
                onSpeakExplanation = onSpeakExplanation,
                onSpeakExplanationWord = onSpeakExplanationWord,
                onStopSpeaking = onStopSpeaking,
                onRegenerate = {
                    onRegenerateExplanation(activeExpandedSegmentIndex ?: paragraph.globalStartSentenceIndex, targetSegmentText)
                },
                onDelete = {
                    if (exp != null) {
                        onDeleteExplanation(exp)
                    }
                },
                onDismiss = onDismissExplanation,
                onOpenChat = {
                    isChatOpen = true
                    isChatMinimized = false
                },
                textFontSizeSp = textFontSizeSp,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )
        }

        // In-place OpenAI Segment Chat Card (Can be minimized or expanded)
        if (isChatOpen) {
            SegmentAiChatCard(
                segmentIndex = paragraph.globalStartSentenceIndex,
                segmentText = combinedText,
                messages = segmentChatMessages,
                isLoading = isSegmentChatLoading,
                errorMessage = segmentChatError,
                isMinimized = isChatMinimized,
                readingTheme = readingTheme,
                category = category,
                categoryColor = categoryColor,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                onToggleMinimize = { isChatMinimized = !isChatMinimized },
                onSendMessage = { question, isAudio ->
                    onSendChatMessage(paragraph.globalStartSentenceIndex, combinedText, question, isAudio)
                },
                onClearChat = {
                    onClearSegmentChat(paragraph.globalStartSentenceIndex)
                },
                onDeleteMessage = onDeleteChatMessage,
                onDismiss = {
                    isChatOpen = false
                    isChatMinimized = false
                },
                onSpeakTextInLang = { langCode, text ->
                    onSpeakExplanation(langCode, text)
                },
                onStopSpeaking = onStopSpeaking
            )
        }

        // Bloquinho flutuante de escolha de motor de voz (Telefone vs OpenAI)
        if (pendingVoiceEngineRequest != null) {
            val req = pendingVoiceEngineRequest!!
            val lang = req.first
            val text = req.second
            val wordOffset = req.third.first
            val lineId = req.third.second
            VoiceEngineChoiceDialog(
                languageName = lang.displayName,
                languageFlag = lang.flag,
                onChooseEngine = { choice, rememberChoice ->
                    if (rememberChoice) {
                        VoiceEnginePreferences.setPreferredEngine(context, lang.code, choice)
                    }
                    pendingVoiceEngineRequest = null
                    if (choice == VoiceEngineChoice.OPENAI) {
                        triggerOpenAiSpeech(lang, text, wordOffset, lineId)
                    } else {
                        onSpeakTranslatedBlock(lang, text, wordOffset, lineId)
                    }
                },
                onDismissRequest = {
                    pendingVoiceEngineRequest = null
                }
            )
        }

        // Diálogo para adicionar e selecionar mais idiomas
        if (showAllLanguagesDialog) {
            AllLanguagesDialog(
                selectedLanguage = selectedBlockLanguage,
                onSelectLanguage = { lang ->
                    val prevLang = selectedBlockLanguage
                    if (prevLang != lang && (isSpeaking || isThisTranslatedBlockSpeaking)) {
                        onStopSpeaking()
                    }
                    selectedBlockLanguage = lang
                    if (lang == StudyLanguage.PORTUGUESE) {
                        blockTranslationText = null
                    } else {
                        isTranslatingBlock = true
                        scope.launch {
                            val openAiKey = OpenAiService.getApiKey(context)
                            val res = TermsNotesTranslator.translateTextBlock(
                                text = combinedText,
                                targetLanguage = lang,
                                customApiKey = openAiKey,
                                context = context
                            )
                            blockTranslationText = res
                            isTranslatingBlock = false
                        }
                    }
                },
                onDismissRequest = {
                    showAllLanguagesDialog = false
                }
            )
        }
    }
}
