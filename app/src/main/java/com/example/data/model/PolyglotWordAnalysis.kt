package com.example.data.model

import androidx.compose.ui.graphics.Color

/**
 * Análise morfológica e lexical adaptada para múltiplos sistemas de escrita:
 * Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico e Inglês.
 */
data class PolyglotWordAnalysis(
    val word: String,
    val languageCode: String = "en",
    val transliteration: String = "",       // Pinyin, Romaji, Transliteração Cirílica/Grega/Árabe/Hebraica
    val phoneticIpa: String = "",           // Pronúncia IPA universal
    val cefrLevel: String = "C1",           // A1, A2, B1, B2, C1, C2
    val partOfSpeech: String = "",          // Substantivo, Verbo, Adjetivo, etc.
    val nativeScriptWithVocal: String = "", // Com Harakaat (árabe), Niqqud (hebraico), ou Ударение (russo/ucraniano)
    val rootOrRadical: String = "",         // Raiz semítica trilítera (ex: K-T-B), radical chinês/japonês, ou raiz grega/latina
    val genderOrGrammarCase: String = "",   // ex: Alemão (der/die/das), Russo/Ucraniano (Genitivo, Acusativo, etc.)
    val compoundBreakdown: String = "",     // Decomposição de palavras compostas (Alemão Komposita ou Chinês composto)
    val monolingualDefinition: String = "", // Definição no idioma nativo do livro
    val portugueseMeaning: String = "",     // Tradução precisa e nuances em português
    val registerAndTone: String = "Formal / Literário",
    val collocations: List<String> = emptyList(),
    val synonymsWithNuance: List<String> = emptyList(),
    val contextualSentence: String = "",
    val sentenceTranslation: String = "",
    val usageTipC2: String = "",            // Dica de nível nativo/C2
    val activeChallengePrompt: String = "", // Desafio de fixação
    val isIdiomOrPhrasalVerb: Boolean = false
) {
    val language: StudyLanguage
        get() = StudyLanguage.fromCode(languageCode)

    val cefrBadgeColor: Color
        get() = when (cefrLevel.uppercase().trim()) {
            "C2" -> Color(0xFFEF4444)
            "C1" -> Color(0xFF8B5CF6)
            "B2" -> Color(0xFF3B82F6)
            "B1" -> Color(0xFF10B981)
            else -> Color(0xFFF59E0B)
        }
}
