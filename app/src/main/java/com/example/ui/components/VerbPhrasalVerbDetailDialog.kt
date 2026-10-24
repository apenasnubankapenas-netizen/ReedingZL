package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StudyLanguage
import com.example.data.model.VerbPhrasalVerbStudyTerm
import com.example.data.model.VerbTenseQuestionExample
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.ui.theme.LanguageFlagTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VerbPhrasalVerbDetailDialog(
    term: VerbPhrasalVerbStudyTerm,
    flagTheme: LanguageFlagTheme,
    apiKey: String = "",
    speechManager: PdfSpeechManager? = null,
    onToggleFavorite: (VerbPhrasalVerbStudyTerm) -> Unit = {},
    onUpdateVerb: (VerbPhrasalVerbStudyTerm) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTerm by remember(term) { mutableStateOf(term) }
    val localSpeechManager = remember { speechManager ?: PdfSpeechManager(context) }
    val examples = remember(currentTerm) { currentTerm.getExamples() }

    var showContestDialog by remember { mutableStateOf(false) }
    var userCritiqueInput by remember { mutableStateOf("") }
    var isContesting by remember { mutableStateOf(false) }

    val isSpeaking by localSpeechManager.isSpeaking.collectAsState()
    val activeLineId by localSpeechManager.activeComparativeLineId.collectAsState()
    val activeWordIndex by localSpeechManager.activeWordIndex.collectAsState()
    val isPaused by localSpeechManager.isStudyPaused.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            localSpeechManager.stop()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = flagTheme.containerBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header com as cores da bandeira do país
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = flagTheme.surfaceCard,
                        titleContentColor = flagTheme.textPrimary,
                        navigationIconContentColor = flagTheme.textPrimary
                    ),
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${flagTheme.flagEmoji} ${if (currentTerm.isPhrasalVerb) "PHRASAL VERB" else "VERBO"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = flagTheme.secondaryAccent
                                )
                                if (currentTerm.prepositionUsed.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = flagTheme.pillPrepositionBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, flagTheme.pillPrepositionText)
                                    ) {
                                        Text(
                                            text = "Prep: ${currentTerm.prepositionUsed.uppercase()}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = flagTheme.pillPrepositionText,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = currentTerm.verbOrPhrase,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = flagTheme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = flagTheme.textPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onToggleFavorite(currentTerm) }) {
                            Icon(
                                imageVector = if (currentTerm.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favoritar",
                                tint = if (currentTerm.isFavorite) Color(0xFFEF4444) else flagTheme.textSecondary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = flagTheme.textPrimary
                            )
                        }
                    }
                )

                // Barra superior de gradiente no tema da bandeira
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(flagTheme.headerGradient)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card Principal do Verbo / Phrasal Verb
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = flagTheme.surfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, flagTheme.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val verbLineId = "verb_${currentTerm.id}"
                                StudyKaraokeSentenceView(
                                    text = currentTerm.verbOrPhrase,
                                    lineId = verbLineId,
                                    isSpeaking = isSpeaking,
                                    activeLineId = activeLineId,
                                    activeWordIndex = activeWordIndex,
                                    isPaused = isPaused,
                                    flagTheme = flagTheme,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 26.sp,
                                    containerColor = flagTheme.containerBackground.copy(alpha = 0.5f),
                                    showPrefix = if (currentTerm.isPhrasalVerb) "⚡ PHRASAL VERB" else "⚡ VERBO",
                                    onTogglePlayPause = {
                                        localSpeechManager.toggleStudyLine(currentTerm.verbOrPhrase, flagTheme.language, verbLineId)
                                    },
                                    onWordClick = { offset ->
                                        localSpeechManager.speakStudyLine(currentTerm.verbOrPhrase, flagTheme.language, verbLineId, offset)
                                    }
                                )

                                if (currentTerm.ipaTranscription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentTerm.ipaTranscription,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = flagTheme.textSecondary,
                                        modifier = Modifier.padding(start = 36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Destaque da Preposição Usada
                                if (currentTerm.prepositionUsed.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = flagTheme.pillPrepositionBg,
                                        border = androidx.compose.foundation.BorderStroke(1.2.dp, flagTheme.pillPrepositionText.copy(alpha = 0.7f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🎯 PREPOSIÇÃO CORRETA:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = flagTheme.pillPrepositionText
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = flagTheme.secondaryAccent
                                            ) {
                                                Text(
                                                    text = " ${currentTerm.prepositionUsed.uppercase()} ",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = flagTheme.containerBackground,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (currentTerm.baseVerb.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "(Verbo base: ${currentTerm.baseVerb})",
                                                    fontSize = 11.sp,
                                                    color = flagTheme.textSecondary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Significado em Português com Leitura Karaokê e Mini Botão Pausar/Despausar
                                val ptMeaningLineId = "meaning_${currentTerm.id}"
                                StudyKaraokeSentenceView(
                                    text = currentTerm.portugueseMeaning,
                                    lineId = ptMeaningLineId,
                                    isSpeaking = isSpeaking,
                                    activeLineId = activeLineId,
                                    activeWordIndex = activeWordIndex,
                                    isPaused = isPaused,
                                    flagTheme = flagTheme,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 19.sp,
                                    containerColor = flagTheme.containerBackground.copy(alpha = 0.6f),
                                    showPrefix = "🇧🇷 SIGNIFICADO EM PORTUGUÊS (TOQUE PARA OUVIR):",
                                    onTogglePlayPause = {
                                        localSpeechManager.toggleStudyPortugueseLine(currentTerm.portugueseMeaning, ptMeaningLineId)
                                    },
                                    onWordClick = { offset ->
                                        localSpeechManager.speakStudyPortugueseLine(currentTerm.portugueseMeaning, ptMeaningLineId, offset)
                                    }
                                )

                                // Botão de Contestar Significado via OpenAI
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { showContestDialog = true },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = flagTheme.secondaryAccent
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, flagTheme.secondaryAccent.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Contestar Significado (OpenAI)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (currentTerm.definitionTargetLang.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val targetDefLineId = "def_${currentTerm.id}"
                                    StudyKaraokeSentenceView(
                                        text = currentTerm.definitionTargetLang,
                                        lineId = targetDefLineId,
                                        isSpeaking = isSpeaking,
                                        activeLineId = activeLineId,
                                        activeWordIndex = activeWordIndex,
                                        isPaused = isPaused,
                                        flagTheme = flagTheme,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 17.sp,
                                        containerColor = flagTheme.containerBackground.copy(alpha = 0.4f),
                                        showPrefix = "Definição no Idioma (${flagTheme.language.displayName}):",
                                        onTogglePlayPause = {
                                            localSpeechManager.toggleStudyLine(currentTerm.definitionTargetLang, flagTheme.language, targetDefLineId)
                                        },
                                        onWordClick = { offset ->
                                            localSpeechManager.speakStudyLine(currentTerm.definitionTargetLang, flagTheme.language, targetDefLineId, offset)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Título da Seção dos 16 Exemplos
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = flagTheme.secondaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "16 TEMPOS VERBAIS COM QUESTION WORDS",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = flagTheme.textPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = flagTheme.secondaryAccent.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, flagTheme.secondaryAccent)
                            ) {
                                Text(
                                    text = "${examples.size} / 16",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = flagTheme.secondaryAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Cada exemplo possui leitura sincronizada palavra por palavra (Karaokê) e botão para pausar/despausar.",
                            fontSize = 11.sp,
                            color = flagTheme.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Lista dos 16 Exemplos
                    itemsIndexed(examples) { idx, ex ->
                        ExampleQuestionCard(
                            termId = currentTerm.id,
                            example = ex,
                            idx = idx + 1,
                            flagTheme = flagTheme,
                            isSpeaking = isSpeaking,
                            activeLineId = activeLineId,
                            activeWordIndex = activeWordIndex,
                            isPaused = isPaused,
                            speechManager = localSpeechManager,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = "${ex.englishSentence}\n${ex.portugueseSentence}"
                                cm.setPrimaryClip(ClipData.newPlainText("Exemplo Verbal", text))
                                Toast.makeText(context, "Exemplo copiado!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // Diálogo Interativo para Contestar e Corrigir o Significado com a OpenAI
        if (showContestDialog) {
            Dialog(onDismissRequest = { if (!isContesting) showContestDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = flagTheme.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, flagTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(flagTheme.secondaryAccent.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = flagTheme.secondaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Contestar Significado",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = flagTheme.textPrimary
                                )
                                Text(
                                    text = currentTerm.verbOrPhrase,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = flagTheme.secondaryAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Significado atual retornado pela OpenAI:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = flagTheme.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = flagTheme.containerBackground.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, flagTheme.cardBorder),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = currentTerm.portugueseMeaning,
                                fontSize = 12.sp,
                                color = flagTheme.textPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Se este significado estiver incorreto, a API da OpenAI reavaliará o termo, corrigirá o significado verdadeiro e consertará todos os 16 tempos verbais com Question Words!",
                            fontSize = 10.5.sp,
                            color = flagTheme.textSecondary,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = userCritiqueInput,
                            onValueChange = { userCritiqueInput = it },
                            placeholder = {
                                Text(
                                    "Ex: Significa 'decolar' ou 'sair rápido', e não 'tirar'...",
                                    fontSize = 11.sp,
                                    color = flagTheme.textSecondary.copy(alpha = 0.6f)
                                )
                            },
                            label = { Text("Seu significado ou crítica (opcional)", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = flagTheme.secondaryAccent,
                                unfocusedBorderColor = flagTheme.cardBorder,
                                focusedTextColor = flagTheme.textPrimary,
                                unfocusedTextColor = flagTheme.textPrimary,
                                focusedContainerColor = flagTheme.containerBackground,
                                unfocusedContainerColor = flagTheme.containerBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { showContestDialog = false },
                                enabled = !isContesting
                            ) {
                                Text("Cancelar", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val keyToUse = if (apiKey.isNotBlank()) apiKey else com.example.data.openai.OpenAiService.getApiKey(context)
                                    if (keyToUse.isBlank()) {
                                        Toast.makeText(context, "Configure sua OpenAI API Key nas configurações primeiro!", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    isContesting = true
                                    scope.launch {
                                        val result = OpenAiService.recheckAndFixVerbPhrasalVerbMeaning(
                                            apiKey = keyToUse,
                                            term = currentTerm,
                                            userCritiqueOrMeaning = userCritiqueInput,
                                            languageCode = flagTheme.language.code
                                        )
                                        isContesting = false
                                        result.onSuccess { updated ->
                                            currentTerm = updated
                                            onUpdateVerb(updated)
                                            showContestDialog = false
                                            userCritiqueInput = ""
                                            Toast.makeText(context, "✅ Significado e 16 exemplos consertados pela OpenAI!", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Erro ao consertar: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isContesting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = flagTheme.primaryAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isContesting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Consertando com OpenAI...", fontSize = 11.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Consertar & Trocar Exemplos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExampleQuestionCard(
    termId: Int,
    example: VerbTenseQuestionExample,
    idx: Int,
    flagTheme: LanguageFlagTheme,
    isSpeaking: Boolean,
    activeLineId: String?,
    activeWordIndex: Int,
    isPaused: Boolean,
    speechManager: PdfSpeechManager,
    onCopy: () -> Unit
) {
    val targetSentenceLineId = "ex_${termId}_${example.tenseNumber}_target"
    val ptSentenceLineId = "ex_${termId}_${example.tenseNumber}_pt"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = flagTheme.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, flagTheme.cardBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Linha do Cabeçalho do Exemplo (Tempo Verbal & Question Word)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = flagTheme.primaryAccent.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, flagTheme.secondaryAccent)
                    ) {
                        Text(
                            text = "$idx",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = flagTheme.secondaryAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = example.tenseName.ifBlank { "Tempo Verbal $idx" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = flagTheme.textPrimary
                    )
                }

                // Badge da Question Word em destaque
                if (example.questionWord.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = flagTheme.pillPrepositionBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, flagTheme.secondaryAccent)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = flagTheme.secondaryAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = example.questionWord.uppercase(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = flagTheme.secondaryAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Frase no Idioma Estudado com Leitura Karaokê Sincronizada e Botão Pausar/Despausar
            StudyKaraokeSentenceView(
                text = example.englishSentence,
                lineId = targetSentenceLineId,
                isSpeaking = isSpeaking,
                activeLineId = activeLineId,
                activeWordIndex = activeWordIndex,
                isPaused = isPaused,
                flagTheme = flagTheme,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                containerColor = flagTheme.containerBackground.copy(alpha = 0.45f),
                onTogglePlayPause = {
                    speechManager.toggleStudyLine(example.englishSentence, flagTheme.language, targetSentenceLineId)
                },
                onWordClick = { offset ->
                    speechManager.speakStudyLine(example.englishSentence, flagTheme.language, targetSentenceLineId, offset)
                }
            )

            if (example.ipaTranscription.isNotBlank()) {
                Text(
                    text = example.ipaTranscription,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = flagTheme.textSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Frase em Português com Leitura Karaokê Sincronizada e Botão Pausar/Despausar
            StudyKaraokeSentenceView(
                text = example.portugueseSentence,
                lineId = ptSentenceLineId,
                isSpeaking = isSpeaking,
                activeLineId = activeLineId,
                activeWordIndex = activeWordIndex,
                isPaused = isPaused,
                flagTheme = flagTheme,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                showPrefix = "🇧🇷 Tradução:",
                containerColor = flagTheme.containerBackground.copy(alpha = 0.6f),
                onTogglePlayPause = {
                    speechManager.toggleStudyPortugueseLine(example.portugueseSentence, ptSentenceLineId)
                },
                onWordClick = { offset ->
                    speechManager.speakStudyPortugueseLine(example.portugueseSentence, ptSentenceLineId, offset)
                }
            )

            if (example.collocationNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 Combinação direta: ${example.collocationNotes}",
                    fontSize = 11.sp,
                    color = flagTheme.secondaryAccent.copy(alpha = 0.95f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ações: Copiar Frase Completa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar Exemplo",
                        tint = flagTheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
