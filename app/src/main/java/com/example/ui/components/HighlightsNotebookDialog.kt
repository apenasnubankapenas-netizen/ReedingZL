package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BookHighlight
import com.example.data.model.PdfBook

@Composable
fun HighlightsNotebookDialog(
    currentBook: PdfBook? = null,
    highlights: List<BookHighlight> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateToPage: ((Int, Int) -> Unit)? = null,
    onSpeakText: ((String) -> Unit)? = null,
    onSaveHighlight: ((BookHighlight) -> Unit)? = null,
    onDeleteHighlight: (BookHighlight) -> Unit,
    speechManager: com.example.data.speech.PdfSpeechManager? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Caderno de Destaques",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (highlights.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum destaque salvo ainda.\nSelecione um texto na leitura para destacar!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        items(highlights) { hl ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onNavigateToPage?.invoke(hl.bookId, hl.pageNumber)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (hl.bookTitle.isNotBlank()) "${hl.bookTitle} • Pág ${hl.pageNumber}" else "Página ${hl.pageNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = { onDeleteHighlight(hl) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    KaraokeDefinitionText(
                                        targetId = "highlight_quote_${hl.id}",
                                        text = hl.quoteText,
                                        speechManager = speechManager,
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        textColor = MaterialTheme.colorScheme.onSurface,
                                        showListenButton = true
                                    )
                                    if (hl.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        KaraokeDefinitionText(
                                            targetId = "highlight_note_${hl.id}",
                                            text = hl.note,
                                            speechManager = speechManager,
                                            accentColor = MaterialTheme.colorScheme.secondary,
                                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            prefixLabel = "Nota:",
                                            showListenButton = true
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
