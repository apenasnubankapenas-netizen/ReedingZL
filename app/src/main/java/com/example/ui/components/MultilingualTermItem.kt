package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.StudyLanguage
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import com.example.data.translator.TermsNotesTranslator
import kotlinx.coroutines.launch

/**
 * Representa um idioma disponível para tradução e pronúncia de termos.
 */
data class TermLanguageOption(
    val id: String,
    val flag: String,
    val name: String,
    val language: StudyLanguage
)

/**
 * Lista de todos os idiomas com bandeiras suportados pelo aplicativo,
 * incluindo variantes solicitadas (como 🇧🇷, 🇺🇸, 🇬🇧, 🇪🇸, 🇫🇷, 🇩🇪, 🇮🇹, 🇵🇹, 🇷🇺, 🇨🇳, etc).
 */
val ALL_TERM_LANGUAGE_OPTIONS: List<TermLanguageOption> = listOf(
    TermLanguageOption("pt_br", "🇧🇷", "Português", StudyLanguage.PORTUGUESE),
    TermLanguageOption("en_us", "🇺🇸", "Inglês (US)", StudyLanguage.ENGLISH),
    TermLanguageOption("en_gb", "🇬🇧", "Inglês (UK)", StudyLanguage.ENGLISH),
    TermLanguageOption("es", "🇪🇸", "Espanhol", StudyLanguage.SPANISH),
    TermLanguageOption("fr", "🇫🇷", "Francês", StudyLanguage.FRENCH),
    TermLanguageOption("de", "🇩🇪", "Alemão", StudyLanguage.GERMAN),
    TermLanguageOption("it", "🇮🇹", "Italiano", StudyLanguage.ITALIAN),
    TermLanguageOption("pt_pt", "🇵🇹", "Português (PT)", StudyLanguage.PORTUGUESE),
    TermLanguageOption("ru", "🇷🇺", "Russo", StudyLanguage.RUSSIAN),
    TermLanguageOption("zh", "🇨🇳", "Chinês", StudyLanguage.CHINESE),
    TermLanguageOption("ja", "🇯🇵", "Japonês", StudyLanguage.JAPANESE),
    TermLanguageOption("ko", "🇰🇷", "Coreano", StudyLanguage.KOREAN),
    TermLanguageOption("ar", "🇸🇦", "Árabe", StudyLanguage.ARABIC),
    TermLanguageOption("tr", "🇹🇷", "Turco", StudyLanguage.TURKISH),
    TermLanguageOption("nl", "🇳🇱", "Holandês", StudyLanguage.DUTCH),
    TermLanguageOption("sv", "🇸🇪", "Sueco", StudyLanguage.SWEDISH),
    TermLanguageOption("pl", "🇵🇱", "Polonês", StudyLanguage.POLISH),
    TermLanguageOption("el", "🇬🇷", "Grego", StudyLanguage.GREEK),
    TermLanguageOption("he", "🇮🇱", "Hebraico", StudyLanguage.HEBREW),
    TermLanguageOption("hi", "🇮🇳", "Hindi", StudyLanguage.HINDI),
    TermLanguageOption("uk", "🇺🇦", "Ucraniano", StudyLanguage.UKRAINIAN),
    TermLanguageOption("ro", "🇷🇴", "Romeno", StudyLanguage.ROMANIAN),
    TermLanguageOption("id", "🇮🇩", "Indonésio", StudyLanguage.INDONESIAN),
    TermLanguageOption("vi", "🇻🇳", "Vietnamita", StudyLanguage.VIETNAMESE),
    TermLanguageOption("no", "🇳🇴", "Norueguês", StudyLanguage.NORWEGIAN),
    TermLanguageOption("da", "🇩🇰", "Dinamarquês", StudyLanguage.DANISH),
    TermLanguageOption("fi", "🇫🇮", "Finlandês", StudyLanguage.FINNISH),
    TermLanguageOption("cs", "🇨🇿", "Tcheco", StudyLanguage.CZECH)
)

/**
 * Exibe um termo do dicionário ou de notas com:
 * 1. Linha horizontal rolável contendo todos os idiomas com bandeiras, o nome traduzido do termo
 *    e um ícone para ouvir a pronúncia gerada/salva pela OpenAI.
 * 2. Abaixo: o significado correspondente naquele idioma, ocupando o mesmo espaço e substituindo
 *    dinamicamente sem gerar todas as traduções de uma vez ou esticar a tela.
 * 3. Efeito karaokê em tempo real com destaque palavra por palavra e compatibilidade de velocidade.
 */
