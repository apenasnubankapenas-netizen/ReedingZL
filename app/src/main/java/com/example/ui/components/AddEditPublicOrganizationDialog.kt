package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.PublicOrganization

@Composable
fun AddEditPublicOrganizationDialog(
    initialOrganization: PublicOrganization? = null,
    onDismiss: () -> Unit,
    onSave: (PublicOrganization) -> Unit
) {
    var name by remember { mutableStateOf(initialOrganization?.name ?: "") }
    var orgType by remember { mutableStateOf(initialOrganization?.organizationType ?: "Órgão de Governo") }
    var description by remember { mutableStateOf(initialOrganization?.description ?: "") }
    var iconUri by remember { mutableStateOf(initialOrganization?.iconUri) }
    var iconPreset by remember { mutableStateOf(initialOrganization?.iconPreset ?: "DEFAULT") }
    var accentColorHex by remember { mutableStateOf(initialOrganization?.accentColorHex ?: "#0098DF") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            iconUri = uri.toString()
        }
    }

    val typeOptions = listOf(
        "Órgão de Governo",
        "Autarquia",
        "Fundação Pública",
        "Empresa Pública",
        "Tribunal / Judiciário",
        "Ministério Público"
    )

    val presetIcons = listOf(
        "DEFAULT" to "Padrão",
        "RECEITA" to "Receita Federal",
        "POLICIA_FEDERAL" to "Polícia Federal",
        "INSS" to "Previdência / INSS",
        "TCU" to "Tribunal de Contas",
        "TRIBUNAL" to "Justiça / Tribunais",
        "MINISTERIO" to "Ministérios",
        "UNIVERSIDADE" to "Universidades"
    )

    val colorOptions = listOf(
        "#0098DF" to "Azul Ciano",
        "#0EF3FF" to "Ciano Neon",
        "#FF2E97" to "Rosa Neon",
        "#FF2CF1" to "Magenta",
        "#0CB886" to "Esmeralda",
        "#0EE5C6" to "Verde Água",
        "#FFCC00" to "Amarelo",
        "#1E293B" to "Escuro",
        "#D97706" to "Laranja"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF030D22),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0EF3FF)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (initialOrganization == null) "Novo Órgão / Autarquia" else "Editar Órgão",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDFEFF)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFFFDFEFF).copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nome do Órgão
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Órgão ou Organização *", color = Color(0xFF0EF3FF)) },
                    placeholder = { Text("Ex: Receita Federal, IBGE, INSS...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFDFEFF),
                        unfocusedTextColor = Color(0xFFFDFEFF),
                        focusedBorderColor = Color(0xFF0EF3FF),
                        unfocusedBorderColor = Color(0xFF0098DF).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("org_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tipo de Organização
                Text(
                    text = "TIPO DE INSTITUIÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    typeOptions.take(3).forEach { type ->
                        val isSel = orgType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF1C1347) else Color(0xFF0A152E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) Color(0xFF0EF3FF) else Color(0xFF1C1347)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { orgType = type }
                        ) {
                            Text(
                                text = type,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color(0xFF0EF3FF) else Color(0xFFFDFEFF).copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    typeOptions.drop(3).forEach { type ->
                        val isSel = orgType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF1C1347) else Color(0xFF0A152E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) Color(0xFF0EF3FF) else Color(0xFF1C1347)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { orgType = type }
                        ) {
                            Text(
                                text = type,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color(0xFF0EF3FF) else Color(0xFFFDFEFF).copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Descrição
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição / Foco dos Editais", color = Color(0xFF0EE5C6)) },
                    placeholder = { Text("Ex: Editais para Auditor, Analista, carreiras fiscais...") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFDFEFF),
                        unfocusedTextColor = Color(0xFFFDFEFF),
                        focusedBorderColor = Color(0xFF0EE5C6),
                        unfocusedBorderColor = Color(0xFF0098DF).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Ícone Full HD ou Capa
                Text(
                    text = "ÍCONE FULL HD / CAPA DO ÓRGÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview do Ícone
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0A152E))
                            .border(1.5.dp, Color(0xFF0EF3FF), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!iconUri.isNullOrBlank()) {
                            AsyncImage(
                                model = iconUri,
                                contentDescription = "Ícone Selecionado",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = getIconForOrgPreset(iconPreset),
                                contentDescription = null,
                                tint = Color(0xFF0EF3FF),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0098DF)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFFFDFEFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escolher Imagem HD", fontSize = 12.sp, color = Color(0xFFFDFEFF))
                        }

                        if (!iconUri.isNullOrBlank()) {
                            TextButton(
                                onClick = { iconUri = null },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Remover imagem personalizada", fontSize = 11.sp, color = Color(0xFFE53935))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets Rápidos de Ícone
                Text(
                    text = "OU ESCOLHA UM ÍCONE PRÉ-DEFINIDO",
                    fontSize = 10.5.sp,
                    color = Color(0xFFFDFEFF).copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetIcons.take(4).forEach { (preset, label) ->
                        val isSel = iconPreset == preset && iconUri.isNullOrBlank()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF1C1347) else Color(0xFF0A152E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) Color(0xFFFF2E97) else Color(0xFF1C1347)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    iconPreset = preset
                                    iconUri = null
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = getIconForOrgPreset(preset),
                                    contentDescription = label,
                                    tint = if (isSel) Color(0xFFFF2E97) else Color(0xFFFDFEFF).copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    color = if (isSel) Color(0xFFFF2E97) else Color(0xFFFDFEFF).copy(alpha = 0.6f),
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cor de Destaque
                Text(
                    text = "COR DE IDENTIFICAÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.take(6).forEach { (hex, _) ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        val isSel = accentColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSel) 3.dp else 1.dp,
                                    color = if (isSel) Color(0xFFFDFEFF) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { accentColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF030D22),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botão Salvar
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val saved = (initialOrganization ?: PublicOrganization(name = name)).copy(
                                name = name.trim(),
                                organizationType = orgType,
                                description = description.trim(),
                                iconUri = iconUri,
                                iconPreset = iconPreset,
                                accentColorHex = accentColorHex
                            )
                            onSave(saved)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0CB886)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_org_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF030D22),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Salvar Órgão",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF030D22)
                    )
                }
            }
        }
    }
}
