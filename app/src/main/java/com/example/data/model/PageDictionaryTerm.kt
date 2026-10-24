package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page_dictionary_terms",
    indices = [
        Index(value = ["bookId", "pageNumber"]),
        Index(value = ["term"])
    ]
)
data class PageDictionaryTerm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val pageNumber: Int,
    val bookTitle: String,
    val term: String,
    val category: String = "Geral", // e.g. "Jurídico", "Composto / Expressão", "Substantivo", "Processo", "Técnico", "Filosófico / Acadêmico", "Erudito"
    val originalContext: String = "",
    val originalDefinition: String = "",
    val spanishTerm: String = "",
    val spanishDefinition: String = "",
    val spanishExample: String = "",
    val frenchTerm: String = "",
    val frenchDefinition: String = "",
    val frenchExample: String = "",
    val englishTerm: String = "",
    val englishDefinition: String = "",
    val englishExample: String = "",
    val audioPathOriginal: String = "",
    val audioPathSpanish: String = "",
    val audioPathFrench: String = "",
    val audioPathEnglish: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
