package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "segment_chat_messages",
    indices = [
        Index(value = ["bookId", "pageNumber", "segmentIndex"])
    ]
)
data class SegmentChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val pageNumber: Int,
    val segmentIndex: Int,
    val sender: String, // "USER" or "ASSISTANT"
    val messageText: String,
    val isAudioInput: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
