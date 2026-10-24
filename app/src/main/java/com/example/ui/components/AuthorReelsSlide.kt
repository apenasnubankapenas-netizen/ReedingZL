package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun AuthorReelsSlide(
    author: AuthorCollection,
    authorBooks: List<PdfBook>,
    readingTheme: String = "CATEGORY_COLOR",
    onOpenCollection: (AuthorCollection) -> Unit,
    onManageBooks: (AuthorCollection) -> Unit,
    onEditAuthor: (AuthorCollection) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Paleta temática personalizada de acordo com o tema selecionado
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
        "STREAMING_BOX" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF161722), Color(0xFF0F111A), Color(0xFF0A0B10))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF090D16))
        )
    }

    val authorNameColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFFFDE68A)
        "TERMINAL_GREEN" -> Color(0xFF00FF66)
        "CYBERPUNK_NEON" -> Color(0xFF00F0FF)
        "CINEMA_YELLOW" -> Color(0xFFFFE600)
        "MONOCHROME_BW" -> Color(0xFFF5F5F5)
        else -> Color.White
    }

    val authorBioColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFFE5D5B8)
        "TERMINAL_GREEN" -> Color(0xFF80FFB0)
        "CYBERPUNK_NEON" -> Color(0xFFE2E8F0)
        "CINEMA_YELLOW" -> Color(0xFFFFF59D)
        "MONOCHROME_BW" -> Color(0xFFCCCCCC)
        else -> Color.White.copy(alpha = 0.82f)
    }

    val primaryActionColor = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFF8B5E34)
        "TERMINAL_GREEN" -> Color(0xFF008033)
        "CYBERPUNK_NEON" -> Color(0xFF7C3AED)
        "CINEMA_YELLOW" -> Color(0xFFD4AC0D)
        "DARK_OLED", "MONOCHROME_BW" -> Color(0xFF2C2C2C)
        else -> Color(0xFF6366F1)
    }

    val accentRingColors = when (readingTheme) {
        "PAPER_SEPIA" -> listOf(Color(0xFFD4AF37), Color(0xFF8B5E34), Color(0xFFE5C07B), Color(0xFFD4AF37))
        "TERMINAL_GREEN" -> listOf(Color(0xFF00FF66), Color(0xFF00AA44), Color(0xFF88FFAA), Color(0xFF00FF66))
        "CYBERPUNK_NEON" -> listOf(Color(0xFF00F0FF), Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF00F0FF))
        "CINEMA_YELLOW" -> listOf(Color(0xFFFFE600), Color(0xFFFFA000), Color(0xFFFFE600))
        "DARK_OLED", "MONOCHROME_BW" -> listOf(Color(0xFFCCCCCC), Color(0xFF666666), Color(0xFFFFFFFF), Color(0xFFCCCCCC))
        else -> listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF6366F1))
    }

    val ringGradient = Brush.sweepGradient(colors = accentRingColors)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Badge superior
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = primaryActionColor.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryActionColor.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = authorNameColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "COLEÇÃO EXCLUSIVA DE AUTOR",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = authorNameColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Foto do Autor ampliada (size 200.dp) com moldura temática e anel degradê luminoso
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .border(4.dp, ringGradient, CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onOpenCollection(author) },
                contentAlignment = Alignment.Center
            ) {
                if (!author.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(author.photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto de ${author.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = authorNameColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nome do Autor
            Text(
                text = author.name,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = authorNameColor,
                textAlign = TextAlign.Center
            )

            // Bio / Descrição profissional gerada pela OpenAI
            if (author.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = author.bio,
                        fontSize = 12.5.sp,
                        color = authorBioColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.5.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contador de Livros
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryActionColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = when (readingTheme) {
                            "PAPER_SEPIA" -> Color(0xFFD4AF37)
                            "TERMINAL_GREEN" -> Color(0xFF00FF66)
                            "CINEMA_YELLOW" -> Color(0xFFFFE600)
                            else -> Color(0xFFFBBF24)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (authorBooks.size == 1) "1 Livro Cadastrado" else "${authorBooks.size} Livros Cadastrados",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = authorNameColor
                    )
                }
            }

            // Prévia rápida dos Livros da Coleção
            if (authorBooks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(authorBooks) { b ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onOpenCollection(author) }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = authorNameColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${b.totalPages} págs",
                                        fontSize = 9.sp,
                                        color = authorBioColor.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = b.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = authorNameColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botão Principal: ENTRAR NA COLEÇÃO DO AUTOR
            Button(
                onClick = { onOpenCollection(author) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryActionColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTRAR NA COLEÇÃO (${authorBooks.size}) ▶",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botões de Gerenciamento da Coleção (+ Livros / Editar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onManageBooks(author) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = authorNameColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = authorNameColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Livros",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { onEditAuthor(author) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = authorNameColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = authorNameColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Editar Autor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
