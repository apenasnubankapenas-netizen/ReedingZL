package com.example.data.openai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.model.ComparativeLanguageTerm
import com.example.data.model.ComparativeUsageExample
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.ScientificPageReconstruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

object OpenAiService {

    private const val TAG = "OpenAiService"
    private const val CHAT_API_URL = "https://api.openai.com/v1/chat/completions"
    private const val AUDIO_SPEECH_API_URL = "https://api.openai.com/v1/audio/speech"

    val AVAILABLE_VOICES = listOf(
        "alloy" to "Alloy (Equilibrada & Clara)",
        "nova" to "Nova (Dinâmica & Expressiva)",
        "shimmer" to "Shimmer (Suave & Acolhedora)",
        "onyx" to "Onyx (Grave & Autoritária)",
        "echo" to "Echo (Firme & Confiante)",
        "fable" to "Fable (Narrativa & Britânica)"
    )

    val AVAILABLE_MODELS = listOf(
        "gpt-4o-mini" to "GPT-4o Mini (Rápido & Econômico)",
        "gpt-4o" to "GPT-4o (Máxima Capacidade Analítica)",
        "gpt-3.5-turbo" to "GPT-3.5 Turbo"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(context: Context): String = getSavedApiKey(context)

    fun getSavedApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("openai_translator_prefs", Context.MODE_PRIVATE)
        val key = prefs.getString("api_key", "") ?: ""
        return key.trim()
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences("openai_translator_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("api_key", apiKey.trim()).apply()
    }

    fun getSummaryStorageDir(context: Context): File {
        val dir = File(context.filesDir, "openai_summaries")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun generatePageSummary(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookTitle: String,
        pageNumber: Int,
        pageText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (pageText.isBlank()) {
            return@withContext Result.failure(Exception("O conteúdo da página está vazio para análise."))
        }

        try {
            val systemPrompt = """
                Você é um analista literário, educador e sintetizador acadêmico de elite.
                Sua missão é realizar uma ANÁLISE COMPLETA E RESUMO ESTRUTURADO, DENSO E PROFUNDO de uma página de livro.
                
                DIRETRIZES OBRIGATÓRIAS DE CONTEÚDO E ESTRUTURA:
                1. TODAS as explicações que o resumo proporcionar DEVEM SEMPRE vir acompanhadas de 3 EXEMPLOS AMPLOS E ROBUSTOS sobre o que aquele texto está falando.
                2. Os 3 exemplos devem ser GRANDES, ricos em detalhes contextuais, aplicações práticas e ilustrações aprofundadas que expliquem com máxima clareza o conteúdo do texto original.
                3. Formate o texto em parágrafos bem densos, coesos e ideais para leitura justificada e narração em áudio.
                4. Estruture o resumo rigorosamente nas seguintes seções:
                   - 📌 **TEMA CENTRAL & CONTEXTO**: O núcleo do que a página aborda e seu enquadramento.
                   - 🔍 **ANÁLISE TEMÁTICA & EXPLICAÇÃO DETALHADA**: Dissecando cada tema, conceito, argumento ou raciocínio individual contido no trecho.
                   - 💡 **CONCEITOS-CHAVE & FUNDAMENTAÇÃO**: As ideias mais valiosas, terminologias e lições essenciais.
                   - 🌟 **3 EXEMPLOS AMPLOS E DETALHADOS DO CONTEÚDO**:
                     • **Exemplo 1 (Grande e Ilustrativo)**: Explicação aprofundada com um caso prático ou analogia substancial demonstrando a primeira ideia central do texto.
                     • **Exemplo 2 (Grande e Ilustrativo)**: Explicação aprofundada com um caso prático ou analogia substancial demonstrando a segunda ideia central do texto.
                     • **Exemplo 3 (Grande e Ilustrativo)**: Explicação aprofundada com um caso prático ou analogia substancial demonstrando a terceira ideia central do texto.
                   - 🎯 **SÍNTESE & CONCLUSÃO**: Resumo integrador da mensagem que fecha a página.
                5. Linguagem elegante, clara, sem preenchimentos superficiais, com foco no enriquecimento intelectual do leitor.
                6. Responda em Português do Brasil de forma fluida.
            """.trimIndent()

            val userContent = """
                Livro: $bookTitle
                Página: $pageNumber
                
                Texto Original da Página:
                $pageText
            """.trimIndent()

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.4)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyStr)
                        errObj.optJSONObject("error")?.optString("message") ?: bodyStr
                    } catch (e: Exception) {
                        "Erro ${response.code}: $bodyStr"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("O resumo retornado pela IA está em branco."))
                }

                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating page summary: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateSpeechAudio(
        context: Context,
        apiKey: String,
        textToSpeak: String,
        voice: String = "alloy",
        bookId: Int,
        pageNumber: Int
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }

        val storageDir = getSummaryStorageDir(context)
        val audioFile = File(storageDir, "audio_summary_b${bookId}_p${pageNumber}_${voice}.mp3")

