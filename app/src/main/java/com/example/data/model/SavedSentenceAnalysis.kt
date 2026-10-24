package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Item de decomposição de token/bloco da frase para análise sintática detalhada.
 */
data class SyntaxTokenItem(
    val token: String,
    val reading: String = "",          // Furigana, Pinyin, Transliteração Cirílica/Grega/Árabe/Hebraica, Tashkeel, Niqqud
    val syntacticRole: String = "",    // Sujeito, Objeto Direto, Partícula は, Conector Subordinado, Verbo Principal, etc.
    val morphologyOrCase: String = "", // Caso Genitivo/Acusativo, V2 Verb, Aspecto Perfeito, Binyan Pa'al, etc.
    val rootOrLemma: String = "",      // Lema de dicionário ou raiz trilítera
    val explanation: String = ""       // Explicação pedagógica contextual
)

/**
 * Regra gramatical ou sintática específica em destaque na frase.
 */
data class GrammarRuleItem(
    val ruleTitle: String,             // ex: "Regência de Caso: Genitivo após preposição", "Partícula de Tópico は vs Sujeito が"
    val category: String,              // "Casos & Declinações", "Partículas & Sintaxe", "Ordem V2 / TeKaMoLo", etc.
    val explanation: String,           // Explicação detalhada da regra no contexto
    val exampleSnippet: String = ""    // Trecho exato da frase analisada
)

/**
 * Entidade Room persistente para salvar análises de sintaxe e regras gramaticais locais.
 * Permite ao leitor salvar frases complexas e estudá-las a qualquer momento offline.
 */
@Entity(tableName = "saved_sentence_analyses")
data class SavedSentenceAnalysis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int = 0,
    val bookTitle: String = "",
    val pageNumber: Int = 1,
    val languageCode: String = "en",
    val sentenceText: String = "",
    val transliteration: String = "",
    val translationPt: String = "",
    val syntacticTreeSummary: String = "",
    val complexityLevel: String = "C1",
    val tokensJson: String = "[]",
    val grammarRulesJson: String = "[]",
    val c2MasteryTip: String = "",
    val practiceChallenge: String = "",
    val isFavorite: Boolean = false,
    val userNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val language: StudyLanguage
        get() = StudyLanguage.fromCode(languageCode)

    fun getTokens(): List<SyntaxTokenItem> {
        if (tokensJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(tokensJson)
            val list = mutableListOf<SyntaxTokenItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SyntaxTokenItem(
                        token = obj.optString("token", ""),
                        reading = obj.optString("reading", ""),
                        syntacticRole = obj.optString("syntacticRole", ""),
                        morphologyOrCase = obj.optString("morphologyOrCase", ""),
                        rootOrLemma = obj.optString("rootOrLemma", ""),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getRules(): List<GrammarRuleItem> {
        if (grammarRulesJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(grammarRulesJson)
            val list = mutableListOf<GrammarRuleItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GrammarRuleItem(
                        ruleTitle = obj.optString("ruleTitle", ""),
                        category = obj.optString("category", ""),
                        explanation = obj.optString("explanation", ""),
                        exampleSnippet = obj.optString("exampleSnippet", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
