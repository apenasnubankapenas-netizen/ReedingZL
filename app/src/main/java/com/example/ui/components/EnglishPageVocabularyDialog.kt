package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.EnglishWordAnalysis
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PdfBook
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.launch

/**
 * Diálogo de Scanner e Auditoria Lexical C2 da Página Inteira.
 * Extrai todos os termos de nível C2, C1, B2, phrasal verbs e expressões idiomáticas da página,
 * permitindo aprender palavra por palavra com fonética e collocations.
 */
@Composable
fun EnglishPageVocabularyDialog(
    book: PdfBook,
    pageNumber: Int,
    pageText: String,
    readingTheme: String = "DARK_OLED",
    speechManager: PdfSpeechManager? = null,
    onSaveDictionaryTerms: ((List<PageDictionaryTerm>) -> Unit)? = null,
    onOpenSingleWordAnalysis: (String, String) -> Unit,
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

    var apiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var wordsList by remember { mutableStateOf<List<EnglishWordAnalysis>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedFilter by remember { mutableStateOf("TODOS") }
    var customSearchInput by remember { mutableStateOf("") }

    val filterOptions = listOf("TODOS", "C2 ELITE", "C1 AVANÇADO", "B2", "PHRASAL & IDIOMS")

    fun scanPageVocabulary() {
        if (apiKey.isBlank()) {
            errorMessage = "Configure sua Chave de API da OpenAI para escanear a página."
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val res = OpenAiService.analyzeEnglishPageVocabularyC2(
                apiKey = apiKey,
                bookTitle = book.title,
                pageNumber = pageNumber,
                pageText = pageText
            )
            isLoading = false
            res.onSuccess { list ->
                wordsList = list
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Erro ao analisar vocabulário da página."
            }
        }
    }

    // Auto-scan on launch if not scanned yet
    LaunchedEffect(book.id, pageNumber) {
        if (apiKey.isNotBlank() && wordsList.isEmpty()) {
            scanPageVocabulary()
        }
    }

    val filteredWords = remember(wordsList, selectedFilter, customSearchInput) {
        var base = wordsList
        if (customSearchInput.isNotBlank()) {
            val q = customSearchInput.trim().lowercase()
            base = base.filter {
                it.word.lowercase().contains(q) ||
                it.englishDefinition.lowercase().contains(q) ||
                it.portugueseMeaning.lowercase().contains(q)
            }
        }
        when (selectedFilter) {
            "C2 ELITE" -> base.filter { it.cefrLevel.equals("C2", ignoreCase = true) }
            "C1 AVANÇADO" -> base.filter { it.cefrLevel.equals("C1", ignoreCase = true) }
            "B2" -> base.filter { it.cefrLevel.equals("B2", ignoreCase = true) }
            "PHRASAL & IDIOMS" -> base.filter { it.isIdiomOrPhrasalVerb || it.partOfSpeech.contains("phrasal", ignoreCase = true) || it.partOfSpeech.contains("idiom", ignoreCase = true) }
            else -> base
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            border = BorderStroke(1.2.dp, borderColor),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Spellcheck,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Vocabulário C2 • Pág. $pageNumber",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "Dissecação de termos nativos e colocações da página",
                                fontSize = 11.sp,
                                color = subTextColor
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { scanPageVocabulary() },
                            modifier = Modifier.size(32.dp),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reescanear", tint = accent, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = subTextColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Word Quick Search Bar
                OutlinedTextField(
                    value = customSearchInput,
                    onValueChange = { customSearchInput = it },
                    placeholder = { Text("Consultar ou filtrar qualquer palavra desta página...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = subTextColor) },
                    trailingIcon = {
                        if (customSearchInput.isNotBlank()) {
                            IconButton(onClick = {
                                onOpenSingleWordAnalysis(customSearchInput.trim(), pageText)
                            }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Analisar", tint = accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // CEFR Level Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accent else cardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accent else borderColor.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) palette.buttonContent else textColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Extracted Words
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = accent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Extraindo e catalogando vocabulário C2 da página...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { scanPageVocabulary() },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Tentar Novamente", color = palette.buttonContent)
                            }
                        }
                    } else if (filteredWords.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = subTextColor, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (wordsList.isEmpty()) "Toque em reescanear ou digite acima para analisar qualquer palavra." else "Nenhuma palavra encontrada neste filtro.",
                                fontSize = 12.5.sp,
                                color = subTextColor
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredWords, key = { it.word }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onOpenSingleWordAnalysis(item.word, item.contextualSentence)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.word,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Serif,
                                                    color = textColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = item.cefrBadgeColor.copy(alpha = 0.18f),
                                                    border = BorderStroke(0.7.dp, item.cefrBadgeColor.copy(alpha = 0.6f))
                                                ) {
                                                    Text(
                                                        text = item.cefrLevel,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = item.cefrBadgeColor,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                                if (item.phoneticIpa.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = item.phoneticIpa,
                                                        fontSize = 12.sp,
                                                        color = accent,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { speechManager?.speakTextImmediately(item.word) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Ouvir", tint = accent, modifier = Modifier.size(16.dp))
                                                }
                                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.englishDefinition,
                                            fontSize = 12.5.sp,
                                            color = textColor.copy(alpha = 0.9f),
                                            lineHeight = 17.sp
                                        )

                                        if (item.portugueseMeaning.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "• PT: ${item.portugueseMeaning}",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF10B981)
                                            )
                                        }

                                        if (item.collocations.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Collocations: ${item.collocations.take(3).joinToString(" • ")}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                        category = item.partOfSpeech.ifBlank { "Inglês C2" },
                                        originalContext = item.contextualSentence,
                                        originalDefinition = "${item.phoneticIpa} [${item.cefrLevel}] ${item.englishDefinition}\n\n• PT: ${item.portugueseMeaning}\n• Collocations: ${item.collocations.joinToString(", ")}",
                                        englishTerm = item.word,
                                        englishDefinition = item.englishDefinition,
                                        englishExample = item.contextualSentence
                                    )
                                }
                                onSaveDictionaryTerms(terms)
                                Toast.makeText(context, "${terms.size} termos salvos no dicionário do livro!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar Todos no Glossário", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Fechar", color = palette.buttonContent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
