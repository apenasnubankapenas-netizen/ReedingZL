package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Dialog moderno e intuitivo para ajuste do tamanho da fonte da leitura nos Reels.
 * Oferece botões rápidos A- e A+, slider de ajuste contínuo, chips de tamanho pré-definido
 * e prévia em tempo real do parágrafo.
 */
@Composable
fun FontSizeDialog(
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    val presets = listOf(
        9f to "9",
        10f to "10",
        11f to "11",
        12f to "12",
        13f to "13",
        14f to "14 (Padrão)",
        16f to "16",
        18f to "18",
        20f to "20",
        22f to "22",
        26f to "26"
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF18181B),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tamanho da Fonte",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Leitura nos Reels",
                                fontSize = 11.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Current Size Display
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF27272A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tamanho Atual",
                            fontSize = 13.sp,
                            color = Color(0xFFA1A1AA)
                        )
                        Text(
                            text = if (fontSizeSp.toInt() == 14) "14 sp (Padrão)" else "${fontSizeSp.toInt()} sp",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF818CF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stepper Buttons (A- e A+) with Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button A- (Diminuir até 9)
                    FilledTonalButton(
                        onClick = {
                            val next = (fontSizeSp - 1f).coerceAtLeast(9f)
                            onFontSizeChange(next)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF27272A),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextDecrease,
                                contentDescription = "Diminuir Fonte",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("A-", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Slider
                    Slider(
                        value = fontSizeSp.coerceIn(9f, 36f),
                        onValueChange = { onFontSizeChange(it.toInt().toFloat()) },
                        valueRange = 9f..36f,
                        steps = 26,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF818CF8),
                            activeTrackColor = Color(0xFF6366F1),
                            inactiveTrackColor = Color(0xFF3F3F46)
                        )
                    )

                    // Button A+ (Aumentar)
                    FilledTonalButton(
                        onClick = {
                            val next = (fontSizeSp + 1f).coerceAtMost(36f)
                            onFontSizeChange(next)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF27272A),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextIncrease,
                                contentDescription = "Aumentar Fonte",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("A+", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets Chips in Smooth Scrollable LazyRow
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(presets) { (size, label) ->
                        val isSelected = fontSizeSp.toInt() == size.toInt()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF27272A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF818CF8) else Color(0xFF3F3F46)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFontSizeChange(size) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${size.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFFE4E4E7)
                                )
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFFA1A1AA)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF09090B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 120.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "📖 Leitura Principal:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "As grandes ideias transformam nosso entendimento do mundo com clareza e beleza.",
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.45f).sp,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Explicação com IA:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Esta formulação didática sintetiza os princípios fundamentais com absoluta clareza prática.",
                            fontSize = (fontSizeSp * 0.95f).coerceIn(11f, 32f).sp,
                            lineHeight = (fontSizeSp * 1.48f).coerceIn(16f, 48f).sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Color(0xFFD1D5DB),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Done Button
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Concluir",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
