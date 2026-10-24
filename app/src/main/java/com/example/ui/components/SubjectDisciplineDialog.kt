package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AuthorCollection
import com.example.data.model.PdfBook
import com.example.data.model.SubjectDiscipline
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SubjectDisciplineDialog(
    subject: SubjectDiscipline? = null,
    allAuthors: List<AuthorCollection> = emptyList(),
    allBooks: List<PdfBook> = emptyList(),
    onDismiss: () -> Unit,
    onSaveSubject: (SubjectDiscipline, List<Int>, List<Int>) -> Unit,
    onDeleteSubject: ((SubjectDiscipline) -> Unit)? = null,
    onAddDirectBooks: ((Int, List<MainViewModel.MultiPdfBookItem>) -> Unit)? = null
) {
    var name by remember(subject) { mutableStateOf(subject?.name ?: "") }
    var description by remember(subject) { mutableStateOf(subject?.description ?: "") }
    var bannerColorHex by remember(subject) { mutableStateOf(subject?.bannerColorHex ?: "#1E3A8A") }
    var selectedAuthorIds by remember(subject) {
        mutableStateOf(subject?.getAuthorIds()?.toSet() ?: emptySet())
    }
    var selectedBookIds by remember(subject) {
        mutableStateOf(subject?.getDirectBookIds()?.toSet() ?: emptySet())
    }

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
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (subject == null) "Nova Disciplina" else "Editar Disciplina",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Disciplina / Matéria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição ou Ementa") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (subject != null && onDeleteSubject != null) {
                        TextButton(
                            onClick = { onDeleteSubject(subject) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excluir")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val saved = (subject ?: SubjectDiscipline(
                                    name = name.trim(),
                                    description = description.trim(),
                                    bannerColorHex = bannerColorHex
                                )).copy(
                                    name = name.trim(),
                                    description = description.trim(),
                                    bannerColorHex = bannerColorHex
                                )
                                onSaveSubject(saved, selectedAuthorIds.toList(), selectedBookIds.toList())
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}
