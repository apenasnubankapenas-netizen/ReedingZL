package com.example.util

object TextFormatUtils {

    /**
     * Limpa espaços irregulares, múltiplos espaços consecutivos, tabulações e excesso de quebras de linha.
     * Garante alinhamento limpo, estético e proporcional.
     */
    fun cleanTextSpacing(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            // Substitui múltiplos espaços horizontais ou tabs por um único espaço
            .replace(Regex("[ \\t]+"), " ")
            // Remove espaços antes de pontuações comuns
            .replace(Regex(" +([.,;:!?])"), "$1")
            // Reduz 3 ou mais quebras de linha consecutivas para no máximo 2
            .replace(Regex("\\n{3,}"), "\n\n")
            // Trima cada linha individualmente
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()
    }

    /**
     * Remove prefixos de área de conhecimento indevidamente inseridos em títulos de termos.
     * Ex: "Direito Constitucional: Habeas Corpus" -> "Habeas Corpus"
     * Ex: "Área de Conhecimento - Mitocôndria" -> "Mitocôndria"
     * Ex: "[Direito Penal] Coação" -> "Coação"
     */
    fun cleanTermTitle(rawTerm: String): String {
        if (rawTerm.isBlank()) return ""
        var cleaned = rawTerm.trim()

        // Remove tags entre colchetes no início: [Direito], [Física], etc.
        cleaned = cleaned.replace(Regex("^\\[[^\\]]+\\]\\s*"), "")

        // Remove prefixos explícitos de área de conhecimento
        val areaPrefixRegex = Regex(
            "^(?:área(?:\\s+(?:de|do))?\\s+conhecimento|área|disciplina|matéria|campo|tema)\\s*[:\\-–—]\\s*",
            RegexOption.IGNORE_CASE
        )
        cleaned = cleaned.replace(areaPrefixRegex, "")

        // Remove disciplinas tradicionais como prefixo antes de dois-pontos ou hífen
        val subjectPrefixRegex = Regex(
            "^(?:direito\\s+[a-zà-ÿ]+|direito|matemática|física|química|biologia|filosofia|sociologia|história|geografia|português|gramática|economia|administração)\\s*[:\\-–—]\\s*",
            RegexOption.IGNORE_CASE
        )
        cleaned = cleaned.replace(subjectPrefixRegex, "")

        // Remove aspas envolventes se houver
        cleaned = cleaned.removePrefix("\"").removeSuffix("\"").removePrefix("'").removeSuffix("'")

        // Normaliza espaços
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        // Garante primeira letra maiúscula para elegância visual
        return if (cleaned.isNotEmpty()) {
            cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            rawTerm.trim()
        }
    }

    /**
     * Verifica se uma string representa o nome de uma área do conhecimento para evitar
     * que ela seja exibida em badges que quebram o layout do card de termos.
     */
    fun isAreaOfKnowledge(categoryOrName: String): Boolean {
        if (categoryOrName.isBlank()) return false
        val lower = categoryOrName.trim().lowercase()
        return lower.contains("área") ||
                lower.contains("conhecimento") ||
                lower.contains("direito") ||
                lower.contains("física") ||
                lower.contains("matemática") ||
                lower.contains("química") ||
                lower.contains("biologia") ||
                lower.contains("filosofia") ||
                lower.contains("sociologia") ||
                lower.contains("disciplina") ||
                lower.contains("ciência") ||
                lower == "geral"
    }

    /**
     * Normaliza e valida a sigla da banca examinadora de questões.
     * Impede que áreas de conhecimento longas (ex: "DIREITO CONSTITUCIONAL") apareçam
     * deformando o cabeçalho e gerando espaços gigantescos no card.
     */
    fun cleanInstitution(rawInstitution: String, defaultFallback: String = "FGV"): String {
        val trimmed = rawInstitution.trim()
        val upper = trimmed.uppercase()
        return when {
            upper in listOf("FGV", "CEBRASPE", "CESPE", "FCC", "VUNESP", "CESGRANRIO", "QUADRIX", "AOCP", "IADES", "ENEM", "MISTA") -> {
                if (upper == "CESPE") "CEBRASPE" else upper
            }
            upper.contains("FGV") -> "FGV"
            upper.contains("CEBRASPE") || upper.contains("CESPE") -> "CEBRASPE"
            upper.contains("FCC") -> "FCC"
            upper.contains("VUNESP") -> "VUNESP"
            upper.contains("CESGRANRIO") -> "CESGRANRIO"
            upper.contains("QUADRIX") -> "QUADRIX"
            upper.contains("AOCP") -> "AOCP"
            upper.contains("IADES") -> "IADES"
            upper.contains("ENEM") -> "ENEM"
            upper.contains("MISTA") -> "MISTA"
            // Se for nome de matéria/área de conhecimento ou tiver mais de 10 caracteres, usa fallback
            isAreaOfKnowledge(trimmed) || trimmed.length > 10 -> defaultFallback
            trimmed.isNotBlank() -> trimmed.take(8).uppercase()
            else -> defaultFallback
        }
    }

