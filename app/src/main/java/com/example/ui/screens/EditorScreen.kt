package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import com.example.data.model.GeminiNote
import com.example.data.model.SubtitleAlignment
import com.example.data.model.SubtitleStylePreset
import com.example.ui.components.GeminiExplanationDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Project
import com.example.data.model.SubtitleCue
import com.example.data.parser.SrtParser
import com.example.ui.components.ExportDialog
import com.example.ui.components.OpenAiTranslatorDialog
import com.example.ui.components.ProjectCoverIcon
import com.example.ui.components.ProjectIconPickerDialog
import com.example.ui.components.RetroAudioPlayerView
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.components.TimingAdjustmentBar

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.components.WordLookupDialog

enum class EditorWorkspaceTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LIST("Legendas", Icons.Default.List),
    PREVIEW("Visualização", Icons.Default.Visibility),
    SYNC("Sincronia", Icons.Default.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    project: Project,
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue> = emptyList(),
    folders: List<Folder>,
    timeOffsetMs: Long,
    fontSizeSp: Float,
    subtitleStylePreset: com.example.data.model.SubtitleStylePreset = com.example.data.model.SubtitleStylePreset.CLASSIC_WHITE,
    subtitleAlignment: com.example.data.model.SubtitleAlignment = com.example.data.model.SubtitleAlignment.CENTER,
    cueSearchQuery: String = "",
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    isSentenceLoop: Boolean = false,
    isAutoPause: Boolean = false,
    starredCueIndices: Set<Int> = emptySet(),
    geminiNotes: List<GeminiNote> = emptyList(),
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onToggleSentenceLoop: () -> Unit = {},
    onToggleAutoPause: () -> Unit = {},
    onToggleStarCue: (Int) -> Unit = {},
    onSaveGeminiNote: (cueIndex: Int, termOrSentence: String, fullContext: String, response: String) -> Unit = { _, _, _, _ -> },
    onDeleteGeminiNote: (GeminiNote) -> Unit = {},
    onDeleteGeminiNoteForCue: (Int) -> Unit = {},
    onSplitCue: (Int, Long) -> Unit = { _, _ -> },
    onMergeCueWithNext: (Int) -> Unit = {},
    onDeleteCue: (Int) -> Unit = {},
    onAddCue: (Long, Long, String) -> Unit = { _, _, _ -> },
    onAdjustOffsetMs: (Long) -> Unit,
    onSetOffsetMs: (Long) -> Unit,
    onSetFontSize: (Float) -> Unit,
    onSetStylePreset: (com.example.data.model.SubtitleStylePreset) -> Unit = {},
    onSetAlignment: (com.example.data.model.SubtitleAlignment) -> Unit = {},
    onSearchCueQueryChange: (String) -> Unit = {},
    onToggleFullscreen: () -> Unit,
    onUpdateCue: (index: Int, text: String, startMs: Long, endMs: Long) -> Unit,
    onMoveToFolder: (Project, Int?) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateProjectIcon: ((String?) -> Unit)? = null,
    onLoadSecondarySrt: (String) -> Unit,
    onCloseProject: () -> Unit
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showIconDialog by remember { mutableStateOf(false) }
    var showSecondarySrtDialog by remember { mutableStateOf(false) }
    var showOpenAiDialog by remember { mutableStateOf(false) }
    var showAddCueDialog by remember { mutableStateOf(false) }
    var cueToSplit by remember { mutableStateOf<SubtitleCue?>(null) }
    var cueToDelete by remember { mutableStateOf<SubtitleCue?>(null) }
    var editingCue by remember { mutableStateOf<SubtitleCue?>(null) }
    var onlyShowStarred by remember { mutableStateOf(false) }
    var onlyShowWithGemini by remember { mutableStateOf(false) }

    // Gemini explanation state
    var geminiCueToExplain by remember { mutableStateOf<SubtitleCue?>(null) }
    var geminiTermToExplain by remember { mutableStateOf<String?>(null) }

    // Word Lookup state
    var lookupWord by remember { mutableStateOf<String?>(null) }
    var lookupSentence by remember { mutableStateOf<String?>(null) }
    var lookupTranslation by remember { mutableStateOf<String?>(null) }

    // Phase-by-phase back handling
    BackHandler(enabled = true) {
        when {
            lookupWord != null -> {
                lookupWord = null
                lookupSentence = null
                lookupTranslation = null
            }
            geminiCueToExplain != null -> {
                geminiCueToExplain = null
                geminiTermToExplain = null
            }
            editingCue != null -> editingCue = null
            showAddCueDialog -> showAddCueDialog = false
            cueToSplit != null -> cueToSplit = null
            cueToDelete != null -> cueToDelete = null
            showRenameDialog -> showRenameDialog = false
            showIconDialog -> showIconDialog = false
            showSecondarySrtDialog -> showSecondarySrtDialog = false
            showOpenAiDialog -> showOpenAiDialog = false
            showExportDialog -> showExportDialog = false
            cueSearchQuery.isNotBlank() -> onSearchCueQueryChange("")
            onlyShowStarred -> onlyShowStarred = false
            onlyShowWithGemini -> onlyShowWithGemini = false
            else -> onCloseProject()
        }
    }

    // Active primary and secondary cues
    val activeCue = cues.firstOrNull { it.isActiveAt(currentPositionMs, timeOffsetMs) }
    val activeSecondaryCue = secondaryCues.firstOrNull { it.isActiveAt(currentPositionMs, timeOffsetMs) }

    // Launcher for secondary SRT file
    val secondarySrtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = readSrtFromUri(context, uri)
            if (content.isNotBlank()) {
                onLoadSecondarySrt(content)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCueDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Nova Legenda", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Modern Project Cover / Icon with edit affordance
                        ProjectCoverIcon(
                            customIconUri = project.customIconUri,
                            mediaType = project.mediaType,
                            size = 38.dp,
                            shape = RoundedCornerShape(10.dp),
                            showEditBadge = true,
                            onClick = { showIconDialog = true }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRenameDialog = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                    contentDescription = "Renomear Áudio/Projeto",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            val folderName = folders.firstOrNull { it.id == project.folderId }?.name ?: "Sem Pasta"
                            Text(
                                text = "Pasta: $folderName • $playbackSpeed x veloc.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseProject) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Tradutor IA Button
                    IconButton(onClick = { showOpenAiDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Tradutor IA OpenAI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Secondary SRT / Translation Button
                    IconButton(onClick = { showSecondarySrtDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "2ª Legenda / Tradução",
                            tint = if (secondaryCues.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Fullscreen button
                    IconButton(onClick = onToggleFullscreen) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Modo Tela Cheia",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Export button
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Exportar Legenda",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // RETRO AUDIO PLAYER & TIMELINE
            RetroAudioPlayerView(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onSeek = onSeek,
                playbackSpeed = playbackSpeed,
                onChangeSpeed = onChangeSpeed,
                isSentenceLoop = isSentenceLoop,
                onToggleSentenceLoop = onToggleSentenceLoop,
                isAutoPause = isAutoPause,
                onToggleAutoPause = onToggleAutoPause,
                modifier = Modifier.fillMaxWidth()
            )

            var currentTab by remember { mutableStateOf(EditorWorkspaceTab.LIST) }

            // COMPACT WORKSPACE TAB ROW
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                EditorWorkspaceTab.entries.forEach { tab ->
                    val isSelected = (currentTab == tab)
                    Tab(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (currentTab) {
                EditorWorkspaceTab.PREVIEW -> {
                    // FULL STYLED PREVIEW & CUSTOMIZATION TAB
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = subtitleStylePreset.cardBackgroundColor),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, subtitleStylePreset.borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = null,
                                        tint = subtitleStylePreset.textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ESTILIZAÇÃO & PREVIEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = subtitleStylePreset.textColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Text Alignment Buttons
                                    SubtitleAlignment.entries.forEach { align ->
                                        val isSel = (subtitleAlignment == align)
                                        val icon = when (align) {
                                            SubtitleAlignment.LEFT -> Icons.Default.FormatAlignLeft
                                            SubtitleAlignment.CENTER -> Icons.Default.FormatAlignCenter
                                            SubtitleAlignment.RIGHT -> Icons.Default.FormatAlignRight
                                            SubtitleAlignment.JUSTIFY -> Icons.Default.FormatAlignJustify
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSel) subtitleStylePreset.textColor.copy(alpha = 0.25f) else Color.Transparent,
                                            border = if (isSel) BorderStroke(1.dp, subtitleStylePreset.textColor) else null,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onSetAlignment(align) }
                                        ) {
                                            Box(modifier = Modifier.padding(3.dp)) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = align.label,
                                                    tint = if (isSel) subtitleStylePreset.textColor else subtitleStylePreset.inactiveTextColor,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Style Presets Pills
                                    SubtitleStylePreset.entries.forEach { preset ->
                                        val isSelected = (subtitleStylePreset == preset)
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(preset.textColor, CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { onSetStylePreset(preset) }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Font size slider inside Preview tab
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Tamanho (${fontSizeSp.toInt()}sp):",
                                    fontSize = 11.sp,
                                    color = subtitleStylePreset.textColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Slider(
                                    value = fontSizeSp,
                                    onValueChange = onSetFontSize,
                                    valueRange = 12f..32f,
                                    modifier = Modifier.weight(1f).height(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = subtitleStylePreset.activeBackgroundColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activeCue != null) {
                                        val chunks = activeCue.get3WordChunks()
                                        val activeChunk = activeCue.getActive3WordChunk(currentPositionMs)

                                        val annotatedText = buildAnnotatedString {
                                            if (chunks.isNotEmpty()) {
                                                chunks.forEachIndexed { chunkIdx, chunk ->
                                                    val isChunkActive = (activeChunk == chunk)
                                                    val chunkText = chunk.joinToString(" ") { it.word }

                                                    if (isChunkActive) {
                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = subtitleStylePreset.textColor,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                background = subtitleStylePreset.cardBackgroundColor
                                                            )
                                                        ) {
                                                            append(chunkText)
                                                        }
                                                    } else {
                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = subtitleStylePreset.textColor.copy(alpha = 0.85f),
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        ) {
                                                            append(chunkText)
                                                        }
                                                    }
                                                    if (chunkIdx < chunks.lastIndex) append(" ")
                                                }
                                            } else {
                                                append(activeCue.text)
                                            }
                                        }

                                        Text(
                                            text = annotatedText,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = (fontSizeSp * 1.35f).sp,
                                            textAlign = subtitleAlignment.align,
                                            style = TextStyle(
                                                lineBreak = LineBreak.Paragraph,
                                                hyphens = Hyphens.Auto
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(
                                            text = "Aguardando áudio / reprodução...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = subtitleStylePreset.inactiveTextColor
                                        )
                                    }
                                }
                            }

                            // SECONDARY DUAL SUBTITLE
                            if (secondaryCues.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = subtitleStylePreset.activeBackgroundColor.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, subtitleStylePreset.borderColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Translate,
                                                contentDescription = null,
                                                tint = subtitleStylePreset.textColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "TRADUÇÃO (2ª LEGENDA)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = subtitleStylePreset.textColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = activeSecondaryCue?.text ?: "...",
                                            fontSize = (fontSizeSp * 0.85f).sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = subtitleAlignment.align,
                                            color = subtitleStylePreset.textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                EditorWorkspaceTab.SYNC -> {
                    // DEDICATED TIMING SYNCHRONIZATION ADJUSTMENT TAB
                    TimingAdjustmentBar(
                        currentOffsetMs = timeOffsetMs,
                        onAdjustMs = onAdjustOffsetMs,
                        onSetOffsetMs = onSetOffsetMs
                    )
                }

                EditorWorkspaceTab.LIST -> {
                    // In LIST mode, if audio is playing or cue is active, show an ultra-compact live ticker ribbon
                    if (activeCue != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = subtitleStylePreset.cardBackgroundColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, subtitleStylePreset.borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentTab = EditorWorkspaceTab.PREVIEW }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${activeCue.index}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeCue.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subtitleStylePreset.textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (activeSecondaryCue != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${activeSecondaryCue.text}",
                                        fontSize = 11.sp,
                                        color = subtitleStylePreset.textColor.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Cues List Header & Search Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filteredCuesCount = if (cueSearchQuery.isBlank()) cues.size else cues.count { it.text.contains(cueSearchQuery, ignoreCase = true) }
                    Text(
                        text = "Linhas de Legenda ($filteredCuesCount/${cues.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { showSecondarySrtDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (secondaryCues.isEmpty()) "+ 2ª Legenda" else "2ª Legenda", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = cueSearchQuery,
                        onValueChange = onSearchCueQueryChange,
                        placeholder = { Text("Buscar na legenda...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (cueSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchCueQueryChange("") },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )

                    FilterChip(
                        selected = !onlyShowStarred && !onlyShowWithGemini,
                        onClick = {
                            onlyShowStarred = false
                            onlyShowWithGemini = false
                        },
                        label = { Text("Todas", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = onlyShowStarred,
                        onClick = {
                            onlyShowStarred = !onlyShowStarred
                            if (onlyShowStarred) onlyShowWithGemini = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                        },
                        label = { Text("⭐ (${starredCueIndices.size})", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = onlyShowWithGemini,
                        onClick = {
                            onlyShowWithGemini = !onlyShowWithGemini
                            if (onlyShowWithGemini) onlyShowStarred = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8E2DE2), modifier = Modifier.size(12.dp))
                        },
                        label = { Text("✨ Gemini (${geminiNotes.size})", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Filtered Cues List
            val filteredCues = remember(cues, cueSearchQuery, onlyShowStarred, onlyShowWithGemini, starredCueIndices, geminiNotes) {
                var list = if (cueSearchQuery.isBlank()) cues else cues.filter { it.text.contains(cueSearchQuery, ignoreCase = true) }
                if (onlyShowStarred) {
                    list = list.filter { starredCueIndices.contains(it.index) }
                }
                if (onlyShowWithGemini) {
                    val noteIndices = geminiNotes.map { it.cueIndex }.toSet()
                    list = list.filter { noteIndices.contains(it.index) }
                }
                list
            }

            val cueListState = rememberLazyListState()
            val density = LocalDensity.current

            val activeIndex = remember(filteredCues, currentPositionMs, timeOffsetMs) {
                filteredCues.indexOfFirst { it.isActiveAt(currentPositionMs, timeOffsetMs) }
            }

            LaunchedEffect(activeIndex, isPlaying) {
                if (activeIndex != -1 && isPlaying) {
                    cueListState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = cueListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp)
            ) {
                items(filteredCues) { cue ->
                    val isActive = cue.isActiveAt(currentPositionMs, timeOffsetMs)
                    val isStarred = starredCueIndices.contains(cue.index)
                    val cueGeminiNote = geminiNotes.firstOrNull { it.cueIndex == cue.index }
                    val adjStart = cue.getAdjustedStart(timeOffsetMs)
                    val adjEnd = cue.getAdjustedEnd(timeOffsetMs)
                    var showCueMenu by remember { mutableStateOf(false) }

                    Card(
                        onClick = { onSeek(adjStart) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isStarred || cueGeminiNote != null) {
                            androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (cueGeminiNote != null) Color(0xFF8E2DE2) else Color(0xFFFFB300)
                            )
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${cue.index}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${SrtParser.formatTimestamp(adjStart)} --> ${SrtParser.formatTimestamp(adjEnd)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = cue.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                // Gemini AI Button
                                IconButton(
                                    onClick = {
                                        geminiCueToExplain = cue
                                        geminiTermToExplain = cueGeminiNote?.termOrSentence ?: cue.text
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Consultar Gemini AI",
                                        tint = if (cueGeminiNote != null) Color(0xFF8E2DE2) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Star Button
                                IconButton(
                                    onClick = { onToggleStarCue(cue.index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favoritar / Marcar",
                                        tint = if (isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // More Options Menu (Edit, Split, Merge, Delete, Dictionary, Gemini)
                                Box {
                                    IconButton(
                                        onClick = { showCueMenu = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opções da Legenda",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showCueMenu,
                                        onDismissRequest = { showCueMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("✨ Consultar Gemini AI") },
                                            onClick = {
                                                showCueMenu = false
                                                geminiCueToExplain = cue
                                                geminiTermToExplain = cueGeminiNote?.termOrSentence ?: cue.text
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("✏️ Editar Texto") },
                                            onClick = {
                                                showCueMenu = false
                                                editingCue = cue
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("📖 Consultar Dicionário") },
                                            onClick = {
                                                showCueMenu = false
                                                lookupWord = cue.text.split(" ").firstOrNull() ?: cue.text
                                                lookupSentence = cue.text
                                                lookupTranslation = secondaryCues.firstOrNull { it.index == cue.index }?.text
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("✂️ Dividir Legenda Aqui") },
                                            onClick = {
                                                showCueMenu = false
                                                cueToSplit = cue
                                            }
                                        )
                                        if (cue.index < cues.size) {
                                            DropdownMenuItem(
                                                text = { Text("🔗 Mesclar com Próxima") },
                                                onClick = {
                                                    showCueMenu = false
                                                    onMergeCueWithNext(cue.index)
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("🗑️ Excluir Linha", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showCueMenu = false
                                                cueToDelete = cue
                                            }
                                        )
                                    }
                                }
                            }

                            // Saved Gemini Note Indicator Card
                            if (cueGeminiNote != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF8E2DE2).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF8E2DE2).copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            geminiCueToExplain = cue
                                            geminiTermToExplain = cueGeminiNote.termOrSentence
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF8E2DE2),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "✨ Explicação Gemini: ${cueGeminiNote.termOrSentence}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8E2DE2),
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Ver",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8E2DE2)
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

    // DIALOG: ALTERAR NOME DO ÁUDIO / PROJETO
    if (showRenameDialog) {
        var newTitleText by remember { mutableStateOf(project.title) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Alterar Nome do Áudio", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite o novo nome para este projeto de áudio e legenda:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTitleText,
                        onValueChange = { newTitleText = it },
                        label = { Text("Nome do Áudio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitleText.isNotBlank()) {
                            onUpdateTitle(newTitleText.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Salvar Nome")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIALOG: ADICIONAR / EDITAR 2ª LEGENDA (TRADUÇÃO)
    if (showSecondarySrtDialog) {
        var secondarySrtInput by remember { mutableStateOf(project.secondarySrtContent ?: "") }

        AlertDialog(
            onDismissRequest = { showSecondarySrtDialog = false },
            title = { Text("2ª Legenda / Tradução (Opcional)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Adicione um segundo arquivo de legenda SRT em outro idioma (ex: Português / Inglês). Ele rodará sincronizado logo abaixo da legenda principal.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showSecondarySrtDialog = false
                            showOpenAiDialog = true
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Traduzir Legenda com IA OpenAI", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            secondarySrtPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escolher Arquivo .SRT do Celular")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = secondarySrtInput,
                        onValueChange = { secondarySrtInput = it },
                        label = { Text("Conteúdo da 2ª Legenda (SRT)") },
                        placeholder = { Text("Cole o arquivo SRT da segunda legenda aqui...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLoadSecondarySrt(secondarySrtInput)
                        showSecondarySrtDialog = false
                    }
                ) {
                    Text("Salvar 2ª Legenda")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (secondaryCues.isNotEmpty()) {
                            onLoadSecondarySrt("") // Remove secondary
                        }
                        showSecondarySrtDialog = false
                    }
                ) {
                    Text(if (secondaryCues.isNotEmpty()) "Remover 2ª Legenda" else "Cancelar")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            projectTitle = project.title,
            cues = cues,
            offsetMs = timeOffsetMs,
            onDismiss = { showExportDialog = false }
        )
    }

    // Edit Cue Dialog
    if (editingCue != null) {
        val cue = editingCue!!
        var text by remember { mutableStateOf(cue.text) }

        AlertDialog(
            onDismissRequest = { editingCue = null },
            title = { Text("Editar Legenda #${cue.index}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Texto da Legenda") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCue(cue.index, text, cue.startTimeMs, cue.endTimeMs)
                        editingCue = null
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCue = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add New Cue Dialog
    if (showAddCueDialog) {
        val suggestedStart = if (currentPositionMs > 0) currentPositionMs else (cues.lastOrNull()?.endTimeMs ?: 0L)
        val suggestedEnd = suggestedStart + 3000L
        var newCueText by remember { mutableStateOf("") }
        var startSecondsStr by remember { mutableStateOf(String.format("%.1f", suggestedStart / 1000f).replace(',', '.')) }
        var endSecondsStr by remember { mutableStateOf(String.format("%.1f", suggestedEnd / 1000f).replace(',', '.')) }

        AlertDialog(
            onDismissRequest = { showAddCueDialog = false },
            title = { Text("Adicionar Nova Linha de Legenda", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCueText,
                        onValueChange = { newCueText = it },
                        label = { Text("Texto da Legenda") },
                        placeholder = { Text("Ex: Hello, welcome back!") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startSecondsStr,
                            onValueChange = { startSecondsStr = it },
                            label = { Text("Início (s)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = endSecondsStr,
                            onValueChange = { endSecondsStr = it },
                            label = { Text("Fim (s)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val startSec = startSecondsStr.toFloatOrNull() ?: (suggestedStart / 1000f)
                        val endSec = endSecondsStr.toFloatOrNull() ?: (suggestedEnd / 1000f)
                        val startMs = (startSec * 1000).toLong()
                        val endMs = (endSec * 1000).toLong().coerceAtLeast(startMs + 500L)
                        if (newCueText.isNotBlank()) {
                            onAddCue(startMs, endMs, newCueText.trim())
                        }
                        showAddCueDialog = false
                    }
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCueDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Split Cue Dialog
    if (cueToSplit != null) {
        val cue = cueToSplit!!
        val midMs = (cue.startTimeMs + cue.endTimeMs) / 2
        var splitSecondsStr by remember {
            val defaultSplit = if (currentPositionMs in cue.startTimeMs..cue.endTimeMs) currentPositionMs else midMs
            mutableStateOf(String.format("%.1f", defaultSplit / 1000f).replace(',', '.'))
        }

        AlertDialog(
            onDismissRequest = { cueToSplit = null },
            title = { Text("Dividir Legenda #${cue.index}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Texto atual:\n\"${cue.text}\"", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = splitSecondsStr,
                        onValueChange = { splitSecondsStr = it },
                        label = { Text("Ponto de Divisão em Segundos (s)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val splitSec = splitSecondsStr.toFloatOrNull() ?: (midMs / 1000f)
                        val splitMs = (splitSec * 1000).toLong()
                        onSplitCue(cue.index, splitMs)
                        cueToSplit = null
                    }
                ) {
                    Text("Dividir em 2")
                }
            },
            dismissButton = {
                TextButton(onClick = { cueToSplit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Cue Confirmation Dialog
    if (cueToDelete != null) {
        val cue = cueToDelete!!
        AlertDialog(
            onDismissRequest = { cueToDelete = null },
            title = { Text("Excluir Legenda #${cue.index}?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Deseja realmente remover esta linha de legenda?\n\n\"${cue.text}\"", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCue(cue.index)
                        cueToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { cueToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Word Lookup / Dictionary Dialog
    if (lookupWord != null) {
        val word = lookupWord!!
        val cue = cues.firstOrNull { it.text.contains(word, ignoreCase = true) } ?: activeCue ?: cues.firstOrNull()
        val isWordStarred = cue != null && starredCueIndices.contains(cue.index)

        WordLookupDialog(
            word = word,
            sentenceContext = lookupSentence,
            translationContext = lookupTranslation,
            isStarred = isWordStarred,
            onToggleStar = {
                if (cue != null) {
                    onToggleStarCue(cue.index)
                }
            },
            onConsultGemini = {
                if (cue != null) {
                    geminiCueToExplain = cue
                    geminiTermToExplain = word
                }
            },
            onDismiss = {
                lookupWord = null
                lookupSentence = null
                lookupTranslation = null
            }
        )
    }

    // Gemini Explanation Dialog
    if (geminiCueToExplain != null) {
        val cue = geminiCueToExplain!!
        val note = geminiNotes.firstOrNull { it.cueIndex == cue.index }
        val term = geminiTermToExplain ?: note?.termOrSentence ?: cue.text
        val translation = secondaryCues.firstOrNull { it.index == cue.index }?.text
        val isCueStarred = starredCueIndices.contains(cue.index)

        GeminiExplanationDialog(
            termOrSentence = term,
            fullContext = cue.text,
            translationContext = translation,
            cueIndex = cue.index,
            existingNote = note,
            isStarred = isCueStarred,
            onToggleStar = { onToggleStarCue(cue.index) },
            onSaveNote = { responseText ->
                onSaveGeminiNote(cue.index, term, cue.text, responseText)
            },
            onDeleteNote = {
                onDeleteGeminiNoteForCue(cue.index)
            },
            onDismiss = {
                geminiCueToExplain = null
                geminiTermToExplain = null
            }
        )
    }

    if (showIconDialog) {
        ProjectIconPickerDialog(
            currentIconUri = project.customIconUri,
            projectTitle = project.title,
            mediaType = project.mediaType,
            onDismiss = { showIconDialog = false },
            onSaveIcon = { newIconUri ->
                onUpdateProjectIcon?.invoke(newIconUri)
                showIconDialog = false
            }
        )
    }

    if (showOpenAiDialog) {
        OpenAiTranslatorDialog(
            currentProjectSrt = project.srtContent,
            currentProjectTitle = project.title,
            onDismiss = { showOpenAiDialog = false },
            onAttachAsSecondary = { translatedSrt ->
                onLoadSecondarySrt(translatedSrt)
            },
            onCreateNewProject = { _, _ -> }
        )
    }
}
