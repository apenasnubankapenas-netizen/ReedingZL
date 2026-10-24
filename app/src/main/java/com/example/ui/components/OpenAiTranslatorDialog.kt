package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.parser.SrtParser
import com.example.data.translator.OpenAiTranslator
import kotlinx.coroutines.launch

data class SrtFileItem(
    val fileName: String,
    val content: String,
    val cueCount: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenAiTranslatorDialog(
    currentProjectSrt: String? = null,
    currentProjectTitle: String? = null,
    onDismiss: () -> Unit,
    onAttachAsSecondary: (translatedSrt: String) -> Unit,
    onCreateNewProject: (title: String, translatedSrt: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("openai_translator_prefs", Context.MODE_PRIVATE) }

    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    val popularLanguages = listOf(
        "Português (Brasil)",
        "Inglês",
        "Espanhol",
        "Francês",
        "Alemão",
        "Italiano",
        "Japonês",
        "Chinês",
        "Coreano",
        "Russo"
    )

    var targetLanguage by remember { mutableStateOf(prefs.getString("target_lang", "Português (Brasil)") ?: "Português (Brasil)") }
    var customLanguage by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini") }

    val selectedFiles = remember { mutableStateListOf<SrtFileItem>() }

    // Translation state
    var isTranslating by remember { mutableStateOf(false) }
    var currentFileIndex by remember { mutableStateOf(0) }
    var currentProgressText by remember { mutableStateOf("") }
    var currentProgressFraction by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var translatedResults = remember { mutableStateListOf<Pair<String, String>>() } // Pair<fileName/Title, translatedSrt>

    // Initialize with current project if available
    LaunchedEffect(currentProjectSrt) {
        if (!currentProjectSrt.isNullOrBlank() && selectedFiles.isEmpty()) {
            val cues = SrtParser.parseSrt(currentProjectSrt)
            val name = currentProjectTitle?.let { "$it (Atual)" } ?: "Projeto Atual"
            selectedFiles.add(SrtFileItem(fileName = name, content = currentProjectSrt, cueCount = cues.size))
        }
    }

    // Multiple file launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val text = stream.bufferedReader().readText()
                        val fileName = getFileNameFromUri(context, uri) ?: "Legenda.srt"
                        val cues = SrtParser.parseSrt(text)
                        if (cues.isNotEmpty()) {
                            selectedFiles.add(SrtFileItem(fileName = fileName, content = text, cueCount = cues.size))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isTranslating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F0F0F),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
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
                            color = Color.White,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "TRADUTOR IA OPENAI",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Sincronização frame a frame idêntica",
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isTranslating) onDismiss() },
                        enabled = !isTranslating
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF262626))
                Spacer(modifier = Modifier.height(16.dp))

                if (translatedResults.isNotEmpty()) {
                    // TRANSLATION COMPLETED RESULTS VIEW
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tradução concluída com sucesso!",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    translatedResults.forEachIndexed { index, pair ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = pair.first,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (currentProjectSrt != null && index == 0) {
                                        Button(
                                            onClick = {
                                                onAttachAsSecondary(pair.second)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Definir como 2ª Legenda", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val title = pair.first.removeSuffix(" (Atual)") + " ($targetLanguage)"
                                            onCreateNewProject(title, pair.second)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF262626),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Criar Novo Projeto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { translatedResults.clear() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF262626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Traduzir Outros Arquivos", fontWeight = FontWeight.Bold)
                    }

                } else {
                    // CONFIGURATION VIEW

                    // 1. OPENAI API KEY INPUT
                    Text(
                        text = "1. CHAVE DA API DA OPENAI",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.edit().putString("api_key", it).apply()
                        },
                        placeholder = { Text("sk-proj-...", color = Color(0xFF666666)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color.White)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedContainerColor = Color(0xFF141414),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. MODEL SELECTOR
                    Text(
                        text = "2. MODELO DA OPENAI",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo").forEach { model ->
                            val isSelected = selectedModel == model
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White else Color(0xFF1A1A1A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333333)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedModel = model
                                        prefs.edit().putString("model", model).apply()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = model,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. TARGET LANGUAGE
                    Text(
                        text = "3. IDIOMA DE DESTINO",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        popularLanguages.forEach { lang ->
                            val isSelected = targetLanguage == lang
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color.White else Color(0xFF181818),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF333333)),
                                modifier = Modifier.clickable {
                                    targetLanguage = lang
                                    prefs.edit().putString("target_lang", lang).apply()
                                }
                            ) {
                                Text(
                                    text = lang,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customLanguage,
                        onValueChange = {
                            customLanguage = it
                            if (it.isNotBlank()) {
                                targetLanguage = it.trim()
                            }
                        },
                        placeholder = { Text("Ou digite outro idioma (ex: Holandês)...", color = Color(0xFF666666)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedContainerColor = Color(0xFF141414),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. ARQUIVOS SRT SELECIONADOS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. ARQUIVOS SRT PARA TRADUZIR (${selectedFiles.size})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF262626),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Selecionar SRTs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedFiles.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF141414),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Nenhum arquivo SRT selecionado. Clique no botão acima para escolher um ou múltiplos arquivos SRT.",
                                color = Color(0xFF888888),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        selectedFiles.forEachIndexed { idx, file ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF181818),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Subtitles,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = file.fileName,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${file.cueCount} quadros de legenda",
                                                color = Color(0xFF888888),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { selectedFiles.removeAt(idx) },
                                        enabled = !isTranslating
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover",
                                            tint = Color(0xFF888888),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ERROR MESSAGE
                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3F1010),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF882222)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFF8888),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // TRANSLATING PROGRESS STATUS
                    if (isTranslating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = currentProgressFraction,
                                color = Color.White,
                                trackColor = Color(0xFF333333),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentProgressText,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // START TRANSLATION BUTTON
                        Button(
                            onClick = {
                                if (apiKey.isBlank()) {
                                    errorMessage = "Insira sua Chave da API da OpenAI acima."
                                    return@Button
                                }
                                if (selectedFiles.isEmpty()) {
                                    errorMessage = "Selecione pelo menos um arquivo SRT para traduzir."
                                    return@Button
                                }

                                errorMessage = null
                                isTranslating = true
                                translatedResults.clear()

                                scope.launch {
                                    var hasError = false
                                    selectedFiles.forEachIndexed { fIdx, file ->
                                        currentFileIndex = fIdx
                                        currentProgressText = "Traduzindo ${file.fileName} (${fIdx + 1}/${selectedFiles.size})..."
                                        currentProgressFraction = 0f

                                        val res = OpenAiTranslator.translateSrt(
                                            apiKey = apiKey,
                                            model = selectedModel,
                                            targetLanguage = targetLanguage,
                                            srtContent = file.content,
                                            onProgress = { done, total ->
                                                val frac = if (total > 0) done.toFloat() / total.toFloat() else 0f
                                                currentProgressFraction = frac
                                                currentProgressText = "Traduzindo ${file.fileName}: $done/$total quadros"
                                            }
                                        )

                                        if (res.isSuccess) {
                                            val translated = res.getOrThrow()
                                            translatedResults.add(Pair(file.fileName, translated))
                                        } else {
                                            hasError = true
                                            errorMessage = "Erro em ${file.fileName}: ${res.exceptionOrNull()?.message}"
                                        }
                                    }

                                    isTranslating = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INICIAR TRADUÇÃO IA", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