@Composable
fun MultilingualTermItem(
    term: PageDictionaryTerm,
    speechManager: PdfSpeechManager?,
    accentColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    termIndex: Int? = null,
    titleFontSize: TextUnit = 13.sp,
    bodyFontSize: TextUnit = 11.sp,
    bodyLineHeight: TextUnit = 15.sp,
    highlightBgColor: Color = Color(0xFFFBBF24).copy(alpha = 0.45f),
    highlightTextColor: Color = Color(0xFFFEF08A),
    cardBgColor: Color = Color.Transparent,
    cardBorderColor: Color? = null,
    showDetailsButton: Boolean = false,
    onDetailsClick: ((PageDictionaryTerm) -> Unit)? = null,
    onTermClick: ((PageDictionaryTerm) -> Unit)? = null,
    onFallbackSpeak: ((langCode: String, text: String) -> Unit)? = null,
    scopeId: String = "term_${term.id}"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedOption by remember(term.id) {
        mutableStateOf(ALL_TERM_LANGUAGE_OPTIONS.first())
    }

    var requestPlayToken by remember(term.id) { mutableIntStateOf(0) }

    val isSpeaking by speechManager?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
    val activeTargetId by speechManager?.activeTargetId?.collectAsState() ?: remember { mutableStateOf(null) }

    val currentTargetId = "${scopeId}_${selectedOption.id}"
    val isPlayingThisLanguage = isSpeaking && activeTargetId == currentTargetId

    // Map reativo para armazenar traduções de nomes dos termos em todos os chips
    val termNamesMap = remember(term.id) {
        mutableStateMapOf<String, String>().apply {
            ALL_TERM_LANGUAGE_OPTIONS.forEach { opt ->
                val name = when {
                    opt.id == "pt_br" -> term.term
                    opt.id == "pt_pt" -> TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                    opt.id.startsWith("en") -> term.englishTerm.ifBlank {
                        TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                    }
                    opt.language == StudyLanguage.SPANISH -> term.spanishTerm.ifBlank {
                        TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                    }
                    opt.language == StudyLanguage.FRENCH -> term.frenchTerm.ifBlank {
                        TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                    }
                    else -> TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                }
                put(opt.id, name)
            }
        }
    }

    // Título traduzido reativo do termo selecionado (0ms instantâneo + atualização em background)
    var displayTermTitle by remember(term.id, selectedOption.id) {
        val initial = termNamesMap[selectedOption.id] ?: when {
            selectedOption.id == "pt_br" -> term.term
            selectedOption.id == "pt_pt" -> TermsNotesTranslator.getMultiLanguageTermName(term.term, selectedOption.language, selectedOption.id)
            selectedOption.id.startsWith("en") -> term.englishTerm.ifBlank {
                TermsNotesTranslator.getMultiLanguageTermName(term.term, selectedOption.language, selectedOption.id)
            }
            selectedOption.language == StudyLanguage.SPANISH -> term.spanishTerm.ifBlank {
                TermsNotesTranslator.getMultiLanguageTermName(term.term, selectedOption.language, selectedOption.id)
            }
            selectedOption.language == StudyLanguage.FRENCH -> term.frenchTerm.ifBlank {
                TermsNotesTranslator.getMultiLanguageTermName(term.term, selectedOption.language, selectedOption.id)
            }
            else -> TermsNotesTranslator.getMultiLanguageTermName(term.term, selectedOption.language, selectedOption.id)
        }
        mutableStateOf(initial)
    }

    // 0ms tradução instantânea da definição com atualização assíncrona
    var dynamicDefinition by remember(term.id, selectedOption.id) {
        val quick = when {
            selectedOption.id == "pt_br" -> term.originalDefinition
            selectedOption.id == "pt_pt" -> TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, selectedOption.language).second
            selectedOption.id.startsWith("en") -> term.englishDefinition.ifBlank {
                TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, selectedOption.language).second
            }
            selectedOption.language == StudyLanguage.SPANISH -> term.spanishDefinition.ifBlank {
                TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, selectedOption.language).second
            }
            selectedOption.language == StudyLanguage.FRENCH -> term.frenchDefinition.ifBlank {
                TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, selectedOption.language).second
            }
            else -> TermsNotesTranslator.getQuickTranslation(term.term, term.originalDefinition, selectedOption.language).second
        }
        mutableStateOf(quick.ifBlank { term.originalDefinition })
    }

    // Busca assíncrona de alta qualidade para o idioma selecionado sob demanda
    LaunchedEffect(term.id, selectedOption.id) {
        if (selectedOption.id != "pt_br") {
            val openAiKey = OpenAiService.getApiKey(context)
            val translated = TermsNotesTranslator.translateDictionaryTerm(
                term = term,
                targetLanguage = selectedOption.language,
                customApiKey = openAiKey,
                context = context
            )
            if (translated.first.isNotBlank()) {
                displayTermTitle = translated.first
                termNamesMap[selectedOption.id] = translated.first
            }
            if (translated.second.isNotBlank()) {
                dynamicDefinition = translated.second
            }
        }
    }

    val content = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (cardBorderColor != null) 10.dp else 0.dp)
        ) {
            // Cabeçalho do Termo: [Índice] Título Original + Categoria + Detalhes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onTermClick != null && !showDetailsButton) Modifier.clickable { onTermClick(term) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (termIndex != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor.copy(alpha = 0.22f),
                            border = BorderStroke(0.6.dp, accentColor.copy(alpha = 0.55f)),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "[$termIndex]",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = term.term,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    if (term.category.isNotBlank() && term.category != "Geral") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = term.category,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor.copy(alpha = 0.90f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (showDetailsButton && onDetailsClick != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = textColor.copy(alpha = 0.1f),
                        border = BorderStroke(0.6.dp, textColor.copy(alpha = 0.2f)),
                        modifier = Modifier.clickable { onDetailsClick(term) }
                    ) {
                        Text(
                            text = "Detalhes",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Linha horizontal de traduções do termo: "uma na frente da outra"
            // com bandeira + termo traduzido + ícone para ouvir em cada idioma
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ALL_TERM_LANGUAGE_OPTIONS.forEach { opt ->
                    val isSelected = selectedOption.id == opt.id
                    val isThisLangPlaying = isSpeaking && activeTargetId == "${scopeId}_${opt.id}"

                    val optTermName = termNamesMap[opt.id] ?: when {
                        opt.id == "pt_br" -> term.term
                        opt.id == "pt_pt" -> TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                        opt.id.startsWith("en") -> term.englishTerm.ifBlank {
                            TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                        }
                        opt.language == StudyLanguage.SPANISH -> term.spanishTerm.ifBlank {
                            TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                        }
                        opt.language == StudyLanguage.FRENCH -> term.frenchTerm.ifBlank {
                            TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                        }
                        else -> TermsNotesTranslator.getMultiLanguageTermName(term.term, opt.language, opt.id)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                        border = null,
                        modifier = Modifier.clickable {
                            // Clicar no termo: seleciona e substitui dinamicamente o significado abaixo
                            val wasSelected = selectedOption.id == opt.id
                            if (!wasSelected && isSpeaking) {
                                speechManager?.stopTarget(currentTargetId)
                            }
                            selectedOption = opt
                            if (wasSelected) {
                                // Se já estava selecionado e clicou nele novamente, aciona reprodução
                                requestPlayToken++
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = opt.flag,
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = optTermName,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentColor else textColor.copy(alpha = 0.88f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))

                            // Ícone para ouvir a pronúncia naquele idioma
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (selectedOption.id != opt.id && isSpeaking) {
                                            speechManager?.stopTarget(currentTargetId)
                                        }
                                        selectedOption = opt
                                        requestPlayToken++
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isThisLangPlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                                    contentDescription = "Ouvir $optTermName em ${opt.name}",
                                    tint = if (isThisLangPlaying) accentColor else (if (isSelected) accentColor else textColor.copy(alpha = 0.65f)),
                                    modifier = Modifier.size(11.5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Significado do termo no idioma selecionado (substitui no mesmo espaço abaixo)
            // com Karaokê sincronizado e compatibilidade total com velocidade/seek
            val definitionFullText = "$displayTermTitle: $dynamicDefinition"
            KaraokeDefinitionText(
                targetId = currentTargetId,
                text = definitionFullText,
                speechManager = speechManager,
                accentColor = accentColor,
                textColor = textColor.copy(alpha = 0.90f),
                fontSize = bodyFontSize,
                lineHeight = bodyLineHeight,
                highlightBgColor = highlightBgColor,
                highlightTextColor = highlightTextColor,
                showListenButton = true,
                language = selectedOption.language,
                requestPlayToken = requestPlayToken,
                onFallbackSpeak = onFallbackSpeak
            )
        }
    }

    if (cardBorderColor != null) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardBgColor,
            border = BorderStroke(0.9.dp, cardBorderColor),
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}
