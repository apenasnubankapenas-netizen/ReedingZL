package com.example.ui.components

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.provider.OpenableColumns
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.pdf.PdfRendererHelper
import com.example.ui.viewmodel.MainViewModel.MultiPdfBookItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AuthorCollection
import com.example.data.model.PdfBook
import com.example.data.openai.OpenAiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthorCollectionDialog(
    author: AuthorCollection? = null,
    allBooks: List<PdfBook>,
    onDismiss: () -> Unit,
    onSaveAuthor: (AuthorCollection, List<Int>, List<MultiPdfBookItem>) -> Unit,
    onDeleteAuthor: ((AuthorCollection) -> Unit)? = null,
    onAddDirectBooks: ((List<MultiPdfBookItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEditing = author != null

    var name by remember { mutableStateOf(author?.name ?: "") }
    var bio by remember { mutableStateOf(author?.bio ?: "") }
    var photoUri by remember { mutableStateOf(author?.photoUri) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isGeneratingBio by remember { mutableStateOf(false) }

    var isImportingDirectBooks by remember { mutableStateOf(false) }
    var importDirectProgress by remember { mutableStateOf<String?>(null) }
    val pendingDirectItems = remember { mutableStateListOf<MultiPdfBookItem>() }

    // Selected books for this author's collection
    val selectedBookIds = remember {
        mutableStateListOf<Int>().apply {
            if (author != null) {
                addAll(allBooks.filter { it.authorCollectionId == author.id }.map { it.id })
            }
        }
    }

    // Auto-pull/match books from device when author name is typed or created
    LaunchedEffect(name) {
        val trimmed = name.trim().lowercase()
        if (trimmed.length >= 3) {
            val matchingIds = allBooks.filter { b ->
                b.author.trim().lowercase().contains(trimmed) ||
                trimmed.contains(b.author.trim().lowercase()) ||
                b.title.trim().lowercase().contains(trimmed)
            }.map { it.id }
            matchingIds.forEach { id ->
                if (!selectedBookIds.contains(id)) {
                    selectedBookIds.add(id)
                }
            }
        }
    }

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
                            author = name.ifBlank { "Autor" },
                            fileUri = effectiveUri,
                            totalPages = count,
                            authorCollectionId = author?.id
                        )
                    )
                }
                pendingDirectItems.addAll(newItems)
                if (author != null) {
                    onAddDirectBooks?.invoke(newItems)
                }
                isImportingDirectBooks = false
                importDirectProgress = null
            }
        }
    }

    val apiKey = remember { OpenAiService.getApiKey(context) }

    // Auto-carregamento de biografia via OpenAI quando o usuário digita o nome
    LaunchedEffect(name) {
        val trimmed = name.trim()
        if (trimmed.length >= 4 && bio.isBlank() && !isGeneratingBio) {
            delay(1200) // Debounce confortável enquanto o usuário digita
            if (name.trim() == trimmed && bio.isBlank()) {
                isGeneratingBio = true
                try {
                    val res = OpenAiService.generateAuthorBiography(trimmed, apiKey)
                    res.onSuccess { generatedBio ->
                        if (generatedBio.isNotBlank() && bio.isBlank()) {
                            bio = generatedBio
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthorCollectionDialog", "Erro ao gerar bio automática", e)
                } finally {
                    isGeneratingBio = false
                }
            }
        }
    }

    fun requestGenerateBio() {
        val trimmed = name.trim()
        if (trimmed.isBlank() || isGeneratingBio) return
        isGeneratingBio = true
        coroutineScope.launch {
            try {
                val res = OpenAiService.generateAuthorBiography(trimmed, apiKey)
                res.onSuccess { generatedBio ->
                    if (generatedBio.isNotBlank()) {
                        bio = generatedBio
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthorCollectionDialog", "Erro ao gerar bio sob demanda", e)
            } finally {
                isGeneratingBio = false
            }
        }
    }

    // Android Photo Picker Launcher (Zero-permission photo selection)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    val primaryGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEditing) "Editar Autor & Coleção" else "Nova Coleção de Autor",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Agrupe livros deste autor para visualização exclusiva",
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

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Author Photo Section (Tamanho ampliado conforme solicitado)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.5.dp, Color(0xFF6366F1), CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!photoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(photoUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Foto do Autor",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Adicionar Foto",
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = Color(0xFFA5B4FC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (photoUri != null) "Trocar Foto da Galeria" else "Escolher Foto da Galeria",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFA5B4FC)
                                        )
                                    }
                                }

                                if (photoUri != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        modifier = Modifier.clickable { photoUri = null }
                                    ) {
                                        Text(
                                            text = "Remover",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFCA5A5),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Author Name Input com trigger de Biografia OpenAI
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nome do Autor *",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                if (isGeneratingBio) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp,
                                            color = Color(0xFF10A37F)
                                        )
                                        Text(
                                            text = "OpenAI gerando bio...",
                                            fontSize = 10.sp,
                                            color = Color(0xFF10A37F),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Ex: Machado de Assis, Clarice Lispector...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }

                    // Author Bio / Description (Gerada automaticamente com OpenAI para apresentação profissional)
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Biografia Profissional (OpenAI)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                // Botão explícito para gerar ou refazer biografia via OpenAI
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10A37F).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10A37F).copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable(enabled = name.isNotBlank() && !isGeneratingBio) {
                                        requestGenerateBio()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isGeneratingBio) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.2.dp,
                                                color = Color(0xFF00DC82)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFF00DC82),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = if (bio.isBlank()) "Gerar com OpenAI" else "Refazer Bio com IA",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00DC82)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                placeholder = { Text("A biografia profissional será gerada automaticamente pela OpenAI ao preencher o nome...", fontSize = 12.sp) },
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10A37F),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }

                    // Books Selection for this Author
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Livros nesta Coleção (${selectedBookIds.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Marque para incluir",
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Botão de Adição Direta de PDFs e EPUBs para este autor
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isImportingDirectBooks) {
                                        val trimmed = name.trim().lowercase()
                                        if (trimmed.isNotBlank()) {
                                            allBooks.filter { b ->
                                                b.author.trim().lowercase().contains(trimmed) ||
                                                trimmed.contains(b.author.trim().lowercase()) ||
                                                b.title.trim().lowercase().contains(trimmed)
                                            }.forEach { b ->
                                                if (!selectedBookIds.contains(b.id)) selectedBookIds.add(b.id)
                                            }
                                        }
                                        directBooksPickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "*/*"))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = if (isImportingDirectBooks) (importDirectProgress ?: "Processando...") else "+ Puxar Livros do Telefone deste Autor",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399)
                                            )
                                            Text(
                                                text = "Puxa livros do aparelho e vincula direto à coleção do autor",
                                                fontSize = 9.5.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isImportingDirectBooks) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            if (pendingDirectItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "✓ ${pendingDirectItems.size} livro(s) importado(s) direto para este autor!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4ADE80)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (allBooks.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Nenhum livro cadastrado na biblioteca ainda.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        allBooks.forEach { book ->
                                            val isSelected = selectedBookIds.contains(book.id)
                                            val isAssignedToOther = book.authorCollectionId != null &&
                                                    (author == null || book.authorCollectionId != author.id)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        if (isSelected) {
                                                            selectedBookIds.remove(book.id)
                                                        } else {
                                                            selectedBookIds.add(book.id)
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.MenuBook,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = book.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                                        maxLines = 1
                                                    )
                                                    if (isAssignedToOther) {
                                                        Text(
                                                            text = "(atribuído a outra coleção - ao marcar, será movido)",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFFFBBF24).copy(alpha = 0.9f)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "${book.totalPages} páginas",
                                                            fontSize = 10.sp,
                                                            color = Color.White.copy(alpha = 0.5f)
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

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Confirmation or Action Buttons
                if (showDeleteConfirm && isEditing && onDeleteAuthor != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Excluir coleção de \"${author.name}\"?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Os livros não serão apagados, eles apenas voltarão a ficar avulsos lá fora na biblioteca geral.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showDeleteConfirm = false },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Cancelar", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onDeleteAuthor(author)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                ) {
                                    Text("Sim, Excluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing && onDeleteAuthor != null && !showDeleteConfirm) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Coleção",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cancelar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val newAuthor = (author ?: AuthorCollection(name = name.trim())).copy(
                                        name = name.trim(),
                                        bio = bio.trim(),
                                        photoUri = photoUri
                                    )
                                    onSaveAuthor(newAuthor, selectedBookIds.toList(), pendingDirectItems.toList())
                                    if (author == null && pendingDirectItems.isNotEmpty()) {
                                        onAddDirectBooks?.invoke(pendingDirectItems.map { it.copy(author = name.trim()) })
                                    }
                                    onDismiss()
                                }
                            },
                            enabled = name.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEditing) "Salvar Alterações" else "Criar Coleção",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