    /**
     * Limpa o texto da alternativa de questão, removendo prefixos de letras ("A) ", "B) ", etc.)
     * que duplicavam a letra já exibida no botão circular e geravam espaçamentos gigantes.
     */
    fun cleanOptionText(rawOption: String): String {
        if (rawOption.isBlank()) return ""
        var cleaned = rawOption.trim()
        // Remove prefixos como "A) ", "B. ", "a) ", "1) ", "C - "
        cleaned = cleaned.replace(Regex("^[A-Ea-e1-5]\\s*[\\)\\.\\-–—:]\\s*"), "")
        // Normaliza espaços e quebras
        return cleanTextSpacing(cleaned)
    }

    /**
     * Limpa o enunciado de questões, removendo linhas de cabeçalho artificiais de área de conhecimento
     * e corrigindo espaçamentos irregulares herdados de extração de PDF.
     */
    fun cleanStatementText(rawStatement: String): String {
        if (rawStatement.isBlank()) return ""
        var cleaned = rawStatement.trim()

        // Remove linhas iniciais como "Área de Conhecimento: Direito Constitucional\n" ou "Disciplina: ...\n"
        val headerAreaRegex = Regex(
            "^(?:área(?:\\s+(?:de|do))?\\s+conhecimento|disciplina|matéria|conteúdo|banca)\\s*[:\\-–—][^\\n]*\\n*",
            RegexOption.IGNORE_CASE
        )
        cleaned = cleaned.replace(headerAreaRegex, "")

        return cleanTextSpacing(cleaned)
    }

    /**
     * Limpa o trecho de contexto motivador (contextSnippet) de questões.
     */
    fun cleanContextSnippet(rawSnippet: String): String {
        if (rawSnippet.isBlank()) return ""
        var cleaned = rawSnippet.trim()
        // Remove cabeçalhos de área
        val headerAreaRegex = Regex(
            "^(?:texto-base|texto\\s+de\\s+apoio|área(?:\\s+(?:de|do))?\\s+conhecimento|disciplina)\\s*[:\\-–—][^\\n]*\\n*",
            RegexOption.IGNORE_CASE
        )
        cleaned = cleaned.replace(headerAreaRegex, "")
        return cleanTextSpacing(cleaned)
    }

    /**
     * Conserta e reconcilia palavras quebradas por hífens de final de linha ou artefatos de OCR/PDF.
     * Ex: "ob- jective" -> "objective", "look- ing" -> "looking", "eat- ing" -> "eating"
     * Ex: "car- ry on" -> "carry on", "ac- complish" -> "accomplish"
     * Preserva hífens legítimos em palavras compostas como "phrasal-verb" ou "well-known".
     */
    fun healBrokenWordsAndHyphens(rawText: String): String {
        if (rawText.isBlank()) return ""
        var text = rawText

        // 1. Reconcilia hífens seguidos de quebra de linha ou espaços: "ex-\nample" ou "ex- ample"
        text = text.replace(Regex("([a-zA-ZÀ-ÿ]{2,})\\s*-\\s*(?:\\r?\\n|\\s+)([a-zA-ZÀ-ÿ]{2,})")) { match ->
            val first = match.groupValues[1]
            val second = match.groupValues[2]
            // Se a segunda parte parece sufixo ou terminação de palavra (ex: ing, ed, tion, ment, ly, etc.)
            // ou se é continuação de palavra comum sem hífen léxico
            val combined = "$first$second"
            combined
        }

        // 2. Corrige contrações quebradas: "do n't" -> "don't", "is n't" -> "isn't", "it 's" -> "it's"
        text = text.replace(Regex("(\\w+)\\s+n't"), "$1n't")
        text = text.replace(Regex("(\\w+)\\s+'([s|m|d|ll|re|ve])"), "$1'$2")

        // 3. Normaliza quebras de linha artificiais no meio de frases
        text = text.replace(Regex("([a-zA-ZÀ-ÿ,;])\\r?\\n([a-zA-ZÀ-ÿ])"), "$1 $2")

        return cleanTextSpacing(text)
    }
}

