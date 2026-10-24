package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun RetroAudioPlayerView(
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    playbackSpeed: Float = 1.0f,
    onChangeSpeed: ((Float) -> Unit)? = null,
    isSentenceLoop: Boolean = false,
    onToggleSentenceLoop: (() -> Unit)? = null,
    isAutoPause: Boolean = false,
    onToggleAutoPause: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Sleek Minimalist Black & White Player Frame
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A0A0A), // Pure minimalist black
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)) // Subtle crisp border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP HEADER: Minimalist Digital Time Display & Quick Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPlaying) "• REPRODUZINDO" else "PAUSADO",
                        color = if (isPlaying) Color.White else Color(0xFF888888),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (isSentenceLoop) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF333333)
                        ) {
                            Text("LOOP", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    if (isAutoPause) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF333333)
                        ) {
                            Text("DITADO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF141414),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text(
                        text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // MINIMALIST WAVEFORM VISUALIZER (B&W)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color(0xFF121212), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF222222), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                    val w = size.width
                    val h = size.height
                    val playedX = w * progress

                    val ticks = 36
                    val step = w / ticks
                    for (i in 0 until ticks) {
                        val x = i * step + step / 2
                        val amplitude = (0.25f + 0.75f * sin(i * 0.7).toFloat()).coerceIn(0.15f, 1f)
                        val tickH = (h * 0.75f * amplitude).coerceAtLeast(3f)
                        val isPlayed = x <= playedX

                        val color = if (isPlayed) Color.White else Color(0xFF444444)
                        val strokeWidth = if (isPlayed) 3.dp.toPx() else 2.dp.toPx()

                        drawLine(
                            color = color,
                            start = Offset(x, (h - tickH) / 2),
                            end = Offset(x, (h + tickH) / 2),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }

            // MINIMALIST B&W SLIDER
            Slider(
                value = progress,
                onValueChange = { newRatio ->
                    onSeek((newRatio * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            // CONTROLS ROW: Loop, -5s, Play/Pause, +5s, Auto-Pause, Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sentence Loop Toggle
                IconButton(
                    onClick = { onToggleSentenceLoop?.invoke() },
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (isSentenceLoop) Color.White else Color(0xFF161616), CircleShape)
                        .border(1.dp, if (isSentenceLoop) Color.White else Color(0xFF333333), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.RepeatOne,
                        contentDescription = "Loop da Frase",
                        tint = if (isSentenceLoop) Color.Black else Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Rewind 5s
                IconButton(
                    onClick = { onSeek((currentPositionMs - 5000L).coerceAtLeast(0L)) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1A1A1A), shape = CircleShape)
                        .border(1.dp, Color(0xFF333333), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Voltar 5s",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Fast Forward 5s
                IconButton(
                    onClick = { onSeek((currentPositionMs + 5000L).coerceAtMost(durationMs)) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1A1A1A), shape = CircleShape)
                        .border(1.dp, Color(0xFF333333), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Avançar 5s",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Auto-Pause (Dictation Mode) Toggle
                IconButton(
                    onClick = { onToggleAutoPause?.invoke() },
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (isAutoPause) Color.White else Color(0xFF161616), CircleShape)
                        .border(1.dp, if (isAutoPause) Color.White else Color(0xFF333333), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Pausa Automática ao Fim da Frase (Ditado)",
                        tint = if (isAutoPause) Color.Black else Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Playback Speed Selector
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF161616),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.clickable { showSpeedMenu = true }
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.5f, 0.75f, 0.85f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { spd ->
                            DropdownMenuItem(
                                text = { Text("${spd}x" + if (spd == playbackSpeed) " ✓" else "") },
                                onClick = {
                                    onChangeSpeed?.invoke(spd)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millisMs: Long): String {
    val totalSec = millisMs / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d", mins, secs)
}

