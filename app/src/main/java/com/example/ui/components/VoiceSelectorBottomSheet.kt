package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.speech.PdfSpeechManager
import com.example.data.speech.TtsVoiceInfo
import kotlin.math.abs

enum class VoiceFilterCategory(val label: String) {
    ALL("🌐 Todas"),
    PORTUGUESE("🇧🇷 Português"),
    ENGLISH("🇺🇸 English"),
    FRENCH("🇫🇷 Français"),
    SPANISH("🇪🇸 Español")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectorBottomSheet(
    speechManager: PdfSpeechManager,
    availableVoices: List<TtsVoiceInfo>,
    selectedVoice: TtsVoiceInfo?,
    readingTheme: String = "MONOCHROME_BW",
    onVoiceSelected: (TtsVoiceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = com.example.ui.theme.ReadingThemeManager.getPalette(readingTheme)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableStateOf(VoiceFilterCategory.PORTUGUESE) }
    val speechPitch by speechManager.speechPitch.collectAsState()
    val speechSpeed by speechManager.speechSpeed.collectAsState()

    val filteredVoices = remember(availableVoices, selectedCategory) {
        when (selectedCategory) {
            VoiceFilterCategory.ALL -> availableVoices
            VoiceFilterCategory.PORTUGUESE -> availableVoices.filter { it.isPortuguese }
            VoiceFilterCategory.ENGLISH -> availableVoices.filter { it.isEnglish }
            VoiceFilterCategory.FRENCH -> availableVoices.filter { it.isFrench }
            VoiceFilterCategory.SPANISH -> availableVoices.filter { it.isSpanish }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                color = palette.textSecondary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Header Compacto e Elegante
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = palette.buttonSecondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Vozes de Leitura Neural",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = selectedVoice?.displayName ?: "Selecione uma voz para ouvir",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Painel Compacto de Tom / Pitch
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Afinação vocal:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    speechPitch < 0.88f -> "Grave"
                                    speechPitch > 1.12f -> "Aguda"
                                    else -> "Natural"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        }

                        // Presets Rápidos inline ultra-compactos
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                Triple("Grave", 0.80f, "🧔"),
                                Triple("Natural", 1.0f, "🎙️"),
                                Triple("Aguda", 1.18f, "👧")
                            ).forEach { (label, pVal, emoji) ->
                                val isSelected = abs(speechPitch - pVal) < 0.06f
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) palette.accent else palette.surface,
                                    border = BorderStroke(1.dp, if (isSelected) palette.accent else palette.border),
                                    modifier = Modifier.clickable { speechManager.setPitch(pVal) }
                                ) {
                                    Text(
                                        text = "$emoji $label",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) palette.surface else palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Slider fino compacto
                    Slider(
                        value = speechPitch,
                        onValueChange = { speechManager.setPitch(it) },
                        valueRange = 0.65f..1.45f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Painel Compacto de Velocidade de Leitura
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Velocidade:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%.2fx".format(speechSpeed),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        }

                        // Presets Rápidos inline ultra-compactos
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                Triple("0.75x", 0.75f, "🐢"),
                                Triple("1.0x", 1.0f, "🎙️"),
                                Triple("1.25x", 1.25f, "⚡"),
                                Triple("1.5x", 1.5f, "🚀")
                            ).forEach { (label, sVal, emoji) ->
                                val isSelected = abs(speechSpeed - sVal) < 0.05f
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) palette.accent else palette.surface,
                                    border = BorderStroke(1.dp, if (isSelected) palette.accent else palette.border),
                                    modifier = Modifier.clickable { speechManager.setSpeed(sVal) }
                                ) {
                                    Text(
                                        text = "$emoji $label",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) palette.surface else palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Slider fino compacto
                    Slider(
                        value = speechSpeed,
                        onValueChange = { speechManager.setSpeed(it) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtros de Idioma com rolagem horizontal compacta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VoiceFilterCategory.values().forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    val count = when (cat) {
                        VoiceFilterCategory.ALL -> availableVoices.size
                        VoiceFilterCategory.PORTUGUESE -> availableVoices.count { it.isPortuguese }
                        VoiceFilterCategory.ENGLISH -> availableVoices.count { it.isEnglish }
                        VoiceFilterCategory.FRENCH -> availableVoices.count { it.isFrench }
                        VoiceFilterCategory.SPANISH -> availableVoices.count { it.isSpanish }
                    }

                    FilterChip(
                        selected = isCatSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = "${cat.label} ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.buttonSecondaryContainer,
                            selectedLabelColor = palette.accent,
                            containerColor = palette.surfaceElevated,
                            labelColor = palette.textSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCatSelected) palette.accent else palette.border
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Vozes Adaptativa (sem altura fixa gigante)
            if (filteredVoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma voz disponível nesta categoria.",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 290.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(filteredVoices, key = { it.id }) { voiceInfo ->
                        val isSelected = selectedVoice?.id == voiceInfo.id

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) palette.surfaceElevated else palette.surfaceVariant
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) palette.accent else palette.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onVoiceSelected(voiceInfo)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Indicador circular de seleção
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = if (isSelected) palette.accent else palette.surface,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selecionada",
                                            tint = palette.surface,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Dados da Voz
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = voiceInfo.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Badge de Idioma
                                        val langBadge = when {
                                            voiceInfo.isPortuguese -> "PT-BR"
                                            voiceInfo.isFrench -> "FR"
                                            voiceInfo.isEnglish -> "EN"
                                            voiceInfo.isSpanish -> "ES"
                                            else -> voiceInfo.locale.language.uppercase()
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = palette.badgeContainer
                                        ) {
                                            Text(
                                                text = langBadge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.accent,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        // Badge de Gênero
                                        if (voiceInfo.isMale) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = palette.surfaceElevated
                                            ) {
                                                Text(
                                                    text = "Masculina",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        } else if (voiceInfo.isFemale) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = palette.surfaceElevated
                                            ) {
                                                Text(
                                                    text = "Feminina",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // Badge Neural
                                        if (voiceInfo.isNeural) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = palette.buttonSecondaryContainer
                                            ) {
                                                Text(
                                                    text = "Neural",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.accent,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = palette.accent.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "● ATIVA",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = palette.accent,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Botão de Testar Amostra
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) palette.buttonSecondaryContainer else palette.surfaceElevated,
                                    border = BorderStroke(1.dp, palette.border),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            speechManager.testVoice(voiceInfo)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Ouvir Amostra",
                                            tint = if (isSelected) palette.accent else palette.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão Concluir Seleção
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.buttonContainer,
                    contentColor = palette.buttonContent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "Concluir Seleção",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rodapé com atalho discreto para Configurações de TTS do Android
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent("com.android.settings.TTS_SETTINGS")
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = palette.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gerenciar ou baixar vozes no Android",
                    color = palette.textSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
