package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyLanguage
import com.example.data.openai.OpenAiService
import com.example.data.openai.OpenAiSpeechCache
import com.example.data.speech.PdfSpeechManager
import com.example.data.speech.VoiceEnginePreferences
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * Componente interativo que exibe o texto de Notas e Termos da Página com:
 * 1. Detecção automática de vozes instaladas no telefone vs Vozes da OpenAI.
 * 2. Bloquinho flutuante de escolha (Voz do Telefone ou Voz da OpenAI com vozes aleatórias masculinas e femininas)
 *    com opção de salvar e lembrar a preferência.
 * 3. Geração e cache local permanente da voz da OpenAI para qualquer idioma.
 * 4. Destaque palavra por palavra (efeito karaokê) sincronizado em tempo real.
 * 5. Compatibilidade total com aceleração/desaceleração de velocidade (setSpeed).
 * 6. Clique em qualquer palavra para saltar a reprodução (seek) imediatamente.
 */
@Composable
fun KaraokeDefinitionText(
    targetId: String,
    text: String,
    speechManager: PdfSpeechManager?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    prefixLabel: String? = null,
    textColor: Color = Color(0xFFE2E8F0),
    fontSize: TextUnit = 12.5.sp,
    lineHeight: TextUnit = 17.5.sp,
    highlightBgColor: Color = Color(0xFFFBBF24).copy(alpha = 0.45f),
    highlightTextColor: Color = Color(0xFFFEF08A),
    showListenButton: Boolean = true,
    language: StudyLanguage? = null,
    requestPlayToken: Int = 0,
    onFallbackSpeak: ((langCode: String, text: String) -> Unit)? = null
) {
    val cleanText = remember(text) { text.replace("~~", "").trim() }
    if (cleanText.isBlank()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isSpeaking by speechManager?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
    val activeTargetId by speechManager?.activeTargetId?.collectAsState() ?: remember { mutableStateOf(null) }
    val activeWordIndex by speechManager?.activeWordIndex?.collectAsState() ?: remember { mutableStateOf(-1) }

    val isThisActive = isSpeaking && activeTargetId == targetId
    val tokens = remember(cleanText) { PdfSpeechManager.tokenizeSentence(cleanText) }

    var isGeneratingAudio by remember { mutableStateOf(false) }
    var showVoiceEngineChoiceDialog by remember { mutableStateOf(false) }
    var pendingPlayOffset by remember { mutableIntStateOf(0) }

    val resolvedLanguage = remember(cleanText, language) {
        if (language != null) {
            language
        } else {
            val detected = speechManager?.detectLanguageLocale(cleanText) ?: Locale("pt", "BR")
            StudyLanguage.allSupportedLanguages.firstOrNull {
                it.code.equals(detected.language, ignoreCase = true) ||
                it.ttsLocale.language.equals(detected.language, ignoreCase = true)
            } ?: StudyLanguage.PORTUGUESE
        }
    }

    fun playWithPhoneVoice(offset: Int) {
        if (offset > 0) {
            speechManager?.speakFromWordInTarget(targetId, cleanText, offset)
        } else {
            speechManager?.startReadingTarget(targetId, cleanText, offset)
        }
    }

    fun playWithOpenAiVoice(offset: Int) {
        isGeneratingAudio = true
        scope.launch {
            val apiKey = OpenAiService.getApiKey(context)
            val audioRes = OpenAiSpeechCache.getOrGenerateAudio(
                context = context,
                text = cleanText,
                langCode = resolvedLanguage.code,
                blockId = targetId,
                customApiKey = apiKey
            )
            isGeneratingAudio = false
            if (audioRes.isSuccess) {
                val audioFile = audioRes.getOrThrow()
                speechManager?.playOpenAiAudio(
                    audioFile = audioFile,
                    lineId = targetId,
                    text = cleanText,
                    startWordOffset = offset
                )
            } else {
                val msg = audioRes.exceptionOrNull()?.message ?: "Erro ao gerar voz OpenAI"
                Toast.makeText(context, "$msg. Usando voz do sistema.", Toast.LENGTH_SHORT).show()
                playWithPhoneVoice(offset)
            }
        }
    }

    fun handlePlayRequest(offset: Int) {
        if (speechManager == null) {
            onFallbackSpeak?.invoke(resolvedLanguage.code, cleanText)
            return
        }

        // Se já estiver reproduzindo e o usuário clicar em uma palavra, salta direto no player OpenAI se estiver ativo
        if (isThisActive && speechManager.seekOpenAiToWord(targetId, offset)) {
            return
        }

        val phoneVoiceAvailable = speechManager.isLanguageAvailableOnDevice(resolvedLanguage.ttsLocale)
        val savedPref = VoiceEnginePreferences.getPreferredEngine(context, resolvedLanguage.code)

        if (!phoneVoiceAvailable) {
            // Se o telefone não tiver voz pré-instalada para este idioma -> Usa voz da OpenAI automaticamente!
            playWithOpenAiVoice(offset)
        } else if (savedPref == VoiceEngineChoice.OPENAI) {
            // Preferência previamente salva pelo usuário para OpenAI
            playWithOpenAiVoice(offset)
        } else if (savedPref == VoiceEngineChoice.PHONE) {
            // Preferência previamente salva pelo usuário para voz do telefone
            playWithPhoneVoice(offset)
        } else {
            // Pergunta através do bloquinho flutuante antes de reproduzir
            pendingPlayOffset = offset
            showVoiceEngineChoiceDialog = true
        }
    }

    var lastHandledToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(requestPlayToken) {
        if (requestPlayToken > 0 && requestPlayToken != lastHandledToken) {
            lastHandledToken = requestPlayToken
            if (isThisActive) {
                speechManager?.stopTarget(targetId)
            } else {
                handlePlayRequest(0)
            }
        }
    }

    val annotatedString = remember(cleanText, tokens, isThisActive, activeWordIndex, textColor, highlightBgColor, highlightTextColor) {
        buildAnnotatedString {
            append(cleanText)
            if (isThisActive && tokens.isNotEmpty() && activeWordIndex in tokens.indices) {
                val currentToken = tokens[activeWordIndex]
                addStyle(
                    style = SpanStyle(
                        background = highlightBgColor,
                        color = highlightTextColor,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    start = currentToken.startChar,
                    end = currentToken.endChar
                )
            }
        }
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (prefixLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = prefixLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )

                if (showListenButton) {
                    AudioControlChip(
                        isThisActive = isThisActive,
                        isGeneratingAudio = isGeneratingAudio,
                        accentColor = accentColor,
                        onToggle = {
                            if (isThisActive) {
                                speechManager?.stopTarget(targetId)
                            } else {
                                handlePlayRequest(0)
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Se não houver prefixLabel, coloca o botão de áudio na lateral ou inline
            if (prefixLabel == null && showListenButton) {
                AudioControlChip(
                    isThisActive = isThisActive,
                    isGeneratingAudio = isGeneratingAudio,
                    accentColor = accentColor,
                    modifier = Modifier.padding(end = 6.dp, top = 2.dp),
                    onToggle = {
                        if (isThisActive) {
                            speechManager?.stopTarget(targetId)
                        } else {
                            handlePlayRequest(0)
                        }
                    }
                )
            }

            Text(
                text = annotatedString,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = textColor,
                textAlign = TextAlign.Justify,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(cleanText, tokens) {
                        detectTapGestures(
                            onTap = { pos ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                val charOffset = layout.getOffsetForPosition(pos)
                                val clickedToken = tokens.firstOrNull { charOffset in it.startChar until it.endChar }
                                    ?: tokens.minByOrNull { abs(charOffset - it.startChar) }

                                if (clickedToken != null) {
                                    handlePlayRequest(clickedToken.index)
                                }
                            }
                        )
                    }
            )
        }
    }

    // Bloquinho flutuante de escolha entre Voz do Telefone e Voz da OpenAI
    if (showVoiceEngineChoiceDialog) {
        VoiceEngineChoiceDialog(
            languageName = resolvedLanguage.displayName,
            languageFlag = resolvedLanguage.flag,
            onChooseEngine = { choice, rememberChoice ->
                if (rememberChoice) {
                    VoiceEnginePreferences.setPreferredEngine(context, resolvedLanguage.code, choice)
                }
                showVoiceEngineChoiceDialog = false
                if (choice == VoiceEngineChoice.OPENAI) {
                    playWithOpenAiVoice(pendingPlayOffset)
                } else {
                    playWithPhoneVoice(pendingPlayOffset)
                }
            },
            onDismissRequest = {
                showVoiceEngineChoiceDialog = false
            }
        )
    }
}

@Composable
private fun AudioControlChip(
    isThisActive: Boolean,
    isGeneratingAudio: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isThisActive) Color(0xFFF59E0B).copy(alpha = 0.25f) else accentColor.copy(alpha = 0.12f),
        border = BorderStroke(
            0.8.dp,
            if (isThisActive) Color(0xFFF59E0B) else accentColor.copy(alpha = 0.35f)
        ),
        modifier = modifier.size(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isGeneratingAudio) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.6.dp,
                    color = accentColor
                )
            } else {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (isThisActive) Icons.Default.Pause else Icons.Default.VolumeUp,
                        contentDescription = if (isThisActive) "Pausar leitura" else "Ouvir com voz",
                        tint = if (isThisActive) Color(0xFFFBBF24) else accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
