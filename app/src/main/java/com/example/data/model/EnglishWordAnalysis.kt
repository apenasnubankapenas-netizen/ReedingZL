package com.example.data.model

import androidx.compose.ui.graphics.Color

/**
 * Modelo de dados para análise lexical profunda voltada ao nível C2 em inglês.
 * Fornece fonética IPA, nível CEFR, definições monolíngues, colocações reais,
 * etimologia e dicas de nuances literárias/nativas.
 */
data class EnglishWordAnalysis(
    val word: String,
    val phoneticIpa: String = "",
    val cefrLevel: String = "C1", // A1, A2, B1, B2, C1, C2
    val partOfSpeech: String = "", // noun, verb, adjective, adverb, phrasal verb, idiom
    val englishDefinition: String = "", // Definição monolíngue imersiva (Oxford/Cambridge)
    val portugueseMeaning: String = "", // Significado preciso e nuance em português
    val registerAndTone: String = "Formal / Literário", // Formal, Literário, Arcaico, Informal, Técnico
    val collocations: List<String> = emptyList(), // e.g. ["ephemeral pleasure", "ephemeral nature of fame"]
    val synonymsWithNuance: List<String> = emptyList(), // e.g. ["fleeting (momento fugaz)", "transient (transitório)"]
    val contextualSentence: String = "", // Frase exemplar no contexto do livro
    val sentenceTranslation: String = "", // Tradução da frase
    val etymologyRoot: String = "", // Raiz etimológica grega/latina
    val usageTipC2: String = "", // Dica avançada para soar nativo (nível C2)
    val activeChallengePrompt: String = "", // Desafio para o leitor produzir sua própria frase
    val isIdiomOrPhrasalVerb: Boolean = false
) {
    /**
     * Cor indicadora do nível CEFR para UI moderna.
     */
    val cefrBadgeColor: Color
        get() = when (cefrLevel.uppercase().trim()) {
            "C2" -> Color(0xFFEF4444) // Vermelho elite C2
            "C1" -> Color(0xFF8B5CF6) // Roxo avançado C1
            "B2" -> Color(0xFF3B82F6) // Azul intermediário superior B2
            "B1" -> Color(0xFF10B981) // Verde intermediário B1
            else -> Color(0xFFF59E0B) // Âmbar A1-A2
        }
}
