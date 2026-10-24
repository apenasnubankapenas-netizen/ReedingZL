package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val folderId: Int? = null,
    val mediaUri: String? = null,
    val mediaType: String = "AUDIO", // "AUDIO" or "VIDEO"
    val srtContent: String,
    val secondarySrtContent: String? = null,
    val timeOffsetMs: Long = 0L,
    val fontSizeSp: Float = 24f,
    val lastModified: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val sampleAssetAudio: String? = null,
    val customIconUri: String? = null
)
