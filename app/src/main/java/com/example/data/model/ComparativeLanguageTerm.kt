package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents linguistic comparative terms extracted via OpenAI API.
 * Contains 3 principal language sections (English, French, Spanish) plus optional other languages,
 * term definitions, IPA phonetic transcriptions, and 16 verb tense examples with affirmations, questions,
 * modal verbs, and connectors.
 */
@Entity(
    tableName = "comparative_language_terms",
    indices = [
        Index(value = ["bookId", "pageNumber"]),
        Index(value = ["term"])
    ]
)
data class ComparativeLanguageTerm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val pageNumber: Int,
    val bookTitle: String,
    val term: String,
    val grammaticalCategory: String = "Geral", // e.g. "Verbo", "Substantivo", "Expressão / Frase Verbal", "Adjetivo"
    val originalContext: String = "",
    val meaning: String = "", // Detailed meaning of the term
    val ipaTranscription: String = "", // International Phonetic Alphabet (IPA) representation
    // 3 Principal Translations
    val englishTerm: String = "",
    val englishMeaning: String = "",
    val englishIpa: String = "",
    val frenchTerm: String = "",
    val frenchMeaning: String = "",
    val frenchIpa: String = "",
    val spanishTerm: String = "",
    val spanishMeaning: String = "",
    val spanishIpa: String = "",
    // Portuguese Translation (crucial when original book is in English, French, Spanish or any foreign language)
    val portugueseTerm: String = "",
    val portugueseMeaning: String = "",
    val portugueseIpa: String = "",
    // Extended Languages (JSON or formatted string if extra languages present)
    val otherLanguagesJson: String = "",
    // 16 Verb Tense / Extended Usage Examples (affirmations, questions, modals, connectors)
    val usageExamplesJson: String = "", // JSON array of ComparativeUsageExample objects
    // Optional audio paths
    val audioPathOriginal: String = "",
    val audioPathEnglish: String = "",
    val audioPathFrench: String = "",
    val audioPathSpanish: String = "",
    val audioPathPortuguese: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ComparativeUsageExample(
    val tenseNumber: Int = 1,
    val tenseName: String = "", // e.g. "Present Simple", "Past Continuous", "Present Perfect", etc.
    val type: String = "Afirmação", // "Afirmação" ou "Pergunta"
    val modalOrConnector: String = "", // e.g. "Modal: can/must", "Conectivo: however/because"
    val englishSentence: String = "",
    val frenchSentence: String = "",
    val spanishSentence: String = "",
    val portugueseSentence: String = "",
    val ipaEnglish: String = ""
)
