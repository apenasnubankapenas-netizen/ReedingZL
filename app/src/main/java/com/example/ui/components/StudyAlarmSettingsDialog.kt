package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.alarm.StudyAlarmSettings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudyAlarmSettingsDialog(
    currentSettings: StudyAlarmSettings,
    onDismiss: () -> Unit,
    onSaveSettings: (
        enabled: Boolean,
        intervalMinutes: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        customAudioUri: String?,
        customAudioName: String?
    ) -> Unit,
    onTestAlarmNow: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(currentSettings.isEnabled) }
    var intervalMinutes by remember { mutableStateOf(currentSettings.intervalMinutes) }
    var soundEnabled by remember { mutableStateOf(currentSettings.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(currentSettings.vibrationEnabled) }
    var customAudioUri by remember { mutableStateOf(currentSettings.customAudioUri) }
    var customAudioName by remember { mutableStateOf(currentSettings.customAudioName) }
    var customMinutesText by remember { mutableStateOf(currentSettings.intervalMinutes.toString()) }

    var isPlayingPreview by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {}
        previewPlayer = null
        isPlayingPreview = false
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            stopPreview()
            customAudioUri = uri.toString()
            val resolvedName = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIdx)
                    } else null
                }
            } catch (_: Exception) {
                null
            } ?: uri.lastPathSegment ?: "Áudio Escolhido"
            customAudioName = resolvedName
        }
    }

    val presetIntervals = listOf(15, 30, 45, 60, 90, 120)

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF030D22), // Cyber background
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0EF3FF)), // Neon cyan
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(4.dp)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF2E97).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF2E97)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Alarme de Estudo",
                                    tint = Color(0xFFFF2E97),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Alarme de Estudo",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDFEFF)
                            )
                            Text(
                                text = "Lembrete periódico aleatório",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0EF3FF)
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
                            tint = Color(0xFFFDFEFF).copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Toggle Geral de Ativação
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) Color(0xFF1C1347) else Color(0xFF0A152E)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isEnabled) Color(0xFFFF2CF1) else Color(0xFF0098DF).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnabled) "Alarme Ativado" else "Alarme Desativado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) Color(0xFFFF2CF1) else Color(0xFFFDFEFF).copy(alpha = 0.8f)
                            )
                            Text(
                                text = if (isEnabled)
                                    "Avisará a cada $intervalMinutes min sorteando um livro onde você parou"
                                else
                                    "Ative para ser lembrado de continuar seus estudos",
                                fontSize = 11.5.sp,
                                color = Color(0xFFFDFEFF).copy(alpha = 0.65f)
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFDFEFF),
                                checkedTrackColor = Color(0xFFFF2E97),
                                uncheckedThumbColor = Color(0xFF888888),
                                uncheckedTrackColor = Color(0xFF1C1347)
                            ),
                            modifier = Modifier.testTag("study_alarm_enable_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seção: Intervalo de Tempo
                Text(
                    text = "INTERVALO ENTRE LEMBRETES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00), // Amarelo de referência
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets rápidos (15m, 30m, 45m, 60m, 90m, 120m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetIntervals.take(3).forEach { minutes ->
                        val isSelected = intervalMinutes == minutes
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0098DF) else Color(0xFF1C1347),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF0EF3FF) else Color(0xFF0098DF).copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    intervalMinutes = minutes
                                    customMinutesText = minutes.toString()
                                }
                        ) {
                            Text(
                                text = "${minutes}m",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFFDFEFF) else Color(0xFFFDFEFF).copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetIntervals.drop(3).forEach { minutes ->
                        val isSelected = intervalMinutes == minutes
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0098DF) else Color(0xFF1C1347),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF0EF3FF) else Color(0xFF0098DF).copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    intervalMinutes = minutes
                                    customMinutesText = minutes.toString()
                                }
                        ) {
                            Text(
                                text = "${minutes}m",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFFDFEFF) else Color(0xFFFDFEFF).copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Campo para intervalo customizado (qualquer número de minutos)
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        customMinutesText = filtered
                        filtered.toIntOrNull()?.let {
                            if (it in 1..1440) {
                                intervalMinutes = it
                            }
                        }
                    },
                    label = { Text("Personalizar minutos (ex: 25, 30, 40, 50...)", fontSize = 11.sp, color = Color(0xFF0EE5C6)) },
                    trailingIcon = {
                        Text("minutos", fontSize = 11.sp, color = Color(0xFF0EE5C6), modifier = Modifier.padding(end = 12.dp))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFDFEFF),
                        unfocusedTextColor = Color(0xFFFDFEFF),
                        focusedBorderColor = Color(0xFF0EF3FF),
                        unfocusedBorderColor = Color(0xFF0098DF).copy(alpha = 0.5f),
                        cursorColor = Color(0xFF0EF3FF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_alarm_minutes_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Opções de Tocar / Avisar
                Text(
                    text = "FORMA DE AVISO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A152E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1347)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = Color(0xFF0CB886),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Tocar som do alarme",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFDFEFF)
                                )
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { soundEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF0CB886)
                                )
                            )
                        }

                        if (soundEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF071126),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EF3FF).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = null,
                                            tint = Color(0xFF0EF3FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Áudio do Alarme",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0EF3FF)
                                            )
                                            Text(
                                                text = customAudioName ?: "Padrão do Sistema (Toque de Alarme)",
                                                fontSize = 11.5.sp,
                                                color = Color.White.copy(alpha = 0.9f),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Escolher Áudio Próprio
                                        Button(
                                            onClick = {
                                                audioPickerLauncher.launch("audio/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LibraryMusic,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (customAudioUri != null) "Trocar Áudio" else "Meu Áudio",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }

                                        // Ouvir Áudio / Parar
                                        OutlinedButton(
                                            onClick = {
                                                if (isPlayingPreview) {
                                                    stopPreview()
                                                } else {
                                                    try {
                                                        stopPreview()
                                                        val uriStr = customAudioUri
                                                        val player = MediaPlayer()
                                                        if (!uriStr.isNullOrBlank()) {
                                                            player.setDataSource(context, Uri.parse(uriStr))
                                                        } else {
                                                            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                                                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                                            player.setDataSource(context, defaultUri)
                                                        }
                                                        player.setAudioAttributes(
                                                            AudioAttributes.Builder()
                                                                .setUsage(AudioAttributes.USAGE_ALARM)
                                                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                                .build()
                                                        )
                                                        player.setOnCompletionListener {
                                                            stopPreview()
                                                        }
                                                        player.prepare()
                                                        player.start()
                                                        previewPlayer = player
                                                        isPlayingPreview = true
                                                    } catch (e: Exception) {
                                                        stopPreview()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isPlayingPreview) Color(0xFFFF2E97) else Color(0xFF0EF3FF)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (isPlayingPreview) Color(0xFFFF2E97) else Color(0xFF0EF3FF),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isPlayingPreview) "Parar" else "Ouvir",
                                                fontSize = 11.5.sp,
                                                color = if (isPlayingPreview) Color(0xFFFF2E97) else Color(0xFF0EF3FF)
                                            )
                                        }

                                        // Restaurar Padrão se tiver áudio próprio
                                        if (customAudioUri != null) {
                                            IconButton(
                                                onClick = {
                                                    stopPreview()
                                                    customAudioUri = null
                                                    customAudioName = null
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RestartAlt,
                                                    contentDescription = "Restaurar padrão",
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = Color(0xFF1C1347), modifier = Modifier.padding(vertical = 6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = Color(0xFF0EE5C6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Vibrar / Notificar no aparelho",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFDFEFF)
                                )
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { vibrationEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF0EE5C6)
                                )
                            )
                        }
                    }
                }

                // Status de Próximo Alarme se ativado
                if (isEnabled && currentSettings.nextTriggerAt > System.currentTimeMillis()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1C1347),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EF3FF).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF0EF3FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val nextTime = Date(currentSettings.nextTriggerAt)
                            Text(
                                text = "Próximo disparo programado para às ${dateFormat.format(nextTime)}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0EF3FF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botão Testar Alarme Imediatamente
                OutlinedButton(
                    onClick = onTestAlarmNow,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2E97)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_study_alarm_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFFF2E97),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Testar Alarme Agora (Sortear Livro)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF2E97)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Salvar Configurações
                Button(
                    onClick = {
                        stopPreview()
                        val finalInterval = customMinutesText.toIntOrNull() ?: intervalMinutes
                        onSaveSettings(
                            isEnabled,
                            finalInterval.coerceAtLeast(1),
                            soundEnabled,
                            vibrationEnabled,
                            customAudioUri,
                            customAudioName
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0CB886)), // Verde esmeralda
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_study_alarm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF030D22),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Salvar Configurações",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF030D22)
                    )
                }
            }
        }
    }
}
