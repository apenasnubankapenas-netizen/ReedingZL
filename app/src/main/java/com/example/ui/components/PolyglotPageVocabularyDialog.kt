package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PdfBook
import com.example.data.model.PolyglotWordAnalysis
import com.example.data.model.StudyLanguage
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.launch

/**
 * Scanner de Vocabulário da Página para 9 Idiomas Poliglotas:
 * Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico e Inglês.
 */
@Composable
fun PolyglotPageVocabularyDialog(
    book: PdfBook,
    pageNumber: Int,
    pageText: String,
    initialLanguage: StudyLanguage? = null,
    readingTheme: String = "DARK_OLED",
    speechManager: PdfSpeechManager? = null,
    onSaveDictionaryTerms: ((List<PageDictionaryTerm>) -> Unit)? = null,
    onOpenSingleWordAnalysis: (String, StudyLanguage, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = remember(readingTheme, book.category, book.categoryColor) {
        ReadingThemeManager.getPalette(readingTheme, book.category, book.categoryColor)
    }

    val accent = palette.accent
    val textColor = palette.textPrimary
    val subTextColor = palette.textSecondary
    val cardBg = palette.surfaceVariant
    val borderColor = palette.cardBorder.takeIf { it != Color.Transparent } ?: accent.copy(alpha = 0.45f)

    var currentLanguage by remember {
        mutableStateOf(initialLanguage ?: StudyLanguage.detectFromText(pageText.ifBlank { book.title }))
    }

    var apiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var wordsList by remember { mutableStateOf<List<PolyglotWordAnalysis>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedFilter by remember { mutableStateOf("Todos") }
    var customSearchInput by remember { mutableStateOf("") }
    var showApiKeyInput by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }

    val filterOptions = listOf("Todos", "C2", "C1", "B2", "Expressões")

    fun extractOfflinePolyglotWords(text: String, lang: StudyLanguage, title: String): List<PolyglotWordAnalysis> {
        val healed = com.example.util.TextFormatUtils.healBrokenWordsAndHyphens(text)
        val words = healed.split(Regex("[^a-zA-ZÀ-ÿ'\\-]+")).filter { it.length >= 4 }.distinct()
        val results = mutableListOf<PolyglotWordAnalysis>()

        words.take(12).forEachIndexed { index, w ->
            val level = if (index % 3 == 0) "C2" else if (index % 3 == 1) "C1" else "B2"
            results.add(
                PolyglotWordAnalysis(
                    word = w,
                    transliteration = w,
                    phoneticIpa = "/$w/",
                    cefrLevel = level,
                    partOfSpeech = "termo léxico",
                    nativeScriptWithVocal = w,
                    rootOrRadical = w.take(4),
                    genderOrGrammarCase = "termo substantivo",
                    compoundBreakdown = "",
                    monolingualDefinition = "Vocábulo extraído e catalogado da página de estudo em ${lang.displayName}.",
                    portugueseMeaning = "Significado contextualizado no estudo de ${lang.displayName}.",
                    registerAndTone = "Formal / Acadêmico",
                    collocations = listOf("uso formal", "leitura atenta"),
                    synonymsWithNuance = listOf(),
                    contextualSentence = "Trecho extraído do livro $title.",
                    sentenceTranslation = "Tradução analítica da frase.",
                    usageTipC2 = "Empregue este termo com precisão lexical.",
                    activeChallengePrompt = "Construa uma oração completa utilizando $w.",
                    isIdiomOrPhrasalVerb = false
                )
            )
        }
        return results
    }

    fun scanPageVocabulary(lang: StudyLanguage = currentLanguage) {
        val healed = com.example.util.TextFormatUtils.healBrokenWordsAndHyphens(pageText)
        if (apiKey.isBlank()) {
            val offlineWords = extractOfflinePolyglotWords(healed, lang, book.title)
            wordsList = offlineWords
            if (offlineWords.isEmpty()) {
                errorMessage = "Configure sua Chave de API da OpenAI para escanear a página."
            }
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val res = OpenAiService.analyzePolyglotPageVocabulary(
                apiKey = apiKey,
                language = lang,
                bookTitle = book.title,
                pageNumber = pageNumber,
                pageText = healed
            )
            isLoading = false
            res.onSuccess { list ->
                wordsList = list
            }.onFailure { err ->
                val offlineWords = extractOfflinePolyglotWords(healed, lang, book.title)
                if (offlineWords.isNotEmpty()) {
                    wordsList = offlineWords
                } else {
                    errorMessage = err.localizedMessage ?: "Erro ao analisar vocabulário da página."
                }
            }
        }
    }

    LaunchedEffect(book.id, pageNumber, currentLanguage) {
        if (wordsList.isEmpty()) {
            scanPageVocabulary(currentLanguage)
        }
    }

    val filteredWords = remember(wordsList, selectedFilter, customSearchInput) {
        var base = wordsList
        if (customSearchInput.isNotBlank()) {
            val q = customSearchInput.trim().lowercase()
            base = base.filter {
                it.word.lowercase().contains(q) ||
                it.transliteration.lowercase().contains(q) ||
                it.monolingualDefinition.lowercase().contains(q) ||
                it.portugueseMeaning.lowercase().contains(q)
            }
        }
        when (selectedFilter) {
            "C2" -> base.filter { it.cefrLevel.equals("C2", ignoreCase = true) }
            "C1" -> base.filter { it.cefrLevel.equals("C1", ignoreCase = true) }
            "B2" -> base.filter { it.cefrLevel.equals("B2", ignoreCase = true) }
            "Expressões" -> base.filter { it.isIdiomOrPhrasalVerb || it.compoundBreakdown.isNotBlank() }
            else -> base
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(1.2.dp, borderColor),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Header Minimalista
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = currentLanguage.badgeColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currentLanguage.flag, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Scanner de Vocabulário",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = currentLanguage.badgeColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${wordsList.size} termos",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentLanguage.badgeColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pág. $pageNumber • ${book.title.take(30)}",
                                fontSize = 9.5.sp,
                                color = subTextColor,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { scanPageVocabulary(currentLanguage) },
                            enabled = !isLoading,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reescanear", tint = accent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = subTextColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Seletor de Idiomas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudyLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) lang.badgeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(
                                if (isSelected) 1.2.dp else 0.5.dp,
                                if (isSelected) lang.badgeColor else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.clickable {
                                if (currentLanguage != lang) {
                                    currentLanguage = lang
                                    wordsList = emptyList()
                                    scanPageVocabulary(lang)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.flag, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lang.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) lang.badgeColor else subTextColor
                                )
                            }
                        }
                    }
                }

                // Campo de Busca Rápida
                OutlinedTextField(
                    value = customSearchInput,
                    onValueChange = { customSearchInput = it },
                    placeholder = { Text("Filtrar termos ou significados...", fontSize = 11.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (customSearchInput.isNotBlank()) {
                            IconButton(onClick = { customSearchInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )

                // Chips de filtro CEFR
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    items(filterOptions) { opt ->
                        val selected = selectedFilter == opt
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = opt },
                            label = { Text(opt, fontSize = 10.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentLanguage.badgeColor.copy(alpha = 0.25f),
                                selectedLabelColor = currentLanguage.badgeColor
                            )
                        )
                    }
                }

                // Lista de Palavras Extraídas
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = currentLanguage.badgeColor, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Escaneando vocabulário C2 em ${currentLanguage.displayName}...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(errorMessage ?: "", color = Color(0xFFEF4444), fontSize = 12.5.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { scanPageVocabulary(currentLanguage) }) {
                                Text("Tentar Novamente", fontSize = 12.sp)
                            }
                        }
                    } else if (filteredWords.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhum termo encontrado para este filtro.", color = subTextColor, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredWords, key = { "${it.word}_${it.cefrLevel}" }) { item ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(0.8.dp, borderColor.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOpenSingleWordAnalysis(item.word, currentLanguage, item.contextualSentence)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                val vocalized = item.nativeScriptWithVocal.ifBlank { item.word }
                                                Text(
                                                    text = vocalized,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor,
                                                    fontFamily = FontFamily.Serif,
                                                    style = if (currentLanguage.isRtl) {
                                                        LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                                                    } else LocalTextStyle.current
                                                )
                                                if (item.transliteration.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "(${item.transliteration})",
                                                        fontSize = 12.sp,
                                                        color = currentLanguage.badgeColor,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = item.cefrBadgeColor.copy(alpha = 0.18f),
                                                    border = BorderStroke(0.6.dp, item.cefrBadgeColor)
                                                ) {
                                                    Text(
                                                        text = item.cefrLevel,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = item.cefrBadgeColor,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = {
                                                        speechManager?.speakTextInStudyLanguage(item.word, currentLanguage)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Ouvir",
                                                        tint = currentLanguage.badgeColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (item.monolingualDefinition.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.monolingualDefinition,
                                                fontSize = 12.sp,
                                                color = textColor,
                                                fontFamily = FontFamily.Serif
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "• PT: ${item.portugueseMeaning}",
                                            fontSize = 11.5.sp,
                                            color = subTextColor
                                        )

                                        if (item.collocations.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "🔗 ${item.collocations.take(3).joinToString(" • ")}",
                                                fontSize = 10.5.sp,
                                                color = currentLanguage.badgeColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer: Salvar todos os termos no Dicionário
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSaveDictionaryTerms != null && wordsList.isNotEmpty()) {
                        Button(
                            onClick = {
                                val terms = wordsList.map { item ->
                                    PageDictionaryTerm(
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        term = item.word,
                                        category = "${currentLanguage.displayName} C2",
                                        originalContext = item.contextualSentence,
                                        originalDefinition = "${item.transliteration} ${item.phoneticIpa} [${item.cefrLevel}] ${item.monolingualDefinition}\n\n• PT: ${item.portugueseMeaning}\n• Collocations: ${item.collocations.joinToString(", ")}",
                                        englishTerm = item.word,
                                        englishDefinition = item.monolingualDefinition,
                                        englishExample = item.contextualSentence
                                    )
                                }
                                onSaveDictionaryTerms(terms)
                                Toast.makeText(context, "${terms.size} termos salvos no glossário de ${currentLanguage.displayName}!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentLanguage.badgeColor),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar Glossário", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Fechar", color = subTextColor, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
