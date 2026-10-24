package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookQuestion
import com.example.util.TextFormatUtils

/**
 * Card de Questão Interativa de Padrão Profissional (Design Concurso & Editorial).
 * Apresenta tipografia de alta legibilidade, feedback tátil (haptic), animações suaves
 * nas alternativas e card educativo de resolução comentada.
 */
@Composable
fun InteractiveQuestionCard(
    question: BookQuestion,
    questionNumber: Int? = null,
    bookTitle: String? = null,
    fontSizeMultiplier: Float = 1.0f,
    onAnswerSelected: (selectedIndex: Int, isCorrect: Boolean) -> Unit,
    onResetAnswer: () -> Unit = {},
    onDeleteQuestion: (() -> Unit)? = null,
    onSpeakText: ((String) -> Unit)? = null,
    onOpenBookAtPage: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val options = question.getOptions()

    // Estado local para feedback instantâneo de latência zero
    var localSelectedAnswer by remember(question.id, question.isAnswered, question.userAnswerIndex) {
        mutableStateOf(question.userAnswerIndex)
    }
    var localIsAnswered by remember(question.id, question.isAnswered) {
        mutableStateOf(question.isAnswered)
    }
    val effectiveIsAnswered = localIsAnswered || question.isAnswered
    val effectiveSelectedIndex = localSelectedAnswer ?: question.userAnswerIndex
    val effectiveIsCorrect = if (effectiveIsAnswered) {
        effectiveSelectedIndex == question.correctOptionIndex
    } else {
        null
    }

    // Normaliza sigla da banca examinadora (impede que nomes longos de matéria deformem o cabeçalho)
    val displayInstitution = remember(question.institution) {
        TextFormatUtils.cleanInstitution(question.institution)
    }

    // Cores autênticas por Banca Examinadora
    val institutionColor = when (displayInstitution) {
        "FGV" -> Color(0xFFD97706) // Ouro/Âmbar FGV
        "CEBRASPE", "CESPE" -> Color(0xFF2563EB) // Azul Real Cebraspe
        "FCC" -> Color(0xFF7C3AED) // Roxo Intenso FCC
        "VUNESP" -> Color(0xFF059669) // Esmeralda VUNESP
        "CESGRANRIO" -> Color(0xFFE11D48) // Vermelho Carmim CESGRANRIO
        "QUADRIX" -> Color(0xFF0891B2) // Ciano QUADRIX
        "AOCP", "INSTITUTO AOCP", "INST_AOCP" -> Color(0xFF4F46E5) // Índigo AOCP
        "IADES", "EADES" -> Color(0xFF0D9488) // Teal IADES
        "MISTA", "TODAS" -> Color(0xFFF59E0B) // Dourado
        else -> Color(0xFF8B5CF6)
    }

    val difficultyColor = when (question.difficulty.uppercase()) {
        "EASY" -> Color(0xFF10B981) // Verde
        "MEDIUM" -> Color(0xFFF59E0B) // Âmbar
        "HARD" -> Color(0xFFEA580C) // Laranja
        "INSANE" -> Color(0xFFDC2626) // Vermelho Insano
        else -> Color(0xFF94A3B8)
    }

    val difficultyLabel = when (question.difficulty.uppercase()) {
        "EASY" -> "Fácil"
        "MEDIUM" -> "Média"
        "HARD" -> "Difícil"
        "INSANE" -> "Nível FGV"
        else -> "Padrão"
    }

    // Card principal com contorno dinâmico
    val cardBorderColor by animateColorAsState(
        targetValue = when {
            effectiveIsAnswered && effectiveIsCorrect == true -> Color(0xFF10B981).copy(alpha = 0.75f)
            effectiveIsAnswered && effectiveIsCorrect == false -> Color(0xFFEF4444).copy(alpha = 0.75f)
            else -> Color(0xFF332948)
        },
        animationSpec = spring(),
        label = "cardBorderColor"
    )

    val cardBgGradient = remember(effectiveIsAnswered, effectiveIsCorrect) {
        when {
            effectiveIsAnswered && effectiveIsCorrect == true -> Brush.verticalGradient(
                listOf(Color(0xFF0D251D), Color(0xFF121024), Color(0xFF0E0C1C))
            )
            effectiveIsAnswered && effectiveIsCorrect == false -> Brush.verticalGradient(
                listOf(Color(0xFF2B1017), Color(0xFF121024), Color(0xFF0E0C1C))
            )
            else -> Brush.verticalGradient(
                listOf(Color(0xFF17132B), Color(0xFF120F22), Color(0xFF0D0B1A))
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorderColor),
        shadowElevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .background(cardBgGradient, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp)
        ) {
            // =================================================================
            // 1. HEADER: Metadados, Banca, Dificuldade e Ações de Áudio/Cópia
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges da questão
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (questionNumber != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.25f),
                            border = BorderStroke(0.8.dp, Color(0xFF818CF8).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Q$questionNumber",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC7D2FE),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Badge da Banca
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = institutionColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, institutionColor.copy(alpha = 0.7f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = institutionColor,
                                modifier = Modifier.size(5.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayInstitution,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Badge de Dificuldade
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = difficultyColor.copy(alpha = 0.16f),
                        border = BorderStroke(0.7.dp, difficultyColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = difficultyLabel,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = difficultyColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }

                    // Tipo de questão
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Text(
                            text = if (question.questionType == "TRUE_FALSE") "C/E" else "Múltipla",
                            fontSize = 8.5.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                // Ações Rápidas do Topo (Áudio, Copiar, Reset, Deletar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Ouvir enunciado
                    if (onSpeakText != null) {
                        IconButton(
                            onClick = { onSpeakText(question.statement) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Ouvir Enunciado",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Copiar questão completa
                    IconButton(
                        onClick = {
                            val optionsFormatted = options.mapIndexed { idx, opt ->
                                val letter = ('A' + idx)
                                "$letter) $opt"
                            }.joinToString("\n")
                            val textToCopy = "BANCA: ${question.institution} • ${question.difficulty}\n\nENUNCIADO:\n${question.statement}\n\nALTERNATIVAS:\n$optionsFormatted"
                            val clip = ClipData.newPlainText("Questão", textToCopy)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "Questão copiada!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Questão",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Refazer / Limpar resposta
                    if (effectiveIsAnswered) {
                        IconButton(
                            onClick = {
                                localIsAnswered = false
                                localSelectedAnswer = null
                                onResetAnswer()
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Tentar Novamente",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Deletar questão
                    if (onDeleteQuestion != null) {
                        IconButton(
                            onClick = onDeleteQuestion,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Deletar Questão",
                                tint = Color(0xFFEF4444).copy(alpha = 0.75f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Referência de Livro e Página (Compacta, elegante e profissional - sem ocupar espaço excessivo)
            val cleanBookName = remember(bookTitle) {
                (bookTitle ?: "Livro")
                    .removeSuffix(".pdf").removeSuffix(".PDF")
                    .removeSuffix(".epub").removeSuffix(".EPUB")
                    .replace("_", " ")
                    .trim()
            }
            if (cleanBookName.isNotBlank() || question.startPage > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF818CF8).copy(alpha = 0.09f),
                    border = BorderStroke(0.5.dp, Color(0xFF818CF8).copy(alpha = 0.22f)),
                    modifier = Modifier.then(
                        if (onOpenBookAtPage != null) Modifier.clickable { onOpenBookAtPage() } else Modifier
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(9.5.dp)
                        )
                        Spacer(modifier = Modifier.width(3.5.dp))
                        Text(
                            text = "$cleanBookName • Pág. ${question.startPage}${if (question.startPage != question.endPage && question.endPage > 0) "-${question.endPage}" else ""}",
                            fontSize = (8.5 * fontSizeMultiplier).sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFC7D2FE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (onOpenBookAtPage != null) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "↗",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            // =================================================================
            // 2. TEXTO-BASE / CONTEXTO MOTIVADOR (Estilo Blockquote Editorial)
            // =================================================================
            val cleanSnippet = remember(question.contextSnippet) {
                TextFormatUtils.cleanContextSnippet(question.contextSnippet)
            }
            if (cleanSnippet.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E1B38).copy(alpha = 0.65f),
                    border = BorderStroke(0.6.dp, Color(0xFF818CF8).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Barra vertical de citação
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF818CF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA).copy(alpha = 0.75f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cleanSnippet,
                            fontSize = (11 * fontSizeMultiplier).sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = (15.5 * fontSizeMultiplier).sp,
                            color = Color(0xFFDDD6FE),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
            }

            // =================================================================
            // 3. ENUNCIADO DA QUESTÃO (Statement de Alta Legibilidade e Alinhamento Justificado)
            // =================================================================
            val cleanStatement = remember(question.statement) {
                TextFormatUtils.cleanStatementText(question.statement)
            }
            Text(
                text = cleanStatement,
                fontSize = (13.5 * fontSizeMultiplier).sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (19 * fontSizeMultiplier).sp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(7.dp))

            // =================================================================
            // 4. ALTERNATIVAS INTERATIVAS (Feedback Tátil & Visual Suave)
            // =================================================================
            Column(
                verticalArrangement = Arrangement.spacedBy(4.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEachIndexed { index, optionText ->
                    val isThisSelected = effectiveIsAnswered && effectiveSelectedIndex == index
                    val isThisCorrect = index == question.correctOptionIndex

                    val targetBg = when {
                        !effectiveIsAnswered -> Color(0xFF18162A)
                        isThisSelected && isThisCorrect -> Color(0xFF064E3B).copy(alpha = 0.65f)
                        isThisSelected && !isThisCorrect -> Color(0xFF7F1D1D).copy(alpha = 0.65f)
                        isThisCorrect -> Color(0xFF064E3B).copy(alpha = 0.35f)
                        else -> Color(0xFF151424).copy(alpha = 0.5f)
                    }

                    val targetBorder = when {
                        !effectiveIsAnswered -> Color(0xFF2C2542)
                        isThisSelected && isThisCorrect -> Color(0xFF10B981)
                        isThisSelected && !isThisCorrect -> Color(0xFFEF4444)
                        isThisCorrect -> Color(0xFF10B981).copy(alpha = 0.85f)
                        else -> Color(0xFF221F33)
                    }

                    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = spring(), label = "optBg")
                    val animatedBorder by animateColorAsState(targetValue = targetBorder, animationSpec = spring(), label = "optBorder")

                    val textColor = when {
                        !effectiveIsAnswered -> Color.White.copy(alpha = 0.95f)
                        isThisSelected && isThisCorrect -> Color(0xFFD1FAE5)
                        isThisSelected && !isThisCorrect -> Color(0xFFFEE2E2)
                        isThisCorrect -> Color(0xFFD1FAE5)
                        else -> Color.White.copy(alpha = 0.45f)
                    }

                    Surface(
                        onClick = {
                            if (!effectiveIsAnswered) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                localIsAnswered = true
                                localSelectedAnswer = index
                                val correct = index == question.correctOptionIndex
                                onAnswerSelected(index, correct)
                            }
                        },
                        enabled = !effectiveIsAnswered,
                        shape = RoundedCornerShape(9.dp),
                        color = animatedBg,
                        border = BorderStroke(if (isThisSelected || (effectiveIsAnswered && isThisCorrect)) 1.2.dp else 0.8.dp, animatedBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Badge circular da alternativa (A, B, C, D, E ou C/E)
                                if (question.questionType == "TRUE_FALSE") {
                                    val isCerto = optionText.contains("Certo", ignoreCase = true)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCerto) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f),
                                        border = BorderStroke(0.8.dp, if (isCerto) Color(0xFF10B981) else Color(0xFFEF4444)),
                                        modifier = Modifier.size(21.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isCerto) "C" else "E",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCerto) Color(0xFF10B981) else Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                } else {
                                    val letter = ('A' + index).toString()
                                    Surface(
                                        shape = CircleShape,
                                        color = when {
                                            isThisSelected && isThisCorrect -> Color(0xFF10B981)
                                            isThisSelected && !isThisCorrect -> Color(0xFFEF4444)
                                            effectiveIsAnswered && isThisCorrect -> Color(0xFF10B981).copy(alpha = 0.35f)
                                            else -> Color.White.copy(alpha = 0.08f)
                                        },
                                        border = BorderStroke(
                                            0.8.dp,
                                            if (isThisSelected || (effectiveIsAnswered && isThisCorrect)) animatedBorder else Color.White.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.size(21.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isThisSelected && isThisCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Correto",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            } else if (isThisSelected && !isThisCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Incorreto",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = letter,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                val cleanOption = remember(optionText) {
                                    TextFormatUtils.cleanOptionText(optionText)
                                }
                                Text(
                                    text = cleanOption,
                                    fontSize = (12.5 * fontSizeMultiplier).sp,
                                    lineHeight = (17 * fontSizeMultiplier).sp,
                                    fontWeight = if (isThisSelected || (effectiveIsAnswered && isThisCorrect)) FontWeight.SemiBold else FontWeight.Normal,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Selo de Gabarito revelado na alternativa correta
                            if (effectiveIsAnswered) {
                                Spacer(modifier = Modifier.width(4.dp))
                                if (isThisCorrect && !isThisSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.22f),
                                        border = BorderStroke(0.8.dp, Color(0xFF10B981))
                                    ) {
                                        Text(
                                            text = "GABARITO",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD1FAE5),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 5. GABARITO COMENTADO & RESOLUÇÃO DIDÁTICA (Aparece ao Responder)
            // =================================================================
            AnimatedVisibility(
                visible = effectiveIsAnswered,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 7.dp)
                ) {
                    val isCorrect = effectiveIsCorrect == true
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCorrect) Color(0xFF042F24).copy(alpha = 0.75f) else Color(0xFF450A0A).copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, if (isCorrect) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFFEF4444).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                            // Status Banner + Letra do Gabarito + Ações
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isCorrect) "Resposta Correta! Parabéns." else "Resposta Incorreta. Revise o gabarito:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrect) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                    )
                                }

                                // Selo do Gabarito Oficial
                                val officialLetter = if (question.questionType == "TRUE_FALSE") {
                                    if (question.correctOptionIndex == 0) "Certo" else "Errado"
                                } else {
                                    ('A' + question.correctOptionIndex).toString()
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                                    border = BorderStroke(0.8.dp, Color(0xFF10B981))
                                ) {
                                    Text(
                                        text = "Gabarito: $officialLetter",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD1FAE5),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            // Texto da Resolução Comentada
                            val cleanExplanation = remember(question.explanation) {
                                TextFormatUtils.cleanTextSpacing(question.explanation)
                            }
                            Text(
                                text = cleanExplanation,
                                fontSize = (11.5 * fontSizeMultiplier).sp,
                                lineHeight = (16.5 * fontSizeMultiplier).sp,
                                color = Color.White.copy(alpha = 0.95f)
                            )

                            // Citação Exata do Livro (Supporting Quote)
                            val cleanQuote = remember(question.supportingQuote) {
                                TextFormatUtils.cleanTextSpacing(question.supportingQuote)
                            }
                            if (cleanQuote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                    border = BorderStroke(0.6.dp, Color(0xFFFBBF24).copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.5.dp))
                                        Text(
                                            text = "« $cleanQuote »",
                                            fontSize = (10.5 * fontSizeMultiplier).sp,
                                            lineHeight = (15.5 * fontSizeMultiplier).sp,
                                            fontStyle = FontStyle.Italic,
                                            fontFamily = FontFamily.Serif,
                                            color = Color(0xFFFEF3C7),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Barra de Ações do Gabarito (Ouvir, Copiar, Refazer)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (onSpeakText != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.08f),
                                            modifier = Modifier.clickable {
                                                onSpeakText("Gabarito comentado: ${question.explanation}")
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Ouvir Gabarito",
                                                    tint = Color(0xFFA78BFA),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Ouvir",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier.clickable {
                                            val clip = ClipData.newPlainText("Resolução", question.explanation)
                                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                            Toast.makeText(context, "Resolução copiada!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copiar",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Copiar",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    if (onOpenBookAtPage != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                            border = BorderStroke(0.6.dp, Color(0xFF818CF8).copy(alpha = 0.5f)),
                                            modifier = Modifier.clickable { onOpenBookAtPage() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MenuBook,
                                                    contentDescription = "Ver no Livro",
                                                    tint = Color(0xFFA78BFA),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Livro ↗",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFDDD6FE)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Botão Refazer
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = BorderStroke(0.6.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        localIsAnswered = false
                                        localSelectedAnswer = null
                                        onResetAnswer()
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refazer Questão",
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Refazer",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFDE68A)
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
