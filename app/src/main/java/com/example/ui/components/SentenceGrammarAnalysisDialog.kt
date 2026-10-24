package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GrammarRuleItem
import com.example.data.model.SavedSentenceAnalysis
import com.example.data.model.StudyLanguage
import com.example.data.model.SyntaxTokenItem
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.launch

/**
 * Diálogo de Análise Estrutural e Gramatical de Frases Complexas nos idiomas:
 * Russo (casos cirílicos, aspecto verbal, regência),
 * Alemão (ordem V2, orações subordinadas, TeKaMoLo, 4 casos),
 * Grego (declinações, aspecto aoristo, clíticos),
 * Chinês (tópico-comentário, classificadores, partículas 的/得/地, aspecto 了/过/着),
 * Japonês (sistema de partículas は/が/を/に/で, ordem SOV, keigo),
 * Ucraniano (7 casos cirílicos incluindo vocativo, prefixos),
 * Árabe (I'rab, orações nominais/verbais, raízes trilíteras, Idafa),
 * Hebraico (Smikhut, raízes Shoresh, templates Binyanim),
 * e Inglês.
 *
 * Permite analisar via OpenAI e salvar localmente no banco para estudo posterior offline.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceGrammarAnalysisDialog(
    sentence: String,
    initialLanguage: StudyLanguage? = null,
    bookTitle: String = "",
    bookId: Int = 0,
    pageNumber: Int = 1,
    readingTheme: String = "DARK_OLED",
    category: String? = null,
    categoryColor: String? = null,
    speechManager: PdfSpeechManager? = null,
    onSaveAnalysis: ((SavedSentenceAnalysis) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = remember(readingTheme, category, categoryColor) {
        ReadingThemeManager.getPalette(readingTheme, category, categoryColor)
    }

    val accent = palette.accent
    val textColor = palette.textPrimary
    val subTextColor = palette.textSecondary
    val cardBg = palette.surfaceVariant
    val borderColor = palette.cardBorder.takeIf { it != Color.Transparent } ?: accent.copy(alpha = 0.45f)

    var currentLanguage by remember {
        mutableStateOf(initialLanguage ?: StudyLanguage.detectFromText(sentence))
    }

    var apiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var showApiKeyInput by remember { mutableStateOf(apiKey.isBlank()) }
    var apiKeyInput by remember { mutableStateOf(apiKey) }

    var analysis by remember { mutableStateOf<SavedSentenceAnalysis?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSavedLocally by remember { mutableStateOf(false) }
    var customUserNotes by remember { mutableStateOf("") }
    var isPlayingAudio by remember { mutableStateOf(false) }

    fun runAnalysis(targetLang: StudyLanguage) {
        val currentKey = apiKey.trim()
        if (currentKey.isBlank()) {
            showApiKeyInput = true
            errorMessage = "Configure a chave de API da OpenAI para analisar a sintaxe da frase."
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = OpenAiService.analyzeSentenceGrammarAndSyntax(
                apiKey = currentKey,
                sentence = sentence,
                language = targetLang,
                bookTitle = bookTitle,
                bookId = bookId,
                pageNumber = pageNumber
            )
            isLoading = false
            result.onSuccess { parsed ->
                analysis = parsed
                isSavedLocally = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Falha ao analisar a gramática da frase."
            }
        }
    }

    LaunchedEffect(sentence, currentLanguage) {
        if (analysis == null && apiKey.isNotBlank()) {
            runAnalysis(currentLanguage)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(1.2.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // 1. CABEÇALHO COMPACTO E MINIMALISTA DO DIÁLOGO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = currentLanguage.badgeColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, currentLanguage.badgeColor)
                        ) {
                            Text(
                                text = currentLanguage.flag,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Raio-X Sintático",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = currentLanguage.badgeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = currentLanguage.displayName,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentLanguage.badgeColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    // Botão Fechar Minimalista
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = subTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. SELETOR RÁPIDO DE IDIOMA (COMPACTO)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) lang.badgeColor.copy(alpha = 0.25f) else cardBg,
                            border = BorderStroke(
                                if (isSelected) 1.dp else 0.5.dp,
                                if (isSelected) lang.badgeColor else borderColor.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable {
                                    if (currentLanguage != lang) {
                                        currentLanguage = lang
                                        runAnalysis(lang)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = lang.flag, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = lang.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) lang.badgeColor else textColor
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 5.dp),
                    color = borderColor.copy(alpha = 0.25f),
                    thickness = 0.5.dp
                )

                // 3. CONFIGURAÇÃO DA CHAVE OPENAI (SE NECESSÁRIO)
                AnimatedVisibility(visible = showApiKeyInput) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Chave da OpenAI Necessária",
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Insira sua API Key para habilitar a decomposição de árvores sintáticas e casos gramaticais.",
                                fontSize = 11.5.sp,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                placeholder = { Text("sk-...", fontSize = 12.sp, color = subTextColor.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = borderColor,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showApiKeyInput = false }) {
                                    Text("Cancelar", color = subTextColor, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (apiKeyInput.isNotBlank()) {
                                            OpenAiService.saveApiKey(context, apiKeyInput.trim())
                                            apiKey = apiKeyInput.trim()
                                            showApiKeyInput = false
                                            runAnalysis(currentLanguage)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                                ) {
                                    Text("Salvar & Analisar", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 4. CONTEÚDO PRINCIPAL (COM ROLAGEM)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = currentLanguage.badgeColor, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Decompondo estrutura sintática em ${currentLanguage.displayName}...",
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    fontSize = 13.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mapeando casos, partículas e árvore de subordinação...",
                                    color = subTextColor,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        errorMessage != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row {
                                    OutlinedButton(onClick = { showApiKeyInput = true }) {
                                        Text("Configurar Chave", color = accent, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { runAnalysis(currentLanguage) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                                    ) {
                                        Text("Tentar Novamente", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        analysis != null -> {
                            val item = analysis!!
                            val tokens = remember(item) { item.getTokens() }
                            val rules = remember(item) { item.getRules() }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // A. CARD DA FRASE ORIGINAL & ÁUDIO
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(1.dp, currentLanguage.badgeColor.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = currentLanguage.badgeColor.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "Frase Analisada",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = currentLanguage.badgeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // TTS Audio Button
                                                IconButton(
                                                    onClick = {
                                                        isPlayingAudio = true
                                                        speechManager?.speakTextInStudyLanguage(
                                                            text = item.sentenceText,
                                                            language = currentLanguage
                                                        )
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Ouvir Pronúncia",
                                                        tint = if (isPlayingAudio) currentLanguage.badgeColor else subTextColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // Copy Button
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        val clip = ClipData.newPlainText("Frase", item.sentenceText)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "Frase copiada!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copiar",
                                                        tint = subTextColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Texto da frase (com suporte a RTL se árabe/hebraico)
                                        Text(
                                            text = item.sentenceText,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            lineHeight = 26.sp,
                                            style = LocalTextStyle.current.copy(
                                                textDirection = if (currentLanguage.isRtl) TextDirection.Rtl else TextDirection.Ltr
                                            )
                                        )

                                        // Transliteração / Pinyin / Romaji / Niqqud / Tashkeel
                                        if (item.transliteration.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.transliteration,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = currentLanguage.badgeColor.copy(alpha = 0.9f),
                                                style = LocalTextStyle.current.copy(
                                                    textDirection = TextDirection.Ltr
                                                )
                                            )
                                        }

                                        // Tradução em Português
                                        if (item.translationPt.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.2f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = item.translationPt,
                                                    fontSize = 13.sp,
                                                    color = textColor.copy(alpha = 0.9f),
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // B. ARQUITETURA SINTÁTICA GERAL (ÁRVORE / ESTRUTURA)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(0.8.dp, borderColor.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountTree,
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Estrutura Sintática Global",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accent
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (item.complexityLevel.uppercase()) {
                                                    "C2" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                    "C1" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                                    else -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                }
                                            ) {
                                                Text(
                                                    text = "Nível ${item.complexityLevel}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = when (item.complexityLevel.uppercase()) {
                                                        "C2" -> Color(0xFFEF4444)
                                                        "C1" -> Color(0xFF8B5CF6)
                                                        else -> Color(0xFF3B82F6)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.syntacticTreeSummary,
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp,
                                            color = textColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // C. DECOMPOSIÇÃO TOKEN A TOKEN (SINTAXE, CASOS, PARTÍCULAS)
                                if (tokens.isNotEmpty()) {
                                    Text(
                                        text = "Decomposição Token a Token (Sintaxe & Morfologia)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    tokens.forEachIndexed { idx, tokenItem ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = cardBg,
                                            border = BorderStroke(0.6.dp, borderColor.copy(alpha = 0.35f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = tokenItem.token,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = currentLanguage.badgeColor
                                                        )
                                                        if (tokenItem.reading.isNotBlank()) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "[${tokenItem.reading}]",
                                                                fontSize = 11.5.sp,
                                                                color = subTextColor
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = accent.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = tokenItem.syntacticRole,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = accent,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                if (tokenItem.morphologyOrCase.isNotBlank() || tokenItem.rootOrLemma.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (tokenItem.morphologyOrCase.isNotBlank()) {
                                                            Text(
                                                                text = tokenItem.morphologyOrCase,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = textColor.copy(alpha = 0.85f)
                                                            )
                                                        }
                                                        if (tokenItem.rootOrLemma.isNotBlank()) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "• Raiz: ${tokenItem.rootOrLemma}",
                                                                fontSize = 10.5.sp,
                                                                color = subTextColor
                                                            )
                                                        }
                                                    }
                                                }

                                                if (tokenItem.explanation.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = tokenItem.explanation,
                                                        fontSize = 11.sp,
                                                        color = subTextColor,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // D. REGRAS GRAMATICAIS EM DESTAQUE (CASOS CIRÍLICOS, PARTÍCULAS JAPONESAS, V2 ALEMÃO, ETC.)
                                if (rules.isNotEmpty()) {
                                    Text(
                                        text = "Regras Gramaticais Aplicadas",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    rules.forEach { rule ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardBg),
                                            border = BorderStroke(0.7.dp, currentLanguage.badgeColor.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = rule.ruleTitle,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = currentLanguage.badgeColor.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = rule.category,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = currentLanguage.badgeColor,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                if (rule.exampleSnippet.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Exemplo na oração: \"${rule.exampleSnippet}\"",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = accent
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = rule.explanation,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // E. DICA C2 DE PENSAMENTO NATIVO
                                if (item.c2MasteryTip.isNotBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.45f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Dica de Maestria C2 (Pensar como Nativo)",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF59E0B)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.c2MasteryTip,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                color = textColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // F. DESAFIO DE PRODUÇÃO SINTÁTICA
                                if (item.practiceChallenge.isNotBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.FitnessCenter,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Desafio de Fixação Sintática",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.practiceChallenge,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                color = textColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // G. CAMPO DE ANOTAÇÕES PESSOAIS DO ALUNO
                                OutlinedTextField(
                                    value = customUserNotes,
                                    onValueChange = { customUserNotes = it },
                                    label = { Text("Minhas Anotações de Estudo (Opcional)", fontSize = 11.5.sp) },
                                    placeholder = { Text("Ex: Memorizar terminação de dativo plural com a preposição...", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accent,
                                        unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                // 5. BARRA DE AÇÕES INFERIOR: SALVAR NO BANCO LOCAL PARA ESTUDO OFFLINE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar", color = subTextColor, fontSize = 13.sp)
                    }

                    if (analysis != null) {
                        Button(
                            onClick = {
                                val current = analysis!!
                                val toSave = current.copy(
                                    userNotes = customUserNotes.trim(),
                                    bookId = bookId,
                                    bookTitle = bookTitle,
                                    pageNumber = pageNumber
                                )
                                onSaveAnalysis?.invoke(toSave)
                                isSavedLocally = true
                                Toast.makeText(
                                    context,
                                    "Frase e regras salvas com sucesso no Caderno de Estudos!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSavedLocally) Color(0xFF10B981) else accent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSavedLocally) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSavedLocally) "Salvo no Caderno" else "Salvar para Estudo",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
