package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ComparativeParagraph
import com.example.data.model.PageMultiTranslation
import com.example.data.model.PdfBook
import com.example.data.model.ReadingLanguage
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MultiTranslationDialogTab {
    READING,
    COMPARATIVE,
    EXPORT
}

enum class ComparativePairFilter {
    PT_EN,
    PT_FR,
    PT_ES,
    ALL_FOUR
}

@Composable
fun PageMultiTranslationDialog(
    book: PdfBook,
    pageNumber: Int,
    pageOriginalText: String,
    savedTranslation: PageMultiTranslation?,
    speechManager: PdfSpeechManager?,
    readingTheme: String = book.readingTheme ?: "MONOCHROME_BW",
    onDismiss: () -> Unit,
    onSaveTranslation: (PageMultiTranslation) -> Unit,
    onDeleteTranslation: (PageMultiTranslation) -> Unit,
    onApplyReadingLanguageToReader: (ReadingLanguage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = com.example.ui.theme.ReadingThemeManager.getPalette(readingTheme, book.category, book.categoryColor)

    var apiKey by remember { mutableStateOf(OpenAiService.getApiKey(context)) }
    var selectedModel by remember { mutableStateOf("gpt-4o-mini") }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var showApiKeyInput by remember { mutableStateOf(apiKey.isBlank()) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(MultiTranslationDialogTab.READING) }
    var activeReadingLang by remember { mutableStateOf(ReadingLanguage.ENGLISH) }
    var comparativePair by remember { mutableStateOf(ComparativePairFilter.PT_EN) }

    // Active playing state for paragraph TTS
    var currentlyPlayingText by remember { mutableStateOf<String?>(null) }

    val currentTranslation = savedTranslation

    Dialog(
        onDismissRequest = {
            if (!isTranslating) {
                speechManager?.pause()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isTranslating,
            dismissOnClickOutside = !isTranslating
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Compact & Professional Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF8E2DE2).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.6f)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tradução Trilateral",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (currentTranslation != null) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f),
                                    border = BorderStroke(0.6.dp, if (currentTranslation != null) Color(0xFF10B981) else Color(0xFF60A5FA))
                                ) {
                                    Text(
                                        text = if (currentTranslation != null) "● Salvo Offline" else "Novo",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentTranslation != null) Color(0xFF6EE7B7) else Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pág. $pageNumber de ${book.totalPages} • ${book.title}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentTranslation != null) {
                            IconButton(
                                onClick = { showApiKeyInput = !showApiKeyInput },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-traduzir / Configurar",
                                    tint = if (showApiKeyInput) Color(0xFFC084FC) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { showConfirmDeleteDialog = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir Tradução",
                                    tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                speechManager?.pause()
                                onDismiss()
                            },
                            enabled = !isTranslating,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // If translation does not exist or user wants to re-translate, show the OpenAI Translate card
                if (currentTranslation == null || showApiKeyInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
                        border = BorderStroke(1.dp, Color(0xFF6B46C1).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚙️ Configuração da Tradução OpenAI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC084FC)
                                )
                                if (currentTranslation != null) {
                                    Text(
                                        text = "Fechar Config",
                                        fontSize = 10.5.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable { showApiKeyInput = false }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    OpenAiService.saveApiKey(context, it.trim())
                                },
                                label = { Text("Chave API OpenAI (sk-...)") },
                                placeholder = { Text("Cole sua chave sk-...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8E2DE2),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFFC084FC),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Model selector & Translate button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Modelo:", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                    listOf("gpt-4o-mini", "gpt-4o").forEach { m ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (selectedModel == m) Color(0xFF8E2DE2) else Color.White.copy(alpha = 0.1f),
                                            modifier = Modifier.clickable { selectedModel = m }
                                        ) {
                                            Text(
                                                text = if (m == "gpt-4o-mini") "4o-mini" else "4o",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (apiKey.isBlank()) {
                                            Toast.makeText(context, "Insira sua chave da API OpenAI", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val textToTranslate = pageOriginalText.ifBlank { book.title }
                                        isTranslating = true
                                        translationError = null

                                        scope.launch {
                                            val res = OpenAiService.translatePageToTrilateralLanguages(
                                                apiKey = apiKey.trim(),
                                                model = selectedModel,
                                                bookTitle = book.title,
                                                pageNumber = pageNumber,
                                                pageText = textToTranslate
                                            )
                                            isTranslating = false
                                            if (res.isSuccess) {
                                                val translation = res.getOrThrow().copy(bookId = book.id)
                                                onSaveTranslation(translation)
                                                showApiKeyInput = false
                                                Toast.makeText(context, "Tradução concluída e salva offline!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                translationError = res.exceptionOrNull()?.message ?: "Erro ao traduzir"
                                            }
                                        }
                                    },
                                    enabled = !isTranslating && apiKey.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (isTranslating) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Traduzindo...", fontSize = 10.sp, color = Color.White)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (currentTranslation == null) "Traduzir (EN, FR, ES)" else "Atualizar Tradução", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (translationError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Erro: $translationError",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF87171)
                                )
                            }
                        }
                    }
                }

                // If translation exists, show unified compact single-row controls
                if (currentTranslation != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Language Selector Pills: EN, FR, ES, PT, Comparar, Exportar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            listOf(
                                ReadingLanguage.ENGLISH,
                                ReadingLanguage.FRENCH,
                                ReadingLanguage.SPANISH,
                                ReadingLanguage.PORTUGUESE
                            ).forEach { lang ->
                                val isSelected = selectedTab == MultiTranslationDialogTab.READING && activeReadingLang == lang
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF8E2DE2) else Color.White.copy(alpha = 0.08f),
                                    border = if (isSelected) BorderStroke(0.8.dp, Color(0xFFC084FC)) else null,
                                    modifier = Modifier.clickable {
                                        selectedTab = MultiTranslationDialogTab.READING
                                        activeReadingLang = lang
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = lang.flag, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = lang.shortLabel,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Comparative Mode Tab Pill
                            val isComparative = selectedTab == MultiTranslationDialogTab.COMPARATIVE
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isComparative) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.08f),
                                border = if (isComparative) BorderStroke(0.8.dp, Color(0xFF93C5FD)) else null,
                                modifier = Modifier.clickable {
                                    selectedTab = if (isComparative) MultiTranslationDialogTab.READING else MultiTranslationDialogTab.COMPARATIVE
                                }
                            ) {
                                Text(
                                    text = "⚖️ Comparar",
                                    fontSize = 10.sp,
                                    fontWeight = if (isComparative) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            // Export Tab Pill
                            val isExport = selectedTab == MultiTranslationDialogTab.EXPORT
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isExport) Color(0xFF10B981) else Color.White.copy(alpha = 0.08f),
                                border = if (isExport) BorderStroke(0.8.dp, Color(0xFF6EE7B7)) else null,
                                modifier = Modifier.clickable {
                                    selectedTab = if (isExport) MultiTranslationDialogTab.READING else MultiTranslationDialogTab.EXPORT
                                }
                            ) {
                                Text(
                                    text = "📋 Exportar",
                                    fontSize = 10.sp,
                                    fontWeight = if (isExport) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Quick Actions on current language (Apply to Reader, TTS, Copy)
                        if (selectedTab == MultiTranslationDialogTab.READING) {
                            val displayText = when (activeReadingLang) {
                                ReadingLanguage.PORTUGUESE -> if (currentTranslation.portugueseText.isNotBlank()) currentTranslation.portugueseText else currentTranslation.originalText
                                ReadingLanguage.ENGLISH -> currentTranslation.englishText
                                ReadingLanguage.FRENCH -> currentTranslation.frenchText
                                ReadingLanguage.SPANISH -> currentTranslation.spanishText
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                // Apply to Reader button (compact pill)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    border = BorderStroke(0.7.dp, Color(0xFF10B981)),
                                    modifier = Modifier.clickable {
                                        onApplyReadingLanguageToReader(activeReadingLang)
                                        Toast.makeText(context, "Leitor configurado para ${activeReadingLang.label}!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                ) {
                                    Text(
                                        text = "▶ Ler",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD1FAE5),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                // Audio TTS button (compact 26dp)
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.10f),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable {
                                            speechManager?.speakTextInLanguage(displayText, activeReadingLang.code)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Ouvir",
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Copy text button (compact 26dp)
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.10f),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable {
                                            val clip = ClipData.newPlainText("Texto em ${activeReadingLang.label}", displayText)
                                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                            Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copiar",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    when (selectedTab) {
                        MultiTranslationDialogTab.READING -> {
                            val displayText = when (activeReadingLang) {
                                ReadingLanguage.PORTUGUESE -> if (currentTranslation.portugueseText.isNotBlank()) currentTranslation.portugueseText else currentTranslation.originalText
                                ReadingLanguage.ENGLISH -> currentTranslation.englishText
                                ReadingLanguage.FRENCH -> currentTranslation.frenchText
                                ReadingLanguage.SPANISH -> currentTranslation.spanishText
                            }

                            // Text Content Card with Justified Reader Typography
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                SelectionContainer {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp)
                                    ) {
                                        val paragraphs = displayText.split("\n\n").filter { it.isNotBlank() }
                                        itemsIndexed(paragraphs) { pIdx, para ->
                                            Text(
                                                text = para,
                                                fontSize = 15.sp,
                                                lineHeight = 22.sp,
                                                fontFamily = FontFamily.Serif,
                                                textAlign = TextAlign.Justify,
                                                color = Color(0xFFE2E8F0)
                                            )
                                            if (pIdx < paragraphs.lastIndex) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        MultiTranslationDialogTab.COMPARATIVE -> {
                            // Comparative Filter Pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ComparativePairFilter.values().forEach { pair ->
                                    val isSelected = comparativePair == pair
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF8E2DE2) else Color.White.copy(alpha = 0.08f),
                                        border = if (isSelected) BorderStroke(0.8.dp, Color(0xFFC084FC)) else null,
                                        modifier = Modifier.clickable { comparativePair = pair }
                                    ) {
                                        Text(
                                            text = when (pair) {
                                                ComparativePairFilter.PT_EN -> "🇧🇷 PT ⟷ 🇬🇧 EN"
                                                ComparativePairFilter.PT_FR -> "🇧🇷 PT ⟷ 🇫🇷 FR"
                                                ComparativePairFilter.PT_ES -> "🇧🇷 PT ⟷ 🇪🇸 ES"
                                                ComparativePairFilter.ALL_FOUR -> "🌟 Todos os 4"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Comparative Paragraph List
                            val paragraphs = currentTranslation.getParagraphs()

                            SelectionContainer {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(paragraphs) { idx, p ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
                                            border = BorderStroke(1.dp, Color(0xFF3B2D54)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // Paragraph number header
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "§ Parágrafo ${idx + 1} de ${paragraphs.size}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFA78BFA)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Original Portuguese
                                                ComparativeParagraphBox(
                                                    flag = "🇧🇷",
                                                    langName = "Português",
                                                    text = p.portuguese,
                                                    color = Color(0xFFFDE68A),
                                                    onSpeak = {
                                                        speechManager?.speakTextInLanguage(p.portuguese, "pt")
                                                    }
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                when (comparativePair) {
                                                    ComparativePairFilter.PT_EN -> {
                                                        ComparativeParagraphBox(
                                                            flag = "🇬🇧",
                                                            langName = "Inglês",
                                                            text = p.english,
                                                            color = Color(0xFF93C5FD),
                                                            onSpeak = {
                                                                speechManager?.speakTextInLanguage(p.english, "en")
                                                            }
                                                        )
                                                    }
                                                    ComparativePairFilter.PT_FR -> {
                                                        ComparativeParagraphBox(
                                                            flag = "🇫🇷",
                                                            langName = "Francês",
                                                            text = p.french,
                                                            color = Color(0xFFF472B6),
                                                            onSpeak = {
                                                                speechManager?.speakTextInLanguage(p.french, "fr")
                                                            }
                                                        )
                                                    }
                                                    ComparativePairFilter.PT_ES -> {
                                                        ComparativeParagraphBox(
                                                            flag = "🇪🇸",
                                                            langName = "Espanhol",
                                                            text = p.spanish,
                                                            color = Color(0xFF6EE7B7),
                                                            onSpeak = {
                                                                speechManager?.speakTextInLanguage(p.spanish, "es")
                                                            }
                                                        )
                                                    }
                                                    ComparativePairFilter.ALL_FOUR -> {
                                                        ComparativeParagraphBox(
                                                            flag = "🇬🇧",
                                                            langName = "Inglês",
                                                            text = p.english,
                                                            color = Color(0xFF93C5FD),
                                                            onSpeak = {
                                                                speechManager?.speakTextInLanguage(p.english, "en")
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        ComparativeParagraphBox(
                                                            flag = "🇫🇷",
                                                            langName = "Francês",
                                                            text = p.french,
                                                            color = Color(0xFFF472B6),
                                                            onSpeak = {
                                                                speechManager?.speakTextInLanguage(p.french, "fr")
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        ComparativeParagraphBox(
                                                            flag = "🇪🇸",
                                                            langName = "Espanhol",
                                                            text = p.spanish,
                                                            color = Color(0xFF6EE7B7),
                                                            onSpeak = {
                                                                speechManager?.speakTextInLanguage(p.spanish, "es")
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        MultiTranslationDialogTab.EXPORT -> {
                            val fullExportText = buildComparativeExportText(currentTranslation, book.title, pageNumber)

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                SelectionContainer {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = fullExportText,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFE2E8F0)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val clip = ClipData.newPlainText("Tradução Trilateral Pág $pageNumber", fullExportText)
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                    Toast.makeText(context, "Dossiê completo de tradução copiado!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Estudo Comparativo Completo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDeleteDialog && currentTranslation != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Excluir Tradução Offline?") },
            text = { Text("Deseja apagar a tradução trilateral desta página salva no dispositivo?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTranslation(currentTranslation)
                        showConfirmDeleteDialog = false
                        Toast.makeText(context, "Tradução excluída.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ComparativeParagraphBox(
    flag: String,
    langName: String,
    text: String,
    color: Color,
    onSpeak: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(0.7.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = flag, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = langName,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Ouvir $langName",
                    tint = color,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onSpeak() }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text.ifBlank { "—" },
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Justify,
                color = Color.White.copy(alpha = 0.94f)
            )
        }
    }
}

private fun buildComparativeExportText(
    translation: PageMultiTranslation,
    bookTitle: String,
    pageNumber: Int
): String {
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("TRADUÇÃO TRILATERAL & COMPARATIVO DE IDIOMAS")
    sb.appendLine("Obra: $bookTitle | Página: $pageNumber")
    sb.appendLine("Salvo no Telefone: Offline Ready")
    sb.appendLine("==========================================\n")

    val paragraphs = translation.getParagraphs()
    paragraphs.forEachIndexed { i, p ->
        sb.appendLine("[Parágrafo ${i + 1}]")
        sb.appendLine("🇧🇷 PORTUGUÊS:")
        sb.appendLine(p.portuguese)
        sb.appendLine("🇬🇧 ENGLISH:")
        sb.appendLine(p.english)
        sb.appendLine("🇫🇷 FRANÇAIS:")
        sb.appendLine(p.french)
        sb.appendLine("🇪🇸 ESPAÑOL:")
        sb.appendLine(p.spanish)
        sb.appendLine("------------------------------------------\n")
    }

    val effectivePt = if (translation.portugueseText.isNotBlank()) translation.portugueseText else translation.originalText
    sb.appendLine("=== TEXTO COMPLETO EM PORTUGUÊS ===")
    sb.appendLine(effectivePt)
    sb.appendLine("\n=== TEXTO COMPLETO EM INGLÊS ===")
    sb.appendLine(translation.englishText)
    sb.appendLine("\n=== TEXTO COMPLETO EM FRANCÊS ===")
    sb.appendLine(translation.frenchText)
    sb.appendLine("\n=== TEXTO COMPLETO EM ESPANHOL ===")
    sb.appendLine(translation.spanishText)

    return sb.toString()
}
