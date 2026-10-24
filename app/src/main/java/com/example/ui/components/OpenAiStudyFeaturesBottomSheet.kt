package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.StudyLanguage
import com.example.data.openai.*
import kotlinx.coroutines.launch
import java.io.File

enum class OpenAiStudyTab(val label: String, val iconName: String) {
    QUIZ("Quiz Ativo", "quiz"),
    PODCAST("Podcast 2 Vozes", "mic"),
    AUDIOGRAM("Resumo 30s", "timer"),
    SIMPLIFY("Níveis de Linguagem", "auto_stories"),
    PRONUNCIATION("Pronúncia", "record_voice_over"),
    EXAMPLES("Frases & Contexto", "format_quote"),
    FALSE_FRIENDS("Falsos Amigos", "warning"),
    SEMANTICS("Mapa Semântico", "hub")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAiStudyFeaturesBottomSheet(
    pageNumber: Int,
    pageText: String,
    pageTerms: List<PageDictionaryTerm>,
    accentColor: Color,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentTab by remember { mutableStateOf(OpenAiStudyTab.QUIZ) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF131722),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Estudo Inteligente OpenAI",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Página $pageNumber • 10 Recursos de IA Integrados",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector Horizontal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OpenAiStudyTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            0.7.dp,
                            if (isSelected) accentColor else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.clickable { currentTab = tab }
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 520.dp)
            ) {
                when (currentTab) {
                    OpenAiStudyTab.QUIZ -> QuizTabContent(pageText, accentColor)
                    OpenAiStudyTab.PODCAST -> PodcastTabContent(pageText, accentColor)
                    OpenAiStudyTab.AUDIOGRAM -> AudiogramTabContent(pageNumber, pageText, accentColor)
                    OpenAiStudyTab.SIMPLIFY -> SimplifyTabContent(pageText, accentColor)
                    OpenAiStudyTab.PRONUNCIATION -> PronunciationTabContent(pageTerms, accentColor)
                    OpenAiStudyTab.EXAMPLES -> ExamplesTabContent(pageTerms, accentColor)
                    OpenAiStudyTab.FALSE_FRIENDS -> FalseFriendsTabContent(pageTerms, accentColor)
                    OpenAiStudyTab.SEMANTICS -> SemanticsTabContent(pageText, pageTerms, accentColor)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1 & 2. QUIZ & FLASHCARDS ATIVOS
// -------------------------------------------------------------
@Composable
private fun QuizTabContent(pageText: String, accentColor: Color) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var studyKit by remember { mutableStateOf<PageActiveStudyKit?>(null) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var isShowingFlashcards by remember { mutableStateOf(false) }

    LaunchedEffect(pageText) {
        if (studyKit == null && pageText.isNotBlank()) {
            isLoading = true
            val res = OpenAiAdvancedStudyService.generatePageActiveStudyKit(pageText, context)
            studyKit = res.getOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isShowingFlashcards) "Flashcards de Memorização" else "Perguntas de Fixação Ativa",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Row {
                TextButton(onClick = { isShowingFlashcards = !isShowingFlashcards }) {
                    Text(
                        text = if (isShowingFlashcards) "Ver Quiz" else "Ver Flashcards",
                        fontSize = 11.sp,
                        color = accentColor
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (studyKit != null) {
            val kit = studyKit!!
            if (!isShowingFlashcards) {
                kit.questions.forEachIndexed { qIdx, question ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${qIdx + 1}. ${question.question}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val chosen = selectedAnswers[qIdx]
                            question.options.forEachIndexed { optIdx, optText ->
                                val isChosen = chosen == optIdx
                                val isCorrect = question.correctIndex == optIdx
                                val showCorrection = chosen != null

                                val btnColor = when {
                                    showCorrection && isCorrect -> Color(0xFF10B981) // Verde
                                    showCorrection && isChosen && !isCorrect -> Color(0xFFEF4444) // Vermelho
                                    isChosen -> accentColor
                                    else -> Color.White.copy(alpha = 0.85f)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        showCorrection && isCorrect -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        showCorrection && isChosen && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                        else -> Color.White.copy(alpha = 0.03f)
                                    },
                                    border = BorderStroke(0.6.dp, btnColor.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            selectedAnswers = selectedAnswers.toMutableMap().apply { put(qIdx, optIdx) }
                                        }
                                ) {
                                    Text(
                                        text = optText,
                                        fontSize = 11.5.sp,
                                        color = btnColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (chosen != null && question.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 ${question.explanation}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }
                    }
                }
            } else {
                // Flashcards
                kit.flashcards.forEach { card ->
                    var isFlipped by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFlipped) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(0.7.dp, if (isFlipped) accentColor else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { isFlipped = !isFlipped }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = card.keyConcept,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                Text(
                                    text = if (isFlipped) "Toque para ocultar" else "Toque para virar",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isFlipped) card.back else card.front,
                                fontSize = 12.sp,
                                color = Color.White,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Não foi possível gerar o quiz automaticamente.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// -------------------------------------------------------------
// 3. MODO PODCAST / DIÁLOGO ENTRE DUAS VOZES
// -------------------------------------------------------------
@Composable
private fun PodcastTabContent(pageText: String, accentColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var episode by remember { mutableStateOf<PodcastEpisode?>(null) }
    var playingIndex by remember { mutableStateOf<Int?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(pageText) {
        if (episode == null && pageText.isNotBlank()) {
            isLoading = true
            val res = OpenAiAdvancedStudyService.generatePodcastDialogue(pageText, context)
            episode = res.getOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (episode != null) {
            val ep = episode!!
            Text(
                text = ep.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (ep.summary.isNotBlank()) {
                Text(
                    text = ep.summary,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            ep.lines.forEachIndexed { idx, line ->
                val isPlaying = playingIndex == idx
                val isAlex = line.speaker.contains("Alex", ignoreCase = true)
                val bubbleColor = if (isAlex) Color(0xFF1E293B) else Color(0xFF2E1065)
                val nameColor = if (isAlex) Color(0xFF38BDF8) else Color(0xFFC084FC)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bubbleColor,
                    border = BorderStroke(0.6.dp, if (isPlaying) accentColor else Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(nameColor.copy(alpha = 0.25f))
                                .clickable {
                                    scope.launch {
                                        try {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            playingIndex = idx

                                            val audioRes = OpenAiAdvancedStudyService.synthesizeVoiceLine(line.text, line.voice, context)
                                            val file = audioRes.getOrNull()
                                            if (file != null) {
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(file.absolutePath)
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener { playingIndex = null }
                                                }
                                            } else {
                                                playingIndex = null
                                            }
                                        } catch (e: Exception) {
                                            playingIndex = null
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Ouvir fala",
                                tint = nameColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${line.speaker} (Voz: ${line.voice.capitalize()})",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = nameColor
                            )
                            Text(
                                text = line.text,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. SIMPLIFICAÇÃO EM NÍVEIS DE LINGUAGEM
// -------------------------------------------------------------
@Composable
private fun SimplifyTabContent(pageText: String, accentColor: Color) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var levels by remember { mutableStateOf<SimplifiedLevels?>(null) }
    var selectedLevel by remember { mutableStateOf(0) } // 0: Didático, 1: Tópicos Essenciais, 2: Original

    LaunchedEffect(pageText) {
        if (levels == null && pageText.isNotBlank()) {
            isLoading = true
            val res = OpenAiAdvancedStudyService.simplifyTextLevels(pageText, context)
            levels = res.getOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("💡 Didático", "⚡ Essencial (Bullets)", "📄 Original").forEachIndexed { idx, label ->
                val isSelected = selectedLevel == idx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.6.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedLevel = idx }
                ) {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (levels != null) {
            val lvl = levels!!
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    when (selectedLevel) {
                        0 -> {
                            Text(
                                text = lvl.didactic,
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 18.sp
                            )
                        }
                        1 -> {
                            lvl.bulletPoints.forEach { point ->
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text(text = "•", color = accentColor, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = point,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.92f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = lvl.original,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. TREINADOR DE PRONÚNCIA E LEITURA EM VOZ ALTA (Whisper + GPT)
// -------------------------------------------------------------
@Composable
private fun PronunciationTabContent(terms: List<PageDictionaryTerm>, accentColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTermIndex by remember { mutableStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var isEvaluating by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PronunciationResult?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }

    val currentTerm = terms.getOrNull(selectedTermIndex)

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Escolha o termo para praticar sua pronúncia:",
            fontSize = 11.5.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Seletor de termos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            terms.take(6).forEachIndexed { idx, t ->
                val isSelected = selectedTermIndex == idx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.6.dp, if (isSelected) accentColor else Color.Transparent),
                    modifier = Modifier.clickable {
                        selectedTermIndex = idx
                        result = null
                    }
                ) {
                    Text(
                        text = t.term,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (currentTerm != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTerm.term,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )

                    if (currentTerm.originalDefinition.isNotBlank()) {
                        Text(
                            text = currentTerm.originalDefinition.take(120) + "...",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botão Gravar / Parar
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color(0xFFEF4444) else accentColor)
                            .clickable {
                                if (!isRecording) {
                                    // Iniciar gravação
                                    try {
                                        val tempFile = File.createTempFile("pronunciation_", ".m4a", context.cacheDir)
                                        recordFile = tempFile
                                        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            MediaRecorder(context)
                                        } else {
                                            @Suppress("DEPRECATION")
                                            MediaRecorder()
                                        }
                                        rec.apply {
                                            setAudioSource(MediaRecorder.AudioSource.MIC)
                                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                            setOutputFile(tempFile.absolutePath)
                                            prepare()
                                            start()
                                        }
                                        recorder = rec
                                        isRecording = true
                                        result = null
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Permissão de microfone necessária", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Parar gravação e avaliar
                                    try {
                                        recorder?.stop()
                                        recorder?.release()
                                        recorder = null
                                        isRecording = false
                                        isEvaluating = true

                                        scope.launch {
                                            val file = recordFile
                                            if (file != null && file.exists()) {
                                                val res = OpenAiAdvancedStudyService.evaluatePronunciation(
                                                    audioRecordFile = file,
                                                    expectedText = currentTerm.term,
                                                    targetLanguage = StudyLanguage.PORTUGUESE,
                                                    context = context
                                                )
                                                result = res.getOrNull()
                                            }
                                            isEvaluating = false
                                        }
                                    } catch (e: Exception) {
                                        isRecording = false
                                        isEvaluating = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Gravar pronúncia",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isRecording) "Gravando... toque para parar" else if (isEvaluating) "Avaliando com Whisper + GPT..." else "Toque para falar o termo",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    // Resultado da avaliação
                    if (result != null) {
                        val res = result!!
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Pontuação de Precisão",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = "${res.scorePercent}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ouvido pela IA: \"${res.recognizedText}\"",
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💬 ${res.feedback}",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                if (res.phoneticTips.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "🎯 ${res.phoneticTips}",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFFDE68A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. AUDIOGRAMA DE 30 SEGUNDOS (Resumo Rápido da Página)
// -------------------------------------------------------------
@Composable
private fun AudiogramTabContent(pageNumber: Int, pageText: String, accentColor: Color) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var audiogram by remember { mutableStateOf<PageAudiogram?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(pageNumber, pageText) {
        if (audiogram == null && pageText.isNotBlank()) {
            isLoading = true
            val res = OpenAiAdvancedStudyService.generatePageAudiogram(pageNumber, pageText, context)
            audiogram = res.getOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Resumo Executivo da Página em 30 Segundos",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = "Ideias centrais sintetizadas e narradas com voz neural da OpenAI.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (audiogram != null) {
            val audio = audiogram!!
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable {
                                    val f = audio.audioFile
                                    if (f != null && f.exists()) {
                                        if (isPlaying) {
                                            mediaPlayer?.pause()
                                            isPlaying = false
                                        } else {
                                            if (mediaPlayer == null) {
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(f.absolutePath)
                                                    prepare()
                                                    setOnCompletionListener { isPlaying = false }
                                                }
                                            }
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        }
                                    } else {
                                        Toast.makeText(context, "Áudio sendo gerado...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Reproduzir audiograma",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Áudio Síntese • 30s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isPlaying) "Reproduzindo agora..." else "Toque para ouvir a síntese",
                                fontSize = 10.5.sp,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = audio.narratorScript,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    audio.bulletPoints.forEach { pt ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(text = "✓", color = Color(0xFF10B981), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pt,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 9. MAPA SEMÂNTICO E CONEXÕES ENTRE OS TERMOS
// -------------------------------------------------------------
@Composable
private fun SemanticsTabContent(pageText: String, terms: List<PageDictionaryTerm>, accentColor: Color) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var relations by remember { mutableStateOf<List<SemanticRelation>?>(null) }

    LaunchedEffect(pageText, terms) {
        if (relations == null && terms.isNotEmpty()) {
            isLoading = true
            val res = OpenAiAdvancedStudyService.generateSemanticRelationships(
                pageText = pageText,
                terms = terms.map { it.term },
                context = context
            )
            relations = res.getOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Conexões e Cadeia Lógica entre os Conceitos",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = "Como o autor articula os termos ao longo da argumentação.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (!relations.isNullOrEmpty()) {
            relations!!.forEach { rel ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rel.sourceTerm,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "→ ${rel.relationType} →",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = rel.targetTerm,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (rel.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rel.explanation,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Nenhuma conexão automática identificada para os termos desta página.",
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// -------------------------------------------------------------
// 6. FRASES DE CONTEXTO REAL COM ÁUDIO PRÓPRIO
// -------------------------------------------------------------
@Composable
private fun ExamplesTabContent(terms: List<PageDictionaryTerm>, accentColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTerm by remember { mutableStateOf(terms.firstOrNull()?.term ?: "Conflito") }
    var selectedLanguage by remember { mutableStateOf(StudyLanguage.ENGLISH) }
    var isLoading by remember { mutableStateOf(false) }
    var examples by remember { mutableStateOf<List<TermContextExample>?>(null) }
    var playingIndex by remember { mutableStateOf<Int?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(selectedTerm, selectedLanguage) {
        if (selectedTerm.isNotBlank()) {
            isLoading = true
            val res = OpenAiAdvancedStudyService.fetchRealWorldExamples(
                term = selectedTerm,
                language = selectedLanguage,
                context = context
            )
            examples = res.getOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Frases Autênticas de Uso no Mundo Real",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = "Veja o termo aplicado em contextos reais com áudio expressivo OpenAI TTS.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Seletor de termos da página
        if (terms.isNotEmpty()) {
            Text(
                text = "Termo da Página:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                terms.forEach { t ->
                    val isSel = selectedTerm == t.term
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(0.6.dp, if (isSel) accentColor else Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.clickable { selectedTerm = t.term }
                    ) {
                        Text(
                            text = t.term,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) accentColor else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Seletor de Idioma Alvo
        Text(
            text = "Idioma de Aplicação:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                StudyLanguage.ENGLISH,
                StudyLanguage.SPANISH,
                StudyLanguage.FRENCH,
                StudyLanguage.GERMAN,
                StudyLanguage.ITALIAN,
                StudyLanguage.RUSSIAN,
                StudyLanguage.CHINESE,
                StudyLanguage.JAPANESE
            ).forEach { lang ->
                val isSel = selectedLanguage == lang
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.6.dp, if (isSel) accentColor else Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.clickable { selectedLanguage = lang }
                ) {
                    Text(
                        text = "${lang.flag} ${lang.displayName}",
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) accentColor else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (!examples.isNullOrEmpty()) {
            examples!!.forEachIndexed { idx, ex ->
                val isPlayingThis = playingIndex == idx
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = ex.contextType,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Botão de Áudio OpenAI TTS
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .clickable {
                                        if (isPlayingThis) {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            playingIndex = null
                                        } else {
                                            scope.launch {
                                                playingIndex = idx
                                                val voice = if (idx % 2 == 0) "nova" else "alloy"
                                                val audioRes = OpenAiAdvancedStudyService.synthesizeVoiceLine(
                                                    text = ex.sentenceOriginal,
                                                    voice = voice,
                                                    context = context
                                                )
                                                val file = audioRes.getOrNull()
                                                if (file != null && file.exists()) {
                                                    mediaPlayer?.release()
                                                    mediaPlayer = MediaPlayer().apply {
                                                        setDataSource(file.absolutePath)
                                                        prepare()
                                                        setOnCompletionListener {
                                                            playingIndex = null
                                                        }
                                                        start()
                                                    }
                                                } else {
                                                    playingIndex = null
                                                    Toast.makeText(context, "Erro gerando áudio", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.VolumeUp,
                                    contentDescription = "Ouvir frase",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ex.sentenceOriginal,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ex.sentencePortuguese,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. ALERTA DE FALSOS AMIGOS E NUANCES CULTURAIS
// -------------------------------------------------------------
@Composable
private fun FalseFriendsTabContent(terms: List<PageDictionaryTerm>, accentColor: Color) {
    val context = LocalContext.current
    var selectedTerm by remember { mutableStateOf(terms.firstOrNull()?.term ?: "Conflito") }
    var selectedLanguage by remember { mutableStateOf(StudyLanguage.ENGLISH) }
    var isLoading by remember { mutableStateOf(false) }
    var alert by remember { mutableStateOf<FalseFriendsAlert?>(null) }

    LaunchedEffect(selectedTerm, selectedLanguage) {
        if (selectedTerm.isNotBlank()) {
            isLoading = true
            val res = OpenAiAdvancedStudyService.detectFalseFriendsAndNuances(
                term = selectedTerm,
                targetLanguage = selectedLanguage,
                context = context
            )
            alert = res.getOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Falsos Amigos & Nuances Culturais",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = "Evite confusões linguísticas comuns entre português e outros idiomas.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Seletor de termos da página
        if (terms.isNotEmpty()) {
            Text(
                text = "Termo Analisado:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                terms.forEach { t ->
                    val isSel = selectedTerm == t.term
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(0.6.dp, if (isSel) accentColor else Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.clickable { selectedTerm = t.term }
                    ) {
                        Text(
                            text = t.term,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) accentColor else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Seletor de Idioma Alvo
        Text(
            text = "Idioma Comparado:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                StudyLanguage.ENGLISH,
                StudyLanguage.SPANISH,
                StudyLanguage.FRENCH,
                StudyLanguage.GERMAN,
                StudyLanguage.ITALIAN
            ).forEach { lang ->
                val isSel = selectedLanguage == lang
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.6.dp, if (isSel) accentColor else Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.clickable { selectedLanguage = lang }
                ) {
                    Text(
                        text = "${lang.flag} ${lang.displayName}",
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) accentColor else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (alert != null) {
            val item = alert!!
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (item.isFalseFriend) Color(0xFFEF4444).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                border = BorderStroke(
                    0.8.dp,
                    if (item.isFalseFriend) Color(0xFFEF4444).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isFalseFriend) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (item.isFalseFriend) Color(0xFFEF4444) else accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.warningTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isFalseFriend) Color(0xFFEF4444) else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.explanation,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 17.sp
                    )

                    if (item.correctEquivalent.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.06f)
                        ) {
                            Text(
                                text = "Equivalente correto recomendado: ${item.correctEquivalent}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

