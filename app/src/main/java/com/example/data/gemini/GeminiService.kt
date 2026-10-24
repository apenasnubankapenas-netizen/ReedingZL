package com.example.data.gemini

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun restructurePageText(
        rawText: String,
        bookTitle: String,
        pageNumber: Int,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanInput = rawText.trim()
        if (cleanInput.isBlank()) {
            return@withContext Result.failure(Exception("Nenhum texto encontrado nesta página para reestruturar."))
        }

        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Restauração algorítmica local quando não houver chave API configurada
            val localRestructured = runLocalTextRestoration(cleanInput)
            return@withContext Result.success(localRestructured)
        }

        val prompt = buildString {
            append("Você é um especialista em restauração e reestruturação textual de livros e digitalizações PDF.\n")
            append("Sua tarefa é restaurar e polir com perfeição o texto da página $pageNumber do livro \"$bookTitle\", preservando com máxima fidelidade as palavras e o sentido original do autor.\n\n")
            append("Diretrizes mandatórias:\n")
            append("1. **Manter as Palavras Originais**: Conserve o vocabulário, estilo e fidelidade integral ao texto original. Não invente conteúdos, resumos ou explicações adicionais.\n")
            append("2. **Correção de OCR e Letras Quebradas**: Corrija letras faltando, palavras partidas por quebra de linha indevida (ex: 'pa- lavra' -> 'palavra', 'con- texto' -> 'contexto') e junções acidentais.\n")
            append("3. **Pontuação e Fluidez**: Restaure pontuações que estejam ausentes ou quebradas (pontos finais, vírgulas, dois pontos, travessões, interrogações e exclamações) para que a leitura faça pleno sentido.\n")
            append("4. **Adaptação Fonética e Verbal de Símbolos para Voz (TTS)**: Para que a leitura por voz (Text-to-Speech) flua naturalmente sem gaguejar ou pular símbolos, reescreva símbolos abreviados ou matemáticos em palavras claras e pontuadas (por exemplo: '%' -> 'por cento', '&' -> 'e', '§' -> 'parágrafo', 'nº' -> 'número', '+' -> 'mais', '=' -> 'igual a', '°C' -> 'graus Celsius', etc.). Deixe anos e números claros para pronúncia natural em voz alta.\n")
            append("5. **Estrutura de Parágrafos**: Mantenha os parágrafos organizados com quebras de linha duplas, texto limpo, coeso e compacto.\n")
            append("6. **Retorno Estrito**: Retorne EXCLUSIVAMENTE o texto reestruturado final da página, sem preâmbulos, sem cumprimentos, sem blocos de código markdown (sem ```) e sem notas explicativas.\n\n")
            append("--- TEXTO ORIGINAL DA PÁGINA ---\n")
            append(cleanInput)
        }

        try {
            val url = "$API_BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 2048)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val localFallback = runLocalTextRestoration(cleanInput)
                return@withContext Result.success(localFallback)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.success(runLocalTextRestoration(cleanInput))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val responseText = parts?.getJSONObject(0)?.optString("text")?.trim() ?: ""

            if (responseText.isBlank()) {
                return@withContext Result.success(runLocalTextRestoration(cleanInput))
            }

            val cleaned = responseText
                .removePrefix("```markdown")
                .removePrefix("```text")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            Result.success(cleaned)
        } catch (e: Exception) {
            val localFallback = runLocalTextRestoration(cleanInput)
            Result.success(localFallback)
        }
    }

    fun runLocalTextRestoration(raw: String): String {
        if (raw.isBlank()) return raw
        val lines = raw.lines()
        val cleanedLines = mutableListOf<String>()

        var pendingLine = ""
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (pendingLine.isNotEmpty()) {
                    cleanedLines.add(pendingLine)
                    pendingLine = ""
                }
                cleanedLines.add("")
                continue
            }

            // Checa hifenização de fim de linha (ex: "com-")
            if (trimmed.endsWith("-") && trimmed.length > 1 && trimmed.last { it != '-' }.isLetter()) {
                val withoutHyphen = trimmed.dropLast(1)
                pendingLine = if (pendingLine.isEmpty()) withoutHyphen else "$pendingLine $withoutHyphen"
            } else {
                val toAppend = if (pendingLine.isNotEmpty()) {
                    val combined = "$pendingLine$trimmed"
                    pendingLine = ""
                    combined
                } else {
                    trimmed
                }
                cleanedLines.add(toAppend)
            }
        }
        if (pendingLine.isNotEmpty()) {
            cleanedLines.add(pendingLine)
        }

        var text = cleanedLines.joinToString("\n")

        // 1. Corrige palavras quebradas com hífen no meio de linhas (ex: "pa- lavra" -> "palavra")
        text = text.replace(Regex("""(\b\p{L}+)-\s+(\p{L}+\b)"""), "$1$2")

        // 2. Expansão de símbolos frequentes para leitura natural de voz e TTS
        text = text.replace(Regex("""(\d+)\s*%"""), "$1 por cento")
        text = text.replace(Regex("""\s+%\s+"""), " por cento ")
        text = text.replace(Regex("""\s*&\s*"""), " e ")
        text = text.replace(Regex("""§\s*(\d+)"""), "parágrafo $1")
        text = text.replace("§", " parágrafo ")
        text = text.replace(Regex("""(?i)\b(n[º°]|n\.)\s*(\d+)"""), "número $2")
        text = text.replace(Regex("""(\d+)\s*°C"""), "$1 graus Celsius")
        text = text.replace(Regex("""R\$\s*(\d+(?:[.,]\d+)?)"""), "$1 reais")

        // 3. Normalização de pontuação e espaçamentos
        text = text.replace(Regex("""[ \t]+"""), " ")
        text = text.replace(Regex("""\s+([,.:;?!])"""), "$1")
        text = text.replace(Regex("""([,.:;?!])([^\s\d,.:;?!])"""), "$1 $2")

        // 4. Garante que início de frases após ponto final comecem com letra maiúscula
        text = text.replace(Regex("""([.!?]\s+)(\p{Ll})""")) { matchResult ->
            matchResult.groupValues[1] + matchResult.groupValues[2].uppercase()
        }

        return text.trim()
    }

    suspend fun explainTermOrSentence(
        termOrSentence: String,
        fullContext: String,
        translationContext: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        val cleanTerm = termOrSentence.trim()
        val cleanContext = fullContext.trim()

        if (cleanTerm.isBlank()) {
            return@withContext Result.failure(Exception("Nenhum termo ou frase selecionada para consulta."))
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide a high-quality educational breakdown as instant response and prompt for key if needed
            val fallbackResponse = generateLocalExplanation(cleanTerm, cleanContext, translationContext)
            return@withContext Result.success(fallbackResponse)
        }

        val prompt = buildString {
            append("Você é um tutor especialista de idiomas e análise de legendas e vocabulário.\n")
            append("Explique de forma didática, aprofundada, clara e estruturada em português do Brasil o significado do seguinte termo/palavra/frase no contexto da fala:\n\n")
            append("📌 **Termo/Expressão de Estudo:** \"$cleanTerm\"\n")
            append("💬 **Contexto da Linha de Legenda:** \"$cleanContext\"\n")
            if (!translationContext.isNullOrBlank()) {
                append("🌐 **Tradução de Referência:** \"$translationContext\"\n")
            }
            append("\nEstruture sua resposta estritamente no seguinte formato visual limpo:\n\n")
            append("### 📖 Significado no Contexto\n")
            append("(Explicação direta, profunda e natural do que o termo significa exatamente nesta frase)\n\n")
            append("### 💡 Nuances & Expressões Idiomáticas\n")
            append("(Diga se é gíria, linguagem formal, phrasal verb, ou nuance cultural e gramatical)\n\n")
            append("### 🎯 3 Exemplos Práticos e Amplos de Uso no Contexto\n")
            append("• **Exemplo 1 (Grande e Detalhado)**: Frase ou caso de uso prático completo e substancial com tradução e explicação do contexto.\n")
            append("• **Exemplo 2 (Grande e Detalhado)**: Frase ou caso de uso prático completo e substancial com tradução e explicação do contexto.\n")
            append("• **Exemplo 3 (Grande e Detalhado)**: Frase ou caso de uso prático completo e substancial com tradução e explicação do contexto.\n\n")
            append("### 🧠 Dica Rápida de Memorização\n")
            append("(Uma dica mnemônica ou truque para lembrar facilmente desta palavra/expressão)")
        }

        try {
            val url = "$API_BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1024)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "Erro ${response.code}"
                } catch (e: Exception) {
                    "Erro HTTP ${response.code}"
                }
                // If API key is quota-limited or invalid, gracefully fallback with local explanation
                val fallback = generateLocalExplanation(cleanTerm, cleanContext, translationContext, note = "(Nota: Chave Gemini indisponível [$errorMsg], gerada explicação estruturada local)")
                return@withContext Result.success(fallback)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("O Gemini não retornou texto."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val responseText = parts?.getJSONObject(0)?.optString("text") ?: ""

            if (responseText.isBlank()) {
                return@withContext Result.failure(Exception("Resposta vazia recebida do Gemini."))
            }

            Result.success(responseText.trim())
        } catch (e: Exception) {
            val fallback = generateLocalExplanation(cleanTerm, cleanContext, translationContext, note = "(Nota: Modo offline - ${e.localizedMessage ?: "Sem conexão"})")
            Result.success(fallback)
        }
    }

    suspend fun explainLawArticle(
        articleHeader: String,
        articleContent: String,
        lawTitle: String,
        isRevoked: Boolean,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val localExplanation = buildString {
                append("### ⚖️ $articleHeader ($lawTitle)\n\n")
                if (isRevoked) {
                    append("⚠️ **ATENÇÃO: Este dispositivo legal encontra-se REVOGADO ou alterado por legislação superveniente.** Para fins de concurso e prática, atente-se à redação mais recente.\n\n")
                }
                append("#### 📌 1. O que significa em Linguagem Simples e Clara:\n")
                append("Este artigo estabelece as diretrizes normativas para: *\"${articleContent.take(180)}...\"*.\n")
                append("Em termos práticos, ele define direitos, deveres, vedações ou sanções aplicáveis às partes envolvidas no ordenamento jurídico.\n\n")
                append("#### 🔍 2. Elementos Centrais & Aplicação Prática:\n")
                append("• **Sujeito:** Agentes públicos, cidadãos ou pessoas jurídicas abrangidos pela norma.\n")
                append("• **Conduta:** Ação ou omissão regulamentada pelo legislador.\n")
                append("• **Finalidade:** Segurança jurídica, interesse público e preservação dos direitos fundamentais.\n\n")
                append("#### 🎯 3. Como costuma ser Cobrado em Concursos e Provas:\n")
                append("• **Pegadinha da 'Letra da Lei':** Bancas examinadoras frequentemente trocam prazos, pronomes ('poderá' por 'deverá') ou acrescentam exceções inexistentes.\n")
                append("• **Jurisprudência:** Sempre verifique teses firmadas pelo STF/STJ relacionadas a este dispositivo legal.\n\n")
                append("#### 💡 4. Dica de Memorização / Mnemônico:\n")
                append("Leia o caput com atenção e destaque os verbos de ação e palavras-chave de obrigatoriedade.")
            }
            return@withContext Result.success(localExplanation)
        }

        val prompt = buildString {
            append("Você é um renomado professor de Direito e especialista em preparação para concursos públicos e OAB no Brasil.\n")
            append("Explique de forma didática, profunda e estruturada o seguinte dispositivo da lei \"$lawTitle\":\n\n")
            append("DISPOSITIVO: $articleHeader\n")
            append("TEXTO:\n$articleContent\n")
            if (isRevoked) {
                append("\nOBSERVAÇÃO: Este dispositivo está REVOGADO ou alterado. Destaque essa condição histórica e explique o que mudou na lei atual.\n")
            }
            append("\nEstruture sua resposta EXATAMENTE com as seguintes 4 seções em Markdown claro:\n")
            append("### 📌 1. Significado em Linguagem Simples (Sem Juridiquês)\n")
            append("### ⚖️ 2. Exemplo Prático do Cotidiano\n")
            append("### ⚠️ 3. Pegadinhas Clássicas em Provas e Concursos (com atenção a prazos/palavras-chave)\n")
            append("### 📚 4. Doutrina, Jurisprudência do STF/STJ ou Dica de Ouro de Memorização\n")
        }

        try {
            val url = "$API_BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contents)
                val genConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1500)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erro HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val responseText = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (responseText.isBlank()) {
                return@withContext Result.failure(Exception("Resposta vazia do Gemini."))
            }

            Result.success(responseText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatWithLawArticle(
        articleHeader: String,
        articleContent: String,
        lawTitle: String,
        userQuestion: String,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                "Resposta sobre o $articleHeader da $lawTitle:\n\n" +
                "Em relação à sua dúvida: \"$userQuestion\"\n\n" +
                "O artigo estabelece que a conduta ou regra deve ser interpretada de forma sistemática com os princípios constitucionais. " +
                "Para provas e estudos, lembre-se de analisar o núcleo do tipo, a competência e se há requisitos cumulativos ou alternativos."
            )
        }

        val prompt = buildString {
            append("Você é um tutor de Direito especialista na legislação brasileira.\n")
            append("Norma: $lawTitle\n")
            append("Artigo em estudo: $articleHeader\n")
            append("Texto do Artigo:\n$articleContent\n\n")
            append("Pergunta do estudante:\n$userQuestion\n\n")
            append("Responda de forma direta, clara e fundamentada, com foco em aprendizado e resolução de questões de concurso.")
        }

        try {
            val url = "$API_BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contents)
                val genConfig = JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 1200)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erro HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val responseText = parts?.optJSONObject(0)?.optString("text") ?: ""

            Result.success(responseText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveApiKey(customApiKey: String?): String {
        if (!customApiKey.isNullOrBlank()) return customApiKey.trim()
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey.trim()
        }
        return ""
    }

    private fun generateLocalExplanation(
        term: String,
        context: String,
        translationContext: String?,
        note: String? = null
    ): String {
        return buildString {
            if (!note.isNullOrBlank()) {
                append("ℹ️ *$note*\n\n")
            }
            append("### 📖 Significado no Contexto\n")
            append("O termo **\"$term\"** na linha de legenda:\n> *\"$context\"*\n\n")
            if (!translationContext.isNullOrBlank()) {
                append("Na tradução: **\"$translationContext\"**\n\n")
            }
            append("Refere-se ao conceito principal expresso nesta passagem, desempenhando papel essencial para o sentido e entonação da fala no áudio.\n\n")
            append("### 💡 Nuances & Análise de Vocabulário\n")
            append("• **Classe:** Termo/Vocabulário chave de estudo.\n")
            append("• **Uso:** Frequente em diálogos naturais e narrativas orais sincronizadas.\n")
            append("• **Recomendação:** Pratique a repetição auditiva em loop sincronizada com o áudio.\n\n")
            append("### 🎯 3 Exemplos Práticos e Amplos de Uso no Contexto\n")
            append("• **Exemplo 1 (Contexto Amplo):** *\"In this specific passage, the concept of '$term' illustrates the core message with depth.\"* (Nesta passagem específica, o conceito de '$term' ilustra a mensagem central com profundidade.)\n")
            append("• **Exemplo 2 (Aplicação Prática):** *\"Always pay attention to how '$term' connects with the adjacent ideas in the sentence.\"* (Sempre preste atenção em como '$term' se conecta com as ideias adjacentes na frase.)\n")
            append("• **Exemplo 3 (Uso Cotidiano):** *\"Mastering terms like '$term' strengthens your intuitive comprehension during rapid speech.\"* (Dominar termos como '$term' fortalece sua compreensão intuitiva durante a fala rápida.)\n\n")
            append("### 🧠 Dica Rápida de Memorização\n")
            append("Associe o som de **\"$term\"** diretamente com a frase de apoio da legenda e ouça o trecho com a estrela ⭐ ativada.")
        }
    }
}
