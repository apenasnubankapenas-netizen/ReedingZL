package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.PdfBook
import com.example.data.model.Project
import com.example.data.model.BookCategoryTaxonomy
import com.example.ui.components.FolderDialog
import com.example.ui.components.NewPdfBookDialog
import com.example.ui.components.OpenAiTranslatorDialog
import com.example.ui.components.PlanaltoLegislationDialog
import androidx.compose.material.icons.filled.Gavel
import com.example.ui.components.ProjectCoverIcon
import com.example.ui.components.ProjectIconPickerDialog
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ViewCarousel
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import com.example.data.pdf.PdfRendererHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.StudyStatsDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    projects: List<Project>,
    folders: List<Folder>,
    pdfBooks: List<PdfBook> = emptyList(),
    selectedFolderId: Int?,
    searchQuery: String,
    isDarkMode: Boolean,
    onSelectFolder: (Int?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenProject: (Project) -> Unit,
    onOpenPdfBook: (PdfBook, startInReels: Boolean) -> Unit = { _, _ -> },
    onCreateProject: (title: String, folderId: Int?, mediaType: String, mediaUri: String?, srtText: String, secondarySrtText: String?, customIconUri: String?) -> Unit,
    onCreatePdfBook: (title: String, author: String, fileUri: String?, totalPages: Int, sampleJson: String?) -> Unit = { _, _, _, _, _ -> },
    onCreateMultiplePdfBooks: (List<com.example.ui.viewmodel.MainViewModel.MultiPdfBookItem>) -> Unit = {},
    authors: List<com.example.data.model.AuthorCollection> = emptyList(),
    onRenameProject: (Project, String) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onDeletePdfBook: (PdfBook) -> Unit = {},
    onRenamePdfBook: (PdfBook, String) -> Unit = { _, _ -> },
    onUpdatePdfBookCover: (PdfBook, String) -> Unit = { _, _ -> },
    onUpdateProjectIcon: (Project, String?) -> Unit,
    onCreateFolder: (name: String, colorHex: String) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    onRestoreBackup: ((List<Project>, List<Folder>) -> Unit)? = null,
    onSaveQuestions: ((List<com.example.data.model.BookQuestion>) -> Unit)? = null,
    onSaveDictionaryTerms: ((List<com.example.data.model.PageDictionaryTerm>) -> Unit)? = null
) {
    var selectedMainTab by remember { mutableStateOf(1) } // 0: Legendas & Áudio, 1: Livros & PDFs
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var renameNewTitle by remember { mutableStateOf("") }
    var bookToRename by remember { mutableStateOf<PdfBook?>(null) }
    var renameBookNewTitle by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var projectForIconPicker by remember { mutableStateOf<Project?>(null) }
    var showNewPdfDialog by remember { mutableStateOf(false) }
    var showLegislationDialog by remember { mutableStateOf(false) }
    var showOpenAiDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var pdfBookToDelete by remember { mutableStateOf<PdfBook?>(null) }

    // Phase-by-phase back handling: dismiss dialogs, clear filters
    BackHandler(
        enabled = showNewProjectDialog || showNewPdfDialog || showLegislationDialog || showCreateFolderDialog ||
                showOpenAiDialog || showStatsDialog || showBackupDialog ||
                projectToRename != null || bookToRename != null || projectToDelete != null || projectForIconPicker != null ||
                pdfBookToDelete != null || selectedFolderId != null || searchQuery.isNotBlank()
    ) {
        when {
            showNewProjectDialog -> showNewProjectDialog = false
            showNewPdfDialog -> showNewPdfDialog = false
            showLegislationDialog -> showLegislationDialog = false
            showCreateFolderDialog -> showCreateFolderDialog = false
            showOpenAiDialog -> showOpenAiDialog = false
            showStatsDialog -> showStatsDialog = false
            showBackupDialog -> showBackupDialog = false
            projectToRename != null -> projectToRename = null
            bookToRename != null -> bookToRename = null
            projectToDelete != null -> projectToDelete = null
            projectForIconPicker != null -> projectForIconPicker = null
            pdfBookToDelete != null -> pdfBookToDelete = null
            selectedFolderId != null -> onSelectFolder(null)
            searchQuery.isNotBlank() -> onSearchQueryChange("")
        }
    }

    val filteredProjects = remember(projects, selectedFolderId, searchQuery) {
        projects.filter { proj ->
            val matchesFolder = selectedFolderId == null || proj.folderId == selectedFolderId
            val matchesQuery = searchQuery.isBlank() || proj.title.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }

    val filteredPdfBooks = pdfBooks.filter { book ->
        searchQuery.isBlank() || book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedMainTab == 0) Icons.Default.Subtitles else Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "JESUS APP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (selectedMainTab == 0) "${projects.size}" else "${pdfBooks.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (selectedMainTab == 0) "Sincronizador & Editor SRT" else "Biblioteca & Modo Reels",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLegislationDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Portal de Legislação",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showStatsDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = "Estatísticas da Biblioteca",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup e Restauração",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showOpenAiDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Tradutor IA OpenAI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Modo Escuro / Claro",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedMainTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showNewProjectDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Novo Projeto SRT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showNewPdfDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Importar Livro PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // MAIN TABS: AUDIO & LEGENDAS vs LIVROS & PDFS
            TabRow(
                selectedTabIndex = selectedMainTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = { selectedMainTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Legendas & Áudio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${projects.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = { selectedMainTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Livros & PDFs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${pdfBooks.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (selectedMainTab == 0) {
                // FOLDERS HORIZONTAL BAR
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = { onSelectFolder(null) },
                            label = { Text("Todas (${projects.size})", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                    items(folders, key = { it.id }) { folder ->
                        val folderColor = try {
                            Color(android.graphics.Color.parseColor(folder.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        val folderProjectCount = projects.count { it.folderId == folder.id }
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = {
                                onSelectFolder(if (selectedFolderId == folder.id) null else folder.id)
                            },
                            label = {
                                Text("${folder.name} ($folderProjectCount)", fontSize = 12.sp)
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(folderColor)
                                )
                            }
                        )
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { showCreateFolderDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Pasta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // SEARCH BAR FOR PROJECTS
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Buscar projetos de legendas...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Delete, contentDescription = "Limpar busca", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                // PROJECTS LIST
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (filteredProjects.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank() || selectedFolderId != null)
                                            "Nenhum projeto encontrado no filtro"
                                        else
                                            "Nenhum projeto de legenda cadastrado",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Crie ou importe um arquivo SRT e áudio para começar a sincronizar.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showNewProjectDialog = true }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Criar Projeto SRT")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredProjects, key = { it.id }) { project ->
                            val folder = folders.firstOrNull { it.id == project.folderId }
                            ProjectItemCard(
                                project = project,
                                folder = folder,
                                onOpen = { onOpenProject(project) },
                                onRename = {
                                    projectToRename = project
                                    renameNewTitle = project.title
                                },
                                onChangeIcon = { projectForIconPicker = project },
                                onRemoveIcon = { onUpdateProjectIcon(project, null) },
                                onDelete = { projectToDelete = project }
                            )
                        }
                    }
                }
            } else {
                // COMPACT SEARCH BAR FOR PDF BOOKS
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Buscar livro ou autor...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Delete, contentDescription = "Limpar busca", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                // PDF BOOKS & REELS LIST
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // HERO CARD: REELS LAUNCHER
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF181528)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8E2DE2).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val firstBook = filteredPdfBooks.firstOrNull() ?: pdfBooks.firstOrNull()
                                    if (firstBook != null) {
                                        onOpenPdfBook(firstBook, true)
                                    } else {
                                        showNewPdfDialog = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF8E2DE2)
                                        ) {
                                            Text(
                                                text = "NOVO",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Modo Reels de Leitura",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Navegue verticalmente como em vídeos curtos, com voz neural de alta definição e tema Preto e Branco E-Ink.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF8E2DE2),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ViewCarousel,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Title
                    // Section Title: Livros & PDFs (Minimalist & Compact Controls)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Livros & PDFs (${filteredPdfBooks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botão Legislação Planalto (Minimalista & Compacto)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD97706).copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                                    modifier = Modifier.clickable { showLegislationDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = "Legislação Planalto",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Leis",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFCD34D)
                                        )
                                    }
                                }

                                // Botão + Novo PDF (Minimalista & Proporcional)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.clickable { showNewPdfDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "Adicionar PDF",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "+ PDF",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredPdfBooks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Nenhum livro PDF cadastrado",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Importe um PDF para ler com voz neural e modo Reels.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredPdfBooks, key = { it.id }) { book ->
                            PdfBookItemCard(
                                book = book,
                                onRead = { onOpenPdfBook(book, true) },
                                onOpenReels = { onOpenPdfBook(book, true) },
                                onDelete = { pdfBookToDelete = book },
                                onRenameClick = {
                                    bookToRename = book
                                    renameBookNewTitle = book.title
                                },
                                onUpdateCover = { coverUri ->
                                    onUpdatePdfBookCover(book, coverUri)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showNewProjectDialog) {
        NewProjectDialog(
            folders = folders,
            defaultFolderId = selectedFolderId,
            onDismiss = { showNewProjectDialog = false },
            onCreateProject = { title, fId, mediaType, mediaUri, srtText, secondarySrtText, customIconUri ->
                onCreateProject(title, fId, mediaType, mediaUri, srtText, secondarySrtText, customIconUri)
                showNewProjectDialog = false
            }
        )
    }

    if (showCreateFolderDialog) {
        FolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreateFolder = { name, colorHex ->
                onCreateFolder(name, colorHex)
                showCreateFolderDialog = false
            }
        )
    }

    if (projectToRename != null) {
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Renomear Projeto") },
            text = {
                OutlinedTextField(
                    value = renameNewTitle,
                    onValueChange = { renameNewTitle = it },
                    label = { Text("Nome do Projeto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectToRename?.let { p ->
                            if (renameNewTitle.isNotBlank()) {
                                onRenameProject(p, renameNewTitle.trim())
                            }
                        }
                        projectToRename = null
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Excluir Projeto") },
            text = { Text("Deseja realmente excluir o projeto \"${projectToDelete?.title}\"? Esta ação removerá as legendas sincronizadas.") },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { onDeleteProject(it) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (projectForIconPicker != null) {
        ProjectIconPickerDialog(
            currentIconUri = projectForIconPicker?.customIconUri,
            projectTitle = projectForIconPicker?.title ?: "",
            mediaType = projectForIconPicker?.mediaType ?: "AUDIO",
            onDismiss = { projectForIconPicker = null },
            onSaveIcon = { iconUri ->
                projectForIconPicker?.let { p ->
                    onUpdateProjectIcon(p, iconUri)
                }
                projectForIconPicker = null
            }
        )
    }

    // Dialogs
    if (showStatsDialog) {
        StudyStatsDialog(
            projects = projects,
            folders = folders,
            onDismiss = { showStatsDialog = false }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            projects = projects,
            folders = folders,
            onRestoreBackup = { importedProjects, importedFolders ->
                onRestoreBackup?.invoke(importedProjects, importedFolders)
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showOpenAiDialog) {
        OpenAiTranslatorDialog(
            currentProjectSrt = null,
            currentProjectTitle = null,
            onDismiss = { showOpenAiDialog = false },
            onAttachAsSecondary = { },
            onCreateNewProject = { translatedTitle, translatedSrt ->
                onCreateProject(
                    translatedTitle,
                    selectedFolderId,
                    "AUDIO",
                    null,
                    translatedSrt,
                    null,
                    null
                )
            }
        )
    }

    if (showNewPdfDialog) {
        NewPdfBookDialog(
            authors = authors,
            onDismiss = { showNewPdfDialog = false },
            onCreateBook = { title, author, fileUri, totalPages, sampleJson ->
                onCreatePdfBook(title, author, fileUri, totalPages, sampleJson)
                showNewPdfDialog = false
            },
            onCreateMultipleBooks = { items ->
                onCreateMultiplePdfBooks(items)
                showNewPdfDialog = false
            }
        )
    }

    if (showLegislationDialog) {
        PlanaltoLegislationDialog(
            savedLawBooks = pdfBooks,
            onSaveQuestions = onSaveQuestions,
            onSaveDictionaryTerms = onSaveDictionaryTerms,
            onOpenBook = { onOpenPdfBook(it, true) },
            onCreateAndOpenLawBook = { title, author, sampleJson, totalPages ->
                showLegislationDialog = false
                onCreatePdfBook(title, author, null, totalPages, sampleJson)
            },
            onDismiss = { showLegislationDialog = false }
        )
    }

    if (bookToRename != null) {
        AlertDialog(
            onDismissRequest = { bookToRename = null },
            title = { Text("Alterar Nome do Livro") },
            text = {
                OutlinedTextField(
                    value = renameBookNewTitle,
                    onValueChange = { renameBookNewTitle = it },
                    label = { Text("Nome do Livro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameBookNewTitle.trim()
                        if (trimmed.isNotBlank()) {
                            bookToRename?.let { b ->
                                onRenamePdfBook(b, trimmed)
                            }
                        }
                        bookToRename = null
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pdfBookToDelete != null) {
        AlertDialog(
            onDismissRequest = { pdfBookToDelete = null },
            title = { Text("Excluir Livro") },
            text = { Text("Tem certeza de que deseja excluir o livro \"${pdfBookToDelete?.title}\"? Esta ação é irreversível.") },
            confirmButton = {
                Button(
                    onClick = {
                        pdfBookToDelete?.let { onDeletePdfBook(it) }
                        pdfBookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sim, excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfBookToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PdfBookItemCard(
    book: PdfBook,
    onRead: () -> Unit,
    onOpenReels: () -> Unit,
    onDelete: () -> Unit,
    onRenameClick: () -> Unit = {},
    onUpdateCover: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val progress = if (book.totalPages > 0) book.currentPage.toFloat() / book.totalPages.toFloat() else 0f
    var coverBitmap by remember(book.fileUri) { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onUpdateCover(uri.toString())
        }
    }

    val hasCover = coverBitmap != null || !book.coverPath.isNullOrBlank()

    LaunchedEffect(book.fileUri) {
        if (!book.fileUri.isNullOrBlank()) {
            try {
                val bmp = withContext(Dispatchers.IO) {
                    PdfRendererHelper.renderPdfPageBitmap(
                        context = context,
                        fileUri = book.fileUri,
                        pageIndexZeroBased = 0,
                        destWidth = 400,
                        readingTheme = "MONOCHROME_BW"
                    )
                }
                coverBitmap = bmp
            } catch (_: Exception) {}
        }
    }

    OutlinedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRead() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Book Cover Graphic / Thumbnail with Red 'X' and Gallery Picker when missing
                Box(
                    modifier = Modifier
                        .width(66.dp)
                        .height(88.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1E1E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (!hasCover) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            }
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
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Quando não aparece capa, possibilidade de adicionar a capa através de uma imagem da galeria
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Adicionar capa da galeria",
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+ Capa",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA)
                                )
                            }
                        }
                    }

                    // X vermelho pequeno no canto superior de cada capa para excluir com confirmação prévia
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDC2626),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .clickable { onDelete() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Excluir Livro",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Clicando em cima do nome do livro, permite alterar para o nome que quiser
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRenameClick() }
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Alterar nome do livro",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${book.author} • Página ${book.currentPage} de ${book.totalPages}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!book.category.isNullOrBlank()) {
                        val catPalette = BookCategoryTaxonomy.getPaletteForCategory(book.category, book.categoryColor)
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catPalette.accentColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, catPalette.accentColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = catPalette.accentColor,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = book.category!!,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = catPalette.accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!book.subcategory.isNullOrBlank()) {
                                    Text(
                                        text = " • ${book.subcategory}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Reading progress bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Ler, Reels, Excluir)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Open Book in standard P&B Reader
                    Button(
                        onClick = onRead,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Open Book in Reels Vertical Swipe Mode
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8E2DE2).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8E2DE2)),
                        modifier = Modifier.clickable { onOpenReels() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewCarousel,
                                contentDescription = null,
                                tint = Color(0xFF8E2DE2),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reels",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E2DE2)
                            )
                        }
                    }

                    // Open Book with Idiomas shortcut
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA)),
                        modifier = Modifier.clickable { onOpenReels() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Idiomas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Livro",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
fun ProjectItemCard(
    project: Project,
    folder: Folder?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onChangeIcon: () -> Unit,
    onRemoveIcon: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(project.lastModified))
    var showMenu by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Custom Project Cover / Icon
            ProjectCoverIcon(
                customIconUri = project.customIconUri,
                mediaType = project.mediaType,
                size = 46.dp,
                shape = RoundedCornerShape(10.dp),
                showEditBadge = false,
                onClick = onChangeIcon
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (folder != null) {
                        val color = try {
                            Color(android.graphics.Color.parseColor(folder.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = folder.name,
                                color = color,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!project.secondarySrtContent.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "2ª Legenda",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    if (project.timeOffsetMs != 0L) {
                        val sign = if (project.timeOffsetMs > 0) "+" else ""
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$sign${project.timeOffsetMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = "• $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Play / Action & Menu
            IconButton(
                onClick = onOpen,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Abrir Sincronizador",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opções do Projeto",
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Alterar Imagem / Capa") },
                        onClick = {
                            showMenu = false
                            onChangeIcon()
                        },
                        leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) }
                    )
                    if (!project.customIconUri.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text("Remover Imagem") },
                            onClick = {
                                showMenu = false
                                onRemoveIcon()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Renomear") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir Projeto", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Arquivo"
}

fun readSrtFromUri(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val maxBytes = 5 * 1024 * 1024 // 5 MB max for subtitle tracks
            val buffer = ByteArray(maxBytes)
            var totalRead = 0
            while (totalRead < maxBytes) {
                val read = stream.read(buffer, totalRead, maxBytes - totalRead)
                if (read == -1) break
                totalRead += read
            }
            if (totalRead <= 0) return@use ""
            val bytes = if (totalRead == buffer.size) buffer else buffer.copyOf(totalRead)
            var text = String(bytes, Charsets.UTF_8)
            if (!text.contains("-->")) {
                text = String(bytes, Charsets.ISO_8859_1)
            }
            if (!text.contains("-->")) {
                text = String(bytes, Charsets.UTF_16)
            }
            text
        } ?: ""
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

@Composable
fun NewProjectDialog(
    folders: List<Folder>,
    defaultFolderId: Int?,
    onDismiss: () -> Unit,
    onCreateProject: (title: String, folderId: Int?, mediaType: String, mediaUri: String?, srtText: String, secondarySrtText: String?, customIconUri: String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf(defaultFolderId) }
    var mediaType by remember { mutableStateOf("AUDIO") }
    var mediaUriString by remember { mutableStateOf<String?>(null) }
    var mediaFileName by remember { mutableStateOf<String?>(null) }
    var srtFileName by remember { mutableStateOf<String?>(null) }
    var secondarySrtFileName by remember { mutableStateOf<String?>(null) }
    var srtText by remember { mutableStateOf("") }
    var secondarySrtText by remember { mutableStateOf("") }
    var customIconUriString by remember { mutableStateOf<String?>(null) }

    // Launcher for Custom Project Icon / Image
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            customIconUriString = uri.toString()
        }
    }

    // Launcher for Audio/Video Media file
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaUriString = uri.toString()
            val name = getFileNameFromUri(context, uri)
            mediaFileName = name

            if (name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".mkv", ignoreCase = true) || name.endsWith(".avi", ignoreCase = true)) {
                mediaType = "VIDEO"
            } else {
                mediaType = "AUDIO"
            }

            if (title.isBlank()) {
                title = name.substringBeforeLast(".")
            }
        }
    }

    // Launcher for Primary SRT Subtitle file
    val srtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val name = getFileNameFromUri(context, uri)
            srtFileName = name
            val content = readSrtFromUri(context, uri)
            if (content.isNotBlank()) {
                srtText = content
            }
            if (title.isBlank()) {
                title = name.substringBeforeLast(".")
            }
        }
    }

    // Launcher for Secondary SRT Subtitle file
    val secondarySrtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val name = getFileNameFromUri(context, uri)
            secondarySrtFileName = name
            val content = readSrtFromUri(context, uri)
            if (content.isNotBlank()) {
                secondarySrtText = content
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Projeto de Sincronização", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título do Projeto / Áudio") },
                    placeholder = { Text("ex: Aula de Inglês - Áudio & Legenda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. SELECT AUDIO / VIDEO FILE FROM TELEPHONE
                Text(
                    text = "1. Selecionar Áudio / Vídeo do Celular:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedCard(
                    onClick = {
                        mediaPickerLauncher.launch(arrayOf("audio/*", "video/*", "*/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (mediaFileName != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (mediaFileName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (mediaFileName != null) Icons.Default.CheckCircle else Icons.Default.AudioFile,
                                    contentDescription = null,
                                    tint = if (mediaFileName != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaFileName ?: "Escolher Arquivo de Áudio/Vídeo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (mediaFileName != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (mediaFileName != null) "Mídia selecionada do telefone" else "Toque para abrir os arquivos do seu celular",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Navegar Arquivos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. SELECT PRIMARY SRT SUBTITLE FILE
                Text(
                    text = "2. Selecionar Legenda Principal (.SRT):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedCard(
                    onClick = {
                        srtPickerLauncher.launch(arrayOf("*/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (srtFileName != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (srtFileName != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (srtFileName != null) Icons.Default.CheckCircle else Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = if (srtFileName != null) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = srtFileName ?: "Escolher Arquivo .SRT Principal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (srtFileName != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (srtFileName != null) "Legenda SRT principal carregada!" else "Toque para buscar seu arquivo de legenda .srt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Navegar Legendas",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. SELECT SECONDARY SRT SUBTITLE FILE (OPTIONAL DUAL SUBTITLE)
                Text(
                    text = "3. Selecionar 2ª Legenda / Tradução (Opcional):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedCard(
                    onClick = {
                        secondarySrtPickerLauncher.launch(arrayOf("*/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (secondarySrtFileName != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (secondarySrtFileName != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (secondarySrtFileName != null) Icons.Default.CheckCircle else Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = if (secondarySrtFileName != null) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = secondarySrtFileName ?: "Escolher 2ª Legenda / Tradução (.SRT)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (secondarySrtFileName != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (secondarySrtFileName != null) "2ª Legenda carregada para rodar abaixo da 1ª!" else "Opcional: Rodará sincronizada logo abaixo da legenda 1",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Navegar 2ª Legenda",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. SELECT CUSTOM PROJECT ICON / IMAGE FROM TELEPHONE
                Text(
                    text = "4. Ícone / Capa do Projeto (Escolher Foto do Celular):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedCard(
                    onClick = {
                        iconPickerLauncher.launch(arrayOf("image/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (customIconUriString != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProjectCoverIcon(
                            customIconUri = customIconUriString,
                            mediaType = mediaType,
                            size = 44.dp,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (customIconUriString != null) "✓ Imagem Personalizada Selecionada" else "Escolher Imagem / Foto do Celular",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (customIconUriString != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (customIconUriString != null) "Toque para trocar a imagem" else "Dê uma capa moderna e visual para seu projeto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (customIconUriString != null) {
                            IconButton(
                                onClick = { customIconUriString = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remover Imagem",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Selecionar Foto",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional text field for main SRT
                Text("Conteúdo da Legenda Principal (SRT):", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = srtText,
                    onValueChange = { srtText = it },
                    placeholder = {
                        Text("Cole o texto da legenda ou selecione um arquivo .srt acima...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateProject(
                        title,
                        selectedFolderId,
                        mediaType,
                        mediaUriString,
                        srtText,
                        secondarySrtText.ifBlank { null },
                        customIconUriString
                    )
                }
            ) {
                Text("Criar e Sincronizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
