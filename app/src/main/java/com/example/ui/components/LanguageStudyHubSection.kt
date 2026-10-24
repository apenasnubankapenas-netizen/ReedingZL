package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PdfBook
import com.example.data.model.StudyLanguage
import com.example.data.model.VerbPhrasalVerbStudyTerm
import com.example.data.model.VerbTenseQuestionExample
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.data.translator.TermsNotesTranslator
import com.example.ui.theme.LanguageFlagTheme
import kotlinx.coroutines.launch

enum class VerbFilterType {
    ALL,
    ONLY_PHRASAL,
    ONLY_VERBS,
    FAVORITES
}

enum class StudySectionTab {
    VERBS_AND_PHRASAL,
    MULTILINGUAL_TERMS
}

/**
 * Seção de Estudo Exclusivo por Idioma no Dicionário.
 * Apresenta temas nas cores das bandeiras de cada país (EUA, Espanha, França, etc.),
 * grid modulado de quadradinhos justificando verbos e phrasal verbs com preposições,
 * e os 16 tempos verbais com Question Words.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageStudyHubSection(
    book: PdfBook,
    pageNumber: Int,
    pageText: String,
    apiKey: String,
    verbsList: List<VerbPhrasalVerbStudyTerm>,
    dictionaryTerms: List<PageDictionaryTerm>,
    speechManager: PdfSpeechManager? = null,
    onSaveVerbs: (List<VerbPhrasalVerbStudyTerm>) -> Unit,
    onToggleFavorite: (VerbPhrasalVerbStudyTerm) -> Unit,
    onDeleteVerb: (VerbPhrasalVerbStudyTerm) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localSpeechManager = remember { speechManager ?: PdfSpeechManager(context) }

    // Idioma atualmente selecionado para estudo
    var selectedLanguage by remember { mutableStateOf(StudyLanguage.ENGLISH) }
    val currentFlagTheme = remember(selectedLanguage) { LanguageFlagTheme.getThemeFor(selectedLanguage) }

    var currentTab by remember { mutableStateOf(StudySectionTab.VERBS_AND_PHRASAL) }
    var filterType by remember { mutableStateOf(VerbFilterType.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    // Estado do Diálogo de Detalhes (quando clica em um quadradinho)
    var selectedVerbForDetail by remember { mutableStateOf<VerbPhrasalVerbStudyTerm?>(null) }

    // Diálogo de Injeção de Lista Separada por Vírgula
    var showInjectionDialog by remember { mutableStateOf(false) }
    var commaSeparatedInput by remember { mutableStateOf("") }
    var isProcessingInjection by remember { mutableStateOf(false) }

    // Processamento de extração da página
    var isScanningPage by remember { mutableStateOf(false) }

    // Lista combinada de termos (adiciona termos padrão de alta qualidade para estudo imediato caso vazio)
    val displayVerbs = remember(verbsList, selectedLanguage) {
        val filteredByLang = verbsList.filter { it.languageCode == selectedLanguage.code }
        if (filteredByLang.isEmpty() && selectedLanguage == StudyLanguage.ENGLISH) {
            getPreloadedEnglishVerbsAndPhrasalVerbs(book.id, pageNumber, book.title)
        } else {
            filteredByLang
        }
    }

    // Filtros de busca e tipo (ordenados RIGOROSAMENTE por ordem alfabética)
    val filteredVerbs = remember(displayVerbs, filterType, searchQuery) {
        displayVerbs.filter { v ->
            val matchesSearch = searchQuery.isBlank() ||
                    v.verbOrPhrase.contains(searchQuery, ignoreCase = true) ||
                    v.portugueseMeaning.contains(searchQuery, ignoreCase = true) ||
                    v.prepositionUsed.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterType) {
                VerbFilterType.ALL -> true
                VerbFilterType.ONLY_PHRASAL -> v.isPhrasalVerb
                VerbFilterType.ONLY_VERBS -> !v.isPhrasalVerb
                VerbFilterType.FAVORITES -> v.isFavorite
            }

            matchesSearch && matchesFilter
        }.sortedBy { it.verbOrPhrase.lowercase(java.util.Locale.getDefault()).trim() }
    }

    // Container tematizado estritamente nas cores da bandeira do país escolhido (Minimalista e compacto)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = currentFlagTheme.containerBackground),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, currentFlagTheme.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Faixa decorativa com o gradiente da bandeira do país
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(currentFlagTheme.headerGradient)
            )

            // Cabeçalho da Seção de Estudos (Minimalista, priorizando espaço de leitura)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentFlagTheme.surfaceCard)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentFlagTheme.flagEmoji,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ESTUDO: ${selectedLanguage.displayName.uppercase()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = currentFlagTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${currentFlagTheme.countryName}",
                            fontSize = 9.5.sp,
                            color = currentFlagTheme.secondaryAccent
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = currentFlagTheme.badgeBg,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, currentFlagTheme.secondaryAccent)
                    ) {
                        Text(
                            text = "${filteredVerbs.size} itens (A-Z)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentFlagTheme.secondaryAccent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SELETOR HORIZONTAL DE IDIOMAS (Compacto)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val supportedLanguages = listOf(
                        StudyLanguage.ENGLISH,
                        StudyLanguage.SPANISH,
                        StudyLanguage.FRENCH,
                        StudyLanguage.GERMAN,
                        StudyLanguage.RUSSIAN,
                        StudyLanguage.JAPANESE,
                        StudyLanguage.ITALIAN,
                        StudyLanguage.CHINESE,
                        StudyLanguage.UKRAINIAN,
                        StudyLanguage.GREEK,
                        StudyLanguage.ARABIC,
                        StudyLanguage.HEBREW
                    )

                    items(supportedLanguages) { lang ->
                        val isSelected = lang == selectedLanguage
                        val langTheme = LanguageFlagTheme.getThemeFor(lang)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) langTheme.primaryAccent else currentFlagTheme.containerBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.dp else 0.5.dp,
                                if (isSelected) langTheme.secondaryAccent else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.clickable { selectedLanguage = lang }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(text = lang.flag, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = lang.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    color = if (isSelected) Color.White else currentFlagTheme.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Abas internas da seção (Compactas)
                TabRow(
                    selectedTabIndex = if (currentTab == StudySectionTab.VERBS_AND_PHRASAL) 0 else 1,
                    containerColor = currentFlagTheme.containerBackground,
                    contentColor = currentFlagTheme.secondaryAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.8.dp, currentFlagTheme.cardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = currentTab == StudySectionTab.VERBS_AND_PHRASAL,
                        onClick = { currentTab = StudySectionTab.VERBS_AND_PHRASAL },
                        text = {
                            Text(
                                text = "🧩 Verbos & Phrasal (A-Z)",
                                fontSize = 10.5.sp,
                                fontWeight = if (currentTab == StudySectionTab.VERBS_AND_PHRASAL) FontWeight.Black else FontWeight.Normal,
                                color = if (currentTab == StudySectionTab.VERBS_AND_PHRASAL) currentFlagTheme.secondaryAccent else currentFlagTheme.textSecondary
                            )
                        }
                    )
                    Tab(
                        selected = currentTab == StudySectionTab.MULTILINGUAL_TERMS,
                        onClick = { currentTab = StudySectionTab.MULTILINGUAL_TERMS },
                        text = {
                            Text(
                                text = "📖 Termos Multilíngues",
                                fontSize = 10.5.sp,
                                fontWeight = if (currentTab == StudySectionTab.MULTILINGUAL_TERMS) FontWeight.Black else FontWeight.Normal,
                                color = if (currentTab == StudySectionTab.MULTILINGUAL_TERMS) currentFlagTheme.secondaryAccent else currentFlagTheme.textSecondary
                            )
                        }
                    )
                }
            }

            // CONTEÚDO DA ABA SELECIONADA
            Column(modifier = Modifier.padding(8.dp)) {
                if (currentTab == StudySectionTab.VERBS_AND_PHRASAL) {
                    // Barra de Ações: Extrair da Página ou Injetar por Vírgula (Minimalista)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                isScanningPage = true
                                scope.launch {
                                    val healedPageText = com.example.util.TextFormatUtils.healBrokenWordsAndHyphens(pageText)
                                    val effectiveText = if (healedPageText.isNotBlank()) healedPageText else book.title
                                    val result = OpenAiService.extractVerbsAndPhrasalVerbs(
                                        apiKey = apiKey,
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        sourceText = effectiveText,
                                        isCommaSeparatedList = false,
                                        languageCode = selectedLanguage.code
                                    )
                                    isScanningPage = false
                                    result.onSuccess { extracted ->
                                        onSaveVerbs(extracted)
                                        Toast.makeText(context, "Extraídos ${extracted.size} verbos com 16 exemplos!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        val offlineExtracted = com.example.data.offline.OfflineVerbPhrasalVerbEngine.extractVerbsAndPhrasalVerbs(
                                            bookId = book.id,
                                            pageNumber = pageNumber,
                                            bookTitle = book.title,
                                            sourceText = effectiveText,
                                            languageCode = selectedLanguage.code
                                        )
                                        if (offlineExtracted.isNotEmpty()) {
                                            onSaveVerbs(offlineExtracted)
                                            Toast.makeText(context, "Extraídos ${offlineExtracted.size} verbos e phrasal verbs da página!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Aviso: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isScanningPage && !isProcessingInjection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentFlagTheme.primaryAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            if (isScanningPage) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(13.dp), strokeWidth = 1.8.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Extraindo...", fontSize = 10.5.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Extrair da Página", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { showInjectionDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentFlagTheme.secondaryAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentFlagTheme.secondaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Injetar por Vírgula", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Campo de Pesquisa Compacto
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Pesquisar verbo, phrasal verb...", fontSize = 10.5.sp, color = currentFlagTheme.textSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = currentFlagTheme.secondaryAccent, modifier = Modifier.size(14.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = currentFlagTheme.textSecondary, modifier = Modifier.size(12.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentFlagTheme.secondaryAccent,
                            unfocusedBorderColor = currentFlagTheme.cardBorder.copy(alpha = 0.4f),
                            focusedTextColor = currentFlagTheme.textPrimary,
                            unfocusedTextColor = currentFlagTheme.textPrimary,
                            focusedContainerColor = currentFlagTheme.surfaceCard,
                            unfocusedContainerColor = currentFlagTheme.surfaceCard
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Chips de Filtro Compactos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = filterType == VerbFilterType.ALL,
                            onClick = { filterType = VerbFilterType.ALL },
                            label = { Text("Todos (${displayVerbs.size})", fontSize = 9.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentFlagTheme.primaryAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = filterType == VerbFilterType.ONLY_PHRASAL,
                            onClick = { filterType = VerbFilterType.ONLY_PHRASAL },
                            label = { Text("🧩 Phrasal (${displayVerbs.count { it.isPhrasalVerb }})", fontSize = 9.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentFlagTheme.primaryAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = filterType == VerbFilterType.ONLY_VERBS,
                            onClick = { filterType = VerbFilterType.ONLY_VERBS },
                            label = { Text("⚡ Verbos (${displayVerbs.count { !it.isPhrasalVerb }})", fontSize = 9.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentFlagTheme.primaryAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = filterType == VerbFilterType.FAVORITES,
                            onClick = { filterType = VerbFilterType.FAVORITES },
                            label = { Text("⭐ Favoritos (${displayVerbs.count { it.isFavorite }})", fontSize = 9.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentFlagTheme.primaryAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // OS QUADRADINHOS MODULADOS: GRID MODULADO JUSTIFICADO
                    // "um phrasal verb aqui, outro verbo aqui, outro phrasal verb aqui, outro verbo aqui...
                    // Um ao lado do outro, bem montado, justificado, certinho, modulado com quadradinhos assim."
                    if (filteredVerbs.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = currentFlagTheme.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentFlagTheme.cardBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Nenhum verbo ou phrasal verb encontrado.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentFlagTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Clique em 'Extrair da Página' ou 'Injetar por Vírgula' para adicionar termos e gerar os 16 exemplos.",
                                    fontSize = 11.sp,
                                    color = currentFlagTheme.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // 1. Barra de Verbos e Phrasal Verbs lado a lado por Ordem Alfabética (A-Z) em Colchetes: [eat], [get across], [get by]...
                        Text(
                            text = "ORDEM ALFABÉTICA (A-Z) • Clique para ver os 16 Tempos Verbais:",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentFlagTheme.secondaryAccent,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            filteredVerbs.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (item.isPhrasalVerb) currentFlagTheme.pillPrepositionBg else currentFlagTheme.quadradinhoBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (item.isPhrasalVerb) currentFlagTheme.pillPrepositionText else currentFlagTheme.quadradinhoBorder
                                    ),
                                    modifier = Modifier
                                        .clickable { selectedVerbForDetail = item }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "[${item.verbOrPhrase}]",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = currentFlagTheme.textPrimary
                                        )
                                        Spacer(modifier = Modifier.width(3.5.dp))
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = if (item.isPhrasalVerb) currentFlagTheme.primaryAccent else currentFlagTheme.secondaryAccent.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (item.isPhrasalVerb) "PV" else "V",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (item.isPhrasalVerb) Color.White else currentFlagTheme.secondaryAccent,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Renderização justificada e modulada em pares (2 quadradinhos por linha)
                        val chunkedList = filteredVerbs.chunked(2)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunkedList.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Primeiro quadradinho
                                    VerbQuadradinhoButton(
                                        verb = pair[0],
                                        flagTheme = currentFlagTheme,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedVerbForDetail = pair[0] },
                                        onToggleFavorite = { onToggleFavorite(pair[0]) }
                                    )

                                    // Segundo quadradinho (se houver)
                                    if (pair.size > 1) {
                                        VerbQuadradinhoButton(
                                            verb = pair[1],
                                            flagTheme = currentFlagTheme,
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectedVerbForDetail = pair[1] },
                                            onToggleFavorite = { onToggleFavorite(pair[1]) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ABA DE TERMOS MULTILÍNGUES:
                    // Se escolher um termo, mostra tanto em inglês, espanhol, francês, alemão, russo, japonês etc., cada um com tradução em português
                    MultilingualTermsComparativeView(
                        book = book,
                        terms = dictionaryTerms,
                        currentFlagTheme = currentFlagTheme,
                        selectedLanguage = selectedLanguage,
                        speechManager = localSpeechManager
                    )
                }
            }
        }
    }

    // Diálogo completo dos 16 Exemplos com Question Words ao clicar no quadradinho
    if (selectedVerbForDetail != null) {
        VerbPhrasalVerbDetailDialog(
            term = selectedVerbForDetail!!,
            flagTheme = currentFlagTheme,
            apiKey = apiKey,
            speechManager = localSpeechManager,
            onToggleFavorite = { verb ->
                onToggleFavorite(verb)
                selectedVerbForDetail = selectedVerbForDetail?.copy(isFavorite = !selectedVerbForDetail!!.isFavorite)
            },
            onUpdateVerb = { updated ->
                val updatedList = verbsList.map { if (it.id == updated.id) updated else it }
                onSaveVerbs(updatedList)
                selectedVerbForDetail = updated
            },
            onDismiss = { selectedVerbForDetail = null }
        )
    }

    // Diálogo de Injeção de Termos Separados por Vírgula via OpenAI
    if (showInjectionDialog) {
        Dialog(onDismissRequest = { if (!isProcessingInjection) showInjectionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = currentFlagTheme.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, currentFlagTheme.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📥 INJETAR TERMOS POR VÍRGULA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = currentFlagTheme.textPrimary
                        )
                        IconButton(onClick = { if (!isProcessingInjection) showInjectionDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = currentFlagTheme.textPrimary)
                        }
                    }

                    Text(
                        text = "Cole ou digite verbos e phrasal verbs separados por vírgula. A IA da OpenAI identificará as preposições, traduções em português e gerará os 16 tempos verbais com Question Words.",
                        fontSize = 11.sp,
                        color = currentFlagTheme.textSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    OutlinedTextField(
                        value = commaSeparatedInput,
                        onValueChange = { commaSeparatedInput = it },
                        placeholder = {
                            Text(
                                text = "Ex: look for, give up, break down, run out of, carry on, achieve, investigate, figure out",
                                fontSize = 11.sp,
                                color = currentFlagTheme.textSecondary.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentFlagTheme.secondaryAccent,
                            unfocusedBorderColor = currentFlagTheme.cardBorder,
                            focusedTextColor = currentFlagTheme.textPrimary,
                            unfocusedTextColor = currentFlagTheme.textPrimary,
                            focusedContainerColor = currentFlagTheme.containerBackground,
                            unfocusedContainerColor = currentFlagTheme.containerBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showInjectionDialog = false },
                            enabled = !isProcessingInjection
                        ) {
                            Text("Cancelar", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (apiKey.isBlank()) {
                                    Toast.makeText(context, "Insira sua OpenAI API Key primeiro!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (commaSeparatedInput.isBlank()) {
                                    Toast.makeText(context, "Digite ao menos um termo separado por vírgula!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isProcessingInjection = true
                                scope.launch {
                                    val result = OpenAiService.extractVerbsAndPhrasalVerbs(
                                        apiKey = apiKey,
                                        bookId = book.id,
                                        pageNumber = pageNumber,
                                        bookTitle = book.title,
                                        sourceText = commaSeparatedInput,
                                        isCommaSeparatedList = true,
                                        languageCode = selectedLanguage.code
                                    )
                                    isProcessingInjection = false
                                    result.onSuccess { injected ->
                                        onSaveVerbs(injected)
                                        showInjectionDialog = false
                                        commaSeparatedInput = ""
                                        Toast.makeText(context, "Injetados ${injected.size} verbos com sucesso!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Erro: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isProcessingInjection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentFlagTheme.primaryAccent,
                                contentColor = Color.White
                            )
                        ) {
                            if (isProcessingInjection) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Processando OpenAI...", fontSize = 11.sp)
                            } else {
                                Text("Processar & Injetar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente do QUADRADINHO individual para cada verbo / phrasal verb.
 * Modulado, justificado, estilizado como botão interativo com cores da bandeira.
 */