        // Prepare text for speech: clean markdown markers (*, #, _, -) so narration is natural
        val cleanSpeechText = textToSpeak
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("\\n+"), " ")
            .trim()

        // OpenAI TTS supports up to 4096 characters per request
        val truncatedSpeechText = if (cleanSpeechText.length > 4000) cleanSpeechText.take(4000) else cleanSpeechText

        try {
            val payload = JSONObject().apply {
                put("model", "tts-1")
                put("input", truncatedSpeechText)
                put("voice", voice.lowercase())
                put("response_format", "mp3")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(AUDIO_SPEECH_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errObj = JSONObject(errBody)
                        errObj.optJSONObject("error")?.optString("message") ?: errBody
                    } catch (e: Exception) {
                        "Erro ${response.code}: $errBody"
                    }
                    return@withContext Result.failure(Exception("Falha na Voz OpenAI: $errorMsg"))
                }

                val responseBody = response.body ?: return@withContext Result.failure(Exception("Corpo de áudio vazio."))
                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(audioFile).use { outputStream ->
                        val buffer = ByteArray(32768)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }

                if (audioFile.exists() && audioFile.length() > 0) {
                    Result.success(audioFile)
                } else {
                    Result.failure(Exception("O arquivo de áudio gerado está vazio."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating OpenAI speech audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateSpeechAudio(
        apiKey: String,
        text: String,
        voice: String = "nova",
        cacheKey: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }

        val cleanSpeechText = text
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("\\n+"), " ")
            .trim()

        val truncatedSpeechText = if (cleanSpeechText.length > 4000) cleanSpeechText.take(4000) else cleanSpeechText
        val tempFile = File.createTempFile("tts_${cacheKey}_", ".mp3")

        try {
            val payload = JSONObject().apply {
                put("model", "tts-1")
                put("input", truncatedSpeechText)
                put("voice", voice.lowercase())
                put("response_format", "mp3")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(AUDIO_SPEECH_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errObj = JSONObject(errBody)
                        errObj.optJSONObject("error")?.optString("message") ?: errBody
                    } catch (e: Exception) {
                        "Erro ${response.code}: $errBody"
                    }
                    return@withContext Result.failure(Exception("Falha na Voz OpenAI: $errorMsg"))
                }

                val responseBody = response.body ?: return@withContext Result.failure(Exception("Corpo de áudio vazio."))
                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(32768)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    Result.success(tempFile)
                } else {
                    Result.failure(Exception("O arquivo de áudio gerado está vazio."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating OpenAI speech audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getDictionaryStorageDir(context: Context): File {
        val dir = File(context.filesDir, "openai_dictionary_audios")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun generateDictionarySectionAudio(
        context: Context,
        apiKey: String,
        textToSpeak: String,
        voice: String = "alloy",
        termId: Int,
        sectionKey: String // "original", "spanish", "french", "english"
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }

        val storageDir = getDictionaryStorageDir(context)
        val textHash = Math.abs(textToSpeak.hashCode())
        val audioFile = File(storageDir, "audio_dict_${termId}_${sectionKey}_${voice}_${textHash}.mp3")

        val cleanSpeechText = textToSpeak
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("\\n+"), " ")
            .trim()

        val truncatedSpeechText = if (cleanSpeechText.length > 4000) cleanSpeechText.take(4000) else cleanSpeechText

        try {
            val payload = JSONObject().apply {
                put("model", "tts-1")
                put("input", truncatedSpeechText)
                put("voice", voice.lowercase())
                put("response_format", "mp3")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(AUDIO_SPEECH_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errObj = JSONObject(errBody)
                        errObj.optJSONObject("error")?.optString("message") ?: errBody
                    } catch (e: Exception) {
                        "Erro ${response.code}: $errBody"
                    }
                    return@withContext Result.failure(Exception("Falha na Voz OpenAI: $errorMsg"))
                }

                val responseBody = response.body ?: return@withContext Result.failure(Exception("Corpo de áudio vazio."))
                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(audioFile).use { outputStream ->
                        val buffer = ByteArray(32768)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }

                if (audioFile.exists() && audioFile.length() > 0) {
                    Result.success(audioFile)
                } else {
                    Result.failure(Exception("O arquivo de áudio gerado está vazio."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating dictionary section audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts difficult, legal, complex, compound, and notable terms from the given page text,
     * generating definitions and 3 language versions (Spanish, French, English) with rich illustrative examples.
     */
    suspend fun extractUniversalDictionaryTerms(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookId: Int,
        pageNumber: Int,
        bookTitle: String,
        pageText: String
    ): Result<List<PageDictionaryTerm>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }

        if (pageText.isBlank()) {
            return@withContext Result.failure(Exception("O texto da página está vazio."))
        }

        try {
            val systemPrompt = """
                Você é um lexicógrafo poliglota, jurista, linguista e professor de elite especializado em análise textual minuciosa e criação de Dicionários Universais Enciclopédicos Multilíngues.
                
                SUA MISSÃO:
                Analisar minuciosamente o texto fornecido da página de livro e extrair UMA QUANTIDADE ABUNDANTE E COMPLETA de termos e expressões (extraia tantos termos quanto forem pertinentes, de 8 a 25+ termos se o texto for denso).
                
                VOCÊ DEVE BUSCAR ATIVAMENTE:
                1. SUBSTANTIVOS diversos:
                   - Nomes de processos, fenômenos, procedimentos e técnicas.
                   - Nomes de conceitos abstratos, instituições, doutrinas e teorias.
                   - Nomes de objetos, formas, estruturas, documentos e instrumentos.
                   - Nomes de situações, estados, condições, eventos e relações humanas/sociais.
                2. TERMOS JURÍDICOS E LEGAIS COMPLEXOS:
                   - Conceitos como coação, recurso, turma recursal, litisconsórcio, preclusão, decadência, prescrição, sucumbência, tutela, lide, dolo, culpa, etc.
                   - Explicar DETALHE POR DETALHE cada instituto jurídico com precisão cirúrgica.
                3. VERBOS RAROS, ERUDITOS OU DIFÍCEIS:
                   - Verbos de ação conceitual, processual, jurídica ou literária (ex: avocar, dirimir, precluir, exaurir, coagir, outorgar, etc.).
                4. TERMOS COMPOSTOS E EXPRESSÕES IDIOMÁTICAS/LOCUÇÕES:
                   - Locuções substantivas, adjetivas ou verbais de alta relevância.
                5. TERMOS FILOSÓFICOS, ACADÊMICOS E CIENTÍFICOS:
                   - Conceitos hermenêuticos, epistemológicos, sociológicos e técnicos.
                
                REQUISITOS PARA CADA TERMO EXTRAÍDO:
                Para CADA termo selecionado, gere:
                1. "term": O termo exato ou expressão no idioma original do texto.
                   REGRA RIGOROSA: NUNCA coloque o nome da área de conhecimento (como "Direito", "Física", "Biologia", "Matemática", etc.) como parte do termo nem como prefixo (NUNCA coloque "Direito: Coação" ou "Área: Preclusão"). O campo "term" deve conter ESTRITAMENTE o termo ou expressão em si (ex: "Coação", "Preclusão", "Entropia", "Tutela Antecipada").
                2. "category": Categoria gramatical/estrutural precisa (ex: "Substantivo", "Processo", "Verbo Complexo", "Termo Composto", "Conceito"). NUNCA use o nome de matérias escolares ou acadêmicas como categoria.
                3. "originalContext": Trecho/frase da página onde o termo ocorre (sem espaços múltiplos ou quebras anormais).
                4. "originalDefinition": Explicação DETALHADA, CONCRETA e PROFUNDA do significado VERDADEIRO da palavra no mundo real (o que significa, o que é, o que representa e como opera). REGRA PROIBITIVA: NUNCA use frases genéricas, vazias ou meta-frases burocráticas como 'conceito e vocábulo substantivo empregado no discurso para delimitar com precisão terminológica o elemento em discussão' ou qualquer fraseologia genérica similar. Forneça sempre o significado autêntico e substancial.
                5. "spanishTerm": Termo correspondente em ESPANHOL.
                6. "spanishDefinition": Significado e explicação clara em Espanhol.
                7. "spanishExample": Exemplo amplo, robusto e ilustrativo em Espanhol explicando a aplicação do termo com tradução.
                8. "frenchTerm": Termo correspondente em FRANCÊS.
                9. "frenchDefinition": Significado e explicação clara em Francês.
                10. "frenchExample": Exemplo amplo, robusto e ilustrativo em Francês explicando a aplicação do termo com tradução.
                11. "englishTerm": Termo correspondente em INGLÊS.
                12. "englishDefinition": Significado e explicação clara em Inglês.
                13. "englishExample": Exemplo amplo, robusto e ilustrativo em Inglês explicando a aplicação do termo com tradução.

                FORMATO DE SAÍDA OBRIGATÓRIO:
                Retorne ESTRITAMENTE um array JSON válido de objetos, sem nenhum texto antes ou depois.
                [
                  {
                    "term": "Termo ou Expressão",
                    "category": "Jurídico",
                    "originalContext": "Frase onde aparece na página...",
                    "originalDefinition": "Explicação detalhada do significado...",
                    "spanishTerm": "Término en español",
                    "spanishDefinition": "Definición clara en español...",
                    "spanishExample": "Ejemplo amplio y explicativo en español... (Tradução)",
                    "frenchTerm": "Terme en français",
                    "frenchDefinition": "Définition claire en français...",
                    "frenchExample": "Exemple complet et explicatif en français... (Tradução)",
                    "englishTerm": "Term in English",
                    "englishDefinition": "Clear definition in English...",
                    "englishExample": "Comprehensive illustrative example in English... (Tradução)"
                  }
                ]
            """.trimIndent()

            val userContent = """
                Livro: $bookTitle
                Página: $pageNumber
                
                --- TEXTO DA PÁGINA ---
                $pageText
                --- FIM DO TEXTO ---
                
                Realize a extração detalhada de termos difíceis, substantivos, processos, termos jurídicos (como coação, recurso, etc.), verbos e expressões compostas com explicações minuciosas e versões completas em Espanhol, Francês e Inglês com exemplos amplos. Retorne em JSON.
            """.trimIndent()

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            }

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.3)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errObj = JSONObject(errBody)
                        errObj.optJSONObject("error")?.optString("message") ?: errBody
                    } catch (e: Exception) {
                        "Erro ${response.code}: $errBody"
                    }
                    return@withContext Result.failure(Exception("Falha na API OpenAI: $errorMsg"))
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Nenhuma resposta recebida do modelo."))
                }

                val content = choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim()
                val parsedTerms = parseTermsJson(content, bookId, pageNumber, bookTitle)
                
                if (parsedTerms.isEmpty()) {
                    return@withContext Result.failure(Exception("Não foi possível extrair termos estruturados do texto. Tente novamente."))
                }

                Result.success(parsedTerms)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting dictionary terms: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseTermsJson(
        rawContent: String,
        bookId: Int,
        pageNumber: Int,
        bookTitle: String
    ): List<PageDictionaryTerm> {
        val cleanJson = rawContent
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

        val terms = mutableListOf<PageDictionaryTerm>()

        try {
            // Find start and end of JSON array if wrapped
            val startIndex = cleanJson.indexOf('[')
            val endIndex = cleanJson.lastIndexOf(']')
            val jsonArrayString = if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleanJson.substring(startIndex, endIndex + 1)
            } else {
                cleanJson
            }

            val array = JSONArray(jsonArrayString)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rawTermName = obj.optString("term", "").trim()
                val cleanTerm = com.example.util.TextFormatUtils.cleanTermTitle(rawTermName)
                if (cleanTerm.isBlank()) continue

                val item = PageDictionaryTerm(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    bookTitle = bookTitle,
                    term = cleanTerm,
                    category = obj.optString("category", "Geral").trim(),
                    originalContext = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("originalContext", "")),
                    originalDefinition = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("originalDefinition", "")),
                    spanishTerm = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("spanishTerm", "")),
                    spanishDefinition = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("spanishDefinition", "")),
                    spanishExample = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("spanishExample", "")),
                    frenchTerm = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("frenchTerm", "")),
                    frenchDefinition = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("frenchDefinition", "")),
                    frenchExample = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("frenchExample", "")),
                    englishTerm = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("englishTerm", "")),
                    englishDefinition = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("englishDefinition", "")),
                    englishExample = com.example.util.TextFormatUtils.cleanTextSpacing(obj.optString("englishExample", ""))
                )
                terms.add(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing terms JSON array: ${e.message}", e)
        }

        return terms
    }

    suspend fun explainTermOrSentence(
        context: Context,
        termOrSentence: String,
        fullContext: String,
        translationContext: String? = null,
        customApiKey: String? = null,
        model: String = "gpt-4o-mini"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.trim()?.ifBlank { null } ?: getSavedApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada. Insira sua OpenAI API Key para gerar a explicação."))
        }
        if (termOrSentence.isBlank() && fullContext.isBlank()) {
            return@withContext Result.failure(Exception("Nenhum texto selecionado para explicação."))
        }

        try {
            val systemPrompt = """
                Você é um professor, linguista e analista literário e educacional de elite.
                Sua missão é fornecer uma EXPLICAÇÃO CLARA, RICA, DENSA E PROFUNDA de uma frase, termo ou seção de texto.
                
                DIRETRIZES OBRIGATÓRIAS DE CONTEÚDO:
                1. Explique o significado exato, o sentido contextual e a intenção da frase ou termo no trecho.
                2. SEMPRE inclua OBRIGATORIAMENTE 3 EXEMPLOS AMPLOS, ROBUSTOS E PRÁTICOS de uso ou aplicação do conceito/frase.
                3. Os 3 exemplos devem ser explicativos, demonstrando diferentes nuances da ideia.
                4. Estruture a resposta com clareza nos seguintes tópicos:
                   - 📖 **SIGNIFICADO & CONTEXTO**: O que a frase/termo significa e sua mensagem central.
                   - 🔍 **ANÁLISE DE NUANCES & VOCABULÁRIO**: Estrutura da linguagem, figuras de estilo ou conceitos-chave.
                   - 🌟 **3 EXEMPLOS AMPLOS E APLICADOS**:
                     • **Exemplo 1**: Aplicação prática e detalhada demonstrando o primeiro sentido.
                     • **Exemplo 2**: Aplicação em outro cenário prático ou formal demonstrando o segundo sentido.
                     • **Exemplo 3**: Aplicação em conversação ou raciocínio demonstrando o terceiro sentido.
                   - 💡 **SÍNTESE & DICA DE FIXAÇÃO**: Conclusão para memorização e enriquecimento do leitor.
                5. Responda em Português do Brasil de forma clara, natural e didática.
            """.trimIndent()

            val userContent = buildString {
                appendLine("Termo / Seção selecionada: \"$termOrSentence\"")
                if (fullContext.isNotBlank() && fullContext != termOrSentence) {
                    appendLine("Contexto completo da frase/parágrafo: \"$fullContext\"")
                }
                if (!translationContext.isNullOrBlank()) {
                    appendLine("Contexto de tradução: \"$translationContext\"")
                }
            }

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.4)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyStr)
                        errObj.optJSONObject("error")?.optString("message") ?: bodyStr
                    } catch (e: Exception) {
                        "Erro ${response.code}: $bodyStr"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("A explicação retornada pela IA está em branco."))
                }

                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating OpenAI explanation: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts linguistic comparative terms from dictionary pages with:
     * - 3 principal language translations: English, French, and Spanish
     * - Term meaning and original context
     * - International Phonetic Alphabet (IPA) for non-Latin and phonetic reference
     * - 16 verb tenses of illustrative usage examples (half affirmative, half question)
     * - All modal verbs (can, could, may, might, must, should, will, would)
     * - Connectives (however, although, because, furthermore, whereas, etc.)
     */
    suspend fun extractComparativeLanguageTerms(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookId: Int,
        pageNumber: Int,
        bookTitle: String,
        pageText: String
    ): Result<List<ComparativeLanguageTerm>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }

        if (pageText.isBlank()) {
            return@withContext Result.failure(Exception("O texto da página do dicionário está vazio."))
        }

        try {
            val systemPrompt = """
                Você é um Mestre Lexicógrafo Poliglota, Linguista Comparativo e Filólogo de Elite especializado em Dicionários, Enciclopédias e Gramática Contrastiva.

                SUA MISSÃO:
                Analisar o texto da página de dicionário/enciclopédia fornecida e extrair todos os termos, palavras, verbos, locuções, frases verbais e substantivos de relevância (extraia entre 4 e 15 termos por página, dependendo da densidade).

                REQUISITOS ESTRITOS PARA CADA TERMO EXTRAÍDO:
                1. "term": O termo exato na língua original do texto.
                2. "grammaticalCategory": Classe gramatical precisa (ex: "Verbo", "Substantivo", "Expressão / Frase Verbal", "Adjetivo", "Locução Prepositiva").
                3. "originalContext": Trecho onde aparece na página.
                4. "meaning": O significado profundo e a definição minuciosa do termo.
                5. "ipaTranscription": Transcrição fonética no Alfabeto Fonético Internacional (IPA) para pronúncia exata.
                6. TRADUÇÕES E DEFINIÇÕES MULTILÍNGUES OBRIGATÓRIAS:
                   - "portugueseTerm", "portugueseMeaning", "portugueseIpa" (PORTUGUÊS - OBRIGATÓRIO: Se o texto original estiver em inglês, francês, espanhol ou outro idioma estrangeiro, você DEVE FORNECER A TRADUÇÃO EXATA E A DEFINIÇÃO EM PORTUGUÊS! Se o original já for português, mantenha em português).
                   - "englishTerm", "englishMeaning", "englishIpa" (INGLÊS)
                   - "frenchTerm", "frenchMeaning", "frenchIpa" (FRANCÊS)
                   - "spanishTerm", "spanishMeaning", "spanishIpa" (ESPANHOL)
                7. "usageExamples": Array com EXATAMENTE 16 TEMPOS VERBAIS de exemplos de uso ilustrativos e ricos:
                   - Metade (8 exemplos) são AFIRMAÇÕES ("type": "Afirmação").
                   - Metade (8 exemplos) são PERGUNTAS ("type": "Pergunta").
                   - Empregue TODOS OS MODAIS POSSÍVEIS ao longo dos exemplos (can, could, must, should, may, might, will, would, ought to).
                   - Empregue QUESTION WORDS ao longo das perguntas (Why, How, When, Where, Which, What, Whom).
                   - Empregue CONECTIVOS ricos e variados ao longo dos exemplos (although, however, nevertheless, furthermore, because, whereas, in order to, therefore).
                   - Para CADA um dos 16 exemplos, forneça:
                     * "tenseNumber": 1 a 16
                     * "tenseName": Nome do tempo verbal (ex: "1. Present Simple", "2. Present Continuous", "3. Present Perfect", "4. Present Perfect Continuous", "5. Past Simple", "6. Past Continuous", "7. Past Perfect", "8. Past Perfect Continuous", "9. Future Simple (will)", "10. Future Continuous", "11. Future Perfect", "12. Future Perfect Continuous", "13. Conditional Simple (would)", "14. Conditional Continuous", "15. Conditional Perfect", "16. Imperative / Subjunctive / Modal Perfect")
                     * "type": "Afirmação" ou "Pergunta"
                     * "modalOrConnector": Nota indicando o modal ou conectivo utilizado (ex: "Modal: can | Conectivo: although")
                     * "englishSentence": Frase de exemplo em Inglês
                     * "frenchSentence": Frase correspondente em Francês
                     * "spanishSentence": Frase correspondente em Espanhol
                     * "portugueseSentence": Tradução fiel em Português
                     * "ipaEnglish": Transcrição fonética IPA da frase em Inglês (ou principal)

                FORMATO DE SAÍDA:
                Retorne ESTRITAMENTE um array JSON válido sem nenhum markdown ou texto antes/depois:
                [
                  {
                    "term": "termo",
                    "grammaticalCategory": "Verbo",
                    "originalContext": "trecho do livro...",
                    "meaning": "definição detalhada...",
                    "ipaTranscription": "/.../",
                    "portugueseTerm": "tradução em português",
                    "portugueseMeaning": "definição e significado em português...",
                    "portugueseIpa": "/.../",
                    "englishTerm": "...",
                    "englishMeaning": "...",
                    "englishIpa": "/.../",
                    "frenchTerm": "...",
                    "frenchMeaning": "...",
                    "frenchIpa": "/.../",
                    "spanishTerm": "...",
                    "spanishMeaning": "...",
                    "spanishIpa": "/.../",
                    "usageExamples": [
                      {
                        "tenseNumber": 1,
                        "tenseName": "Present Simple",
                        "type": "Afirmação",
                        "modalOrConnector": "Conectivo: however",
                        "englishSentence": "...",
                        "frenchSentence": "...",
                        "spanishSentence": "...",
                        "portugueseSentence": "...",
                        "ipaEnglish": "/.../"
                      }
                    ]
                  }
                ]
            """.trimIndent()

            val userContent = """
                Livro (Dicionário): $bookTitle
                Página: $pageNumber

                --- TEXTO DA PÁGINA DO DICIONÁRIO ---
                $pageText
                --- FIM DO TEXTO ---

                Extraia todos os termos e expressões com suas traduções nas línguas principais (Português, Inglês, Francês, Espanhol), IPA, significado e os 16 tempos verbais de exemplos de uso (8 afirmações, 8 perguntas, todos os modais e conectivos). Se a página estiver em inglês ou outro idioma estrangeiro, garanta a tradução precisa de cada termo para o Português. Retorne em JSON.
            """.trimIndent()

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            }

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.3)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errObj = JSONObject(errBody)
                        errObj.optJSONObject("error")?.optString("message") ?: errBody
                    } catch (e: Exception) {
                        "Erro ${response.code}: $errBody"
                    }
                    return@withContext Result.failure(Exception("Falha na API OpenAI: $errorMsg"))
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Nenhuma resposta recebida do modelo."))
                }

                val content = choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim()
                val parsedTerms = parseComparativeTermsJson(content, bookId, pageNumber, bookTitle)

                if (parsedTerms.isEmpty()) {
                    return@withContext Result.failure(Exception("Não foi possível extrair os comparativos linguísticos da página. Tente novamente."))
                }

                Result.success(parsedTerms)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting comparative language terms: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseComparativeTermsJson(
        rawContent: String,
        bookId: Int,
        pageNumber: Int,
        bookTitle: String
    ): List<ComparativeLanguageTerm> {
        val cleanJson = rawContent
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

        val terms = mutableListOf<ComparativeLanguageTerm>()

        try {
            val startIndex = cleanJson.indexOf('[')
            val endIndex = cleanJson.lastIndexOf(']')
            val jsonArrayString = if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleanJson.substring(startIndex, endIndex + 1)
            } else {
                cleanJson
            }

            val array = JSONArray(jsonArrayString)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val termName = obj.optString("term", "").trim()
                if (termName.isBlank()) continue

                val usageExamplesArr = obj.optJSONArray("usageExamples")
                val usageJsonString = usageExamplesArr?.toString() ?: "[]"

                val item = ComparativeLanguageTerm(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    bookTitle = bookTitle,
                    term = termName,
                    grammaticalCategory = obj.optString("grammaticalCategory", "Geral").trim(),
                    originalContext = obj.optString("originalContext", "").trim(),
                    meaning = obj.optString("meaning", "").trim(),
                    ipaTranscription = obj.optString("ipaTranscription", "").trim(),
                    englishTerm = obj.optString("englishTerm", "").trim(),
                    englishMeaning = obj.optString("englishMeaning", "").trim(),
                    englishIpa = obj.optString("englishIpa", "").trim(),
                    frenchTerm = obj.optString("frenchTerm", "").trim(),
                    frenchMeaning = obj.optString("frenchMeaning", "").trim(),
                    frenchIpa = obj.optString("frenchIpa", "").trim(),
                    spanishTerm = obj.optString("spanishTerm", "").trim(),
                    spanishMeaning = obj.optString("spanishMeaning", "").trim(),
                    spanishIpa = obj.optString("spanishIpa", "").trim(),
                    portugueseTerm = obj.optString("portugueseTerm", "").trim().ifBlank { termName },
                    portugueseMeaning = obj.optString("portugueseMeaning", "").trim().ifBlank { obj.optString("meaning", "").trim() },
                    portugueseIpa = obj.optString("portugueseIpa", "").trim(),
                    usageExamplesJson = usageJsonString
                )
                terms.add(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing comparative terms JSON: ${e.message}", e)
        }

        return terms
    }

    private fun bitmapToBase64(bitmap: android.graphics.Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        val maxDim = 512
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    /**
     * Classifies a book's category and subcategory using the OpenAI API (GPT-4o-mini with Vision),
     * matching against the complete 38-category taxonomy requested by the user.
     * If no API key is available or network fails, falls back gracefully to taxonomy heuristics.
     */
    suspend fun classifyBookCategory(
        context: Context,
        bookTitle: String,
        bookAuthor: String,
        coverBitmap: android.graphics.Bitmap? = null,
        sampleText: String? = null
    ): Result<com.example.data.model.BookClassificationResult> = withContext(Dispatchers.IO) {
        val apiKey = getSavedApiKey(context)
        if (apiKey.isBlank()) {
            val heuristic = com.example.data.model.BookCategoryTaxonomy.classifyByHeuristics(bookTitle, bookAuthor, sampleText)
            return@withContext Result.success(heuristic)
        }

        try {
            val categoriesListing = com.example.data.model.BookCategoryTaxonomy.CATEGORIES.joinToString("\n") { cat ->
                "- ${cat.name}: ${cat.subcategories.joinToString(", ")}"
            }

            val systemPrompt = """
                Você é um bibliotecário e classificador acadêmico de livros de altíssima precisão.
                Sua tarefa é classificar o livro analisando a imagem da sua capa (se fornecida), seu título e autor.
                
                Você DEVE escolher OBRIGATORIAMENTE uma das 38 categorias principais abaixo e selecionar a subcategoria correspondente mais precisa da lista:
                
                $categoriesListing
                
                Responda ESTRITAMENTE em formato JSON puro:
                {
                  "category": "NomeExatoDaCategoria",
                  "subcategory": "NomeExatoDaSubcategoria",
                  "colorHex": "#HexCodeSugerido"
                }
            """.trimIndent()

            val contentArray = JSONArray()
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", "Classifique a categoria deste livro:\nTítulo: $bookTitle\nAutor: $bookAuthor\nTrecho/Amostra: ${sampleText?.take(600) ?: "N/A"}")
            })

            if (coverBitmap != null) {
                try {
                    val base64 = bitmapToBase64(coverBitmap)
                    contentArray.put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64")
                        })
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao codificar imagem da capa: ${e.message}")
                }
            }

            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.1)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    })
                })
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w(TAG, "OpenAI classification HTTP error: ${response.code}, falling back to heuristics")
                    val fallback = com.example.data.model.BookCategoryTaxonomy.classifyByHeuristics(bookTitle, bookAuthor, sampleText)
                    return@withContext Result.success(fallback)
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    val fallback = com.example.data.model.BookCategoryTaxonomy.classifyByHeuristics(bookTitle, bookAuthor, sampleText)
                    return@withContext Result.success(fallback)
                }

                val rawContent = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                val resJson = JSONObject(rawContent)

                val returnedCat = resJson.optString("category", "").trim()
                val returnedSub = resJson.optString("subcategory", "").trim()
                val returnedHex = resJson.optString("colorHex", "").trim()

                val matchedDef = com.example.data.model.BookCategoryTaxonomy.getCategoryByName(returnedCat)
                val finalCategory = matchedDef?.name ?: returnedCat.ifBlank { "Reference" }
                val finalSubcategory = if (returnedSub.isNotBlank()) returnedSub else matchedDef?.subcategories?.firstOrNull()
                val finalHex = if (returnedHex.isNotBlank() && returnedHex.startsWith("#")) returnedHex else matchedDef?.defaultColorHex ?: "#8E2DE2"

                Result.success(
                    com.example.data.model.BookClassificationResult(
                        category = finalCategory,
                        subcategory = finalSubcategory,
                        colorHex = finalHex
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in classifyBookCategory, falling back to heuristics: ${e.message}", e)
            val fallback = com.example.data.model.BookCategoryTaxonomy.classifyByHeuristics(bookTitle, bookAuthor, sampleText)
            Result.success(fallback)
        }
    }

    suspend fun translatePageToTrilateralLanguages(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookTitle: String,
        pageNumber: Int,
        pageText: String
    ): Result<com.example.data.model.PageMultiTranslation> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (pageText.isBlank()) {
            return@withContext Result.failure(Exception("Texto da página está vazio para tradução."))
        }

        try {
            val systemPrompt = """
                Você é um tradutor literário e acadêmico poliglota de altíssimo nível internacional.
                Sua missão fundamental é pegar o texto completo de uma página de livro e fornecer versões integrais em QUATRO IDIOMAS FUNDAMENTAIS:
                1. Português (Português do Brasil) - REGRA ABSOLUTAMENTE OBRIGATÓRIA:
                   - Se o texto original da página estiver em INGLÊS, FRANCÊS, ESPANHOL, ALEMÃO, ITALIANO ou qualquer outro idioma estrangeiro, você DEVE REALIZAR UMA TRADUÇÃO COMPLETA, NATURAL E IMPECÁVEL PARA O PORTUGUÊS! O campo "portuguese" NUNCA DEVE FICAR EM INGLÊS OU NO IDIOMA ORIGINAL ESTRANGEIRO!
                   - Se o texto original já estiver em português, mantenha e aprimore o texto em português.
                2. Inglês (English) - Tradução integral para o inglês (ou mantenha em inglês com alta fidelidade se o original já for inglês).
                3. Francês (Français) - Tradução integral para o francês.
                4. Espanhol (Español) - Tradução integral para o espanhol.

                DIRETRIZES DE TRADUÇÃO:
                - Preserve com fidelidade o sentido, nuances literárias, estilo, ritmo e registro formal do texto original.
                - Não resuma, não corte trechos e não adicione comentários próprios fora da tradução.
                - Mantenha a divisão e correspondência dos parágrafos de modo que cada parágrafo original tenha sua tradução correspondente exata.
                - Em cada item do array "paragraphs", o campo "portuguese" DEVE OBRIGATORIAMENTE conter o texto em português (traduzido com perfeição se o livro for em outro idioma).

                FORMATO DE RETORNO OBRIGATÓRIO (JSON PURO):
                Responda APENAS com um objeto JSON válido no formato abaixo, sem delimitadores markdown adicionais:
                {
                  "portuguese": "Texto completo integral em PORTUGUÊS (tradução fiel para o português se o livro for estrangeiro) com parágrafos separados por duas quebras de linha (\n\n)",
                  "english": "Texto completo integral traduzido para o inglês com parágrafos separados por duas quebras de linha (\n\n)",
                  "french": "Texte intégral complet traduit en français com parágrafos separados por duas quebras de linha (\n\n)",
                  "spanish": "Texto completo integral traducido al español com parágrafos separados por duas quebras de linha (\n\n)",
                  "paragraphs": [
                    {
                      "paragraphIndex": 0,
                      "portuguese": "Tradução fiel deste parágrafo específico para o Português",
                      "english": "English translation of this specific paragraph",
                      "french": "Traduction en français de ce paragraphe spécifique",
                      "spanish": "Traducción al español de este párrafo específico"
                    }
                  ]
                }
            """.trimIndent()

            val userContent = """
                Obra: "$bookTitle"
                Página: $pageNumber

                Texto Original da Página:
                $pageText
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.3)
                put("response_format", JSONObject().put("type", "json_object"))
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                }
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(bodyStr).optJSONObject("error")?.optString("message") ?: "Erro HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Erro HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception("OpenAI API: $errMsg"))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                val json = JSONObject(content)

                val portuguese = json.optString("portuguese", "").trim()
                val english = json.optString("english", "").trim()
                val french = json.optString("french", "").trim()
                val spanish = json.optString("spanish", "").trim()
                val paragraphsArray = json.optJSONArray("paragraphs")

                val comparativeParagraphsJson = if (paragraphsArray != null && paragraphsArray.length() > 0) {
                    paragraphsArray.toString()
                } else {
                    ""
                }

                val pageTranslation = com.example.data.model.PageMultiTranslation(
                    bookId = 0,
                    pageNumber = pageNumber,
                    originalText = pageText,
                    englishText = english,
                    frenchText = french,
                    spanishText = spanish,
                    portugueseText = portuguese,
                    comparativeParagraphsJson = comparativeParagraphsJson,
                    translatedAt = System.currentTimeMillis()
                )

                Result.success(pageTranslation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in translatePageToTrilateralLanguages: ${e.message}", e)
            Result.failure(Exception("Falha na tradução OpenAI: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Reestrutura páginas com fórmulas matemáticas, reações químicas, citologia, símbolos biológicos,
     * física, diagramas e raciocínio lógico, eliminando distorções de OCR e gerando texto fonético ideal para TTS.
     * Suporta análise multimodal (GPT-4o Vision) enviando o bitmap da página para decodificar gráficos e fórmulas visuais.
     */
    suspend fun reconstructScientificPage(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookId: Int,
        bookTitle: String,
        pageNumber: Int,
        pageText: String,
        pageBitmap: Bitmap? = null
    ): Result<ScientificPageReconstruction> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (pageText.isBlank() && pageBitmap == null) {
            return@withContext Result.failure(Exception("Conteúdo e imagem da página estão vazios para análise científica."))
        }

        try {
            val systemPrompt = """
                Você é um Cientista Polímata e Mestre em Didática das Ciências Exatas e da Natureza (Matemática, Física, Química Geral e Orgânica, Citologia, Biologia Celular e Molecular, Genética e Raciocínio Lógico-Matemático).

                PROBLEMA A RESOLVER:
                Muitos livros científicos possuem fórmulas complexas, reações químicas, organelas celulares, mapas metabólicos, frações empilhadas, matrizes, tabelas e símbolos especiais que, ao serem extraídos de PDFs, ficam totalmente DESORDENADOS, QUEBRADOS e "BUGADOS" (ex: subscritos viram números soltos, setas de reação somem, frações se partem em linhas aleatórias, legendas de imagens se misturam com o texto e o áudio TTS não consegue ler).
                Quando uma imagem/bitmap da página for fornecida, analise visualmente com máxima precisão os gráficos, equações, reações orgânicas, cálculos e figuras presentes na foto da página!

                DIRETRIZ MÁXIMA - SEM DEIXAR FALTAR NADA:
                Restaure absolutamente TODAS as equações, cálculos, reações químicas, estruturas de biologia, proposições lógicas, fórmulas, deduções passo a passo, notas e legendas contidas na página original, sem omitir ou resumir nada:
                1. BIOLOGIA: Vias metabólicas (glicólise, ciclo de Krebs, cadeia respiratória, fotossíntese), organelas, ácidos nucleicos (DNA, RNA, códons), síntese proteica, genética mendeliana e molecular.
                2. QUÍMICA: Reações químicas balanceadas com coeficientes estequiométricos, setas de reação (→, ⇌, ⇄), estados físicos ((s), (l), (g), (aq)), índices atômicos subscritos nítidos (H₂O, CO₂, H₂SO₄, C₆H₁₂O₆), cargas iônicas sobrescritas (Fe³⁺, Ca²⁺, SO₄²⁻), equilíbrios químicos e entalpia (ΔH).
                3. FÍSICA: Fórmulas da mecânica, termodinâmica, eletromagnetismo, óptica, ondas, gravitação e física quântica/moderna. Grandezas vetoriais, relações de proporcionalidade e todas as unidades do Sistema Internacional (SI) como N, J, W, Pa, Ω, T, V, Hz, kg·m/s².
                4. CÁLCULO & MATEMÁTICA: Integrais simples, duplas e triplas (∫, ∬, ∮), limites (lim), derivadas totais e parciais (df/dx, ∂), somatórios (∑), produtórios (∏), frações empilhadas, raízes quadradas e enésimas (√, ∛), expoentes (x², e⁻ᵗ), matrizes, determinantes e equações polinomiais.
                5. RACIOCÍNIO LÓGICO & MATEMÁTICA DISCRETA: Conectivos proposicionais (conjunção ∧, disjunção ∨, negação ¬, condicional →, bicondicional ↔, disjunção exclusiva ⊻), quantificadores (∀, ∃), teoremas, regras de inferência (Modus Ponens, Modus Tollens), símbolos de dedução (∴, ∵, ⊢, ⊨), teoria dos conjuntos (∈, ∉, ⊂, ⊆, ∪, ∩, ∅, ℝ, ℕ, ℤ, ℚ) e tabelas-verdade.
                6. OPERAÇÕES DE QUALQUER ÁREA DE EXATAS: Trigonometria, geometria analítica, estatística e probabilidade.

                SUA MISSÃO:
                Reestruturar com máxima clareza, rigor científico e didática impecável o conteúdo desta página para renderização em formato de livro didático de ensino real dos alunos:
                1. Explicação teórica clara, profunda e didática em Português (estilo livro didático físico).
                2. Fórmulas completas com notação Unicode limpa, notação LaTeX rigorosa, análise dimensional (SI) e demonstração/resolução passo a passo.
                3. Esquemas visuais, organelas ou gráficos decifrados.
                4. Tabela de símbolos científicos e constantes.
                5. Flashcards de memorização ativa (Active Recall) para fixação das fórmulas e conceitos-chave.
                6. Texto adaptado para leitura em voz alta neural (TTS).

                CAMPOS OBRIGATÓRIOS DO JSON:
                1. "subjectArea": Área da ciência (ex: "Química Geral & Orgânica", "Citologia & Bioenergética", "Cálculo Diferencial & Integral", "Física Mecânica / Termodinâmica", "Genética Molecular", "Raciocínio Lógico Proposicional").
                2. "pageTitle": Título didático do tema abordado na página.
                3. "structuredExplanation": Conteúdo teórico integral da página reescrito de forma límpida, sem quebras aleatórias, em parágrafos coerentes, ricos e didáticos em Português.
                4. "formulas": Array com todas as fórmulas e equações presentes ou implícitas na página:
                   - "id": "eq_1", "eq_2"...
                   - "category": ex: "Química", "Matemática", "Física", "Citologia/Bioquímica", "Lógica"
                   - "formulaNotation": Formatação tipográfica idêntica à diagramação de livros didáticos físicos impressos (use subscritos Unicode perfeitos como H₂O, CO₂, C₆H₁₂O₆, v₀, aₙ; expoentes/sobrescritos reais como x², x³, e⁻, Fe³⁺; operadores matemáticos de livros como · (multiplicação), ÷, ±, √, ∛, ∫, ∑, ∏, lim, ∞, π, Δ; NUNCA use sintaxe de computador como asterisco (*), circunflexo (^), sqrt() ou código de programação. As equações devem ser apresentadas com a mesma elegância visual de páginas de livros escolares de física, química e cálculo).
                   - "latexNotation": Notação formal em LaTeX / KaTeX (ex: "\int_0^\infty \frac{\sin x}{x} dx = \frac{\pi}{2}" ou "\Delta G^\circ = -RT \ln K_{eq}").
                   - "dimensionalAnalysis": Análise dimensional e unidades no SI (ex: "[J] = kg·m²·s⁻²" ou "Constante R em J/(mol·K)").
                   - "stepByStepSolution": Array de strings demonstrando a dedução matemática passo a passo, etapas do balanceamento químico ou mecanismo da reação, sempre na notação clássica de livro físico didático.
                   - "explanation": Explicação clara do que a fórmula descreve e o significado de cada variável.
                   - "spokenTtsGuide": Como a fórmula deve ser lida e narrada em voz alta em português de forma perfeitamente natural (ex: "Água reage com dióxido de carbono formando ácido carbônico").
                5. "diagrams": Array de figuras, ilustrações, organelas, gráficos ou esquemas da página:
                   - "id": "diag_1"...
                   - "labelOrFigure": ex: "Figura 1", "Esquema da Mitocôndria", "Diagrama de Venn", "Gráfico Cartesiano"
                   - "title": Nome do diagrama
                   - "description": O que a imagem/diagrama demonstra passo a passo
                   - "keyComponents": Array de elementos identificados (ex: ["Cristas mitocondriais", "Matriz", "Membrana externa"])
                6. "symbols": Array de símbolos técnicos e constantes encontrados (ex: Δ, λ, pH, ATP, NADH, ⇌, ∀, ħ):
                   - "symbol": o símbolo
                   - "name": nome por extenso
                   - "contextAndUnit": contexto de uso e unidade de medida.
                7. "flashcards": Array de cartões de estudo ativo (Active Recall) para fixação rápida da matéria da página:
                   - "id": "card_1", "card_2"...
                   - "question": Pergunta conceitual direta sobre a fórmula, princípio ou lei científica da página.
                   - "answer": Resposta detalhada e didática explicando o porquê e as condições de validade.
                   - "formulaOrRule": A fórmula ou regra científica principal do cartão.
                   - "hint": Dica mnemônica ou intuição para recordar a resposta.
                8. "ttsOptimizedText": Texto integral em português contínuo, adaptado para leitura em voz alta neural (TTS), onde cada fórmula e símbolo é articulado de forma clara e fluida, sem siglas confusas ou códigos quebrados.

                FORMATO DE RETORNO OBRIGATÓRIO (JSON PURO):
                Responda APENAS com um objeto JSON válido, sem texto antes ou depois.
            """.trimIndent()

            val textPrompt = """
                Livro: "$bookTitle"
                Página: $pageNumber

                Conteúdo Bruto Extraído da Página:
                ${pageText.ifBlank { "(Página baseada na imagem visual fornecida)" }}
            """.trimIndent()

            val targetModel = if (pageBitmap != null && model.contains("3.5")) "gpt-4o-mini" else model.ifBlank { "gpt-4o-mini" }

            val userMessageObj = JSONObject().apply {
                put("role", "user")
                if (pageBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    val maxDim = 1280
                    val scaledBitmap = if (pageBitmap.width > maxDim || pageBitmap.height > maxDim) {
                        val factor = maxDim.toFloat() / maxOf(pageBitmap.width, pageBitmap.height)
                        Bitmap.createScaledBitmap(pageBitmap, (pageBitmap.width * factor).toInt(), (pageBitmap.height * factor).toInt(), true)
                    } else {
                        pageBitmap
                    }
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val base64Img = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$base64Img"

                    val contentArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", textPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", dataUrl)
                                put("detail", "high")
                            })
                        })
                    }
                    put("content", contentArr)
                } else {
                    put("content", textPrompt)
                }
            }

            val jsonBody = JSONObject().apply {
                put("model", targetModel)
                put("temperature", 0.2)
                put("response_format", JSONObject().put("type", "json_object"))
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(userMessageObj)
                }
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(bodyStr).optJSONObject("error")?.optString("message") ?: "Erro HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Erro HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception("OpenAI API: $errMsg"))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                val json = JSONObject(content)

                val subjectArea = json.optString("subjectArea", "Ciências Exatas & Biológicas").trim()
                val pageTitle = json.optString("pageTitle", "Página $pageNumber").trim()
                val structuredExplanation = json.optString("structuredExplanation", pageText).trim()
                val formulasArr = json.optJSONArray("formulas")
                val diagramsArr = json.optJSONArray("diagrams")
                val symbolsArr = json.optJSONArray("symbols")
                val flashcardsArr = json.optJSONArray("flashcards")
                val ttsOptimizedText = json.optString("ttsOptimizedText", structuredExplanation).trim()

                val reconstruction = ScientificPageReconstruction(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    subjectArea = subjectArea,
                    pageTitle = pageTitle,
                    structuredExplanation = structuredExplanation,
                    formulasJson = formulasArr?.toString() ?: "[]",
                    diagramsJson = diagramsArr?.toString() ?: "[]",
                    symbolsJson = symbolsArr?.toString() ?: "[]",
                    flashcardsJson = flashcardsArr?.toString() ?: "[]",
                    ttsOptimizedText = ttsOptimizedText,
                    createdAt = System.currentTimeMillis()
                )

                Result.success(reconstruction)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in reconstructScientificPage: ${e.message}", e)
            Result.failure(Exception("Falha na reestruturação científica: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Gera o texto integral contínuo da página a partir dos dados científicos reestruturados,
     * integrando perfeitamente a teoria, equações, reações químicas, cálculos e lógica em formato límpido.
     */
    fun buildContinuousRestoredPageText(reconstruction: ScientificPageReconstruction): String {
        val sb = StringBuilder()
        if (reconstruction.pageTitle.isNotBlank()) {
            sb.append(reconstruction.pageTitle.trim()).append("\n\n")
        }
        if (reconstruction.structuredExplanation.isNotBlank()) {
            sb.append(reconstruction.structuredExplanation.trim())
        }
        val formulas = reconstruction.getFormulas()
        if (formulas.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append("📐 EQUAÇÕES & FÓRMULAS:\n")
            formulas.forEachIndexed { idx, f ->
                val cat = if (f.category.isNotBlank()) "[${f.category}] " else ""
                sb.append("${idx + 1}. $cat${f.formulaNotation}\n")
                if (f.explanation.isNotBlank()) {
                    sb.append("   • ${f.explanation}\n")
                }
                if (f.dimensionalAnalysis.isNotBlank()) {
                    sb.append("   • Unidades (SI): ${f.dimensionalAnalysis}\n")
                }
                if (f.stepByStepSolution.isNotEmpty()) {
                    sb.append("   • Resolução: ${f.stepByStepSolution.joinToString(" ➔ ")}\n")
                }
            }
        }
        val diagrams = reconstruction.getDiagrams()
        if (diagrams.isNotEmpty()) {
            sb.append("\n\n🔬 DIAGRAMAS & ESQUEMAS:\n")
            diagrams.forEach { d ->
                sb.append("• ${d.labelOrFigure}: ${d.title}\n")
                if (d.description.isNotBlank()) {
                    sb.append("  ${d.description}\n")
                }
                if (d.keyComponents.isNotEmpty()) {
                    sb.append("  Componentes: ${d.keyComponents.joinToString(", ")}\n")
                }
            }
        }
        val symbols = reconstruction.getSymbols()
        if (symbols.isNotEmpty()) {
            sb.append("\n\n🔣 SÍMBOLOS & CONSTANTES:\n")
            symbols.forEach { s ->
                sb.append("• ${s.symbol}: ${s.name} (${s.contextAndUnit})\n")
            }
        }
        return sb.toString().trim()
    }

    suspend fun generateBookQuestions(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookId: Int,
        bookTitle: String,
        startPage: Int,
        endPage: Int,
        contentSnippet: String,
        institution: String = "FGV",
        difficulty: String = "MEDIUM",
        questionCount: Int = 5
    ): Result<List<com.example.data.model.BookQuestion>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (contentSnippet.isBlank()) {
            return@withContext Result.failure(Exception("Texto das páginas está vazio para criação de questões."))
        }

        try {
            val systemPrompt = """
                Você é o mais prestigiado elaborador e especialista de bancas examinadoras de concursos públicos de elite do Brasil, com domínio cirúrgico dos estilos:
                1. FGV (Fundação Getulio Vargas) - O ápice da dificuldade no Brasil: interpretação profunda de Língua Portuguesa, semântica contextual refinada, operadores argumentativos, pressupostos vs subentendidos, ambiguidade, polissemia, reescritura com nuance milimétrica e distratores ardilosos de altíssima plausibilidade.
                2. CEBRASPE (CESPE) - Julgamento de itens Certo ou Errado e assertivas categóricas, textos-base densos, cobrança de extrapolação textual, limite inferencial e rigor gramatical.
                3. FCC (Fundação Carlos Chagas) - Clássico padrão com análise de assertivas numeradas (I, II, III, IV) para julgar quais estão corretas/incorretas, paralelismo sintático-semântico e correlação lógica entre orações.
                4. VUNESP - Casos práticos detalhados, interpretação textual contextualizada e sólida cobrança gramatical normativa.
                5. CESGRANRIO - Estilo Concurso Nacional Unificado (CNU) e grandes estatais (Caixa, BB, Petrobras): interpretação crítica, contextualização aplicada e dilemas sociotécnicos.
                6. QUADRIX & INSTITUTO AOCP - Análise minuciosa de itens afirmativos, regência/crase/concordância aplicadas ao texto e julgamento de assertivas (I, II, III).
                7. IADES / EADES - Progressão temática e coesão textual.
                8. MISTA (Simulado Multibancas) - Distribuição equilibrada entre questões padrão FGV, CEBRASPE, FCC e VUNESP/CESGRANRIO.

                DIRETRIZES DE ALTO NÍVEL:
                1. INTERPRETAÇÃO E LÍNGUA PORTUGUESA EXTREMAMENTE RICAS:
                   - Não crie perguntas óbvias ou decorebas. As questões devem dissecar a fundo o texto da página selecionada.
                   - Inclua sempre um trecho significativo de apoio no "contextSnippet" ou no próprio enunciado para que o candidato possa fazer a interpretação textual contextualizada.
                2. VARIAÇÃO DE TIPOS DE QUESTÕES:
                   - MULTIPLE_CHOICE: 5 alternativas (A, B, C, D, E) com distratores refinados (estilo FGV/VUNESP/CESGRANRIO).
                   - ASSERTIVAS: Enunciado contendo itens numerados (ex: "Considere as seguintes assertivas sobre o texto: I. ... II. ... III. ... Está correto o que se afirma em: A) Apenas I; B) Apenas II; C) I e III; D) II e III; E) I, II e III") - estilo clássico FCC/FGV.
                   - TRUE_FALSE: Itens de asserção categórica para julgar em Certo ou Errado (estilo CEBRASPE).
                3. NÍVEIS DE DIFICULDADE:
                   - EASY: Compreensão direta do texto.
                   - MEDIUM: Interpretação inferencial e relações de causa/efeito.
                   - HARD: Relações discursivas complexas, pressuposições sutis e intenção do autor.
                   - INSANE: Nível extremo padrão FGV (Auditor-Fiscal, Diplomacia, Juiz) — distratores dificílimos, pegadinhas semânticas elegantes e análise textual cirúrgica.
                4. GABARITO COMENTADO E EXPLICAÇÃO DETALHADA (OBRIGATÓRIO):
                   - No campo "explanation", forneça uma justificativa pedagógica COMPLETA e aprofundada:
                     * Explique por que a alternativa correta é a única aceita com base na teoria e no texto.
                     * Comente os erros de cada um dos distratores/assertivas incorretas, desarmando as pegadinhas.
                   - No campo "supportingQuote", transcreva a CITAÇÃO DIRETA, LITERAL E EXATA da página que comprova o gabarito.

                5. REGRAS CRÍTICAS DE ÁREA DO CONHECIMENTO E ESPAÇAMENTOS (OBRIGATÓRIO):
                   - No campo "institution": coloque ESTRITAMENTE a sigla da banca examinadora solicitada (ex: "FGV", "CEBRASPE", "FCC", "VUNESP", "CESGRANRIO", "QUADRIX", "AOCP", "IADES"). NUNCA coloque nomes de áreas de conhecimento, disciplinas ou matérias (NUNCA coloque "Direito Constitucional", "Física", "Área de Conhecimento", etc.).
                   - No campo "statement": vá DIRETO ao enunciado formal da questão. NUNCA inicie o enunciado com "Área de Conhecimento:", "Disciplina:", "Matéria:", "Conteúdo:".
                   - Nas "options": NUNCA inclua espaços longos, tabulações ou quebras de linha anormais. As alternativas devem ter espaçamento uniforme e rigorosamente justificado.
                   - Não duplique a letra da alternativa dentro do texto (ex: envie apenas o texto da assertiva, sem "A) ", "B) ").

                FORMATO DE RESPOSTA OBRIGATÓRIO (JSON PURO):
                Responda APENAS com um objeto JSON válido contendo a chave "questions":
                {
                  "questions": [
                    {
                      "institution": "FGV ou CEBRASPE ou FCC ou VUNESP ou CESGRANRIO ou QUADRIX ou AOCP ou IADES",
                      "difficulty": "EASY ou MEDIUM ou HARD ou INSANE",
                      "questionType": "MULTIPLE_CHOICE ou ASSERTIVAS ou TRUE_FALSE",
                      "statement": "Enunciado completo e contextualizado da questão...",
                      "contextSnippet": "Trecho de apoio ou parágrafo do livro analisado...",
                      "options": [
                        "Primeira opção...",
                        "Segunda opção...",
                        "Terceira opção...",
                        "Quarta opção...",
                        "Quinta opção..."
                      ],
                      "correctOptionIndex": 0,
                      "explanation": "Gabarito comentado detalhado, analisando a correta e desarmando as incorretas...",
                      "supportingQuote": "Citação literal do texto original que comprova a resposta...",
                      "targetParagraph": "Parágrafo ou oração explorada"
                    }
                  ]
                }
                * Nota: Para TRUE_FALSE (CEBRASPE), "options" deve ser exatamente ["Certo", "Errado"], com "correctOptionIndex" sendo 0 ou 1.
                * Nota: Para ASSERTIVAS, o enunciado deve conter as assertivas I, II, III e as opções devem ser ["Apenas I...", "Apenas II...", "I e III...", "II e III...", "I, II e III..."].
            """.trimIndent()

            val targetCount = questionCount.coerceIn(2, 20)
            val userContent = """
                Livro: "$bookTitle"
                Páginas selecionadas: ${if (startPage == endPage) "Página $startPage" else "Páginas $startPage até $endPage"}
                Banca desejada: $institution
                Dificuldade desejada: $difficulty
                Quantidade de questões a gerar: $targetCount

                TEXTO COMPLETO DAS PÁGINAS SELECIONADAS:
                $contentSnippet
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.4)
                put("response_format", JSONObject().put("type", "json_object"))
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                }
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Erro desconhecido"
                    return@withContext Result.failure(Exception("Erro na API da OpenAI (${response.code}): $errorBody"))
                }

                val bodyStr = response.body?.string()
                if (bodyStr.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val jsonResponse = JSONObject(bodyStr)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Nenhuma escolha retornada pela OpenAI."))
                }

                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                val rootJson = JSONObject(content)
                val questionsArray = rootJson.optJSONArray("questions")
                    ?: return@withContext Result.failure(Exception("JSON retornado não contém o array 'questions'."))

                val resultList = mutableListOf<com.example.data.model.BookQuestion>()
                for (i in 0 until questionsArray.length()) {
                    val qJson = questionsArray.getJSONObject(i)
                    val rawInst = qJson.optString("institution", institution).ifBlank { institution }
                    val inst = com.example.util.TextFormatUtils.cleanInstitution(rawInst, institution)
                    val diff = qJson.optString("difficulty", difficulty).ifBlank { difficulty }
                    val qType = qJson.optString("questionType", "MULTIPLE_CHOICE")
                    val rawStatement = qJson.optString("statement", "").trim()
                    val statement = com.example.util.TextFormatUtils.cleanStatementText(rawStatement)
                    val rawSnippet = qJson.optString("contextSnippet", "").trim()
                    val contextSnippet = com.example.util.TextFormatUtils.cleanContextSnippet(rawSnippet)

                    val optArray = qJson.optJSONArray("options")
                    val optionsList = mutableListOf<String>()
                    if (optArray != null) {
                        for (j in 0 until optArray.length()) {
                            val cleanOpt = com.example.util.TextFormatUtils.cleanOptionText(optArray.getString(j))
                            if (cleanOpt.isNotBlank()) optionsList.add(cleanOpt)
                        }
                    }
                    if (optionsList.isEmpty()) {
                        if (qType == "TRUE_FALSE") {
                            optionsList.addAll(listOf("Certo", "Errado"))
                        } else {
                            optionsList.addAll(listOf("Alternativa A", "Alternativa B", "Alternativa C", "Alternativa D", "Alternativa E"))
                        }
                    }

                    val correctIdx = qJson.optInt("correctOptionIndex", 0).coerceIn(0, optionsList.size - 1)
                    val explanation = com.example.util.TextFormatUtils.cleanTextSpacing(qJson.optString("explanation", "Sem fundamentação disponível."))
                    val supportingQuote = com.example.util.TextFormatUtils.cleanTextSpacing(qJson.optString("supportingQuote", ""))
                    val targetParagraph = com.example.util.TextFormatUtils.cleanTextSpacing(qJson.optString("targetParagraph", ""))

                    val optJsonArray = JSONArray()
                    optionsList.forEach { optJsonArray.put(it) }

                    if (statement.isNotBlank()) {
                        resultList.add(
                            com.example.data.model.BookQuestion(
                                id = 0,
                                bookId = bookId,
                                startPage = startPage,
                                endPage = endPage,
                                institution = inst,
                                difficulty = diff,
                                questionType = qType,
                                statement = statement,
                                contextSnippet = contextSnippet,
                                optionsJson = optJsonArray.toString(),
                                correctOptionIndex = correctIdx,
                                explanation = explanation,
                                supportingQuote = supportingQuote,
                                targetParagraph = targetParagraph,
                                userAnswerIndex = null,
                                isAnswered = false,
                                isCorrect = null,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                if (resultList.isEmpty()) {
                    Result.failure(Exception("Nenhuma questão válida pôde ser extraída."))
                } else {
                    Result.success(resultList)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateBookQuestions: ${e.message}", e)
            Result.failure(Exception("Falha ao gerar questões via OpenAI: ${e.localizedMessage ?: e.message}"))
        }
    }

    // -------------------------------------------------------------------------
    // BOOKS: Análise Exaustiva de Temas da Página e Termos Entre Vírgulas
    // Recomendações de Livros Reais via OpenAI com Fallback Offline Robusto
    // -------------------------------------------------------------------------
    suspend fun generateBookRecommendations(
        context: Context,
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookId: Int,
        bookTitle: String,
        pageNumber: Int,
        pageText: String
    ): Result<List<com.example.data.model.BookPageRecommendation>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val trimmedText = pageText.trim()

        if (trimmedText.isBlank()) {
            return@withContext Result.failure(Exception("O texto da página está vazio para recomendação de livros."))
        }

        // Se a chave não estiver configurada, usa o analisador bibliográfico estruturado offline
        if (cleanKey.isBlank()) {
            Log.d(TAG, "OpenAI API Key vazia. Usando analisador bibliográfico inteligente offline.")
            val offlineList = generateOfflineBookRecommendations(bookId, pageNumber, bookTitle, trimmedText)
            return@withContext Result.success(offlineList)
        }

        try {
            val systemPrompt = """
                Você é um bibliógrafo de excelência internacional, consultor literário e enciclopedista acadêmico.
                Sua missão é realizar uma curadoria bibliográfica exaustiva de LIVROS REAIS E CONSAGRADOS com base estrita no texto da página fornecida.

                DIRETRIZES OBRIGATÓRIAS DE ANÁLISE (DOIS PILARES COMPULSÓRIOS):

                1. PILAR 1 - ANÁLISE DOS ASSUNTOS DA PÁGINA (PAGE_THEME):
                   - Identifique TODOS os assuntos, temas centrais, conceitos essenciais, argumentos, disciplinas e teorias desenvolvidas no texto desta página.
                   - Para CADA assunto identificado, indique um LIVRO REAL, clássico ou de referência indispensável na literatura mundial para quem deseja aprofundar exatamente aquele tema.
                   - ESGOTE TODOS OS ASSUNTOS da página; nada deve ficar sem uma recomendação.

                2. PILAR 2 - ANÁLISE DE TERMOS E TEMAS ENTRE VÍRGULAS (COMMA_SEPARATED):
                   - Inspecione minuciosamente a página procurando enumerações, listas de palavras-chave, conceitos, termos técnicos ou sequências separadas por VÍRGULAS (ex: "conceito A, conceito B, conceito C", listas de tópicos, princípios ou matérias).
                   - Extraia CADA TERMO OU TEMA individual que esteja delimitado por vírgulas.
                   - Para CADA UM dos temas entre vírgulas, traga obrigatoriamente um LIVRO REAL específico de referência bibliográfica.
                   - ESGOTE TODOS os termos entre vírgulas encontrados. Não deixe nenhum termo ou tema sem indicação de livro real de referência.

                REGRAS DE OURO:
                - LIVROS REAIS: Todos os livros indicados devem ser obras reais, existentes, publicadas e renomadas (com autor real e título verdadeiro).
                - QUALQUER IDIOMA: Funciona para textos em qualquer idioma (português, inglês, espanhol, francês, alemão, italiano, latim, etc.). As obras recomendadas podem ser no idioma original ou edições clássicas traduzidas.
                - NÃO DEIXE SOBRAR NADA: Faça uma varredura completa. Se houver múltiplos termos entre vírgulas, traga recomendações para TODOS eles sem exceção.

                FORMATO DE RESPOSTA OBRIGATÓRIO (JSON PURO):
                Responda APENAS com um objeto JSON válido contendo a lista "recommendations":
                {
                  "recommendations": [
                    {
                      "themeOrTopic": "Assunto da página ou termo exato entre vírgulas",
                      "recommendedBookTitle": "Título Real do Livro Consagrado",
                      "author": "Nome do Autor da Obra",
                      "relevanceDescription": "Justificativa clara de por que este livro é a leitura indispensável sobre este assunto/termo.",
                      "analysisType": "PAGE_THEME ou COMMA_SEPARATED",
                      "yearOrEdition": "Ano aproximado ou referência clássica"
                    }
                  ]
                }
            """.trimIndent()

            val userContent = """
                Livro Original: "$bookTitle"
                Página Atual: $pageNumber

                CONTEÚDO TEXTUAL DA PÁGINA PARA ANÁLISE E ESGOTAMENTO DE TEMAS:
                $trimmedText
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.3)
                put("response_format", JSONObject().put("type", "json_object"))
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                }
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .header("Authorization", "Bearer $cleanKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.w(TAG, "OpenAI error code: ${response.code}, message: $errBody. Falling back to offline curator.")
                val offlineList = generateOfflineBookRecommendations(bookId, pageNumber, bookTitle, trimmedText)
                return@withContext Result.success(offlineList)
            }

            val respBody = response.body?.string()
            if (respBody.isNullOrBlank()) {
                val offlineList = generateOfflineBookRecommendations(bookId, pageNumber, bookTitle, trimmedText)
                return@withContext Result.success(offlineList)
            }

            val rootJson = JSONObject(respBody)
            val choices = rootJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                val offlineList = generateOfflineBookRecommendations(bookId, pageNumber, bookTitle, trimmedText)
                return@withContext Result.success(offlineList)
            }

            val messageObj = choices.getJSONObject(0).getJSONObject("message")
            val contentString = messageObj.getString("content")

            val parsedRecommendations = mutableListOf<com.example.data.model.BookPageRecommendation>()
            val parsedJson = JSONObject(contentString)
            val recArray = parsedJson.optJSONArray("recommendations") ?: JSONArray()

            for (i in 0 until recArray.length()) {
                val item = recArray.getJSONObject(i)
                val theme = item.optString("themeOrTopic", "").trim()
                val title = item.optString("recommendedBookTitle", "").trim()
                val author = item.optString("author", "").trim()
                val desc = item.optString("relevanceDescription", "").trim()
                val type = item.optString("analysisType", "PAGE_THEME").trim()
                val year = item.optString("yearOrEdition", "").trim()

                if (theme.isNotBlank() && title.isNotBlank()) {
                    parsedRecommendations.add(
                        com.example.data.model.BookPageRecommendation(
                            bookId = bookId,
                            pageNumber = pageNumber,
                            themeOrTopic = theme,
                            recommendedBookTitle = title,
                            author = author.ifBlank { "Referência Canônica" },
                            relevanceDescription = desc.ifBlank { "Leitura essencial para aprofundamento do tema." },
                            analysisType = if (type.contains("COMMA", ignoreCase = true)) "COMMA_SEPARATED" else "PAGE_THEME",
                            yearOrEdition = year,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            if (parsedRecommendations.isEmpty()) {
                val offlineList = generateOfflineBookRecommendations(bookId, pageNumber, bookTitle, trimmedText)
                Result.success(offlineList)
            } else {
                Result.success(parsedRecommendations)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in generateBookRecommendations: ${e.message}. Using offline bibliography.", e)
            val offlineList = generateOfflineBookRecommendations(bookId, pageNumber, bookTitle, trimmedText)
            Result.success(offlineList)
        }
    }

    /**
     * Motor Bibliográfico Inteligente Offline:
     * Analisa o texto da página, extrai temas gerais e termos separados por vírgula,
     * cruzando com um catálogo enciclopédico de livros reais para não deixar nenhum tema sem cobertura.
     */
    private fun generateOfflineBookRecommendations(
        bookId: Int,
        pageNumber: Int,
        bookTitle: String,
        pageText: String
    ): List<com.example.data.model.BookPageRecommendation> {
        val list = mutableListOf<com.example.data.model.BookPageRecommendation>()
        val seenThemes = mutableSetOf<String>()

        // 1. Extração exaustiva de termos entre vírgulas
        val lines = pageText.split("\n", ".", ";", ":")
        for (line in lines) {
            if (line.contains(",")) {
                val rawCommaTerms = line.split(",")
                if (rawCommaTerms.size >= 2) {
                    for (rawTerm in rawCommaTerms) {
                        val clean = rawTerm
                            .replace(Regex("""^[\d\s\-\•\*\(\)\[\]"'\„\“]+"""), "")
                            .replace(Regex("""[\.\!\?\)\]"'\„\“]+$"""), "")
                            .trim()

                        // Filtrar preposições avulsas e termos muito curtos ou longos
                        if (clean.length in 3..45 && !clean.matches(Regex("""^(e|ou|de|do|da|dos|das|em|no|na|nos|nas|com|por|para|a|o|os|as|and|or|of|in|to|the)$""", RegexOption.IGNORE_CASE))) {
                            val lower = clean.lowercase()
                            if (seenThemes.add(lower)) {
                                val match = findRealBookForTheme(clean)
                                list.add(
                                    com.example.data.model.BookPageRecommendation(
                                        bookId = bookId,
                                        pageNumber = pageNumber,
                                        themeOrTopic = clean,
                                        recommendedBookTitle = match.first,
                                        author = match.second,
                                        relevanceDescription = match.third,
                                        analysisType = "COMMA_SEPARATED",
                                        yearOrEdition = match.fourth,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Extração de assuntos gerais da página (Pilar 1)
        val words = pageText.split(Regex("""\s+"""))
        val candidateTopics = mutableListOf<String>()

        // Detecta possíveis grandes temas a partir do texto
        val thematicKeywords = listOf(
            "direito", "justiça", "lei", "constituição", "moral", "ética", "filosofia", "política", "estado",
            "economia", "mercado", "capital", "trabalho", "história", "sociedade", "cultura", "linguagem",
            "educação", "ciência", "tecnologia", "psicologia", "mente", "comportamento", "física", "biologia",
            "lógica", "matemática", "literatura", "arte", "administração", "estratégia", "comunicação", "religião"
        )

        for (kw in thematicKeywords) {
            if (pageText.contains(kw, ignoreCase = true) && seenThemes.add(kw)) {
                candidateTopics.add(kw.replaceFirstChar { it.uppercase() })
            }
        }

        // Se encontrou poucos tópicos, extrai frases-chave como temas da página
        if (candidateTopics.isEmpty()) {
            val firstSentence = pageText.substringBefore(".").take(60).trim()
            if (firstSentence.isNotBlank()) {
                candidateTopics.add(firstSentence)
            }
        }

        for (topic in candidateTopics) {
            val match = findRealBookForTheme(topic)
            list.add(
                com.example.data.model.BookPageRecommendation(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    themeOrTopic = topic,
                    recommendedBookTitle = match.first,
                    author = match.second,
                    relevanceDescription = match.third,
                    analysisType = "PAGE_THEME",
                    yearOrEdition = match.fourth,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        // Garantir que haja sempre pelo menos recomendações robustas
        if (list.isEmpty()) {
            val defaultBook = findRealBookForTheme(bookTitle)
            list.add(
                com.example.data.model.BookPageRecommendation(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    themeOrTopic = "Análise Integral da Página",
                    recommendedBookTitle = defaultBook.first,
                    author = defaultBook.second,
                    relevanceDescription = defaultBook.third,
                    analysisType = "PAGE_THEME",
                    yearOrEdition = defaultBook.fourth,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        return list
    }

    /**
     * Catálogo bibliográfico de livros reais consagrados na literatura mundial
     * Retorna Quadruple: (Título Real, Autor, Justificativa de Relevância, Ano/Edição)
     */
    /**
     * Gera automaticamente uma biografia profissional e fidedigna de um autor/escritor via OpenAI.
     */
    suspend fun generateAuthorBiography(
        authorName: String,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedName = authorName.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Nome do autor não pode ser vazio."))
        }

        if (apiKey.isBlank()) {
            Log.d(TAG, "OpenAI API Key vazia. Gerando biografia profissional offline.")
            return@withContext Result.success(generateOfflineAuthorBiography(trimmedName))
        }

        try {
            val systemPrompt = "Você é um biógrafo literário e enciclopedista sênior. " +
                    "Crie uma biografia profissional, fidedigna, envolvente e concisa sobre o autor ou pensador solicitado. " +
                    "O texto deve conter de 2 a 4 frases refinadas em português do Brasil, destacando sua relevância, estilo, movimento literário ou principais contribuições intelectuais. " +
                    "Retorne única e exclusivamente o parágrafo da biografia, sem aspas, sem títulos e sem saudações."

            val userPrompt = "Autor: $trimmedName\n\nElabore uma biografia profissional completa e concisa para a apresentação deste autor em uma coleção literária de prestígio."

            val requestJson = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.4)
                put("max_tokens", 350)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank()) {
                Log.w(TAG, "OpenAI biography call failed code: ${response.code}. Using offline bibliography.")
                return@withContext Result.success(generateOfflineAuthorBiography(trimmedName))
            }

            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.success(generateOfflineAuthorBiography(trimmedName))
            }

            val messageObj = choices.getJSONObject(0).optJSONObject("message")
            val bioContent = messageObj?.optString("content")?.trim() ?: ""

            if (bioContent.isNotBlank()) {
                Result.success(bioContent)
            } else {
                Result.success(generateOfflineAuthorBiography(trimmedName))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar biografia via OpenAI: ${e.message}. Usando biografia local.", e)
            Result.success(generateOfflineAuthorBiography(trimmedName))
        }
    }

    /**
     * Catálogo e gerador de biografias profissionais offline para autores.
     */
    private fun generateOfflineAuthorBiography(name: String): String {
        val n = name.lowercase().trim()
        return when {
            n.contains("machado de assis") ->
                "Um dos maiores nomes da literatura mundial e fundador da Academia Brasileira de Letras. Mestre do Realismo e da ironia refinada, suas obras como 'Dom Casmurro' e 'Memórias Póstumas de Brás Cubas' revolucionaram a narrativa psicológica com penetração analítica incomparável."
            n.contains("clarice lispector") ->
                "Escritora e jornalista singular da literatura brasileira, consagrada pelo lirismo existencial e pela prosa intimista de epifanias. Obras fundamentais como 'A Hora da Estrela' e 'Perto do Coração Selvagem' desvelam os abismos da consciência e da condição humana."
            n.contains("guimarães rosa") ->
                "Genial renovador da linguagem literária e expoente supremo do modernismo brasileiro. Sua obra-prima 'Grande Sertão: Veredas' funde a metafísica universal às vivências do sertão com invenção vocabular sem precedentes."
            n.contains("jorge amado") ->
                "Um dos escritores brasileiros mais traduzidos e reverenciados internacionalmente. Cronista da identidade cultural e da beleza do povo baiano, imortalizou personagens e costumes em romances icônicos como 'Gabriela, Cravo e Canela' e 'Capitães da Areia'."
            n.contains("fernando pessoa") ->
                "Poeta e pensador português fundamental da modernidade ocidental. Criador de múltiplos heterônimos como Álvaro de Campos e Ricardo Reis, explorou com gênio estético a multiplicidade do eu e a inquietude da existência."
            n.contains("shakespeare") || n.contains("william shakespeare") ->
                "Dramaturgo e poeta inglês consagrado como o maior escritor da língua inglesa e um dos dramaturgos mais influentes de todos os tempos. Suas tragédias e comédias exploram as paixões e dilemas humanos com universalidade atemporal."
            n.contains("aristóteles") ->
                "Filósofo grego da Antiguidade Clássica cujos escritos moldaram a lógica, a ética, a política, a poética e a ciência ocidental por milênios. Sua abordagem empírica e sistemática estabeleceu as bases do pensamento filosófico contemporâneo."
            n.contains("platão") ->
                "Filósofo ateniense do período clássico, discípulo de Sócrates e fundador da Academia de Atenas. Seus diálogos magistrais fundaram a teoria das ideias, a dialética e a filosofia política no Ocidente."
            n.contains("dante alighieri") ->
                "Poeta florentino supremo da Baixa Idade Média, apelidado de 'o Poeta Supremo'. Sua 'Divina Comédia' é o ápice da literatura italiana e uma das maiores realizações estéticas e espirituais da civilização."
            n.contains("kant") || n.contains("immanuel kant") ->
                "Filósofo prussiano central da era moderna que realizou a síntese crítica entre empirismo e racionalismo. Suas 'Críticas' redefiniram a epistemologia, a ética deontológica e o juízo estético universal."
            n.contains("nietzsche") || n.contains("friedrich nietzsche") ->
                "Filósofo, filólogo e ensaísta alemão cujas teses radicais sobre a vontade de potência, o eterno retorno e a crítica aos valores tradicionais exerceram influência profunda na cultura e na filosofia contemporânea."
            n.contains("graciliano ramos") ->
                "Proeminente romancista modernista brasileiro da geração de 30. Caracterizado pela concisão formal e denúncia contundente das contradições sociais em clássicos eternos como 'Vidas Secas' e 'São Bernardo'."
            n.contains("monteiro lobato") ->
                "Pioneiro do conto e da literatura infanto-juvenil no Brasil, além de ensaísta e nacionalista incansável. Criou o universo memorável do Sítio do Picapau Amarelo e analisou com vigor o desenvolvimento sociocultural do país."
            n.contains("carlos drummond de andrade") || n.contains("drummond") ->
                "Ícone da poesia moderna em língua portuguesa. Com olhar agudo e sensibilidade cósmica, transitou entre o lirismo pessoal, a reflexão social e o sentimento do mundo com maestria incomparável."
            else ->
                "Autor e pensador notável, destacando-se pelo rigor reflexivo e pela profundidade temática de sua produção. Sua trajetória literária reúne expressiva densidade conceitual, elegância estilística e valiosa contribuição ao enriquecimento do pensamento crítico e cultural."
        }
    }

    private fun findRealBookForTheme(theme: String): Quadruple<String, String, String, String> {
        val t = theme.lowercase()
        return when {
            t.contains("ética") || t.contains("moral") || t.contains("virtude") ->
                Quadruple("Ética a Nicômaco", "Aristóteles", "A obra fundamental da filosofia prática e da ética das virtudes no pensamento ocidental.", "Século IV a.C.")
            t.contains("direito") || t.contains("lei") || t.contains("norma") || t.contains("jurídic") ->
                Quadruple("Teoria Pura do Direito", "Hans Kelsen", "Tratado canônico que estabelece a ciência jurídica com autonomia dogmática e a teoria da norma.", "1934")
            t.contains("constitui") || t.contains("estado") || t.contains("soberania") ->
                Quadruple("Do Espírito das Leis", "Montesquieu", "A formulação clássica da separação dos poderes e a fundamentação do Estado Democrático de Direito.", "1748")
            t.contains("política") || t.contains("poder") || t.contains("governo") ->
                Quadruple("O Príncipe", "Nicolau Maquiavel", "O marco seminal do realismo político moderno, dissecando a conquista e manutenção do poder.", "1532")
            t.contains("economia") || t.contains("mercado") || t.contains("preço") || t.contains("moeda") ->
                Quadruple("A Riqueza das Nações", "Adam Smith", "Obra fundadora da ciência econômica moderna sobre a divisão do trabalho e os mercados.", "1776")
            t.contains("capital") || t.contains("trabalho") || t.contains("produção") ->
                Quadruple("O Capital (Crítica da Economia Política)", "Karl Marx", "Exame monumental das dinâmicas do modo de produção e da acumulação de capital.", "1867")
            t.contains("história") || t.contains("civiliza") || t.contains("passado") ->
                Quadruple("Sapiens: Uma Breve História da Humanidade", "Yuval Noah Harari", "Síntese brilhante da evolução biológica e cultural que moldou as sociedades humanas.", "2014")
            t.contains("mente") || t.contains("psicol") || t.contains("inconsciente") || t.contains("ego") ->
                Quadruple("A Interpretação dos Sonhos", "Sigmund Freud", "O texto inaugural da psicanálise que revolucionou o entendimento da mente e da subjetividade humana.", "1899")
            t.contains("comportamento") || t.contains("hábito") || t.contains("cogni") ->
                Quadruple("Rápido e Devagar: Duas Formas de Pensar", "Daniel Kahneman", "Referência máxima em economia comportamental e heurísticas de tomada de decisão.", "2011")
            t.contains("ciência") || t.contains("método") || t.contains("pesquisa") || t.contains("paradigma") ->
                Quadruple("A Estrutura das Revoluções Científicas", "Thomas Kuhn", "O clássico da epistemologia que definiu os conceitos de paradigma e quebra de modelo na ciência.", "1962")
            t.contains("tecnologia") || t.contains("software") || t.contains("computa") || t.contains("algoritmo") ->
                Quadruple("The Art of Computer Programming", "Donald E. Knuth", "A enciclopédia definitiva e reverenciada da ciência dos algoritmos e estruturas de dados.", "1968")
            t.contains("filosofia") || t.contains("metafísica") || t.contains("razão") ->
                Quadruple("Crítica da Razão Pura", "Immanuel Kant", "O ponto de inflexão da filosofia moderna investigando os limites do conhecimento humano.", "1781")
            t.contains("linguagem") || t.contains("língua") || t.contains("semiótica") || t.contains("signo") ->
                Quadruple("Curso de Linguística Geral", "Ferdinand de Saussure", "A obra base da linguística moderna e do estruturalismo.", "1916")
            t.contains("educa") || t.contains("pedagogia") || t.contains("ensino") ->
                Quadruple("Pedagogia do Oprimido", "Paulo Freire", "Referência mundial de teoria educacional crítica e conscientização libertadora.", "1968")
            t.contains("sociedade") || t.contains("sociol") || t.contains("comunidade") ->
                Quadruple("Economia e Sociedade", "Max Weber", "Monumento fundacional da sociologia compreensiva, dos tipos de dominação e da burocracia.", "1922")
            t.contains("lógica") || t.contains("argument") || t.contains("premissa") ->
                Quadruple("Tractatus Logico-Philosophicus", "Ludwig Wittgenstein", "Investigação rigorosa sobre a relação entre lógica, pensamento e a estrutura da linguagem.", "1921")
            t.contains("literatura") || t.contains("narrativa") || t.contains("ficção") ->
                Quadruple("Mimesis: A Representação da Realidade na Literatura Ocidental", "Erich Auerbach", "O clássico supremo da crítica e teoria literária comparada.", "1946")
            t.contains("estratégia") || t.contains("guerra") || t.contains("conflito") ->
                Quadruple("A Arte da Guerra", "Sun Tzu", "O mais célebre tratado milenar sobre tática, estratégia e antecipação de movimentos.", "Século V a.C.")
            t.contains("administra") || t.contains("gestão") || t.contains("liderança") ->
                Quadruple("O Gestor Eficaz", "Peter F. Drucker", "A obra essencial do pai da administração moderna sobre foco em resultados e prioridades.", "1966")
            t.contains("religião") || t.contains("teologia") || t.contains("sagrado") || t.contains("deus") ->
                Quadruple("Suma Teológica", "Tomás de Aquino", "A síntese máxima da teologia cristã e do pensamento escolástico medieval.", "1274")
            else ->
                Quadruple(
                    "Enciclopédia das Ciências e das Artes: $theme",
                    "Diderot & D'Alembert",
                    "Obra monumental iluminista que cataloga sistematicamente os fundamentos deste ramo do saber.",
                    "1751"
                )
        }
    }

    /**
     * Restauração de alta fidelidade via API da OpenAI.
     * Conserta estritamente pontuação, caracteres quebrados/OCR e quebras de linha com hífen,
     * e restaura com precisão absoluta páginas científicas (biologia, química, física, cálculos, equações e lógica),
     * mantendo 100% de integridade e literalidade ao texto original do autor, sem deixar faltar nada.
     */
    suspend fun restorePageTextPunctuationAndCharacters(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookTitle: String,
        pageNumber: Int,
        rawText: String,
        pageBitmap: Bitmap? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada. Insira sua chave sk-... para restaurar."))
        }
        val cleanInput = rawText.trim()
        if (cleanInput.isBlank() && pageBitmap == null) {
            return@withContext Result.failure(Exception("O conteúdo da página está vazio."))
        }

        try {
            val systemPrompt = """
                Você é um restaurador textual de máxima fidelidade e especialista em transcrição exata de livros digitalizados (OCR), livros acadêmicos e livros didáticos de Ciências Exatas e Naturais.
                REGRA ABSOLUTA E INEGOCIÁVEL:
                NÃO MUDE PRATICAMENTE NADA DO TEXTO. O texto DEVE continuar rigorosamente idêntico ao que o autor escreveu:
                - NÃO resuma, não explique e não parafraseie.
                - NÃO altere palavras por sinônimos, não modernize vocabulário, não corte trechos.
                - O texto tem que ser ORIGINAL do jeito que ele está no livro impresso real, reescrito na íntegra.

                DIRETRIZ CIENTÍFICA - SEM DEIXAR FALTAR NADA:
                Se a página contiver conteúdos de ciências exatas, biológicas ou da natureza, restaure cada elemento com fidelidade impecável:
                1. BIOLOGIA: Preserve terminologia científica e taxonômica, organelas celulares, vias metabólicas (glicólise, ciclo de Krebs, cadeia respiratória, fotossíntese), ácidos nucleicos (DNA, RNA, códons), reações bioquímicas, ATP e genética.
                2. QUÍMICA: Restaure e preserve equações químicas completas com balanceamento estequiométrico, setas de reação (→, ⇌, ⇄), estados de agregação ((s), (l), (g), (aq)), índices atômicos subscritos em Unicode límpido (ex: H₂O, CO₂, H₂SO₄, C₆H₁₂O₆), cargas iônicas sobrescritas (ex: Fe³⁺, Ca²⁺, SO₄²⁻), cadeias carbônicas e valores de entalpia (ΔH).
                3. FÍSICA: Preserve todas as fórmulas físicas, grandezas vetoriais, relações de proporcionalidade e todas as unidades do Sistema Internacional (SI) como N, J, W, Pa, Ω, T, V, Hz, kg·m/s², sem corromper potências ou índices.
                4. CÁLCULO & MATEMÁTICA: Restaure integrais (∫, ∬, ∮), derivadas (df/dx, ∂), limites (lim), frações empilhadas, raízes quadradas e enésimas (√, ∛), somatórios (∑), produtórios (∏), expoentes (x², e⁻ᵗ), matrizes, determinantes e equações polinomiais.
                5. RACIOCÍNIO LÓGICO & MATEMÁTICA DISCRETA: Preserve todos os conectivos e operadores proposicionais (conjunção ∧, disjunção ∨, negação ¬, condicional →, bicondicional ↔, disjunção exclusiva ⊻), quantificadores (universal ∀, existencial ∃), símbolos de dedução lógica (∴, ∵, ⊢, ⊨), operações da teoria dos conjuntos (∈, ∉, ⊂, ⊆, ∪, ∩, ∅, ℝ, ℕ, ℤ, ℚ) e tabelas-verdade.
                6. OPERAÇÕES DE QUALQUER ÁREA DE EXATAS: Trigonometria (sin, cos, tan), geometria, estatística e probabilidade.

                O QUE VOCÊ DEVE CONSERTAR:
                1. PONTUAÇÃO: Corrija e insira pontuações faltantes ou deslocadas por falha no OCR (pontos finais, vírgulas, dois-pontos, ponto e vírgula, aspas, travessões, interrogações e exclamações).
                2. CARACTERES E FÓRMULAS: Conserte caracteres quebrados, caracteres corrompidos por digitalização (ex: "rn" -> "m", "1" ou "|" -> "l", letras acentuadas no português, símbolos estranhos ou caracteres corrompidos).
                3. HIFENIZAÇÃO E QUEBRAS DE LINHA: Una palavras partidas indevidamente por quebra de linha com hífen (exemplo: "pa- lavra" -> "palavra", "trans- missão" -> "transmissão", "cons- ciência" -> "consciência").
                4. ESPAÇAMENTOS: Remova espaços duplicados ou espaços indevidos antes de pontuações.
                5. PARÁGRAFOS: Mantenha a divisão original de parágrafos do texto do livro.

                SAÍDA:
                Retorne EXCLUSIVAMENTE o texto restaurado na sua integridade literal exata, sem introdução, sem notas explicativas, sem saudações e SEM blocos de código Markdown (não use ```).
            """.trimIndent()

            val textPrompt = """
                Restaure o texto abaixo da página $pageNumber do livro "$bookTitle" usando rigorosamente as regras acima.
                Mantenha todas as palavras, equações, cálculos, reações químicas, estruturas biológicas e símbolos lógicos originais exatamente como estão, sem deixar faltar nada, consertando pontuação, caracteres corrompidos/OCR e quebras de linha:

                ${cleanInput.ifBlank { "(Página baseada na imagem visual fornecida)" }}
            """.trimIndent()

            val targetModel = if (pageBitmap != null && model.contains("3.5")) "gpt-4o-mini" else model.ifBlank { "gpt-4o-mini" }

            val userMessageObj = JSONObject().apply {
                put("role", "user")
                if (pageBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    val maxDim = 1280
                    val scaledBitmap = if (pageBitmap.width > maxDim || pageBitmap.height > maxDim) {
                        val factor = maxDim.toFloat() / maxOf(pageBitmap.width, pageBitmap.height)
                        Bitmap.createScaledBitmap(pageBitmap, (pageBitmap.width * factor).toInt(), (pageBitmap.height * factor).toInt(), true)
                    } else {
                        pageBitmap
                    }
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val base64Img = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$base64Img"

                    val contentArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", textPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", dataUrl)
                                put("detail", "high")
                            })
                        })
                    }
                    put("content", contentArr)
                } else {
                    put("content", textPrompt)
                }
            }

            val jsonBody = JSONObject().apply {
                put("model", targetModel)
                put("temperature", 0.0) // 0.0 garante máximo determinismo e fidelidade literal
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(userMessageObj)
                }
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(bodyStr).optJSONObject("error")?.optString("message") ?: "Erro HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Erro HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception("OpenAI: $errMsg"))
                }

                if (bodyStr.isBlank()) {
                    return@withContext Result.failure(Exception("Resposta vazia da API OpenAI."))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Nenhuma resposta gerada pela OpenAI."))
                }

                var restored = choices.getJSONObject(0)
                    .optJSONObject("message")
                    ?.optString("content") ?: ""

                restored = restored.trim()
                if (restored.startsWith("```markdown")) {
                    restored = restored.removePrefix("```markdown")
                } else if (restored.startsWith("```text")) {
                    restored = restored.removePrefix("```text")
                } else if (restored.startsWith("```")) {
                    restored = restored.removePrefix("```")
                }
                if (restored.endsWith("```")) {
                    restored = restored.removeSuffix("```")
                }
                restored = restored.trim()

                if (restored.isBlank()) {
                    return@withContext Result.failure(Exception("O texto retornado pela OpenAI estava vazio."))
                }

                Result.success(restored)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao restaurar página com OpenAI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Gera uma explicação detalhada e aprofundada de um trecho/frase/parágrafo do livro,
     * contendo análise conceitual minuciosa e 3 exemplos ricos e ilustrativos sobre o que o texto está falando.
     * Retorna a explicação em 3 idiomas: Espanhol (ES), Inglês (EN) e Francês (FR) - além de Português se aplicável.
     */
    suspend fun generateSegmentExplanationTrilateral(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookTitle: String,
        pageNumber: Int,
        segmentText: String
    ): Result<com.example.data.model.PageSegmentExplanation> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        val cleanText = segmentText.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(Exception("O trecho selecionado está vazio."))
        }

        try {
            val systemPrompt = """
                Você é um pedagogo, filósofo e analista literário multilíngue de elite.
                Sua tarefa é fornecer uma EXPLICAÇÃO PROFUNDA, DETALHADA E COMPLETA de um trecho específico de uma obra literária.
                
                REQUISITOS CRUCIAIS:
                1. A explicação deve destrinchar o sentido exato do trecho, o que o autor quis dizer, nuances conceituais e contexto.
                2. DEVE OBRIGATORIAMENTE conter 3 EXEMPLOS AMPLOS E ROBUSTOS sobre o que o trecho está falando (com casos reais, analogias do cotidiano ou situações práticas).
                3. A explicação deve ser redigida em 3 IDIOMAS distintos:
                   - ESPANHOL (ES)
                   - INGLÊS (EN)
                   - FRANCÊS (FR)
                   - PORTUGUÊS (PT)
                4. O texto da explicação em cada idioma deve ser denso, coeso, com linguagem clara e natural para leitura e síntese de voz (TTS).
                5. Responda ESTRITAMENTE em formato JSON com as chaves:
                   {
                     "explanation_spanish": "...",
                     "explanation_english": "...",
                     "explanation_french": "...",
                     "explanation_portuguese": "..."
                   }
                Não inclua nenhuma outra palavra ou formatação fora do JSON.
            """.trimIndent()

            val userContent = """
                Obra: $bookTitle
                Página: $pageNumber
                
                Trecho selecionado para explicação detalhada:
                "$cleanText"
            """.trimIndent()

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.4)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyStr)
                        errObj.optJSONObject("error")?.optString("message") ?: bodyStr
                    } catch (e: Exception) {
                        "Erro ${response.code}: $bodyStr"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("Conteúdo da explicação vazio."))
                }

                val jsonResult = JSONObject(content)
                val expEs = jsonResult.optString("explanation_spanish", "").trim()
                val expEn = jsonResult.optString("explanation_english", "").trim()
                val expFr = jsonResult.optString("explanation_french", "").trim()
                val expPt = jsonResult.optString("explanation_portuguese", "").trim()

                val result = com.example.data.model.PageSegmentExplanation(
                    bookId = 0,
                    pageNumber = pageNumber,
                    segmentIndex = 0,
                    segmentText = cleanText,
                    explanationSpanish = expEs,
                    explanationEnglish = expEn,
                    explanationFrench = expFr,
                    explanationPortuguese = expPt,
                    modelUsed = model.ifBlank { "gpt-4o-mini" }
                )

                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar explicação de trecho com OpenAI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Gera uma explicação jurídica e didática minuciosa de um artigo ou dispositivo de lei,
     * contendo análise conceitual hermenêutica e OBRIGATORIAMENTE 3 exemplos práticos e ricos
     * utilizando personagens clássicos (Vilela, Janinho, Márcia, Parroca, Lorenzzo, etc.)
     * Retorna a explicação em 4 idiomas (Português, Espanhol, Inglês e Francês).
     */
    suspend fun generateLawExplanationWithCharacters(
        apiKey: String,
        model: String = "gpt-4o-mini",
        lawTitle: String,
        articleHeader: String,
        articleText: String
    ): Result<com.example.data.model.PageSegmentExplanation> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada. Insira sua chave OpenAI para gerar a explicação."))
        }
        val cleanText = articleText.trim()
        if (cleanText.isBlank() && articleHeader.isBlank()) {
            return@withContext Result.failure(Exception("O texto do dispositivo legal está vazio."))
        }

        try {
            val systemPrompt = """
                Você é um eminente jurista, professor de Direito e preceptor de alta performance em concursos públicos (Magistratura, Ministério Público, Defensoria, Delegado e Carreiras Jurídicas).
                Sua tarefa é fornecer uma EXPLICAÇÃO JURÍDICA COMPLETA, PEDAGÓGICA E PROFUNDA sobre o dispositivo legal.
                
                DIRETRIZES FUNDAMENTAIS OBRIGATÓRIAS:
                1. DESTRIÇAR O SIGNIFICADO JURÍDICO:
                   - Explique a finalidade da norma, bem jurídico tutelado, destinatários, regras de aplicação e eventuais posicionamentos doutrinários/jurisprudenciais pacificados.
                2. OBRIGATÓRIO: 3 EXEMPLOS PRÁTICOS RICOS COM PERSONAGENS:
                   - Forneça OBRIGATORIAMENTE 3 casos concretos ilustrando a aplicação prática da norma, utilizando explicitamente os personagens: "Vilela", "Janinho", "Márcia", "Parroca", "Lorenzzo", etc., adaptando cada caso ao contexto do artigo.
                   - Exemplo 1: Situação cotidiana com Vilela e Janinho.
                   - Exemplo 2: Situação processual ou contratual com Márcia e Parroca.
                   - Exemplo 3: Situação de prova/concurso com Lorenzzo.
                3. IDIOMAS E ESTILO:
                   - Redija em linguagem culta, elegante e fluida, sem asteriscos ou marcadores poluídos, adequada para leitura visual e conversão em voz neural (TTS).
                   - Forneça a explicação em 4 IDIOMAS: Português (PT), Espanhol (ES), Inglês (EN) e Francês (FR).
                4. FORMATO ESTRITO: Responda ESTRITAMENTE em formato JSON com as chaves:
                   {
                     "explanation_portuguese": "...",
                     "explanation_spanish": "...",
                     "explanation_english": "...",
                     "explanation_french": "..."
                   }
            """.trimIndent()

            val userContent = """
                Norma: $lawTitle
                Dispositivo: $articleHeader
                
                Texto da Lei:
                \"\"\"
                $articleHeader
                $cleanText
                \"\"\"
            """.trimIndent()

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.4)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .header("Authorization", "Bearer $cleanKey")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = try {
                    val errJson = JSONObject(respBody)
                    errJson.optJSONObject("error")?.optString("message") ?: respBody
                } catch (e: Exception) {
                    respBody
                }
                return@withContext Result.failure(Exception("OpenAI API: $errMsg"))
            }

            val respJson = JSONObject(respBody)
            val choices = respJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
            }

            val choice0 = choices.getJSONObject(0)
            val message = choice0.optJSONObject("message")
            val rawContent = message?.optString("content") ?: ""

            val parsedExpJson = JSONObject(rawContent)
            val expPt = parsedExpJson.optString("explanation_portuguese").trim()
            val expEs = parsedExpJson.optString("explanation_spanish").trim()
            val expEn = parsedExpJson.optString("explanation_english").trim()
            val expFr = parsedExpJson.optString("explanation_french").trim()

            val result = com.example.data.model.PageSegmentExplanation(
                id = System.currentTimeMillis(),
                bookId = 0,
                pageNumber = 1,
                segmentIndex = 0,
                segmentText = "$articleHeader: $cleanText",
                explanationSpanish = expEs,
                explanationEnglish = expEn,
                explanationFrench = expFr,
                explanationPortuguese = expPt.ifBlank { expEs },
                modelUsed = model.ifBlank { "gpt-4o-mini" }
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar explicação jurídica com OpenAI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Correção local determinística caso o usuário queira reparar pontuação e caracteres sem internet.
     */
    fun runLocalTextPunctuationFix(rawText: String): String {
        if (rawText.isBlank()) return ""
        var text = rawText
        // 1. Une hifens no final de linha ("pa-\nlavra" -> "palavra")
        text = text.replace(Regex("(\\b\\p{L}+)-\\s*\\r?\\n\\s*(\\p{L}+\\b)"), "$1$2")
        // 2. Une hifens com espaço intermediário ("pa- lavra" -> "palavra")
        text = text.replace(Regex("(\\b\\p{L}{2,})-\\s+(\\p{L}{2,}\\b)"), "$1$2")
        // 3. Remove espaços antes de pontuação
        text = text.replace(Regex("\\s+([,.:;?!])"), "$1")
        // 4. Garante espaço após pontuação se seguida de letra maiúscula
        text = text.replace(Regex("([,.:;?!])([\\p{Lu}])"), "$1 $2")
        // 5. Normaliza quebras de linha excessivas
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("(\\r?\\n){3,}"), "\n\n")
        return text.trim()
    }

    /**
     * Envia uma pergunta do usuário no chat contextual sobre uma etapa/sessão específica do livro.
     * Retorna uma resposta certeira, altamente explicativa, pedagógica, acadêmica e sem asteriscos ou formatações genéricas.
     */
    suspend fun sendSegmentChatMessage(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookTitle: String,
        bookAuthor: String,
        pageNumber: Int,
        segmentText: String,
        history: List<Pair<String, String>>,
        userQuestion: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave de API da OpenAI não configurada. Insira sua chave nas configurações ou no topo da tela."))
        }

        try {
            val systemPrompt = """
                Você é um preceptor, filósofo e crítico literário de elite.
                O usuário está lendo e dissecando a obra: "$bookTitle", de autoria de: "$bookAuthor" (Página $pageNumber).
                Esta conversa foca exclusivamente na seguinte etapa/sessão do texto original:
                
                \"\"\"
                $segmentText
                \"\"\"
                
                DIRETRIZES FUNDAMENTAIS PARA RESPOSTAS PROFISSIONAIS DE ALTO NÍVEL:
                1. RESPOSTAS CERTEIRAS E PROFUNDAS: Seja direto ao ponto indagado pelo usuário com profundidade analítica, erudição acessível e precisão cirúrgica. Jamais forneça saudações vazias, frases clichês ("Olá! Como posso ajudar?", "Ótima pergunta!"), nem introduções e conclusões protocolares. Comece respondendo a essência da questão imediatamente.
                2. NUNCA USE ASTERISCOS OU MARCADORES DE MARKDOWN EXCESSIVOS: Não use ***, **, * nem # em nenhuma circunstância. Apresente o texto em prosa fluida, elegante e contínua, estruturada em parágrafos coesos e bem pontuados, adequada para leitura visual e conversão em voz (TTS).
                3. EXPLICATIVIDADE E CONTEXTO: Destrinche o significado denotativo e conotativo das palavras, a intenção do autor, a época histórica e as correntes de pensamento subjacentes a esta etapa.
                4. COERÊNCIA CONTEXTUAL: Mantenha estrita conexão com a passagem citada e o histórico de perguntas desta etapa do livro.
            """.trimIndent()

            val messagesArray = JSONArray()
            messagesArray.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemPrompt)
            )

            // Inclui até as 10 mensagens anteriores para manter o fio da meada
            val recentHistory = history.takeLast(10)
            for ((role, content) in recentHistory) {
                val apiRole = if (role.equals("USER", ignoreCase = true) || role.equals("user", ignoreCase = true)) "user" else "assistant"
                messagesArray.put(
                    JSONObject()
                        .put("role", apiRole)
                        .put("content", content)
                )
            }

            // Pergunta atual do usuário
            messagesArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("content", userQuestion.trim())
            )

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", messagesArray)
                put("temperature", 0.5)
                put("max_tokens", 1600)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyStr)
                        errObj.optJSONObject("error")?.optString("message") ?: bodyStr
                    } catch (e: Exception) {
                        "Erro ${response.code}: $bodyStr"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Resposta vazia retornada pela OpenAI."))
                }

                val reply = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                if (reply.isBlank()) {
                    return@withContext Result.failure(Exception("Conteúdo da resposta OpenAI veio em branco."))
                }

                // Higienização completa para evitar asteriscos (***, **, *), hashtags e quebras desnecessárias
                val cleanReply = reply
                    .replace(Regex("\\*{1,3}"), "") // remove asteriscos
                    .replace(Regex("(?m)^#{1,6}\\s*"), "") // remove cabeçalhos markdown
                    .replace(Regex("\n{3,}"), "\n\n") // normaliza parágrafos
                    .trim()

                Result.success(cleanReply)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no chat OpenAI do trecho: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Realiza uma análise lexical profunda de nível C2 para qualquer palavra em inglês.
     * Retorna fonética IPA, nível CEFR, colocations, etimologia, sinônimos com nuance e desafio ativo.
     */
    suspend fun analyzeEnglishWordC2(
        apiKey: String,
        word: String,
        contextSentence: String = "",
        bookTitle: String = "",
        model: String = "gpt-4o-mini"
    ): Result<com.example.data.model.EnglishWordAnalysis> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanWord = word.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (cleanWord.isBlank()) {
            return@withContext Result.failure(Exception("Palavra vazia para análise."))
        }

        try {
            val systemPrompt = """
                You are a world-class Oxford/Cambridge linguist, lexicographer, and C2 English Mastery Coach.
                Your task is to analyze ANY English word, phrasal verb, or idiom in high pedagogical detail for a student aiming for full C2 Mastery.
                
                Respond STRICTLY with a valid JSON object matching this schema:
                {
                  "word": "canonical word or expression",
                  "phoneticIpa": "/IPA phonetic transcription/",
                  "cefrLevel": "A1, A2, B1, B2, C1, or C2",
                  "partOfSpeech": "noun, verb, adjective, phrasal verb, idiom, etc.",
                  "englishDefinition": "Clear, elegant Oxford-style monolingual definition in English",
                  "portugueseMeaning": "Tradução precisa em português explicando a nuance exata e conotação",
                  "registerAndTone": "e.g. Formal / Literary / Academic / Informal / Idiomatic",
                  "collocations": ["frequent collocation 1", "frequent collocation 2", "frequent collocation 3"],
                  "synonymsWithNuance": ["synonym 1 (explain nuance difference)", "synonym 2 (explain nuance difference)"],
                  "contextualSentence": "Natural, high-register example sentence showing the word in action",
                  "sentenceTranslation": "Tradução da frase exemplar para o português",
                  "etymologyRoot": "Etimologia sucinta (raiz latina, grega, francesa ou germânica)",
                  "usageTipC2": "Dica prática de ouro para soar nativo nível C2 (como nativos cultos usam vs erro de intermediários)",
                  "activeChallengePrompt": "Desafio de 1 frase para o leitor produzir e fixar a palavra",
                  "isIdiomOrPhrasalVerb": false or true
                }
            """.trimIndent()

            val userContent = buildString {
                appendLine("Target Word / Expression: $cleanWord")
                if (contextSentence.isNotBlank()) {
                    appendLine("Sentence Context in Book: \"$contextSentence\"")
                }
                if (bookTitle.isNotBlank()) {
                    appendLine("Source Book: $bookTitle")
                }
            }

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.3)
                put("max_tokens", 1000)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyStr)
                        errObj.optJSONObject("error")?.optString("message") ?: bodyStr
                    } catch (e: Exception) {
                        "Erro ${response.code}: $bodyStr"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                val content = choices?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val json = JSONObject(content)
                val collArray = json.optJSONArray("collocations")
                val collocations = mutableListOf<String>()
                if (collArray != null) {
                    for (i in 0 until collArray.length()) collocations.add(collArray.getString(i))
                }

                val synArray = json.optJSONArray("synonymsWithNuance")
                val synonyms = mutableListOf<String>()
                if (synArray != null) {
                    for (i in 0 until synArray.length()) synonyms.add(synArray.getString(i))
                }

                val analysis = com.example.data.model.EnglishWordAnalysis(
                    word = json.optString("word", cleanWord),
                    phoneticIpa = json.optString("phoneticIpa", ""),
                    cefrLevel = json.optString("cefrLevel", "C1"),
                    partOfSpeech = json.optString("partOfSpeech", ""),
                    englishDefinition = json.optString("englishDefinition", ""),
                    portugueseMeaning = json.optString("portugueseMeaning", ""),
                    registerAndTone = json.optString("registerAndTone", "Formal / Literário"),
                    collocations = collocations,
                    synonymsWithNuance = synonyms,
                    contextualSentence = json.optString("contextualSentence", ""),
                    sentenceTranslation = json.optString("sentenceTranslation", ""),
                    etymologyRoot = json.optString("etymologyRoot", ""),
                    usageTipC2 = json.optString("usageTipC2", ""),
                    activeChallengePrompt = json.optString("activeChallengePrompt", "Crie uma frase em inglês usando '$cleanWord'."),
                    isIdiomOrPhrasalVerb = json.optBoolean("isIdiomOrPhrasalVerb", false)
                )

                Result.success(analysis)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar palavra C2: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Analisa o vocabulário da página inteira e extrai as palavras mais sofisticadas (níveis B2, C1, C2)
     * e expressões idiomáticas / phrasal verbs presentes naquele trecho.
     */
    suspend fun analyzeEnglishPageVocabularyC2(
        apiKey: String,
        bookTitle: String,
        pageNumber: Int,
        pageText: String,
        model: String = "gpt-4o-mini"
    ): Result<List<com.example.data.model.EnglishWordAnalysis>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (pageText.isBlank()) {
            return@withContext Result.failure(Exception("Texto da página vazio para análise."))
        }

        try {
            val systemPrompt = """
                You are an elite C2 English lexicographer and vocabulary extractor.
                Scan the provided book page text and extract the most valuable, sophisticated, or challenging vocabulary items (focus on B2, C1, C2 levels, plus idioms and phrasal verbs).
                Extract between 5 and 10 notable terms from the page.
                
                Respond STRICTLY with a JSON object containing an array "words":
                {
                  "words": [
                    {
                      "word": "word or idiom",
                      "phoneticIpa": "/IPA/",
                      "cefrLevel": "B2, C1, or C2",
                      "partOfSpeech": "noun, verb, etc.",
                      "englishDefinition": "Oxford-style English definition",
                      "portugueseMeaning": "Tradução precisa em português e nuance",
                      "registerAndTone": "Formal / Literary / Academic",
                      "collocations": ["collocation 1", "collocation 2"],
                      "synonymsWithNuance": ["synonym with difference"],
                      "contextualSentence": "Original sentence from the text or clear usage",
                      "sentenceTranslation": "Tradução",
                      "etymologyRoot": "Etimologia resumida",
                      "usageTipC2": "Dica C2 de uso nativo",
                      "activeChallengePrompt": "Desafio de produção",
                      "isIdiomOrPhrasalVerb": false or true
                    }
                  ]
                }
            """.trimIndent()

            val userContent = """
                Book: $bookTitle
                Page: $pageNumber
                
                Page Text:
                $pageText
            """.trimIndent()

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.3)
                put("max_tokens", 2200)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Erro ${response.code}: $bodyStr"))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                val content = choices?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val json = JSONObject(content)
                val wordsArray = json.optJSONArray("words") ?: JSONArray()
                val list = mutableListOf<com.example.data.model.EnglishWordAnalysis>()

                for (i in 0 until wordsArray.length()) {
                    val wObj = wordsArray.getJSONObject(i)
                    val collArray = wObj.optJSONArray("collocations")
                    val collocations = mutableListOf<String>()
                    if (collArray != null) {
                        for (c in 0 until collArray.length()) collocations.add(collArray.getString(c))
                    }

                    val synArray = wObj.optJSONArray("synonymsWithNuance")
                    val synonyms = mutableListOf<String>()
                    if (synArray != null) {
                        for (s in 0 until synArray.length()) synonyms.add(synArray.getString(s))
                    }

                    list.add(
                        com.example.data.model.EnglishWordAnalysis(
                            word = wObj.optString("word", ""),
                            phoneticIpa = wObj.optString("phoneticIpa", ""),
                            cefrLevel = wObj.optString("cefrLevel", "C1"),
                            partOfSpeech = wObj.optString("partOfSpeech", ""),
                            englishDefinition = wObj.optString("englishDefinition", ""),
                            portugueseMeaning = wObj.optString("portugueseMeaning", ""),
                            registerAndTone = wObj.optString("registerAndTone", "Formal"),
                            collocations = collocations,
                            synonymsWithNuance = synonyms,
                            contextualSentence = wObj.optString("contextualSentence", ""),
                            sentenceTranslation = wObj.optString("sentenceTranslation", ""),
                            etymologyRoot = wObj.optString("etymologyRoot", ""),
                            usageTipC2 = wObj.optString("usageTipC2", ""),
                            activeChallengePrompt = wObj.optString("activeChallengePrompt", ""),
                            isIdiomOrPhrasalVerb = wObj.optBoolean("isIdiomOrPhrasalVerb", false)
                        )
                    )
                }

                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar vocabulário da página C2: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Avalia uma frase produzida pelo usuário em inglês para verificar se soa natural e digna de nível C2,
     * corrigindo gramática e sugerindo reformulações nativas elegantes.
     */
    suspend fun evaluateEnglishSentenceC2(
        apiKey: String,
        targetWord: String,
        userSentence: String,
        model: String = "gpt-4o-mini"
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (userSentence.isBlank()) {
            return@withContext Result.failure(Exception("Digite uma frase para avaliação."))
        }

        try {
            val systemPrompt = """
                You are a master C2 English assessor and coach.
                The user has written a sentence to practice using the target word: "$targetWord".
                Provide clear, encouraging, expert feedback in Portuguese and English:
                1. 🎯 **Avaliação de Naturalidade (0 a 10)**: Avalie se soa como um falante nativo.
                2. 🔍 **Correções Gramaticais & Sintaxe**: Destaque ajustes necessários ou confirme se está perfeita.
                3. 🌟 **Versão Nativa Nível C2 (Polimento Literário)**: Reescreva a ideia do usuário com vocabulário culto e estruturas avançadas (inversion, cleft sentences ou collocations sofisticadas).
                4. 💡 **Por que a versão C2 se destaca**: Explicação curta da nuance.
            """.trimIndent()

            val userContent = "Target Word: $targetWord\nUser Sentence: $userSentence"

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
                put("temperature", 0.4)
                put("max_tokens", 800)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Erro ${response.code}: $bodyStr"))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                val reply = choices?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                Result.success(reply.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao avaliar frase C2: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun evaluatePolyglotSentenceC2(
        apiKey: String,
        language: com.example.data.model.StudyLanguage,
        targetWord: String,
        userSentence: String,
        model: String = "gpt-4o-mini"
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (userSentence.isBlank()) {
            return@withContext Result.failure(Exception("Digite uma frase para avaliação."))
        }

        try {
            val systemPrompt = """
                You are a master C2 language coach for ${language.displayName} (${language.nativeName}).
                The user has written a practice sentence in ${language.displayName} using the target word: "$targetWord".
                Provide clear, encouraging feedback in Portuguese and ${language.displayName}:
                1. 🎯 **Avaliação de Naturalidade (0 a 10)**: Avalie concordância, partículas/casos/gênero e naturalidade nativa.
                2. 🔍 **Correções Morfossintáticas**: Destaque ajustes necessários (declinações, conjugações, artigos, ordem das palavras).
                3. 🌟 **Versão Nativa Nível C2**: Reescreva a frase de forma literária e fluente.
                4. 💡 **Explicação da Nuance C2**: Explique por que a versão recomendada soa nativa.
            """.trimIndent()

            val userContent = "Language: ${language.displayName}\nTarget Word: $targetWord\nUser Sentence: $userSentence"

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
                put("temperature", 0.4)
                put("max_tokens", 900)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Erro ${response.code}: $bodyStr"))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                val reply = choices?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                Result.success(reply.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao avaliar frase poliglota: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Análise profunda poliglota adaptada para Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico e Inglês.
     */
    suspend fun analyzePolyglotWord(
        apiKey: String,
        word: String,
        language: com.example.data.model.StudyLanguage,
        contextSentence: String = "",
        bookTitle: String = "",
        model: String = "gpt-4o-mini"
    ): Result<com.example.data.model.PolyglotWordAnalysis> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanWord = word.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (cleanWord.isBlank()) {
            return@withContext Result.failure(Exception("Palavra vazia para análise."))
        }

        try {
            val systemPrompt = """
                You are a world-leading polyglot linguist and master lexicographer specializing in ${language.displayName} (${language.nativeName}).
                Analyze the given word/expression for an advanced student targeting complete fluency / C2 literary mastery.
                
                Language-specific guidelines:
                - If Russian or Ukrainian: Provide stress mark (e.g. соба́ка), grammar case/declension, aspect (perfective/imperfective), and root.
                - If German: Provide gender with color context (der/die/das), plural form, and compound breakdown (Komposita).
                - If Chinese: Provide Pinyin with tone marks, radicals, stroke nuance, and HSK/C2 register.
                - If Japanese: Provide Furigana / Hiragana, Kanji on/kun readings, and JLPT/literary nuance.
                - If Greek: Provide Greek diacritics/stress, transliteration, and ancient/modern etymology.
                - If Arabic: Provide full Tashkeel/Harakaat vocalization, triliteral root (e.g. ك-ت-ب), and verb form (I-X).
                - If Hebrew: Provide full Niqqud vocalization, root (שורש), and binyan/structure.
                - If English: Provide IPA, Oxford-style definition, collocations, and C2 nuance.

                Respond STRICTLY with a valid JSON matching this schema:
                {
                  "word": "canonical word or dictionary lemma",
                  "transliteration": "Pinyin / Romaji / Latin transliteration",
                  "phoneticIpa": "/IPA phonetic transcription/",
                  "cefrLevel": "B2, C1, or C2",
                  "partOfSpeech": "noun, verb, adjective, particle, idiom, etc.",
                  "nativeScriptWithVocal": "Word with full vocalization/accents (Tashkeel, Niqqud, Stress marks)",
                  "rootOrRadical": "Root/Radical (e.g. triliteral root, kanji radical, or etymological root)",
                  "genderOrGrammarCase": "e.g. der/die/das in German, Case in Slavic, Form in Arabic",
                  "compoundBreakdown": "Breakdown into sub-elements if compound, or empty",
                  "monolingualDefinition": "Elegant monolingual definition in ${language.displayName}",
                  "portugueseMeaning": "Tradução detalhada em português com explicação da nuance e tom",
                  "registerAndTone": "Formal / Literário / Poético / Técnico / Diário",
                  "collocations": ["collocation 1", "collocation 2", "collocation 3"],
                  "synonymsWithNuance": ["synonym 1 (nuance difference)", "synonym 2 (nuance difference)"],
                  "contextualSentence": "High-register example sentence in ${language.displayName}",
                  "sentenceTranslation": "Tradução da frase para o português",
                  "usageTipC2": "Dica de ouro para soar como um falante nativo culto nível C2",
                  "activeChallengePrompt": "Desafio prático de 1 frase para o estudante fixar o termo",
                  "isIdiomOrPhrasalVerb": false or true
                }
            """.trimIndent()

            val userContent = buildString {
                appendLine("Target Language: ${language.displayName} (${language.code})")
                appendLine("Word / Expression: $cleanWord")
                if (contextSentence.isNotBlank()) {
                    appendLine("Sentence Context in Book: \"$contextSentence\"")
                }
                if (bookTitle.isNotBlank()) {
                    appendLine("Source Book: $bookTitle")
                }
            }

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.3)
                put("max_tokens", 1400)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val err = try { JSONObject(bodyStr).optJSONObject("error")?.optString("message") ?: bodyStr } catch (e: Exception) { bodyStr }
                    return@withContext Result.failure(Exception("Erro ${response.code}: $err"))
                }

                val resObj = JSONObject(bodyStr)
                val content = resObj.optJSONArray("choices")?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val json = JSONObject(content)
                val collArray = json.optJSONArray("collocations")
                val collocations = mutableListOf<String>()
                if (collArray != null) {
                    for (i in 0 until collArray.length()) collocations.add(collArray.getString(i))
                }

                val synArray = json.optJSONArray("synonymsWithNuance")
                val synonyms = mutableListOf<String>()
                if (synArray != null) {
                    for (i in 0 until synArray.length()) synonyms.add(synArray.getString(i))
                }

                val result = com.example.data.model.PolyglotWordAnalysis(
                    word = json.optString("word", cleanWord),
                    languageCode = language.code,
                    transliteration = json.optString("transliteration", ""),
                    phoneticIpa = json.optString("phoneticIpa", ""),
                    cefrLevel = json.optString("cefrLevel", "C1"),
                    partOfSpeech = json.optString("partOfSpeech", ""),
                    nativeScriptWithVocal = json.optString("nativeScriptWithVocal", ""),
                    rootOrRadical = json.optString("rootOrRadical", ""),
                    genderOrGrammarCase = json.optString("genderOrGrammarCase", ""),
                    compoundBreakdown = json.optString("compoundBreakdown", ""),
                    monolingualDefinition = json.optString("monolingualDefinition", ""),
                    portugueseMeaning = json.optString("portugueseMeaning", ""),
                    registerAndTone = json.optString("registerAndTone", "Formal / Literário"),
                    collocations = collocations,
                    synonymsWithNuance = synonyms,
                    contextualSentence = json.optString("contextualSentence", ""),
                    sentenceTranslation = json.optString("sentenceTranslation", ""),
                    usageTipC2 = json.optString("usageTipC2", ""),
                    activeChallengePrompt = json.optString("activeChallengePrompt", "Crie uma frase autêntica usando '$cleanWord'."),
                    isIdiomOrPhrasalVerb = json.optBoolean("isIdiomOrPhrasalVerb", false)
                )

                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar palavra poliglota: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Escaneia e extrai vocabulário profundo da página inteira para o idioma escolhido.
     */
    suspend fun analyzePolyglotPageVocabulary(
        apiKey: String,
        language: com.example.data.model.StudyLanguage,
        bookTitle: String,
        pageNumber: Int,
        pageText: String,
        model: String = "gpt-4o-mini"
    ): Result<List<com.example.data.model.PolyglotWordAnalysis>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (pageText.isBlank()) {
            return@withContext Result.failure(Exception("Texto da página vazio para análise."))
        }

        try {
            val systemPrompt = """
                You are a world-class polyglot lexicographer for ${language.displayName} (${language.nativeName}).
                Scan the provided book page text in ${language.displayName} and extract 6 to 12 of the most valuable, sophisticated words or idioms (focusing on B2, C1, and C2 levels).
                Ensure accurate transliterations, roots/radicals, gender/cases, and vocalizations according to ${language.displayName} rules.

                Respond STRICTLY with a valid JSON object matching:
                {
                  "words": [
                    {
                      "word": "lemma or expression",
                      "transliteration": "Latin transcription",
                      "phoneticIpa": "/IPA/",
                      "cefrLevel": "B2, C1, or C2",
                      "partOfSpeech": "part of speech",
                      "nativeScriptWithVocal": "vocalized/accented text",
                      "rootOrRadical": "root or radical",
                      "genderOrGrammarCase": "gender/case",
                      "compoundBreakdown": "breakdown",
                      "monolingualDefinition": "monolingual definition",
                      "portugueseMeaning": "tradução e nuance em português",
                      "registerAndTone": "register",
                      "collocations": ["collocation 1", "collocation 2"],
                      "synonymsWithNuance": ["synonym 1"],
                      "contextualSentence": "sentence from page or context",
                      "sentenceTranslation": "tradução",
                      "usageTipC2": "dica C2",
                      "activeChallengePrompt": "desafio",
                      "isIdiomOrPhrasalVerb": false or true
                    }
                  ]
                }
            """.trimIndent()

            val userContent = """
                Language: ${language.displayName} (${language.code})
                Book: $bookTitle
                Page: $pageNumber

                Page Text:
                $pageText
            """.trimIndent()

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.3)
                put("max_tokens", 2500)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Erro ${response.code}: $bodyStr"))
                }

                val resObj = JSONObject(bodyStr)
                val content = resObj.optJSONArray("choices")?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                if (content.isBlank()) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val json = JSONObject(content)
                val wordsArray = json.optJSONArray("words") ?: JSONArray()
                val list = mutableListOf<com.example.data.model.PolyglotWordAnalysis>()

                for (i in 0 until wordsArray.length()) {
                    val wObj = wordsArray.getJSONObject(i)
                    val collArray = wObj.optJSONArray("collocations")
                    val collocations = mutableListOf<String>()
                    if (collArray != null) {
                        for (c in 0 until collArray.length()) collocations.add(collArray.getString(c))
                    }

                    val synArray = wObj.optJSONArray("synonymsWithNuance")
                    val synonyms = mutableListOf<String>()
                    if (synArray != null) {
                        for (s in 0 until synArray.length()) synonyms.add(synArray.getString(s))
                    }

                    list.add(
                        com.example.data.model.PolyglotWordAnalysis(
                            word = wObj.optString("word", ""),
                            languageCode = language.code,
                            transliteration = wObj.optString("transliteration", ""),
                            phoneticIpa = wObj.optString("phoneticIpa", ""),
                            cefrLevel = wObj.optString("cefrLevel", "C1"),
                            partOfSpeech = wObj.optString("partOfSpeech", ""),
                            nativeScriptWithVocal = wObj.optString("nativeScriptWithVocal", ""),
                            rootOrRadical = wObj.optString("rootOrRadical", ""),
                            genderOrGrammarCase = wObj.optString("genderOrGrammarCase", ""),
                            compoundBreakdown = wObj.optString("compoundBreakdown", ""),
                            monolingualDefinition = wObj.optString("monolingualDefinition", ""),
                            portugueseMeaning = wObj.optString("portugueseMeaning", ""),
                            registerAndTone = wObj.optString("registerAndTone", "Formal"),
                            collocations = collocations,
                            synonymsWithNuance = synonyms,
                            contextualSentence = wObj.optString("contextualSentence", ""),
                            sentenceTranslation = wObj.optString("sentenceTranslation", ""),
                            usageTipC2 = wObj.optString("usageTipC2", ""),
                            activeChallengePrompt = wObj.optString("activeChallengePrompt", ""),
                            isIdiomOrPhrasalVerb = wObj.optBoolean("isIdiomOrPhrasalVerb", false)
                        )
                    )
                }

                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar vocabulário da página poliglota: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Análise estrutural e gramatical profunda de frases complexas nos 8 idiomas de alta complexidade:
     * Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico (e Inglês).
     * Fornece árvore sintática, decomposição token a token, casos cirílicos, partículas japonesas,
     * regras de ordem verbal V2 alemã, raízes semíticas, etc., retornando o modelo pronto para estudo local.
     */
    suspend fun analyzeSentenceGrammarAndSyntax(
        apiKey: String,
        sentence: String,
        language: com.example.data.model.StudyLanguage,
        bookTitle: String = "",
        bookId: Int = 0,
        pageNumber: Int = 1,
        model: String = "gpt-4o-mini"
    ): Result<com.example.data.model.SavedSentenceAnalysis> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanSentence = sentence.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        if (cleanSentence.isBlank()) {
            return@withContext Result.failure(Exception("Frase vazia para análise."))
        }

        try {
            val systemPrompt = """
                You are a world-class theoretical and pedagogical linguist, master syntactician, and native-level grammar professor in ${language.displayName} (${language.nativeName}).
                Your task is to analyze a complex sentence in depth for an advanced student aiming for C1/C2 mastery.
                
                You must pay deep, rigorous attention to the target language's structural mechanics:
                - Russian & Ukrainian:
                  * Cyrillic case usage (Nominative, Genitive, Dative, Accusative, Instrumental, Prepositional, and Vocative for Ukrainian).
                  * Verb aspect (perfective vs imperfective), government of prepositions (управление предлогов), animate vs inanimate accusative, reflexive participles, and gerunds (деепричастия).
                - German:
                  * Clause architecture: Main clause V2 rule, subordinate clause verb-final (kicker), infinitive clauses with 'zu', TeKaMoLo order of adverbials.
                  * Four cases (Nominativ, Akkusativ, Dativ, Genitiv), adjective declension (strong/weak/mixed), separable prefixes, passive voice forms (Vorgangspassiv vs Zustandspassiv), and Konjunktiv II.
                - Japanese:
                  * Particle system: Subject/Topic distinction (は vs が), case particles (を, に, で, と, へ, から, まで, より), sentence-ending particles, and binding particles.
                  * Sentence structure (SOV), relative clauses modifying head nouns, te-form connections, honorific register (Keigo: Sonkeigo, Kenjougo, Teineigo), causative and passive conjugations.
                - Chinese (Mandarin):
                  * Topic-comment structure, lack of inflection compensated by syntax and word order.
                  * Measure words / classifiers (量词), aspect markers (了, 着, 过), structural particles (的, 得, 地), disposal construction (把), passive construction (被), and resultative/directional complements.
                - Greek:
                  * Modern/Classical Greek syntax: Noun/adjective declensions (Nom, Gen, Acc, Voc), verbal aspects (Aorist vs Imperfective vs Perfect), clitic pronoun placement, subjunctive markers (να).
                - Arabic:
                  * Syntax (I'rab / إعراب): Marfoo' (مرفوع), Mansoub (منصوب), Majroor (مجرور), Majzoom (مجزوم).
                  * Nominal sentence (جملة اسمية - مبتدأ وخبر) vs Verbal sentence (جملة فعلية - فعل وفاعل ومفعول).
                  * Idafa construct state (إضافة), Nunation (تنوين), triliteral roots (جذور) and morphological weight templates (أوزان).
                - Hebrew:
                  * Smikhut construct state (סמיכות), Binyanim verb templates (פיעל, הפעיל, התפעל, etc.), Shoresh 3-letter roots (שורש).
                  * Direct object marker 'et' (את), prepositional prefixes (ב, ל, מ, כ), and relative connector (ש).
                - English & others:
                  * Clause structures, inversion, conditional nuances, participle clauses, syntactic tree breakdown.
                
                Respond STRICTLY with a valid JSON matching this schema:
                {
                  "transliteration": "Romanization, Pinyin with tones, Romaji, or phonetic transliteration for non-Latin scripts",
                  "translationPt": "Tradução elegante e precisa para o português destacando a nuance estilística",
                  "syntacticTreeSummary": "Resumo claro da arquitetura sintática da frase (ex: Oração principal com verbo em posição V2 + Oração subordinada concessiva com verbo no final; Estrutura Tópico-Comentário com partícula は, etc.)",
                  "complexityLevel": "B2, C1, or C2",
                  "tokens": [
                    {
                      "token": "palavra ou constituinte sintático",
                      "reading": "Furigana, Pinyin, transliteração ou vocalização (Tashkeel/Niqqud)",
                      "syntacticRole": "Papel sintático (ex: Sujeito Gramatical, Objeto Indireto, Partícula de Tópico は, Núcleo Verbal, Marcador de Aspecto)",
                      "morphologyOrCase": "Caso e Morfologia (ex: Genitivo singular feminino, Verbo V2, Partícula enclítica, Preposição com Dativo)",
                      "rootOrLemma": "Forma canônica de dicionário ou raiz consonantal (ex: K-T-B, 食べる, gehen)",
                      "explanation": "Explicação concisa e clara da função desta palavra nesta oração"
                    }
                  ],
                  "grammarRules": [
                    {
                      "ruleTitle": "Título da regra (ex: Uso do Caso Instrumental após preposição С, Contraste de Partículas は vs が, Regra TeKaMoLo de Advérbios)",
                      "category": "Casos & Declinações, Partículas & Sintaxe, Morfologia Verbal, Ordem de Palavras, ou Conectivos",
                      "explanation": "Explicação pedagógica detalhada de como e por que esta regra opera nesta oração",
                      "exampleSnippet": "Trecho exato da frase que ilustra a regra"
                    }
                  ],
                  "c2MasteryTip": "Dica de ouro de nível nativo/C2 sobre como pensar internamente nesta estrutura sem traduzir palavra por palavra",
                  "practiceChallenge": "Desafio de fixação rápida para o estudante produzir uma variação estrutural desta mesma sintaxe"
                }
            """.trimIndent()

            val userContent = buildString {
                appendLine("Target Language: ${language.displayName} (${language.code}) - ${language.scriptFamily}")
                appendLine("Complex Sentence to Analyze: \"$cleanSentence\"")
                if (bookTitle.isNotBlank()) {
                    appendLine("Source Book: $bookTitle")
                }
            }

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.3)
            }

            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Erro na API OpenAI (${response.code}): $errBody"))
                }

                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                val choices = rootJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(Exception("Resposta vazia da OpenAI."))
                }

                val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                val json = JSONObject(content)

                val tokensArr = json.optJSONArray("tokens") ?: JSONArray()
                val rulesArr = json.optJSONArray("grammarRules") ?: JSONArray()

                val savedAnalysis = com.example.data.model.SavedSentenceAnalysis(
                    bookId = bookId,
                    bookTitle = bookTitle,
                    pageNumber = pageNumber,
                    languageCode = language.code,
                    sentenceText = cleanSentence,
                    transliteration = json.optString("transliteration", ""),
                    translationPt = json.optString("translationPt", ""),
                    syntacticTreeSummary = json.optString("syntacticTreeSummary", ""),
                    complexityLevel = json.optString("complexityLevel", "C1"),
                    tokensJson = tokensArr.toString(),
                    grammarRulesJson = rulesArr.toString(),
                    c2MasteryTip = json.optString("c2MasteryTip", ""),
                    practiceChallenge = json.optString("practiceChallenge", ""),
                    createdAt = System.currentTimeMillis()
                )

                Result.success(savedAnalysis)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar sintaxe e gramática da frase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extrai ou injeta Verbos e Phrasal Verbs de uma página de livro ou de uma lista separada por vírgula.
     * Identifica a correta preposição usada, definição, significado em Português e gera EXATAMENTE 16 tempos
     * verbais, onde CADA exemplo utiliza uma Question Word obrigatória combinada diretamente com o contexto/collocation.
     */
    suspend fun extractVerbsAndPhrasalVerbs(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookId: Int,
        pageNumber: Int,
        bookTitle: String,
        sourceText: String,
        isCommaSeparatedList: Boolean = false,
        languageCode: String = "en"
    ): Result<List<com.example.data.model.VerbPhrasalVerbStudyTerm>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val healedSource = com.example.util.TextFormatUtils.healBrokenWordsAndHyphens(sourceText)

        // Se a chave estiver em branco, executa imediatamente a extração com o motor offline
        if (cleanKey.isBlank()) {
            val offlineList = com.example.data.offline.OfflineVerbPhrasalVerbEngine.extractVerbsAndPhrasalVerbs(
                bookId = bookId,
                pageNumber = pageNumber,
                bookTitle = bookTitle,
                sourceText = healedSource,
                languageCode = languageCode
            )
            return@withContext Result.success(offlineList)
        }
        if (healedSource.isBlank()) {
            return@withContext Result.failure(Exception("O texto ou lista fornecida está vazia."))
        }

        try {
            val systemPrompt = """
                Você é um Mestre Linguista, Lexicógrafo e Especialista em Ensino de Verbos, Phrasal Verbs e Preposições de Elite.

                SUA MISSÃO:
                1. REPARAR E CONSERTAR PALAVRAS QUEBRADAS: Corrija quaisquer palavras fragmentadas por hífens de final de linha ou falhas de OCR/PDF (ex: 'ob- jective' -> 'objective', 'look- ing' -> 'looking', 'eat- ing' -> 'eating', 'get- across' -> 'get across').
                2. ESCANEAR E EXTRAIR TODOS OS VERBOS E PHRASAL VERBS: Analise minuciosamente todo o texto da página ou lista e extraia CADA verbo e phrasal verb relevante presente.
                3. ESTRUTURAÇÃO OBRIGATÓRIA: Para cada item extraído, indique a correta preposição usada, a tradução fiel em português e gere RIGOROSAMENTE 16 TEMPOS VERBAIS DE EXEMPLOS, ONDE CADA UM DOS 16 EXEMPLOS É UMA PERGUNTA QUE EMPREGA UMA QUESTION WORD (Why, Where, When, What, Who, Whom, Which, Whose, How, How long, How much, By when, etc.) combinada diretamente com colocações autênticas (collocations).

                REQUISITOS ESTRUTURAIS PARA CADA ITEM:
                1. "verbOrPhrase": O verbo ou phrasal verb (ex: "eat", "get by", "get across", "look for", "give up", "break down", "achieve", "depend on", "run out of", "carry on", "turn down").
                2. "isPhrasalVerb": true se for phrasal verb ou verbo preposicionado, false se for verbo simples/regular/irregular.
                3. "prepositionUsed": A correta preposição usada (ex: "for", "up", "down", "on", "into", "out of", "after", "off", "with", "about"). Se não usar preposição, deixe vazio "".
                4. "baseVerb": O verbo base (ex: "eat", "get", "look", "give", "break", "achieve", "depend").
                5. "grammaticalCategory": "Phrasal Verb", "Verbo Preposicionado", "Verbo Transitivo" ou "Verbo Intransitivo".
                6. "definitionTargetLang": Definição clara no idioma original.
                7. "portugueseMeaning": Tradução e significado detalhado em PORTUGUÊS (OBRIGATÓRIO).
                   ATENÇÃO CRÍTICA PARA PHRASAL VERBS: O significado NÃO deve ser literal pelo verbo raiz; deve ser o significado IDIOMÁTICO EXATO e autêntico em português (ex: "take off" = decolar / tirar a roupa / sair correndo; "get by" = virar-se / sobreviver; "give up" = desistir; "run out of" = ficar sem / esgotar-se; "look up to" = admirar). Os 16 exemplos devem empregar o phrasal verb estritamente de acordo com esse significado autêntico.
                8. "ipaTranscription": Transcrição fonética no Alfabeto Fonético Internacional (ex: "/iːt/", "/ɡɛt baɪ/").
                9. "examples": ARRAY COM EXATAMENTE 16 EXEMPLOS cobrindo os 16 tempos verbais.
                   REGRA INEGOCIÁVEL DOS EXEMPLOS:
                   - TODOS OS 16 EXEMPLOS DEVEM CONTER UMA QUESTION WORD (Why, Where, When, What, Who, Which, How, How long, How much, How often, By when, etc.).
                   - As frases devem usar palavras que combinam diretamente com o verbo/phrasal verb (collocations autênticas).
                   - Cada exemplo DEVE ter a tradução fiel e precisa para o PORTUGUÊS.
                   - Ordem dos 16 tempos verbais:
                     1. Present Simple
                     2. Present Continuous
                     3. Present Perfect
                     4. Present Perfect Continuous
                     5. Past Simple
                     6. Past Continuous
                     7. Past Perfect
                     8. Past Perfect Continuous
                     9. Future Simple (will)
                     10. Future Continuous
                     11. Future Perfect
                     12. Future Perfect Continuous
                     13. Conditional Simple (would)
                     14. Conditional Continuous
                     15. Conditional Perfect (would have)
                     16. Modal Perfect / Subjunctive / Imperative (can/must/should/ought to)

                Para CADA um dos 16 exemplos no array "examples", retorne:
                - "tenseNumber": número de 1 a 16
                - "tenseName": nome do tempo verbal
                - "questionWord": a Question Word utilizada
                - "englishSentence": a pergunta completa no idioma estudado
                - "portugueseSentence": a tradução completa em português
                - "collocationNotes": colocação usada
                - "ipaTranscription": IPA da pergunta

                FORMATO DE SAÍDA:
                Retorne ESTRITAMENTE um array JSON de objetos válidos, sem markdown.
            """.trimIndent()

            val userContent = if (isCommaSeparatedList) {
                """
                    Livro / Coleção: $bookTitle
                    Página de Referência: $pageNumber

                    LISTA DE VERBOS E PHRASAL VERBS SEPARADOS POR VÍRGULA:
                    $healedSource

                    Conserte palavras quebradas e processe CADA verbo e phrasal verb da lista.
                    ATENÇÃO OBRIGATÓRIA: Para cada phrasal verb (ex: get by, run out of, take off, look for, bring up, put off, etc.), traga o significado IDIOMÁTICO VERDADEIRO e autêntico em português (não a tradução literal da raiz) e formule os 16 tempos verbais com Question Words perfeitamente alinhados a esse significado. Retorne em JSON.
                """.trimIndent()
            } else {
                """
                    Livro: $bookTitle
                    Página: $pageNumber

                    --- TEXTO DA PÁGINA DO LIVRO / DICIONÁRIO ---
                    $healedSource
                    --- FIM DO TEXTO ---

                    Vasculhe todo o texto acima, conserte quaisquer palavras quebradas por hífens ou falhas de OCR, encontre e extraia todos os verbos e phrasal verbs presentes. Indique a correta preposição usada em cada um, o significado em português e gere para cada um os 16 exemplos em tempos verbais com Question Words. Retorne em JSON.
                """.trimIndent()
            }

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            }

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.2)
                put("max_tokens", 4096)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val fallback = com.example.data.offline.OfflineVerbPhrasalVerbEngine.extractVerbsAndPhrasalVerbs(
                        bookId = bookId,
                        pageNumber = pageNumber,
                        bookTitle = bookTitle,
                        sourceText = healedSource,
                        languageCode = languageCode
                    )
                    if (fallback.isNotEmpty()) {
                        return@withContext Result.success(fallback)
                    }
                    val errBody = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Falha na API OpenAI: $errBody"))
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    val fallback = com.example.data.offline.OfflineVerbPhrasalVerbEngine.extractVerbsAndPhrasalVerbs(
                        bookId = bookId,
                        pageNumber = pageNumber,
                        bookTitle = bookTitle,
                        sourceText = healedSource,
                        languageCode = languageCode
                    )
                    return@withContext Result.success(fallback)
                }

                val content = choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim()
                val parsedList = parseVerbPhrasalTermsJson(content, bookId, pageNumber, bookTitle, languageCode)

                if (parsedList.isEmpty()) {
                    val fallback = com.example.data.offline.OfflineVerbPhrasalVerbEngine.extractVerbsAndPhrasalVerbs(
                        bookId = bookId,
                        pageNumber = pageNumber,
                        bookTitle = bookTitle,
                        sourceText = healedSource,
                        languageCode = languageCode
                    )
                    return@withContext Result.success(fallback)
                }

                // ORDENA RIGOROSAMENTE POR ORDEM ALFABÉTICA (A -> Z)
                Result.success(parsedList.sortedBy { it.verbOrPhrase.lowercase(Locale.getDefault()).trim() })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair verbos e phrasal verbs, recorrendo ao motor offline: ${e.message}", e)
            val fallback = com.example.data.offline.OfflineVerbPhrasalVerbEngine.extractVerbsAndPhrasalVerbs(
                bookId = bookId,
                pageNumber = pageNumber,
                bookTitle = bookTitle,
                sourceText = healedSource,
                languageCode = languageCode
            )
            Result.success(fallback)
        }
    }

    private fun parseVerbPhrasalTermsJson(
        rawContent: String,
        bookId: Int,
        pageNumber: Int,
        bookTitle: String,
        languageCode: String
    ): List<com.example.data.model.VerbPhrasalVerbStudyTerm> {
        val cleanJson = rawContent
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

        val terms = mutableListOf<com.example.data.model.VerbPhrasalVerbStudyTerm>()

        try {
            val startIndex = cleanJson.indexOf('[')
            val endIndex = cleanJson.lastIndexOf(']')
            var jsonArrayString = if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleanJson.substring(startIndex, endIndex + 1)
            } else if (startIndex != -1) {
                // Tenta reparar array JSON truncado
                cleanJson.substring(startIndex).trimEnd().let { if (!it.endsWith("]")) "$it}]" else it }
            } else {
                cleanJson
            }

            val array = JSONArray(jsonArrayString)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val verbOrPhrase = obj.optString("verbOrPhrase", "").trim()
                if (verbOrPhrase.isBlank()) continue

                val isPhrasal = obj.optBoolean("isPhrasalVerb", verbOrPhrase.contains(" "))
                val prep = obj.optString("prepositionUsed", "").trim()
                val base = obj.optString("baseVerb", verbOrPhrase.substringBefore(" ")).trim()
                val category = obj.optString("grammaticalCategory", if (isPhrasal) "Phrasal Verb" else "Verbo").trim()
                val defTarget = obj.optString("definitionTargetLang", "").trim()
                val ptMeaning = obj.optString("portugueseMeaning", "").trim()
                val ipa = obj.optString("ipaTranscription", "").trim()
                val examplesArr = obj.optJSONArray("examples") ?: JSONArray()

                // Se houver menos de 16 exemplos, complementa para garantir rigorosamente 16 tempos verbais
                val finalExamplesJson = if (examplesArr.length() < 16) {
                    val entry = com.example.data.offline.OfflineVerbPhrasalVerbEngine.VerbDictionaryEntry(
                        verbOrPhrase = verbOrPhrase,
                        isPhrasal = isPhrasal,
                        prep = prep,
                        base = base,
                        ptMeaning = ptMeaning.ifBlank { "Verbo de ação." },
                        defEn = defTarget,
                        ipa = ipa,
                        languageCode = languageCode
                    )
                    val full16 = com.example.data.offline.OfflineVerbPhrasalVerbEngine.generateSixteenTenseExamples(entry)
                    com.example.data.model.VerbPhrasalVerbStudyTerm.buildJsonFromExamples(full16)
                } else {
                    examplesArr.toString()
                }

                val term = com.example.data.model.VerbPhrasalVerbStudyTerm(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    bookTitle = bookTitle,
                    verbOrPhrase = verbOrPhrase,
                    isPhrasalVerb = isPhrasal,
                    prepositionUsed = prep,
                    baseVerb = base,
                    grammaticalCategory = category,
                    languageCode = languageCode,
                    definitionTargetLang = defTarget,
                    portugueseMeaning = ptMeaning,
                    ipaTranscription = ipa,
                    examplesJson = finalExamplesJson,
                    createdAt = System.currentTimeMillis()
                )
                terms.add(term)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parsear JSON de verbos e phrasal verbs: ${e.message}", e)
        }

        // Ordena alfabeticamente
        return terms.sortedBy { it.verbOrPhrase.lowercase(Locale.getDefault()).trim() }
    }

    /**
     * Contesta e corrige o significado de um verbo ou phrasal verb com a API da OpenAI.
     * Reavalia a preposição, o significado exato em português e regenera todos os 16 tempos
     * verbais com Question Words e collocations condizentes com o significado corrigido.
     */
    suspend fun recheckAndFixVerbPhrasalVerbMeaning(
        apiKey: String,
        model: String = "gpt-4o-mini",
        term: com.example.data.model.VerbPhrasalVerbStudyTerm,
        userCritiqueOrMeaning: String = "",
        languageCode: String = "en"
    ): Result<com.example.data.model.VerbPhrasalVerbStudyTerm> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            // Suporte Offline / Local Imediato para a função Corrija
            val fallbackMeaning = if (userCritiqueOrMeaning.isNotBlank()) {
                userCritiqueOrMeaning.trim()
            } else {
                com.example.data.offline.OfflineReadingEngine.generateMeaningForWord(term.verbOrPhrase)
            }
            val updated = term.copy(
                portugueseMeaning = fallbackMeaning
            )
            return@withContext Result.success(updated)
        }

        try {
            val systemPrompt = """
                Você é um Mestre Linguista, Lexicógrafo e Especialista em Ensino de Vocabulário, Verbos e Significados no Mundo Real.

                O usuário está CONTESTANDO e CORRIGINDO o significado trazido anteriormente para o seguinte termo/verbo:
                "${term.verbOrPhrase}"
                
                Significado contestado:
                "${term.portugueseMeaning}"
                
                Observação / correção enviada pelo usuário (se houver):
                "${if (userCritiqueOrMeaning.isNotBlank()) userCritiqueOrMeaning else "(Nenhuma observação extra - encontre o significado verdadeiro, concreto e real no mundo)"}"

                SUA MISSÃO OBRIGATÓRIA:
                1. REAVALIAR E CONSERTAR O SIGNIFICADO: Forneça o VERDADEIRO e MAIS PRECISO significado no mundo real ('portugueseMeaning'). 
                   ATENÇÃO: O significado DEVE vir com a explicação do que a coisa ou ação É na realidade concreta (por exemplo: se o termo for 'Pássaro', deve vir 'Animal vertebrado com penas e asas que voa, come sementes e grãos e vive livre na natureza'; se for um verbo ou phrasal verb, o que a ação concretamente faz no mundo).
                   REGRA PROIBITIVA: NUNCA use meta-frases genéricas como 'conceito e vocábulo empregado no discurso'.
                2. REVISAR A PREPOSIÇÃO: Identifique a preposição correta em 'prepositionUsed'.
                3. REGENERAR RIGOROSAMENTE OS 16 EXEMPLOS: Substitua e gere 16 NOVOS exemplos completos (um para cada um dos 16 tempos verbais), TODOS com Question Words autênticas (Why, Where, When, What, Who, How, How long, etc.), perfeitamente alinhados ao NOVO significado corrigido e com tradução fiel em português.

                Retorne ESTRITAMENTE um ÚNICO objeto JSON (sem markdown, sem colchetes de array exterior) no formato:
                {
                  "verbOrPhrase": "${term.verbOrPhrase}",
                  "isPhrasalVerb": ${term.isPhrasalVerb},
                  "prepositionUsed": "preposição correta ou vazia",
                  "baseVerb": "verbo raiz",
                  "grammaticalCategory": "Phrasal Verb ou Verbo",
                  "definitionTargetLang": "Definição no idioma estudado",
                  "portugueseMeaning": "Significado corrigido verdadeiro em português",
                  "ipaTranscription": "IPA",
                  "examples": [
                    {
                      "tenseNumber": 1,
                      "tenseName": "Present Simple",
                      "questionWord": "Why",
                      "englishSentence": "...",
                      "portugueseSentence": "...",
                      "collocationNotes": "...",
                      "ipaTranscription": "..."
                    }
                  ]
                }
            """.trimIndent()

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Corrija o significado e regenere os 16 tempos verbais com Question Words para: ${term.verbOrPhrase}")
                })
            }

            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", messages)
                put("temperature", 0.2)
                put("max_tokens", 4096)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Erro na API OpenAI: $err"))
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "") ?: ""

                val cleanJson = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val obj = JSONObject(cleanJson)

                val verbOrPhrase = obj.optString("verbOrPhrase", term.verbOrPhrase).trim()
                val isPhrasal = obj.optBoolean("isPhrasalVerb", term.isPhrasalVerb)
                val prep = obj.optString("prepositionUsed", term.prepositionUsed).trim()
                val base = obj.optString("baseVerb", term.baseVerb).trim()
                val category = obj.optString("grammaticalCategory", term.grammaticalCategory).trim()
                val defTarget = obj.optString("definitionTargetLang", term.definitionTargetLang).trim()
                val ptMeaning = obj.optString("portugueseMeaning", term.portugueseMeaning).trim()
                val ipa = obj.optString("ipaTranscription", term.ipaTranscription).trim()
                val examplesArr = obj.optJSONArray("examples") ?: JSONArray()

                val finalExamplesJson = if (examplesArr.length() >= 16) {
                    examplesArr.toString()
                } else {
                    term.examplesJson
                }

                val updatedTerm = term.copy(
                    verbOrPhrase = verbOrPhrase,
                    isPhrasalVerb = isPhrasal,
                    prepositionUsed = prep,
                    baseVerb = base,
                    grammaticalCategory = category,
                    definitionTargetLang = defTarget,
                    portugueseMeaning = ptMeaning,
                    ipaTranscription = ipa,
                    examplesJson = finalExamplesJson
                )
                Result.success(updatedTerm)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao contestar e corrigir verbo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Restaura e traduz o texto completo da página preservando 100% das palavras,
     * orações, frases, parágrafos, fórmulas e símbolos científicos no idioma escolhido.
     */
    suspend fun restoreAndTranslatePageText(
        apiKey: String,
        model: String = "gpt-4o-mini",
        bookTitle: String,
        pageNumber: Int,
        rawText: String,
        pageBitmap: Bitmap? = null,
        targetLanguageCode: String,
        targetLanguageName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não configurada."))
        }
        val cleanInput = rawText.trim()
        if (cleanInput.isBlank() && pageBitmap == null) {
            return@withContext Result.failure(Exception("O conteúdo da página está vazio."))
        }

        try {
            val isPortuguese = targetLanguageCode.lowercase().trim().let { it == "pt" || it == "portuguese" }
            val systemPrompt = if (isPortuguese) {
                """
                Você é um restaurador textual de máxima fidelidade e especialista em transcrição exata de livros digitalizados (OCR), livros acadêmicos e livros didáticos de Ciências Exatas e Naturais.
                REGRA ABSOLUTA:
                NÃO MUDE PRATICAMENTE NADA DO TEXTO. O texto DEVE continuar rigorosamente idêntico ao que o autor escreveu:
                - NÃO resuma, não explique e não parafraseie.
                - Conserte pontuações, caracteres OCR corrompidos, hifenizações e quebras de linha indevidas.
                - Preserve fórmulas, reações químicas, equações e todos os termos científicos.
                Retorne EXCLUSIVAMENTE o texto restaurado na sua integridade literal exata, sem introdução e SEM markdown (```).
                """.trimIndent()
            } else {
                """
                Você é um restaurador textual e tradutor acadêmico e literário de elite.
                SUA MISSÃO INEGOCIÁVEL:
                Restaurar e traduzir INTEGRALMENTE cada frase, oração e parágrafo da página para o idioma: $targetLanguageName ($targetLanguageCode).
                
                DIRETRIZ FUNDAMENTAL DO USUÁRIO:
                "Tem que ter todas as palavrinhas que tinha no português ali, todas as frases, orações, certinho, bonitinho, do jeito que estava, só que em outro idioma."
                
                REGRAS ESTRITAS:
                1. NÃO resuma, não corte e não simplifique nada.
                2. Traduza frase por frase, oração por oração, mantendo exatamente a mesma estrutura sequencial e a mesma divisão de parágrafos.
                3. Se houver fórmulas matemáticas, termos científicos, equações ou símbolos lógicos, preserve-os intactos no lugar exato onde ocorrem.
                4. Corrija quaisquer palavras partidas ou quebras indevidas do OCR original enquanto traduz.
                5. Retorne EXCLUSIVAMENTE o texto restaurado e traduzido para $targetLanguageName, sem comentários, sem avisos, sem saudações e SEM blocos markdown (```).
                """.trimIndent()
            }

            val textPrompt = """
                Livro: "$bookTitle"
                Página: $pageNumber
                Idioma de Destino: $targetLanguageName ($targetLanguageCode)

                TEXTO ORIGINAL DA PÁGINA:
                ${cleanInput.ifBlank { "(Página baseada na imagem visual fornecida)" }}

                Restaure e entregue o texto completo traduzido em $targetLanguageName com fidelidade total a todas as frases e orações originais.
            """.trimIndent()

            val targetModel = if (pageBitmap != null && model.contains("3.5")) "gpt-4o-mini" else model.ifBlank { "gpt-4o-mini" }

            val userMessageObj = JSONObject().apply {
                put("role", "user")
                if (pageBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    val maxDim = 1280
                    val scaledBitmap = if (pageBitmap.width > maxDim || pageBitmap.height > maxDim) {
                        val factor = maxDim.toFloat() / maxOf(pageBitmap.width, pageBitmap.height)
                        Bitmap.createScaledBitmap(pageBitmap, (pageBitmap.width * factor).toInt(), (pageBitmap.height * factor).toInt(), true)
                    } else {
                        pageBitmap
                    }
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val base64Img = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$base64Img"

                    val contentArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", textPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", dataUrl)
                                put("detail", "high")
                            })
                        })
                    }
                    put("content", contentArr)
                } else {
                    put("content", textPrompt)
                }
            }

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(userMessageObj)
            }

            val payload = JSONObject().apply {
                put("model", targetModel)
                put("messages", messages)
                put("temperature", 0.1)
                put("max_tokens", 4096)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Erro na OpenAI: $err"))
                }
                val body = response.body?.string() ?: ""
                val respJson = JSONObject(body)
                val choices = respJson.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "") ?: ""
                val cleaned = content.trim().removePrefix("```text").removePrefix("```").removeSuffix("```").trim()
                Result.success(cleaned)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro em restoreAndTranslatePageText: ${e.message}", e)
            Result.failure(e)
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
