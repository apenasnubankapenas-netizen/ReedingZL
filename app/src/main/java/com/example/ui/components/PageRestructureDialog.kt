package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PdfBook
import com.example.data.model.ScientificDiagramItem
import com.example.data.model.ScientificFormulaItem
import com.example.data.model.ScientificPageReconstruction
import com.example.data.model.ScientificSymbolItem
import com.example.data.openai.OpenAiService
import com.example.data.pdf.PdfRendererHelper
import com.example.data.speech.PdfSpeechManager
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modos de visualização do diálogo de restauração:
 * - TEXTBOOK_RENDERED: Formato de livro de ensino dos alunos na vida real (fórmulas em caixas destacadas, reações químicas, biologia e lógica)
 * - RAW_EDITOR: Editor de texto contínuo e salvo na página do leitor
 */
enum class PageDialogDisplayMode {
    TEXTBOOK_RENDERED,
    RAW_EDITOR
}

@Composable
fun PageRestructureDialog(
    book: PdfBook,
    pageNumber: Int,
    currentRawText: String,
    speechManager: PdfSpeechManager?,
    viewModel: MainViewModel? = null,
    onDismiss: () -> Unit,
    onApplyRestructuredText: (newText: String) -> Unit,
    onResetToOriginal: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(OpenAiService.getApiKey(context)) }
    var showApiKeyConfig by remember { mutableStateOf(apiKey.isBlank()) }
    var selectedModel by remember { mutableStateOf("gpt-4o-mini") }

    var isProcessing by remember { mutableStateOf(false) }
    var processingAction by remember { mutableStateOf<String?>(null) } // "SCIENTIFIC" ou "CLASSIC"
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // Modo de exibição: Livro didático renderizado ou Editor de texto bruto
    var displayMode by remember { mutableStateOf(PageDialogDisplayMode.TEXTBOOK_RENDERED) }

    // Reconstrução científica em memória (se disponível)
    var scientificReconstruction by remember { mutableStateOf<ScientificPageReconstruction?>(null) }

    // Verifica se já existe texto customizado salvo no armazenamento interno do aparelho
    var hasCustomSaved by remember {
        mutableStateOf(PdfRendererHelper.hasCustomPageText(context, book.id, pageNumber))
    }

    var editableText by remember {
        val saved = PdfRendererHelper.getSavedCustomPageText(context, book.id, pageNumber)
        mutableStateOf(if (!saved.isNullOrBlank()) saved else currentRawText.trim())
    }

    data class RestoreLanguageOption(
        val code: String,
        val displayName: String,
        val flag: String
    )

    val restoreLanguages = remember {
        listOf(
            RestoreLanguageOption("pt", "Português", "🇧🇷"),
            RestoreLanguageOption("en", "Inglês", "🇺🇸"),
            RestoreLanguageOption("es", "Espanhol", "🇪🇸"),
            RestoreLanguageOption("fr", "Francês", "🇫🇷"),
            RestoreLanguageOption("de", "Alemão", "🇩🇪"),
            RestoreLanguageOption("it", "Italiano", "🇮🇹")
        )
    }

    var selectedRestoreLang by remember { mutableStateOf(restoreLanguages[0]) }
    var pageTranslationRecord by remember { mutableStateOf<com.example.data.model.PageMultiTranslation?>(null) }

    // Carrega reconstrução científica e traduções salvas no banco se houver
    LaunchedEffect(book.id, pageNumber) {
        if (viewModel != null) {
            val existing = viewModel.getScientificReconstructionForPage(book.id, pageNumber).firstOrNull()
            if (existing != null) {
                scientificReconstruction = existing
            }
            viewModel.getTranslationsForBook(book.id).collect { list ->
                pageTranslationRecord = list.firstOrNull { it.pageNumber == pageNumber }
            }
        }
    }

    // Para o áudio de prévia caso o diálogo seja fechado
    DisposableEffect(Unit) {
        onDispose {
            if (isPreviewPlaying) {
                speechManager?.stop()
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (isPreviewPlaying) speechManager?.stop()
            onDismiss()
        },
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
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(Color(0xFF10A37F), Color(0xFF0284C7))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Restaurar & Traduzir Página",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pág. $pageNumber de ${book.totalPages} • ${book.title}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showApiKeyConfig = !showApiKeyConfig },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar API OpenAI",
                            tint = if (showApiKeyConfig) Color(0xFF10A37F) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isPreviewPlaying) speechManager?.stop()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card de status de persistência (Fixo no PDF / Salvo no Aparelho)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasCustomSaved) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (hasCustomSaved) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasCustomSaved) Icons.Default.Check else Icons.Default.History,
                                contentDescription = null,
                                tint = if (hasCustomSaved) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasCustomSaved) "Página restaurada e salva no aparelho" else "Texto original da extração do PDF",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasCustomSaved) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (hasCustomSaved) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        PdfRendererHelper.deleteCustomPageText(context, book.id, pageNumber)
                                        hasCustomSaved = false
                                        val rawOrig = PdfRendererHelper.extractRawOriginalTextFromPage(context, book, pageNumber)
                                        editableText = if (rawOrig.isNotBlank()) rawOrig else currentRawText
                                        scientificReconstruction = null
                                        onResetToOriginal()
                                        Toast.makeText(context, "Restaurado texto bruto original da página", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Reverter",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Configuração da API OpenAI (Expansível ou exibida se chave vazia)
                AnimatedVisibility(visible = showApiKeyConfig) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        border = BorderStroke(1.dp, Color(0xFF10A37F).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔑 Configuração da API OpenAI",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10A37F)
                                )
                                if (apiKey.isNotBlank()) {
                                    Text(
                                        text = "Fechar",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { showApiKeyConfig = false }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    OpenAiService.saveApiKey(context, it.trim())
                                },
                                label = { Text("Chave API da OpenAI (sk-...)", fontSize = 11.sp) },
                                placeholder = { Text("sk-proj-...", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10A37F),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Seletor de Modelo OpenAI
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Modelo:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                listOf("gpt-4o-mini", "gpt-4o").forEach { modelOption ->
                                    val isSelected = selectedModel == modelOption
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF10A37F).copy(alpha = 0.2f) else Color.Transparent,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF10A37F) else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedModel = modelOption }
                                    ) {
                                        Text(
                                            text = if (modelOption == "gpt-4o-mini") "GPT-4o Mini" else "GPT-4o",
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF10A37F) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==============================================================
                // PAINEL DE AÇÃO: RESTAURAÇÃO CIENTÍFICA & EXATAS (OPENAI)
                // ==============================================================
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                    border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🔬 Restauração Científica & Exatas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "Fidelidade 100%",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7DD3FC),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Biologia • Química • Física • Cálculos • Equações • Raciocínio Lógico",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(vertical = 3.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Botão Primário: Restaurar Conteúdo Científico & Exatas com OpenAI
                        Button(
                            onClick = {
                                val cleanKey = apiKey.trim()
                                if (cleanKey.isBlank()) {
                                    showApiKeyConfig = true
                                    errorMessage = "Insira sua chave da API OpenAI acima para restaurar."
                                    return@Button
                                }

                                isProcessing = true
                                processingAction = "SCIENTIFIC"
                                errorMessage = null
                                statusMessage = "Renderizando imagem da página e analisando com OpenAI..."

                                scope.launch {
                                    try {
                                        // 1. Renderiza imagem de alta resolução da página do PDF se houver URI
                                        val pageBitmap: Bitmap? = withContext(Dispatchers.IO) {
                                            val uri = book.fileUri
                                            if (!uri.isNullOrBlank()) {
                                                PdfRendererHelper.renderPdfPageBitmap(
                                                    context = context,
                                                    fileUri = uri,
                                                    pageIndexZeroBased = (pageNumber - 1).coerceAtLeast(0),
                                                    destWidth = 1100
                                                )
                                            } else null
                                        }

                                        val targetText = editableText.ifBlank { currentRawText }
                                        statusMessage = "Restaurando equações, reações químicas, biologia e lógica..."

                                        // 2. Chama a reestruturação científica com visão e texto
                                        val result = OpenAiService.reconstructScientificPage(
                                            apiKey = cleanKey,
                                            model = selectedModel,
                                            bookId = book.id,
                                            bookTitle = book.title,
                                            pageNumber = pageNumber,
                                            pageText = targetText,
                                            pageBitmap = pageBitmap
                                        )

                                        result.onSuccess { recon ->
                                            scientificReconstruction = recon
                                            val fullText = OpenAiService.buildContinuousRestoredPageText(recon)
                                            editableText = fullText
                                            displayMode = PageDialogDisplayMode.TEXTBOOK_RENDERED
                                            statusMessage = "Página científica restaurada no formato didático dos livros de ensino!"
                                            errorMessage = null

                                            // Salva no banco de dados para sincronizar com o app
                                            viewModel?.saveScientificReconstruction(recon)

                                            Toast.makeText(
                                                context,
                                                "Restauração científica concluída com formato renderizado!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Erro ao comunicar com a OpenAI"
                                            statusMessage = null
                                        }
                                    } finally {
                                        isProcessing = false
                                        processingAction = null
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isProcessing && processingAction == "SCIENTIFIC") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restaurando Ciências & Exatas...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Restaurar Ciências & Exatas (OpenAI)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Seletor de Idioma para a Restauração & Tradução
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "🌐 Idioma da Restauração (Tradução Integral):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                restoreLanguages.forEach { lang ->
                                    val isSel = selectedRestoreLang.code == lang.code
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF10A37F).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            if (isSel) 1.2.dp else 0.6.dp,
                                            if (isSel) Color(0xFF10A37F) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.clickable {
                                            selectedRestoreLang = lang
                                            val savedForLang = PdfRendererHelper.getSavedCustomPageLanguageText(context, book.id, pageNumber, lang.code)
                                            if (!savedForLang.isNullOrBlank()) {
                                                editableText = savedForLang
                                            } else if (lang.code == "en" && !pageTranslationRecord?.englishText.isNullOrBlank()) {
                                                editableText = pageTranslationRecord!!.englishText
                                            } else if (lang.code == "es" && !pageTranslationRecord?.spanishText.isNullOrBlank()) {
                                                editableText = pageTranslationRecord!!.spanishText
                                            } else if (lang.code == "fr" && !pageTranslationRecord?.frenchText.isNullOrBlank()) {
                                                editableText = pageTranslationRecord!!.frenchText
                                            } else if (lang.code == "pt") {
                                                val savedPt = PdfRendererHelper.getSavedCustomPageText(context, book.id, pageNumber)
                                                editableText = if (!savedPt.isNullOrBlank()) savedPt else currentRawText.trim()
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = lang.flag, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = lang.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color(0xFF10A37F) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Ações Secundárias: Restauração Textual Geral + Ouvir Prévia TTS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Botão: Restauração de Texto Geral no Idioma Selecionado
                            OutlinedButton(
                                onClick = {
                                    val cleanKey = apiKey.trim()
                                    if (cleanKey.isBlank()) {
                                        showApiKeyConfig = true
                                        errorMessage = "Insira sua chave da API OpenAI acima para restaurar."
                                        return@OutlinedButton
                                    }

                                    isProcessing = true
                                    processingAction = "CLASSIC"
                                    errorMessage = null
                                    statusMessage = if (selectedRestoreLang.code == "pt") {
                                        "Restaurando pontuação e caracteres originais em Português..."
                                    } else {
                                        "Restaurando e traduzindo integralmente para ${selectedRestoreLang.displayName}..."
                                    }

                                    scope.launch {
                                        try {
                                            val pageBitmap: Bitmap? = withContext(Dispatchers.IO) {
                                                val uri = book.fileUri
                                                if (!uri.isNullOrBlank()) {
                                                    PdfRendererHelper.renderPdfPageBitmap(
                                                        context = context,
                                                        fileUri = uri,
                                                        pageIndexZeroBased = (pageNumber - 1).coerceAtLeast(0),
                                                        destWidth = 1000
                                                    )
                                                } else null
                                            }

                                            val targetText = editableText.ifBlank { currentRawText }
                                            val result = OpenAiService.restoreAndTranslatePageText(
                                                apiKey = cleanKey,
                                                model = selectedModel,
                                                bookTitle = book.title,
                                                pageNumber = pageNumber,
                                                rawText = targetText,
                                                pageBitmap = pageBitmap,
                                                targetLanguageCode = selectedRestoreLang.code,
                                                targetLanguageName = selectedRestoreLang.displayName
                                            )
                                            result.onSuccess { restored ->
                                                editableText = restored
                                                statusMessage = if (selectedRestoreLang.code == "pt") {
                                                    "Texto restaurado com 100% de integridade original!"
                                                } else {
                                                    "Página 100% restaurada e traduzida para ${selectedRestoreLang.displayName}!"
                                                }
                                                errorMessage = null
                                                Toast.makeText(context, "Texto restaurado em ${selectedRestoreLang.displayName}! Revise e salve.", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Erro ao comunicar com a OpenAI"
                                                statusMessage = null
                                            }
                                        } finally {
                                            isProcessing = false
                                            processingAction = null
                                        }
                                    }
                                },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10A37F).copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                if (isProcessing && processingAction == "CLASSIC") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFF10A37F),
                                        strokeWidth = 1.8.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restaurando...", fontSize = 11.sp, color = Color(0xFF10A37F))
                                } else {
                                    Text(
                                        if (selectedRestoreLang.code == "pt") "📝 Restauração Geral" else "🌐 Restaurar (${selectedRestoreLang.flag} ${selectedRestoreLang.code.uppercase()})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10A37F)
                                    )
                                }
                            }

                            // Botão Ouvir Prévia TTS
                            OutlinedButton(
                                onClick = {
                                    if (isPreviewPlaying) {
                                        speechManager?.stop()
                                        isPreviewPlaying = false
                                    } else {
                                        if (editableText.isNotBlank()) {
                                            isPreviewPlaying = true
                                            speechManager?.speakTextImmediately(editableText)
                                        } else {
                                            Toast.makeText(context, "Nenhum texto para reproduzir", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isPreviewPlaying) Color(0xFFEF4444) else Color(0xFF38BDF8)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isPreviewPlaying) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPreviewPlaying) "Parar" else "Ouvir",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPreviewPlaying) Color(0xFFEF4444) else Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }

                // Mensagens de Status ou Erro
                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = "✨ ${statusMessage!!}",
                        fontSize = 11.5.sp,
                        color = Color(0xFF10A37F),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "⚠️ $errorMessage",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Permite correção local em caso de falha da API
                            Text(
                                text = "🔧 Aplicar correção local imediata (sem API)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val fixed = OpenAiService.runLocalTextPunctuationFix(editableText.ifBlank { currentRawText })
                                    editableText = fixed
                                    errorMessage = null
                                    statusMessage = "Correção local de pontuação e caracteres aplicada."
                                }
                            )
                        }
                    }
                }

                // ==============================================================
                // SELETOR DE VISUALIZAÇÃO: LIVRO DIDÁTICO RENDERIZADO VS EDITOR
                // ==============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isRendered = displayMode == PageDialogDisplayMode.TEXTBOOK_RENDERED
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isRendered) Color(0xFF0284C7).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isRendered) Color(0xFF38BDF8) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { displayMode = PageDialogDisplayMode.TEXTBOOK_RENDERED }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📖 Livro Didático (Renderizado)",
                                fontSize = 11.sp,
                                fontWeight = if (isRendered) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRendered) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isEditor = displayMode == PageDialogDisplayMode.RAW_EDITOR
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEditor) Color(0xFF10A37F).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isEditor) Color(0xFF10A37F) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { displayMode = PageDialogDisplayMode.RAW_EDITOR }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✏️ Editor de Texto Salvo",
                                fontSize = 11.sp,
                                fontWeight = if (isEditor) FontWeight.Bold else FontWeight.Medium,
                                color = if (isEditor) Color(0xFF10A37F) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ==============================================================
                // CONTEÚDO DA PÁGINA (RENDERIZADO OU EDITOR)
                // ==============================================================
                when (displayMode) {
                    PageDialogDisplayMode.TEXTBOOK_RENDERED -> {
                        RenderedTextbookPageView(
                            reconstruction = scientificReconstruction,
                            fallbackText = editableText,
                            speechManager = speechManager,
                            context = context
                        )
                    }
                    PageDialogDisplayMode.RAW_EDITOR -> {
                        Column {
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = { editableText = it },
                                label = { Text("Texto da Página (Salvo no Aparelho)", fontSize = 11.5.sp) },
                                placeholder = { Text("O texto da página restaurado pela OpenAI aparecerá aqui.", fontSize = 11.5.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 290.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10A37F),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 20.sp)
                            )

                            // Estatísticas de palavras e caracteres
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val wordsCount = remember(editableText) {
                                    editableText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                }
                                Text(
                                    text = "$wordsCount palavras • ${editableText.length} caracteres",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Texto Restaurado", editableText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Guia de diretrizes de fidelidade científica e exatas
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🛡️ Fidelidade Didática & Científica OpenAI:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "• Biologia: Citologia, vias metabólicas (glicólise, Krebs, ATP, fotossíntese), DNA/RNA e genética.\n" +
                                   "• Química: Equações balanceadas (→, ⇌), índices subscritos límpidos (H₂O, C₆H₁₂O₆), cargas e entalpia (ΔH).\n" +
                                   "• Física: Fórmulas mecânicas, eletromagnéticas e termodinâmicas com todas as unidades SI preservadas.\n" +
                                   "• Cálculos: Integrais (∫), derivadas, limites, somatórios (∑), frações e raízes empilhadas.\n" +
                                   "• Raciocínio Lógico: Proposições, conectivos (∧, ∨, ¬, →, ↔), quantificadores (∀, ∃) e tabelas-verdade.\n" +
                                   "• Salva diretamente no armazenamento do seu aparelho nesta página do livro.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPreviewPlaying) speechManager?.stop()
                    val trimmed = editableText.trim()
                    if (trimmed.isNotBlank()) {
                        PdfRendererHelper.saveCustomPageLanguageText(context, book.id, pageNumber, selectedRestoreLang.code, trimmed)
                        if (selectedRestoreLang.code == "pt") {
                            PdfRendererHelper.saveCustomPageText(context, book.id, pageNumber, trimmed)
                            onApplyRestructuredText(trimmed)
                        }

                        if (viewModel != null) {
                            val existingTrans = pageTranslationRecord
                            val updatedTrans = if (existingTrans != null) {
                                when (selectedRestoreLang.code) {
                                    "en" -> existingTrans.copy(englishText = trimmed, translatedAt = System.currentTimeMillis())
                                    "es" -> existingTrans.copy(spanishText = trimmed, translatedAt = System.currentTimeMillis())
                                    "fr" -> existingTrans.copy(frenchText = trimmed, translatedAt = System.currentTimeMillis())
                                    else -> existingTrans.copy(portugueseText = trimmed, originalText = trimmed, translatedAt = System.currentTimeMillis())
                                }
                            } else {
                                com.example.data.model.PageMultiTranslation(
                                    bookId = book.id,
                                    pageNumber = pageNumber,
                                    originalText = currentRawText,
                                    englishText = if (selectedRestoreLang.code == "en") trimmed else "",
                                    spanishText = if (selectedRestoreLang.code == "es") trimmed else "",
                                    frenchText = if (selectedRestoreLang.code == "fr") trimmed else "",
                                    portugueseText = if (selectedRestoreLang.code == "pt") trimmed else "",
                                    translatedAt = System.currentTimeMillis()
                                )
                            }
                            viewModel.savePageTranslation(updatedTrans)
                        }

                        scientificReconstruction?.let { recon ->
                            viewModel?.saveScientificReconstruction(recon)
                        }
                        viewModel?.applyScientificTextToReader(book.id, pageNumber, trimmed)
                        Toast.makeText(context, "Página $pageNumber restaurada em ${selectedRestoreLang.displayName} e salva no telefone!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "O texto não pode ficar em branco", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Salvar na Página",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isPreviewPlaying) speechManager?.stop()
                    onDismiss()
                }
            ) {
                Text("Cancelar", fontSize = 13.sp)
            }
        }
    )
}