@Composable
private fun VerbQuadradinhoButton(
    verb: VerbPhrasalVerbStudyTerm,
    flagTheme: LanguageFlagTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = flagTheme.quadradinhoBg,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, flagTheme.quadradinhoBorder),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            // Linha Superior: Badge de Categoria e Estrela de Favorito
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (verb.isPhrasalVerb) flagTheme.pillPrepositionBg else flagTheme.primaryAccent.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (verb.isPhrasalVerb) flagTheme.pillPrepositionText else flagTheme.primaryAccent
                    )
                ) {
                    Text(
                        text = if (verb.isPhrasalVerb) "PHRASAL" else "VERBO",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (verb.isPhrasalVerb) flagTheme.pillPrepositionText else flagTheme.textPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (verb.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (verb.isFavorite) Color(0xFFEF4444) else flagTheme.textSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Nome do Verbo / Phrasal Verb em destaque grande
            Text(
                text = verb.verbOrPhrase,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = flagTheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Preposição Usada em destaque (se houver)
            if (verb.prepositionUsed.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = flagTheme.secondaryAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Prep: ${verb.prepositionUsed.uppercase()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = flagTheme.secondaryAccent,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = if (verb.baseVerb.isNotBlank()) "Base: ${verb.baseVerb}" else "Verbo simples",
                    fontSize = 9.sp,
                    color = flagTheme.textSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Significado resumido em Português
            Text(
                text = verb.portugueseMeaning.ifBlank { "Verbo com preposição contextual." },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = flagTheme.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rodapé do Quadradinho: Botão indicador dos 16 Exemplos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "16 Exemplos",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = flagTheme.secondaryAccent
                )
                Text(
                    text = "Ver ➔",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = flagTheme.secondaryAccent
                )
            }
        }
    }
}

private data class TermLanguageDisplay(
    val language: StudyLanguage,
    val termInLang: String,
    val defInLang: String,
    val ptTranslation: String
)

/**
 * Visualização comparativa de termos em todos os idiomas estudados com tradução para português.
 * Se escolher um termo do texto, exibe em Inglês, Espanhol, Francês, Alemão, Russo, Japonês, etc.
 * Cada versão possui leitura pela voz sincronizada palavra por palavra (Karaokê) e botão pausar/despausar.
 */
@Composable
private fun MultilingualTermsComparativeView(
    book: PdfBook,
    terms: List<PageDictionaryTerm>,
    currentFlagTheme: LanguageFlagTheme,
    selectedLanguage: StudyLanguage,
    speechManager: PdfSpeechManager
) {
    val isSpeaking by speechManager.isSpeaking.collectAsState()
    val activeLineId by speechManager.activeComparativeLineId.collectAsState()
    val activeWordIndex by speechManager.activeWordIndex.collectAsState()
    val isPaused by speechManager.isStudyPaused.collectAsState()

    if (terms.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = currentFlagTheme.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, currentFlagTheme.cardBorder.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nenhum termo multilíngue extraído para esta página.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentFlagTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Os termos colhidos do texto aparecem aqui com versões para cada um dos idiomas estudados (Inglês, Espanhol, Francês, Alemão, Russo, Japonês...) e tradução para português com leitura por voz sincronizada.",
                    fontSize = 11.sp,
                    color = currentFlagTheme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            terms.forEach { term ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = currentFlagTheme.surfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentFlagTheme.cardBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TERMO: ${term.term.uppercase()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = currentFlagTheme.secondaryAccent
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = currentFlagTheme.primaryAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = term.category,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentFlagTheme.textPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (term.originalDefinition.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val defLineId = "term_pt_${term.id}"
                            StudyKaraokeSentenceView(
                                text = term.originalDefinition,
                                lineId = defLineId,
                                isSpeaking = isSpeaking,
                                activeLineId = activeLineId,
                                activeWordIndex = activeWordIndex,
                                isPaused = isPaused,
                                flagTheme = currentFlagTheme,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp,
                                showPrefix = "🇧🇷 Definição Base em Português:",
                                containerColor = currentFlagTheme.containerBackground.copy(alpha = 0.5f),
                                onTogglePlayPause = {
                                    speechManager.toggleStudyPortugueseLine(term.originalDefinition, defLineId)
                                },
                                onWordClick = { offset ->
                                    speechManager.speakStudyPortugueseLine(term.originalDefinition, defLineId, offset)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = currentFlagTheme.cardBorder.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "VERSÕES EM TODOS OS IDIOMAS COM TRADUÇÃO PARA PORTUGUÊS:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = currentFlagTheme.secondaryAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Versões nos principais idiomas estudados (Inglês, Espanhol, Francês, Alemão, Russo, Japonês, etc.)
                        val langVersions = listOf(
                            TermLanguageDisplay(
                                language = StudyLanguage.ENGLISH,
                                termInLang = term.englishTerm.ifBlank { term.term },
                                defInLang = term.englishDefinition,
                                ptTranslation = term.originalDefinition.ifBlank { "Tradução fiel em português." }
                            ),
                            TermLanguageDisplay(
                                language = StudyLanguage.SPANISH,
                                termInLang = term.spanishTerm.ifBlank { term.term },
                                defInLang = term.spanishDefinition,
                                ptTranslation = term.originalDefinition.ifBlank { "Significado correspondente em português." }
                            ),
                            TermLanguageDisplay(
                                language = StudyLanguage.FRENCH,
                                termInLang = term.frenchTerm.ifBlank { term.term },
                                defInLang = term.frenchDefinition,
                                ptTranslation = term.originalDefinition.ifBlank { "Equivalente em língua portuguesa." }
                            ),
                            TermLanguageDisplay(
                                language = StudyLanguage.GERMAN,
                                termInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.GERMAN).first,
                                defInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.GERMAN).second,
                                ptTranslation = "Tradução (Alemão ➔ Português): ${term.originalDefinition}"
                            ),
                            TermLanguageDisplay(
                                language = StudyLanguage.RUSSIAN,
                                termInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.RUSSIAN).first,
                                defInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.RUSSIAN).second,
                                ptTranslation = "Tradução (Russo ➔ Português): ${term.originalDefinition}"
                            ),
                            TermLanguageDisplay(
                                language = StudyLanguage.JAPANESE,
                                termInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.JAPANESE).first,
                                defInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.JAPANESE).second,
                                ptTranslation = "Tradução (Japonês ➔ Português): ${term.originalDefinition}"
                            ),
                            TermLanguageDisplay(
                                language = StudyLanguage.ITALIAN,
                                termInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.ITALIAN).first,
                                defInLang = TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, StudyLanguage.ITALIAN).second,
                                ptTranslation = "Tradução (Italiano ➔ Português): ${term.originalDefinition}"
                            )
                        )

                        langVersions.forEachIndexed { langIdx, langDisplay ->
                            LanguageVersionRow(
                                langDisplay = langDisplay,
                                termId = term.id,
                                langIdx = langIdx,
                                flagTheme = currentFlagTheme,
                                isSpeaking = isSpeaking,
                                activeLineId = activeLineId,
                                activeWordIndex = activeWordIndex,
                                isPaused = isPaused,
                                speechManager = speechManager
                            )
                            if (langIdx < langVersions.size - 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageVersionRow(
    langDisplay: TermLanguageDisplay,
    termId: Int,
    langIdx: Int,
    flagTheme: LanguageFlagTheme,
    isSpeaking: Boolean,
    activeLineId: String?,
    activeWordIndex: Int,
    isPaused: Boolean,
    speechManager: PdfSpeechManager
) {
    val langTheme = LanguageFlagTheme.getThemeFor(langDisplay.language)
    val targetSentence = if (langDisplay.defInLang.isNotBlank()) {
        "${langDisplay.termInLang}. ${langDisplay.defInLang}"
    } else {
        langDisplay.termInLang
    }
    val targetLineId = "lang_${termId}_${langIdx}_target"
    val ptLineId = "lang_${termId}_${langIdx}_pt"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = flagTheme.containerBackground.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, langTheme.secondaryAccent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            // Linha do idioma de estudo
            StudyKaraokeSentenceView(
                text = targetSentence,
                lineId = targetLineId,
                isSpeaking = isSpeaking,
                activeLineId = activeLineId,
                activeWordIndex = activeWordIndex,
                isPaused = isPaused,
                flagTheme = langTheme,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp,
                showPrefix = "${langDisplay.language.flag} ${langDisplay.language.displayName.uppercase()}:",
                onTogglePlayPause = {
                    speechManager.toggleStudyLine(targetSentence, langDisplay.language, targetLineId)
                },
                onWordClick = { offset ->
                    speechManager.speakStudyLine(targetSentence, langDisplay.language, targetLineId, offset)
                }
            )

            // Tradução em Português
            if (langDisplay.ptTranslation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                StudyKaraokeSentenceView(
                    text = langDisplay.ptTranslation,
                    lineId = ptLineId,
                    isSpeaking = isSpeaking,
                    activeLineId = activeLineId,
                    activeWordIndex = activeWordIndex,
                    isPaused = isPaused,
                    flagTheme = flagTheme,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 15.sp,
                    showPrefix = "🇧🇷 Tradução PT:",
                    containerColor = flagTheme.surfaceCard.copy(alpha = 0.4f),
                    onTogglePlayPause = {
                        speechManager.toggleStudyPortugueseLine(langDisplay.ptTranslation, ptLineId)
                    },
                    onWordClick = { offset ->
                        speechManager.speakStudyPortugueseLine(langDisplay.ptTranslation, ptLineId, offset)
                    }
                )
            }
        }
    }
}

/**
 * Cria termos padrão predefinidos para o estudo de inglês imediato
 * com 16 tempos verbais e question words em cada um.
 */
fun getPreloadedEnglishVerbsAndPhrasalVerbs(bookId: Int, pageNumber: Int, bookTitle: String): List<VerbPhrasalVerbStudyTerm> {
    return listOf(
        createPreloadedTerm(
            bookId, pageNumber, bookTitle,
            verbOrPhrase = "look for",
            isPhrasal = true,
            prep = "for",
            base = "look",
            pt = "Procurar, buscar, tentar encontrar algo ou alguém.",
            defEn = "To try to find something that is lost, or someone who is not present.",
            ipa = "/lʊk fɔːr/",
            examples = listOf(
                VerbTenseQuestionExample(1, "1. Present Simple", "Why", "Why do you always look for shortcuts instead of following the rules?", "Por que você sempre procura atalhos em vez de seguir as regras?", "look for + shortcuts"),
                VerbTenseQuestionExample(2, "2. Present Continuous", "Where", "Where are the researchers looking for fresh evidence right now?", "Onde os pesquisadores estão procurando novas evidências agora?", "look for + fresh evidence"),
                VerbTenseQuestionExample(3, "3. Present Perfect", "How", "How has your team looked for innovative solutions this quarter?", "Como sua equipe tem procurado soluções inovadoras neste trimestre?", "look for + innovative solutions"),
                VerbTenseQuestionExample(4, "4. Present Perfect Continuous", "How long", "How long have you been looking for that specific manuscript?", "Há quanto tempo você está procurando aquele manuscrito específico?", "look for + manuscript"),
                VerbTenseQuestionExample(5, "5. Past Simple", "When", "When did the detectives look for fingerprints at the scene?", "Quando os detetives procuraram impressões digitais na cena?", "look for + fingerprints"),
                VerbTenseQuestionExample(6, "6. Past Continuous", "Why", "Why were the passengers looking for shelter during the sudden blizzard?", "Por que os passageiros estavam procurando abrigo durante a nevasca repentina?", "look for + shelter"),
                VerbTenseQuestionExample(7, "7. Past Perfect", "Where", "Where had she looked for the lost necklace before calling the insurance company?", "Onde ela havia procurado o colar perdido antes de ligar para a seguradora?", "look for + lost necklace"),
                VerbTenseQuestionExample(8, "8. Past Perfect Continuous", "How many days", "How many days had they been looking for survivors before finding the cabin?", "Por quantos dias eles vinham procurando sobreviventes antes de encontrar a cabana?", "look for + survivors"),
                VerbTenseQuestionExample(9, "9. Future Simple (will)", "Who", "Who will look for the replacement parts tomorrow morning?", "Quem vai procurar as peças de reposição amanhã de manhã?", "look for + replacement parts"),
                VerbTenseQuestionExample(10, "10. Future Continuous", "At what time", "At what time will they be looking for our flight on the arrivals screen?", "A que horas eles estarão procurando nosso voo na tela de chegadas?", "look for + flight arrivals"),
                VerbTenseQuestionExample(11, "11. Future Perfect", "By when", "By when will the auditors have looked for all accounting discrepancies?", "Até quando os auditores terão procurado todas as discrepâncias contábeis?", "look for + discrepancies"),
                VerbTenseQuestionExample(12, "12. Future Perfect Continuous", "How many years", "How many years will the astronomers have been looking for exoplanets by 2030?", "Há quantos anos os astrônomos estarão procurando exoplanetas até 2030?", "look for + exoplanets"),
                VerbTenseQuestionExample(13, "13. Conditional Simple (would)", "Why", "Why would anyone look for complications when a simple method exists?", "Por que alguém procuraria complicações quando existe um método simples?", "look for + complications"),
                VerbTenseQuestionExample(14, "14. Conditional Continuous", "Where", "Where would they be looking for capital if the primary investor backed out?", "Onde eles estariam procurando capital se o investidor principal desistisse?", "look for + capital"),
                VerbTenseQuestionExample(15, "15. Conditional Perfect", "Which options", "Which options would you have looked for if you were in my position?", "Quais opções você teria procurado se estivesse na minha posição?", "look for + options"),
                VerbTenseQuestionExample(16, "16. Modal Perfect / Subjunctive", "Why should", "Why should we look for external approval when our metrics are outstanding?", "Por que deveríamos procurar aprovação externa quando nossas métricas são excelentes?", "look for + external approval")
            )
        ),
        createPreloadedTerm(
            bookId, pageNumber, bookTitle,
            verbOrPhrase = "achieve",
            isPhrasal = false,
            prep = "",
            base = "achieve",
            pt = "Alcançar, atingir, conquistar uma meta ou objetivo.",
            defEn = "To successfully bring about or reach a desired objective by effort or skill.",
            ipa = "/əˈtʃiːv/",
            examples = listOf(
                VerbTenseQuestionExample(1, "1. Present Simple", "What goals", "What goals do ambitious entrepreneurs achieve in their first five years?", "Quais metas empreendedores ambiciosos alcançam em seus primeiros cinco anos?", "achieve + goals"),
                VerbTenseQuestionExample(2, "2. Present Continuous", "How fast", "How fast are developing economies achieving digital transformation?", "Quão rápido economias em desenvolvimento estão alcançando a transformação digital?", "achieve + digital transformation"),
                VerbTenseQuestionExample(3, "3. Present Perfect", "What milestones", "What major milestones has the research laboratory achieved this decade?", "Quais marcos importantes o laboratório de pesquisa atingiu nesta década?", "achieve + milestones"),
                VerbTenseQuestionExample(4, "4. Present Perfect Continuous", "Why", "Why have human rights organizations been achieving steady progress worldwide?", "Por que organizações de direitos humanos vêm conquistando progresso constante pelo mundo?", "achieve + steady progress"),
                VerbTenseQuestionExample(5, "5. Past Simple", "When", "When did the mountaineers achieve the historic summit of K2?", "Quando os alpinistas alcançaram o cume histórico do K2?", "achieve + summit"),
                VerbTenseQuestionExample(6, "6. Past Continuous", "How", "How was the startup achieving profitability while rivals were burning cash?", "Como a startup estava alcançando lucratividade enquanto rivais queimavam caixa?", "achieve + profitability"),
                VerbTenseQuestionExample(7, "7. Past Perfect", "What level", "What level of fluency had she achieved before moving to Tokyo?", "Qual nível de fluência ela havia alcançado antes de se mudar para Tóquio?", "achieve + fluency"),
                VerbTenseQuestionExample(8, "8. Past Perfect Continuous", "How many breakthroughs", "How many breakthroughs had the team been achieving before the budget was cut?", "Quantas descobertas a equipe vinha alcançando antes do orçamento ser cortado?", "achieve + breakthroughs"),
                VerbTenseQuestionExample(9, "9. Future Simple (will)", "How", "How will sustainable aviation achieve zero net carbon emissions by 2050?", "Como a aviação sustentável alcançará zero emissões líquidas de carbono até 2050?", "achieve + zero emissions"),
                VerbTenseQuestionExample(10, "10. Future Continuous", "In what areas", "In what areas will medical science be achieving cures over the next generation?", "Em quais áreas a ciência médica estará alcançando curas ao longo da próxima geração?", "achieve + cures"),
                VerbTenseQuestionExample(11, "11. Future Perfect", "By when", "By when will developing nations have achieved universal primary literacy?", "Até quando as nações em desenvolvimento terão atingido a alfabetização primária universal?", "achieve + literacy"),
                VerbTenseQuestionExample(12, "12. Future Perfect Continuous", "For how long", "For how long will the space agency have been achieving missions before Mars landing?", "Por quanto tempo a agência espacial terá estado alcançando missões antes do pouso em Marte?", "achieve + missions"),
                VerbTenseQuestionExample(13, "13. Conditional Simple (would)", "Why", "Why would a company achieve short-term profits at the expense of ethics?", "Por que uma empresa alcançaria lucros de curto prazo às custas da ética?", "achieve + short-term profits"),
                VerbTenseQuestionExample(14, "14. Conditional Continuous", "Where", "Where would educators be achieving greater student engagement with new tools?", "Onde os educadores estariam alcançando maior engajamento dos alunos com novas ferramentas?", "achieve + student engagement"),
                VerbTenseQuestionExample(15, "15. Conditional Perfect", "Which objectives", "Which objectives would our firm have achieved if we secured the contract?", "Quais objetivos nossa empresa teria alcançado se garantíssemos o contrato?", "achieve + objectives"),
                VerbTenseQuestionExample(16, "16. Modal Perfect / Subjunctive", "What can", "What can humanity achieve when global cooperation prevails over division?", "O que a humanidade pode alcançar quando a cooperação global prevalece sobre a divisão?", "achieve + great feats")
            )
        ),
        createPreloadedTerm(
            bookId, pageNumber, bookTitle,
            verbOrPhrase = "give up",
            isPhrasal = true,
            prep = "up",
            base = "give",
            pt = "Desistir, renunciar, abandonar um hábito ou rendição.",
            defEn = "To cease making an effort; to surrender or resign from an attempt or habit.",
            ipa = "/ɡɪv ʌp/",
            examples = listOf(
                VerbTenseQuestionExample(1, "1. Present Simple", "Why", "Why do so many people give up their fitness routines before February?", "Por que tantas pessoas desistem de suas rotinas de exercícios antes de fevereiro?", "give up + fitness routine"),
                VerbTenseQuestionExample(2, "2. Present Continuous", "Who", "Who is giving up smoking in your workplace this month?", "Quem está abandonando o cigarro no seu local de trabalho este mês?", "give up + smoking"),
                VerbTenseQuestionExample(3, "3. Present Perfect", "Why has", "Why has the negotiation panel given up all hope of an immediate truce?", "Por que a bancada de negociação desistiu de toda esperança de trégua imediata?", "give up + hope"),
                VerbTenseQuestionExample(4, "4. Present Perfect Continuous", "How long", "How long have analysts been giving up on traditional retail forecasting models?", "Há quanto tempo os analistas vêm desistindo dos modelos tradicionais de previsão?", "give up on + models"),
                VerbTenseQuestionExample(5, "5. Past Simple", "When", "When did the resistance soldiers finally give up their fortress?", "Quando os soldados da resistência finalmente entregaram sua fortaleza?", "give up + fortress"),
                VerbTenseQuestionExample(6, "6. Past Continuous", "Why were", "Why were the marathon runners giving up halfway through the grueling course?", "Por que os maratonistas estavam desistindo no meio do percurso extenuante?", "give up + halfway"),
                VerbTenseQuestionExample(7, "7. Past Perfect", "Where had", "Where had the suspect given up before federal marshals located him?", "Onde o suspeito havia se rendido antes dos delegados federais o localizarem?", "give up + surrender"),
                VerbTenseQuestionExample(8, "8. Past Perfect Continuous", "Why had they", "Why had investors been giving up on the biotech project before the breakthrough?", "Por que os investidores vinham desistindo do projeto de biotecnologia antes do avanço?", "give up on + biotech"),
                VerbTenseQuestionExample(9, "9. Future Simple (will)", "Why will", "Why will determined scholars never give up the pursuit of scientific truth?", "Por que acadêmicos determinados nunca desistirão da busca pela verdade científica?", "give up + pursuit of truth"),
                VerbTenseQuestionExample(10, "10. Future Continuous", "Who will", "Who will be giving up their weekend to supervise the server migration?", "Quem estará abrindo mão do seu fim de semana para supervisionar a migração dos servidores?", "give up + weekend"),
                VerbTenseQuestionExample(11, "11. Future Perfect", "By when", "By when will the patient have given up sugar completely?", "Até quando o paciente terá abandonado o açúcar completamente?", "give up + sugar"),
                VerbTenseQuestionExample(12, "12. Future Perfect Continuous", "How many years", "For how many years will athletes have been giving up personal comfort for the Olympics?", "Por quantos anos atletas terão estado abrindo mão do conforto pessoal pelas Olimpíadas?", "give up + comfort"),
                VerbTenseQuestionExample(13, "13. Conditional Simple (would)", "Why would", "Why would a passionate musician give up their instrument for corporate work?", "Por que um músico apaixonado desistiria de seu instrumento por trabalho corporativo?", "give up + instrument"),
                VerbTenseQuestionExample(14, "14. Conditional Continuous", "Where would", "Where would citizens be giving up civil liberties if the regime tightened control?", "Onde os cidadãos estariam abrindo mão de liberdades civis se o regime apertasse o controle?", "give up + liberties"),
                VerbTenseQuestionExample(15, "15. Conditional Perfect", "Under what conditions", "Under what conditions would you have given up the legal battle?", "Sob quais condições você teria desistido da batalha jurídica?", "give up + legal battle"),
                VerbTenseQuestionExample(16, "16. Modal Perfect / Subjunctive", "Why must", "Why must champions never give up when exhaustion sets in?", "Por que os campeões nunca devem desistir quando o cansaço surge?", "give up + never")
            )
        ),
        createPreloadedTerm(
            bookId, pageNumber, bookTitle,
            verbOrPhrase = "depend on",
            isPhrasal = true,
            prep = "on",
            base = "depend",
            pt = "Depender de, confiar em algo ou alguém.",
            defEn = "To rely on, be controlled by, or determined by someone or something.",
            ipa = "/dɪˈpend ɒn/",
            examples = listOf(
                VerbTenseQuestionExample(1, "1. Present Simple", "How much", "How much does economic prosperity depend on reliable infrastructure?", "Quanto a prosperidade econômica depende de infraestrutura confiável?", "depend on + infrastructure"),
                VerbTenseQuestionExample(2, "2. Present Continuous", "Why", "Why are emerging industries depending on renewable energy subsidies right now?", "Por que as indústrias emergentes estão dependendo de subsídios de energia renovável agora?", "depend on + subsidies"),
                VerbTenseQuestionExample(3, "3. Present Perfect", "To what extent", "To what extent has global supply chain resilience depended on maritime security?", "Em que medida a resiliência da cadeia global de suprimentos dependeu da segurança marítima?", "depend on + maritime security"),
                VerbTenseQuestionExample(4, "4. Present Perfect Continuous", "Why have", "Why have rural communities been depending on water trucks for six months?", "Por que as comunidades rurais têm dependido de caminhões-pipa por seis meses?", "depend on + water supply"),
                VerbTenseQuestionExample(5, "5. Past Simple", "Upon whom", "Upon whom did the kingdom depend during the monarch's illness?", "De quem o reino dependeu durante a doença do monarca?", "depend upon + regent"),
                VerbTenseQuestionExample(6, "6. Past Continuous", "Why were", "Why were the patients depending on manual ventilators during the blackout?", "Por que os pacientes estavam dependendo de ventiladores manuais durante o apagão?", "depend on + ventilators"),
                VerbTenseQuestionExample(7, "7. Past Perfect", "How long had", "How long had the colony depended on imports before establishing local agriculture?", "Por quanto tempo a colônia havia dependido de importações antes de estabelecer a agricultura local?", "depend on + imports"),
                VerbTenseQuestionExample(8, "8. Past Perfect Continuous", "Why had they", "Why had the villagers been depending on rainwater before the well was drilled?", "Por que os moradores vinham dependendo da água da chuva antes de o poço ser perfurado?", "depend on + rainwater"),
                VerbTenseQuestionExample(9, "9. Future Simple (will)", "How heavily", "How heavily will future autonomous vehicles depend on satellite telemetry?", "Com que intensidade os futuros veículos autônomos dependerão da telemetria por satélite?", "depend on + telemetry"),
                VerbTenseQuestionExample(10, "10. Future Continuous", "In what ways", "In what ways will modern hospitals be depending on robotic assistants by 2035?", "De que maneiras os hospitais modernos estarão dependendo de assistentes robóticos até 2035?", "depend on + robotics"),
                VerbTenseQuestionExample(11, "11. Future Perfect", "By when", "By when will clean energy grids have depended entirely on battery storage?", "Até quando as redes de energia limpa terão dependido inteiramente de armazenamento em baterias?", "depend on + battery storage"),
                VerbTenseQuestionExample(12, "12. Future Perfect Continuous", "How many decades", "For how many decades will modern civilization have been depending on semiconductors?", "Por quantas décadas a civilização moderna terá dependido de semicondutores?", "depend on + semiconductors"),
                VerbTenseQuestionExample(13, "13. Conditional Simple (would)", "Why would", "Why would a sovereign nation depend on foreign food imports indefinitely?", "Por que uma nação soberana dependeria de importações estrangeiras de alimentos indefinidamente?", "depend on + imports"),
                VerbTenseQuestionExample(14, "14. Conditional Continuous", "Where would", "Where would coastal cities be depending on seawalls if sea levels rose faster?", "Onde as cidades costeiras estariam dependendo de diques marítimos se o nível do mar subisse mais rápido?", "depend on + seawalls"),
                VerbTenseQuestionExample(15, "15. Conditional Perfect", "How much", "How much would our project have depended on government funding if we had no private angel investors?", "Quanto nosso projeto teria dependido de financiamento governamental se não tivéssemos investidores anjo privados?", "depend on + funding"),
                VerbTenseQuestionExample(16, "16. Modal Perfect / Subjunctive", "Why should", "Why should a healthy adult depend on other people's validation?", "Por que um adulto saudável deveria depender da validação alheia?", "depend on + validation")
            )
        )
    )
}

private fun createPreloadedTerm(
    bookId: Int,
    pageNumber: Int,
    bookTitle: String,
    verbOrPhrase: String,
    isPhrasal: Boolean,
    prep: String,
    base: String,
    pt: String,
    defEn: String,
    ipa: String,
    examples: List<VerbTenseQuestionExample>
): VerbPhrasalVerbStudyTerm {
    return VerbPhrasalVerbStudyTerm(
        bookId = bookId,
        pageNumber = pageNumber,
        bookTitle = bookTitle,
        verbOrPhrase = verbOrPhrase,
        isPhrasalVerb = isPhrasal,
        prepositionUsed = prep,
        baseVerb = base,
        grammaticalCategory = if (isPhrasal) "Phrasal Verb" else "Verbo",
        languageCode = "en",
        definitionTargetLang = defEn,
        portugueseMeaning = pt,
        ipaTranscription = ipa,
        examplesJson = VerbPhrasalVerbStudyTerm.buildJsonFromExamples(examples)
    )
}
