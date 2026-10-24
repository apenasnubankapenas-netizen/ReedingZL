package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ComparativeLanguageTerm
import com.example.data.model.ComparativeUsageExample
import com.example.data.model.PdfBook
import com.example.data.openai.OpenAiAudioPlayer
import com.example.data.openai.OpenAiService
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class ComparativeScopeTab {
    CURRENT_PAGE,
    THIS_BOOK,
    ALL_DICTIONARIES
}

enum class ComparativeLanguageFilter {
    ALL,
    PORTUGUESE,
    ENGLISH,
    FRENCH,
    SPANISH
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComparativeLanguageDialog(
    book: PdfBook,
    pageNumber: Int,
    pageText: String,
    pageTerms: List<ComparativeLanguageTerm>,
    bookTerms: List<ComparativeLanguageTerm>,
    allTerms: List<ComparativeLanguageTerm>,
    readingTheme: String = book.readingTheme ?: "MONOCHROME_BW",
    onDismiss: () -> Unit,
    onSaveTerms: (List<ComparativeLanguageTerm>) -> Unit,
    onUpdateTerm: (ComparativeLanguageTerm) -> Unit,
    onDeleteTerm: (ComparativeLanguageTerm) -> Unit,
    onDeleteTermsForPage: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = com.example.ui.theme.ReadingThemeManager.getPalette(readingTheme, book.category, book.categoryColor)

    var apiKey by remember { mutableStateOf(OpenAiService.getApiKey(context)) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isConfigExpanded by remember { mutableStateOf(apiKey.isBlank()) }
    var selectedModel by remember { mutableStateOf("gpt-4o-mini") }
    var selectedVoice by remember { mutableStateOf("nova") }

    var selectedTab by remember { mutableStateOf(ComparativeScopeTab.CURRENT_PAGE) }
    var selectedLangFilter by remember { mutableStateOf(ComparativeLanguageFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var fullScreenTerm by remember { mutableStateOf<ComparativeLanguageTerm?>(null) }

    val audioPlayer = remember { OpenAiAudioPlayer() }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    val baseTerms = remember(selectedTab, pageTerms, bookTerms, allTerms) {
        when (selectedTab) {
            ComparativeScopeTab.CURRENT_PAGE -> pageTerms
            ComparativeScopeTab.THIS_BOOK -> bookTerms.ifEmpty { pageTerms }
            ComparativeScopeTab.ALL_DICTIONARIES -> allTerms.ifEmpty { bookTerms.ifEmpty { pageTerms } }
        }
    }

    val availableCategories = remember(baseTerms) {
        baseTerms.map { it.grammaticalCategory }.filter { it.isNotBlank() }.distinct()
    }

    val filteredTerms = remember(baseTerms, searchQuery, selectedCategoryFilter) {
        baseTerms.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.term.contains(searchQuery, ignoreCase = true) ||
                    item.meaning.contains(searchQuery, ignoreCase = true) ||
                    item.englishTerm.contains(searchQuery, ignoreCase = true) ||
                    item.frenchTerm.contains(searchQuery, ignoreCase = true) ||
                    item.spanishTerm.contains(searchQuery, ignoreCase = true) ||
                    item.grammaticalCategory.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == null ||
                    item.grammaticalCategory.equals(selectedCategoryFilter, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.surface,
                        titleContentColor = palette.textPrimary,
                        navigationIconContentColor = palette.textPrimary
                    ),
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🌐 COMPARATIVO DE IDIOMAS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = palette.accent
                                ) {
                                    Text(
                                        text = "OpenAI",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.surface,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${book.title} • Pág. $pageNumber (Dicionário)",
                                fontSize = 11.sp,
                                color = palette.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = palette.textPrimary
                            )
                        }
                    },
                    actions = {
                        // Settings / API Key Button
                        IconButton(onClick = { isConfigExpanded = !isConfigExpanded }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Configurar API",
                                tint = if (apiKey.isNotBlank()) palette.accent else Color(0xFFF59E0B)
                            )
                        }

                        // Share All Extracted Terms Button
                        if (filteredTerms.isNotEmpty()) {
                            IconButton(onClick = {
                                val shareText = buildComparativeExportText(filteredTerms, book.title, pageNumber)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Comparativo de Idiomas - ${book.title} Pág $pageNumber")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartilhar Comparativo"))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartilhar",
                                    tint = palette.textPrimary
                                )
                            }
                        }
                    }
                )

                // Collapsible Config Drawer
                AnimatedVisibility(
                    visible = isConfigExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color(0xFF161E31),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chave da API da OpenAI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (apiKey.isNotBlank()) "✓ Chave Ativa" else "Chave Necessária",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (apiKey.isNotBlank()) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    OpenAiService.saveApiKey(context, it)
                                },
                                label = { Text("Chave da API da OpenAI (sk-...)", fontSize = 12.sp) },
                                placeholder = { Text("Cole sua chave sk-...", fontSize = 12.sp) },
                                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFA78BFA),
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Model Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("gpt-4o-mini" to "Mini", "gpt-4o" to "GPT-4o (Completo)").forEach { (mKey, mLabel) ->
                                    val isSel = selectedModel == mKey
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF7C3AED) else Color(0xFF0F172A),
                                        modifier = Modifier.clickable { selectedModel = mKey }
                                    ) {
                                        Text(
                                            text = mLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Scope Tabs
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color(0xFF131826),
                    contentColor = Color(0xFFA78BFA)
                ) {
                    Tab(
                        selected = selectedTab == ComparativeScopeTab.CURRENT_PAGE,
                        onClick = { selectedTab = ComparativeScopeTab.CURRENT_PAGE },
                        text = {
                            Text(
                                text = "Página $pageNumber (${pageTerms.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == ComparativeScopeTab.CURRENT_PAGE) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == ComparativeScopeTab.THIS_BOOK,
                        onClick = { selectedTab = ComparativeScopeTab.THIS_BOOK },
                        text = {
                            Text(
                                text = "Neste Dicionário (${bookTerms.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == ComparativeScopeTab.THIS_BOOK) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == ComparativeScopeTab.ALL_DICTIONARIES,
                        onClick = { selectedTab = ComparativeScopeTab.ALL_DICTIONARIES },
                        text = {
                            Text(
                                text = "Universal (${allTerms.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == ComparativeScopeTab.ALL_DICTIONARIES) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Search & Filter Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1120))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Pesquisar termo, verbo, substantivo ou tradução...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpar",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF161E31),
                            unfocusedContainerColor = Color(0xFF161E31)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Language Focus Chips (All, Portuguese, English, French, Spanish)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ComparativeFilterChip(
                            label = "Todos Idiomas",
                            isSelected = selectedLangFilter == ComparativeLanguageFilter.ALL,
                            onClick = { selectedLangFilter = ComparativeLanguageFilter.ALL }
                        )
                        ComparativeFilterChip(
                            label = "🇧🇷 Português",
                            isSelected = selectedLangFilter == ComparativeLanguageFilter.PORTUGUESE,
                            onClick = { selectedLangFilter = ComparativeLanguageFilter.PORTUGUESE }
                        )
                        ComparativeFilterChip(
                            label = "🇬🇧 Inglês",
                            isSelected = selectedLangFilter == ComparativeLanguageFilter.ENGLISH,
                            onClick = { selectedLangFilter = ComparativeLanguageFilter.ENGLISH }
                        )
                        ComparativeFilterChip(
                            label = "🇫🇷 Francês",
                            isSelected = selectedLangFilter == ComparativeLanguageFilter.FRENCH,
                            onClick = { selectedLangFilter = ComparativeLanguageFilter.FRENCH }
                        )
                        ComparativeFilterChip(
                            label = "🇪🇸 Espanhol",
                            isSelected = selectedLangFilter == ComparativeLanguageFilter.SPANISH,
                            onClick = { selectedLangFilter = ComparativeLanguageFilter.SPANISH }
                        )
                    }

                    // Category Filter Chips
                    if (availableCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("Todas Classes", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.35f),
                                    selectedLabelColor = Color(0xFFA78BFA),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF334155),
                                    selectedBorderColor = Color(0xFFA78BFA),
                                    enabled = true,
                                    selected = selectedCategoryFilter == null
                                )
                            )
                            availableCategories.forEach { category ->
                                val isSelected = selectedCategoryFilter.equals(category, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = if (isSelected) null else category },
                                    label = { Text(category, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.35f),
                                        selectedLabelColor = Color(0xFFA78BFA),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White.copy(alpha = 0.7f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFF334155),
                                        selectedBorderColor = Color(0xFFA78BFA),
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }

                // Main Content List or Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isProcessing) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFA78BFA),
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = statusMessage.ifBlank { "Analisando termos com IA da OpenAI..." },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Extraindo termos do dicionário, gerando traduções completas em Inglês, Francês e Espanhol, IPA fonético e 16 tempos verbais de uso (afirmações, perguntas, modais e conectivos).",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (filteredTerms.isEmpty()) {
                        EmptyComparativeState(
                            selectedTab = selectedTab,
                            pageNumber = pageNumber,
                            hasApiKey = apiKey.isNotBlank(),
                            onExtract = {
                                if (apiKey.isBlank()) {
                                    isConfigExpanded = true
                                    errorMessage = "Por favor, insira sua chave da API da OpenAI acima."
                                    return@EmptyComparativeState
                                }
                                scope.launch {
                                    isProcessing = true
                                    errorMessage = null
                                    statusMessage = "Conectando à OpenAI ($selectedModel)..."
                                    val result = OpenAiService.extractComparativeLanguageTerms(
                                        apiKey = apiKey,
                                        model = selectedModel,
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        pageText = pageText
                                    )
                                    isProcessing = false
                                    result.onSuccess { extracted ->
                                        onSaveTerms(extracted)
                                        Toast.makeText(
                                            context,
                                            "✨ ${extracted.size} termos comparativos extraídos com sucesso!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Erro desconhecido ao extrair comparativos."
                                    }
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${filteredTerms.size} termo(s) comparativo(s) • Toque para Detalhes e Áudio",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA78BFA)
                                    )
                                    if (selectedTab == ComparativeScopeTab.CURRENT_PAGE && pageTerms.isNotEmpty()) {
                                        Text(
                                            text = "🗑️ Limpar Página",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .clickable {
                                                    onDeleteTermsForPage(book.id, pageNumber)
                                                    Toast.makeText(context, "Termos da página excluídos", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }

                            items(filteredTerms, key = { if (it.id > 0) "comp_id_${it.id}" else "comp_${it.term}_${it.pageNumber}" }) { termItem ->
                                ComparativeTermCard(
                                    termItem = termItem,
                                    langFilter = selectedLangFilter,
                                    apiKey = apiKey,
                                    voice = selectedVoice,
                                    audioPlayer = audioPlayer,
                                    onOpenFullScreen = { fullScreenTerm = termItem },
                                    onDelete = { onDeleteTerm(termItem) },
                                    onCopy = {
                                        val copyText = buildSingleComparativeCopyText(termItem)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Termo Comparativo", copyText))
                                        Toast.makeText(context, "Termo copiado!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = Color(0xFF131826),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("← Voltar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (apiKey.isBlank()) {
                                    isConfigExpanded = true
                                    errorMessage = "Por favor, insira sua chave da API da OpenAI nas configurações."
                                    return@Button
                                }
                                scope.launch {
                                    isProcessing = true
                                    errorMessage = null
                                    statusMessage = "Extraindo comparativos linguísticos com OpenAI..."
                                    val result = OpenAiService.extractComparativeLanguageTerms(
                                        apiKey = apiKey,
                                        model = selectedModel,
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        pageText = pageText
                                    )
                                    isProcessing = false
                                    result.onSuccess { extracted ->
                                        onSaveTerms(extracted)
                                        Toast.makeText(
                                            context,
                                            "✨ ${extracted.size} termos comparativos extraídos com sucesso!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Falha ao extrair comparativos da página."
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.6f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (pageTerms.isNotEmpty()) Icons.Default.Refresh else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pageTerms.isNotEmpty()) "Re-extrair Comparativos" else "Extrair Comparativos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Full-screen / Detailed Modal
            if (fullScreenTerm != null) {
                FullScreenComparativeDialog(
                    termItem = fullScreenTerm!!,
                    apiKey = apiKey,
                    voice = selectedVoice,
                    audioPlayer = audioPlayer,
                    onDismiss = { fullScreenTerm = null }
                )
            }
        }
    }
}

@Composable
fun ComparativeFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFA78BFA) else Color(0xFF334155)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ComparativeTermCard(
    termItem: ComparativeLanguageTerm,
    langFilter: ComparativeLanguageFilter,
    apiKey: String,
    voice: String,
    audioPlayer: OpenAiAudioPlayer,
    onOpenFullScreen: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSpeakingTerm by remember { mutableStateOf(false) }

    // Parse usage examples to know count
    val examples = remember(termItem.usageExamplesJson) {
        parseUsageExamples(termItem.usageExamplesJson)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFullScreen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3D5C))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Term, Grammatical Category, Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = termItem.term,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF7C3AED).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED))
                        ) {
                            Text(
                                text = termItem.grammaticalCategory,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // IPA Phonetic Transcription (Crucial for Non-Latin alphabets and precise pronunciation)
                    if (termItem.ipaTranscription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "IPA: ${termItem.ipaTranscription}",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio Pronunciation Button
                    IconButton(
                        onClick = {
                            if (apiKey.isBlank()) {
                                Toast.makeText(context, "Configure a chave da API da OpenAI", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            scope.launch {
                                isSpeakingTerm = true
                                val audioRes = OpenAiService.generateSpeechAudio(
                                    apiKey = apiKey,
                                    text = termItem.term,
                                    voice = voice,
                                    cacheKey = "comp_term_${termItem.id}_${termItem.term.hashCode()}"
                                )
                                audioRes.onSuccess { f ->
                                    audioPlayer.playFile(f) {
                                        isSpeakingTerm = false
                                    }
                                }.onFailure {
                                    isSpeakingTerm = false
                                    Toast.makeText(context, "Falha ao reproduzir áudio", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeakingTerm) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ouvir Termo",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Fullscreen
                    IconButton(
                        onClick = onOpenFullScreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Expandir",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Meaning of the term
            if (termItem.meaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = termItem.meaning,
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Language Translations: Portuguese, English, French, Spanish
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1322), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF22314E), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (termItem.portugueseTerm.isNotBlank() && (langFilter == ComparativeLanguageFilter.ALL || langFilter == ComparativeLanguageFilter.PORTUGUESE)) {
                    LanguageRow(
                        flag = "🇧🇷",
                        langName = "Português",
                        term = termItem.portugueseTerm,
                        ipa = termItem.portugueseIpa,
                        meaning = termItem.portugueseMeaning,
                        accentColor = Color(0xFF10B981)
                    )
                }

                if (langFilter == ComparativeLanguageFilter.ALL || langFilter == ComparativeLanguageFilter.ENGLISH) {
                    LanguageRow(
                        flag = "🇬🇧",
                        langName = "Inglês",
                        term = termItem.englishTerm,
                        ipa = termItem.englishIpa,
                        meaning = termItem.englishMeaning,
                        accentColor = Color(0xFF38BDF8)
                    )
                }

                if (langFilter == ComparativeLanguageFilter.ALL || langFilter == ComparativeLanguageFilter.FRENCH) {
                    LanguageRow(
                        flag = "🇫🇷",
                        langName = "Francês",
                        term = termItem.frenchTerm,
                        ipa = termItem.frenchIpa,
                        meaning = termItem.frenchMeaning,
                        accentColor = Color(0xFFA78BFA)
                    )
                }

                if (langFilter == ComparativeLanguageFilter.ALL || langFilter == ComparativeLanguageFilter.SPANISH) {
                    LanguageRow(
                        flag = "🇪🇸",
                        langName = "Espanhol",
                        term = termItem.spanishTerm,
                        ipa = termItem.spanishIpa,
                        meaning = termItem.spanishMeaning,
                        accentColor = Color(0xFFF59E0B)
                    )
                }
            }

            // Summary of 16 Verb Tenses & Examples Available
            if (examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📖 ${examples.size} Tempos Verbais de Exemplos (8 Afirmações / 8 Perguntas)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA78BFA)
                    )
                    Text(
                        text = "Toque para abrir ↗",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageRow(
    flag: String,
    langName: String,
    term: String,
    ipa: String,
    meaning: String,
    accentColor: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = flag, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = langName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = term.ifBlank { "—" },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (ipa.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[$ipa]",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        if (meaning.isNotBlank()) {
            Text(
                text = meaning,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenComparativeDialog(
    termItem: ComparativeLanguageTerm,
    apiKey: String,
    voice: String,
    audioPlayer: OpenAiAudioPlayer,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var areExamplesExpanded by remember { mutableStateOf(true) }
    var isSpeakingTerm by remember { mutableStateOf(false) }

    val examples = remember(termItem.usageExamplesJson) {
        parseUsageExamples(termItem.usageExamplesJson)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090D16)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = termItem.term,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${termItem.grammaticalCategory} • Pág. ${termItem.pageNumber}",
                                fontSize = 11.sp,
                                color = Color(0xFFA78BFA)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Fechar",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Pronounce Original Term
                        IconButton(onClick = {
                            if (apiKey.isBlank()) {
                                Toast.makeText(context, "Configure a chave da API da OpenAI", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            scope.launch {
                                isSpeakingTerm = true
                                val audioRes = OpenAiService.generateSpeechAudio(
                                    apiKey = apiKey,
                                    text = termItem.term,
                                    voice = voice,
                                    cacheKey = "full_comp_${termItem.id}_${termItem.term.hashCode()}"
                                )
                                audioRes.onSuccess { f ->
                                    audioPlayer.playFile(f) {
                                        isSpeakingTerm = false
                                    }
                                }.onFailure {
                                    isSpeakingTerm = false
                                    Toast.makeText(context, "Falha ao reproduzir áudio", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isSpeakingTerm) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ouvir",
                                tint = Color(0xFFA78BFA)
                            )
                        }

                        // Copy Everything
                        IconButton(onClick = {
                            val copyText = buildSingleComparativeCopyText(termItem)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Termo Completo", copyText))
                            Toast.makeText(context, "Termo e 16 tempos copiados!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF131826))
                )

                // Scrollable Detailed Content
                SelectionContainer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // IPA Card (Essential for non-Latin alphabets and phonetics)
                        if (termItem.ipaTranscription.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🗣️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Alfabeto Fonético Internacional (IPA):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Text(
                                            text = termItem.ipaTranscription,
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Definition / Meaning Section
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3D5C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "📖 SIGNIFICADO E DEFINIÇÃO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFA78BFA)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = termItem.meaning.ifBlank { "Sem definição registrada." },
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    lineHeight = 20.sp
                                )

                                if (termItem.originalContext.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Contexto na página:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "“${termItem.originalContext}”",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }

                        // 3 Principal Language Sessions: English, French, Spanish
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3D5C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🌐 AS 3 SESSÕES DE TRADUÇÃO PRINCIPAIS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFA78BFA)
                                )

                                // English Session
                                FullLanguageBlock(
                                    flag = "🇬🇧",
                                    languageName = "INGLÊS (ENGLISH)",
                                    term = termItem.englishTerm,
                                    ipa = termItem.englishIpa,
                                    meaning = termItem.englishMeaning,
                                    accentColor = Color(0xFF38BDF8),
                                    apiKey = apiKey,
                                    voice = voice,
                                    audioPlayer = audioPlayer
                                )

                                // French Session
                                FullLanguageBlock(
                                    flag = "🇫🇷",
                                    languageName = "FRANCÊS (FRANÇAIS)",
                                    term = termItem.frenchTerm,
                                    ipa = termItem.frenchIpa,
                                    meaning = termItem.frenchMeaning,
                                    accentColor = Color(0xFFA78BFA),
                                    apiKey = apiKey,
                                    voice = voice,
                                    audioPlayer = audioPlayer
                                )

                                // Spanish Session
                                FullLanguageBlock(
                                    flag = "🇪🇸",
                                    languageName = "ESPANHOL (ESPAÑOL)",
                                    term = termItem.spanishTerm,
                                    ipa = termItem.spanishIpa,
                                    meaning = termItem.spanishMeaning,
                                    accentColor = Color(0xFFF59E0B),
                                    apiKey = apiKey,
                                    voice = voice,
                                    audioPlayer = audioPlayer
                                )
                            }
                        }

                        // 16 Verb Tenses of Illustrative Usage Examples (Collapsible)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3D5C)),
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
                                            text = "⏱️ 16 TEMPOS VERBAIS DE USO",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFA78BFA)
                                        )
                                        Text(
                                            text = "8 Afirmações • 8 Perguntas • Modais & Conectivos",
                                            fontSize = 10.5.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Toggle Button to Hide / Show Large Examples
                                    Button(
                                        onClick = { areExamplesExpanded = !areExamplesExpanded },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (areExamplesExpanded) Color(0xFF334155) else Color(0xFF7C3AED),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (areExamplesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (areExamplesExpanded) "Ocultar Exemplos" else "Exibir Exemplos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = areExamplesExpanded,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (examples.isEmpty()) {
                                            Text(
                                                text = "Nenhum exemplo gerado no momento. Use 'Re-extrair Comparativos' para gerar os 16 tempos verbais.",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                        } else {
                                            examples.forEach { ex ->
                                                UsageExampleItem(
                                                    example = ex,
                                                    apiKey = apiKey,
                                                    voice = voice,
                                                    audioPlayer = audioPlayer
                                                )
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
    }
}

@Composable
fun FullLanguageBlock(
    flag: String,
    languageName: String,
    term: String,
    ipa: String,
    meaning: String,
    accentColor: Color,
    apiKey: String,
    voice: String,
    audioPlayer: OpenAiAudioPlayer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSpeaking by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0C1322),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22314E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = flag, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = languageName,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }

                if (term.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (apiKey.isBlank()) {
                                Toast.makeText(context, "Configure a chave da API da OpenAI", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            scope.launch {
                                isSpeaking = true
                                val audioRes = OpenAiService.generateSpeechAudio(
                                    apiKey = apiKey,
                                    text = term,
                                    voice = voice,
                                    cacheKey = "comp_lang_${term.hashCode()}"
                                )
                                audioRes.onSuccess { f ->
                                    audioPlayer.playFile(f) {
                                        isSpeaking = false
                                    }
                                }.onFailure {
                                    isSpeaking = false
                                    Toast.makeText(context, "Falha no áudio", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ouvir",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = term.ifBlank { "Sem tradução disponível" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (ipa.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[$ipa]",
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            if (meaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = meaning,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun UsageExampleItem(
    example: ComparativeUsageExample,
    apiKey: String,
    voice: String,
    audioPlayer: OpenAiAudioPlayer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSpeakingEnglish by remember { mutableStateOf(false) }

    val isQuestion = example.type.contains("Pergunta", ignoreCase = true)
    val badgeColor = if (isQuestion) Color(0xFFEC4899) else Color(0xFF10B981)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0C1322),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22314E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header of example: Tense name, Type (Affirmation/Question), Modal/Connector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = if (isQuestion) "❓ PERGUNTA" else "✓ AFIRMAÇÃO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = example.tenseName.ifBlank { "Tempo ${example.tenseNumber}" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (example.englishSentence.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (apiKey.isBlank()) {
                                Toast.makeText(context, "Configure a chave da API", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            scope.launch {
                                isSpeakingEnglish = true
                                val audioRes = OpenAiService.generateSpeechAudio(
                                    apiKey = apiKey,
                                    text = example.englishSentence,
                                    voice = voice,
                                    cacheKey = "ex_en_${example.englishSentence.hashCode()}"
                                )
                                audioRes.onSuccess { f ->
                                    audioPlayer.playFile(f) {
                                        isSpeakingEnglish = false
                                    }
                                }.onFailure {
                                    isSpeakingEnglish = false
                                    Toast.makeText(context, "Falha no áudio", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeakingEnglish) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ouvir Frase",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (example.modalOrConnector.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = example.modalOrConnector,
                    fontSize = 10.sp,
                    color = Color(0xFFA78BFA),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // English sentence
            if (example.englishSentence.isNotBlank()) {
                Text(
                    text = "🇬🇧 ${example.englishSentence}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )
            }

            // French sentence
            if (example.frenchSentence.isNotBlank()) {
                Text(
                    text = "🇫🇷 ${example.frenchSentence}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFA78BFA)
                )
            }

            // Spanish sentence
            if (example.spanishSentence.isNotBlank()) {
                Text(
                    text = "🇪🇸 ${example.spanishSentence}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFF59E0B)
                )
            }

            // Portuguese sentence
            if (example.portugueseSentence.isNotBlank()) {
                Text(
                    text = "🇧🇷 ${example.portugueseSentence}",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun EmptyComparativeState(
    selectedTab: ComparativeScopeTab,
    pageNumber: Int,
    hasApiKey: Boolean,
    onExtract: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF7C3AED).copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = Color(0xFFA78BFA),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Nenhum Termo Comparativo Extraído",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Exclusivo para dicionários e enciclopédias: extraia termos com tradução completa em Inglês, Francês e Espanhol, fonética IPA e 16 tempos verbais com afirmações, perguntas, modais e conectivos.",
            fontSize = 12.5.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onExtract,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Extrair Comparativos da Pág. $pageNumber",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun parseUsageExamples(jsonStr: String): List<ComparativeUsageExample> {
    if (jsonStr.isBlank()) return emptyList()
    val list = mutableListOf<ComparativeUsageExample>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                ComparativeUsageExample(
                    tenseNumber = obj.optInt("tenseNumber", i + 1),
                    tenseName = obj.optString("tenseName", "Tempo ${i + 1}"),
                    type = obj.optString("type", "Afirmação"),
                    modalOrConnector = obj.optString("modalOrConnector", ""),
                    englishSentence = obj.optString("englishSentence", ""),
                    frenchSentence = obj.optString("frenchSentence", ""),
                    spanishSentence = obj.optString("spanishSentence", ""),
                    portugueseSentence = obj.optString("portugueseSentence", ""),
                    ipaEnglish = obj.optString("ipaEnglish", "")
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

fun buildSingleComparativeCopyText(term: ComparativeLanguageTerm): String {
    val sb = StringBuilder()
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    sb.appendLine("📌 ${term.term.uppercase()} (${term.grammaticalCategory})")
    if (term.ipaTranscription.isNotBlank()) {
        sb.appendLine("🗣️ IPA: ${term.ipaTranscription}")
    }
    if (term.meaning.isNotBlank()) {
        sb.appendLine("📖 Definição: ${term.meaning}")
    }
    sb.appendLine()
    sb.appendLine("🌐 TRADUÇÕES E DEFINIÇÕES MULTILÍNGUES:")
    if (term.portugueseTerm.isNotBlank()) {
        sb.appendLine("🇧🇷 Português: ${term.portugueseTerm} ${if (term.portugueseIpa.isNotBlank()) "[${term.portugueseIpa}]" else ""} - ${term.portugueseMeaning}")
    }
    sb.appendLine("🇬🇧 Inglês: ${term.englishTerm} ${if (term.englishIpa.isNotBlank()) "[${term.englishIpa}]" else ""} - ${term.englishMeaning}")
    sb.appendLine("🇫🇷 Francês: ${term.frenchTerm} ${if (term.frenchIpa.isNotBlank()) "[${term.frenchIpa}]" else ""} - ${term.frenchMeaning}")
    sb.appendLine("🇪🇸 Espanhol: ${term.spanishTerm} ${if (term.spanishIpa.isNotBlank()) "[${term.spanishIpa}]" else ""} - ${term.spanishMeaning}")

    val examples = parseUsageExamples(term.usageExamplesJson)
    if (examples.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("⏱️ 16 TEMPOS VERBAIS DE EXEMPLOS:")
        examples.forEach { ex ->
            sb.appendLine("• [${ex.tenseName}] (${ex.type}) ${if (ex.modalOrConnector.isNotBlank()) "[${ex.modalOrConnector}]" else ""}")
            if (ex.englishSentence.isNotBlank()) sb.appendLine("  🇬🇧 ${ex.englishSentence}")
            if (ex.frenchSentence.isNotBlank()) sb.appendLine("  🇫🇷 ${ex.frenchSentence}")
            if (ex.spanishSentence.isNotBlank()) sb.appendLine("  🇪🇸 ${ex.spanishSentence}")
            if (ex.portugueseSentence.isNotBlank()) sb.appendLine("  🇧🇷 ${ex.portugueseSentence}")
        }
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    return sb.toString()
}

fun buildComparativeExportText(terms: List<ComparativeLanguageTerm>, bookTitle: String, pageNumber: Int): String {
    val sb = StringBuilder()
    sb.appendLine("📚 COMPARATIVO LINGUÍSTICO (OPENAI) - $bookTitle")
    sb.appendLine("Página: $pageNumber • Total de Termos: ${terms.size}")
    sb.appendLine()
    terms.forEach { term ->
        sb.append(buildSingleComparativeCopyText(term))
        sb.appendLine()
    }
    return sb.toString()
}
