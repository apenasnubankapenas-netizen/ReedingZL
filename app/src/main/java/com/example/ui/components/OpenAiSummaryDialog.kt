package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OpenAiPageSummary
import com.example.data.model.PdfBook
import com.example.data.openai.OpenAiAudioPlayer
import com.example.data.openai.OpenAiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OpenAiSummaryDialog(
    book: PdfBook,
    pageNumber: Int,
    pageText: String,
    existingSummary: OpenAiPageSummary?,
    readingTheme: String = book.readingTheme ?: "MONOCHROME_BW",
    onSaveSummary: (OpenAiPageSummary) -> Unit,
    onDeleteSummary: (OpenAiPageSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = com.example.ui.theme.ReadingThemeManager.getPalette(readingTheme, book.category, book.categoryColor)

    var apiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("gpt-4o-mini") }
    var selectedVoice by remember { mutableStateOf(existingSummary?.voiceName ?: "alloy") }
    var shouldGenerateVoice by remember { mutableStateOf(true) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConfigExpanded by remember { mutableStateOf(existingSummary == null) }

    val audioPlayer = remember { OpenAiAudioPlayer() }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentPosMs by audioPlayer.currentPositionMs.collectAsState()
    val durationMs by audioPlayer.durationMs.collectAsState()
    val activeAudioPath by audioPlayer.activeFilePath.collectAsState()

    val healedPageText = remember(pageText) { com.example.util.TextFormatUtils.healBrokenWordsAndHyphens(pageText) }
    val verbsOnPage = remember(healedPageText, book.id, pageNumber) {
        com.example.data.offline.OfflineVerbPhrasalVerbEngine.extractVerbsAndPhrasalVerbs(
            bookId = book.id,
            pageNumber = pageNumber,
            bookTitle = book.title,
            sourceText = healedPageText.ifBlank { book.title },
            languageCode = "en"
        )
    }
    var selectedVerbForDetail by remember { mutableStateOf<com.example.data.model.VerbPhrasalVerbStudyTerm?>(null) }

    // Auto-load audio if summary already has an audio file
    LaunchedEffect(existingSummary) {
        if (existingSummary?.audioFilePath != null && activeAudioPath != existingSummary.audioFilePath) {
            val file = java.io.File(existingSummary.audioFilePath)
            if (file.exists() && file.length() > 0) {
                audioPlayer.loadAndPlay(existingSummary.audioFilePath, autoPlay = false)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    fun startAnalysisAndGeneration() {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            errorMessage = "Por favor, insira sua chave da API de IA (sk-...)."
            return
        }

        if (pageText.isBlank()) {
            errorMessage = "Nenhum texto encontrado nesta página para análise."
            return
        }

        errorMessage = null
        isProcessing = true
        statusMessage = "1/2 • Analisando profundamente temas da página com ${selectedModel.uppercase()}..."

        // Save API key for future use
        OpenAiService.saveApiKey(context, cleanKey)

        scope.launch {
            try {
                val summaryResult = OpenAiService.generatePageSummary(
                    apiKey = cleanKey,
                    model = selectedModel,
                    bookTitle = book.title,
                    pageNumber = pageNumber,
                    pageText = pageText
                )

                if (summaryResult.isFailure) {
                    val ex = summaryResult.exceptionOrNull()
                    errorMessage = ex?.message ?: "Falha ao analisar a página com IA."
                    isProcessing = false
                    return@launch
                }

                val summaryText = summaryResult.getOrNull() ?: ""
                var savedAudioPath: String? = null

                if (shouldGenerateVoice) {
                    statusMessage = "2/2 • Sintetizando voz neural (${selectedVoice.uppercase()}) e salvando no dispositivo..."
                    val audioResult = OpenAiService.generateSpeechAudio(
                        context = context,
                        apiKey = cleanKey,
                        textToSpeak = summaryText,
                        voice = selectedVoice,
                        bookId = book.id,
                        pageNumber = pageNumber
                    )

                    if (audioResult.isSuccess) {
                        val file = audioResult.getOrNull()
                        savedAudioPath = file?.absolutePath
                    } else {
                        // Log or soft warn, but still save text summary
                        val voiceErr = audioResult.exceptionOrNull()?.message
                        Toast.makeText(context, "Resumo salvo! Aviso de áudio: $voiceErr", Toast.LENGTH_LONG).show()
                    }
                }

                val newSummary = OpenAiPageSummary(
                    id = existingSummary?.id ?: 0,
                    bookId = book.id,
                    pageNumber = pageNumber,
                    bookTitle = book.title,
                    originalPageText = pageText,
                    summaryText = summaryText,
                    audioFilePath = savedAudioPath ?: existingSummary?.audioFilePath,
                    voiceName = selectedVoice,
                    modelUsed = selectedModel,
                    createdAt = System.currentTimeMillis()
                )

                onSaveSummary(newSummary)
                isProcessing = false
                isConfigExpanded = false

                if (savedAudioPath != null) {
                    audioPlayer.loadAndPlay(savedAudioPath, autoPlay = true)
                }

                Toast.makeText(context, "✨ Resumo e voz neural salvos com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                errorMessage = "Erro inesperado: ${e.message}"
                isProcessing = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // TOP BAR WITH RETURN BUTTON
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.surface,
                        titleContentColor = palette.textPrimary,
                        navigationIconContentColor = palette.textPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = palette.textPrimary
                            )
                        }
                    },
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            palette.accent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = palette.surface,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resumo & Voz IA",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                            Text(
                                text = "${book.title} • Página $pageNumber",
                                fontSize = 12.sp,
                                color = palette.textSecondary,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voltar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )

                // MAIN SCROLLABLE CONTENT
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 1. AUDIO PLAYER SECTION (If audio is available)
                    if (existingSummary?.audioFilePath != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFF10A37F), Color(0xFF2A9D8F)))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF10A37F).copy(alpha = 0.2f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = Color(0xFF10A37F),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Narração Voz IA",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Voz: ${existingSummary.voiceName.uppercase()} • Salvo no aparelho",
                                                fontSize = 11.sp,
                                                color = Color(0xFF10A37F)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10A37F).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ÁUDIO LOCAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10A37F),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Timeline Slider
                                val safeDuration = if (durationMs > 0) durationMs.toFloat() else 1f
                                var sliderPos by remember { mutableFloatStateOf(0f) }
                                var isDragging by remember { mutableStateOf(false) }

                                val progressFraction = if (isDragging) sliderPos else (currentPosMs.toFloat() / safeDuration).coerceIn(0f, 1f)

                                Slider(
                                    value = progressFraction,
                                    onValueChange = {
                                        isDragging = true
                                        sliderPos = it
                                    },
                                    onValueChangeFinished = {
                                        val targetMs = (sliderPos * safeDuration).toInt()
                                        audioPlayer.seekTo(targetMs)
                                        isDragging = false
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF10A37F),
                                        activeTrackColor = Color(0xFF10A37F),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTimeMs(currentPosMs),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = formatTimeMs(durationMs),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Playback controls (Minimalistas: play 36dp, icones compactos)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            audioPlayer.seekTo((currentPosMs - 5000).coerceAtLeast(0))
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay5,
                                            contentDescription = "Voltar 5s",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10A37F),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable {
                                                if (activeAudioPath == null && existingSummary.audioFilePath != null) {
                                                    audioPlayer.loadAndPlay(existingSummary.audioFilePath, autoPlay = true)
                                                } else {
                                                    audioPlayer.togglePlayPause()
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    IconButton(
                                        onClick = {
                                            audioPlayer.seekTo((currentPosMs + 5000).coerceAtMost(durationMs))
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward5,
                                            contentDescription = "Avançar 5s",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Audio Playback Speed Selector (Minimalista)
                                val currentSpeed by audioPlayer.playbackSpeed.collectAsState()
                                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        speeds.forEach { sp ->
                                            val isSelected = (currentSpeed == sp)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) Color(0xFF10A37F) else Color.White.copy(alpha = 0.08f),
                                                modifier = Modifier.clickable { audioPlayer.setPlaybackSpeed(sp) }
                                            ) {
                                                Text(
                                                    text = "${sp}x",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. EXISTING SUMMARY DISPLAY (If summary already exists)
                    if (existingSummary != null && !isConfigExpanded) {
                        val cleanSummary = remember(existingSummary.summaryText) {
                            existingSummary.summaryText
                                .replace(Regex("\\*{1,3}"), "")
                                .replace(Regex("(?m)^#{1,6}\\s*"), "")
                                .trim()
                        }
                        val words = remember(cleanSummary) {
                            cleanSummary.split(Regex("\\s+")).filter { it.isNotBlank() }
                        }
                        val activeWordIdx = remember(currentPosMs, durationMs, isPlaying, words.size) {
                            if (isPlaying && durationMs > 0 && words.isNotEmpty()) {
                                val fraction = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                (fraction * words.size).toInt().coerceIn(0, words.size - 1)
                            } else {
                                -1
                            }
                        }
                        val annotatedSummary = remember(cleanSummary, activeWordIdx, isPlaying) {
                            buildAnnotatedString {
                                if (!isPlaying || activeWordIdx < 0 || words.isEmpty()) {
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFFE2E8F0),
                                            fontStyle = FontStyle.Italic
                                        )
                                    ) {
                                        append(cleanSummary)
                                    }
                                } else {
                                    var currentOffset = 0
                                    words.forEachIndexed { idx, word ->
                                        val wordStart = cleanSummary.indexOf(word, currentOffset)
                                        if (wordStart >= 0) {
                                            if (wordStart > currentOffset) {
                                                withStyle(SpanStyle(color = Color(0xFFE2E8F0), fontStyle = FontStyle.Italic)) {
                                                    append(cleanSummary.substring(currentOffset, wordStart))
                                                }
                                            }
                                            if (idx == activeWordIdx) {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color(0xFF00DC82),
                                                        background = Color(0xFF00DC82).copy(alpha = 0.25f),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontStyle = FontStyle.Italic
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            } else if (idx < activeWordIdx) {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color.White,
                                                        fontStyle = FontStyle.Italic
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            } else {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color(0xFFE2E8F0).copy(alpha = 0.70f),
                                                        fontStyle = FontStyle.Italic
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            }
                                            currentOffset = wordStart + word.length
                                        }
                                    }
                                    if (currentOffset < cleanSummary.length) {
                                        withStyle(SpanStyle(color = Color(0xFFE2E8F0), fontStyle = FontStyle.Italic)) {
                                            append(cleanSummary.substring(currentOffset))
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10A37F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Resumo",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }

                                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(existingSummary.createdAt))
                                    Text(
                                        text = dateStr,
                                        fontSize = 9.5.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // The Italic Justified Text Summary with Word Highlighting as Voice Speaks
                                SelectionContainer {
                                    Text(
                                        text = annotatedSummary,
                                        fontSize = 13.5.sp,
                                        lineHeight = 21.sp,
                                        textAlign = TextAlign.Justify,
                                        fontFamily = FontFamily.Default
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons (Minimalistas): copiar, enviar, refazer, excluir
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Resumo IA", cleanSummary)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Resumo copiado!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("copiar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Resumo: ${book.title} (Pág. $pageNumber)")
                                                putExtra(Intent.EXTRA_TEXT, "📚 ${book.title} • Pág. $pageNumber\n\n$cleanSummary")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar"))
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("enviar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10A37F).copy(alpha = 0.16f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF10A37F).copy(alpha = 0.45f)),
                                        modifier = Modifier.clickable { isConfigExpanded = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF00DC82))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("refazer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00DC82), maxLines = 1)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                        modifier = Modifier.clickable {
                                            onDeleteSummary(existingSummary)
                                            isConfigExpanded = true
                                            Toast.makeText(context, "Resumo excluído.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("excluir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // CARD: VERBOS & 16 TEMPOS VERBAIS DA PÁGINA (A-Z)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Verbos & 16 Tempos Verbais",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color.White
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF38BDF8).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${verbsOnPage.size} itens (A-Z)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Varredura de verbos e phrasal verbs da página. Toque para ver os 16 exemplos em tempos verbais com áudio sincronizado:",
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    verbsOnPage.forEach { v ->
                                        Surface(
                                            shape = RoundedCornerShape(7.dp),
                                            color = if (v.isPhrasalVerb) Color(0xFF6366F1).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (v.isPhrasalVerb) Color(0xFFA5B4FC) else Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.clickable { selectedVerbForDetail = v }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "[${v.verbOrPhrase}]",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(3.5.dp))
                                                Text(
                                                    text = if (v.isPhrasalVerb) "PV" else "V",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (v.isPhrasalVerb) Color(0xFFA5B4FC) else Color(0xFF38BDF8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    // 3. GENERATION & CONFIGURATION CARD (If creating new or re-generating)
                    if (isConfigExpanded) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFF10A37F), Color(0xFF00DC82)))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = if (existingSummary != null) "Refazer Resumo / Nova Análise" else "Criar Resumo com IA",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "A IA analisará todos os temas da página, gerará o resumo e sintetizará o áudio.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // API KEY INPUT
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = { apiKey = it },
                                    label = { Text("Chave da API de IA (sk-...)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = Color(0xFF10A37F)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                            Icon(
                                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isApiKeyVisible) "Ocultar" else "Mostrar",
                                                tint = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    },
                                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF10A37F),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF10A37F),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // MODEL SELECTION
                                Text(
                                    text = "Modelo de IA:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OpenAiService.AVAILABLE_MODELS.forEach { (modelKey, label) ->
                                        val isSelected = selectedModel == modelKey
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) Color(0xFF10A37F) else Color.White.copy(alpha = 0.1f),
                                            modifier = Modifier.clickable { selectedModel = modelKey }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // VOICE SELECTION
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Voz Neural para Leitura:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Gerar Áudio",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = shouldGenerateVoice,
                                            onCheckedChange = { shouldGenerateVoice = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.Black,
                                                checkedTrackColor = Color(0xFF10A37F)
                                            )
                                        )
                                    }
                                }

                                if (shouldGenerateVoice) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OpenAiService.AVAILABLE_VOICES.forEach { (voiceKey, label) ->
                                            val isSelected = selectedVoice == voiceKey
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) Color(0xFF00DC82) else Color.White.copy(alpha = 0.1f),
                                                modifier = Modifier.clickable { selectedVoice = voiceKey }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Mic,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.Black else Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Error message
                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = errorMessage!!,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                // Progress status
                                if (isProcessing) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF10A37F),
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = statusMessage,
                                            fontSize = 12.sp,
                                            color = Color(0xFF10A37F),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Action Button
                                Button(
                                    onClick = { startAnalysisAndGeneration() },
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10A37F),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isProcessing) "Processando..." else "Gerar Resumo & Voz IA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                if (existingSummary != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { isConfigExpanded = false },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancelar e Ver Resumo Existente", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // 4. ORIGINAL PAGE TEXT PREVIEW CARD
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922).copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Texto Original da Página ($pageNumber)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (pageText.isNotBlank()) pageText else "(Página vazia ou sem texto legível extraído)",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 6
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // BOTTOM FIXED RETURN BUTTON
                Surface(
                    color = Color(0xFF161922),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voltar à Leitura", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (selectedVerbForDetail != null) {
        VerbPhrasalVerbDetailDialog(
            term = selectedVerbForDetail!!,
            flagTheme = com.example.ui.theme.LanguageFlagTheme.getThemeFor(com.example.data.model.StudyLanguage.ENGLISH),
            onDismiss = { selectedVerbForDetail = null }
        )
    }
}
}

private fun formatTimeMs(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
