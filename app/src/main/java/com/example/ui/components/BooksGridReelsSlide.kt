package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BookCategoryTaxonomy
import com.example.data.model.PdfBook
import com.example.data.pdf.PdfRendererHelper

/**
 * Slide do Reels que exibe 4 livros simultaneamente na tela (Grade 2x2).
 * Cada livro possui sua capa de alta qualidade, informações e o botão dedicado
 * "Toque para Ler" indicando a página onde o usuário parou a leitura.
 */
@Composable
fun BooksGridReelsSlide(
    books: List<PdfBook>,
    readingTheme: String = "CATEGORY_COLOR",
    onEnterBook: (PdfBook) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 58.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
    ) {
        val displayBooks = books.take(4)
        val rows = displayBooks.chunked(2)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { rowBooks ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowBooks.forEach { book ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            BookGridCard(
                                book = book,
                                readingTheme = readingTheme,
                                onEnterBook = { onEnterBook(book) }
                            )
                        }
                    }
                    if (rowBooks.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (rows.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Card individual do livro na grade de 4 do Reels.
 */
@Composable
fun BookGridCard(
    book: PdfBook,
    readingTheme: String,
    onEnterBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coverBitmap by remember(book.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(book.id, book.fileUri) {
        if (!book.fileUri.isNullOrBlank() && coverBitmap == null) {
            try {
                coverBitmap = PdfRendererHelper.renderPdfPageBitmap(
                    context = context,
                    fileUri = book.fileUri,
                    pageIndexZeroBased = 0,
                    destWidth = 420,
                    readingTheme = "ORIGINAL"
                )
            } catch (_: Throwable) {
                // Silently fallback to vector gradient cover
            }
        }
    }

    val categoryPalette = remember(book.category, book.categoryColor) {
        BookCategoryTaxonomy.getPaletteForCategory(book.category, book.categoryColor)
    }

    val accentColor = when (readingTheme) {
        "CATEGORY_COLOR" -> categoryPalette.accentColor
        "PAPER_SEPIA" -> Color(0xFFD4AF37)
        "DARK_OLED" -> Color(0xFFE2E8F0)
        "CINEMA_YELLOW" -> Color(0xFFFFD700)
        "CYBERPUNK_NEON" -> Color(0xFF00FFCC)
        "STREAMING_BOX" -> Color(0xFF818CF8)
        "TERMINAL_GREEN" -> Color(0xFF00FF66)
        else -> Color(0xFFC084FC)
    }

    val cardBg = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFF2A2117)
        "DARK_OLED" -> Color(0xFF111111)
        "CINEMA_YELLOW" -> Color(0xFF14130A)
        "CYBERPUNK_NEON" -> Color(0xFF0C1022)
        "STREAMING_BOX" -> Color(0xFF16192E)
        "TERMINAL_GREEN" -> Color(0xFF021406)
        else -> Color(0xFF1A1A24)
    }

    val textCol = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFFF7EED8)
        "CINEMA_YELLOW" -> Color(0xFFFFFAEB)
        "TERMINAL_GREEN" -> Color(0xFF80FF9F)
        else -> Color.White
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEnterBook() }
            .testTag("book_grid_card_${book.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Área da Capa com distintivos e proporção adaptável
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                if (!book.coverPath.isNullOrBlank()) {
                    AsyncImage(
                        model = book.coverPath,
                        contentDescription = "Capa de ${book.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = "Capa de ${book.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Fallback temático caso o PDF ainda não tenha capa extraída
                    val gradientColors = when (readingTheme) {
                        "CATEGORY_COLOR" -> listOf(categoryPalette.surfaceColor, categoryPalette.accentColor.copy(alpha = 0.5f))
                        "PAPER_SEPIA" -> listOf(Color(0xFF4A3828), Color(0xFF2E2419))
                        "DARK_OLED" -> listOf(Color(0xFF222222), Color(0xFF0D0D0D))
                        "CYBERPUNK_NEON" -> listOf(Color(0xFF260D2E), Color(0xFF0D1127))
                        "TERMINAL_GREEN" -> listOf(Color(0xFF002B11), Color(0xFF021204))
                        else -> listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(gradientColors))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Tag de progresso e página no canto superior direito
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = if (book.currentPage > 1) "Pág. ${book.currentPage}/${book.totalPages}" else "${book.totalPages} págs",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (book.currentPage > 1) accentColor else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Tag da categoria no canto superior esquerdo
                if (!book.category.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        color = categoryPalette.surfaceColor.copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, categoryPalette.accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = categoryPalette.accentColor,
                                modifier = Modifier.size(5.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = book.category!!,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = categoryPalette.accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Título e Autor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = book.title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = book.author,
                    fontSize = 9.5.sp,
                    color = textCol.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Botão Toque para Ler da Página onde Parou (destaque e acessibilidade)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.20f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onEnterBook() }
                    .testTag("read_button_${book.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Toque para Ler",
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (book.currentPage > 1) {
                            "Continuar Pág. ${book.currentPage}"
                        } else {
                            "Toque para Ler • Pág. 1"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textCol,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