/**
 * Renderizador de página no formato real de Livro de Ensino dos Alunos:
 * Exibe cabeçalho temático da disciplina (Cálculo, Química, Física, Biologia, Lógica),
 * texto teórico editorial e caixas de teoremas e fórmulas com notação nítida, LaTeX e pronúncia TTS.
 */
@Composable
private fun RenderedTextbookPageView(
    reconstruction: ScientificPageReconstruction?,
    fallbackText: String,
    speechManager: PdfSpeechManager?,
    context: Context
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 340.dp)
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (reconstruction != null) {
                    // 1. Cabeçalho Editorial do Livro Didático
                    val subjectColor = when {
                        reconstruction.subjectArea.contains("Química", ignoreCase = true) -> Color(0xFFF59E0B)
                        reconstruction.subjectArea.contains("Bio", ignoreCase = true) || reconstruction.subjectArea.contains("Cito", ignoreCase = true) -> Color(0xFF10B981)
                        reconstruction.subjectArea.contains("Física", ignoreCase = true) -> Color(0xFF8B5CF6)
                        reconstruction.subjectArea.contains("Lógica", ignoreCase = true) -> Color(0xFF6366F1)
                        else -> Color(0xFF0284C7)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = subjectColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = reconstruction.subjectArea.ifBlank { "Ciências Exatas & Naturais" },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = subjectColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "Livro Didático • Fiel ao Original",
                            fontSize = 9.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (reconstruction.pageTitle.isNotBlank()) {
                        Text(
                            text = reconstruction.pageTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            lineHeight = 20.sp
                        )
                    }

                    // 2. Parágrafos teóricos em tipografia editorial serifada
                    val paragraphs = reconstruction.structuredExplanation
                        .split("\n\n")
                        .filter { it.isNotBlank() }

                    paragraphs.forEach { para ->
                        Text(
                            text = para,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Justify,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    // 3. Caixas de Fórmulas e Equações (Renderizadas no estilo de livro escolar)
                    val formulas = reconstruction.getFormulas()
                    if (formulas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📐 Equações, Cálculos & Reações Químicas:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )

                        formulas.forEach { formula ->
                            TextbookFormulaCard(formula = formula, speechManager = speechManager, context = context)
                        }
                    }

                    // 4. Diagramas e Esquemas Citológicos/Biológicos
                    val diagrams = reconstruction.getDiagrams()
                    if (diagrams.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔬 Diagramas & Estruturas:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )

                        diagrams.forEach { diag ->
                            TextbookDiagramCard(diag)
                        }
                    }

                    // 5. Símbolos e Constantes
                    val symbols = reconstruction.getSymbols()
                    if (symbols.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔣 Símbolos Científicos & Constantes SI:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA)
                        )

                        symbols.forEach { sym ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sym.symbol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFA78BFA)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = sym.name, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        if (sym.contextAndUnit.isNotBlank()) {
                                            Text(text = sym.contextAndUnit, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Fallback se ainda não houver reconstrução científica gerada pela OpenAI
                    Text(
                        text = "📖 Visualização Didática do Texto",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Toque em 'Restaurar Ciências & Exatas (OpenAI)' acima para que o modelo processe a página do livro, extraia todas as fórmulas, reações químicas e equações, e apresente no formato renderizado idêntico aos livros de ensino.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val rawParagraphs = fallbackText.split("\n\n").filter { it.isNotBlank() }
                    rawParagraphs.forEach { p ->
                        Text(
                            text = p,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Justify,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Caixa de Teorema e Equação no estilo de livro didático real de física/química/matemática
 */
@Composable
private fun TextbookFormulaCard(
    formula: ScientificFormulaItem,
    speechManager: PdfSpeechManager?,
    context: Context
) {
    var showLatex by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.25f)
                ) {
                    Text(
                        text = formula.category.ifBlank { "Equação" },
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7DD3FC),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (formula.latexNotation.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (showLatex) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .clickable { showLatex = !showLatex }
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                text = "LaTeX",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showLatex) Color.Black else Color(0xFF93C5FD),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val textToSpeak = formula.spokenTtsGuide.ifBlank { formula.formulaNotation }
                            speechManager?.speakTextInLanguage(textToSpeak, "pt")
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Ouvir fórmula",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Fórmula", formula.formulaNotation)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Fórmula copiada!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Equação formatada no estilo e diagramação dos livros didáticos físicos
            TextbookEquationView(
                formulaNotation = formula.formulaNotation,
                latexNotation = formula.latexNotation,
                accentColor = Color(0xFF38BDF8)
            )

            if (showLatex && formula.latexNotation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LaTeX: ${formula.latexNotation}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }

            if (formula.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = formula.explanation,
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }

            if (formula.dimensionalAnalysis.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "📐 Unidades (SI): ${formula.dimensionalAnalysis}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7DD3FC)
                )
            }

            if (formula.stepByStepSolution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "🪜 Demonstração / Passo a Passo:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        formula.stepByStepSolution.forEachIndexed { i, step ->
                            Text(
                                text = "${i + 1}. $step",
                                fontSize = 10.5.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Caixa de Diagrama/Esquema do livro didático
 */
@Composable
private fun TextbookDiagramCard(diag: ScientificDiagramItem) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "${diag.labelOrFigure}: ${diag.title}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399)
            )
            if (diag.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = diag.description,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 15.sp
                )
            }
            if (diag.keyComponents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Componentes: ${diag.keyComponents.joinToString(", ")}",
                    fontSize = 10.sp,
                    color = Color(0xFF6EE7B7)
                )
            }
        }
    }
}
