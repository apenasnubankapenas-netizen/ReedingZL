package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SavedSentenceAnalysis
import com.example.data.model.StudyLanguage
import com.example.data.speech.PdfSpeechManager
import com.example.ui.theme.ReadingThemeManager

/**
 * Caderno de Estudos de Gramática & Sintaxe Poliglota:
 * Exibe todas as frases salvas localmente para estudo posterior offline.
 * Suporta filtragem por idioma (Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico, Inglês),
 * busca textual, áudio TTS e expansão detalhada de regras e tokens.
 */
@Composable
fun SavedSentencesStudySheet(
    savedAnalyses: List<SavedSentenceAnalysis>,
    readingTheme: String = "DARK_OLED",
    category: String? = null,
    categoryColor: String? = null,
    speechManager: PdfSpeechManager? = null,
    onToggleFavorite: (SavedSentenceAnalysis) -> Unit = {},
    onDeleteAnalysis: (Long) -> Unit = {},
    onUpdateNotes: (SavedSentenceAnalysis) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = remember(readingTheme, category, categoryColor) {
        ReadingThemeManager.getPalette(readingTheme, category, categoryColor)
    }

    val accent = palette.accent
    val textColor = palette.textPrimary
    val subTextColor = palette.textSecondary
    val cardBg = palette.surfaceVariant
    val borderColor = palette.cardBorder.takeIf { it != Color.Transparent } ?: accent.copy(alpha = 0.45f)

    var selectedLangFilter by remember { mutableStateOf<StudyLanguage?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }

    val filteredList = remember(savedAnalyses, selectedLangFilter, searchQuery, onlyFavorites) {
        savedAnalyses.filter { item ->
            val matchLang = selectedLangFilter == null || item.languageCode == selectedLangFilter!!.code
            val matchFav = !onlyFavorites || item.isFavorite
            val matchQuery = searchQuery.isBlank() ||
                item.sentenceText.contains(searchQuery, ignoreCase = true) ||
                item.translationPt.contains(searchQuery, ignoreCase = true) ||
                item.transliteration.contains(searchQuery, ignoreCase = true) ||
                item.syntacticTreeSummary.contains(searchQuery, ignoreCase = true) ||
                item.userNotes.contains(searchQuery, ignoreCase = true)

            matchLang && matchFav && matchQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(22.dp),
            color = palette.surface,
            border = BorderStroke(1.2.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // 1. CABEÇALHO DO CADERNO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, accent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Caderno de Sintaxe & Gramática",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "${savedAnalyses.size} frases salvas para estudo offline",
                                fontSize = 11.5.sp,
                                color = subTextColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = subTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. BARRA DE BUSCA E FILTRO DE FAVORITOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar frase, tradução ou regra...", fontSize = 12.sp, color = subTextColor) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = subTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar", tint = subTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botão Favoritos
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (onlyFavorites) Color(0xFFF59E0B).copy(alpha = 0.22f) else cardBg,
                        border = BorderStroke(
                            1.dp,
                            if (onlyFavorites) Color(0xFFF59E0B) else borderColor.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .clickable { onlyFavorites = !onlyFavorites }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Apenas Favoritos",
                                tint = if (onlyFavorites) Color(0xFFF59E0B) else subTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. CHIPS DE FILTRO POR IDIOMA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip "Todos"
                    val isAllSelected = selectedLangFilter == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllSelected) accent.copy(alpha = 0.25f) else cardBg,
                        border = BorderStroke(
                            if (isAllSelected) 1.2.dp else 0.5.dp,
                            if (isAllSelected) accent else borderColor.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { selectedLangFilter = null }
                    ) {
                        Text(
                            text = "Todos (${savedAnalyses.size})",
                            fontSize = 11.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAllSelected) accent else textColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Chips por idioma
                    StudyLanguage.entries.forEach { lang ->
                        val count = savedAnalyses.count { it.languageCode == lang.code }
                        if (count > 0 || lang != StudyLanguage.ENGLISH) {
                            val isSelected = selectedLangFilter == lang
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) lang.badgeColor.copy(alpha = 0.28f) else cardBg,
                                border = BorderStroke(
                                    if (isSelected) 1.2.dp else 0.5.dp,
                                    if (isSelected) lang.badgeColor else borderColor.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { selectedLangFilter = if (isSelected) null else lang }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = lang.flag, fontSize = 11.5.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${lang.displayName} ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) lang.badgeColor else textColor
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = borderColor.copy(alpha = 0.3f),
                    thickness = 0.8.dp
                )

                // 4. LISTA DE FRASES SALVAS
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = subTextColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (savedAnalyses.isEmpty()) "Nenhuma frase salva ainda." else "Nenhuma frase corresponde aos filtros.",
                                color = textColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Selecione frases complexas nos livros e use 'sintaxe' para analisá-las e guardá-las aqui.",
                                color = subTextColor,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 30.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            SavedSentenceCard(
                                item = item,
                                cardBg = cardBg,
                                borderColor = borderColor,
                                textColor = textColor,
                                subTextColor = subTextColor,
                                accent = accent,
                                speechManager = speechManager,
                                onToggleFavorite = { onToggleFavorite(item) },
                                onDelete = { onDeleteAnalysis(item.id) },
                                onUpdateNotes = onUpdateNotes
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // 5. RODAPÉ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Concluir Estudo", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSentenceCard(
    item: SavedSentenceAnalysis,
    cardBg: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
    accent: Color,
    speechManager: PdfSpeechManager?,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onUpdateNotes: (SavedSentenceAnalysis) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var isEditingNotes by remember { mutableStateOf(false) }
    var notesText by remember(item.userNotes) { mutableStateOf(item.userNotes) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    val lang = item.language

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(0.8.dp, if (item.isFavorite) Color(0xFFF59E0B).copy(alpha = 0.6f) else borderColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Linha Superior: Idioma, Livro, Favorito, Áudio, Lixeira
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = lang.badgeColor.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lang.flag, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = lang.displayName,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = lang.badgeColor
                            )
                        }
                    }

                    if (item.bookTitle.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${item.bookTitle} (pág ${item.pageNumber})",
                            fontSize = 10.5.sp,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Áudio
                    IconButton(
                        onClick = {
                            isPlayingAudio = true
                            speechManager?.speakTextInStudyLanguage(
                                text = item.sentenceText,
                                language = lang
                            )
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ouvir",
                            tint = if (isPlayingAudio) lang.badgeColor else subTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Favorito
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (item.isFavorite) Color(0xFFF59E0B) else subTextColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Excluir
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remover",
                            tint = subTextColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Frase Original
            Text(
                text = item.sentenceText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                lineHeight = 22.sp,
                style = LocalTextStyle.current.copy(
                    textDirection = if (lang.isRtl) TextDirection.Rtl else TextDirection.Ltr
                )
            )

            // Transliteração
            if (item.transliteration.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.transliteration,
                    fontSize = 11.5.sp,
                    color = lang.badgeColor.copy(alpha = 0.9f)
                )
            }

            // Tradução Português
            if (item.translationPt.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = item.translationPt,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.85f)
                )
            }

            // Resumo da Árvore Sintática
            if (item.syntacticTreeSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.syntacticTreeSummary,
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.9f),
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Detalhes Expandidos: Tokens, Regras e Dica C2
            AnimatedVisibility(visible = isExpanded) {
                val tokens = remember(item) { item.getTokens() }
                val rules = remember(item) { item.getRules() }

                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Tokens
                    if (tokens.isNotEmpty()) {
                        Text(
                            text = "Decomposição Token a Token:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        tokens.forEach { tokenItem ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = tokenItem.token,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = lang.badgeColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tokenItem.syntacticRole} • ${tokenItem.morphologyOrCase}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor
                                        )
                                        if (tokenItem.explanation.isNotBlank()) {
                                            Text(
                                                text = tokenItem.explanation,
                                                fontSize = 10.sp,
                                                color = subTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Regras Gramaticais
                    if (rules.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Regras Gramaticais Aplicadas:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = lang.badgeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        rules.forEach { rule ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.15f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(
                                        text = rule.ruleTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = rule.explanation,
                                        fontSize = 10.5.sp,
                                        color = subTextColor
                                    )
                                }
                            }
                        }
                    }

                    // Dica C2
                    if (item.c2MasteryTip.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                            border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Dica C2 de Domínio Nativo:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                                Text(
                                    text = item.c2MasteryTip,
                                    fontSize = 10.5.sp,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // Anotações Pessoais
            if (item.userNotes.isNotBlank() && !isEditingNotes) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Nota: ${item.userNotes}",
                            fontSize = 11.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isEditingNotes = true }, modifier = Modifier.size(20.dp)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar nota", tint = accent, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            if (isEditingNotes) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Anotações de estudo...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditingNotes = false }) {
                        Text("Cancelar", fontSize = 11.sp, color = subTextColor)
                    }
                    TextButton(onClick = {
                        onUpdateNotes(item.copy(userNotes = notesText.trim()))
                        isEditingNotes = false
                    }) {
                        Text("Salvar Nota", fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Barra inferior do card: botão Expandir / Ocultar
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Ocultar detalhes sintáticos" else "Ver decomposição sintática completa",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
