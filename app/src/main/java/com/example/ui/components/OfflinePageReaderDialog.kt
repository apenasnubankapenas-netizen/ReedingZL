package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GeminiNote
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PdfBook
import com.example.data.offline.ClozeTestItem
import com.example.data.offline.OfflineLexiconTerm
import com.example.data.offline.OfflinePageAnalysis
import com.example.data.offline.OfflineReadingEngine
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.util.TextFormatUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OfflinePageReaderDialog(
    book: PdfBook,
    pageNumber: Int,
    pageText: String,
    rawSentences: List<String>,
    speechManager: PdfSpeechManager?,
    isSpeaking: Boolean,
    pageNotes: List<GeminiNote>,
    pageDictionaryTerms: List<PageDictionaryTerm>,
    onSaveNote: (term: String, noteText: String) -> Unit,
    onDeleteNote: (GeminiNote) -> Unit,
    onSaveDictionaryTerm: (PageDictionaryTerm) -> Unit,
    onDeleteDictionaryTerm: (PageDictionaryTerm) -> Unit,
    onDismiss: () -> Unit,
    onSaveDictionaryTerms: ((List<PageDictionaryTerm>) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // Análise offline imediata da página via OfflineReadingEngine
    val analysis = remember(book.id, pageNumber, pageText) {
        OfflineReadingEngine.analyzePage(pageText, rawSentences)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "📊 Métricas & Resumo",
        "🎯 Cloze (Fixação)",
        "📚 Dicionário Offline",
        "📝 Notas da Pág",
        "🎙️ Leitor TTS"
    )

    // Estado da Colheita Automática com OpenAI para o Dicionário Offline
    var isHarvestingTerms by remember { mutableStateOf(false) }
    var harvestStatusMessage by remember { mutableStateOf<String?>(null) }
    var autoHarvestTerms by remember { mutableStateOf(true) }
    var showOpenAiKeyDialog by remember { mutableStateOf(false) }
    var openAiApiKey by remember { mutableStateOf(OpenAiService.getApiKey(context)) }
    val scannedPagesSet = remember { mutableSetOf<String>() }

    // Auto-colheita automática com a API da OpenAI ao ler qualquer página
    LaunchedEffect(book.id, pageNumber, pageText, autoHarvestTerms) {
        if (!autoHarvestTerms) return@LaunchedEffect
        val pageKey = "${book.id}_$pageNumber"
        if (scannedPagesSet.contains(pageKey)) return@LaunchedEffect
        val cleanKey = OpenAiService.getApiKey(context).trim()
        if (cleanKey.isBlank()) return@LaunchedEffect
        if (pageText.isBlank() || pageText.length < 25) return@LaunchedEffect

        // Se a página atual já possui abundância de termos salvos (15+), não repete desnecessariamente
        val termsForThisPage = pageDictionaryTerms.filter { it.bookId == book.id && it.pageNumber == pageNumber }
        if (termsForThisPage.size >= 15) {
            scannedPagesSet.add(pageKey)
            return@LaunchedEffect
        }

        isHarvestingTerms = true
        harvestStatusMessage = "⚡ Vasculhando novos termos nesta página..."
        try {
            val result = OpenAiService.extractUniversalDictionaryTerms(
                apiKey = cleanKey,
                bookId = book.id,
                pageNumber = pageNumber,
                bookTitle = book.title,
                pageText = pageText
            )
            isHarvestingTerms = false
            scannedPagesSet.add(pageKey)

            result.onSuccess { extracted ->
                val existing = pageDictionaryTerms.map { it.term.trim().lowercase() }.toSet()
                val newTerms = extracted
                    .distinctBy { it.term.trim().lowercase() }
                    .filter { it.term.trim().lowercase() !in existing && it.term.isNotBlank() }
                if (newTerms.isNotEmpty()) {
                    if (onSaveDictionaryTerms != null) {
                        onSaveDictionaryTerms(newTerms)
                    } else {
                        newTerms.forEach { onSaveDictionaryTerm(it) }
                    }
                    harvestStatusMessage = "✨ ${newTerms.size} novos termos catalogados no dicionário offline!"
                } else {
                    harvestStatusMessage = "Termos da página já catalogados no glossário."
                }
            }.onFailure {
                // Modo offline: falhas de conexão/chave não atrapalham a leitura
                harvestStatusMessage = null
            }
        } catch (e: Exception) {
            isHarvestingTerms = false
            harvestStatusMessage = null
        }
    }

    val onTriggerManualHarvest: () -> Unit = {
        val cleanKey = OpenAiService.getApiKey(context).trim()
        if (pageText.isBlank()) {
            Toast.makeText(context, "A página não possui texto suficiente para análise.", Toast.LENGTH_SHORT).show()
        } else {
            coroutineScope.launch {
                isHarvestingTerms = true
                harvestStatusMessage = "⚡ Extraindo termos e notas de rodapé..."

                // 1. Extração offline imediata (100% nativa)
                val localTerms = OfflineReadingEngine.extractLocalTerms(book.id, pageNumber, book.title, pageText)
                val existing = pageDictionaryTerms.map { it.term.trim().lowercase() }.toSet()
                val newLocal = localTerms.filter { it.term.trim().lowercase() !in existing && it.term.isNotBlank() }
                if (newLocal.isNotEmpty()) {
                    if (onSaveDictionaryTerms != null) {
                        onSaveDictionaryTerms(newLocal)
                    } else {
                        newLocal.forEach { onSaveDictionaryTerm(it) }
                    }
                    harvestStatusMessage = "✨ ${newLocal.size} termos extraídos offline no glossário!"
                }

                if (cleanKey.isBlank()) {
                    isHarvestingTerms = false
                    if (newLocal.isNotEmpty()) {
                        Toast.makeText(context, "✨ ${newLocal.size} novos termos salvos no Dicionário Offline!", Toast.LENGTH_SHORT).show()
                    } else {
                        showOpenAiKeyDialog = true
                        Toast.makeText(context, "Termos já catalogados. Insira sua OpenAI API Key para colheita avançada com IA.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                harvestStatusMessage = "🤖 Aprofundando termos com OpenAI na página $pageNumber..."
                try {
                    val result = OpenAiService.extractUniversalDictionaryTerms(
                        apiKey = cleanKey,
                        bookId = book.id,
                        pageNumber = pageNumber,
                        bookTitle = book.title,
                        pageText = pageText
                    )
                    isHarvestingTerms = false
                    result.onSuccess { extracted ->
                        val curExisting = (pageDictionaryTerms + newLocal).map { it.term.trim().lowercase() }.toSet()
                        val newTerms = extracted
                            .distinctBy { it.term.trim().lowercase() }
                            .filter { it.term.trim().lowercase() !in curExisting && it.term.isNotBlank() }
                        if (newTerms.isNotEmpty()) {
                            if (onSaveDictionaryTerms != null) {
                                onSaveDictionaryTerms(newTerms)
                            } else {
                                newTerms.forEach { onSaveDictionaryTerm(it) }
                            }
                            harvestStatusMessage = "✨ ${newLocal.size + newTerms.size} novos termos colhidos e salvos offline!"
                            Toast.makeText(context, "✨ ${newLocal.size + newTerms.size} novos termos salvos no Dicionário Offline!", Toast.LENGTH_SHORT).show()
                        } else {
                            harvestStatusMessage = "Termos desta página já estão no dicionário (sem duplicatas)."
                            Toast.makeText(context, "Os termos desta página já constam no dicionário.", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { err ->
                        harvestStatusMessage = if (newLocal.isNotEmpty()) "✨ ${newLocal.size} termos salvos offline (OpenAI: ${err.message})" else "Falha: ${err.message}"
                        Toast.makeText(context, "OpenAI: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isHarvestingTerms = false
                    harvestStatusMessage = if (newLocal.isNotEmpty()) "✨ ${newLocal.size} termos salvos offline." else "Erro ao conectar com OpenAI."
                }
            }
        }
    }

    // Estado do Cloze Test (respostas do usuário)
    val clozeAnswers = remember { mutableStateMapOf<String, String>() }
    var clozeScore by remember { mutableIntStateOf(0) }

    // Estado do Dicionário
    var lexiconSearchQuery by remember { mutableStateOf("") }
    var showAddTermForm by remember { mutableStateOf(false) }
    var newTermWord by remember { mutableStateOf("") }
    var newTermDefinition by remember { mutableStateOf("") }

    // Estado de Novas Notas
    var newNoteText by remember { mutableStateOf("") }

    // TTS Speed local
    var currentSpeed by remember { mutableStateOf(1.0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0D0B18),
            border = BorderStroke(1.dp, Color(0xFF3B2F52))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF130E26),
                                Color(0xFF0A0714),
                                Color(0xFF06040C)
                            )
                        )
                    )
            ) {
                // =============================================================
                // 1. CABEÇALHO DA CENTRAL OFFLINE
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "100% OFFLINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                        }

                        if (isHarvestingTerms) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, Color(0xFFA78BFA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.5.dp,
                                        color = Color(0xFFFBBF24)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Colhendo OpenAI...",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDDD6FE)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Pág. $pageNumber • ${book.title}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Inteligência Local • Zero Consumo de Dados",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

                // =============================================================
                // 2. ABAS DE NAVEGAÇÃO
                // =============================================================
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFA78BFA),
                    edgePadding = 10.dp,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                height = 2.5.dp,
                                color = Color(0xFFA78BFA)
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                selectedTabIndex = index
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFDDD6FE) else Color.White.copy(alpha = 0.55f)
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

                // =============================================================
                // 3. CONTEÚDO DA ABA SELECIONADA
                // =============================================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> MetricsAndSummaryTab(analysis, speechManager, context)
                        1 -> ClozeTestTab(
                            clozeTests = analysis.clozeTests,
                            userAnswers = clozeAnswers,
                            onAnswer = { testId, option, isCorrect ->
                                clozeAnswers[testId] = option
                                if (isCorrect) {
                                    clozeScore++
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                }
                            }
                        )
                        2 -> OfflineLexiconTab(
                            matchedTerms = analysis.matchedLexiconTerms,
                            pageDictionaryTerms = pageDictionaryTerms,
                            searchQuery = lexiconSearchQuery,
                            onSearchQueryChange = { lexiconSearchQuery = it },
                            showAddForm = showAddTermForm,
                            onToggleAddForm = { showAddTermForm = !showAddTermForm },
                            newWord = newTermWord,
                            onWordChange = { newTermWord = it },
                            newDef = newTermDefinition,
                            onDefChange = { newTermDefinition = it },
                            onSaveNewTerm = {
                                if (newTermWord.isNotBlank() && newTermDefinition.isNotBlank()) {
                                    val wordTrim = newTermWord.trim()
                                    if (pageDictionaryTerms.any { it.term.equals(wordTrim, ignoreCase = true) }) {
                                        Toast.makeText(context, "O termo '$wordTrim' já existe no dicionário!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val newTerm = PageDictionaryTerm(
                                            bookId = book.id,
                                            pageNumber = pageNumber,
                                            bookTitle = book.title,
                                            term = wordTrim,
                                            originalDefinition = newTermDefinition.trim()
                                        )
                                        onSaveDictionaryTerm(newTerm)
                                        newTermWord = ""
                                        newTermDefinition = ""
                                        showAddTermForm = false
                                        Toast.makeText(context, "Termo salvo no glossário offline!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDeleteTerm = onDeleteDictionaryTerm,
                            speechManager = speechManager,
                            isHarvestingWithOpenAi = isHarvestingTerms,
                            harvestStatusMessage = harvestStatusMessage,
                            autoHarvestEnabled = autoHarvestTerms,
                            onToggleAutoHarvest = { autoHarvestTerms = !autoHarvestTerms },
                            onTriggerHarvest = onTriggerManualHarvest,
                            hasOpenAiKey = OpenAiService.getApiKey(context).isNotBlank(),
                            onOpenKeyConfig = { showOpenAiKeyDialog = true }
                        )
                        3 -> PageNotesTab(
                            pageNotes = pageNotes,
                            speechManager = speechManager,
                            newNoteText = newNoteText,
                            onNoteTextChange = { newNoteText = it },
                            onSaveNote = {
                                if (newNoteText.isNotBlank()) {
                                    onSaveNote("Página $pageNumber", newNoteText.trim())
                                    newNoteText = ""
                                    Toast.makeText(context, "Nota salva offline!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteNote = onDeleteNote
                        )
                        4 -> OfflineTtsTab(
                            rawSentences = rawSentences,
                            pageText = pageText,
                            speechManager = speechManager,
                            isSpeaking = isSpeaking,
                            currentSpeed = currentSpeed,
                            onSpeedChange = { newSpeed ->
                                currentSpeed = newSpeed
                                speechManager?.setSpeed(newSpeed)
                            }
                        )
                    }
                }
            }
        }

        // Diálogo para Configurar a Chave da API da OpenAI
        if (showOpenAiKeyDialog) {
            var keyDraft by remember { mutableStateOf(openAiApiKey) }
            AlertDialog(
                onDismissRequest = { showOpenAiKeyDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chave da API OpenAI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Insira sua OpenAI API Key para vasculhar automaticamente mais termos em cada página lida e enriquecer seu dicionário offline:",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = keyDraft,
                            onValueChange = { keyDraft = it },
                            placeholder = { Text("sk-...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFA78BFA),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            OpenAiService.saveApiKey(context, keyDraft.trim())
                            openAiApiKey = keyDraft.trim()
                            showOpenAiKeyDialog = false
                            Toast.makeText(context, "Chave OpenAI salva!", Toast.LENGTH_SHORT).show()
                            if (keyDraft.isNotBlank()) {
                                onTriggerManualHarvest()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Salvar Chave", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showOpenAiKeyDialog = false }) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF1A162B)
            )
        }
    }
}

// =============================================================================
// ABA 1: MÉTRICAS COGNITIVAS & RESUMO EXTRATIVO LOCAL
// =============================================================================
@Composable
private fun MetricsAndSummaryTab(
    analysis: OfflinePageAnalysis,
    speechManager: PdfSpeechManager?,
    context: Context
) {
    val metrics = analysis.metrics
    val summary = analysis.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Grid de Métricas 4x
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Tempo
                MetricCard(
                    title = "Tempo de Leitura",
                    value = metrics.formattedReadingTime,
                    subtitle = "Média ~185 WPM",
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFF60A5FA),
                    modifier = Modifier.weight(1f)
                )
                // Card 2: Palavras
                MetricCard(
                    title = "Extensão",
                    value = "${metrics.wordCount} pal.",
                    subtitle = "${metrics.charCount} caracteres",
                    icon = Icons.Default.MenuBook,
                    accentColor = Color(0xFFA78BFA),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 3: Nível de Complexidade Flesch
                MetricCard(
                    title = "Índice Flesch",
                    value = "${metrics.fleschScore}",
                    subtitle = metrics.difficultyLevel,
                    icon = Icons.Default.Psychology,
                    accentColor = Color(metrics.difficultyColorHex),
                    modifier = Modifier.weight(1f)
                )
                // Card 4: Frases
                MetricCard(
                    title = "Densidade Frasal",
                    value = "${metrics.sentenceCount} frases",
                    subtitle = "${(metrics.wordCount / metrics.sentenceCount.coerceAtLeast(1))} pal/frase",
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Dica Pedagógica
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = metrics.readingTip,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Conceitos-Chave da Página
        if (summary.keyConcepts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "CONCEITOS-CHAVE DA PÁGINA (TOP FREQUÊNCIA)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        summary.keyConcepts.forEach { concept ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.18f),
                                border = BorderStroke(0.6.dp, Color(0xFFA78BFA).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "• $concept",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDDD6FE),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Frase de Ouro (Golden Sentence)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1408),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FRASE DE OURO DA PÁGINA",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    speechManager?.speakTextImmediately(summary.goldenSentence)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Ouvir",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Frase de Ouro", summary.goldenSentence)
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                    Toast.makeText(context, "Frase copiada!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${summary.goldenSentence}\"",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFEF3C7),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Resumo Extrativo Local (Top 3 Frases)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131024),
                border = BorderStroke(0.8.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📑 RESUMO EXTRATIVO LOCAL (TOP 3 FRASES)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    summary.topSentences.forEachIndexed { i, sentence ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${i + 1}.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sentence,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                        if (i < summary.topSentences.size - 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161326),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =============================================================================
// ABA 2: CLOZE TEST (PREENCHIMENTO DE LACUNAS PARA FIXAÇÃO OFFLINE)
// =============================================================================
@Composable
private fun ClozeTestTab(
    clozeTests: List<ClozeTestItem>,
    userAnswers: Map<String, String>,
    onAnswer: (testId: String, selectedOption: String, isCorrect: Boolean) -> Unit
) {
    if (clozeTests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Página com texto curto para geração de lacunas.\nAvance para páginas mais ricas para resolver o Cloze Test.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                border = BorderStroke(0.8.dp, Color(0xFFA78BFA).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cloze Test: Preencha as lacunas com o conceito original lido nesta página para fixação de memória.",
                        fontSize = 11.sp,
                        color = Color(0xFFDDD6FE)
                    )
                }
            }
        }

        items(clozeTests, key = { it.id }) { item ->
            val userAnswer = userAnswers[item.id]
            val isAnswered = userAnswer != null
            val isCorrect = userAnswer?.equals(item.targetWord, ignoreCase = true) == true

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF141124),
                border = BorderStroke(
                    1.dp,
                    when {
                        isAnswered && isCorrect -> Color(0xFF10B981).copy(alpha = 0.7f)
                        isAnswered && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.7f)
                        else -> Color(0xFF2C2440)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = item.maskedSentence,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Opções
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.options.forEach { option ->
                            val isThisSelected = userAnswer == option
                            val isThisRealAnswer = option.equals(item.targetWord, ignoreCase = true)

                            val btnBg by animateColorAsState(
                                targetValue = when {
                                    isAnswered && isThisRealAnswer -> Color(0xFF064E3B)
                                    isAnswered && isThisSelected && !isThisRealAnswer -> Color(0xFF7F1D1D)
                                    else -> Color.White.copy(alpha = 0.06f)
                                },
                                label = "clozeBtnBg"
                            )

                            val btnBorder by animateColorAsState(
                                targetValue = when {
                                    isAnswered && isThisRealAnswer -> Color(0xFF10B981)
                                    isAnswered && isThisSelected && !isThisRealAnswer -> Color(0xFFEF4444)
                                    else -> Color.White.copy(alpha = 0.12f)
                                },
                                label = "clozeBtnBorder"
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = btnBg,
                                border = BorderStroke(1.dp, btnBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAnswered) {
                                        onAnswer(item.id, option, option.equals(item.targetWord, ignoreCase = true))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isThisSelected || (isAnswered && isThisRealAnswer)) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAnswered && isThisRealAnswer) Color(0xFFD1FAE5) else Color.White
                                    )
                                    if (isAnswered && isThisRealAnswer) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Correto",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isAnswered) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isCorrect) "✓ Exato! ${item.explanation}" else "✗ Incorreto. ${item.explanation}",
                                fontSize = 10.5.sp,
                                color = if (isCorrect) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// =============================================================================
// ABA 3: DICIONÁRIO E GLOSSÁRIO OFFLINE
// =============================================================================
@Composable
private fun OfflineLexiconTab(
    matchedTerms: List<OfflineLexiconTerm>,
    pageDictionaryTerms: List<PageDictionaryTerm>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showAddForm: Boolean,
    onToggleAddForm: () -> Unit,
    newWord: String,
    onWordChange: (String) -> Unit,
    newDef: String,
    onDefChange: (String) -> Unit,
    onSaveNewTerm: () -> Unit,
    onDeleteTerm: (PageDictionaryTerm) -> Unit,
    speechManager: PdfSpeechManager?,
    isHarvestingWithOpenAi: Boolean = false,
    harvestStatusMessage: String? = null,
    autoHarvestEnabled: Boolean = true,
    onToggleAutoHarvest: () -> Unit = {},
    onTriggerHarvest: () -> Unit = {},
    hasOpenAiKey: Boolean = false,
    onOpenKeyConfig: () -> Unit = {}
) {
    // Filtro unificado de termos (tanto do catálogo nativo quanto dos salvos pelo usuário)
    val filteredCatalog = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            OfflineReadingEngine.BUILT_IN_LEXICON
        } else {
            OfflineReadingEngine.BUILT_IN_LEXICON.filter {
                it.term.contains(searchQuery, ignoreCase = true) ||
                        it.definition.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Filtro e deduplicação de termos catalogados (visíveis globalmente em qualquer livro ou página)
    val distinctUserTerms = remember(pageDictionaryTerms, searchQuery) {
        val base = pageDictionaryTerms.distinctBy { TextFormatUtils.cleanTermTitle(it.term).lowercase() }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter {
                val cleanT = TextFormatUtils.cleanTermTitle(it.term)
                cleanT.contains(searchQuery, ignoreCase = true) ||
                        it.originalDefinition.contains(searchQuery, ignoreCase = true) ||
                        it.spanishTerm.contains(searchQuery, ignoreCase = true) ||
                        it.frenchTerm.contains(searchQuery, ignoreCase = true) ||
                        it.englishTerm.contains(searchQuery, ignoreCase = true) ||
                        it.bookTitle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 🤖 CARD DE COLHEITA AUTOMÁTICA OPENAI (DICIONÁRIO OFFLINE)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1B1433),
                border = BorderStroke(1.dp, if (isHarvestingWithOpenAi) Color(0xFFFBBF24) else Color(0xFF6366F1).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Colheita de Termos com OpenAI",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Toggle Auto-Colheita ao ler
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (autoHarvestEnabled) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.7.dp, if (autoHarvestEnabled) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { onToggleAutoHarvest() }
                            ) {
                                Text(
                                    text = if (autoHarvestEnabled) "Auto: ATIVO" else "Auto: PAUSA",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoHarvestEnabled) Color(0xFFD1FAE5) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = onOpenKeyConfig,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "Chave OpenAI",
                                    tint = if (hasOpenAiKey) Color(0xFF34D399) else Color(0xFFF87171),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Vasculha termos e conceitos aprofundados na página atual usando a OpenAI e salva direto no dicionário offline deste livro.",
                        fontSize = 9.5.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 13.sp
                    )

                    if (harvestStatusMessage != null || isHarvestingWithOpenAi) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF281F47),
                            border = BorderStroke(0.6.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isHarvestingWithOpenAi) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = Color(0xFFFBBF24)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = harvestStatusMessage ?: "Vasculhando termos na página...",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFFDDD6FE),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onTriggerHarvest,
                            enabled = !isHarvestingWithOpenAi,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                disabledContainerColor = Color(0xFF3730A3)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHarvestingWithOpenAi) "Vasculhando..." else "Vasculhar Página Agora",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Barra de busca + Botão de adicionar termo
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar termo offline...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFA78BFA),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (showAddForm) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF8B5CF6).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, if (showAddForm) Color(0xFFEF4444) else Color(0xFFA78BFA)),
                    modifier = Modifier.clickable { onToggleAddForm() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Novo Termo",
                            tint = if (showAddForm) Color(0xFFFCA5A5) else Color(0xFFDDD6FE),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showAddForm) "Fechar" else "+ Termo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showAddForm) Color(0xFFFCA5A5) else Color(0xFFDDD6FE)
                        )
                    }
                }
            }
        }

        // Formulário para Salvar Novo Termo
        if (showAddForm) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1736),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ADICIONAR TERMO AO GLOSSÁRIO DESTA PÁGINA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newWord,
                            onValueChange = onWordChange,
                            placeholder = { Text("Palavra / Expressão", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newDef,
                            onValueChange = onDefChange,
                            placeholder = { Text("Significado / Explicação", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSaveNewTerm,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Salvar Termo Offline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Seção: Termos detectados especificamente nesta página
        if (matchedTerms.isNotEmpty()) {
            item {
                Text(
                    text = "🎯 TERMOS DETECTADOS NESTA PÁGINA (${matchedTerms.size})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8)
                )
            }

            items(matchedTerms, key = { "matched_${it.term}" }) { term ->
                LexiconCard(
                    term = term.term,
                    category = term.category,
                    definition = term.definition,
                    example = term.example,
                    speechManager = speechManager,
                    isDetectedOnPage = true
                )
            }
        }

        // Seção: Termos salvos e extraídos no dicionário offline (visíveis em todos os livros e páginas sem duplicatas)
        if (distinctUserTerms.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📚 SEU DICIONÁRIO OFFLINE (${distinctUserTerms.size} termos catalogados)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF34D399)
                    )
                    Text(
                        text = "Unificado para todos os livros",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            items(distinctUserTerms, key = { "user_term_${it.id}_${it.term}" }) { term ->
                val hasTranslations = term.spanishTerm.isNotBlank() || term.frenchTerm.isNotBlank() || term.englishTerm.isNotBlank()
                var showTranslations by remember(term.id) { mutableStateOf(false) }
                val cleanTitle = remember(term.term) { TextFormatUtils.cleanTermTitle(term.term) }
                val cleanDef = remember(term.originalDefinition) { TextFormatUtils.cleanTextSpacing(term.originalDefinition) }
                val cleanContext = remember(term.originalContext) { TextFormatUtils.cleanTextSpacing(term.originalContext) }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1E17),
                    border = BorderStroke(0.8.dp, if (hasTranslations) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        // Título do termo limpo e ações compactas (sem áreas de conhecimento deformando o card)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cleanTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD1FAE5),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                if (hasTranslations) {
                                    IconButton(
                                        onClick = { showTranslations = !showTranslations },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = "Traduções",
                                            tint = if (showTranslations) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteTerm(term) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Excluir",
                                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Referência compacta do Livro e Página
                        val cleanBook = remember(term.bookTitle) {
                            term.bookTitle.removeSuffix(".pdf").removeSuffix(".PDF")
                                .removeSuffix(".epub").removeSuffix(".EPUB")
                                .replace("_", " ")
                                .trim()
                        }
                        if (cleanBook.isNotBlank() || term.pageNumber > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                                border = BorderStroke(0.5.dp, Color(0xFF475569))
                            ) {
                                val shortTitle = cleanBook.take(22)
                                Text(
                                    text = if (shortTitle.isNotBlank()) "📖 $shortTitle • Pág. ${term.pageNumber}" else "Pág. ${term.pageNumber}",
                                    fontSize = 7.5.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (cleanContext.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "\"$cleanContext\"",
                                fontSize = 9.5.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color(0xFFA7F3D0).copy(alpha = 0.7f),
                                lineHeight = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        KaraokeDefinitionText(
                            targetId = "user_term_${term.id}_${cleanTitle.hashCode()}",
                            text = cleanDef,
                            speechManager = speechManager,
                            accentColor = Color(0xFF34D399),
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            showListenButton = true
                        )

                        // Painel de Traduções Multilíngues (ES, FR, EN) salvas 100% offline
                        if (showTranslations && hasTranslations) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF07140E),
                                border = BorderStroke(0.6.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    if (term.spanishTerm.isNotBlank()) {
                                        Text(
                                            text = "🇪🇸 ${term.spanishTerm}${if (term.spanishDefinition.isNotBlank()) ": ${term.spanishDefinition}" else ""}",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFFFDE68A),
                                            lineHeight = 13.sp
                                        )
                                    }
                                    if (term.frenchTerm.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🇫🇷 ${term.frenchTerm}${if (term.frenchDefinition.isNotBlank()) ": ${term.frenchDefinition}" else ""}",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFFBAE6FD),
                                            lineHeight = 13.sp
                                        )
                                    }
                                    if (term.englishTerm.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🇬🇧 ${term.englishTerm}${if (term.englishDefinition.isNotBlank()) ": ${term.englishDefinition}" else ""}",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFFDDD6FE),
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Seção: Catálogo Geral Offline
        item {
            Text(
                text = "📚 CATÁLOGO ENCICLOPÉDICO OFFLINE (${filteredCatalog.size})",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFA78BFA)
            )
        }

        items(filteredCatalog, key = { "catalog_${it.term}" }) { term ->
            LexiconCard(
                term = term.term,
                category = term.category,
                definition = term.definition,
                example = term.example,
                speechManager = speechManager,
                isDetectedOnPage = false
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun LexiconCard(
    term: String,
    category: String,
    definition: String,
    example: String,
    speechManager: PdfSpeechManager?,
    isDetectedOnPage: Boolean
) {
    val cleanTerm = remember(term) { TextFormatUtils.cleanTermTitle(term) }
    val cleanDef = remember(definition) { TextFormatUtils.cleanTextSpacing(definition) }
    val cleanEx = remember(example) { TextFormatUtils.cleanTextSpacing(example) }
    val targetId = remember(term, isDetectedOnPage) { "lexicon_${if (isDetectedOnPage) "det" else "cat"}_${cleanTerm.hashCode()}" }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDetectedOnPage) Color(0xFF0F172A) else Color(0xFF141124),
        border = BorderStroke(
            0.8.dp,
            if (isDetectedOnPage) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cleanTerm,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDetectedOnPage) Color(0xFF7DD3FC) else Color(0xFFE2E8F0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (category.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = (if (isDetectedOnPage) Color(0xFF0284C7) else Color(0xFF8B5CF6)).copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, if (isDetectedOnPage) Color(0xFF38BDF8) else Color(0xFFA78BFA))
                    ) {
                        Text(
                            text = category.take(18),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDetectedOnPage) Color(0xFFBAE6FD) else Color(0xFFDDD6FE),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            KaraokeDefinitionText(
                targetId = targetId,
                text = cleanDef,
                speechManager = speechManager,
                accentColor = if (isDetectedOnPage) Color(0xFF38BDF8) else Color(0xFFA78BFA),
                fontSize = 11.5.sp,
                lineHeight = 15.5.sp,
                showListenButton = true
            )

            if (cleanEx.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ex: \"$cleanEx\"",
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// =============================================================================
// ABA 4: CADERNO DE NOTAS DA PÁGINA (PERSISTIDO EM ROOM)
// =============================================================================
@Composable
private fun PageNotesTab(
    pageNotes: List<GeminiNote>,
    speechManager: PdfSpeechManager? = null,
    newNoteText: String,
    onNoteTextChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onDeleteNote: (GeminiNote) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Campo para Criar Nova Nota
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF16122C),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NOVA ANOTAÇÃO OFFLINE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDDD6FE)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = onNoteTextChange,
                        placeholder = { Text("Escreva seus pensamentos, teses ou resumos desta página...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onSaveNote,
                        enabled = newNoteText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6),
                            disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gravar Nota no Banco Local", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Lista de Notas da Página
        if (pageNotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma anotação nesta página ainda.\nDigite acima para registrar suas reflexões.",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "SUAS ANOTAÇÕES GRAVADAS (${pageNotes.size})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFA78BFA)
                )
            }

            items(pageNotes, key = { it.id }) { note ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF141124),
                    border = BorderStroke(0.8.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateFormat.format(Date(note.createdAt)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA78BFA)
                            )
                            IconButton(
                                onClick = { onDeleteNote(note) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Excluir Nota",
                                    tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val noteContent = note.geminiResponse.ifBlank { note.fullContext.ifBlank { note.termOrSentence } }
                        KaraokeDefinitionText(
                            targetId = "offline_note_${note.id}",
                            text = "${note.termOrSentence}: $noteContent",
                            speechManager = speechManager,
                            accentColor = Color(0xFFA78BFA),
                            textColor = Color.White.copy(alpha = 0.92f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            highlightBgColor = Color(0xFFFBBF24),
                            highlightTextColor = Color(0xFF000000),
                            showListenButton = true
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// =============================================================================
// ABA 5: LEITOR DE VOZ OFFLINE (MOTOR TTS NATIVO DO ANDROID)
// =============================================================================
@Composable
private fun OfflineTtsTab(
    rawSentences: List<String>,
    pageText: String,
    speechManager: PdfSpeechManager?,
    isSpeaking: Boolean,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF16122C),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MOTOR TTS ANDROID NATIVO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFDDD6FE)
                            )
                            Text(
                                text = "Funciona no avião, metrô e sem internet",
                                fontSize = 9.5.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSpeaking) Color(0xFF10B981).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, if (isSpeaking) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = if (isSpeaking) "● LENDO..." else "○ PARADO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpeaking) Color(0xFF34D399) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botão Principal Ouvir / Parar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isSpeaking) {
                                    speechManager?.pause()
                                } else {
                                    speechManager?.speakTextImmediately(pageText)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpeaking) Color(0xFFEF4444) else Color(0xFF8B5CF6)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSpeaking) "Pausar Leitura" else "Ouvir Página Inteira",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seletor de Velocidade Rápida
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Velocidade da Voz:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            speeds.forEach { sp ->
                                val isSelected = kotlin.math.abs(sp - currentSpeed) < 0.05f
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.08f),
                                    border = if (isSelected) BorderStroke(1.dp, Color(0xFFA78BFA)) else null,
                                    modifier = Modifier.clickable { onSpeedChange(sp) }
                                ) {
                                    Text(
                                        text = "${sp}x",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ouvir Sentença Específica
        item {
            Text(
                text = "OUVIR SENTENÇA INDIVIDUAL (${rawSentences.size} FRASES)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFA78BFA)
            )
        }

        items(rawSentences.filter { it.isNotBlank() }.take(15)) { sentence ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF141124),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { speechManager?.speakTextImmediately(sentence) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = sentence,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Ouvir",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
