package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(tableName = "public_organizations")
data class PublicOrganization(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val organizationType: String = "Órgão de Governo", // Órgão de Governo, Autarquia, etc.
    val description: String = "",
    val iconUri: String? = null, // URI local para imagem Full HD / Alta Qualidade
    val iconPreset: String = "", // RECEITA, POLICIA_FEDERAL, INSS, TCU, etc.
    val accentColorHex: String = "#0284C7",
    val pdfBookIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getPdfBookIds(): List<Int> {
        return try {
            val list = mutableListOf<Int>()
            val jsonArray = JSONArray(pdfBookIdsJson)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getInt(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun withAddedPdfBookId(bookId: Int): PublicOrganization {
        val current = getPdfBookIds().toMutableSet()
        current.add(bookId)
        val jsonArray = JSONArray()
        current.forEach { jsonArray.put(it) }
        return copy(pdfBookIdsJson = jsonArray.toString())
    }

    fun withRemovedPdfBookId(bookId: Int): PublicOrganization {
        val current = getPdfBookIds().toMutableSet()
        current.remove(bookId)
        val jsonArray = JSONArray()
        current.forEach { jsonArray.put(it) }
        return copy(pdfBookIdsJson = jsonArray.toString())
    }
}
