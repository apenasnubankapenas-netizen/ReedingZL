package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.data.model.FullscreenDisplayMode
import com.example.data.model.SubtitleAlignment
import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitleStylePreset
import com.example.data.parser.SrtParser
import com.example.ui.components.RetroAudioPlayerView

data class AlignedWordPair(
    val primaryWord: String,
    val secondaryWord: String
)

fun alignWords(primaryText: String, secondaryText: String): List<AlignedWordPair> {
    if (primaryText.isBlank() || secondaryText.isBlank()) return emptyList()

    val primaryTokens = primaryText.split(Regex("\\s+")).filter { it.isNotBlank() }
    val secondaryTokens = secondaryText.split(Regex("\\s+")).filter { it.isNotBlank() }

    val pairs = mutableListOf<AlignedWordPair>()
    val maxCount = maxOf(primaryTokens.size, secondaryTokens.size)

    for (i in 0 until maxCount) {
        val p = primaryTokens.getOrNull(i) ?: ""
        val s = secondaryTokens.getOrNull(i) ?: ""
        if (p.isNotBlank() || s.isNotBlank()) {
            pairs.add(AlignedWordPair(p, s))
        }
    }
    return pairs
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterlinearWordPairRow(
    primaryText: String,
    secondaryText: String,
    primaryColor: Color,
    secondaryColor: Color,
    fontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val wordPairs = remember(primaryText, secondaryText) { alignWords(primaryText, secondaryText) }

    if (wordPairs.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        wordPairs.forEach { pair ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = primaryColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = pair.primaryWord,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSizeSp * 0.82f).sp
                    )
                    if (pair.secondaryWord.isNotBlank()) {
                        Text(
                            text = " = ",
                            color = primaryColor.copy(alpha = 0.45f),
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSizeSp * 0.72f).sp
                        )
                        Text(
                            text = pair.secondaryWord,
                            color = secondaryColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = (fontSizeSp * 0.78f).sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullScreenSubtitleView(
    projectTitle: String,
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue> = emptyList(),
    currentPositionMs: Long,
    timeOffsetMs: Long,
    fontSizeSp: Float,
    subtitleStylePreset: SubtitleStylePreset = SubtitleStylePreset.CLASSIC_WHITE,
    subtitleAlignment: SubtitleAlignment = SubtitleAlignment.CENTER,
    displayMode: FullscreenDisplayMode = FullscreenDisplayMode.CONTINUOUS_3PARTS,
    isPlaying: Boolean,
    durationMs: Long,
    playbackSpeed: Float = 1.0f,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onAdjustOffsetMs: (Long) -> Unit = {},
    onSetFontSize: (Float) -> Unit = {},
    onChangeSpeed: (Float) -> Unit = {},
    onSetStylePreset: (SubtitleStylePreset) -> Unit = {},
    onSetAlignment: (SubtitleAlignment) -> Unit = {},
    onSetDisplayMode: (FullscreenDisplayMode) -> Unit = {},
    onLoadSecondarySrt: ((String) -> Unit)? = null,
    onExitFullscreen: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val secondarySrtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && onLoadSecondarySrt != null) {
            val content = readSrtFromUri(context, uri)
            if (content.isNotBlank()) {
                onLoadSecondarySrt(content)
            }
        }
    }

    // Resolve continuous 3-part indices
    val primaryIdx = get3PartsCueIndex(cues, currentPositionMs, timeOffsetMs)
    val secondaryIdx = get3PartsCueIndex(secondaryCues, currentPositionMs, timeOffsetMs)

    val cycleDisplayMode = {
        val modes = FullscreenDisplayMode.entries
        val nextIdx = (modes.indexOf(displayMode) + 1) % modes.size
        onSetDisplayMode(modes[nextIdx])
    }

    val topSpacerHeight = if (showControls) 115.dp else 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            },
        contentAlignment = Alignment.Center
    ) {
        when (displayMode) {
            FullscreenDisplayMode.CONTINUOUS_3PARTS -> {
                Continuous3PartsContent(
                    cues = cues,
                    secondaryCues = secondaryCues,
                    primaryIdx = primaryIdx,
                    secondaryIdx = secondaryIdx,
                    currentPositionMs = currentPositionMs,
                    timeOffsetMs = timeOffsetMs,
                    fontSizeSp = fontSizeSp,
                    subtitleStylePreset = subtitleStylePreset,
                    subtitleAlignment = subtitleAlignment,
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onCycleDisplayMode = cycleDisplayMode,
                    topSpacerHeight = topSpacerHeight
                )
            }
            FullscreenDisplayMode.BIG_CINEMA -> {
                BigCinemaContent(
                    cues = cues,
                    secondaryCues = secondaryCues,
                    primaryIdx = primaryIdx,
                    secondaryIdx = secondaryIdx,
                    currentPositionMs = currentPositionMs,
                    timeOffsetMs = timeOffsetMs,
                    fontSizeSp = fontSizeSp,
                    subtitleStylePreset = subtitleStylePreset,
                    subtitleAlignment = subtitleAlignment,
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onCycleDisplayMode = cycleDisplayMode,
                    topSpacerHeight = topSpacerHeight
                )
            }
            FullscreenDisplayMode.PAPER_SHEET -> {
                PaperSheetContent(
                    cues = cues,
                    secondaryCues = secondaryCues,
                    primaryIdx = primaryIdx,
                    currentPositionMs = currentPositionMs,
                    timeOffsetMs = timeOffsetMs,
                    fontSizeSp = fontSizeSp,
                    subtitleStylePreset = subtitleStylePreset,
                    subtitleAlignment = subtitleAlignment,
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onCycleDisplayMode = cycleDisplayMode,
                    topSpacerHeight = topSpacerHeight
                )
            }
            FullscreenDisplayMode.TRANSCRIPT_SCROLL -> {
                TranscriptScrollContent(
                    cues = cues,
                    secondaryCues = secondaryCues,
                    primaryIdx = primaryIdx,
                    currentPositionMs = currentPositionMs,
                    timeOffsetMs = timeOffsetMs,
                    fontSizeSp = fontSizeSp,
                    subtitleStylePreset = subtitleStylePreset,
                    subtitleAlignment = subtitleAlignment,
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    topSpacerHeight = topSpacerHeight
                )
            }
            FullscreenDisplayMode.BOOK_MODE -> {
                BookModeContent(
                    cues = cues,
                    secondaryCues = secondaryCues,
                    primaryIdx = primaryIdx,
                    currentPositionMs = currentPositionMs,
                    timeOffsetMs = timeOffsetMs,
                    fontSizeSp = fontSizeSp,
                    subtitleStylePreset = subtitleStylePreset,
                    subtitleAlignment = subtitleAlignment,
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onCycleDisplayMode = cycleDisplayMode,
                    topSpacerHeight = topSpacerHeight
                )
            }
        }

        // TOP HEADER OVERLAY (CONTROLS & ADJUSTMENTS)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Title Bar with Exit Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = projectTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Toque na tela para ocultar/exibir controles",
                                color = Color(0xFF888888),
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onExitFullscreen,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Sair da Tela Cheia",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sair", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // QUICK IN-FULLSCREEN ADJUSTMENT TOOLBAR (Compact Horizontal Scroll)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // 1. Display Mode Switcher
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                FullscreenDisplayMode.entries.forEach { mode ->
                                    val isSel = (displayMode == mode)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) Color.White else Color.Transparent,
                                        modifier = Modifier.clickable { onSetDisplayMode(mode) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            val icon = when (mode) {
                                                FullscreenDisplayMode.CONTINUOUS_3PARTS -> Icons.Default.ViewCarousel
                                                FullscreenDisplayMode.BIG_CINEMA -> Icons.Default.Tv
                                                FullscreenDisplayMode.PAPER_SHEET -> Icons.Default.Description
                                                FullscreenDisplayMode.TRANSCRIPT_SCROLL -> Icons.Default.FormatListBulleted
                                                FullscreenDisplayMode.BOOK_MODE -> Icons.Default.MenuBook
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = mode.label,
                                                tint = if (isSel) Color.Black else Color(0xFF888888),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = mode.label,
                                                color = if (isSel) Color.Black else Color(0xFF888888),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Secondary Subtitle (.SRT) Picker Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (secondaryCues.isNotEmpty()) Color(0xFF00363D) else Color(0xFF181818),
                            border = BorderStroke(1.dp, if (secondaryCues.isNotEmpty()) Color(0xFF00B0FF) else Color(0xFF333333)),
                            modifier = Modifier.clickable {
                                secondarySrtLauncher.launch(arrayOf("*/*"))
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Carregar 2ª Legenda",
                                    tint = if (secondaryCues.isNotEmpty()) Color(0xFF00E5FF) else Color(0xFF888888),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (secondaryCues.isNotEmpty()) "2ª Legenda OK (Trocar)" else "+ 2ª Legenda (.SRT)",
                                    color = if (secondaryCues.isNotEmpty()) Color.White else Color(0xFF888888),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Subtitle Preset Color Style Switcher
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Estilo da Legenda",
                                    tint = subtitleStylePreset.textColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                SubtitleStylePreset.entries.forEach { preset ->
                                    val isSelected = (subtitleStylePreset == preset)
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(preset.textColor, CircleShape)
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { onSetStylePreset(preset) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }
                        }

                        // 3. Text Alignment Switcher
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                SubtitleAlignment.entries.forEach { align ->
                                    val isSel = (subtitleAlignment == align)
                                    val icon = when (align) {
                                        SubtitleAlignment.LEFT -> Icons.Default.FormatAlignLeft
                                        SubtitleAlignment.CENTER -> Icons.Default.FormatAlignCenter
                                        SubtitleAlignment.RIGHT -> Icons.Default.FormatAlignRight
                                        SubtitleAlignment.JUSTIFY -> Icons.Default.FormatAlignJustify
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) Color.White else Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onSetAlignment(align) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = align.label,
                                                tint = if (isSel) Color.Black else Color(0xFF888888),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            if (isSel) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = align.label,
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Font Size Controls
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Fonte:",
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                                IconButton(
                                    onClick = { onSetFontSize((fontSizeSp - 2f).coerceAtLeast(14f)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Diminuir Fonte",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    "${fontSizeSp.toInt()}sp",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { onSetFontSize((fontSizeSp + 2f).coerceAtMost(48f)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Aumentar Fonte",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // 5. Time Offset Controls
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (timeOffsetMs == 0L) "0ms" else "${timeOffsetMs}ms",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF262626),
                                    modifier = Modifier.clickable { onAdjustOffsetMs(-100L) }
                                ) {
                                    Text("-100", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF262626),
                                    modifier = Modifier.clickable { onAdjustOffsetMs(+100L) }
                                ) {
                                    Text("+100", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }

                        // 6. Speed Controls
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                    val isSelected = (playbackSpeed == spd)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        modifier = Modifier.clickable { onChangeSpeed(spd) }
                                    ) {
                                        Text(
                                            text = "${spd}x",
                                            color = if (isSelected) Color.Black else Color(0xFF888888),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

/**
 * 1. Mode 1: Continuous 3-Parts Flow Content
 */
@Composable
private fun Continuous3PartsContent(
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue>,
    primaryIdx: Int,
    secondaryIdx: Int,
    currentPositionMs: Long,
    timeOffsetMs: Long,
    fontSizeSp: Float,
    subtitleStylePreset: SubtitleStylePreset,
    subtitleAlignment: SubtitleAlignment,
    isPlaying: Boolean,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleDisplayMode: () -> Unit = {},
    topSpacerHeight: Dp = 12.dp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topSpacerHeight))

        RetroAudioPlayerView(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onTogglePlayPause = onTogglePlayPause,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // LEGENDA PRINCIPAL (3 PARTES CONTINUAS)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = subtitleStylePreset.cardBackgroundColor,
            border = BorderStroke(1.5.dp, subtitleStylePreset.borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCycleDisplayMode() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = subtitleStylePreset.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LEGENDA PRINCIPAL (FLUXO CONTINUO 3 PARTES)",
                        color = subtitleStylePreset.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (primaryIdx != -1 && primaryIdx < cues.size) {
                    val part1 = if (primaryIdx > 0) cues[primaryIdx - 1] else null
                    val part2 = cues[primaryIdx]
                    val part3 = if (primaryIdx < cues.lastIndex) cues[primaryIdx + 1] else null

                    val isPart2Active = part2.isActiveAt(currentPositionMs, timeOffsetMs)

                    val annotatedText = buildAnnotatedString {
                        if (part1 != null) {
                            withStyle(
                                style = SpanStyle(
                                    color = subtitleStylePreset.inactiveTextColor,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(part1.text)
                            }
                            append("  •  ")
                        }

                        if (isPart2Active) {
                            withStyle(
                                style = SpanStyle(
                                    color = subtitleStylePreset.textColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    textDecoration = TextDecoration.Underline,
                                    background = subtitleStylePreset.activeBackgroundColor
                                )
                            ) {
                                append(" ${part2.text} ")
                            }
                        } else {
                            withStyle(
                                style = SpanStyle(
                                    color = subtitleStylePreset.textColor.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(part2.text)
                            }
                        }

                        if (part3 != null) {
                            append("  •  ")
                            withStyle(
                                style = SpanStyle(
                                    color = subtitleStylePreset.inactiveTextColor,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(part3.text)
                            }
                        }
                    }

                    Text(
                        text = annotatedText,
                        fontSize = (fontSizeSp * 1.35f).sp,
                        lineHeight = (fontSizeSp * 1.7f).sp,
                        textAlign = subtitleAlignment.align,
                        style = TextStyle(
                            lineBreak = LineBreak.Paragraph,
                            hyphens = Hyphens.Auto
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = "Nenhuma legenda ativa no momento.",
                        color = subtitleStylePreset.inactiveTextColor,
                        fontSize = (fontSizeSp * 1.1f).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // 2ª LEGENDA / TRADUÇÃO
        if (secondaryCues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = subtitleStylePreset.cardBackgroundColor,
                border = BorderStroke(1.dp, subtitleStylePreset.borderColor.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = subtitleStylePreset.textColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TRADUÇÃO (2ª LEGENDA)",
                            color = subtitleStylePreset.textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (secondaryIdx != -1 && secondaryIdx < secondaryCues.size) {
                        val secPart1 = if (secondaryIdx > 0) secondaryCues[secondaryIdx - 1] else null
                        val secPart2 = secondaryCues[secondaryIdx]
                        val secPart3 = if (secondaryIdx < secondaryCues.lastIndex) secondaryCues[secondaryIdx + 1] else null

                        val isSec2Active = secPart2.isActiveAt(currentPositionMs, timeOffsetMs)

                        val annotatedSecText = buildAnnotatedString {
                            if (secPart1 != null) {
                                withStyle(
                                    style = SpanStyle(
                                        color = subtitleStylePreset.inactiveTextColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append(secPart1.text)
                                }
                                append("  •  ")
                            }

                            if (isSec2Active) {
                                withStyle(
                                    style = SpanStyle(
                                        color = subtitleStylePreset.textColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        background = subtitleStylePreset.activeBackgroundColor
                                    )
                                ) {
                                    append(" ${secPart2.text} ")
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = subtitleStylePreset.textColor.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(secPart2.text)
                                }
                            }

                            if (secPart3 != null) {
                                append("  •  ")
                                withStyle(
                                    style = SpanStyle(
                                        color = subtitleStylePreset.inactiveTextColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append(secPart3.text)
                                }
                            }
                        }

                        Text(
                            text = annotatedSecText,
                            fontSize = (fontSizeSp * 1.2f).sp,
                            lineHeight = (fontSizeSp * 1.5f).sp,
                            textAlign = subtitleAlignment.align,
                            style = TextStyle(
                                lineBreak = LineBreak.Paragraph,
                                hyphens = Hyphens.Auto
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * 2. Mode 2: Big Cinema Card Content (Large Typography)
 */
@Composable
private fun BigCinemaContent(
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue>,
    primaryIdx: Int,
    secondaryIdx: Int,
    currentPositionMs: Long,
    timeOffsetMs: Long,
    fontSizeSp: Float,
    subtitleStylePreset: SubtitleStylePreset,
    subtitleAlignment: SubtitleAlignment,
    isPlaying: Boolean,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleDisplayMode: () -> Unit = {},
    topSpacerHeight: Dp = 12.dp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(topSpacerHeight))

        RetroAudioPlayerView(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onTogglePlayPause = onTogglePlayPause,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Subtitle Banner
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = subtitleStylePreset.cardBackgroundColor,
            border = BorderStroke(2.dp, subtitleStylePreset.borderColor),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCycleDisplayMode() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val activeCue = if (primaryIdx != -1 && primaryIdx < cues.size) cues[primaryIdx] else null

                if (activeCue != null) {
                    val isActive = activeCue.isActiveAt(currentPositionMs, timeOffsetMs)

                    Text(
                        text = activeCue.text.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ").trim(),
                        color = if (isActive) subtitleStylePreset.textColor else subtitleStylePreset.inactiveTextColor,
                        fontSize = (fontSizeSp * 1.6f).sp,
                        lineHeight = (fontSizeSp * 2.0f).sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                        textAlign = subtitleAlignment.align,
                        style = TextStyle(
                            lineBreak = LineBreak.Paragraph,
                            hyphens = Hyphens.Auto
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${SrtParser.formatTimestamp(activeCue.startTimeMs)} → ${SrtParser.formatTimestamp(activeCue.endTimeMs)}",
                        color = subtitleStylePreset.borderColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Aguardando próxima legenda...",
                        color = subtitleStylePreset.inactiveTextColor,
                        fontSize = (fontSizeSp * 1.2f).sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Secondary Cinema Subtitle Banner
        if (secondaryCues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            val activeSecCue = if (secondaryIdx != -1 && secondaryIdx < secondaryCues.size) secondaryCues[secondaryIdx] else null

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = subtitleStylePreset.cardBackgroundColor,
                border = BorderStroke(1.dp, subtitleStylePreset.borderColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (activeSecCue != null) {
                        Text(
                            text = activeSecCue.text.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ").trim(),
                            color = subtitleStylePreset.textColor.copy(alpha = 0.9f),
                            fontSize = (fontSizeSp * 1.3f).sp,
                            lineHeight = (fontSizeSp * 1.6f).sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = subtitleAlignment.align,
                            style = TextStyle(
                                lineBreak = LineBreak.Paragraph,
                                hyphens = Hyphens.Auto
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Sem tradução para esta posição",
                            color = subtitleStylePreset.inactiveTextColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * Mode: Paper Sheet Content ("Folha de Papel")
 * Displays cues in clean paper prose format, dynamically highlighting the active cue,
 * keeping it in view with smooth reading flow without index numbers or timestamps.
 */
@Composable
private fun PaperSheetContent(
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue>,
    primaryIdx: Int,
    currentPositionMs: Long,
    timeOffsetMs: Long,
    fontSizeSp: Float,
    subtitleStylePreset: SubtitleStylePreset,
    subtitleAlignment: SubtitleAlignment,
    isPlaying: Boolean,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleDisplayMode: () -> Unit,
    topSpacerHeight: Dp = 12.dp
) {
    val listState = rememberLazyListState()

    // Smooth auto-scroll to keep active spoken sentence in view
    LaunchedEffect(primaryIdx, isPlaying) {
        if (primaryIdx in cues.indices && isPlaying) {
            listState.animateScrollToItem((primaryIdx - 1).coerceAtLeast(0))
        }
    }

    val progressPercent = if (durationMs > 0) {
        ((currentPositionMs.toFloat() / durationMs.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    // Theme palette based on subtitleStylePreset
    val paperBg = subtitleStylePreset.cardBackgroundColor
    val paperBorder = subtitleStylePreset.borderColor
    val textColorMain = subtitleStylePreset.textColor
    val textColorMuted = subtitleStylePreset.inactiveTextColor
    val activeHighlightBg = subtitleStylePreset.activeBackgroundColor
    val activeHighlightBorder = subtitleStylePreset.borderColor

    val alignmentToUse = subtitleAlignment.align

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topSpacerHeight))

        RetroAudioPlayerView(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onTogglePlayPause = onTogglePlayPause,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ELEGANT PAPER SHEET CONTAINER ("FOLHA DE PAPEL")
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = paperBg,
            border = BorderStroke(1.5.dp, paperBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header Bar (Reading Specs & Progress)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCycleDisplayMode() }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = textColorMain,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODO FOLHA DE PAPEL",
                            color = textColorMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = activeHighlightBg,
                            border = BorderStroke(1.dp, activeHighlightBorder.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Lido: $progressPercent%",
                                color = textColorMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onCycleDisplayMode() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Mudar Modo",
                                tint = textColorMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = paperBorder.copy(alpha = 0.7f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Paper Prose
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(cues) { index, cue ->
                        val isCurrent = cue.isActiveAt(currentPositionMs, timeOffsetMs)
                        val isPast = cue.getAdjustedEnd(timeOffsetMs) < currentPositionMs

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) activeHighlightBg else Color.Transparent,
                            border = BorderStroke(
                                width = if (isCurrent) 1.5.dp else 0.dp,
                                color = if (isCurrent) activeHighlightBorder else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSeek(cue.startTimeMs) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(24.dp)
                                            .clip(CircleShape)
                                            .background(activeHighlightBorder)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cue.text.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ").trim(),
                                        color = when {
                                            isCurrent -> textColorMain
                                            isPast -> textColorMuted
                                            else -> textColorMain.copy(alpha = 0.85f)
                                        },
                                        fontSize = (fontSizeSp * 0.95f).sp,
                                        lineHeight = (fontSizeSp * 1.45f).sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = alignmentToUse,
                                        style = TextStyle(
                                            lineBreak = LineBreak.Paragraph,
                                            hyphens = Hyphens.Auto
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (secondaryCues.isNotEmpty()) {
                                        val secCue = secondaryCues.getOrNull(index)
                                            ?: secondaryCues.firstOrNull { it.isActiveAt(cue.startTimeMs, timeOffsetMs) }
                                        if (secCue != null) {
                                            Text(
                                                text = secCue.text.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ").trim(),
                                                color = if (isCurrent) activeHighlightBorder else textColorMuted,
                                                fontSize = (fontSizeSp * 0.82f).sp,
                                                lineHeight = (fontSizeSp * 1.3f).sp,
                                                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                                                textAlign = alignmentToUse,
                                                style = TextStyle(
                                                    lineBreak = LineBreak.Paragraph,
                                                    hyphens = Hyphens.Auto
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 3.dp)
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

/**
 * 3. Mode 3: Interactive Transcript Scroll Mode
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscriptScrollContent(
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue> = emptyList(),
    primaryIdx: Int,
    currentPositionMs: Long,
    timeOffsetMs: Long,
    fontSizeSp: Float,
    subtitleStylePreset: SubtitleStylePreset,
    subtitleAlignment: SubtitleAlignment = SubtitleAlignment.CENTER,
    isPlaying: Boolean,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    topSpacerHeight: Dp = 12.dp
) {
    val listState = rememberLazyListState()

    // Auto-scroll to keep active cue in view
    LaunchedEffect(primaryIdx, isPlaying) {
        if (primaryIdx in cues.indices && isPlaying) {
            listState.animateScrollToItem((primaryIdx - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topSpacerHeight))

        RetroAudioPlayerView(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onTogglePlayPause = onTogglePlayPause,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            itemsIndexed(cues) { index, cue ->
                val isCurrent = cue.isActiveAt(currentPositionMs, timeOffsetMs)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) subtitleStylePreset.cardBackgroundColor else Color(0xFF0F0F0F)
                    ),
                    border = BorderStroke(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) subtitleStylePreset.borderColor else Color(0xFF262626)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeek(cue.startTimeMs) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) subtitleStylePreset.borderColor else Color(0xFF222222)
                        ) {
                            Text(
                                text = "#${cue.index}",
                                color = if (isCurrent) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cue.text,
                                color = if (isCurrent) subtitleStylePreset.textColor else Color(0xFFCCCCCC),
                                fontSize = (fontSizeSp * 0.9f).sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                textAlign = subtitleAlignment.align,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (secondaryCues.isNotEmpty()) {
                                val secCue = secondaryCues.getOrNull(index)
                                    ?: secondaryCues.firstOrNull { it.isActiveAt(cue.startTimeMs, timeOffsetMs) }
                                if (secCue != null) {
                                    Spacer(modifier = Modifier.height(6.dp))

                                    InterlinearWordPairRow(
                                        primaryText = cue.text,
                                        secondaryText = secCue.text,
                                        primaryColor = if (isCurrent) subtitleStylePreset.textColor else Color.LightGray,
                                        secondaryColor = subtitleStylePreset.borderColor,
                                        fontSizeSp = fontSizeSp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = secCue.text,
                                        color = if (isCurrent) subtitleStylePreset.borderColor else Color(0xFF888888),
                                        fontSize = (fontSizeSp * 0.82f).sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = subtitleAlignment.align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${SrtParser.formatTimestamp(cue.startTimeMs)} → ${SrtParser.formatTimestamp(cue.endTimeMs)}",
                                color = Color(0xFF777777),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Mode 5: True Book Page Mode ("Modo Livro PDF Folha Imersiva")
 * Continuous book prose with words flowing naturally together in paragraphs,
 * Serif book typography, real-time sentence highlighting, perfect justification without broken gaps,
 * dynamic text reflow when resizing fonts, and safe navigation bar spacing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookModeContent(
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue>,
    primaryIdx: Int,
    currentPositionMs: Long,
    timeOffsetMs: Long,
    fontSizeSp: Float,
    subtitleStylePreset: SubtitleStylePreset,
    subtitleAlignment: SubtitleAlignment,
    isPlaying: Boolean,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleDisplayMode: () -> Unit = {},
    topSpacerHeight: Dp = 12.dp
) {
    val listState = rememberLazyListState()
    var isImmersiveMode by remember { mutableStateOf(false) }
    var showInlineTranslation by remember { mutableStateOf(false) }

    // Group cues into natural, balanced book paragraphs (6 cues per paragraph)
    val chunkSize = 6
    val cueChunks = remember(cues) { cues.chunked(chunkSize) }

    val activeChunkIndex = remember(primaryIdx) {
        if (primaryIdx in cues.indices) {
            (primaryIdx / chunkSize).coerceIn(0, (cueChunks.size - 1).coerceAtLeast(0))
        } else 0
    }

    // Auto scroll smoothly to active paragraph during playback
    LaunchedEffect(activeChunkIndex, isPlaying) {
        if (cueChunks.isNotEmpty() && isPlaying) {
            listState.animateScrollToItem(activeChunkIndex)
        }
    }

    val progressPercent = if (durationMs > 0) {
        ((currentPositionMs.toFloat() / durationMs.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    val paperBg = subtitleStylePreset.cardBackgroundColor
    val paperBorder = subtitleStylePreset.borderColor
    val textColorMain = subtitleStylePreset.textColor
    val textColorMuted = subtitleStylePreset.inactiveTextColor
    val activeHighlightBg = subtitleStylePreset.activeBackgroundColor
    val activeHighlightBorder = subtitleStylePreset.borderColor

    // Active translation for the current playing sentence
    val activeSecondaryCue = remember(primaryIdx, secondaryCues, timeOffsetMs) {
        if (secondaryCues.isNotEmpty() && primaryIdx in cues.indices) {
            val curCue = cues[primaryIdx]
            secondaryCues.getOrNull(primaryIdx)
                ?: secondaryCues.firstOrNull { it.isActiveAt(curCue.startTimeMs, timeOffsetMs) }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = if (isImmersiveMode) 0.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isImmersiveMode) {
            Spacer(modifier = Modifier.height(topSpacerHeight))
        }

        // Book Page Container (PDF Leaf)
        Surface(
            shape = if (isImmersiveMode) RectangleShape else RoundedCornerShape(18.dp),
            color = paperBg,
            border = if (isImmersiveMode) null else BorderStroke(1.5.dp, paperBorder),
            shadowElevation = if (isImmersiveMode) 0.dp else 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isImmersiveMode) 22.dp else 18.dp,
                            vertical = if (isImmersiveMode) 16.dp else 14.dp
                        )
                ) {
                    // Header Bar (Hidden in Immersive Mode)
                    AnimatedVisibility(
                        visible = !isImmersiveMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onCycleDisplayMode() }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Modo Livro",
                                        tint = textColorMain,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MODO LIVRO",
                                        color = textColorMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (secondaryCues.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (showInlineTranslation) activeHighlightBorder.copy(alpha = 0.25f) else activeHighlightBg,
                                            border = BorderStroke(1.dp, activeHighlightBorder.copy(alpha = 0.6f)),
                                            modifier = Modifier.clickable { showInlineTranslation = !showInlineTranslation }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Translate,
                                                    contentDescription = "Tradução",
                                                    tint = textColorMain,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (showInlineTranslation) "Trad. Ativa" else "Trad.",
                                                    color = textColorMain,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Immersive Zen Button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = activeHighlightBg,
                                        border = BorderStroke(1.dp, activeHighlightBorder.copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable { isImmersiveMode = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = "Só Texto",
                                                tint = textColorMain,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Só Texto",
                                                color = textColorMain,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = activeHighlightBg,
                                        border = BorderStroke(1.dp, activeHighlightBorder.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "$progressPercent%",
                                            color = textColorMain,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = paperBorder.copy(alpha = 0.5f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Continuous Book Prose Reading (Full justification, clean word spacing, no broken gaps)
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(cueChunks) { chunkIndex, chunkCues ->
                            val baseCueIndex = chunkIndex * chunkSize

                            // Build continuous prose annotated string for this book paragraph
                            val paragraphAnnotatedString = remember(
                                chunkCues,
                                secondaryCues,
                                primaryIdx,
                                subtitleStylePreset,
                                fontSizeSp,
                                showInlineTranslation
                            ) {
                                buildAnnotatedString {
                                    var isFirstCueInParagraph = true
                                    chunkCues.forEachIndexed { innerIndex, cue ->
                                        val globalIndex = baseCueIndex + innerIndex
                                        val isCurrent = (globalIndex == primaryIdx)
                                        val isPast = globalIndex < primaryIdx

                                        // Clean text: strip all \r\n and consecutive whitespaces so there are no empty gaps
                                        val cleanText = cue.text
                                            .replace("\r\n", " ")
                                            .replace("\n", " ")
                                            .replace("\r", " ")
                                            .replace(Regex("\\s+"), " ")
                                            .trim()

                                        if (cleanText.isNotEmpty()) {
                                            if (!isFirstCueInParagraph) {
                                                // Avoid leading space if cue begins with punctuation
                                                val startsWithPunctuation = cleanText.firstOrNull()?.let { it in ".,!?;:)]}'\"" } == true
                                                if (!startsWithPunctuation) {
                                                    append(" ")
                                                }
                                            }
                                            isFirstCueInParagraph = false

                                            val startOffset = length

                                            val spanStyle = SpanStyle(
                                                color = when {
                                                    isCurrent -> textColorMain
                                                    isPast -> textColorMuted
                                                    else -> textColorMain.copy(alpha = 0.95f)
                                                },
                                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                                background = if (isCurrent) activeHighlightBg.copy(alpha = 0.45f) else Color.Transparent,
                                                fontSize = (fontSizeSp * 0.95f).sp
                                            )

                                            pushStyle(spanStyle)
                                            append(cleanText)
                                            pop()

                                            // Inline translation if user explicitly enabled it
                                            if (showInlineTranslation && secondaryCues.isNotEmpty()) {
                                                val secCue = secondaryCues.getOrNull(globalIndex)
                                                    ?: secondaryCues.firstOrNull { it.isActiveAt(cue.startTimeMs, timeOffsetMs) }
                                                if (secCue != null) {
                                                    val cleanSecText = secCue.text
                                                        .replace("\r\n", " ")
                                                        .replace("\n", " ")
                                                        .replace("\r", " ")
                                                        .replace(Regex("\\s+"), " ")
                                                        .trim()
                                                    if (cleanSecText.isNotEmpty()) {
                                                        append(" ")
                                                        pushStyle(
                                                            SpanStyle(
                                                                color = activeHighlightBorder,
                                                                fontSize = (fontSizeSp * 0.75f).sp,
                                                                fontWeight = FontWeight.Medium,
                                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                                background = activeHighlightBorder.copy(alpha = 0.12f)
                                                            )
                                                        )
                                                        append("[$cleanSecText]")
                                                        pop()
                                                    }
                                                }
                                            }

                                            addStringAnnotation(
                                                tag = "CUE_INDEX",
                                                annotation = globalIndex.toString(),
                                                start = startOffset,
                                                end = length
                                            )
                                        }
                                    }
                                }
                            }

                            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                            val paragraphIndent = remember(subtitleAlignment, fontSizeSp) {
                                if (subtitleAlignment == SubtitleAlignment.JUSTIFY || subtitleAlignment == SubtitleAlignment.LEFT) {
                                    TextIndent(firstLine = (fontSizeSp * 1.35f).sp)
                                } else {
                                    TextIndent.None
                                }
                            }

                            Text(
                                text = paragraphAnnotatedString,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(paragraphAnnotatedString) {
                                        detectTapGestures { pos ->
                                            textLayoutResult?.let { layout ->
                                                val offset = layout.getOffsetForPosition(pos)
                                                val annotations = paragraphAnnotatedString.getStringAnnotations("CUE_INDEX", offset, offset)
                                                val targetIdx = annotations.firstOrNull()?.item?.toIntOrNull()
                                                if (targetIdx != null && targetIdx in cues.indices) {
                                                    onSeek(cues[targetIdx].startTimeMs)
                                                } else {
                                                    isImmersiveMode = !isImmersiveMode
                                                }
                                            }
                                        }
                                    },
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = (fontSizeSp * 0.95f).sp,
                                    lineHeight = (fontSizeSp * 1.6f).sp,
                                    textAlign = subtitleAlignment.align,
                                    lineBreak = LineBreak.Paragraph,
                                    hyphens = Hyphens.Auto,
                                    letterSpacing = (-0.05).sp,
                                    textIndent = paragraphIndent
                                ),
                                onTextLayout = { textLayoutResult = it }
                            )
                        }
                    }

                    // Book Footer Page Number (Classic book design)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "— Página ${(activeChunkIndex + 1)} de ${cueChunks.size.coerceAtLeast(1)} —",
                            color = textColorMuted.copy(alpha = 0.75f),
                            fontFamily = FontFamily.Serif,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }

                    // Active Translation Pill at bottom of book sheet (Keeps book text clean while giving live sync translation)
                    if (activeSecondaryCue != null && !showInlineTranslation) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = activeHighlightBg.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, activeHighlightBorder.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = activeHighlightBorder,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeSecondaryCue.text.replace("\r", " ").replace("\n", " ").trim(),
                                    color = textColorMain,
                                    fontSize = (fontSizeSp * 0.80f).sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    lineHeight = (fontSizeSp * 1.25f).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Floating Restore Button when in Immersive Mode
                if (isImmersiveMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = paperBorder.copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { isImmersiveMode = false }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Sair do Modo Imersivo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sair do Modo PDF",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Player Bar (Hidden in Immersive Mode for pure reading view)
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RetroAudioPlayerView(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onSeek = onSeek,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
    }
}

/**
 * Calculates the index of the active or nearest cue to display continuous 3 parts.
 */
private fun get3PartsCueIndex(cues: List<SubtitleCue>, currentMs: Long, offsetMs: Long): Int {
    if (cues.isEmpty()) return -1

    val activeIdx = cues.indexOfFirst { it.isActiveAt(currentMs, offsetMs) }
    if (activeIdx != -1) return activeIdx

    val lastStartedIdx = cues.indexOfLast { (it.startTimeMs + offsetMs) <= currentMs }
    if (lastStartedIdx != -1) return lastStartedIdx

    return 0
}
