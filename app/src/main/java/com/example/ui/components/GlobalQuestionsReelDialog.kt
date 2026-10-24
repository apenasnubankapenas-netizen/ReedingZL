package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookQuestion
import com.example.data.model.PdfBook
import kotlinx.coroutines.launch

@Composable
fun GlobalQuestionsReelDialog(
    allQuestions: List<BookQuestion>,
    allBooks: List<PdfBook>,
    onAnswerSelected: (questionId: Long, selectedIndex: Int, isCorrect: Boolean) -> Unit,
    onResetAnswer: (questionId: Long) -> Unit,
    onResetAllQuestionsGlobal: () -> Unit,
    onDeleteQuestion: (questionId: Long) -> Unit,
    onDismiss: () -> Unit,
    onSpeakText: ((String) -> Unit)? = null,
    onOpenBookAtPage: ((book: PdfBook, page: Int) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val booksMap = remember(allBooks) {
        allBooks.associateBy { it.id }
    }

    // Estados de visualização e filtros
    var isReelsMode by remember { mutableStateOf(true) } // true = Reels Swipe Vertical (arrastar p/ cima/baixo), false = Lista Feed
    var isFullscreenQuiz by remember { mutableStateOf(false) } // Modo Tela Cheia Imersivo (Foco total nas questões)
    var showFiltersInFullscreen by remember { mutableStateOf(false) } // Expande filtros no modo tela cheia se desejado
    var isRandomOrder by remember { mutableStateOf(true) } // Aleatoriedade ativa por padrão
    var statusFilter by remember { mutableStateOf("TODAS") } // "TODAS", "NAO_RESPONDIDAS", "ERROS", "ACERTOS"
    var institutionFilter by remember { mutableStateOf("TODAS") } // "TODAS", "FGV", "CEBRASPE", "FCC", etc.
    var fontSizeMultiplier by remember { mutableFloatStateOf(1.0f) } // Multiplicador de escala de fonte para leitura profissional
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

    // Seed de aleatorização para poder reembaralhar
    var shuffleSeed by remember { mutableIntStateOf(1) }

    // Lista processada de questões
    val displayedQuestions = remember(allQuestions, statusFilter, institutionFilter, isRandomOrder, shuffleSeed) {
        var list = allQuestions.filter { q ->
            val matchesStatus = when (statusFilter) {
                "NAO_RESPONDIDAS" -> !q.isAnswered
                "ERROS" -> q.isAnswered && q.isCorrect == false
                "ACERTOS" -> q.isAnswered && q.isCorrect == true
                else -> true
            }
            val matchesInst = when (institutionFilter) {
                "TODAS" -> true
                else -> q.institution.uppercase().contains(institutionFilter.uppercase()) ||
                        institutionFilter.uppercase().contains(q.institution.uppercase())
            }
            matchesStatus && matchesInst
        }

        if (isRandomOrder) {
            val random = java.util.Random(shuffleSeed.toLong())
            list = list.shuffled(random)
        }
        list
    }

    // Estado do VerticalPager para gesto de arrastar cima/baixo estilo Reels
    val pagerState = rememberPagerState(initialPage = 0) {
        displayedQuestions.size
    }

    // Estatísticas globais
    val totalGlobal = allQuestions.size
    val totalAnswered = remember(allQuestions) { allQuestions.count { it.isAnswered } }
    val totalCorrect = remember(allQuestions) { allQuestions.count { it.isAnswered && it.isCorrect == true } }
    val totalIncorrect = remember(allQuestions) { allQuestions.count { it.isAnswered && it.isCorrect == false } }
    val accuracyPercent = remember(totalAnswered, totalCorrect) {
        if (totalAnswered > 0) ((totalCorrect.toFloat() / totalAnswered.toFloat()) * 100).toInt() else 0
    }

    val dialogBgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F0B1E),
            Color(0xFF090614),
            Color(0xFF04030A)
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = !isFullscreenQuiz,
            decorFitsSystemWindows = !isFullscreenQuiz
        )
    ) {
        Surface(
            modifier = if (isFullscreenQuiz) {
                Modifier
                    .fillMaxSize()
                    .background(dialogBgGradient)
            } else {
                Modifier
                    .fillMaxWidth(0.98f)
                    .fillMaxHeight(0.97f)
                    .background(dialogBgGradient, RoundedCornerShape(16.dp))
            },
            shape = if (isFullscreenQuiz) RectangleShape else RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = if (isFullscreenQuiz) null else BorderStroke(1.dp, Color(0xFF3B2F52))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isFullscreenQuiz) Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        else Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
            ) {
                // =============================================================
                // 1. CABEÇALHO MINIMALISTA DO QUIZ & CONTROLES
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Título e Contador Compacto
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD97706).copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quiz",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                            border = BorderStroke(0.6.dp, Color(0xFFA78BFA))
                        ) {
                            Text(
                                text = "$totalGlobal Qs",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDDD6FE),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        if (isFullscreenQuiz) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✓$totalCorrect  ✗$totalIncorrect",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = if (accuracyPercent >= 70) Color(0xFF10B981) else Color(0xFFFBBF24)
                            )
                        }
                    }

                    // AÇÕES MINIMALISTAS DO TOPO
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Botão DEDICADO DE TELA CHEIA (Ícone minimalista de alto contraste)
                        IconButton(
                            onClick = { isFullscreenQuiz = !isFullscreenQuiz },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreenQuiz) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreenQuiz) "Sair Tela Cheia" else "Tela Cheia",
                                tint = if (isFullscreenQuiz) Color(0xFF38BDF8) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Ajuste de Tamanho da Fonte (A-, A, A+)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (fontSizeMultiplier != 1.0f) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(0.6.dp, if (fontSizeMultiplier != 1.0f) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.clickable {
                                fontSizeMultiplier = when (fontSizeMultiplier) {
                                    0.9f -> 1.0f
                                    1.0f -> 1.15f
                                    1.15f -> 1.3f
                                    else -> 0.9f
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = "Tamanho do Texto",
                                    tint = if (fontSizeMultiplier != 1.0f) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", fontSizeMultiplier)}x",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fontSizeMultiplier != 1.0f) Color(0xFFDDD6FE) else Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        // Toggle de Filtros (no modo tela cheia fica opcional para economizar 100% do espaço)
                        if (isFullscreenQuiz) {
                            IconButton(
                                onClick = { showFiltersInFullscreen = !showFiltersInFullscreen },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filtros",
                                    tint = if (showFiltersInFullscreen) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Embaralhar / Aleatório
                        IconButton(
                            onClick = {
                                isRandomOrder = true
                                shuffleSeed += 1
                                coroutineScope.launch {
                                    if (displayedQuestions.isNotEmpty()) {
                                        pagerState.scrollToPage(0)
                                    }
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "Embaralhar",
                                tint = if (isRandomOrder) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Alternar entre modo Reels e Lista
                        IconButton(
                            onClick = { isReelsMode = !isReelsMode },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isReelsMode) Icons.Default.ViewAgenda else Icons.Default.ViewCarousel,
                                contentDescription = if (isReelsMode) "Ver em Lista" else "Ver em Reels",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Reiniciar
                        IconButton(
                            onClick = { showResetConfirmationDialog = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Fechar
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // =============================================================
                // 2. PLACAR & FILTROS (Ocultados ou colapsados no modo tela cheia para prioridade total das perguntas)
                // =============================================================
                if (!isFullscreenQuiz || showFiltersInFullscreen) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Barra de Placar Compacta
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 3.5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Acertos: $totalCorrect",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Erros: $totalIncorrect",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                                Text(
                                    text = "Respondidas: $totalAnswered/$totalGlobal",
                                    fontSize = 9.5.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (accuracyPercent >= 70) Color(0xFF065F46) else Color(0xFF831843),
                                border = BorderStroke(0.6.dp, if (accuracyPercent >= 70) Color(0xFF10B981) else Color(0xFFF43F5E))
                            ) {
                                Text(
                                    text = "Taxa: $accuracyPercent%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Filtros Rápidos Minimalistas
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Linha de Status (Todas, Pendentes, Erros, Acertos)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 1.dp)
                        ) {
                            val statusList = listOf(
                                "TODAS" to "Todas (${allQuestions.size})",
                                "NAO_RESPONDIDAS" to "Pendentes (${allQuestions.count { !it.isAnswered }})",
                                "ERROS" to "Erros (${allQuestions.count { it.isAnswered && it.isCorrect == false }})",
                                "ACERTOS" to "Acertos (${allQuestions.count { it.isAnswered && it.isCorrect == true }})"
                            )
                            items(statusList) { (key, label) ->
                                val isSelected = statusFilter == key
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.04f),
                                    border = BorderStroke(0.6.dp, if (isSelected) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.clickable {
                                        statusFilter = key
                                        coroutineScope.launch {
                                            if (displayedQuestions.isNotEmpty()) {
                                                pagerState.scrollToPage(0)
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Linha de Bancas Minimalista
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            contentPadding = PaddingValues(vertical = 1.dp)
                        ) {
                            val instList = listOf("TODAS", "FGV", "CEBRASPE", "FCC", "VUNESP", "CESGRANRIO", "QUADRIX", "AOCP", "IADES")
                            items(instList) { inst ->
                                val isSelected = institutionFilter == inst
                                val count = if (inst == "TODAS") allQuestions.size else allQuestions.count { it.institution.contains(inst, ignoreCase = true) }
                                if (count > 0 || inst == "TODAS") {
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = if (isSelected) Color(0xFFD97706).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.03f),
                                        border = BorderStroke(0.6.dp, if (isSelected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.06f)),
                                        modifier = Modifier.clickable {
                                            institutionFilter = inst
                                            coroutineScope.launch {
                                                if (displayedQuestions.isNotEmpty()) {
                                                    pagerState.scrollToPage(0)
                                                }
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = if (inst == "TODAS") "Bancas" else "$inst ($count)",
                                            fontSize = 8.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // =============================================================
                // 3. ÁREA PRINCIPAL: FOCO MÁXIMO NAS QUESTÕES (REELS / LISTA)
                // =============================================================
                if (displayedQuestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA).copy(alpha = 0.4f),
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (allQuestions.isEmpty()) "Nenhuma questão disponível ainda." else "Nenhuma questão para o filtro atual.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Abra qualquer livro, clique no botão 'Question' e gere questões com IA para que elas apareçam automaticamente no Quiz!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            if (statusFilter != "TODAS" || institutionFilter != "TODAS") {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        statusFilter = "TODAS"
                                        institutionFilter = "TODAS"
                                    }
                                ) {
                                    Text("Limpar Filtros", color = Color(0xFFA78BFA), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else if (isReelsMode) {
                    // MODO REELS: VerticalPager nativo com snap e swipe vertical
                    val currentPage = pagerState.currentPage.coerceIn(0, displayedQuestions.size - 1)
                    val activeQ = displayedQuestions.getOrNull(currentPage)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Navegador superior minimalista do Reel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Questão ${currentPage + 1}/${displayedQuestions.size}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDDD6FE)
                                )
                                if (activeQ != null && activeQ.isAnswered) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (activeQ.isCorrect == true) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f),
                                        border = BorderStroke(0.6.dp, if (activeQ.isCorrect == true) Color(0xFF10B981) else Color(0xFFEF4444))
                                    ) {
                                        Text(
                                            text = if (activeQ.isCorrect == true) "✓ Acertou" else "✗ Errou",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeQ.isCorrect == true) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "• Deslize ↕ para passar",
                                        fontSize = 9.5.sp,
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(currentPage - 1)
                                            }
                                        }
                                    },
                                    enabled = currentPage > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Anterior",
                                        tint = if (currentPage > 0) Color.White else Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (currentPage < displayedQuestions.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(currentPage + 1)
                                            }
                                        }
                                    },
                                    enabled = currentPage < displayedQuestions.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Próxima",
                                        tint = if (currentPage < displayedQuestions.size - 1) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Barra de progresso visual ultra-fina da rodada
                        LinearProgressIndicator(
                            progress = { ((currentPage + 1).toFloat() / displayedQuestions.size.toFloat()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = Color(0xFF8B5CF6),
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // VerticalPager: Arrastar suavemente para cima ou para baixo
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            pageSpacing = 8.dp
                        ) { page ->
                            val currentQuestion = displayedQuestions[page]
                            val parentBook = booksMap[currentQuestion.bookId]

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    InteractiveQuestionCard(
                                        question = currentQuestion,
                                        questionNumber = page + 1,
                                        bookTitle = parentBook?.title ?: if (currentQuestion.bookId == 0) "Legislação Planalto" else "Livro #${currentQuestion.bookId}",
                                        fontSizeMultiplier = fontSizeMultiplier,
                                        onAnswerSelected = { selectedIdx, isCorrect ->
                                            onAnswerSelected(currentQuestion.id, selectedIdx, isCorrect)
                                        },
                                        onResetAnswer = {
                                            onResetAnswer(currentQuestion.id)
                                        },
                                        onDeleteQuestion = {
                                            onDeleteQuestion(currentQuestion.id)
                                        },
                                        onSpeakText = onSpeakText,
                                        onOpenBookAtPage = if (parentBook != null && onOpenBookAtPage != null) {
                                            { onOpenBookAtPage(parentBook, currentQuestion.startPage) }
                                        } else null
                                    )
                                }

                                // Botão minimalista para avançar após responder
                                if (currentQuestion.isAnswered && page < displayedQuestions.size - 1) {
                                    item {
                                        Surface(
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(page + 1)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                                            border = BorderStroke(0.8.dp, Color(0xFFA78BFA)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 7.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Próxima Questão ▶",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                } else if (currentQuestion.isAnswered && page == displayedQuestions.size - 1) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            border = BorderStroke(0.8.dp, Color(0xFF10B981)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 7.dp, horizontal = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🎉 Final do Quiz • Acertos: $totalCorrect/$totalAnswered",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFD1FAE5)
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    // MODO FEED / LISTA COMPLETA ROLÁVEL
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(displayedQuestions, key = { _, q -> q.id }) { index, question ->
                            val parentBook = booksMap[question.bookId]
                            InteractiveQuestionCard(
                                question = question,
                                questionNumber = index + 1,
                                bookTitle = parentBook?.title ?: if (question.bookId == 0) "Legislação Planalto" else "Livro #${question.bookId}",
                                fontSizeMultiplier = fontSizeMultiplier,
                                onAnswerSelected = { selectedIdx, isCorrect ->
                                    onAnswerSelected(question.id, selectedIdx, isCorrect)
                                },
                                onResetAnswer = {
                                    onResetAnswer(question.id)
                                },
                                onDeleteQuestion = {
                                    onDeleteQuestion(question.id)
                                },
                                onSpeakText = onSpeakText,
                                onOpenBookAtPage = if (parentBook != null && onOpenBookAtPage != null) {
                                    { onOpenBookAtPage(parentBook, question.startPage) }
                                } else null
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmação para reiniciar rodada global
    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            title = {
                Text("Reiniciar Rodada?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Text("Isso limpará as respostas de todas as questões para que você possa responder do zero.", fontSize = 12.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllQuestionsGlobal()
                        showResetConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reiniciar Tudo", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmationDialog = false }) {
                    Text("Cancelar", fontSize = 11.sp)
                }
            }
        )
    }
}
