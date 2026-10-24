package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PdfBook
import com.example.data.offline.OfflineReadingEngine
import com.example.data.openai.OpenAiAudioPlayer
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class DictionaryViewMode {
    CURRENT_PAGE,
    CURRENT_BOOK,
    ALL_BOOKS,
    LANGUAGE_STUDY
}

enum class LanguageFilter {
    ALL,
    SPANISH,
    FRENCH,
    ENGLISH
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OpenAiDictionaryDialog(
    book: PdfBook,
    pageNumber: Int,
    pageText: String,
    pageTerms: List<PageDictionaryTerm>,
    bookTerms: List<PageDictionaryTerm> = emptyList(),
    allTerms: List<PageDictionaryTerm> = emptyList(),
    initialSearchQuery: String = "",
    initialFocusedTerm: PageDictionaryTerm? = null,
    readingTheme: String = book.readingTheme ?: "MONOCHROME_BW",
    speechManager: PdfSpeechManager? = null,
    verbsList: List<com.example.data.model.VerbPhrasalVerbStudyTerm> = emptyList(),
    onSaveVerbs: (List<com.example.data.model.VerbPhrasalVerbStudyTerm>) -> Unit = {},
    onToggleFavoriteVerb: (com.example.data.model.VerbPhrasalVerbStudyTerm) -> Unit = {},
    onSaveTerms: (List<PageDictionaryTerm>) -> Unit,
    onUpdateTerm: (PageDictionaryTerm) -> Unit = {},
    onDeleteTerm: (PageDictionaryTerm) -> Unit,
    onDeleteTermsForPage: (bookId: Int, page: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = com.example.ui.theme.ReadingThemeManager.getPalette(readingTheme, book.category, book.categoryColor)
    val localSpeechManager = remember { speechManager ?: PdfSpeechManager(context) }

    var apiKey by remember { mutableStateOf(OpenAiService.getSavedApiKey(context)) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("gpt-4o-mini") }
    var selectedVoice by remember { mutableStateOf("alloy") }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConfigExpanded by remember { mutableStateOf(pageTerms.isEmpty() && apiKey.isBlank()) }

    var selectedTab by remember { mutableStateOf(DictionaryViewMode.CURRENT_PAGE) }
    var selectedLang by remember { mutableStateOf(LanguageFilter.ALL) }
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showCategoryFilters by remember { mutableStateOf(false) }

    // Full screen term view state
    var fullScreenTerm by remember { mutableStateOf<PageDictionaryTerm?>(initialFocusedTerm) }

    LaunchedEffect(initialFocusedTerm) {
        if (initialFocusedTerm != null) {
            fullScreenTerm = initialFocusedTerm
        }
    }

    // Audio player for dictionary terms
    val audioPlayer = remember { OpenAiAudioPlayer() }
    val isAudioPlaying by audioPlayer.isPlaying.collectAsState()
    val audioPosMs by audioPlayer.currentPositionMs.collectAsState()
    val audioDurationMs by audioPlayer.durationMs.collectAsState()
    val activeAudioPath by audioPlayer.activeFilePath.collectAsState()
    val audioSpeed by audioPlayer.playbackSpeed.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
            localSpeechManager.stop()
        }
    }

    // Active terms depending on selected tab
    val baseTerms = when (selectedTab) {
        DictionaryViewMode.CURRENT_PAGE -> pageTerms
        DictionaryViewMode.CURRENT_BOOK -> bookTerms.ifEmpty { pageTerms }
        DictionaryViewMode.ALL_BOOKS -> allTerms.ifEmpty { bookTerms.ifEmpty { pageTerms } }
        DictionaryViewMode.LANGUAGE_STUDY -> allTerms.ifEmpty { bookTerms.ifEmpty { pageTerms } }
    }

