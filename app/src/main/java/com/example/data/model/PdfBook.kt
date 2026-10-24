package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_books")
data class PdfBook(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val author: String = "Autor Desconhecido",
    val fileUri: String? = null,
    val coverPath: String? = null,
    val totalPages: Int = 1,
    val currentPage: Int = 1,
    val speechSpeed: Float = 1.0f,
    val readingTheme: String = "PAPER_SEPIA",
    val starredPagesJson: String = "[]",
    val sampleContentJson: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val categoryColor: String? = null,
    val authorCollectionId: Int? = null,
    val disciplineId: Int? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastReadTimestamp: Long = System.currentTimeMillis()
) {
    val readingProgressPercent: Float
        get() = if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()) * 100f else 0f

    val lastReadPage: Int
        get() = currentPage
}
