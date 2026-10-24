package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Model representing a mathematically and scientifically restructured page.
 * Eliminates visual bugs in formulas, chemical reactions, biological cytology diagrams,
 * and logic symbols, while providing LaTeX, step-by-step solver proofs, dimensional analysis,
 * study flashcards, and phonetically adapted text for smooth neural TTS reading.
 */
@Entity(
    tableName = "scientific_page_reconstructions",
    indices = [
        Index(value = ["bookId", "pageNumber"], unique = true)
    ]
)
data class ScientificPageReconstruction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val pageNumber: Int,
    val subjectArea: String = "Ciências Exatas & Biológicas", // "Matemática", "Química", "Biologia / Citologia", "Física", "Raciocínio Lógico"
    val pageTitle: String = "",
    val structuredExplanation: String = "", // Clean didactic text without fragmentation or broken lines
    val formulasJson: String = "", // List of ScientificFormulaItem
    val diagramsJson: String = "", // List of ScientificDiagramItem
    val symbolsJson: String = "", // List of ScientificSymbolItem
    val flashcardsJson: String = "", // List of ScientificFlashcardItem
    val ttsOptimizedText: String = "", // Spoken explanation adapted for neural TTS reading
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormulas(): List<ScientificFormulaItem> {
        if (formulasJson.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(formulasJson)
            val list = mutableListOf<ScientificFormulaItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val stepsArr = obj.optJSONArray("stepByStepSolution")
                val steps = mutableListOf<String>()
                if (stepsArr != null) {
                    for (s in 0 until stepsArr.length()) {
                        steps.add(stepsArr.optString(s))
                    }
                }
                list.add(
                    ScientificFormulaItem(
                        id = obj.optString("id", "eq_${i + 1}"),
                        category = obj.optString("category", "Fórmula"),
                        formulaNotation = obj.optString("formulaNotation", ""),
                        latexNotation = obj.optString("latexNotation", ""),
                        dimensionalAnalysis = obj.optString("dimensionalAnalysis", ""),
                        stepByStepSolution = steps,
                        explanation = obj.optString("explanation", ""),
                        spokenTtsGuide = obj.optString("spokenTtsGuide", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getDiagrams(): List<ScientificDiagramItem> {
        if (diagramsJson.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(diagramsJson)
            val list = mutableListOf<ScientificDiagramItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val compArr = obj.optJSONArray("keyComponents")
                val components = mutableListOf<String>()
                if (compArr != null) {
                    for (c in 0 until compArr.length()) {
                        components.add(compArr.optString(c))
                    }
                }
                list.add(
                    ScientificDiagramItem(
                        id = obj.optString("id", "diag_${i + 1}"),
                        labelOrFigure = obj.optString("labelOrFigure", "Esquema"),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        keyComponents = components
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getSymbols(): List<ScientificSymbolItem> {
        if (symbolsJson.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(symbolsJson)
            val list = mutableListOf<ScientificSymbolItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ScientificSymbolItem(
                        symbol = obj.optString("symbol", ""),
                        name = obj.optString("name", ""),
                        contextAndUnit = obj.optString("contextAndUnit", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getFlashcards(): List<ScientificFlashcardItem> {
        if (flashcardsJson.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(flashcardsJson)
            val list = mutableListOf<ScientificFlashcardItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ScientificFlashcardItem(
                        id = obj.optString("id", "card_${i + 1}"),
                        question = obj.optString("question", ""),
                        answer = obj.optString("answer", ""),
                        formulaOrRule = obj.optString("formulaOrRule", ""),
                        hint = obj.optString("hint", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class ScientificFormulaItem(
    val id: String = "",
    val category: String = "Fórmula", // ex: "Química Orgânica", "Cálculo Diferencial", "Citologia / Bioenergética", "Física Mecânica", "Lógica Proposicional"
    val formulaNotation: String = "", // e.g. "C₆H₁₂O₆ + 6O₂ → 6CO₂ + 6H₂O + 38 ATP" ou "∫₀^∞ (sin x / x) dx = π/2"
    val latexNotation: String = "", // e.g. "\int_0^\infty \frac{\sin x}{x} dx = \frac{\pi}{2}" ou "\Delta G^\circ = -RT \ln K"
    val dimensionalAnalysis: String = "", // Análise dimensional e unidades no SI (ex: [J] = kg·m²·s⁻²)
    val stepByStepSolution: List<String> = emptyList(), // Passos da dedução, resolução algébrica ou mecanismo da reação
    val explanation: String = "", // Significado de variáveis e descrição teórica
    val spokenTtsGuide: String = "" // Guia fonético de como ler em voz alta
)

data class ScientificDiagramItem(
    val id: String = "",
    val labelOrFigure: String = "", // ex: "Figura 3.2", "Organela", "Mitocôndria", "Diagrama de Venn", "Gráfico Cartesiano"
    val title: String = "",
    val description: String = "", // Explicação didática visual
    val keyComponents: List<String> = emptyList() // "Cristas mitocondriais", "Matriz", "Membrana externa"
)

data class ScientificSymbolItem(
    val symbol: String = "", // ex: "ΔG", "λ", "pH", "ATP", "∀", "⇌", "ħ"
    val name: String = "", // ex: "Energia Livre de Gibbs", "Comprimento de onda", "Reação em Equilíbrio"
    val contextAndUnit: String = "" // ex: "Bioquímica (kJ/mol)", "Óptica (nm)", "Constante de Planck Reduzida (J·s)"
)

data class ScientificFlashcardItem(
    val id: String = "",
    val question: String = "", // Pergunta para repetição espaçada / teste ativo
    val answer: String = "", // Resposta completa e didática
    val formulaOrRule: String = "", // Notação ou fórmula associada
    val hint: String = "" // Dica mnemônica ou raciocínio intuitivo
)
