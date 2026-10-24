package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformView(
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    playheadColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val barCount = 48
    val waveHeights = remember {
        List(barCount) { idx ->
            val factor = sin(idx * 0.35) * 0.4 + 0.6
            (0.2f + (0.75f * factor.toFloat())).coerceIn(0.15f, 0.95f)
        }
    }

    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val clickedRatio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((clickedRatio * durationMs).toLong())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = (canvasWidth / barCount) * 0.65f
            val barGap = (canvasWidth / barCount) * 0.35f

            val playedWidth = canvasWidth * progress

            for (i in 0 until barCount) {
                val xPos = i * (barWidth + barGap) + barGap / 2f
                val hFactor = waveHeights[i]
                val barH = canvasHeight * hFactor
                val yPos = (canvasHeight - barH) / 2f

                val isPlayed = xPos <= playedWidth
                val color = if (isPlayed) activeColor else inactiveColor

                drawRoundRect(
                    color = color,
                    topLeft = Offset(xPos, yPos),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Draw Playhead line
            drawRoundRect(
                color = playheadColor,
                topLeft = Offset(playedWidth - 2.dp.toPx(), 0f),
                size = Size(4.dp.toPx(), canvasHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
