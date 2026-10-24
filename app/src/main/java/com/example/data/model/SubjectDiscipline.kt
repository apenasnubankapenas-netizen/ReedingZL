package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(tableName = "subject_disciplines")
data class SubjectDiscipline(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val bannerColorHex: String = "#1E3A8A",
    val authorIdsJson: String = "[]",
    val directBookIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getAuthorIds(): List<Int> {
        return try {
            val arr = JSONArray(authorIdsJson)
            List(arr.length()) { arr.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDirectBookIds(): List<Int> {
        return try {
            val arr = JSONArray(directBookIdsJson)
            List(arr.length()) { arr.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun listToJson(list: List<Int>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            return arr.toString()
        }
    }
}
