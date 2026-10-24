package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page_segment_explanations",
    indices = [
        Index(value = ["bookId", "pageNumber"]),
        Index(value = ["bookId", "pageNumber", "segmentIndex"], unique = true)
    ]
)
data class PageSegmentExplanation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val bookId: Int,
    val pageNumber: Int,
    val segmentIndex: Int = 0,
    val segmentText: String,
    val explanationSpanish: String = "",
    val explanationEnglish: String = "",
    val explanationFrench: String = "",
    val explanationPortuguese: String = "",
    val modelUsed: String = "gpt-4o-mini",
    val createdAt: Long = System.currentTimeMillis()
) {
    val originalText: String get() = segmentText
    val simplifiedExplanation: String get() = explanationPortuguese

    fun getExplanationForLanguage(language: String): String {
        return when (language.lowercase().trim()) {
            "es", "spanish", "espanhol" -> explanationSpanish.ifBlank { explanationPortuguese }
            "en", "english", "inglês", "ingles" -> explanationEnglish.ifBlank { explanationPortuguese }
            "fr", "french", "francês", "frances" -> explanationFrench.ifBlank { explanationPortuguese }
            else -> explanationPortuguese.ifBlank { explanationEnglish }
        }
    }
}
