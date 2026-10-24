package com.example.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.pdf.PdfRendererHelper
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.model.AuthorCollection
import com.example.ui.viewmodel.MainViewModel.MultiPdfBookItem

@Composable
fun NewPdfBookDialog(
    authors: List<AuthorCollection> = emptyList(),
    initialAuthorId: Int? = null,
    onDismiss: () -> Unit,
    onCreateBook: (title: String, author: String, fileUri: String?, totalPages: Int, sampleJson: String?) -> Unit,
    onCreateMultipleBooks: ((List<MultiPdfBookItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAuthorId by remember { mutableStateOf(initialAuthorId) }
    val selectedAuthor = remember(selectedAuthorId, authors) {
        authors.find { it.id == selectedAuthorId }
    }

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf(selectedAuthor?.name ?: "") }
    var selectedPdfUri by remember { mutableStateOf<String?>(null) }
    var pdfFileName by remember { mutableStateOf<String?>(null) }
    var pageCount by remember { mutableStateOf(1) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Multi-PDF State
    var multiPdfItems by remember { mutableStateOf<List<MultiPdfBookItem>>(emptyList()) }
    var multiPdfProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) } // current, total

    val multiPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            if (uris.size == 1) {
                // Single file mode (PDF or EPUB)
                val uri = uris.first()
                val name = getFileName(context, uri) ?: "Documento.pdf"
                val isEpub = name.lowercase().endsWith(".epub") || name.lowercase().contains("epub")
                pdfFileName = name
                if (title.isBlank()) {
                    title = name.removeSuffix(".pdf").removeSuffix(".PDF")
                        .removeSuffix(".epub").removeSuffix(".EPUB")
                        .replace("_", " ")
                }
                multiPdfItems = emptyList()
                scope.launch {
                    isAnalyzing = true
                    val ext = if (isEpub) ".epub" else ".pdf"
                    val prefix = if (isEpub) "epub_" else "pdf_"
                    val cleanBase = name.removeSuffix(".pdf").removeSuffix(".PDF").removeSuffix(".epub").removeSuffix(".EPUB").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    val targetName = "${prefix}${System.currentTimeMillis()}_${cleanBase}${ext}"
                    val savedFile = PdfRendererHelper.copyUriToInternalStorage(context, uri, targetName)
                    val effectiveUri = savedFile?.absolutePath ?: uri.toString()
                    selectedPdfUri = effectiveUri
                    val count = PdfRendererHelper.getPdfPageCount(context, effectiveUri)
                    pageCount = count
                    isAnalyzing = false
                }
            } else {
                // Multiple files mode (PDFs and EPUBs)
                selectedPdfUri = null
                pdfFileName = null
                multiPdfItems = emptyList()
                scope.launch {
                    isAnalyzing = true
                    val total = uris.size
                    val items = mutableListOf<MultiPdfBookItem>()
                    for ((index, uri) in uris.withIndex()) {
                        multiPdfProgress = Pair(index + 1, total)
                        val name = getFileName(context, uri) ?: "Livro_${index + 1}.pdf"
                        val isEpub = name.lowercase().endsWith(".epub") || name.lowercase().contains("epub")
                        val bookTitle = name.removeSuffix(".pdf").removeSuffix(".PDF")
                            .removeSuffix(".epub").removeSuffix(".EPUB")
                            .replace("_", " ")
                        val ext = if (isEpub) ".epub" else ".pdf"
                        val prefix = if (isEpub) "epub_" else "pdf_"
                        val cleanBase = name.removeSuffix(".pdf").removeSuffix(".PDF").removeSuffix(".epub").removeSuffix(".EPUB").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                        val targetName = "${prefix}${System.currentTimeMillis()}_${index}_${cleanBase}${ext}"
                        val savedFile = PdfRendererHelper.copyUriToInternalStorage(context, uri, targetName)
                        val effectiveUri = savedFile?.absolutePath ?: uri.toString()
                        val count = PdfRendererHelper.getPdfPageCount(context, effectiveUri)
                        items.add(
                            MultiPdfBookItem(
                                title = bookTitle,
                                author = selectedAuthor?.name ?: if (author.isNotBlank()) author else "Biblioteca Pessoal",
                                fileUri = effectiveUri,
                                totalPages = count,
                                authorCollectionId = selectedAuthorId
                            )
                        )
                    }
                    multiPdfItems = items
                    multiPdfProgress = null
                    isAnalyzing = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (multiPdfItems.isNotEmpty()) Icons.Default.LibraryBooks else Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (multiPdfItems.isNotEmpty()) "Importar Vários Livros" else "Novo Livro (PDF ou EPUB)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (multiPdfItems.isNotEmpty()) "${multiPdfItems.size} livros (PDF/EPUB) prontos" else "Suporta PDF e EPUB com Voz Neural",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // PDF/EPUB File Selection Button / Card
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isAnalyzing) {
                            multiPdfPickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "*/*"))
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                multiPdfItems.isNotEmpty() -> Icons.Default.LibraryBooks
                                selectedPdfUri != null -> Icons.Default.Description
                                else -> Icons.Default.UploadFile
                            },
                            contentDescription = null,
                            tint = if (selectedPdfUri != null || multiPdfItems.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    multiPdfItems.isNotEmpty() -> "${multiPdfItems.size} arquivos selecionados (PDF/EPUB)"
                                    selectedPdfUri != null -> (pdfFileName ?: "Livro Selecionado")
                                    else -> "Selecionar PDF(s) ou EPUB(s)"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedPdfUri != null || multiPdfItems.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPdfUri != null || multiPdfItems.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    isAnalyzing && multiPdfProgress != null -> "Processando ${multiPdfProgress?.first} de ${multiPdfProgress?.second} livros..."
                                    isAnalyzing -> "Analisando páginas e estrutura..."
                                    multiPdfItems.isNotEmpty() -> "Toque para escolher outros livros"
                                    selectedPdfUri != null -> "$pageCount páginas detectadas • Pronto para leitura"
                                    else -> "Adicione quantos livros quiser de uma só vez (PDF ou EPUB)"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Seletor de Autor opcional se houver autores cadastrados
                if (authors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Vincular a um Autor (opcional):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedAuthorId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedAuthorId = null
                                        if (multiPdfItems.isNotEmpty()) {
                                            multiPdfItems = multiPdfItems.map { it.copy(authorCollectionId = null, author = author.ifBlank { "Biblioteca Pessoal" }) }
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Sem autor específico",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selectedAuthorId == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedAuthorId == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(authors) { aut ->
                            val isSel = selectedAuthorId == aut.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedAuthorId = aut.id
                                        author = aut.name
                                        if (multiPdfItems.isNotEmpty()) {
                                            multiPdfItems = multiPdfItems.map { it.copy(authorCollectionId = aut.id, author = aut.name) }
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = aut.name,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (isAnalyzing) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (multiPdfItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Arquivos que serão adicionados:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(multiPdfItems) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.totalPages} páginas",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do Livro / Artigo") },
                        placeholder = { Text("Ex: Filosofia e Foco, Artigo Científico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Autor / Fonte (opcional)") },
                        placeholder = { Text("Ex: Machado de Assis, Artigo Acadêmico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF8E2DE2),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Renderização fluida em alto contraste, modo Reels e IA integrada para todos os livros importados.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (multiPdfItems.isNotEmpty()) {
                        onCreateMultipleBooks?.invoke(multiPdfItems)
                        onDismiss()
                    } else {
                        val finalTitle = title.ifBlank { pdfFileName?.removeSuffix(".pdf") ?: "Meu Livro" }
                        val finalAuthor = author.ifBlank { "Biblioteca Pessoal" }
                        onCreateBook(
                            finalTitle,
                            finalAuthor,
                            selectedPdfUri,
                            pageCount,
                            null
                        )
                        onDismiss()
                    }
                },
                enabled = !isAnalyzing && (multiPdfItems.isNotEmpty() || title.isNotBlank() || selectedPdfUri != null)
            ) {
                Text(
                    if (multiPdfItems.isNotEmpty()) "Carregar Todos (${multiPdfItems.size})" else "Abrir e Ler"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAnalyzing) {
                Text("Cancelar")
            }
        }
    )
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
