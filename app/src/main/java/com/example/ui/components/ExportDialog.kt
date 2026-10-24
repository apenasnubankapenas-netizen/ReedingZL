package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubtitleCue
import com.example.data.parser.SrtParser

@Composable
fun ExportDialog(
    projectTitle: String,
    cues: List<SubtitleCue>,
    secondaryCues: List<SubtitleCue> = emptyList(),
    offsetMs: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exportedSrt = remember(cues, offsetMs) { SrtParser.exportSrt(cues, offsetMs) }
    val capCutFormat = remember(cues, offsetMs) { SrtParser.exportCapCutFormat(cues, offsetMs) }

    // Bilingual study booklet format
    val studyBookletFormat = remember(cues, secondaryCues) {
        val sb = StringBuilder()
        sb.append("=== $projectTitle ===\n\n")
        cues.forEachIndexed { index, cue ->
            sb.append("${cue.text}\n")
            val sec = secondaryCues.getOrNull(index)
            if (sec != null && sec.text.isNotBlank()) {
                sb.append("→ ${sec.text}\n")
            }
            sb.append("\n")
        }
        sb.toString().trim()
    }

    // Flashcards / Anki CSV format (Front [tab] Back)
    val ankiCsvFormat = remember(cues, secondaryCues) {
        val sb = StringBuilder()
        cues.forEachIndexed { index, cue ->
            val sec = secondaryCues.getOrNull(index)?.text ?: ""
            sb.append("${cue.text}\t$sec\n")
        }
        sb.toString().trim()
    }

    // Clean plain text without timestamps
    val plainTextFormat = remember(cues) {
        cues.joinToString(" ") { it.text }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Exportar & Compartilhar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ajuste aplicado: $offsetMs ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: Livreto de Estudo Bilíngue
                ExportOptionCard(
                    title = "📖 Livreto de Estudo / Leitura",
                    subtitle = "Texto formatado com tradução interlinear para imprimir ou ler.",
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, studyBookletFormat)
                            putExtra(Intent.EXTRA_SUBJECT, "$projectTitle - Estudo.txt")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Livreto"))
                    }
                )

                // Option 2: Flashcards Anki / CSV
                ExportOptionCard(
                    title = "🎴 Flashcards Anki / Quizlet (CSV)",
                    subtitle = "Tabela tabulada [Frase Original + Tradução] pronta para Anki.",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Flashcards Anki", ankiCsvFormat))
                        Toast.makeText(context, "Flashcards copiados para o Anki!", Toast.LENGTH_SHORT).show()
                    }
                )

                // Option 3: Arquivo SRT Padrão
                ExportOptionCard(
                    title = "📄 Arquivo de Legenda (.srt)",
                    subtitle = "Padrão universal compatível com players de vídeo (VLC, YouTube, etc).",
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, exportedSrt)
                            putExtra(Intent.EXTRA_SUBJECT, "$projectTitle.srt")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartilhar $projectTitle.srt"))
                    }
                )

                // Option 4: Formato CapCut / Premiere
                ExportOptionCard(
                    title = "🎬 CapCut / Premiere / Reels",
                    subtitle = "Texto com marcadores de tempo [00:00 - 00:05] para editores de vídeo.",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("CapCut Roteiro", capCutFormat))
                        Toast.makeText(context, "Marcadores CapCut copiados com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                )

                // Option 5: Texto Corrido Limpo
                ExportOptionCard(
                    title = "📝 Texto Contínuo Limpo",
                    subtitle = "Apenas as frases unidas em texto puro, sem marcações de tempo.",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Texto Limpo", plainTextFormat))
                        Toast.makeText(context, "Texto corrido copiado!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Concluído")
            }
        }
    )
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
