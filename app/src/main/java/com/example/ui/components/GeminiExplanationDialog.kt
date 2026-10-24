package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeminiNote
import com.example.data.openai.OpenAiService
import com.example.ui.theme.ReadingThemeManager
import kotlinx.coroutines.launch

@Composable
fun GeminiExplanationDialog(
    termOrSentence: String,
    fullContext: String,
    translationContext: String? = null,
    cueIndex: Int,
    existingNote: GeminiNote? = null,
    isStarred: Boolean = false,
    readingTheme: String = "DARK_OLED",
    category: String? = null,
    categoryColor: String? = null,
    onToggleStar: () -> Unit = {},
    onSaveNote: (explanation: String) -> Unit,
    onDeleteNote: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val palette = remember(readingTheme, category, categoryColor) {
        ReadingThemeManager.getPalette(readingTheme, category, categoryColor)
    }
    val accentColor = palette.accent
    val textColor = palette.textPrimary
    val subTextColor = palette.textSecondary
    val cardBg = palette.surface
    val borderColor = palette.cardBorder.takeIf { it != Color.Transparent } ?: accentColor.copy(alpha = 0.40f)

    var savedApiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var apiKeyInput by remember { mutableStateOf(savedApiKey) }
    var showApiKeyInput by remember { mutableStateOf(savedApiKey.isBlank()) }

    var responseText by remember { mutableStateOf(existingNote?.geminiResponse ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun fetchOpenAiExplanation(keyToUse: String? = null) {
        val currentKey = keyToUse ?: savedApiKey
        if (currentKey.isBlank()) {
            showApiKeyInput = true
            errorMessage = "Configure sua Chave de API da OpenAI para gerar a explicação."
            return
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = OpenAiService.explainTermOrSentence(
                context = context,
                termOrSentence = termOrSentence,
                fullContext = fullContext,
                translationContext = translationContext,
                customApiKey = currentKey
            )
            isLoading = false
            res.onSuccess { text ->
                responseText = text
                onSaveNote(text)
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Erro ao consultar OpenAI"
            }
        }
    }

    // Auto-fetch if no saved response exists yet and API key is available
    LaunchedEffect(existingNote, savedApiKey) {
        if (existingNote != null && existingNote.geminiResponse.isNotBlank()) {
            responseText = existingNote.geminiResponse
        } else if (responseText.isBlank() && !isLoading && savedApiKey.isNotBlank()) {
            fetchOpenAiExplanation()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceVariant,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
        title = {
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
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.75f))
                                )
                            )
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = palette.buttonContent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Explicação OpenAI AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (existingNote != null || responseText.isNotBlank()) "✨ Salvo na Memória do Telefone" else "Consulta OpenAI em tempo real",
                            fontSize = 11.sp,
                            color = if (existingNote != null || responseText.isNotBlank()) accentColor else subTextColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showApiKeyInput = !showApiKeyInput },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Chave da API",
                            tint = if (savedApiKey.isNotBlank()) accentColor else Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.BookmarkBorder,
                            contentDescription = "Favoritar com Estrela",
                            tint = if (isStarred) Color(0xFFFFB300) else subTextColor
                        )
                    }
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(scrollState)
            ) {
                // Optional API Key Config Panel
                AnimatedVisibility(visible = showApiKeyInput) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBg
                        ),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🔑 Chave de API da OpenAI (sk-...)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("OpenAI API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        OpenAiService.saveApiKey(context, apiKeyInput.trim())
                                        savedApiKey = apiKeyInput.trim()
                                        showApiKeyInput = false
                                        Toast.makeText(context, "Chave OpenAI salva com sucesso!", Toast.LENGTH_SHORT).show()
                                        if (responseText.isBlank()) {
                                            fetchOpenAiExplanation(apiKeyInput.trim())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Salvar Chave", color = palette.buttonContent, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Header context card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBg
                    ),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TERMO / SEÇÃO MARCADA:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = accentColor.copy(alpha = 0.15f),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "Seção #$cueIndex",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = termOrSentence,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        if (fullContext != termOrSentence && fullContext.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Contexto Completo: \"$fullContext\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        if (!translationContext.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tradução: $translationContext",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Loading State
                if (isLoading) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = accentColor.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = accentColor,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Consultando OpenAI sobre o significado & exemplos...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
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
                            Text(
                                text = "Aviso: $errorMessage",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { fetchOpenAiExplanation() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Tentar Novamente", color = palette.buttonContent, fontSize = 12.sp)
                            }
                        }
                    }
                } else if (responseText.isNotBlank()) {
                    // Response Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBg
                        ),
                        border = BorderStroke(1.2.dp, borderColor),
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
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Análise e Explicação da OpenAI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Salvo no Telefone",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = responseText,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                color = textColor
                            )
                        }
                    }
                } else if (savedApiKey.isBlank()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Insira sua Chave de API da OpenAI acima para gerar explicações automáticas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = subTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy action
                    OutlinedButton(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val contentToCopy = "Termo / Seção: $termOrSentence\n\n$responseText"
                            clip.setPrimaryClip(ClipData.newPlainText("OpenAI Explicação", contentToCopy))
                            Toast.makeText(context, "Explicação copiada!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        enabled = responseText.isNotBlank()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar", fontSize = 12.sp, color = textColor)
                    }

                    // Reconsult OpenAI
                    OutlinedButton(
                        onClick = {
                            fetchOpenAiExplanation()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atualizar", fontSize = 12.sp, color = textColor)
                    }
                }

                if (existingNote != null || responseText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            onDeleteNote()
                            responseText = ""
                            Toast.makeText(context, "Explicação removida da memória.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir Explicação Salva", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Concluir", color = palette.buttonContent)
            }
        }
    )
}
