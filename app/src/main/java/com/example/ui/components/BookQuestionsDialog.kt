package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookQuestion
import com.example.data.model.PdfBook
import com.example.data.openai.OpenAiService

enum class QuestionDialogTab {
    PRACTICE,
    GENERATE,
    ANALYTICS
}

@Composable
fun BookQuestionsDialog(
    book: PdfBook,
    currentPage: Int,
    questions: List<BookQuestion>,
    allGlobalQuestions: List<BookQuestion> = emptyList(),
    allBooks: List<PdfBook> = emptyList(),
    isGenerating: Boolean,
    generationError: String?,
    readingTheme: String = book.readingTheme ?: "MONOCHROME_BW",
    onDismiss: () -> Unit,
    onAnswerSelected: (questionId: Long, selectedIndex: Int, isCorrect: Boolean) -> Unit,
    onResetAnswer: (questionId: Long) -> Unit,
    onResetAllAnswers: () -> Unit,
    onDeleteQuestion: (questionId: Long) -> Unit,
    onDeleteQuestionsForPage: (page: Int) -> Unit,
    onDeleteAllQuestionsForBook: () -> Unit,
    onGenerateQuestions: (
        startPage: Int,
        endPage: Int,
        institution: String,
        difficulty: String,
        questionCount: Int,
        model: String
    ) -> Unit,
    onSpeakText: ((String) -> Unit)? = null,
    onOpenGlobalReels: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val palette = com.example.ui.theme.ReadingThemeManager.getPalette(readingTheme, book.category, book.categoryColor)
    var selectedTab by remember {
        mutableStateOf(if (questions.isNotEmpty()) QuestionDialogTab.PRACTICE else QuestionDialogTab.GENERATE)
    }

    // Filtros de Prática e Escopo (Este Livro vs Todos os Livros)
    var isGlobalScope by remember { mutableStateOf(false) }
    var practiceFilter by remember { mutableStateOf("TODAS") } // "TODAS", "PAGINA_ATUAL", "NAO_RESPONDIDAS", "ERROS", "ACERTOS"

    // Auto-transição para a aba de Praticar após gerar questões com sucesso
    var previousIsGenerating by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(isGenerating) {
        if (previousIsGenerating && !isGenerating && generationError == null) {
            selectedTab = QuestionDialogTab.PRACTICE
            practiceFilter = "TODAS"
        }
        previousIsGenerating = isGenerating
    }

    // Configurações de Geração
    var isRangeMode by remember { mutableStateOf(false) }
    var startPageText by remember { mutableStateOf(currentPage.toString()) }
    var endPageText by remember { mutableStateOf(currentPage.toString()) }

    var selectedInstitution by remember { mutableStateOf("FGV") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var selectedCount by remember { mutableIntStateOf(5) }
    var selectedModel by remember { mutableStateOf("gpt-4o-mini") }

    val bookQuestions = remember(questions, book.id) {
        questions.filter { it.bookId == book.id }
    }

    val activeQuestionsPool = remember(isGlobalScope, bookQuestions, allGlobalQuestions) {
        if (isGlobalScope) allGlobalQuestions else bookQuestions
    }

    val filteredQuestions = remember(activeQuestionsPool, practiceFilter, currentPage, isGlobalScope) {
        when (practiceFilter) {
            "PAGINA_ATUAL" -> activeQuestionsPool.filter { it.startPage <= currentPage && it.endPage >= currentPage }
            "NAO_RESPONDIDAS" -> activeQuestionsPool.filter { !it.isAnswered }
            "ERROS" -> activeQuestionsPool.filter { it.isAnswered && it.isCorrect == false }
            "ACERTOS" -> activeQuestionsPool.filter { it.isAnswered && it.isCorrect == true }
            else -> activeQuestionsPool
        }
    }

    val totalAnswered = remember(activeQuestionsPool) { activeQuestionsPool.count { it.isAnswered } }
    val totalCorrect = remember(activeQuestionsPool) { activeQuestionsPool.count { it.isAnswered && it.isCorrect == true } }
    val accuracyPercent = remember(totalAnswered, totalCorrect) {
        if (totalAnswered > 0) ((totalCorrect.toFloat() / totalAnswered.toFloat()) * 100).toInt() else 0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.96f),
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Header do Diálogo (Compacto e Elegante)
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
                            shape = CircleShape,
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            border = BorderStroke(1.2.dp, Color(0xFFA78BFA)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val cleanHeaderTitle = remember(book.title) {
                                book.title.removeSuffix(".pdf").removeSuffix(".PDF")
                                    .removeSuffix(".epub").removeSuffix(".EPUB")
                                    .replace("_", " ")
                                    .trim()
                            }
                            Text(
                                text = "Simulado & Questões",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$cleanHeaderTitle • Pág. $currentPage/${book.totalPages} • ${bookQuestions.size} salvas",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onOpenGlobalReels != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD97706).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { onOpenGlobalReels() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reels Global (${allGlobalQuestions.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDE68A)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Abas de Navegação Compactas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Triple(QuestionDialogTab.PRACTICE, "📝 Praticar (${bookQuestions.size})", Color(0xFF8B5CF6)),
                        Triple(QuestionDialogTab.GENERATE, "⚡ Gerar Novas", Color(0xFF00DC82)),
                        Triple(QuestionDialogTab.ANALYTICS, "📊 Desempenho", Color(0xFF38BDF8))
                    )

                    tabs.forEach { (tab, title, activeColor) ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) activeColor.copy(alpha = 0.25f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, activeColor) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clickable { selectedTab = tab }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Conteúdo da Aba Selecionada
                when (selectedTab) {
                    QuestionDialogTab.PRACTICE -> {
                        PracticeTabContent(
                            questions = filteredQuestions,
                            allBookQuestions = bookQuestions,
                            allBooks = allBooks,
                            currentBookTitle = book.title,
                            currentPage = currentPage,
                            practiceFilter = practiceFilter,
                            onFilterChanged = { practiceFilter = it },
                            totalAnswered = totalAnswered,
                            totalCorrect = totalCorrect,
                            accuracyPercent = accuracyPercent,
                            onAnswerSelected = onAnswerSelected,
                            onResetAnswer = onResetAnswer,
                            onResetAllAnswers = onResetAllAnswers,
                            onDeleteQuestion = onDeleteQuestion,
                            onDeleteQuestionsForPage = { onDeleteQuestionsForPage(currentPage) },
                            onDeleteAllQuestions = onDeleteAllQuestionsForBook,
                            onGoToGenerate = { selectedTab = QuestionDialogTab.GENERATE },
                            onSpeakText = onSpeakText
                        )
                    }

                    QuestionDialogTab.GENERATE -> {
                        GenerateTabContent(
                            book = book,
                            currentPage = currentPage,
                            isRangeMode = isRangeMode,
                            onToggleRangeMode = { isRangeMode = it },
                            startPageText = startPageText,
                            onStartPageChange = { startPageText = it },
                            endPageText = endPageText,
                            onEndPageChange = { endPageText = it },
                            selectedInstitution = selectedInstitution,
                            onInstitutionChange = { selectedInstitution = it },
                            selectedDifficulty = selectedDifficulty,
                            onDifficultyChange = { selectedDifficulty = it },
                            selectedCount = selectedCount,
                            onCountChange = { selectedCount = it },
                            selectedModel = selectedModel,
                            onModelChange = { selectedModel = it },
                            isGenerating = isGenerating,
                            generationError = generationError,
                            onGenerate = {
                                val sPage = if (isRangeMode) (startPageText.toIntOrNull() ?: currentPage).coerceIn(1, book.totalPages) else currentPage
                                val ePage = if (isRangeMode) (endPageText.toIntOrNull() ?: currentPage).coerceIn(sPage, book.totalPages) else currentPage
                                onGenerateQuestions(
                                    sPage,
                                    ePage,
                                    selectedInstitution,
                                    selectedDifficulty,
                                    selectedCount,
                                    selectedModel
                                )
                            }
                        )
                    }

                    QuestionDialogTab.ANALYTICS -> {
                        AnalyticsTabContent(
                            questions = bookQuestions,
                            totalAnswered = totalAnswered,
                            totalCorrect = totalCorrect,
                            accuracyPercent = accuracyPercent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeTabContent(
    questions: List<BookQuestion>,
    allBookQuestions: List<BookQuestion>,
    allBooks: List<PdfBook> = emptyList(),
    currentBookTitle: String = "",
    currentPage: Int,
    practiceFilter: String,
    onFilterChanged: (String) -> Unit,
    totalAnswered: Int,
    totalCorrect: Int,
    accuracyPercent: Int,
    onAnswerSelected: (questionId: Long, selectedIndex: Int, isCorrect: Boolean) -> Unit,
    onResetAnswer: (questionId: Long) -> Unit,
    onResetAllAnswers: () -> Unit,
    onDeleteQuestion: (questionId: Long) -> Unit,
    onDeleteQuestionsForPage: () -> Unit,
    onDeleteAllQuestions: () -> Unit,
    onGoToGenerate: () -> Unit,
    onSpeakText: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Barra Compacta de Estatísticas Rápidas, Ações e Filtros
        if (allBookQuestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // Linha 1: Aproveitamento e Botões de Ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val accColor = if (accuracyPercent >= 70) Color(0xFF10B981) else if (accuracyPercent >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accColor.copy(alpha = 0.18f),
                                border = BorderStroke(0.8.dp, accColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "$accuracyPercent% Acerto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "$totalCorrect/$totalAnswered respondidas",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (totalAnswered > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable { onResetAllAnswers() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Refazer tudo",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onDeleteQuestionsForPage() }
                            ) {
                                Text(
                                    text = "Limpar Pág",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFCA5A5),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Linha 2: Filtros Compactos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filters = listOf(
                            "TODAS" to "Todas (${allBookQuestions.size})",
                            "PAGINA_ATUAL" to "Pág $currentPage",
                            "NAO_RESPONDIDAS" to "Pendentes",
                            "ERROS" to "Erros",
                            "ACERTOS" to "Acertos"
                        )

                        filters.forEach { (key, label) ->
                            val isSelected = practiceFilter == key
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(0.8.dp, if (isSelected) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.clickable { onFilterChanged(key) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Lista de Questões
        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA).copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (allBookQuestions.isEmpty()) "Nenhuma questão gerada para este livro ainda." else "Nenhuma questão corresponde ao filtro selecionado.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gere questões estilo FGV, CEBRASPE, FCC e IADES para praticar e fixar o conteúdo!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGoToGenerate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerar Questões com IA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(questions, key = { _, q -> q.id }) { index, question ->
                    val resolvedTitle = allBooks.find { it.id == question.bookId }?.title ?: currentBookTitle
                    InteractiveQuestionCard(
                        question = question,
                        questionNumber = index + 1,
                        bookTitle = resolvedTitle,
                        onAnswerSelected = { selectedIdx, isCorrect ->
                            onAnswerSelected(question.id, selectedIdx, isCorrect)
                        },
                        onResetAnswer = {
                            onResetAnswer(question.id)
                        },
                        onDeleteQuestion = {
                            onDeleteQuestion(question.id)
                        },
                        onSpeakText = onSpeakText
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun GenerateTabContent(
    book: PdfBook,
    currentPage: Int,
    isRangeMode: Boolean,
    onToggleRangeMode: (Boolean) -> Unit,
    startPageText: String,
    onStartPageChange: (String) -> Unit,
    endPageText: String,
    onEndPageChange: (String) -> Unit,
    selectedInstitution: String,
    onInstitutionChange: (String) -> Unit,
    selectedDifficulty: String,
    onDifficultyChange: (String) -> Unit,
    selectedCount: Int,
    onCountChange: (Int) -> Unit,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    isGenerating: Boolean,
    generationError: String?,
    onGenerate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Escopo das Páginas (Página Atual ou Intervalo)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "1. Seleção de Páginas para Análise",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isRangeMode) Color(0xFF8B5CF6).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (!isRangeMode) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleRangeMode(false) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📄 Página Atual",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Página $currentPage de ${book.totalPages}",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRangeMode) Color(0xFF8B5CF6).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (isRangeMode) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleRangeMode(true) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📑 Intervalo de Páginas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ex: Pág. 1 até 5",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    if (isRangeMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = startPageText,
                                onValueChange = onStartPageChange,
                                label = { Text("Pág. Inicial", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFA78BFA),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Text(text = "até", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                            OutlinedTextField(
                                value = endPageText,
                                onValueChange = onEndPageChange,
                                label = { Text("Pág. Final", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFA78BFA),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                        Text(
                            text = "Dica: Para análises profundas de cada parágrafo, escolha de 1 a 5 páginas por vez.",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // 2. Seleção de Banca Examinadora
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "2. Estilo da Banca Examinadora",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val institutions = listOf(
                        Triple("FGV", "FGV (Interpretação Difícil & Semântica)", Color(0xFFD97706)),
                        Triple("CEBRASPE", "CEBRASPE (Certo ou Errado / Textual)", Color(0xFF2563EB)),
                        Triple("FCC", "FCC (Assertivas I-III & Correlação)", Color(0xFF7C3AED)),
                        Triple("VUNESP", "VUNESP (Casos Práticos & Interpretação)", Color(0xFF059669)),
                        Triple("CESGRANRIO", "CESGRANRIO (CNU / Dinâmica Aplicada)", Color(0xFFE11D48)),
                        Triple("QUADRIX", "QUADRIX (Gramática & Assertivas)", Color(0xFF0891B2)),
                        Triple("AOCP", "INSTITUTO AOCP (Doutrina & Análise)", Color(0xFF4F46E5)),
                        Triple("IADES", "IADES / EADES (Coerência & Discurso)", Color(0xFF0D9488)),
                        Triple("MISTA", "Simulado Multibancas (Mix Completo)", Color(0xFFF59E0B))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        institutions.forEach { (inst, label, col) ->
                            val isSelected = selectedInstitution == inst
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) col.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(1.dp, if (isSelected) col else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onInstitutionChange(inst) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) col else Color.Transparent,
                                            border = BorderStroke(1.5.dp, col),
                                            modifier = Modifier.size(16.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                                        )
                                    }

                                    val tagText = when (inst) {
                                        "CEBRASPE" -> "C / E"
                                        "FCC" -> "I, II, III"
                                        "QUADRIX" -> "C/E ou A-E"
                                        "AOCP" -> "Assertivas"
                                        else -> "A, B, C, D, E"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = tagText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Nível de Dificuldade
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "3. Nível de Dificuldade & Exigência",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val difficulties = listOf(
                        Triple("EASY", "Fácil", Color(0xFF10B981)),
                        Triple("MEDIUM", "Médio", Color(0xFFF59E0B)),
                        Triple("HARD", "Difícil", Color(0xFFEA580C)),
                        Triple("INSANE", "Insano 🔥", Color(0xFFDC2626))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        difficulties.forEach { (diff, label, col) ->
                            val isSelected = selectedDifficulty == diff
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) col.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, if (isSelected) col else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDifficultyChange(diff) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) col else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (selectedDifficulty) {
                            "INSANE" -> "🔥 Modo Insano: Pegadinhas milimétricas de semântica, análise sintático-semântica de orações subordinadas e raciocínio textual de nível Magistratura/Diplomacia."
                            "HARD" -> "⚡ Modo Difícil: Exige boa interpretação, identificação de pressupostos, intenção do autor e relações oracionais."
                            "MEDIUM" -> "🎯 Modo Médio: Relações de causa e efeito, inferências e substituição de vocabulário."
                            else -> "🌱 Modo Fácil: Relações explícitas e compreensão direta do texto."
                        },
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }

        // 4. Quantidade de Questões & Modelo
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "4. Quantidade de Questões",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val counts = listOf(3, 5, 8, 10, 15)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        counts.forEach { count ->
                            val isSelected = selectedCount == count
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCountChange(count) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Modelo OpenAI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modelo de IA:",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("gpt-4o-mini" to "GPT-4o Mini", "gpt-4o" to "GPT-4o").forEach { (mId, mLabel) ->
                                val isMSelected = selectedModel == mId
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isMSelected) Color(0xFF00DC82).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, if (isMSelected) Color(0xFF00DC82) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.clickable { onModelChange(mId) }
                                ) {
                                    Text(
                                        text = mLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isMSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isMSelected) Color(0xFF00DC82) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Erro (se houver)
        if (generationError != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDC2626).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Erro: $generationError",
                        fontSize = 11.5.sp,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Botão de Disparo / Geração com Loading
        item {
            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Analisando parágrafos e gerando questões...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✨ Gerar Questões Inéditas com OpenAI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnalyticsTabContent(
    questions: List<BookQuestion>,
    totalAnswered: Int,
    totalCorrect: Int,
    accuracyPercent: Int
) {
    if (questions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Gere e responda questões para visualizar as estatísticas de desempenho.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Resumo Geral
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1B4B).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Desempenho Geral do Livro",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${questions.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Questões", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$totalAnswered", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                            Text(text = "Respondidas", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$totalCorrect", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            Text(text = "Acertos", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${totalAnswered - totalCorrect}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171)
                            )
                            Text(text = "Erros", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$accuracyPercent%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (accuracyPercent >= 70) Color(0xFF34D399) else if (accuracyPercent >= 50) Color(0xFFFBBF24) else Color(0xFFF87171)
                            )
                            Text(text = "Taxa Acerto", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        // Desempenho por Banca
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Aproveitamento por Banca Examinadora",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val instGroups = questions.groupBy { it.institutionDisplayName }
                    instGroups.forEach { (inst, list) ->
                        val ans = list.count { it.isAnswered }
                        val cor = list.count { it.isAnswered && it.isCorrect == true }
                        val pct = if (ans > 0) ((cor.toFloat() / ans.toFloat()) * 100).toInt() else 0

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = inst, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "$cor/$ans acertos ($pct%)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { if (ans > 0) cor.toFloat() / ans.toFloat() else 0f },
                                color = if (pct >= 70) Color(0xFF34D399) else if (pct >= 50) Color(0xFFFBBF24) else Color(0xFFF87171),
                                trackColor = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // Desempenho por Dificuldade
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Aproveitamento por Dificuldade",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val diffGroups = listOf("EASY" to "Fácil", "MEDIUM" to "Médio", "HARD" to "Difícil", "INSANE" to "Insano 🔥")
                    diffGroups.forEach { (dKey, dLabel) ->
                        val list = questions.filter { it.difficulty.equals(dKey, ignoreCase = true) }
                        if (list.isNotEmpty()) {
                            val ans = list.count { it.isAnswered }
                            val cor = list.count { it.isAnswered && it.isCorrect == true }
                            val pct = if (ans > 0) ((cor.toFloat() / ans.toFloat()) * 100).toInt() else 0

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = dLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "$cor/$ans acertos ($pct%)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { if (ans > 0) cor.toFloat() / ans.toFloat() else 0f },
                                    color = if (pct >= 70) Color(0xFF34D399) else if (pct >= 50) Color(0xFFFBBF24) else Color(0xFFF87171),
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
