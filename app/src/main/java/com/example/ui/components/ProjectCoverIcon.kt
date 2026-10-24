package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Modern project icon / cover widget.
 * Renders user's chosen custom photo from their device with smooth scaling,
 * or a modern gradient card fallback.
 */
@Composable
fun ProjectCoverIcon(
    customIconUri: String?,
    mediaType: String = "AUDIO",
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasCustomImage = !customIconUri.isNullOrBlank()

    val modernGradient = remember(mediaType) {
        if (mediaType == "VIDEO") {
            Brush.linearGradient(
                listOf(
                    Color(0xFFE91E63),
                    Color(0xFFFF5722),
                    Color(0xFFFF9800)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFF3F51B5),
                    Color(0xFF673AB7),
                    Color(0xFF2196F3)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            if (hasCustomImage) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(customIconUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Ícone Personalizado do Projeto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                )
            } else {
                // Modern gradient fallback with styled icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(modernGradient)
                        .clip(shape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mediaType == "VIDEO") Icons.Default.VideoLibrary else Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }
        }

        // Optional subtle overlay border for modern depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shape = shape
                )
        )

        // Edit badge
        if (showEditBadge) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size((size * 0.38f).coerceAtLeast(20.dp))
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Editar Ícone",
                        modifier = Modifier.size((size * 0.22f).coerceAtLeast(12.dp))
                    )
                }
            }
        }
    }
}

/**
 * Dialog to pick or change project cover icon.
 * User can pick ANY image from their mobile gallery/files, or choose a preset icon.
 */
@Composable
fun ProjectIconPickerDialog(
    currentIconUri: String?,
    projectTitle: String,
    mediaType: String = "AUDIO",
    onDismiss: () -> Unit,
    onSaveIcon: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf(currentIconUri) }

    // Launcher to select ANY image from the mobile phone
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ícone / Capa do Projeto",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Escolha qualquer imagem do seu celular para representar este projeto com design moderno.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Live Preview
                Box(contentAlignment = Alignment.Center) {
                    ProjectCoverIcon(
                        customIconUri = selectedUri,
                        mediaType = mediaType,
                        size = 96.dp,
                        shape = RoundedCornerShape(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!selectedUri.isNullOrBlank()) "✓ Imagem Personalizada Selecionada" else "Ícone Padrão Moderno",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!selectedUri.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Button: Choose Image from phone gallery
                Button(
                    onClick = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedUri.isNullOrBlank()) "Escolher Imagem do Celular" else "Trocar Imagem do Celular",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!selectedUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { selectedUri = null },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remover Imagem e Usar Padrão")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveIcon(selectedUri)
                    onDismiss()
                }
            ) {
                Text("Salvar Ícone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
