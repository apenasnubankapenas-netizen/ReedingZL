package com.example.ui.components

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SegmentChatMessage
import com.example.data.openai.OpenAiService
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card de Chat Interativo com OpenAI para cada etapa/sessão do texto.
 * Design profissional, refinado e compacto, com:
 * - Botões menores e minimalistas
 * - Tamanho de texto personalizável (Pequeno, Médio, Grande)
 * - Textos das explicações em itálico e justificados
 * - Sem asteriscos nem respostas genéricas
 * - Leitura em voz alta (TTS) com opções de idioma (Português, Espanhol, Francês e Inglês)
 * - Tradução instantânea de respostas para 4 idiomas
 */
@Composable
fun SegmentAiChatCard(
    segmentIndex: Int,
    segmentText: String,
    messages: List<SegmentChatMessage>,
    isLoading: Boolean,
    errorMessage: String? = null,
    isMinimized: Boolean,
    isDialogMode: Boolean = false,
    readingTheme: String = "DARK_OLED",
    category: String? = null,
    categoryColor: String? = null,
    modifier: Modifier = Modifier,
    onToggleMinimize: () -> Unit,
    onSendMessage: (question: String, isAudio: Boolean) -> Unit,
    onClearChat: () -> Unit,
    onDeleteMessage: (SegmentChatMessage) -> Unit = {},
    onDismiss: () -> Unit,
    onSpeakTextInLang: (langCode: String, text: String) -> Unit = { _, _ -> },
    onStopSpeaking: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }

    // Tamanho personalizável de fonte do chat: "compact" (11sp), "normal" (12.5sp), "large" (14sp)
    var chatFontScale by remember { mutableStateOf("normal") }
    val contentFontSize = when (chatFontScale) {
        "compact" -> 11.sp
        "large" -> 14.sp
        else -> 12.5.sp
    }
    val contentLineHeight = when (chatFontScale) {
        "compact" -> 16.sp
        "large" -> 20.sp
        else -> 18.sp
    }

    // Cache local de traduções por mensagem: Map<messageId_langCode, translatedText>
    val translationsCache = remember { mutableStateMapOf<String, String>() }
    val translatingMap = remember { mutableStateMapOf<String, Boolean>() }

    val listState = rememberLazyListState()

    // Rola suavemente até o final quando novas mensagens chegam
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Launcher para captura de fala e transcrição automática por áudio
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenTextList?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
                onSendMessage(spoken, true)
                inputText = ""
                Toast.makeText(context, "Pergunta de voz enviada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cores dinâmicas de acordo com o tema de leitura escolhido
    val palette = remember(readingTheme, category, categoryColor) {
        ReadingThemeManager.getPalette(readingTheme, category, categoryColor)
    }

    val cardBg = palette.surfaceVariant
    val textColor = palette.textPrimary
    val openAiAccent = palette.accent
    val borderColor = palette.cardBorder.takeIf { it != Color.Transparent } ?: palette.accent.copy(alpha = 0.40f)

    // DIÁLOGO DE CONFIGURAÇÃO DE CHAVE OPENAI
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = openAiAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chave de API de IA", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Para que o tutor responda suas dúvidas profundas sobre o texto, insira sua chave de IA:",
                        fontSize = 12.5.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("sk-proj-...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        OpenAiService.saveApiKey(context, apiKeyInput.trim())
                        showApiKeyDialog = false
                        Toast.makeText(context, "Chave salva com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Salvar", color = openAiAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO DE CONFIRMAÇÃO PARA LIMPAR CHAT
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpar Conversa Desta Etapa?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Todas as mensagens e análises desta etapa serão removidas do aparelho.",
                    fontSize = 12.5.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearChat()
                        showClearDialog = false
                        Toast.makeText(context, "Histórico limpo.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- SE O CHAT ESTIVER MINIMIZADO ---
    if (isMinimized) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 1.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clickable { onToggleMinimize() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(openAiAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            tint = openAiAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = if (messages.isNotEmpty()) "Chat do trecho (${messages.size})" else "Chat do trecho (Minimizado)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botão compacto expandir
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = openAiAccent.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, openAiAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onToggleMinimize() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expandir",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = openAiAccent
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expandir",
                                tint = openAiAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar chat",
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
        return
    }

    // --- CHAT TOTALMENTE EXPANDIDO ---
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = if (isDialogMode) {
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(10.dp)
            }
        ) {
            // CABEÇALHO COMPACTO E PROFISSIONAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(openAiAccent.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = openAiAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Column {
                        Text(
                            text = "Tutor da Etapa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            letterSpacing = 0.1.sp
                        )
                        Text(
                            text = "Análise e dúvidas sobre o parágrafo",
                            fontSize = 9.5.sp,
                            color = textColor.copy(alpha = 0.65f)
                        )
                    }
                }

                // CONTROLES SUPERIORES (TAMANHO DE FONTE, MINIMIZAR, LIMPAR, FECHAR)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Alternador compacto de tamanho de fonte
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = textColor.copy(alpha = 0.08f),
                        modifier = Modifier.clickable {
                            chatFontScale = when (chatFontScale) {
                                "compact" -> "normal"
                                "normal" -> "large"
                                else -> "compact"
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Tamanho do texto",
                                tint = textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = when (chatFontScale) {
                                    "compact" -> "P"
                                    "large" -> "G"
                                    else -> "M"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // Botão compacto Minimizar
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = openAiAccent.copy(alpha = 0.12f),
                        border = BorderStroke(0.7.dp, openAiAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onToggleMinimize() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Minimizar",
                                tint = openAiAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Minimizar",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = openAiAccent
                            )
                        }
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Limpar conversa",
                                tint = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // RECORTE DA SESSÃO/ETAPA DO TEXTO (JUSTIFICADO E EM ITÁLICO)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = textColor.copy(alpha = 0.04f),
                border = BorderStroke(0.7.dp, textColor.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = openAiAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = segmentText.trim(),
                        fontSize = 10.5.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = textColor.copy(alpha = 0.80f),
                        textAlign = TextAlign.Justify,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // AVISO CASO A CHAVE OPENAI NÃO ESTEJA CONFIGURADA
            val savedKey = OpenAiService.getSavedApiKey(context)
            if (savedKey.isBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFA000).copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, Color(0xFFFFA000).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showApiKeyDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "Chave de IA não configurada",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                        Text(
                            "Configurar",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA000)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // LISTA DE MENSAGENS SALVAS NO DISPOSITIVO (Rolável suavemente)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (messages.isEmpty() && !isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = openAiAccent.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nenhuma indagação nesta etapa ainda.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor.copy(alpha = 0.65f)
                            )
                            Text(
                                text = "Grave um áudio no microfone ou digite sua dúvida.",
                                fontSize = 10.5.sp,
                                color = textColor.copy(alpha = 0.50f)
                            )
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        val isAssistant = !msg.sender.equals("USER", ignoreCase = true)
                        var activeLang by remember(msg.id) { mutableStateOf("pt") }

                        val cacheKey = "${msg.id}_$activeLang"
                        val currentText = if (activeLang == "pt" || !isAssistant) {
                            msg.messageText
                        } else {
                            translationsCache[cacheKey] ?: msg.messageText
                        }

                        val isTranslatingThis = translatingMap[cacheKey] == true

                        ProfessionalChatMessageBubble(
                            message = msg,
                            displayedText = currentText,
                            activeLanguage = activeLang,
                            isTranslating = isTranslatingThis,
                            fontSize = contentFontSize,
                            lineHeight = contentLineHeight,
                            textColor = textColor,
                            openAiAccent = openAiAccent,
                            onDelete = { onDeleteMessage(msg) },
                            onSpeak = { langCode ->
                                onSpeakTextInLang(langCode, currentText)
                            },
                            onSelectLanguage = { langCode ->
                                activeLang = langCode
                                if (langCode != "pt" && !translationsCache.containsKey(cacheKey)) {
                                    translatingMap[cacheKey] = true
                                    coroutineScope.launch {
                                        val translated = translateTextViaOpenAi(
                                            apiKey = savedKey,
                                            text = msg.messageText,
                                            targetLang = langCode
                                        )
                                        translationsCache[cacheKey] = translated
                                        translatingMap[cacheKey] = false
                                    }
                                }
                            }
                        )
                    }

                    if (isLoading) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = openAiAccent.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, openAiAccent.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = openAiAccent,
                                        strokeWidth = 1.8.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Elaborando análise minuciosa...",
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = openAiAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MENSAGEM DE ERRO SE HOUVER
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.20f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        if (errorMessage.contains("Chave", ignoreCase = true) || errorMessage.contains("API", ignoreCase = true)) {
                            TextButton(onClick = { showApiKeyDialog = true }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("Chave", fontSize = 10.sp, color = openAiAccent)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // BARRA DE ENTRADA COMPACTA: TEXTO + MICROFONE + ENVIAR COM BOTÕES MENORES
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Dúvida sobre a etapa (ou toque no mic)...",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.45f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = openAiAccent,
                        unfocusedBorderColor = textColor.copy(alpha = 0.18f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    maxLines = 2,
                    singleLine = false,
                    trailingIcon = {
                        // BOTÃO MENOR DE ÁUDIO / MICROFONE
                        IconButton(
                            onClick = {
                                try {
                                    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua dúvida sobre este trecho...")
                                    }
                                    speechRecognizerLauncher.launch(speechIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Reconhecimento de voz indisponível no aparelho.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Falar por áudio",
                                tint = openAiAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.width(5.dp))

                // BOTÃO MENOR E COMPACTO DE ENVIAR
                val canSend = inputText.isNotBlank() && !isLoading
                Surface(
                    shape = CircleShape,
                    color = if (canSend) openAiAccent else textColor.copy(alpha = 0.10f),
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canSend) {
                            val textToSend = inputText.trim()
                            if (textToSend.isNotBlank()) {
                                onSendMessage(textToSend, false)
                                inputText = ""
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar pergunta",
                            tint = if (canSend) palette.buttonContent else textColor.copy(alpha = 0.30f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bolha de Mensagem Refinada:
 * - Textos das explicações justificados e em itálico
 * - Sem asteriscos nem marcas de markdown desnecessárias
 * - Botões compactos para ouvir voz nos idiomas PT, ES, EN e FR
 * - Seletor compacto de idioma para tradução
 */
@Composable
private fun ProfessionalChatMessageBubble(
    message: SegmentChatMessage,
    displayedText: String,
    activeLanguage: String,
    isTranslating: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    textColor: Color,
    openAiAccent: Color,
    onDelete: () -> Unit,
    onSpeak: (langCode: String) -> Unit,
    onSelectLanguage: (langCode: String) -> Unit
) {
    val context = LocalContext.current
    val isUser = message.sender.equals("USER", ignoreCase = true)
    val timeStr = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    // Normaliza texto removendo quaisquer asteriscos residuais e quebras excessivas
    val cleanText = remember(displayedText) {
        displayedText
            .replace(Regex("\\*{1,3}"), "")
            .replace(Regex("(?m)^#{1,6}\\s*"), "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = if (isUser) 10.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 10.dp
            ),
            color = if (isUser) openAiAccent.copy(alpha = 0.16f) else textColor.copy(alpha = 0.06f),
            border = BorderStroke(
                0.8.dp,
                if (isUser) openAiAccent.copy(alpha = 0.35f) else textColor.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.98f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                // Barra superior da bolha: Nome do emissor + badge de voz + hora + botões menores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isUser) "Você" else "Tutor Literário",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = openAiAccent
                        )
                        if (message.isAudioInput) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = openAiAccent.copy(alpha = 0.16f),
                                modifier = Modifier.padding(horizontal = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Áudio",
                                        tint = openAiAccent,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Text(
                                        "Voz",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = openAiAccent
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = timeStr,
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.40f)
                        )

                        if (!isUser) {
                            // Botão ouvir voz no idioma ativo
                            IconButton(
                                onClick = { onSpeak(activeLanguage) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Ouvir em voz alta",
                                    tint = openAiAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            // Botão copiar resposta limpa
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Resposta Tutor", cleanText))
                                    Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar resposta",
                                    tint = textColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Botão apagar mensagem
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Apagar mensagem",
                                tint = textColor.copy(alpha = 0.35f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Texto da mensagem: Justificado e em itálico para ar acadêmico e literário
                SelectionContainer {
                    Text(
                        text = cleanText,
                        fontSize = fontSize,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = textColor,
                        lineHeight = lineHeight,
                        textAlign = TextAlign.Justify
                    )
                }

                // IDIOMAS E TRADUÇÃO (PT, ES, EN, FR) PARA RESPOSTAS DO TUTOR
                if (!isUser) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Chips menores de idiomas
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            listOf("pt" to "PT", "es" to "ES", "en" to "EN", "fr" to "FR").forEach { (code, label) ->
                                val isSelected = activeLanguage == code
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) openAiAccent.copy(alpha = 0.22f) else textColor.copy(alpha = 0.05f),
                                    border = BorderStroke(
                                        0.6.dp,
                                        if (isSelected) openAiAccent else textColor.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.clickable { onSelectLanguage(code) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) openAiAccent else textColor.copy(alpha = 0.65f),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Indicador se estiver traduzindo sob demanda
                        if (isTranslating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(9.dp),
                                    color = openAiAccent,
                                    strokeWidth = 1.2.dp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Traduzindo...",
                                    fontSize = 9.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = openAiAccent
                                )
                            }
                        } else {
                            // Botão rápido de ouvir no idioma selecionado
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onSpeak(activeLanguage) }
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Ouvir neste idioma",
                                    tint = openAiAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Ouvir em ${activeLanguage.uppercase()}",
                                    fontSize = 9.sp,
                                    color = openAiAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Função utilitária para traduzir uma resposta para Espanhol, Francês ou Inglês sob demanda via OpenAI
 */
private suspend fun translateTextViaOpenAi(
    apiKey: String,
    text: String,
    targetLang: String
): String = withContext(Dispatchers.IO) {
    val cleanKey = apiKey.trim()
    if (cleanKey.isBlank() || text.isBlank()) return@withContext text

    val langName = when (targetLang.lowercase()) {
        "es" -> "Espanhol"
        "en" -> "Inglês"
        "fr" -> "Francês"
        else -> "Português"
    }

    try {
        val systemPrompt = """
            Você é um tradutor literário e acadêmico fluente.
            Traduza o seguinte texto para o idioma $langName.
            Mantenha o tom refinado, rigor conceituai e elegante.
            NÃO use asteriscos, hashtags nem marcadores. Responda apenas com a tradução em prosa fluida.
        """.trimIndent()

        val payload = org.json.JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", 0.3)
            put("messages", org.json.JSONArray().apply {
                put(org.json.JSONObject().put("role", "system").put("content", systemPrompt))
                put(org.json.JSONObject().put("role", "user").put("content", text))
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toString().toRequestBody(mediaType)
        val request = okhttp3.Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $cleanKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = okhttp3.OkHttpClient()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext text
            val resObj = org.json.JSONObject(body)
            val translated = resObj.optJSONArray("choices")?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: text
            translated
                .replace(Regex("\\*{1,3}"), "")
                .replace(Regex("(?m)^#{1,6}\\s*"), "")
                .trim()
        }
    } catch (e: Exception) {
        text
    }
}