    val availableCategories = remember(baseTerms) {
        baseTerms.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredTerms = remember(baseTerms, searchQuery, selectedCategoryFilter) {
        baseTerms.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.term.contains(searchQuery, ignoreCase = true) ||
                    item.originalDefinition.contains(searchQuery, ignoreCase = true) ||
                    item.spanishTerm.contains(searchQuery, ignoreCase = true) ||
                    item.frenchTerm.contains(searchQuery, ignoreCase = true) ||
                    item.englishTerm.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == null || item.category.equals(selectedCategoryFilter, ignoreCase = true)
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
                                    text = "📚 DICIONÁRIO UNIVERSAL",
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
                                text = "${book.title} • Pág. $pageNumber",
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
                        IconButton(onClick = { isConfigExpanded = !isConfigExpanded }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Configurar Chave OpenAI",
                                tint = if (apiKey.isNotBlank()) Color(0xFF38BDF8) else Color(0xFFEF4444)
                            )
                        }
                        if (baseTerms.isNotEmpty()) {
                            IconButton(onClick = {
                                val exportText = buildExportText(book.title, pageNumber, baseTerms)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Dicionário de Termos - ${book.title} (Pág $pageNumber)")
                                    putExtra(Intent.EXTRA_TEXT, exportText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Dicionário"))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartilhar Termos",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White
                            )
                        }
                    }
                )

                // Settings & API Key Drawer (collapsible)
                AnimatedVisibility(
                    visible = isConfigExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color(0xFF162234),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3F5B)),
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
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Configuração da API da OpenAI",
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
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF3B4D66),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF38BDF8),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Model & Voice Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modelo Extração:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("gpt-4o-mini" to "Mini", "gpt-4o" to "GPT-4o").forEach { (mKey, mLabel) ->
                                            val isSel = selectedModel == mKey
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) Color(0xFF0284C7) else Color(0xFF0F172A),
                                                modifier = Modifier.clickable { selectedModel = mKey }
                                            ) {
                                                Text(
                                                    text = mLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = "Voz Neural para Áudios:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        OpenAiService.AVAILABLE_VOICES.forEach { (vKey, vName) ->
                                            val isSel = selectedVoice == vKey
                                            val shortName = vName.substringBefore(" ")
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) Color(0xFF0284C7) else Color(0xFF0F172A),
                                                modifier = Modifier.clickable { selectedVoice = vKey }
                                            ) {
                                                Text(
                                                    text = shortName,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
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

                // Top Scope Tabs: [Esta Página (Pág X)] [Neste Livro] [Dicionário Universal (Todos)]
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color(0xFF141C28),
                    contentColor = Color(0xFF38BDF8),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = Color(0xFF38BDF8),
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == DictionaryViewMode.CURRENT_PAGE,
                        onClick = { selectedTab = DictionaryViewMode.CURRENT_PAGE },
                        text = {
                            Text(
                                text = "Esta Página (${pageTerms.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == DictionaryViewMode.CURRENT_PAGE) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == DictionaryViewMode.CURRENT_BOOK,
                        onClick = { selectedTab = DictionaryViewMode.CURRENT_BOOK },
                        text = {
                            Text(
                                text = "Neste Livro (${bookTerms.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == DictionaryViewMode.CURRENT_BOOK) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == DictionaryViewMode.ALL_BOOKS,
                        onClick = { selectedTab = DictionaryViewMode.ALL_BOOKS },
                        text = {
                            Text(
                                text = "Universal (${allTerms.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == DictionaryViewMode.ALL_BOOKS) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == DictionaryViewMode.LANGUAGE_STUDY,
                        onClick = { selectedTab = DictionaryViewMode.LANGUAGE_STUDY },
                        text = {
                            Text(
                                text = "🎯 Idiomas & Verbos",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == DictionaryViewMode.LANGUAGE_STUDY) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == DictionaryViewMode.LANGUAGE_STUDY) Color(0xFFFACC15) else Color.White
                            )
                        }
                    )
                }

                // Search Bar and Language Filter Chips (Apenas para abas gerais de termos)
                if (selectedTab != DictionaryViewMode.LANGUAGE_STUDY) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Pesquisar substantivo, processo, termo jurídico...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
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
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2E3F5B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF141C28),
                            unfocusedContainerColor = Color(0xFF141C28)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Language Filter Chips (Horizontal scrollable to prevent wrapping/overflow)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageFilterChip(
                            label = "Todos Idiomas",
                            isSelected = selectedLang == LanguageFilter.ALL,
                            onClick = { selectedLang = LanguageFilter.ALL }
                        )
                        LanguageFilterChip(
                            label = "🇪🇸 Espanhol",
                            isSelected = selectedLang == LanguageFilter.SPANISH,
                            onClick = { selectedLang = LanguageFilter.SPANISH }
                        )
                        LanguageFilterChip(
                            label = "🇫🇷 Francês",
                            isSelected = selectedLang == LanguageFilter.FRENCH,
                            onClick = { selectedLang = LanguageFilter.FRENCH }
                        )
                        LanguageFilterChip(
                            label = "🇬🇧 Inglês",
                            isSelected = selectedLang == LanguageFilter.ENGLISH,
                            onClick = { selectedLang = LanguageFilter.ENGLISH }
                        )
                    }

                    // Category Filter Mechanism (Collapsible, single-line horizontal LazyRow, never expands vertically)
                    if (availableCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showCategoryFilters = !showCategoryFilters }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = if (selectedCategoryFilter != null) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedCategoryFilter != null)
                                        "Filtro ativo: $selectedCategoryFilter"
                                    else
                                        "Categorias (${availableCategories.size})",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedCategoryFilter != null) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedCategoryFilter != null) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showCategoryFilters) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            if (selectedCategoryFilter != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable { selectedCategoryFilter = null }
                                ) {
                                    Text(
                                        text = "✕ Limpar filtro",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Strictly 1-line horizontal scrollable row, never wraps or fills the screen
                        AnimatedVisibility(
                            visible = showCategoryFilters,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == null,
                                        onClick = { selectedCategoryFilter = null },
                                        label = { Text("Todas Categorias", fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                            selectedLabelColor = Color(0xFF38BDF8),
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = Color.White.copy(alpha = 0.7f)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = Color(0xFF2E3F5B),
                                            selectedBorderColor = Color(0xFF38BDF8),
                                            enabled = true,
                                            selected = selectedCategoryFilter == null
                                        )
                                    )
                                }
                                items(availableCategories) { category ->
                                    val isSelected = selectedCategoryFilter.equals(category, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategoryFilter = if (isSelected) null else category },
                                        label = { Text(category, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                            selectedLabelColor = Color(0xFF38BDF8),
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = Color.White.copy(alpha = 0.7f)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = Color(0xFF2E3F5B),
                                            selectedBorderColor = Color(0xFF38BDF8),
                                            enabled = true,
                                            selected = isSelected
                                        )
                                    )
                                }
                            }
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
                    if (selectedTab == DictionaryViewMode.LANGUAGE_STUDY) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            item {
                                LanguageStudyHubSection(
                                    book = book,
                                    pageNumber = pageNumber,
                                    pageText = pageText,
                                    apiKey = apiKey,
                                    verbsList = verbsList,
                                    dictionaryTerms = baseTerms,
                                    speechManager = localSpeechManager,
                                    onSaveVerbs = onSaveVerbs,
                                    onToggleFavorite = onToggleFavoriteVerb
                                )
                            }
                        }
                    } else if (isProcessing) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF38BDF8),
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
                                text = "Buscando substantivos, processos, termos jurídicos (coação, recursos...), verbos e gerando versões completas em 🇪🇸 🇫🇷 🇬🇧.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (filteredTerms.isEmpty()) {
                        EmptyDictionaryState(
                            selectedTab = selectedTab,
                            pageNumber = pageNumber,
                            hasApiKey = apiKey.isNotBlank(),
                            onExtract = {
                                scope.launch {
                                    isProcessing = true
                                    errorMessage = null
                                    statusMessage = "Extraindo termos do texto..."

                                    // 1. Extração offline imediata do texto
                                    val localTerms = OfflineReadingEngine.extractLocalTerms(
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        text = pageText
                                    )

                                    if (apiKey.isBlank()) {
                                        isProcessing = false
                                        if (localTerms.isNotEmpty()) {
                                            onSaveTerms(localTerms)
                                            Toast.makeText(
                                                context,
                                                "✨ ${localTerms.size} termos extraídos offline no dicionário!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            errorMessage = "Nenhum termo catalogado offline. Configure sua chave da API OpenAI para extração com IA."
                                            isConfigExpanded = true
                                        }
                                        return@launch
                                    }

                                    statusMessage = "Conectando à OpenAI ($selectedModel)..."
                                    val result = OpenAiService.extractUniversalDictionaryTerms(
                                        apiKey = apiKey,
                                        model = selectedModel,
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        pageText = pageText
                                    )
                                    isProcessing = false
                                    result.onSuccess { extracted ->
                                        val existing = localTerms.map { it.term.trim().lowercase() }.toSet()
                                        val combined = localTerms + extracted.filter { it.term.trim().lowercase() !in existing }
                                        onSaveTerms(combined)
                                        Toast.makeText(
                                            context,
                                            "✨ ${combined.size} termos e notas extraídos com sucesso!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure { err ->
                                        if (localTerms.isNotEmpty()) {
                                            onSaveTerms(localTerms)
                                            Toast.makeText(
                                                context,
                                                "✨ ${localTerms.size} termos salvos offline (OpenAI indisponível).",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            errorMessage = err.message ?: "Erro ao extrair termos."
                                        }
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
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E3A8A).copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTab = DictionaryViewMode.LANGUAGE_STUDY }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "🇺🇸 🇪🇸 🇫🇷 🇩🇪", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Estudo de Verbos & Phrasal Verbs (16 Tempos)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Quadradinhos modulados nas cores das bandeiras",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF93C5FD)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF38BDF8)
                                        ) {
                                            Text(
                                                text = "Abrir ➔",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${filteredTerms.size} termo(s) • Toque em um termo para Tela Cheia",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    if (selectedTab == DictionaryViewMode.CURRENT_PAGE && pageTerms.isNotEmpty()) {
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

                            items(filteredTerms, key = { if (it.id > 0) "id_${it.id}" else "term_${it.term}_${it.pageNumber}_${it.category}" }) { termItem ->
                                TermDictionaryCard(
                                    termItem = termItem,
                                    languageFilter = selectedLang,
                                    apiKey = apiKey,
                                    voice = selectedVoice,
                                    audioPlayer = audioPlayer,
                                    speechManager = localSpeechManager,
                                    onUpdateTerm = onUpdateTerm,
                                    onOpenFullScreen = { fullScreenTerm = termItem },
                                    onDelete = { onDeleteTerm(termItem) },
                                    onCopy = {
                                        val copyText = buildSingleTermCopyText(termItem)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Termo Dicionário", copyText))
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
                    color = Color(0xFF141C28),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3F5B)),
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B4D66))
                        ) {
                            Text("← Voltar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    errorMessage = null
                                    statusMessage = "Extraindo termos e conceitos da página..."

                                    val localTerms = OfflineReadingEngine.extractLocalTerms(
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        text = pageText
                                    )

                                    if (apiKey.isBlank()) {
                                        isProcessing = false
                                        if (localTerms.isNotEmpty()) {
                                            onSaveTerms(localTerms)
                                            Toast.makeText(
                                                context,
                                                "✨ ${localTerms.size} termos extraídos offline!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            errorMessage = "Nenhum termo catalogado offline nesta página. Configure sua chave da OpenAI para análise por IA."
                                            isConfigExpanded = true
                                        }
                                        return@launch
                                    }

                                    statusMessage = "Extraindo e analisando termos com OpenAI..."
                                    val result = OpenAiService.extractUniversalDictionaryTerms(
                                        apiKey = apiKey,
                                        model = selectedModel,
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        pageText = pageText
                                    )
                                    isProcessing = false
                                    result.onSuccess { extracted ->
                                        val existing = localTerms.map { it.term.trim().lowercase() }.toSet()
                                        val combined = localTerms + extracted.filter { it.term.trim().lowercase() !in existing }
                                        onSaveTerms(combined)
                                        Toast.makeText(
                                            context,
                                            "✨ ${combined.size} termos e notas extraídos com sucesso!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure { err ->
                                        if (localTerms.isNotEmpty()) {
                                            onSaveTerms(localTerms)
                                            Toast.makeText(
                                                context,
                                                "✨ ${localTerms.size} termos salvos offline (OpenAI indisponível).",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            errorMessage = err.message ?: "Falha ao extrair termos da página."
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.6f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
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
                                text = if (pageTerms.isNotEmpty()) "Re-extrair Termos (Pág $pageNumber)" else "Extrair Termos (Pág $pageNumber)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // FULL SCREEN TERM READER DIALOG
            if (fullScreenTerm != null) {
                FullScreenTermDialog(
                    termItem = fullScreenTerm!!,
                    apiKey = apiKey,
                    voice = selectedVoice,
                    audioPlayer = audioPlayer,
                    speechManager = localSpeechManager,
                    onUpdateTerm = { updated ->
                        fullScreenTerm = updated
                        onUpdateTerm(updated)
                    },
                    onDismiss = { fullScreenTerm = null }
                )
            }
        }
    }
}

@Composable
private fun LanguageFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF38BDF8) else Color(0xFF2E3F5B)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TermDictionaryCard(
    termItem: PageDictionaryTerm,
    languageFilter: LanguageFilter,
    apiKey: String,
    voice: String,
    audioPlayer: OpenAiAudioPlayer,
    speechManager: PdfSpeechManager?,
    onUpdateTerm: (PageDictionaryTerm) -> Unit,
    onOpenFullScreen: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val categoryIcon = when {
        termItem.category.contains("juríd", ignoreCase = true) || termItem.category.contains("legal", ignoreCase = true) -> "⚖️"
        termItem.category.contains("compost", ignoreCase = true) || termItem.category.contains("express", ignoreCase = true) -> "🧩"
        termItem.category.contains("process", ignoreCase = true) -> "⚙️"
        termItem.category.contains("filosóf", ignoreCase = true) || termItem.category.contains("acadêm", ignoreCase = true) -> "💡"
        termItem.category.contains("substantiv", ignoreCase = true) -> "🏷️"
        termItem.category.contains("verb", ignoreCase = true) -> "⚡"
        termItem.category.contains("técnic", ignoreCase = true) -> "🔬"
        termItem.category.contains("erudit", ignoreCase = true) || termItem.category.contains("raro", ignoreCase = true) -> "🏛️"
        else -> "📖"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141C28)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3F5B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFullScreen() }
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header of Card: Category Tag + Term Name + Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$categoryIcon ${termItem.category.ifBlank { "Geral" }} • Pág. ${termItem.pageNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenFullScreen, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Tela Cheia",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Termo",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Termo",
                                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Term Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = termItem.term,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onOpenFullScreen() }
                    ) {
                        Text(
                            text = "TELA CHEIA ⛶",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Original Context
                if (termItem.originalContext.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📍 Contexto: \"${termItem.originalContext}\"",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Original Definition (Portuguese / Base language) com karaokê sincronizado e clique em palavras
                if (termItem.originalDefinition.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    KaraokeDefinitionText(
                        targetId = "dict_def_${termItem.id}",
                        text = "${termItem.term}: ${termItem.originalDefinition}",
                        speechManager = speechManager,
                        accentColor = Color(0xFF38BDF8),
                        prefixLabel = "Significado & Nuances:",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        highlightBgColor = Color(0xFFFBBF24),
                        highlightTextColor = Color(0xFF000000),
                        showListenButton = true
                    )

                    // Compact Audio Generator / Player for Original
                    CompactAudioSection(
                        sectionTitle = "Áudio da Definição",
                        textToSpeak = "${termItem.term}. ${termItem.originalDefinition}",
                        currentAudioPath = termItem.audioPathOriginal,
                        apiKey = apiKey,
                        voice = voice,
                        termId = termItem.id,
                        sectionKey = "original",
                        audioPlayer = audioPlayer,
                        accentColor = Color(0xFF38BDF8),
                        onAudioGenerated = { path ->
                            val updated = termItem.copy(audioPathOriginal = path)
                            onUpdateTerm(updated)
                        },
                        onAudioDeleted = {
                            val updated = termItem.copy(audioPathOriginal = "")
                            onUpdateTerm(updated)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF2E3F5B).copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                // 3 Language Versions (Spanish, French, English)
                if (languageFilter == LanguageFilter.ALL || languageFilter == LanguageFilter.SPANISH) {
                    LanguageVersionBox(
                        flag = "🇪🇸",
                        languageName = "ESPANHOL",
                        translatedTerm = termItem.spanishTerm,
                        definition = termItem.spanishDefinition,
                        example = termItem.spanishExample,
                        accentColor = Color(0xFFF59E0B), // Amber
                        audioPath = termItem.audioPathSpanish,
                        apiKey = apiKey,
                        voice = voice,
                        termId = termItem.id,
                        sectionKey = "spanish",
                        audioPlayer = audioPlayer,
                        speechManager = speechManager,
                        onAudioGenerated = { path ->
                            val updated = termItem.copy(audioPathSpanish = path)
                            onUpdateTerm(updated)
                        },
                        onAudioDeleted = {
                            val updated = termItem.copy(audioPathSpanish = "")
                            onUpdateTerm(updated)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (languageFilter == LanguageFilter.ALL || languageFilter == LanguageFilter.FRENCH) {
                    LanguageVersionBox(
                        flag = "🇫🇷",
                        languageName = "FRANCÊS",
                        translatedTerm = termItem.frenchTerm,
                        definition = termItem.frenchDefinition,
                        example = termItem.frenchExample,
                        accentColor = Color(0xFF818CF8), // Indigo
                        audioPath = termItem.audioPathFrench,
                        apiKey = apiKey,
                        voice = voice,
                        termId = termItem.id,
                        sectionKey = "french",
                        audioPlayer = audioPlayer,
                        speechManager = speechManager,
                        onAudioGenerated = { path ->
                            val updated = termItem.copy(audioPathFrench = path)
                            onUpdateTerm(updated)
                        },
                        onAudioDeleted = {
                            val updated = termItem.copy(audioPathFrench = "")
                            onUpdateTerm(updated)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (languageFilter == LanguageFilter.ALL || languageFilter == LanguageFilter.ENGLISH) {
                    LanguageVersionBox(
                        flag = "🇬🇧",
                        languageName = "INGLÊS",
                        translatedTerm = termItem.englishTerm,
                        definition = termItem.englishDefinition,
                        example = termItem.englishExample,
                        accentColor = Color(0xFF34D399), // Emerald
                        audioPath = termItem.audioPathEnglish,
                        apiKey = apiKey,
                        voice = voice,
                        termId = termItem.id,
                        sectionKey = "english",
                        audioPlayer = audioPlayer,
                        speechManager = speechManager,
                        onAudioGenerated = { path ->
                            val updated = termItem.copy(audioPathEnglish = path)
                            onUpdateTerm(updated)
                        },
                        onAudioDeleted = {
                            val updated = termItem.copy(audioPathEnglish = "")
                            onUpdateTerm(updated)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageVersionBox(
    flag: String,
    languageName: String,
    translatedTerm: String,
    definition: String,
    example: String,
    accentColor: Color,
    audioPath: String = "",
    apiKey: String = "",
    voice: String = "alloy",
    termId: Int = 0,
    sectionKey: String = "",
    audioPlayer: OpenAiAudioPlayer,
    speechManager: PdfSpeechManager? = null,
    onAudioGenerated: (String) -> Unit = {},
    onAudioDeleted: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = flag, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = languageName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
                if (translatedTerm.isNotBlank()) {
                    Text(
                        text = translatedTerm,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (definition.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                KaraokeDefinitionText(
                    targetId = "dict_lang_${termId}_${sectionKey}",
                    text = if (translatedTerm.isNotBlank()) "$translatedTerm: $definition" else definition,
                    speechManager = speechManager,
                    accentColor = accentColor,
                    prefixLabel = "📖 Definição:",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    highlightBgColor = Color(0xFFFBBF24),
                    highlightTextColor = Color(0xFF000000),
                    showListenButton = true
                )
            }

            if (example.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "💡 Exemplo Ilustrativo & Aplicação:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = example,
                            fontSize = 11.sp,
                            color = Color(0xFFF1F5F9),
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }

            // Compact Audio Player / Generator for this language version
            val textToSpeak = listOf(translatedTerm, definition, example).filter { it.isNotBlank() }.joinToString(". ")
            if (textToSpeak.isNotBlank()) {
                CompactAudioSection(
                    sectionTitle = "Áudio $languageName ($flag)",
                    textToSpeak = textToSpeak,
                    currentAudioPath = audioPath,
                    apiKey = apiKey,
                    voice = voice,
                    termId = termId,
                    sectionKey = sectionKey,
                    audioPlayer = audioPlayer,
                    accentColor = accentColor,
                    onAudioGenerated = onAudioGenerated,
                    onAudioDeleted = onAudioDeleted
                )
            }
        }
    }
}

@Composable
private fun CompactAudioSection(
    sectionTitle: String,
    textToSpeak: String,
    currentAudioPath: String,
    apiKey: String,
    voice: String,
    termId: Int,
    sectionKey: String,
    audioPlayer: OpenAiAudioPlayer,
    accentColor: Color,
    onAudioGenerated: (String) -> Unit,
    onAudioDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGeneratingAudio by remember { mutableStateOf(false) }

    val activePath by audioPlayer.activeFilePath.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentPosMs by audioPlayer.currentPositionMs.collectAsState()
    val durationMs by audioPlayer.durationMs.collectAsState()
    val currentSpeed by audioPlayer.playbackSpeed.collectAsState()

    val hasSavedAudio = currentAudioPath.isNotBlank() && File(currentAudioPath).exists()
    val isThisActive = activePath == currentAudioPath

    Spacer(modifier = Modifier.height(6.dp))

    if (!hasSavedAudio) {
        // Button to generate audio with OpenAI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isGeneratingAudio) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gerando áudio OpenAI...", fontSize = 10.sp, color = accentColor)
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable {
                        if (apiKey.isBlank()) {
                            Toast.makeText(context, "Configure sua chave da API OpenAI primeiro", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        scope.launch {
                            isGeneratingAudio = true
                            val result = OpenAiService.generateDictionarySectionAudio(
                                context = context,
                                apiKey = apiKey,
                                textToSpeak = textToSpeak,
                                voice = voice,
                                termId = termId,
                                sectionKey = sectionKey
                            )
                            isGeneratingAudio = false
                            result.onSuccess { file ->
                                onAudioGenerated(file.absolutePath)
                                Toast.makeText(context, "🎙️ Áudio gerado e salvo no celular!", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Erro ao gerar áudio: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "🎙️ Gerar Áudio (IA)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    } else {
        // Inline Compact Audio Player
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.8f),
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    if (!isThisActive) {
                                        audioPlayer.loadAndPlay(currentAudioPath, autoPlay = true)
                                    } else {
                                        audioPlayer.togglePlayPause()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isThisActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isThisActive && isPlaying) "Pausar" else "Ouvir",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isThisActive && isPlaying) "Ouvindo..." else "Áudio Salvo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Speed Chips (0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { sp ->
                            val isSel = (currentSpeed == sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) accentColor else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.clickable { audioPlayer.setPlaybackSpeed(sp) }
                            ) {
                                Text(
                                    text = "${sp}x",
                                    fontSize = 9.sp,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isSel) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                try {
                                    File(currentAudioPath).delete()
                                } catch (_: Exception) {}
                                if (isThisActive) {
                                    audioPlayer.stop()
                                }
                                onAudioDeleted()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Áudio",
                                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (isThisActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val safeDuration = durationMs.coerceAtLeast(1)
                    val progressFraction = (currentPosMs.toFloat() / safeDuration).coerceIn(0f, 1f)
                    Slider(
                        value = progressFraction,
                        onValueChange = { audioPlayer.seekTo((it * safeDuration).toInt()) },
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTimeMs(currentPosMs), fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(formatTimeMs(durationMs), fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Term Reader Dialog for immersive reading
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenTermDialog(
    termItem: PageDictionaryTerm,
    apiKey: String,
    voice: String,
    audioPlayer: OpenAiAudioPlayer,
    speechManager: PdfSpeechManager?,
    onUpdateTerm: (PageDictionaryTerm) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090D16)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = termItem.term,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${termItem.bookTitle} • Página ${termItem.pageNumber}",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar Tela Cheia",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val copyText = buildSingleTermCopyText(termItem)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Termo Dicionário", copyText))
                            Toast.makeText(context, "Termo copiado!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            val exportText = buildSingleTermCopyText(termItem)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Termo: ${termItem.term}")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Termo"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141C28))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // Category & Page Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = "🏷️ ${termItem.category.ifBlank { "Geral" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "📖 Pág. ${termItem.pageNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Display Term Name
                    Text(
                        text = termItem.term,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 34.sp
                    )

                    // Original Context
                    if (termItem.originalContext.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF131D2D),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3F5B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📍 Contexto na Obra:\n\"${termItem.originalContext}\"",
                                fontSize = 13.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color(0xFFBAE6FD),
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detailed Original Definition com karaokê e clique nas palavras
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141C28)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3F5B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            KaraokeDefinitionText(
                                targetId = "dict_full_${termItem.id}",
                                text = "${termItem.term}: ${termItem.originalDefinition}",
                                speechManager = speechManager,
                                accentColor = Color(0xFF38BDF8),
                                prefixLabel = "📖 DEFINIÇÃO & NUANCES (ORIGINAL)",
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                highlightBgColor = Color(0xFFFBBF24),
                                highlightTextColor = Color(0xFF000000),
                                showListenButton = true
                            )

                            CompactAudioSection(
                                sectionTitle = "Áudio da Definição",
                                textToSpeak = "${termItem.term}. ${termItem.originalDefinition}",
                                currentAudioPath = termItem.audioPathOriginal,
                                apiKey = apiKey,
                                voice = voice,
                                termId = termItem.id,
                                sectionKey = "original",
                                audioPlayer = audioPlayer,
                                accentColor = Color(0xFF38BDF8),
                                onAudioGenerated = { path ->
                                    val updated = termItem.copy(audioPathOriginal = path)
                                    onUpdateTerm(updated)
                                },
                                onAudioDeleted = {
                                    val updated = termItem.copy(audioPathOriginal = "")
                                    onUpdateTerm(updated)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🇪🇸 ESPANHOL
                    LanguageVersionBox(
                        flag = "🇪🇸",
                        languageName = "ESPANHOL (Español)",
                        translatedTerm = termItem.spanishTerm,
                        definition = termItem.spanishDefinition,
                        example = termItem.spanishExample,
                        accentColor = Color(0xFFF59E0B),
                        audioPath = termItem.audioPathSpanish,
                        apiKey = apiKey,
                        voice = voice,
                        termId = termItem.id,
                        sectionKey = "spanish",
                        audioPlayer = audioPlayer,
                        speechManager = speechManager,
                        onAudioGenerated = { path ->
                            val updated = termItem.copy(audioPathSpanish = path)
                            onUpdateTerm(updated)
                        },
                        onAudioDeleted = {
                            val updated = termItem.copy(audioPathSpanish = "")
                            onUpdateTerm(updated)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🇫🇷 FRANCÊS
                    LanguageVersionBox(
                        flag = "🇫🇷",
                        languageName = "FRANCÊS (Français)",
                        translatedTerm = termItem.frenchTerm,
                        definition = termItem.frenchDefinition,
                        example = termItem.frenchExample,
                        accentColor = Color(0xFF818CF8),
                        audioPath = termItem.audioPathFrench,
                        apiKey = apiKey,
                        voice = voice,
                        termId = termItem.id,
                        sectionKey = "french",
                        audioPlayer = audioPlayer,
                        speechManager = speechManager,
                        onAudioGenerated = { path ->
                            val updated = termItem.copy(audioPathFrench = path)
                            onUpdateTerm(updated)
                        },
                        onAudioDeleted = {
                            val updated = termItem.copy(audioPathFrench = "")
                            onUpdateTerm(updated)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🇬🇧 INGLÊS
                    LanguageVersionBox(
                        flag = "🇬🇧",
                        languageName = "INGLÊS (English)",
                        translatedTerm = termItem.englishTerm,
                        definition = termItem.englishDefinition,
                        example = termItem.englishExample,
                        accentColor = Color(0xFF34D399),
                        audioPath = termItem.audioPathEnglish,
                        apiKey = apiKey,
                        voice = voice,
                        termId = termItem.id,
                        sectionKey = "english",
                        audioPlayer = audioPlayer,
                        speechManager = speechManager,
                        onAudioGenerated = { path ->
                            val updated = termItem.copy(audioPathEnglish = path)
                            onUpdateTerm(updated)
                        },
                        onAudioDeleted = {
                            val updated = termItem.copy(audioPathEnglish = "")
                            onUpdateTerm(updated)
                        }
                    )

                    Spacer(modifier = Modifier.height(30.dp))
                }

                // Close Button bottom bar
                Surface(
                    color = Color(0xFF141C28),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fechar Leitura em Tela Cheia", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDictionaryState(
    selectedTab: DictionaryViewMode,
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
            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (selectedTab) {
                DictionaryViewMode.CURRENT_PAGE -> "Nenhum termo extraído para a Página $pageNumber"
                DictionaryViewMode.CURRENT_BOOK -> "Nenhum termo extraído neste livro ainda"
                DictionaryViewMode.ALL_BOOKS -> "Dicionário Universal Vazio"
                DictionaryViewMode.LANGUAGE_STUDY -> "Seção de Estudo por Idioma"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "O Dicionário Universal analisa o texto da página com a OpenAI e extrai automaticamente termos jurídicos (coação, recursos...), substantivos, processos, verbos e conceitos difíceis, gerando versões completas em Espanhol 🇪🇸, Francês 🇫🇷 e Inglês 🇬🇧 com exemplos e áudio neural.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onExtract,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0284C7),
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
                text = "✨ Extrair Termos Desta Página com OpenAI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTimeMs(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun buildExportText(bookTitle: String, pageNumber: Int, terms: List<PageDictionaryTerm>): String {
    val sb = StringBuilder()
    sb.append("📚 DICIONÁRIO UNIVERSAL DE TERMOS\n")
    sb.append("Livro: $bookTitle | Página: $pageNumber\n")
    sb.append("Total de Termos: ${terms.size}\n\n")
    sb.append("========================================\n\n")

    terms.forEachIndexed { index, term ->
        sb.append("${index + 1}. ${term.term.uppercase()} [${term.category}]\n")
        if (term.originalContext.isNotBlank()) {
            sb.append("• Contexto: \"${term.originalContext}\"\n")
        }
        sb.append("• Significado: ${term.originalDefinition}\n\n")
        sb.append("  🇪🇸 ESPANHOL: ${term.spanishTerm}\n")
        sb.append("     Definição: ${term.spanishDefinition}\n")
        sb.append("     Exemplo: ${term.spanishExample}\n\n")
        sb.append("  🇫🇷 FRANCÊS: ${term.frenchTerm}\n")
        sb.append("     Definição: ${term.frenchDefinition}\n")
        sb.append("     Exemplo: ${term.frenchExample}\n\n")
        sb.append("  🇬🇧 INGLÊS: ${term.englishTerm}\n")
        sb.append("     Definição: ${term.englishDefinition}\n")
        sb.append("     Exemplo: ${term.englishExample}\n\n")
        sb.append("  ----------------------------------------\n\n")
    }

    return sb.toString()
}

private fun buildSingleTermCopyText(term: PageDictionaryTerm): String {
    val sb = StringBuilder()
    sb.append("📌 ${term.term.uppercase()} [${term.category}] (Pág. ${term.pageNumber})\n")
    if (term.originalContext.isNotBlank()) {
        sb.append("📍 Contexto: \"${term.originalContext}\"\n")
    }
    sb.append("📖 Significado: ${term.originalDefinition}\n\n")
    sb.append("🇪🇸 Espanhol: ${term.spanishTerm}\n")
    sb.append("• ${term.spanishDefinition}\n")
    sb.append("• Exemplo: ${term.spanishExample}\n\n")
    sb.append("🇫🇷 Francês: ${term.frenchTerm}\n")
    sb.append("• ${term.frenchDefinition}\n")
    sb.append("• Exemplo: ${term.frenchExample}\n\n")
    sb.append("🇬🇧 Inglês: ${term.englishTerm}\n")
    sb.append("• ${term.englishDefinition}\n")
    sb.append("• Exemplo: ${term.englishExample}\n")
    return sb.toString()
}
