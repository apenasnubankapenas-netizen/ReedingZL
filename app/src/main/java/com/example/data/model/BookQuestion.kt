package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(
    tableName = "book_questions",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "startPage", "endPage"])
    ]
)
data class BookQuestion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val startPage: Int,
    val endPage: Int,
    val institution: String, // "FGV", "CEBRASPE", "FCC", "IADES", "ENEM"
    val difficulty: String,  // "EASY", "MEDIUM", "HARD", "INSANE"
    val questionType: String, // "MULTIPLE_CHOICE", "TRUE_FALSE"
    val statement: String,   // Enunciado da questão
    val contextSnippet: String = "", // Trecho de apoio do texto
    val optionsJson: String, // JSON array com as opções: ["A) ...", "B) ..."]
    val correctOptionIndex: Int, // 0-indexed
    val explanation: String, // Explicação detalhada do porquê acertou ou errou
    val supportingQuote: String, // Texto de apoio/citação exata do livro comprovando a resposta
    val targetParagraph: String = "", // Parágrafo ou oração foco
    val userAnswerIndex: Int? = null, // Alternativa escolhida pelo usuário
    val isAnswered: Boolean = false,
    val isCorrect: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getOptions(): List<String> {
        if (optionsJson.isBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    val institutionDisplayName: String
        get() = when (institution.uppercase()) {
            "FGV" -> "FGV"
            "CEBRASPE", "CESPE" -> "CEBRASPE"
            "FCC" -> "FCC"
            "VUNESP" -> "VUNESP"
            "CESGRANRIO" -> "CESGRANRIO"
            "QUADRIX" -> "QUADRIX"
            "AOCP", "INSTITUTO AOCP", "INST_AOCP" -> "INST. AOCP"
            "IADES", "EADES" -> "IADES"
            "MISTA", "TODAS" -> "MISTA"
            else -> institution.uppercase()
        }

    val difficultyDisplayName: String
        get() = when (difficulty.uppercase()) {
            "EASY" -> "Fácil"
            "MEDIUM" -> "Médio"
            "HARD" -> "Difícil"
            "INSANE" -> "Insano FGV 🔥"
            else -> difficulty
        }

    val questionTypeDisplayName: String
        get() = when (questionType.uppercase()) {
            "TRUE_FALSE" -> "Certo / Errado"
            "ASSERTIVAS", "ROMAN_NUMERALS" -> "Assertivas (I, II, III)"
            else -> "Múltipla Escolha (A-E)"
        }
}
