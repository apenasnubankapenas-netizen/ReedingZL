package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "author_collections")
data class AuthorCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val bio: String = "",
    val photoUri: String? = null,
    val bannerColorHex: String = "#6366F1",
    val createdAt: Long = System.currentTimeMillis()
)
