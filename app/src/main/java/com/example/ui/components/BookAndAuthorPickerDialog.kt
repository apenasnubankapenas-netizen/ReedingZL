package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AuthorCollection
import com.example.data.model.BookCategoryTaxonomy
import com.example.data.model.PdfBook
import com.example.data.model.SubjectDiscipline

/**
 * Diálogo para escolher livros e navegar pelos autores e matérias diretamente nos Reels.
 */
@Composable
fun BookAndAuthorPickerDialog(
    books: List<PdfBook>,
    authors: List<AuthorCollection>,
    subjects: List<SubjectDiscipline> = emptyList(),
    onSelectBook: (PdfBook) -> Unit,
    onSelectAuthor: (AuthorCollection) -> Unit,
    onSelectSubject: ((SubjectDiscipline) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Livros, 1 = Autores, 2 = Matérias
    var searchQuery by remember { mutableStateOf("") }

    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books
        else {
            val q = searchQuery.trim().lowercase()
            books.filter {
                it.title.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                (it.category?.lowercase()?.contains(q) == true)
            }
        }
    }

    val filteredAuthors = remember(authors, searchQuery) {
        if (searchQuery.isBlank()) authors
        else {
            val q = searchQuery.trim().lowercase()
            authors.filter {
                it.name.lowercase().contains(q) ||
                it.bio.lowercase().contains(q)
            }
        }
    }

    val filteredSubjects = remember(subjects, searchQuery) {
        if (searchQuery.isBlank()) subjects
        else {
            val q = searchQuery.trim().lowercase()
            subjects.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF13131A),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Escolher Livros & Autores",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Navegue pelo catálogo nos Reels",
                                fontSize = 11.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: [Livros (N)] [Autores (N)]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E28), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 0) Color(0xFF6366F1) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.White else Color(0xFFA1A1AA),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Livros (${books.size})",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color.White else Color(0xFFA1A1AA)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 1) Color(0xFF6366F1) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else Color(0xFFA1A1AA),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Autores (${authors.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color.White else Color(0xFFA1A1AA)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 2) Color(0xFF3B82F6) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 2 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = if (selectedTab == 2) Color.White else Color(0xFFA1A1AA),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Matérias (${subjects.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 2) Color.White else Color(0xFFA1A1AA)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Buscar por título, autor ou tema..."
                                1 -> "Buscar autor ou biografia..."
                                else -> "Buscar matéria ou disciplina..."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF71717A)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E28),
                        unfocusedContainerColor = Color(0xFF1A1A22),
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF2E2E3C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Content List
                if (selectedTab == 0) {
                    // Books List
                    if (filteredBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum livro encontrado.",
                                color = Color(0xFF71717A),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredBooks, key = { it.id }) { book ->
                                val catPalette = BookCategoryTaxonomy.getPaletteForCategory(book.category, book.categoryColor)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E1E28),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E3C)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectBook(book)
                                            onDismissRequest()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Book Icon / Badge
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = catPalette.cardBg,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, catPalette.accentColor.copy(alpha = 0.5f)),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MenuBook,
                                                    contentDescription = null,
                                                    tint = catPalette.accentColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = book.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = book.author,
                                                fontSize = 12.sp,
                                                color = Color(0xFFA1A1AA),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = catPalette.accentColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = book.category ?: "Livro",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = catPalette.accentColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Página ${book.currentPage} de ${book.totalPages}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF71717A)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF6366F1),
                                            modifier = Modifier.clickable {
                                                onSelectBook(book)
                                                onDismissRequest()
                                            }
                                        ) {
                                            Text(
                                                text = "Ler",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // Authors List
                    if (filteredAuthors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum autor encontrado.",
                                color = Color(0xFF71717A),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredAuthors, key = { it.id }) { author ->
                                val authorBooks = books.filter { it.authorCollectionId == author.id }
                                val bannerColor = try {
                                    Color(android.graphics.Color.parseColor(author.bannerColorHex))
                                } catch (_: Exception) {
                                    Color(0xFF6366F1)
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E1E28),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E3C)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectAuthor(author)
                                            onDismissRequest()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Author Avatar
                                        if (!author.photoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = author.photoUri,
                                                contentDescription = author.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, bannerColor, CircleShape)
                                            )
                                        } else {
                                            Surface(
                                                shape = CircleShape,
                                                color = bannerColor.copy(alpha = 0.3f),
                                                border = androidx.compose.foundation.BorderStroke(1.5.dp, bannerColor),
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = author.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            if (author.bio.isNotBlank()) {
                                                Text(
                                                    text = author.bio,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFA1A1AA),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = bannerColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "${authorBooks.size} ${if (authorBooks.size == 1) "livro" else "livros"}",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = bannerColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = bannerColor.copy(alpha = 0.85f),
                                            modifier = Modifier.clickable {
                                                onSelectAuthor(author)
                                                onDismissRequest()
                                            }
                                        ) {
                                            Text(
                                                text = "Ver",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Subjects / Disciplines List
                    if (filteredSubjects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma matéria encontrada.",
                                color = Color(0xFF71717A),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredSubjects, key = { it.id }) { subjectItem ->
                                val subAuthorIds = subjectItem.getAuthorIds()
                                val subDirectBookIds = subjectItem.getDirectBookIds()
                                val subBooks = books.filter { b ->
                                    subDirectBookIds.contains(b.id) || (b.authorCollectionId != null && subAuthorIds.contains(b.authorCollectionId))
                                }
                                val bannerColor = try {
                                    Color(android.graphics.Color.parseColor(subjectItem.bannerColorHex))
                                } catch (_: Exception) {
                                    Color(0xFF1E3A8A)
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E1E28),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectSubject?.invoke(subjectItem)
                                            onDismissRequest()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Subject Cover Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(width = 44.dp, height = 60.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(bannerColor)
                                                .border(1.dp, Color(0xFFFDE047).copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!subjectItem.coverUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = subjectItem.coverUri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = subjectItem.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (subjectItem.description.isNotBlank()) {
                                                Text(
                                                    text = subjectItem.description,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFA1A1AA),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "${subAuthorIds.size} aut.",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF93C5FD),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "${subBooks.size} ${if (subBooks.size == 1) "livro" else "livros"}",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF6EE7B7),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = bannerColor,
                                            modifier = Modifier.clickable {
                                                onSelectSubject?.invoke(subjectItem)
                                                onDismissRequest()
                                            }
                                        ) {
                                            Text(
                                                text = "Abrir",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
