package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookCategoryDefinition
import com.example.data.model.BookCategoryTaxonomy
import com.example.data.model.PdfBook

/**
 * Top floating badge displayed on the book cover.
 * Subtle, discreet, and professional according to user intent.
 */
@Composable
fun BookCoverCategoryTopBadge(
    book: PdfBook,
    modifier: Modifier = Modifier,
    isClassifying: Boolean = false,
    isSpeakingCategory: Boolean = false,
    onSpeakCategory: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val palette = BookCategoryTaxonomy.getPaletteForCategory(book.category, book.categoryColor)
    val categoryLabel = book.category?.trim()?.ifBlank { null } ?: "Classificando..."

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xDD0C0D12),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isSpeakingCategory) palette.accentColor else palette.accentColor.copy(alpha = 0.65f)
        ),
        shadowElevation = if (isSpeakingCategory) 12.dp else 8.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            if (isClassifying) {
                CircularProgressIndicator(
                    color = palette.accentColor,
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(10.dp)
                )
            } else if (isSpeakingCategory) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Lendo categoria em inglês",
                    tint = palette.accentColor,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = palette.accentColor,
                    modifier = Modifier.size(8.dp)
                ) {}
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = categoryLabel.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.accentColor,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!book.subcategory.isNullOrBlank()) {
                Text(
                    text = " • ${book.subcategory}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onSpeakCategory != null && !isSpeakingCategory) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Ouvir em inglês",
                    tint = palette.accentColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(11.dp)
                        .clickable { onSpeakCategory() }
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Ver categoria e cores",
                tint = palette.accentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

/**
 * Dialog displaying full classification details with OpenAI Vision reclassification,
 * Category Color Mode activation, and taxonomy explorer with all 38 categories (no numbers).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookCategoryDetailsDialog(
    book: PdfBook,
    isClassifying: Boolean,
    currentReadingTheme: String,
    onDismiss: () -> Unit,
    onApplyCategoryColorTheme: () -> Unit,
    onReclassifyWithOpenAi: () -> Unit,
    onSelectCategory: (category: String, subcategory: String?, colorHex: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Visão Geral, 1 = Todas Categorias
    val palette = BookCategoryTaxonomy.getPaletteForCategory(book.category, book.categoryColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10121A)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.accentColor.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with category badge & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = palette.accentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = palette.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Classificação do Livro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "OpenAI Vision & Taxonomia Acadêmica",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1D28), RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    val tabModifier0 = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) palette.accentColor else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp)

                    val tabModifier1 = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) palette.accentColor else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp)

                    Box(modifier = tabModifier0, contentAlignment = Alignment.Center) {
                        Text(
                            text = "Modo de Cores & Status",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Box(modifier = tabModifier1, contentAlignment = Alignment.Center) {
                        Text(
                            text = "38 Categorias (Taxonomia)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Overview & Color Mode Screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Current Category Hero Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surfaceColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "CATEGORIA ATUAL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentColor,
                                        letterSpacing = 1.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = palette.accentColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = palette.hexCode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = book.category ?: "Não classificado",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = palette.textColor
                                )

                                if (!book.subcategory.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Subcategoria: ${book.subcategory}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.textColor.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Color preview swatches
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Paleta:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textColor.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.surfaceColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                        modifier = Modifier.size(18.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.activeSentenceBg,
                                        modifier = Modifier.size(18.dp)
                                    ) {}
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action 1: Activate Category Color Mode in Reels
                        val isColorModeActive = currentReadingTheme == "CATEGORY_COLOR"
                        Button(
                            onClick = {
                                onApplyCategoryColorTheme()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isColorModeActive) Color(0xFF10B981) else palette.accentColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (isColorModeActive) Icons.Default.Check else Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isColorModeActive) "Modo de Cores Ativo no Reels" else "Ativar Modo de Cores no Reels",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action 2: Reclassify using OpenAI Vision
                        OutlinedButton(
                            onClick = onReclassifyWithOpenAi,
                            enabled = !isClassifying,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC084FC).copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isClassifying) {
                                CircularProgressIndicator(
                                    color = Color(0xFFC084FC),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Classificando com OpenAI...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reclassificar Capa com OpenAI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Info Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF181B26),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "A API da OpenAI analisa a capa, título e autor do livro e associa automaticamente a uma das 38 categorias oficiais, aplicando a paleta harmônica correspondente na experiência de leitura no Reels.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    // Category Taxonomy Explorer (All 38 Categories, Clean, No Numbers)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar categoria ou subcategoria...", fontSize = 12.sp, color = Color.Gray) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredCategories = remember(searchQuery) {
                            if (searchQuery.isBlank()) {
                                BookCategoryTaxonomy.CATEGORIES
                            } else {
                                val q = searchQuery.trim().lowercase()
                                BookCategoryTaxonomy.CATEGORIES.filter { cat ->
                                    cat.name.lowercase().contains(q) ||
                                            cat.subcategories.any { sub -> sub.lowercase().contains(q) }
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredCategories, key = { it.name }) { categoryDef ->
                                CategoryTaxonomyRow(
                                    categoryDef = categoryDef,
                                    isSelected = book.category.equals(categoryDef.name, ignoreCase = true),
                                    onSelect = { sub ->
                                        onSelectCategory(categoryDef.name, sub, categoryDef.defaultColorHex)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryTaxonomyRow(
    categoryDef: BookCategoryDefinition,
    isSelected: Boolean,
    onSelect: (subcategory: String?) -> Unit
) {
    var isExpanded by remember { mutableStateOf(isSelected) }
    val catColor = try {
        Color(android.graphics.Color.parseColor(categoryDef.defaultColorHex))
    } catch (_: Exception) {
        Color(0xFF8E2DE2)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1E2333) else Color(0xFF141722),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) catColor else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = catColor,
                        modifier = Modifier.size(12.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = categoryDef.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) catColor else Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catColor.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onSelect(null) }
                    ) {
                        Text(
                            text = if (isSelected) "Selecionado" else "Escolher",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (isExpanded && categoryDef.subcategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryDef.subcategories.forEach { sub ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF222738),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { onSelect(sub) }
                        ) {
                            Text(
                                text = sub,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
