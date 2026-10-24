package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gemini_notes")
data class GeminiNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int,
    val cueIndex: Int,
    val termOrSentence: String,
    val fullContext: String,
    val geminiResponse: String,
    val createdAt: Long = System.currentTimeMillis()
)
