package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.alarm.StudyAlarmEvent
import com.example.data.model.PdfBook

@Composable
fun StudyAlarmAlertOverlay(
    alarmEvent: StudyAlarmEvent,
    onDismiss: () -> Unit,
    onOpenBookNow: (PdfBook) -> Unit
) {
    val book = alarmEvent.book

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF030D22), // Fundo do tema ciber
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0EF3FF)), // Ciano neon
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone Pulsante de Alarme
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF2E97).copy(alpha = 0.25f), // Rosa neon
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF2E97)),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alarme de Estudo",
                            tint = Color(0xFFFF2E97),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "⏰ HORA DO SEU ESTUDO!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0EF3FF), // Ciano neon
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Lembrete periódico para continuar a sua leitura programada.",
                    fontSize = 12.sp,
                    color = Color(0xFFFDFEFF).copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card do Livro Selecionado Aleatoriamente
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1347)), // Linha selecionada do tema
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0098DF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFCC00).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFFCC00))
                        ) {
                            Text(
                                text = "LIVRO ESCOLHIDO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFCC00),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = book.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDFEFF),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = book.author,
                            fontSize = 12.sp,
                            color = Color(0xFF0EE5C6),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Destaque da página onde o usuário parou!
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF030D22),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0CB886)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📖 Você parou na Página ${book.currentPage} de ${book.totalPages}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0CB886),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botão de Ação: Continuar Lendo Agora
                Button(
                    onClick = {
                        onOpenBookNow(book)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2CF1)), // Magenta neon
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_book_from_alarm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF030D22),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continuar Lendo Agora (Pág. ${book.currentPage})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF030D22)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botão Soneca / Fechar
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0098DF).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Lembrar mais tarde / Dispensar",
                        fontSize = 12.sp,
                        color = Color(0xFFFDFEFF).copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
