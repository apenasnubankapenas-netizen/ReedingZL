package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_page_recommendations",
    indices = [
        Index(value = ["bookId", "pageNumber"])
    ]
)
data class BookPageRecommendation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val pageNumber: Int,
    val themeOrTopic: String,          // Tema, assunto ou termo entre vírgulas extraído
    val recommendedBookTitle: String,  // Título real e consagrado do livro recomendado
    val author: String,                // Autor de referência da obra
    val relevanceDescription: String,  // Por que este livro é a referência indispensável para este tema
    val analysisType: String = "PAGE_THEME", // "PAGE_THEME" ou "COMMA_SEPARATED"
    val yearOrEdition: String = "",    // Ano ou edição clássica aproximada
    val createdAt: Long = System.currentTimeMillis()
)
