package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Entidade para estudo dedicado de Verbos e Phrasal Verbs no Dicionário.
 * Armazena a correta preposição usada, traduções em português e 16 tempos verbais
 * com Question Words e palavras de combinação direta (collocations).
 */
@Entity(
    tableName = "verb_phrasal_study_terms",
    indices = [
        Index(value = ["bookId", "pageNumber"]),
        Index(value = ["verbOrPhrase"]),
        Index(value = ["languageCode"])
    ]
)
data class VerbPhrasalVerbStudyTerm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val pageNumber: Int,
    val bookTitle: String,
    val verbOrPhrase: String,
    val isPhrasalVerb: Boolean,
    val prepositionUsed: String,
    val baseVerb: String = "",
    val grammaticalCategory: String = "Phrasal Verb", // "Phrasal Verb", "Verbo Transitivo", "Verbo Intransitivo", etc.
    val languageCode: String = "en",
    val definitionTargetLang: String = "",
    val portugueseMeaning: String = "",
    val ipaTranscription: String = "",
    val examplesJson: String = "[]",
    val isFavorite: Boolean = false,
    val userNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getExamples(): List<VerbTenseQuestionExample> {
        val list = mutableListOf<VerbTenseQuestionExample>()
        if (examplesJson.isBlank() || examplesJson == "[]") return list
        try {
            val array = JSONArray(examplesJson)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(
                    VerbTenseQuestionExample(
                        tenseNumber = obj.optInt("tenseNumber", i + 1),
                        tenseName = obj.optString("tenseName", "Tempo ${i + 1}"),
                        questionWord = obj.optString("questionWord", "Why"),
                        englishSentence = obj.optString("englishSentence", ""),
                        portugueseSentence = obj.optString("portugueseSentence", ""),
                        collocationNotes = obj.optString("collocationNotes", ""),
                        ipaTranscription = obj.optString("ipaTranscription", "")
                    )
                )
            }
        } catch (_: Exception) { }
        return list
    }

    companion object {
        fun buildJsonFromExamples(examples: List<VerbTenseQuestionExample>): String {
            val array = JSONArray()
            examples.forEach { ex ->
                val obj = JSONObject().apply {
                    put("tenseNumber", ex.tenseNumber)
                    put("tenseName", ex.tenseName)
                    put("questionWord", ex.questionWord)
                    put("englishSentence", ex.englishSentence)
                    put("portugueseSentence", ex.portugueseSentence)
                    put("collocationNotes", ex.collocationNotes)
                    put("ipaTranscription", ex.ipaTranscription)
                }
                array.put(obj)
            }
            return array.toString()
        }
    }
}

/**
 * Representa um dos 16 exemplos de tempos verbais com Question Words e tradução para português.
 */
data class VerbTenseQuestionExample(
    val tenseNumber: Int = 1,
    val tenseName: String = "",
    val questionWord: String = "",
    val englishSentence: String = "",
    val portugueseSentence: String = "",
    val collocationNotes: String = "",
    val ipaTranscription: String = ""
)
