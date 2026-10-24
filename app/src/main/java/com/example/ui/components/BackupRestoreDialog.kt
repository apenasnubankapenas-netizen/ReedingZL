package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Folder
import com.example.data.model.Project
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun BackupRestoreDialog(
    projects: List<Project>,
    folders: List<Folder>,
    onRestoreBackup: (List<Project>, List<Folder>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRestoreMode by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    val backupJson = remember(projects, folders) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val foldersArray = JSONArray()
        folders.forEach { f ->
            val fo = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("colorHex", f.colorHex)
            }
            foldersArray.put(fo)
        }
        root.put("folders", foldersArray)

        val projectsArray = JSONArray()
        projects.forEach { p ->
            val po = JSONObject().apply {
                put("title", p.title)
                put("folderId", p.folderId ?: JSONObject.NULL)
                put("mediaType", p.mediaType)
                put("mediaUri", p.mediaUri ?: JSONObject.NULL)
                put("srtContent", p.srtContent)
                put("secondarySrtContent", p.secondarySrtContent ?: JSONObject.NULL)
                put("timeOffsetMs", p.timeOffsetMs)
                put("fontSizeSp", p.fontSizeSp.toDouble())
                put("customIconUri", p.customIconUri ?: JSONObject.NULL)
            }
            projectsArray.put(po)
        }
        root.put("projects", projectsArray)

        root.toString(2)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRestoreMode) Icons.Default.CloudDownload else Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isRestoreMode) "Restaurar Backup" else "Backup da Biblioteca",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRestoreMode) "Cole o JSON para recuperar seu acervo" else "Exporte ou salve seus projetos em JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isRestoreMode) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Resumo do Backup:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• ${projects.size} Áudios / Projetos\n• ${folders.size} Pastas\n• Legendas principais e secundárias incluídas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FilledTonalButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, backupJson)
                                putExtra(Intent.EXTRA_SUBJECT, "Backup_SubtitleSync.json")
                                type = "application/json"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Backup")
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartilhar Arquivo JSON")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("Backup JSON", backupJson))
                            Toast.makeText(context, "Backup copiado para a Área de Transferência!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar JSON para Área de Transferência")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { isRestoreMode = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deseja restaurar um backup existente?")
                    }
                } else {
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        label = { Text("Conteúdo JSON de Backup") },
                        placeholder = { Text("Cole aqui o texto JSON de backup...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clip.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!item.isNullOrBlank()) {
                                    restoreJsonText = item
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Colar")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                try {
                                    val obj = JSONObject(restoreJsonText)
                                    val importedFolders = mutableListOf<Folder>()
                                    if (obj.has("folders")) {
                                        val fArray = obj.getJSONArray("folders")
                                        for (i in 0 until fArray.length()) {
                                            val fo = fArray.getJSONObject(i)
                                            importedFolders.add(
                                                Folder(
                                                    name = fo.getString("name"),
                                                    colorHex = fo.optString("colorHex", "#4F46E5")
                                                )
                                            )
                                        }
                                    }

                                    val importedProjects = mutableListOf<Project>()
                                    if (obj.has("projects")) {
                                        val pArray = obj.getJSONArray("projects")
                                        for (i in 0 until pArray.length()) {
                                            val po = pArray.getJSONObject(i)
                                            importedProjects.add(
                                                Project(
                                                    title = po.getString("title"),
                                                    mediaType = po.optString("mediaType", "AUDIO"),
                                                    mediaUri = if (po.isNull("mediaUri")) null else po.optString("mediaUri"),
                                                    srtContent = po.getString("srtContent"),
                                                    secondarySrtContent = if (po.isNull("secondarySrtContent")) null else po.optString("secondarySrtContent"),
                                                    timeOffsetMs = po.optLong("timeOffsetMs", 0L),
                                                    fontSizeSp = po.optDouble("fontSizeSp", 24.0).toFloat(),
                                                    customIconUri = if (po.isNull("customIconUri")) null else po.optString("customIconUri")
                                                )
                                            )
                                        }
                                    }

                                    if (importedProjects.isNotEmpty() || importedFolders.isNotEmpty()) {
                                        onRestoreBackup(importedProjects, importedFolders)
                                        Toast.makeText(context, "${importedProjects.size} projetos restaurados!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Nenhum projeto encontrado no JSON", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao analisar JSON: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restaurar")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = { isRestoreMode = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Voltar para Exportação")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Fechar")
            }
        }
    )
}
