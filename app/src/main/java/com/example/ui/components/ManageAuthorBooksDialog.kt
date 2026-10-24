package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AuthorCollection
import com.example.data.model.PdfBook

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.pdf.PdfRendererHelper
import com.example.ui.viewmodel.MainViewModel.MultiPdfBookItem
import kotlinx.coroutines.launch

@Composable
fun ManageAuthorBooksDialog(
    author: AuthorCollection,
    allBooks: List<PdfBook>,
    onDismiss: () -> Unit,
    onSaveBooks: (List<Int>) -> Unit,
    onAddDirectBooks: ((List<MultiPdfBookItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isImportingDirectBooks by remember { mutableStateOf(false) }
    var importDirectProgress by remember { mutableStateOf<String?>(null) }
    val directAddedCount = remember { mutableStateOf(0) }

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
                    val prefix = if (isEpub) "epub_" else "pdf_"
                    val cleanBase = fileName!!.removeSuffix(".pdf").removeSuffix(".PDF").removeSuffix(".epub").removeSuffix(".EPUB").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    val bookTitle = fileName!!.removeSuffix(".pdf").removeSuffix(".PDF").removeSuffix(".epub").removeSuffix(".EPUB").replace("_", " ")
                    val targetName = "${prefix}${System.currentTimeMillis()}_${index}_${cleanBase}${ext}"
                    val savedFile = PdfRendererHelper.copyUriToInternalStorage(context, uri, targetName)
                    val effectiveUri = savedFile?.absolutePath ?: uri.toString()
                    val count = PdfRendererHelper.getPdfPageCount(context, effectiveUri)
                    newItems.add(
                        MultiPdfBookItem(
                            title = bookTitle,
                            author = author.name,
                            fileUri = effectiveUri,
                            totalPages = count,
                            authorCollectionId = author.id
                        )
                    )
                }
                directAddedCount.value += newItems.size
                onAddDirectBooks?.invoke(newItems)
                isImportingDirectBooks = false
                importDirectProgress = null
            }
        }
    }

    val selectedBookIds = remember {
        mutableStateListOf<Int>().apply {
            addAll(allBooks.filter { it.authorCollectionId == author.id }.map { it.id })
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
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!author.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(author.photoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Livros de ${author.name}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${selectedBookIds.size} de ${allBooks.size} livros selecionados",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Selecione os livros que devem pertencer a esta coleção. Os livros não selecionados continuam normalmente fora, na biblioteca geral.",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Botão de Adição Direta de PDFs e EPUBs (quantos quiser de uma vez)
                OutlinedButton(
                    onClick = {
                        directBooksPickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "*/*"))
                    },
                    enabled = !isImportingDirectBooks,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF818CF8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isImportingDirectBooks) (importDirectProgress ?: "Processando...") else "+ Adicionar PDFs / EPUBs Direto deste Autor",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isImportingDirectBooks) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (directAddedCount.value > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ ${directAddedCount.value} livro(s) importado(s) direto para ${author.name}!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4ADE80)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allBooks) { book ->
                        val isSelected = selectedBookIds.contains(book.id)
                        val isOtherAuthor = book.authorCollectionId != null && book.authorCollectionId != author.id

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF6366F1) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedBookIds.remove(book.id)
                                    else selectedBookIds.add(book.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedBookIds.add(book.id)
                                        else selectedBookIds.remove(book.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF6366F1),
                                        uncheckedColor = Color.White.copy(alpha = 0.4f),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                    if (isOtherAuthor) {
                                        Text(
                                            text = "Em outra coleção (será transferido para cá)",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFBBF24)
                                        )
                                    } else {
                                        Text(
                                            text = "${book.totalPages} páginas • ${book.author}",
                                            fontSize = 10.5.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancelar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveBooks(selectedBookIds.toList())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar Livros da Coleção", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
