package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "openai_page_summaries",
    indices = [Index(value = ["bookId", "pageNumber"], unique = true)]
)
data class OpenAiPageSummary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val pageNumber: Int,
    val bookTitle: String,
    val originalPageText: String,
    val summaryText: String,
    val audioFilePath: String? = null,
    val voiceName: String = "alloy",
    val modelUsed: String = "gpt-4o-mini",
    val createdAt: Long = System.currentTimeMillis()
)
