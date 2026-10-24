package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PdfBook
import com.example.data.model.ScientificDiagramItem
import com.example.data.model.ScientificFlashcardItem
import com.example.data.model.ScientificFormulaItem
import com.example.data.model.ScientificPageReconstruction
import com.example.data.model.ScientificSymbolItem
import com.example.data.openai.OpenAiService
import com.example.data.pdf.PdfRendererHelper
import com.example.data.speech.PdfSpeechManager
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class ScientificDialogTab {
    EXPLANATION,
    FORMULAS,
    STEP_BY_STEP,
    FLASHCARDS,
    DIAGRAMS,
    SYMBOLS,
    TTS_MODE,
    EXPORT
}

@Composable
fun ScientificReconstructionDialog(
    viewModel: MainViewModel,
    book: PdfBook,
    pageNumber: Int,
    pageRawText: String,
    speechManager: PdfSpeechManager?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reconstructionFlow = remember(book.id, pageNumber) {
        viewModel.getScientificReconstructionForPage(book.id, pageNumber)
    }
    val currentReconstruction by reconstructionFlow.collectAsState(initial = null)

    var selectedTab by remember { mutableStateOf(ScientificDialogTab.EXPLANATION) }
    var apiKey by remember { mutableStateOf(OpenAiService.getApiKey(context)) }
    var isProcessing by remember { mutableStateOf(false) }
    var useVisionMode by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showApiKeyInput by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(book.id, pageNumber) {
        apiKey = OpenAiService.getApiKey(context)
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) {
                speechManager?.pause()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F1422),
                border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🔬", fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Reconstrução Científica",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (currentReconstruction != null) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                                        border = BorderStroke(0.6.dp, if (currentReconstruction != null) Color(0xFF10B981) else Color(0xFF38BDF8))
                                    ) {
                                        Text(
                                            text = if (currentReconstruction != null) "● Salvo Offline" else "Novo",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentReconstruction != null) Color(0xFF6EE7B7) else Color(0xFF7DD3FC),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Pág. $pageNumber • ${currentReconstruction?.subjectArea ?: "Fórmulas, Química, Citologia, Física e Lógica"}",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentReconstruction != null) {
                                IconButton(
                                    onClick = { showApiKeyInput = !showApiKeyInput },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Re-analisar",
                                        tint = if (showApiKeyInput) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showConfirmDeleteDialog = true },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir Reconstrução",
                                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    speechManager?.pause()
                                    onDismiss()
                                },
                                enabled = !isProcessing,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Configuration Drawer
                    if (currentReconstruction == null || showApiKeyInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
                            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚙️ IA Científica (Decodificador de Notações)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    if (currentReconstruction != null) {
                                        Text(
                                            text = "Fechar Config",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.clickable { showApiKeyInput = false }
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
                                    label = { Text("Chave API OpenAI (sk-...)") },
                                    placeholder = { Text("Cole sua chave sk-...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0284C7),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF38BDF8),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF090D17),
                                    border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = "📷 Decodificação Multimodal (GPT-4o Vision)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                            Text(
                                                text = "Renderiza a página para decodificar gráficos, curvas, mapas metabólicos e figuras.",
                                                fontSize = 9.5.sp,
                                                color = Color.White.copy(alpha = 0.65f)
                                            )
                                        }
                                        Switch(
                                            checked = useVisionMode,
                                            onCheckedChange = { useVisionMode = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF38BDF8),
                                                checkedTrackColor = Color(0xFF0284C7).copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (apiKey.isBlank()) {
                                            Toast.makeText(context, "Por favor, insira a Chave da API OpenAI.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isProcessing = true
                                        errorMessage = null
                                        statusMessage = "Iniciando análise científica..."

                                        scope.launch {
                                            var pageBitmap: Bitmap? = null
                                            if (useVisionMode && !book.fileUri.isNullOrBlank()) {
                                                statusMessage = "Renderizando imagem da página para análise multimodal..."
                                                try {
                                                    pageBitmap = PdfRendererHelper.renderPdfPageBitmap(
                                                        context = context,
                                                        fileUri = book.fileUri,
                                                        pageIndexZeroBased = (pageNumber - 1).coerceAtLeast(0),
                                                        destWidth = 1080
                                                    )
                                                } catch (e: Exception) {
                                                    android.util.Log.e("ScientificDialog", "Erro ao renderizar bitmap: ${e.message}")
                                                }
                                            }

                                            statusMessage = if (pageBitmap != null) {
                                                "Decodificando fórmulas, gráficos e reações com GPT-4o Vision..."
                                            } else {
                                                "Decodificando fórmulas, equações e símbolos da página..."
                                            }

                                            val result = OpenAiService.reconstructScientificPage(
                                                apiKey = apiKey,
                                                model = "gpt-4o-mini",
                                                bookId = book.id,
                                                bookTitle = book.title,
                                                pageNumber = pageNumber,
                                                pageText = pageRawText,
                                                pageBitmap = pageBitmap
                                            )
                                            isProcessing = false
                                            result.onSuccess { recon ->
                                                viewModel.saveScientificReconstruction(recon)
                                                showApiKeyInput = false
                                                Toast.makeText(context, "Página científica decodificada com sucesso!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Erro desconhecido ao processar página científica."
                                            }
                                        }
                                    },
                                    enabled = !isProcessing,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(statusMessage, fontSize = 11.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (currentReconstruction != null) "Re-processar Notações e Fórmulas" else "Reestruturar e Desbugar Notações da Página",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠️ $errorMessage",
                                        color = Color(0xFFF87171),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    val reconstruction = currentReconstruction
                    if (reconstruction == null) {
                        // Empty State explaining benefits
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                item {
                                    Text("🧬 ⚗️ 📐", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Mecanismo Anti-Bugs Científicos",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Projetado para leitura de livros acadêmicos com equações matemáticas, reações químicas, organelas da citologia, biologia molecular, física e raciocínio lógico.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0B101D), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FeatureBenefitItem("⚗️ Fórmulas Químicas", "Estrutura reações completas (ex: H₂O, C₆H₁₂O₆, setas ⇌, coeficientes estequiométricos) sem quebras.")
                                        FeatureBenefitItem("📐 Matemática & Física", "Decifra frações, integrais ∫, somatórios ∑, raízes √, matrizes e potências em Unicode legível.")
                                        FeatureBenefitItem("🔬 Citologia & Biologia", "Identifica organelas celulares, mapas metabólicos, cadeias de DNA/RNA e processos fisiológicos.")
                                        FeatureBenefitItem("🧠 Lógica Proposicional", "Organiza proposições, conectivos (∧, ∨, ¬, →, ↔) e quantificadores (∀, ∃).")
                                        FeatureBenefitItem("🎧 TTS Fonético Fluido", "Gera texto contínuo especialmente narrado para que o áudio não trave ao encontrar fórmulas.")
                                    }
                                }
                            }
                        }
                    } else {
                        // Navigation Tabs & Action Bar
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScientificTabPill(
                                label = "📖 Explicação Didática",
                                isSelected = selectedTab == ScientificDialogTab.EXPLANATION,
                                onClick = { selectedTab = ScientificDialogTab.EXPLANATION }
                            )
                            ScientificTabPill(
                                label = "📐 Fórmulas & LaTeX (${reconstruction.getFormulas().size})",
                                isSelected = selectedTab == ScientificDialogTab.FORMULAS,
                                onClick = { selectedTab = ScientificDialogTab.FORMULAS }
                            )
                            ScientificTabPill(
                                label = "🪜 Passo a Passo (${reconstruction.getFormulas().count { it.stepByStepSolution.isNotEmpty() }})",
                                isSelected = selectedTab == ScientificDialogTab.STEP_BY_STEP,
                                onClick = { selectedTab = ScientificDialogTab.STEP_BY_STEP }
                            )
                            ScientificTabPill(
                                label = "💡 Flashcards (${reconstruction.getFlashcards().size})",
                                isSelected = selectedTab == ScientificDialogTab.FLASHCARDS,
                                onClick = { selectedTab = ScientificDialogTab.FLASHCARDS }
                            )
                            ScientificTabPill(
                                label = "🔬 Diagramas (${reconstruction.getDiagrams().size})",
                                isSelected = selectedTab == ScientificDialogTab.DIAGRAMS,
                                onClick = { selectedTab = ScientificDialogTab.DIAGRAMS }
                            )
                            ScientificTabPill(
                                label = "🔣 Símbolos SI (${reconstruction.getSymbols().size})",
                                isSelected = selectedTab == ScientificDialogTab.SYMBOLS,
                                onClick = { selectedTab = ScientificDialogTab.SYMBOLS }
                            )
                            ScientificTabPill(
                                label = "🎧 Modo Áudio TTS",
                                isSelected = selectedTab == ScientificDialogTab.TTS_MODE,
                                onClick = { selectedTab = ScientificDialogTab.TTS_MODE }
                            )
                            ScientificTabPill(
                                label = "📋 Dossiê",
                                isSelected = selectedTab == ScientificDialogTab.EXPORT,
                                onClick = { selectedTab = ScientificDialogTab.EXPORT }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tab Content Area
                        when (selectedTab) {
                            ScientificDialogTab.EXPLANATION -> {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    SelectionContainer {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize().padding(14.dp)
                                        ) {
                                            item {
                                                Text(
                                                    text = reconstruction.pageTitle,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF38BDF8)
                                                )
                                                Text(
                                                    text = "Área: ${reconstruction.subjectArea}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF7DD3FC)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }

                                            val paragraphs = reconstruction.structuredExplanation.split("\n\n").filter { it.isNotBlank() }
                                            items(paragraphs) { para ->
                                                Text(
                                                    text = para,
                                                    fontSize = 14.5.sp,
                                                    lineHeight = 22.sp,
                                                    fontFamily = FontFamily.Serif,
                                                    textAlign = TextAlign.Justify,
                                                    color = Color(0xFFE2E8F0)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            ScientificDialogTab.FORMULAS -> {
                                val formulas = reconstruction.getFormulas()
                                if (formulas.isEmpty()) {
                                    EmptyTabCard("Nenhuma fórmula isolada detectada nesta página.", "O conteúdo teórico já está consolidado na aba Explicação.")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(formulas) { formula ->
                                            FormulaCard(formula, speechManager)
                                        }
                                    }
                                }
                            }

                            ScientificDialogTab.STEP_BY_STEP -> {
                                val formulasWithSteps = reconstruction.getFormulas().filter { it.stepByStepSolution.isNotEmpty() }
                                if (formulasWithSteps.isEmpty()) {
                                    EmptyTabCard("Nenhuma dedução passo a passo disponível.", "As fórmulas desta página são diretas ou conceituais. Use o botão Re-analisar para gerar novas demonstrações.")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(formulasWithSteps) { formula ->
                                            StepByStepFormulaCard(formula)
                                        }
                                    }
                                }
                            }

                            ScientificDialogTab.FLASHCARDS -> {
                                val flashcards = reconstruction.getFlashcards()
                                if (flashcards.isEmpty()) {
                                    EmptyTabCard("Nenhum flashcard gerado ainda.", "Re-processe a página com o modelo para gerar cartões de repetição espaçada e memorização ativa.")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(flashcards) { card ->
                                            FlashcardCard(card)
                                        }
                                    }
                                }
                            }

                            ScientificDialogTab.DIAGRAMS -> {
                                val diagrams = reconstruction.getDiagrams()
                                if (diagrams.isEmpty()) {
                                    EmptyTabCard("Nenhum diagrama ou figura isolada detectada.", "A página é predominantemente discursiva e conceitual.")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(diagrams) { diag ->
                                            DiagramCard(diag)
                                        }
                                    }
                                }
                            }

                            ScientificDialogTab.SYMBOLS -> {
                                val symbols = reconstruction.getSymbols()
                                if (symbols.isEmpty()) {
                                    EmptyTabCard("Nenhum símbolo especial encontrado.", "Todos os termos foram integrados na explicação teórica.")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(symbols) { sym ->
                                            SymbolCard(sym)
                                        }
                                    }
                                }
                            }

                            ScientificDialogTab.TTS_MODE -> {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "🎧 Texto Fonético Fluido para TTS",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6EE7B7)
                                                )
                                                Text(
                                                    text = "Fórmulas e símbolos narrados de forma natural, sem engasgos.",
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.6f)
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Speak button
                                                IconButton(
                                                    onClick = {
                                                        speechManager?.speakTextInLanguage(reconstruction.ttsOptimizedText, "pt")
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = "Ouvir TTS",
                                                        tint = Color(0xFF6EE7B7),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                // Apply to Reader
                                                Button(
                                                    onClick = {
                                                        viewModel.applyScientificTextToReader(book.id, pageNumber, reconstruction.ttsOptimizedText)
                                                        Toast.makeText(context, "Texto reestruturado aplicado ao leitor!", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("▶ Aplicar ao Leitor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        SelectionContainer {
                                            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                                item {
                                                    Text(
                                                        text = reconstruction.ttsOptimizedText,
                                                        fontSize = 14.5.sp,
                                                        lineHeight = 22.sp,
                                                        fontFamily = FontFamily.Serif,
                                                        color = Color(0xFFF1F5F9)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            ScientificDialogTab.EXPORT -> {
                                val fullExport = buildScientificExport(reconstruction, book.title, pageNumber)
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                        SelectionContainer(modifier = Modifier.weight(1f)) {
                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                item {
                                                    Text(
                                                        text = fullExport,
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFFE2E8F0)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                val clip = ClipData.newPlainText("Dossiê Científico Pág $pageNumber", fullExport)
                                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                                Toast.makeText(context, "Dossiê científico copiado!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copiar Estudo Científico Completo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDeleteDialog && currentReconstruction != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Excluir Reconstrução Científica?") },
            text = { Text("Deseja apagar a reestruturação e as fórmulas decodificadas desta página salvas offline?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScientificReconstruction(book.id, pageNumber)
                        showConfirmDeleteDialog = false
                        Toast.makeText(context, "Reconstrução excluída.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ScientificTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.08f),
        border = if (isSelected) BorderStroke(0.8.dp, Color(0xFF38BDF8)) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FeatureBenefitItem(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("✓", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            Text(text = description, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun FormulaCard(
    formula: ScientificFormulaItem,
    speechManager: PdfSpeechManager?
) {
    val context = LocalContext.current
    var showLatex by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    border = BorderStroke(0.7.dp, Color(0xFF38BDF8))
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
                            shape = RoundedCornerShape(6.dp),
                            color = if (showLatex) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .clickable { showLatex = !showLatex }
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                text = "LaTeX",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showLatex) Color.Black else Color(0xFF93C5FD),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            speechManager?.speakTextInLanguage(formula.spokenTtsGuide.ifBlank { formula.formulaNotation }, "pt")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Ouvir Pronúncia da Fórmula",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Equação formatada no estilo e diagramação de livro didático físico
            TextbookEquationView(
                formulaNotation = formula.formulaNotation,
                latexNotation = formula.latexNotation,
                accentColor = Color(0xFF38BDF8)
            )

            // LaTeX View & Copy
            if (showLatex && formula.latexNotation.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(0.8.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SelectionContainer(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formula.latexNotation,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF93C5FD)
                            )
                        }
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("LaTeX", formula.latexNotation)
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                Toast.makeText(context, "Código LaTeX copiado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar LaTeX",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formula.explanation,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            if (formula.dimensionalAnalysis.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.35f),
                    border = BorderStroke(0.7.dp, Color(0xFF059669).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📐 Análise Dimensional (SI): ", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                        Text(text = formula.dimensionalAnalysis, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFD1FAE5))
                    }
                }
            }

            if (formula.spokenTtsGuide.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗣️ Leitura Oral: ", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                    Text(text = formula.spokenTtsGuide, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun StepByStepFormulaCard(formula: ScientificFormulaItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF4F46E5).copy(alpha = 0.25f),
                    border = BorderStroke(0.8.dp, Color(0xFF818CF8))
                ) {
                    Text(
                        text = "Dedução / Resolução Passo a Passo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC7D2FE),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "${formula.stepByStepSolution.size} etapas",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Equação formatada no estilo e diagramação de livro didático físico
            TextbookEquationView(
                formulaNotation = formula.formulaNotation,
                latexNotation = formula.latexNotation,
                accentColor = Color(0xFF818CF8)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                formula.stepByStepSolution.forEachIndexed { index, step ->
                    val formattedStep = remember(step) {
                        TextbookMathFormatter.formatToTextbookNotation(step)
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SelectionContainer {
                            Text(
                                text = formattedStep,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Normal,
                                lineHeight = 19.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            if (formula.dimensionalAnalysis.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Unidade Resultante: ${formula.dimensionalAnalysis}",
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF6EE7B7),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardCard(card: ScientificFlashcardItem) {
    var isAnswerRevealed by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAnswerRevealed) Color(0xFF1E2640) else Color(0xFF131C31)
        ),
        border = BorderStroke(
            1.dp,
            if (isAnswerRevealed) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isAnswerRevealed = !isAnswerRevealed }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = BorderStroke(0.7.dp, Color(0xFFFBBF24))
                ) {
                    Text(
                        text = if (card.formulaOrRule.isNotBlank()) card.formulaOrRule else "Conceito Científico",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                    border = BorderStroke(0.7.dp, Color(0xFF38BDF8))
                ) {
                    Text(
                        text = "Flashcard",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7DD3FC),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "❓ ${card.question}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp,
                color = Color.White
            )

            if (card.hint.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 Dica: ${card.hint}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                visible = isAnswerRevealed,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0A1324),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 Resposta / Solução:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            SelectionContainer {
                                Text(
                                    text = card.answer,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFFF1F5F9)
                                )
                            }
                        }
                    }
                }
            }

            if (!isAnswerRevealed) {
                Text(
                    text = "Toque no cartão para revelar a resposta 👆",
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun DiagramCard(diag: ScientificDiagramItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
        border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    border = BorderStroke(0.7.dp, Color(0xFFA78BFA))
                ) {
                    Text(
                        text = diag.labelOrFigure.ifBlank { "Esquema" },
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDDD6FE),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = diag.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = diag.description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            if (diag.keyComponents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Componentes Chave:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                Spacer(modifier = Modifier.height(4.dp))
                diag.keyComponents.forEach { comp ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text("• ", color = Color(0xFFA78BFA), fontSize = 11.sp)
                        Text(text = comp, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolCard(sym: ScientificSymbolItem) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF131C31),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0284C7).copy(alpha = 0.25f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = sym.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = sym.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (sym.contextAndUnit.isNotBlank()) {
                    Text(text = sym.contextAndUnit, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}

@Composable
private fun EmptyTabCard(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔍", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

private fun buildScientificExport(
    recon: ScientificPageReconstruction,
    bookTitle: String,
    pageNumber: Int
): String {
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("DOSSIÊ DE RECONSTRUÇÃO CIENTÍFICA (OPENAI)")
    sb.appendLine("Obra: $bookTitle | Página: $pageNumber")
    sb.appendLine("Área: ${recon.subjectArea}")
    sb.appendLine("Tema: ${recon.pageTitle}")
    sb.appendLine("==========================================\n")

    sb.appendLine("=== CONTEÚDO TEÓRICO REESTRUTURADO ===")
    sb.appendLine(recon.structuredExplanation)
    sb.appendLine()

    val formulas = recon.getFormulas()
    if (formulas.isNotEmpty()) {
        sb.appendLine("=== FÓRMULAS, LATEX & EQUAÇÕES DECODIFICADAS ===")
        formulas.forEachIndexed { i, f ->
            sb.appendLine("[${i + 1}] (${f.category}) ${f.formulaNotation}")
            if (f.latexNotation.isNotBlank()) {
                sb.appendLine("    LaTeX: ${f.latexNotation}")
            }
            if (f.dimensionalAnalysis.isNotBlank()) {
                sb.appendLine("    Análise Dimensional (SI): ${f.dimensionalAnalysis}")
            }
            sb.appendLine("    Explicação: ${f.explanation}")
            if (f.stepByStepSolution.isNotEmpty()) {
                sb.appendLine("    Passo a Passo:")
                f.stepByStepSolution.forEachIndexed { sIdx, step ->
                    sb.appendLine("      ${sIdx + 1}. $step")
                }
            }
            if (f.spokenTtsGuide.isNotBlank()) {
                sb.appendLine("    Leitura Oral (TTS): ${f.spokenTtsGuide}")
            }
            sb.appendLine()
        }
    }

    val flashcards = recon.getFlashcards()
    if (flashcards.isNotEmpty()) {
        sb.appendLine("=== FLASHCARDS DE REPETIÇÃO ESPAÇADA ===")
        flashcards.forEachIndexed { i, fc ->
            val tag = if (fc.formulaOrRule.isNotBlank()) " (${fc.formulaOrRule})" else ""
            sb.appendLine("[FC ${i + 1}]$tag")
            sb.appendLine("  Pergunta: ${fc.question}")
            sb.appendLine("  Resposta: ${fc.answer}")
            if (fc.hint.isNotBlank()) {
                sb.appendLine("  Dica: ${fc.hint}")
            }
            sb.appendLine()
        }
    }

    val diagrams = recon.getDiagrams()
    if (diagrams.isNotEmpty()) {
        sb.appendLine("=== DIAGRAMAS, MAPAS & ORGANELAS CELULARES ===")
        diagrams.forEachIndexed { i, d ->
            sb.appendLine("[${i + 1}] ${d.labelOrFigure}: ${d.title}")
            sb.appendLine("    ${d.description}")
            if (d.keyComponents.isNotEmpty()) {
                sb.appendLine("    Componentes: ${d.keyComponents.joinToString(", ")}")
            }
            sb.appendLine()
        }
    }

    val symbols = recon.getSymbols()
    if (symbols.isNotEmpty()) {
        sb.appendLine("=== GLOSSÁRIO DE SÍMBOLOS & CONSTANTES ===")
        symbols.forEach { s ->
            sb.appendLine("• ${s.symbol}: ${s.name} (${s.contextAndUnit})")
        }
        sb.appendLine()
    }

    sb.appendLine("=== TEXTO ADAPTADO PARA LEITURA ORAL (TTS) ===")
    sb.appendLine(recon.ttsOptimizedText)

    return sb.toString()
}
