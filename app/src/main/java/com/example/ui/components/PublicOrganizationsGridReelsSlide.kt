package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PublicOrganization

/**
 * Slide do Reels que exibe Órgãos de Governo, Autarquias e Organizações
 * de 4 em 4 na tela (Grade 2x2), similar ao slide de 4 capas de livros.
 */
@Composable
fun PublicOrganizationsGridReelsSlide(
    organizations: List<PublicOrganization>,
    readingTheme: String = "CATEGORY_COLOR",
    onSelectOrganization: (PublicOrganization) -> Unit,
    onAddNewOrganization: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 58.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
    ) {
        val displayOrgs = organizations.take(4)
        val rows = displayOrgs.chunked(2)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header da Grade
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0098DF).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EF3FF))
                    ) {
                        Text(
                            text = "ÓRGÃOS & AUTARQUIAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0EF3FF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Editais de Concurso",
                        fontSize = 12.sp,
                        color = Color(0xFFFDFEFF).copy(alpha = 0.75f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0CB886),
                    modifier = Modifier
                        .clickable { onAddNewOrganization() }
                        .testTag("add_organization_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar Órgão",
                            tint = Color(0xFF030D22),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Novo Órgão",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF030D22)
                        )
                    }
                }
            }

            rows.forEach { rowOrgs ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowOrgs.forEach { org ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            OrganizationGridCard(
                                organization = org,
                                readingTheme = readingTheme,
                                onSelect = { onSelectOrganization(org) }
                            )
                        }
                    }
                    if (rowOrgs.size == 1) {
                        // Card placeholder para adicionar novo órgão
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            AddOrganizationPlaceholderCard(
                                onClick = onAddNewOrganization
                            )
                        }
                    }
                }
            }
            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AddOrganizationPlaceholderCard(
                        onClick = onAddNewOrganization
                    )
                }
            } else if (rows.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun OrganizationGridCard(
    organization: PublicOrganization,
    readingTheme: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(organization.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(organization.accentColorHex))
        } catch (_: Exception) {
            Color(0xFF0098DF)
        }
    }

    val cardBg = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFF2A2117)
        "DARK_OLED" -> Color(0xFF111111)
        "CINEMA_YELLOW" -> Color(0xFF14130A)
        "CYBERPUNK_NEON" -> Color(0xFF0C1022)
        "STREAMING_BOX" -> Color(0xFF16192E)
        "TERMINAL_GREEN" -> Color(0xFF021406)
        else -> Color(0xFF0A152E)
    }

    val textCol = when (readingTheme) {
        "PAPER_SEPIA" -> Color(0xFFF7EED8)
        "CINEMA_YELLOW" -> Color(0xFFFFFAEB)
        "TERMINAL_GREEN" -> Color(0xFF80FF9F)
        else -> Color(0xFFFDFEFF)
    }

    val pdfCount = organization.getPdfBookIds().size

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .testTag("org_card_${organization.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Área da Capa Grande e Imersiva (Praticamente Idêntica à Capa dos Livros)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                if (!organization.iconUri.isNullOrBlank()) {
                    AsyncImage(
                        model = organization.iconUri,
                        contentDescription = "Capa de ${organization.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Capa em degradê institucional de alta fidelidade
                    val gradientColors = listOf(
                        accentColor.copy(alpha = 0.85f),
                        Color(0xFF030D22),
                        Color(0xFF000814)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(gradientColors))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val iconVector = getIconForOrgPreset(organization.iconPreset)
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = organization.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = organization.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Tag de Editais no canto superior direito (idêntica à tag de páginas dos livros)
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = if (pdfCount == 1) "1 Edital" else "$pdfCount Editais",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Tag do Tipo de Órgão no canto superior esquerdo (idêntica à tag de categoria dos livros)
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = Color(0xFF030D22).copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF0EF3FF).copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0EF3FF),
                            modifier = Modifier.size(5.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = organization.organizationType,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0EF3FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Nome do Órgão Grande e legível logo abaixo da capa
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = organization.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = organization.organizationType,
                    fontSize = 9.5.sp,
                    color = textCol.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Botão Toque para Ler / Ver Editais (idêntico ao dos livros)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.20f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Ver Editais",
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (pdfCount > 0) "Ler Editais ($pdfCount)" else "Vincular PDF",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun AddOrganizationPlaceholderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A152E).copy(alpha = 0.65f)),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0EE5C6).copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0EE5C6).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EE5C6)),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Órgão",
                        tint = Color(0xFF0EE5C6),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Novo Órgão",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0EE5C6),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Cadastre concursos & editais",
                fontSize = 9.5.sp,
                color = Color(0xFFFDFEFF).copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getIconForOrgPreset(preset: String): ImageVector {
    return when (preset.uppercase()) {
        "RECEITA" -> Icons.Default.AccountBalance
        "POLICIA_FEDERAL" -> Icons.Default.Security
        "INSS" -> Icons.Default.HealthAndSafety
        "TCU" -> Icons.Default.Gavel
        "TRIBUNAL" -> Icons.Default.Balance
        "MINISTERIO" -> Icons.Default.AccountBalance
        "UNIVERSIDADE" -> Icons.Default.School
        else -> Icons.Default.CorporateFare
    }
}
