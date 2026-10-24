package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AuthorCollection
import com.example.data.model.PdfBook
import com.example.data.model.SubjectDiscipline

/**
 * Slide do modo Reels exibindo a Matéria / Disciplina no formato de capa de livro realista.
 * O nome da disciplina é apresentado com grande destaque visual ("o nome dela vai ser grande").
 */
@Composable
fun SubjectReelsSlide(
    subject: SubjectDiscipline,
    subjectBooks: List<PdfBook>,
    subjectAuthors: List<AuthorCollection>,
    readingTheme: String = "CATEGORY_COLOR",
    onOpenDiscipline: (SubjectDiscipline) -> Unit,
    onManageContent: (SubjectDiscipline) -> Unit,
    onAddDirectBook: (SubjectDiscipline) -> Unit,
    onEditSubject: (SubjectDiscipline) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Parse base subject color or default to academic blue
    val parsedColor = remember(subject.bannerColorHex) {
        try {
            Color(android.graphics.Color.parseColor(subject.bannerColorHex))
        } catch (e: Exception) {
            Color(0xFF1E3A8A)
        }
    }

    // Outer background gradient following the reading theme
    val bgGradient = when (readingTheme) {
        "PAPER_SEPIA" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF2C2218), Color(0xFF1F1710), Color(0xFF140F0A))
        )
        "TERMINAL_GREEN" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF031006), Color(0xFF020703), Color(0xFF000000))
        )
        "CYBERPUNK_NEON" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0F0C24), Color(0xFF180A28), Color(0xFF070A14))
        )
        "CINEMA_YELLOW" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF141305), Color(0xFF0D0C03), Color(0xFF000000))
        )
        "DARK_OLED" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF000000), Color(0xFF000000))
        )
        "MONOCHROME_BW" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D), Color(0xFF000000))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0F1D), Color(0xFF0F172A), Color(0xFF060911))
        )
    }

    // Rich hardcover book texture gradient
    val bookCoverGradient = Brush.verticalGradient(
        colors = listOf(
            parsedColor.copy(alpha = 0.95f),
            parsedColor.copy(alpha = 0.80f),
            Color(0xFF0B0F19).copy(alpha = 0.95f)
        )
    )

    // Gold foil metallic border brush
    val goldFoilBorder = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFDE047),
            Color(0xFFCA8A04),
            Color(0xFFFEF08A),
            Color(0xFFB45309)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // =========================================================================
            // CAPA DE LIVRO REALISTA (Hardcover Book Cover)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                    .shadow(elevation = 18.dp, shape = RoundedCornerShape(18.dp))
                    .border(
                        width = 2.5.dp,
                        brush = goldFoilBorder,
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                    )
                    .background(bookCoverGradient)
                    .clickable { onOpenDiscipline(subject) }
            ) {
                // If custom cover image is set, display it in background with dark scrim
                if (!subject.coverUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(subject.coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Capa de ${subject.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    // Dark vignette overlay for crisp legibility
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Black.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                }

                // Efeito 3D de lombada de livro no lado esquerdo (Book Spine Shadow)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(22.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Conteúdo da Capa
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 20.dp, top = 26.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge superior da disciplina
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "MATÉRIA & DISCIPLINA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFEF08A),
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ícone Acadêmico central decorativo
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // =====================================================================
                    // O NOME DA DISCIPLINA VAI SER GRANDE (Requisito fundamental do usuário)
                    // =====================================================================
                    Text(
                        text = subject.name.uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Linha decorativa dourada
                    Box(
                        modifier = Modifier
                            .padding(vertical = 14.dp)
                            .width(80.dp)
                            .height(2.5.dp)
                            .background(goldFoilBorder)
                    )

                    // Descrição / Ementa da Disciplina
                    if (subject.description.isNotBlank()) {
                        Text(
                            text = subject.description,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    } else {
                        Text(
                            text = "Toque para abrir todos os livros e autores desta disciplina",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tags com contagens de Autores e Livros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tag de Autores
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${subjectAuthors.size} ${if (subjectAuthors.size == 1) "Autor" else "Autores"}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tag de Livros
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF86EFAC),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${subjectBooks.size} ${if (subjectBooks.size == 1) "Livro" else "Livros"}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Mini carrossel de livros pertencentes a esta disciplina
                    if (subjectBooks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(subjectBooks.take(6)) { book ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.55f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier.width(95.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = book.title,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =========================================================================
            // BOTÕES DE AÇÃO E GERENCIAMENTO
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(0.94f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Abrir Matéria (Filtra o feed de reels exclusivamente para esta disciplina)
                Button(
                    onClick = { onOpenDiscipline(subject) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = parsedColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Abrir Matéria",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 2. Gerenciar Autores e Livros
                OutlinedButton(
                    onClick = { onManageContent(subject) },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Gerenciar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Gerenciar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // 3. + Livro Direto
                OutlinedButton(
                    onClick = { onAddDirectBook(subject) },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Livro",
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Livro",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                }

                // 4. Editar Matéria
                IconButton(
                    onClick = { onEditSubject(subject) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Matéria",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
