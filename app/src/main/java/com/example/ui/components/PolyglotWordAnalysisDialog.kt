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
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PolyglotWordAnalysis
import com.example.data.model.StudyLanguage
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.launch

/**
 * Diálogo unificado de Domínio Lexical Poliglota C2:
 * Suporta Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico e Inglês.
 * Fornece:
 * - Acentuação tônica (Russo/Ucraniano)
 * - Pinyin com tons e Radicais (Chinês)
 * - Furigana e Leituras On/Kun (Japonês)
 * - Artigos der/die/das e Komposita (Alemão)
 * - Diacríticos e Transliteração (Grego)
 * - Vocalização Tashkeel/Niqqud e Raízes Trilíteras (Árabe e Hebraico)
 * - Fonética IPA, áudio nativo, collocations e desafio ativo avaliado por IA.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PolyglotWordAnalysisDialog(
    word: String,
    initialLanguage: StudyLanguage? = null,
    sentenceContext: String = "",
    bookTitle: String = "",
    bookId: Int = 0,
    pageNumber: Int = 1,
    readingTheme: String = "DARK_OLED",
    category: String? = null,
    categoryColor: String? = null,
    speechManager: PdfSpeechManager? = null,
    onSaveToDictionary: ((PageDictionaryTerm) -> Unit)? = null,
    onAnalyzeSentenceGrammar: ((sentence: String, lang: StudyLanguage) -> Unit)? = null,
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
        mutableStateOf(initialLanguage ?: StudyLanguage.detectFromText(word.ifBlank { sentenceContext }))
    }

    var apiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var showApiKeyInput by remember { mutableStateOf(apiKey.isBlank()) }
    var apiKeyInput by remember { mutableStateOf(apiKey) }

    var analysis by remember { mutableStateOf<PolyglotWordAnalysis?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Active Challenge State
    var userSentenceInput by remember { mutableStateOf("") }
    var isEvaluatingSentence by remember { mutableStateOf(false) }
    var sentenceEvaluationResult by remember { mutableStateOf<String?>(null) }
    var sentenceEvalError by remember { mutableStateOf<String?>(null) }
    var isSavedToDictionary by remember { mutableStateOf(false) }

    fun speakCurrentWord() {
        val target = analysis?.word?.ifBlank { word } ?: word
        speechManager?.speakTextInStudyLanguage(target, currentLanguage)
    }

    fun fetchAnalysis(keyToUse: String? = null, langToUse: StudyLanguage = currentLanguage) {
        val currentKey = keyToUse ?: apiKey
        if (currentKey.isBlank()) {
            showApiKeyInput = true
            errorMessage = "Configure sua Chave de API da OpenAI para gerar o Raio-X Poliglota."
            return
        }

        isLoading = true
        errorMessage = null
        scope.launch {
            val res = OpenAiService.analyzePolyglotWord(
                apiKey = currentKey,
                word = word,
                language = langToUse,
                contextSentence = sentenceContext,
                bookTitle = bookTitle
            )
            isLoading = false
            res.onSuccess { data ->
                analysis = data
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Erro ao consultar OpenAI."
            }
        }
    }

    LaunchedEffect(word, currentLanguage) {
        if (apiKey.isNotBlank()) {
            fetchAnalysis(langToUse = currentLanguage)
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
                .fillMaxHeight(0.94f)
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = currentLanguage.badgeColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currentLanguage.flag, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Raio-X Poliglota C2",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = (analysis?.cefrBadgeColor ?: currentLanguage.badgeColor).copy(alpha = 0.2f),
                                    border = BorderStroke(0.8.dp, analysis?.cefrBadgeColor ?: currentLanguage.badgeColor)
                                ) {
                                    Text(
                                        text = analysis?.cefrLevel ?: "C2 DOMAIN",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = analysis?.cefrBadgeColor ?: currentLanguage.badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${currentLanguage.displayName} • ${currentLanguage.scriptFamily}",
                                fontSize = 10.5.sp,
                                color = subTextColor
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showApiKeyInput = !showApiKeyInput },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Chave API",
                                tint = if (apiKey.isNotBlank()) accent else Color(0xFFFFB300),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = subTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Seletor Horizontal de Idiomas (Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico, Inglês)
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
                                    analysis = null
                                    fetchAnalysis(langToUse = lang)
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

                // API Key Configuration Card
                AnimatedVisibility(visible = showApiKeyInput) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Chave OpenAI para Análise Poliglota C2:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                placeholder = { Text("sk-proj-...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = {
                                        OpenAiService.saveApiKey(context, apiKeyInput.trim())
                                        apiKey = apiKeyInput.trim()
                                        showApiKeyInput = false
                                        Toast.makeText(context, "Chave OpenAI salva com sucesso!", Toast.LENGTH_SHORT).show()
                                        fetchAnalysis(apiKeyInput.trim(), currentLanguage)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Salvar Chave", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = borderColor.copy(alpha = 0.25f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Content Body
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = currentLanguage.badgeColor, modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Decodificando morfo-sintaxe de '${word}' em ${currentLanguage.displayName}...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                text = "Consultando regras C2 de ${currentLanguage.scriptFamily}",
                                fontSize = 11.sp,
                                color = subTextColor
                            )
                        }
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(errorMessage ?: "", color = textColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { fetchAnalysis(langToUse = currentLanguage) },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tentar Novamente", fontSize = 12.sp)
                            }
                        }
                    } else if (analysis != null) {
                        val item = analysis!!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Card Hero da Palavra
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.2.dp, currentLanguage.badgeColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            // Texto com Vocalização / Acentos se houver
                                            val vocalized = item.nativeScriptWithVocal.ifBlank { item.word }
                                            Text(
                                                text = vocalized,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textColor,
                                                fontFamily = FontFamily.Serif,
                                                style = if (currentLanguage.isRtl) {
                                                    LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                                                } else LocalTextStyle.current
                                            )

                                            // Transliteração (Pinyin, Romaji ou Cirílico/Grego/Árabe/Hebraico)
                                            if (item.transliteration.isNotBlank()) {
                                                Text(
                                                    text = "🔤 ${item.transliteration}",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = currentLanguage.badgeColor
                                                )
                                            }

                                            // Fonética IPA
                                            if (item.phoneticIpa.isNotBlank()) {
                                                Text(
                                                    text = item.phoneticIpa,
                                                    fontSize = 12.sp,
                                                    color = subTextColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        // Botão Ouvir Pronúncia Nativa
                                        IconButton(
                                            onClick = { speakCurrentWord() },
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(currentLanguage.badgeColor.copy(alpha = 0.2f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Ouvir",
                                                tint = currentLanguage.badgeColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Badges morfológicas
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (item.partOfSpeech.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White.copy(alpha = 0.08f)
                                            ) {
                                                Text(
                                                    text = item.partOfSpeech,
                                                    fontSize = 11.sp,
                                                    color = textColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (item.genderOrGrammarCase.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = currentLanguage.badgeColor.copy(alpha = 0.15f),
                                                border = BorderStroke(0.6.dp, currentLanguage.badgeColor)
                                            ) {
                                                Text(
                                                    text = item.genderOrGrammarCase,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = currentLanguage.badgeColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (item.rootOrRadical.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                border = BorderStroke(0.6.dp, Color(0xFF10B981))
                                            ) {
                                                Text(
                                                    text = "Raiz/Radical: ${item.rootOrRadical}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Decomposição Composta (Alemão Komposita ou Chinês Composto)
                            if (item.compoundBreakdown.isNotBlank()) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🧩 Decomposição Morfológica:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = currentLanguage.badgeColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.compoundBreakdown,
                                            fontSize = 12.5.sp,
                                            color = textColor
                                        )
                                    }
                                }
                            }

                            // Definição Monolíngue & Tradução Portuguesa
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    if (item.monolingualDefinition.isNotBlank()) {
                                        Text(
                                            text = "Definição Monolíngue (${currentLanguage.nativeName}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = subTextColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.monolingualDefinition,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor,
                                            fontFamily = FontFamily.Serif
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    Text(
                                        text = "Nuance em Português:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = subTextColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.portugueseMeaning,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                }
                            }

                            // Collocações / Expressões Frequentes
                            if (item.collocations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "🔗 Collocations e Expressões Nativas:",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item.collocations.forEach { coll ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = cardBg,
                                            border = BorderStroke(0.6.dp, borderColor.copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable {
                                                speechManager?.speakTextInStudyLanguage(coll, currentLanguage)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = subTextColor, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(coll, fontSize = 11.5.sp, color = textColor)
                                            }
                                        }
                                    }
                                }
                            }

                            // Frase Exemplar do Livro
                            if (item.contextualSentence.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(0.8.dp, currentLanguage.badgeColor.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📖 Exemplo Literário:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = currentLanguage.badgeColor)
                                            IconButton(
                                                onClick = { speechManager?.speakTextInStudyLanguage(item.contextualSentence, currentLanguage) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = currentLanguage.badgeColor, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.contextualSentence, fontSize = 13.sp, color = textColor, fontFamily = FontFamily.Serif)
                                        if (item.sentenceTranslation.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(item.sentenceTranslation, fontSize = 11.5.sp, color = subTextColor)
                                        }

                                        if (onAnalyzeSentenceGrammar != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = currentLanguage.badgeColor.copy(alpha = 0.15f),
                                                border = BorderStroke(0.6.dp, currentLanguage.badgeColor.copy(alpha = 0.45f)),
                                                modifier = Modifier.clickable {
                                                    onAnalyzeSentenceGrammar(item.contextualSentence, currentLanguage)
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountTree,
                                                        contentDescription = null,
                                                        tint = currentLanguage.badgeColor,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "Analisar Sintaxe & Gramática Desta Frase",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = currentLanguage.badgeColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Dica de Ouro C2
                            if (item.usageTipC2.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706).copy(alpha = 0.12f)),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                        Text("💡", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Dica de Fluência C2:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(item.usageTipC2, fontSize = 12.sp, color = textColor)
                                        }
                                    }
                                }
                            }

                            // Desafio Ativo com OpenAI
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "🎯 Desafio Ativo de Construção de Frase:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.activeChallengePrompt.ifBlank { "Escreva uma frase em ${currentLanguage.displayName} usando '${item.word}':" },
                                        fontSize = 12.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = userSentenceInput,
                                        onValueChange = { userSentenceInput = it },
                                        placeholder = { Text("Escreva sua frase em ${currentLanguage.displayName}...", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = {
                                                if (userSentenceInput.isBlank()) {
                                                    Toast.makeText(context, "Digite uma frase para avaliação.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                isEvaluatingSentence = true
                                                sentenceEvalError = null
                                                scope.launch {
                                                    val res = OpenAiService.evaluatePolyglotSentenceC2(
                                                        apiKey = apiKey,
                                                        language = currentLanguage,
                                                        targetWord = item.word,
                                                        userSentence = userSentenceInput
                                                    )
                                                    isEvaluatingSentence = false
                                                    res.onSuccess { reply ->
                                                        sentenceEvaluationResult = reply
                                                    }.onFailure { err ->
                                                        sentenceEvalError = err.localizedMessage
                                                    }
                                                }
                                            },
                                            enabled = !isEvaluatingSentence,
                                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            if (isEvaluatingSentence) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text("Avaliar Frase (IA)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (sentenceEvaluationResult != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            border = BorderStroke(0.8.dp, Color(0xFF10B981)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = sentenceEvaluationResult!!,
                                                fontSize = 12.sp,
                                                color = textColor,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Bottom Footer: Salvar no Dicionário / Glossário
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSaveToDictionary != null && analysis != null) {
                        Button(
                            onClick = {
                                val item = analysis!!
                                val termToSave = PageDictionaryTerm(
                                    bookId = bookId,
                                    pageNumber = pageNumber,
                                    bookTitle = bookTitle.ifBlank { "Livro Poliglota" },
                                    term = item.word,
                                    category = "${currentLanguage.displayName} C2",
                                    originalContext = sentenceContext.ifBlank { item.contextualSentence },
                                    originalDefinition = "${item.transliteration} ${item.phoneticIpa} [${item.cefrLevel}] ${item.monolingualDefinition}\n\n• PT: ${item.portugueseMeaning}\n• Collocations: ${item.collocations.joinToString(", ")}",
                                    englishTerm = item.word,
                                    englishDefinition = item.monolingualDefinition,
                                    englishExample = item.contextualSentence
                                )
                                onSaveToDictionary(termToSave)
                                isSavedToDictionary = true
                                Toast.makeText(context, "'${item.word}' salvo no seu Vocabulário de ${currentLanguage.displayName}!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSavedToDictionary) Color(0xFF10B981) else currentLanguage.badgeColor
                            )
                        ) {
                            Icon(
                                imageVector = if (isSavedToDictionary) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSavedToDictionary) "Salvo no Glossário" else "Salvar no Glossário",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Fechar", color = subTextColor, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
