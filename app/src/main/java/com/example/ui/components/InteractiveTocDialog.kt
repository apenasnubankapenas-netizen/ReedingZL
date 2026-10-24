package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.example.data.model.BookCategoryTaxonomy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PdfBook
import com.example.data.pdf.PdfOutlineExtractor
import com.example.data.pdf.PdfRendererHelper
import com.example.data.pdf.PdfTocItem
import kotlinx.coroutines.launch

/**
 * Diálogo interativo profissional de Sumário / Marcadores e Links (Table of Contents).
 * Experiência idêntica ao leitor de PDF do Microsoft Edge e VS Code Tree View:
 *
 * 1. Ao clicar em um Tema (com subtemas): expande/recolhe seus subtemas diretamente.
 * 2. Ao clicar em um Subtema: salta imediatamente para a página vinculada e fecha.
 * 3. Permite saltar para o tema principal pelo chip de página "p. X".
 * 4. Aba dedicada "Todos os Links do Livro" que extrai todos os links web e referências do PDF.
 */
@Composable
fun InteractiveTocDialog(
    book: PdfBook,
    currentPage: Int,
    tocItems: List<PdfTocItem>,
    readingTheme: String = "MONOCHROME_BW",
    onDismiss: () -> Unit,
    onSelectPage: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var internalItems by remember(book.id) { mutableStateOf(if (tocItems.isNotEmpty()) tocItems else emptyList()) }
    var isLoadingOutline by remember(book.id) { mutableStateOf(false) }
    var searchQuery by remember(book.id) { mutableStateOf("") }
    var quickPageInput by remember(book.id) { mutableStateOf("") }
    var selectedTab by remember(book.id) { mutableIntStateOf(0) } // 0 = Sumário, 1 = Links do Livro

    // Carregar dinamicamente se a lista estiver vazia ou sincronizar com tocItems do livro atual
    LaunchedEffect(book.id, tocItems) {
        if (tocItems.isNotEmpty()) {
            internalItems = tocItems
            isLoadingOutline = false
        } else {
            isLoadingOutline = true
            val loaded = PdfRendererHelper.generateTableOfContents(context, book)
            internalItems = loaded
            isLoadingOutline = false
        }
    }

    // Organizar itens em árvore de tópicos
    val treeRoots = remember(internalItems) {
        PdfRendererHelper.buildTocTree(internalItems)
    }

    // Extrair todos os links do livro a partir da árvore e do arquivo PDF
    var bookLinksList by remember(book.id, treeRoots) { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    LaunchedEffect(book.id, treeRoots) {
        val collected = mutableListOf<Pair<String, Int>>()
        fun collectFromTree(nodes: List<PdfTocItem>) {
            for (node in nodes) {
                for (link in node.links) {
                    collected.add(Pair(link, node.pageNumber))
                }
                collectFromTree(node.children)
            }
        }
        collectFromTree(treeRoots)

        // Tentar extrair do arquivo nativo se houver links adicionais (com suporte a arquivos de até 10GB)
        try {
            val (_, nativeLinks) = PdfOutlineExtractor.extractOutlineAndLinks(context, book)
            for (url in nativeLinks) {
                if (collected.none { it.first == url }) {
                    collected.add(Pair(url, 1))
                }
            }
        } catch (_: Exception) {}

        bookLinksList = collected.distinctBy { it.first }
    }

    // Conjunto de nós expandidos (por padrão, os temas de nível 0 começam expandidos)
    var expandedNodeIds by remember(treeRoots) {
        val initial = mutableSetOf<String>()
        fun collectInitial(list: List<PdfTocItem>) {
            for (node in list) {
                if (node.level == 0 && node.children.isNotEmpty()) {
                    initial.add(node.id)
                }
            }
        }
        collectInitial(treeRoots)
        mutableStateOf(initial.toSet())
    }

    // Paleta de cores baseada no tema de leitura
    val (bgColor, textColor, surfaceColor, accentColor, bulletColor) = when (readingTheme) {
        "CATEGORY_COLOR" -> {
            val pal = BookCategoryTaxonomy.getPaletteForCategory(book.category, book.categoryColor)
            Quintuple(
                pal.headerBg,
                pal.textColor,
                pal.cardBg,
                pal.primaryColor,
                pal.badgeBg
            )
        }
        "PAPER_SEPIA" -> Quintuple(
            Color(0xFF231D15),
            Color(0xFFF5E6D3),
            Color(0xFF33291E),
            Color(0xFFD4A373),
            Color(0xFF8B5E34)
        )
        "DARK_OLED", "NIGHT_BLUE" -> Quintuple(
            Color(0xFF000000),
            Color(0xFFF3F4F6),
            Color(0xFF141416),
            Color(0xFFA78BFA),
            Color(0xFFD1D5DB)
        )
        "CINEMA_YELLOW" -> Quintuple(
            Color(0xFF121000),
            Color(0xFFFFE600),
            Color(0xFF221E00),
            Color(0xFFFFE600),
            Color(0xFFFFEA33)
        )
        "CYBERPUNK_NEON" -> Quintuple(
            Color(0xFF080210),
            Color(0xFF00F0FF),
            Color(0xFF160628),
            Color(0xFFFF007F),
            Color(0xFF00F0FF)
        )
        "STREAMING_BOX" -> Quintuple(
            Color(0xFF10121A),
            Color(0xFFF3F4F6),
            Color(0xFF1C2030),
            Color(0xFF818CF8),
            Color(0xFF6366F1)
        )
        "TERMINAL_GREEN" -> Quintuple(
            Color(0xFF020C04),
            Color(0xFF00FF66),
            Color(0xFF071C0A),
            Color(0xFF00FF66),
            Color(0xFF33FF88)
        )
        else -> Quintuple(
            Color(0xFF121214),
            Color(0xFFF4F4F5),
            Color(0xFF1E1E24),
            Color(0xFFC084FC),
            Color(0xFFA855F7)
        )
    }

    // Achatamento da árvore de acordo com expansão e busca
    val visibleItems = remember(treeRoots, expandedNodeIds, searchQuery) {
        if (searchQuery.isNotBlank()) {
            val matchedIds = mutableSetOf<String>()
            val ancestorMap = mutableMapOf<String, String>()

            fun mapAncestors(nodes: List<PdfTocItem>, parentId: String?) {
                for (node in nodes) {
                    if (parentId != null) ancestorMap[node.id] = parentId
                    val matches = node.title.contains(searchQuery, ignoreCase = true) ||
                            node.sectionNumber.contains(searchQuery, ignoreCase = true) ||
                            node.snippet.contains(searchQuery, ignoreCase = true) ||
                            node.links.any { it.contains(searchQuery, ignoreCase = true) }
                    if (matches) {
                        matchedIds.add(node.id)
                    }
                    mapAncestors(node.children, node.id)
                }
            }
            mapAncestors(treeRoots, null)

            val expandedForSearch = mutableSetOf<String>()
            for (id in matchedIds) {
                var cur = id
                while (ancestorMap.containsKey(cur)) {
                    val p = ancestorMap[cur]!!
                    expandedForSearch.add(p)
                    cur = p
                }
            }

            val flattened = mutableListOf<PdfTocItem>()
            fun collectVisibleSearch(nodes: List<PdfTocItem>) {
                for (node in nodes) {
                    val isDirectMatch = matchedIds.contains(node.id)
                    val isAncestor = expandedForSearch.contains(node.id)
                    if (isDirectMatch || isAncestor) {
                        flattened.add(node)
                        collectVisibleSearch(node.children)
                    }
                }
            }
            collectVisibleSearch(treeRoots)
            flattened
        } else {
            val flattened = mutableListOf<PdfTocItem>()
            fun collectVisible(nodes: List<PdfTocItem>) {
                for (node in nodes) {
                    flattened.add(node)
                    if (expandedNodeIds.contains(node.id)) {
                        collectVisible(node.children)
                    }
                }
            }
            collectVisible(treeRoots)
            flattened
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.12f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header com Título do Livro e Ações Globais
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
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = "Sumário",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sumário e Marcadores",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "${book.title} • ${book.totalPages} páginas",
                                fontSize = 11.5.sp,
                                color = textColor.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Ações: Recarregar Outline, Expandir Tudo, Recolher Tudo, Fechar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoadingOutline = true
                                    val refreshed = PdfRendererHelper.generateTableOfContents(context, book, forceRefresh = true)
                                    internalItems = refreshed
                                    isLoadingOutline = false
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Escanear PDF novamente",
                                tint = textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val all = mutableSetOf<String>()
                                fun collectAll(nodes: List<PdfTocItem>) {
                                    for (n in nodes) {
                                        if (n.children.isNotEmpty()) {
                                            all.add(n.id)
                                            collectAll(n.children)
                                        }
                                    }
                                }
                                collectAll(treeRoots)
                                expandedNodeIds = all
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Expandir Todos",
                                tint = textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { expandedNodeIds = emptySet() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandLess,
                                contentDescription = "Recolher Todos",
                                tint = textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(textColor.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Abas Superiores: [ 📑 Sumário e Subtemas ] vs [ 🔗 Links do Livro ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 0) accentColor else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.White else textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sumário Hierárquico (${treeRoots.size})",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color.White else textColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 1) accentColor else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Links do Livro (${bookLinksList.size})",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color.White else textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Barra de Busca e Salto Rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (selectedTab == 0) "Buscar temas, subtópicos..." else "Buscar links ou páginas...",
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Limpar",
                                        tint = textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedContainerColor = surfaceColor.copy(alpha = 0.5f),
                            unfocusedContainerColor = surfaceColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )

                    // Quick Jump Box "Ir para pág"
                    OutlinedTextField(
                        value = quickPageInput,
                        onValueChange = { quickPageInput = it.filter { ch -> ch.isDigit() }.take(4) },
                        placeholder = {
                            Text("Pág", fontSize = 11.sp, color = textColor.copy(alpha = 0.4f))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val p = quickPageInput.toIntOrNull()
                                if (p != null && p in 1..book.totalPages) {
                                    onSelectPage(p)
                                    onDismiss()
                                }
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedContainerColor = surfaceColor.copy(alpha = 0.5f),
                            unfocusedContainerColor = surfaceColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .width(68.dp)
                            .height(44.dp)
                    )

                    IconButton(
                        onClick = {
                            val p = quickPageInput.toIntOrNull()
                            if (p != null && p in 1..book.totalPages) {
                                onSelectPage(p)
                                onDismiss()
                            }
                        },
                        enabled = quickPageInput.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (quickPageInput.isNotBlank()) accentColor else surfaceColor,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ir",
                            tint = if (quickPageInput.isNotBlank()) Color.White else textColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Área de Conteúdo
                if (isLoadingOutline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = accentColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Escaneando marcadores, sumário e links do PDF...",
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (selectedTab == 0) {
                    // TAB 0: SUMÁRIO HIERÁRQUICO
                    if (visibleItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Nenhum tópico encontrado para \"$searchQuery\"" else "Nenhum sumário encontrado no documento.",
                                    fontSize = 13.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(visibleItems, key = { it.id }) { item ->
                                val isCurrentPage = item.pageNumber == currentPage
                                val hasChildren = item.children.isNotEmpty()
                                val isExpanded = expandedNodeIds.contains(item.id)
                                val indentDp = (item.level * 16).dp

                                val chevronRotation by animateFloatAsState(
                                    targetValue = if (isExpanded) 90f else 0f,
                                    label = "chevron_rot"
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCurrentPage) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                                    border = if (isCurrentPage) BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (hasChildren) {
                                                // COMPORTAMENTO EXATO MICROSOFT EDGE:
                                                // Clicar em um tema com subtemas abre/recolhe seus subtemas!
                                                expandedNodeIds = if (isExpanded) {
                                                    expandedNodeIds - item.id
                                                } else {
                                                    expandedNodeIds + item.id
                                                }
                                            } else {
                                                // Clicar em um subtema (folha): salta diretamente para a página!
                                                onSelectPage(item.pageNumber)
                                                onDismiss()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 4.dp + indentDp,
                                                end = 6.dp,
                                                top = if (item.level == 0) 6.dp else 4.dp,
                                                bottom = if (item.level == 0) 6.dp else 4.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Linha-guia vertical para níveis recuados (igual ao VS Code)
                                        if (item.level > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .width(1.5.dp)
                                                    .height(18.dp)
                                                    .background(textColor.copy(alpha = 0.15f))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        // Botão ou Chevron de expansão/recolhimento
                                        if (hasChildren) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowRight,
                                                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                                    tint = if (isExpanded) accentColor else textColor.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .rotate(chevronRotation)
                                                )
                                            }
                                        } else {
                                            // Marcador bullet estilizado "▪" para itens folha
                                            Box(
                                                modifier = Modifier.size(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (item.level == 1) 5.dp else 4.dp)
                                                        .background(
                                                            if (isCurrentPage) accentColor else bulletColor.copy(alpha = 0.7f),
                                                            RoundedCornerShape(1.dp)
                                                        )
                                                )
                                            }
                                        }

                                        // Ícone de pasta/capítulo para temas principais
                                        if (item.level == 0) {
                                            Icon(
                                                imageVector = if (hasChildren && isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = if (isCurrentPage) accentColor else textColor.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .padding(end = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        // Título do Tema ou Subtema
                                        Text(
                                            text = item.title,
                                            fontSize = if (item.level == 0) 13.5.sp else 12.2.sp,
                                            fontWeight = if (isCurrentPage) FontWeight.Bold else if (item.level == 0) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isCurrentPage) accentColor else textColor,
                                            fontFamily = FontFamily.SansSerif,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Badge se contiver links
                                        if (item.links.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = "Contém link",
                                                tint = accentColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        // Contador discreto de subtemas vinculados
                                        if (hasChildren) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = surfaceColor,
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${item.children.size}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor.copy(alpha = 0.6f),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Chip clicável de Página de Destino
                                        // (Permite saltar para a página do tema pai mesmo se ele tiver subtemas)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isCurrentPage) accentColor else surfaceColor,
                                            border = BorderStroke(0.5.dp, if (isCurrentPage) Color.Transparent else textColor.copy(alpha = 0.15f)),
                                            modifier = Modifier.clickable {
                                                onSelectPage(item.pageNumber)
                                                onDismiss()
                                            }
                                        ) {
                                            Text(
                                                text = if (isCurrentPage) "Pág ${item.pageNumber} • Atual" else "p. ${item.pageNumber}",
                                                fontSize = 10.sp,
                                                fontWeight = if (isCurrentPage) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrentPage) Color.White else textColor.copy(alpha = 0.75f),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: TODOS OS LINKS DO LIVRO
                    val filteredLinks = remember(bookLinksList, searchQuery) {
                        if (searchQuery.isBlank()) bookLinksList
                        else bookLinksList.filter { it.first.contains(searchQuery, ignoreCase = true) }
                    }

                    if (filteredLinks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Nenhum link encontrado para \"$searchQuery\"" else "Nenhum hiperlink externo encontrado nas anotações do PDF.",
                                    fontSize = 12.5.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredLinks) { (url, page) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = surfaceColor,
                                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = url,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Referência localizada na página $page",
                                                fontSize = 10.5.sp,
                                                color = textColor.copy(alpha = 0.55f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Botão Ir para Página
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = bgColor,
                                            border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.2f)),
                                            modifier = Modifier.clickable {
                                                onSelectPage(page)
                                                onDismiss()
                                            }
                                        ) {
                                            Text(
                                                text = "p. $page",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Botão Abrir URL no Navegador
                                        FilledTonalIconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Não foi possível abrir o link: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Abrir no navegador",
                                                tint = accentColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Rodapé com Contadores e Botão Fechar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "${visibleItems.size} tópico(s) visíveis no sumário" else "${bookLinksList.size} link(s) encontrados no livro",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )

                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Fechar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
