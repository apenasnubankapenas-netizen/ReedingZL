package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AuthorCollection
import com.example.data.model.PdfBook
import com.example.data.model.SubjectDiscipline
import com.example.data.pdf.PdfRendererHelper
import com.example.ui.viewmodel.MainViewModel.MultiPdfBookItem
import kotlinx.coroutines.launch

@Composable
fun ManageSubjectContentDialog(
    subject: SubjectDiscipline,
    allAuthors: List<AuthorCollection>,
    allBooks: List<PdfBook>,
    onDismiss: () -> Unit,
    onSaveAssociations: (authorIds: List<Int>, directBookIds: List<Int>) -> Unit,
    onAddDirectBooks: (subjectId: Int, List<MultiPdfBookItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Autores, 1 = Livros Diretos

    val selectedAuthorIds = remember {
        mutableStateListOf<Int>().apply { addAll(subject.getAuthorIds()) }
    }

    val selectedDirectBookIds = remember {
        mutableStateListOf<Int>().apply { addAll(subject.getDirectBookIds()) }
    }

    // Direct PDF importing state
    var isImportingDirectBooks by remember { mutableStateOf(false) }
    var importDirectProgress by remember { mutableStateOf<String?>(null) }

    val directBooksPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            coroutineScope.launch {
                isImportingDirectBooks = true
                val total = uris.size
                val newItems = mutableListOf<MultiPdfBookItem>()
                for ((index, uri) in uris.withIndex()) {
                    importDirectProgress = "Processando ${index + 1} de $total..."
                    var fileName: String? = null
                    if (uri.scheme == "content") {
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (idx >= 0) fileName = it.getString(idx)
                            }
                        }
                    }
                    if (fileName == null) fileName = uri.lastPathSegment ?: "Livro_${index + 1}.pdf"
                    val isEpub = fileName!!.lowercase().endsWith(".epub") || fileName!!.lowercase().contains("epub")
                    val ext = if (isEpub) ".epub" else ".pdf"
                    val cleanBase = fileName!!.removeSuffix(".pdf").removeSuffix(".PDF").removeSuffix(".epub").removeSuffix(".EPUB").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    val bookTitle = fileName!!.removeSuffix(".pdf").removeSuffix(".PDF").removeSuffix(".epub").removeSuffix(".EPUB").replace("_", " ")
                    val targetName = "subject_book_${System.currentTimeMillis()}_${index}_${cleanBase}${ext}"
                    val savedFile = PdfRendererHelper.copyUriToInternalStorage(context, uri, targetName)
                    val effectiveUri = savedFile?.absolutePath ?: uri.toString()
                    val count = PdfRendererHelper.getPdfPageCount(context, effectiveUri)
                    newItems.add(
                        MultiPdfBookItem(
                            title = bookTitle,
                            author = subject.name,
                            fileUri = effectiveUri,
                            totalPages = count
                        )
                    )
                }
                onAddDirectBooks(subject.id, newItems)
                isImportingDirectBooks = false
                importDirectProgress = null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Gerenciar Conteúdo da Matéria",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = subject.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF93C5FD),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Abas
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .background(Color(0xFF3B82F6))
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Autores Vinculados (${selectedAuthorIds.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Livros Diretos (${selectedDirectBookIds.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // Lista de Autores
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Marque os autores para incluir automaticamente todos os seus respectivos livros nesta matéria.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (allAuthors.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhum autor cadastrado.", color = Color.White.copy(alpha = 0.6f))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(allAuthors) { author ->
                                        val isChecked = selectedAuthorIds.contains(author.id)
                                        val count = allBooks.count { it.authorCollectionId == author.id }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isChecked) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isChecked) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) selectedAuthorIds.remove(author.id)
                                                    else selectedAuthorIds.add(author.id)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) selectedAuthorIds.add(author.id)
                                                        else selectedAuthorIds.remove(author.id)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF3B82F6),
                                                        checkmarkColor = Color.White
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!author.photoUri.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context).data(author.photoUri).crossfade(true).build(),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.People, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = author.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text(text = "$count livros deste autor", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Lista de Livros Diretos + Importação direta
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    directBooksPickerLauncher.launch(
                                        arrayOf("application/pdf", "application/epub+zip", "application/octet-stream", "*/*")
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Importar PDFs Diretamente para esta Matéria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                            }

                            if (isImportingDirectBooks) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.fillMaxWidth())
                                importDirectProgress?.let {
                                    Text(text = it, fontSize = 11.sp, color = Color(0xFF6EE7B7), modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (allBooks.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhum livro cadastrado na biblioteca.", color = Color.White.copy(alpha = 0.6f))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(allBooks) { book ->
                                        val isChecked = selectedDirectBookIds.contains(book.id)

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isChecked) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isChecked) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) selectedDirectBookIds.remove(book.id)
                                                    else selectedDirectBookIds.add(book.id)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) selectedDirectBookIds.add(book.id)
                                                        else selectedDirectBookIds.remove(book.id)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF10B981),
                                                        checkmarkColor = Color.White
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = if (isChecked) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = book.title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(text = book.author.ifBlank { "Sem autor" }, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.8f))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSaveAssociations(selectedAuthorIds.toList(), selectedDirectBookIds.toList())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Salvar Alterações", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
