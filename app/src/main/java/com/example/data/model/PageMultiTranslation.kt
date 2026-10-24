package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "page_multi_translations",
    indices = [Index(value = ["bookId", "pageNumber"], unique = true)]
)
data class PageMultiTranslation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val pageNumber: Int,
    val originalText: String,
    val englishText: String,
    val frenchText: String,
    val spanishText: String,
    val portugueseText: String = "",
    val comparativeParagraphsJson: String = "",
    val translatedAt: Long = System.currentTimeMillis()
) {
    fun getParagraphs(): List<ComparativeParagraph> {
        if (comparativeParagraphsJson.isBlank()) {
            return fallbackParagraphs()
        }
        return try {
            val jsonArray = JSONArray(comparativeParagraphsJson)
            val list = mutableListOf<ComparativeParagraph>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pt = obj.optString("portuguese", "").ifBlank {
                    if (portugueseText.isNotBlank()) portugueseText else originalText
                }
                list.add(
                    ComparativeParagraph(
                        paragraphIndex = obj.optInt("paragraphIndex", i),
                        portuguese = pt,
                        english = obj.optString("english", ""),
                        french = obj.optString("french", ""),
                        spanish = obj.optString("spanish", "")
                    )
                )
            }
            if (list.isEmpty()) fallbackParagraphs() else list
        } catch (_: Exception) {
            fallbackParagraphs()
        }
    }

    private fun fallbackParagraphs(): List<ComparativeParagraph> {
        val effectivePt = if (portugueseText.isNotBlank()) portugueseText else originalText
        val ptParas = effectivePt.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        val enParas = englishText.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        val frParas = frenchText.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        val esParas = spanishText.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        val maxLen = maxOf(ptParas.size, enParas.size, frParas.size, esParas.size).coerceAtLeast(1)

        val list = mutableListOf<ComparativeParagraph>()
        for (i in 0 until maxLen) {
            list.add(
                ComparativeParagraph(
                    paragraphIndex = i,
                    portuguese = ptParas.getOrElse(i) { "" },
                    english = enParas.getOrElse(i) { "" },
                    french = frParas.getOrElse(i) { "" },
                    spanish = esParas.getOrElse(i) { "" }
                )
            )
        }
        return list
    }
}

data class ComparativeParagraph(
    val paragraphIndex: Int,
    val portuguese: String,
    val english: String,
    val french: String,
    val spanish: String
)

enum class ReadingLanguage(val code: String, val label: String, val shortLabel: String, val flag: String) {
    PORTUGUESE("pt", "Português", "PT", "🇧🇷"),
    ENGLISH("en", "Inglês", "EN", "🇬🇧"),
    FRENCH("fr", "Francês", "FR", "🇫🇷"),
    SPANISH("es", "Espanhol", "ES", "🇪🇸")
}
