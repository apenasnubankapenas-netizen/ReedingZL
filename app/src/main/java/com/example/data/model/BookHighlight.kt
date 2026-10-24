package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_highlights",
    indices = [
        Index(value = ["bookId"])
    ]
)
data class BookHighlight(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val pageNumber: Int = 1,
    val quoteText: String = "",
    val note: String = "",
    val colorTag: String = "#F59E0B",
    val createdAt: Long = System.currentTimeMillis()
)
