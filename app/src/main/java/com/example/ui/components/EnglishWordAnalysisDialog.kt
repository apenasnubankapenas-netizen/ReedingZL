package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.EnglishWordAnalysis
import com.example.data.model.PageDictionaryTerm
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Diálogo imersivo de Raio-X Lexical e Domínio C2 de Inglês para qualquer palavra do livro.
 * Oferece fonética IPA, áudio US/UK, definição monolíngue (Oxford style), nuance em português,
 * collocations reais, etimologia e desafio ativo com avaliação imediata da OpenAI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnglishWordAnalysisDialog(
    word: String,
    sentenceContext: String = "",
    bookTitle: String = "",
    bookId: Int = 0,
    pageNumber: Int = 1,
    readingTheme: String = "DARK_OLED",
    category: String? = null,
    categoryColor: String? = null,
    speechManager: PdfSpeechManager? = null,
    onSaveToDictionary: ((PageDictionaryTerm) -> Unit)? = null,
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

    var apiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var showApiKeyInput by remember { mutableStateOf(apiKey.isBlank()) }
    var apiKeyInput by remember { mutableStateOf(apiKey) }

    var analysis by remember { mutableStateOf<EnglishWordAnalysis?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Active Challenge State
    var userSentenceInput by remember { mutableStateOf("") }
    var isEvaluatingSentence by remember { mutableStateOf(false) }
    var sentenceEvaluationResult by remember { mutableStateOf<String?>(null) }
    var sentenceEvalError by remember { mutableStateOf<String?>(null) }
    var isSavedToDictionary by remember { mutableStateOf(false) }

    // Pronunciation function
    fun speakWord(languageCode: String) {
        val target = analysis?.word ?: word
        speechManager?.speakTextImmediately(target)
    }

    // Load analysis from OpenAI
    fun fetchAnalysis(keyToUse: String? = null) {
        val currentKey = keyToUse ?: apiKey
        if (currentKey.isBlank()) {
            showApiKeyInput = true
            errorMessage = "Configure sua Chave de API da OpenAI para gerar a análise C2."
            return
        }

        isLoading = true
        errorMessage = null
        scope.launch {
            val res = OpenAiService.analyzeEnglishWordC2(
                apiKey = currentKey,
                word = word,
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

    // Auto-fetch on mount
    LaunchedEffect(word) {
        if (apiKey.isNotBlank()) {
            fetchAnalysis()
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
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Raio-X Lexical C2",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.18f),
                                    border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = analysis?.cefrLevel ?: "C2 MASTER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = analysis?.cefrBadgeColor ?: Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Domínio pleno de vocabulário, nuance e collocations",
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

                // API Key Configuration Card (if needed)
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
                                text = "Chave OpenAI para Análise C2 e Tutoria:",
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
                                        fetchAnalysis(apiKeyInput.trim())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Salvar Chave", color = palette.buttonContent, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Word Header & Audio Dock
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = analysis?.word?.ifBlank { word } ?: word,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Serif,
                                    color = textColor
                                )
                                if (!analysis?.phoneticIpa.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = analysis!!.phoneticIpa,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = accent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (!analysis?.partOfSpeech.isNullOrBlank()) {
                                    Text(
                                        text = "${analysis!!.partOfSpeech} • ${analysis!!.registerAndTone}",
                                        fontSize = 11.sp,
                                        color = subTextColor,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }

                            // Audio Pronunciation Actions (US & UK)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = { speakWord("en-US") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = accent.copy(alpha = 0.15f),
                                        contentColor = accent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("🇺🇸 US", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { speakWord("en-GB") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = accent.copy(alpha = 0.15f),
                                        contentColor = accent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("🇬🇧 UK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Body
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    if (isLoading) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = accent, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Dissecando fonética, collocations e nuances nativas C2...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    } else if (errorMessage != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Aviso: $errorMessage", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { fetchAnalysis() },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Tentar Novamente", color = palette.buttonContent, fontSize = 12.sp)
                                }
                            }
                        }
                    } else if (analysis != null) {
                        val item = analysis!!

                        // 1. Oxford Monolingual Definition (Imersão em Inglês)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "OXFORD MONOLINGUAL DEFINITION (THINK IN ENGLISH)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = accent,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.englishDefinition,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = palette.divider)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TRADUÇÃO & NUANCE EXATA (PORTUGUÊS)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF10B981),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.portugueseMeaning,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp,
                                    color = textColor.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Collocations Reais (Como os nativos usam juntas)
                        if (item.collocations.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "COLLOCATIONS FREQUENTES (COMBINAÇÕES NATIVAS)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF38BDF8),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item.collocations.forEach { coll ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                                                border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
                                            ) {
                                                Text(
                                                    text = coll,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFBAE6FD),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 3. Sinônimos com Nuance Real
                        if (item.synonymsWithNuance.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SINÔNIMOS & DIFERENÇAS DE NUANCE C2",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFA855F7),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    item.synonymsWithNuance.forEach { syn ->
                                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                            Text(text = "• ", color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                                            Text(text = syn, fontSize = 12.5.sp, color = textColor, lineHeight = 17.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 4. Frase Contextual e Etimologia
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (item.contextualSentence.isNotBlank()) {
                                    Text(
                                        text = "EXEMPLO EM CONTEXTO:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = accent,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${item.contextualSentence}\"",
                                        fontSize = 13.5.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = textColor,
                                        lineHeight = 19.sp
                                    )
                                    if (item.sentenceTranslation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.sentenceTranslation,
                                            fontSize = 11.5.sp,
                                            color = subTextColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                if (item.etymologyRoot.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ETIMOLOGIA & RAÍZES:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.etymologyRoot,
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.85f),
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                if (item.usageTipC2.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = accent.copy(alpha = 0.12f),
                                        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "DICA C2 PARA SOAR NATIVO:",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accent
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.usageTipC2,
                                                fontSize = 12.sp,
                                                color = textColor,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. Desafio Ativo de 1 Frase (Active Recall)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DESAFIO ATIVO: TRANSFORME EM VOCABULÁRIO ATIVO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF38BDF8),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.activeChallengePrompt.ifBlank { "Crie uma frase em inglês usando '$word' para fixar de verdade:" },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = userSentenceInput,
                                    onValueChange = { userSentenceInput = it },
                                    placeholder = { Text("Escreva sua frase em inglês aqui...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            if (userSentenceInput.isNotBlank() && apiKey.isNotBlank()) {
                                                isEvaluatingSentence = true
                                                sentenceEvalError = null
                                                scope.launch {
                                                    val evalRes = OpenAiService.evaluateEnglishSentenceC2(
                                                        apiKey = apiKey,
                                                        targetWord = item.word,
                                                        userSentence = userSentenceInput.trim()
                                                    )
                                                    isEvaluatingSentence = false
                                                    evalRes.onSuccess { evalText ->
                                                        sentenceEvaluationResult = evalText
                                                    }.onFailure { err ->
                                                        sentenceEvalError = err.localizedMessage
                                                    }
                                                }
                                            }
                                        },
                                        enabled = userSentenceInput.isNotBlank() && !isEvaluatingSentence,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        if (isEvaluatingSentence) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Avaliando...", fontSize = 11.5.sp)
                                        } else {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Avaliar com Tutor C2", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Evaluation Feedback Box
                                if (sentenceEvaluationResult != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Feedback do Tutor C2:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = sentenceEvaluationResult!!,
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.95f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy Action
                    OutlinedButton(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val textToCopy = buildString {
                                appendLine("Word: ${analysis?.word ?: word} (${analysis?.cefrLevel ?: "C2"})")
                                appendLine("IPA: ${analysis?.phoneticIpa ?: ""}")
                                appendLine("Definition: ${analysis?.englishDefinition ?: ""}")
                                appendLine("Português: ${analysis?.portugueseMeaning ?: ""}")
                                if (analysis?.collocations?.isNotEmpty() == true) {
                                    appendLine("Collocations: ${analysis?.collocations?.joinToString(", ")}")
                                }
                            }
                            clip.setPrimaryClip(ClipData.newPlainText("C2 Word Analysis", textToCopy))
                            Toast.makeText(context, "Análise copiada!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar", fontSize = 11.5.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Save to dictionary button
                        if (onSaveToDictionary != null && analysis != null) {
                            Button(
                                onClick = {
                                    val item = analysis!!
                                    val termToSave = PageDictionaryTerm(
                                        bookId = bookId,
                                        pageNumber = pageNumber,
                                        bookTitle = bookTitle.ifBlank { "Livro" },
                                        term = item.word,
                                        category = item.partOfSpeech.ifBlank { "Inglês C2" },
                                        originalContext = sentenceContext.ifBlank { item.contextualSentence },
                                        originalDefinition = "${item.phoneticIpa} [${item.cefrLevel}] ${item.englishDefinition}\n\n• PT: ${item.portugueseMeaning}\n• Collocations: ${item.collocations.joinToString(", ")}",
                                        englishTerm = item.word,
                                        englishDefinition = item.englishDefinition,
                                        englishExample = item.contextualSentence
                                    )
                                    onSaveToDictionary(termToSave)
                                    isSavedToDictionary = true
                                    Toast.makeText(context, "'${item.word}' salvo no seu Vocabulário!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSavedToDictionary) Color(0xFF10B981) else Color(0xFF6366F1)
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
}
