package com.example.data.translator

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.StudyLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object TermsNotesTranslator {

    private val cache = ConcurrentHashMap<String, Pair<String, String>>()

    /**
     * Resolve tradução para um termo de dicionário da página
     * Retorna Pair(termoTraduzido, definicaoTraduzida)
     */
    suspend fun translateDictionaryTerm(
        term: PageDictionaryTerm,
        targetLanguage: StudyLanguage,
        customApiKey: String? = null,
        context: Context? = null
    ): Pair<String, String> {
        val cacheKey = "term_${term.id}_${term.term}_${targetLanguage.code}"
        cache[cacheKey]?.let { return it }

        // 1. Idioma Original (Português)
        if (targetLanguage == StudyLanguage.PORTUGUESE) {
            val res = term.term to term.originalDefinition
            cache[cacheKey] = res
            return res
        }

        // 2. Traduções já salvas no modelo Room (para EN, ES, FR)
        when (targetLanguage) {
            StudyLanguage.ENGLISH -> {
                if (term.englishDefinition.isNotBlank()) {
                    val res = (term.englishTerm.ifBlank { term.term }) to term.englishDefinition
                    cache[cacheKey] = res
                    return res
                }
            }
            StudyLanguage.SPANISH -> {
                if (term.spanishDefinition.isNotBlank()) {
                    val res = (term.spanishTerm.ifBlank { term.term }) to term.spanishDefinition
                    cache[cacheKey] = res
                    return res
                }
            }
            StudyLanguage.FRENCH -> {
                if (term.frenchDefinition.isNotBlank()) {
                    val res = (term.frenchTerm.ifBlank { term.term }) to term.frenchDefinition
                    cache[cacheKey] = res
                    return res
                }
            }
            else -> {}
        }

        // 3. Traduzir prioritariamente com a API da OpenAI (e fallbacks para todos os idiomas)
        val translated = translateTextPair(
            termText = term.term,
            definitionText = term.originalDefinition,
            targetLanguage = targetLanguage,
            customApiKey = customApiKey,
            context = context
        )

        cache[cacheKey] = translated
        return translated
    }

    /**
     * Resolve tradução para anotação ou síntese da página
     */
    suspend fun translateNoteText(
        noteTitle: String,
        noteContent: String,
        targetLanguage: StudyLanguage,
        customApiKey: String? = null,
        context: Context? = null
    ): Pair<String, String> {
        val cacheKey = "note_${noteTitle.hashCode()}_${noteContent.hashCode()}_${targetLanguage.code}"
        cache[cacheKey]?.let { return it }

        if (targetLanguage == StudyLanguage.PORTUGUESE) {
            val res = noteTitle to noteContent
            cache[cacheKey] = res
            return res
        }

        val translated = translateTextPair(
            termText = noteTitle,
            definitionText = noteContent,
            targetLanguage = targetLanguage,
            customApiKey = customApiKey,
            context = context
        )
        cache[cacheKey] = translated
        return translated
    }

    private val textBlockCache = ConcurrentHashMap<String, String>()

    /**
     * Resolve tradução para um bloco de texto completo da página via OpenAI API (com fallback neural multilíngue)
     */
    suspend fun translateTextBlock(
        text: String,
        targetLanguage: StudyLanguage,
        customApiKey: String? = null,
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isBlank() || targetLanguage == StudyLanguage.PORTUGUESE) {
            return@withContext clean
        }

        val cacheKey = "block_${clean.hashCode()}_${targetLanguage.code}"
        textBlockCache[cacheKey]?.let { return@withContext it }

        // 1. PRIORIDADE MÁXIMA MANDATÓRIA: API da OpenAI (gpt-4o-mini)
        val openAiKey = resolveOpenAiApiKey(customApiKey, context)
        if (openAiKey.isNotBlank()) {
            val openAiResult = callOpenAiTextBlockTranslation(clean, targetLanguage, openAiKey)
            if (!openAiResult.isNullOrBlank()) {
                textBlockCache[cacheKey] = openAiResult
                return@withContext openAiResult
            }
        }

        // 2. Fallback MyMemory Neural Machine Translation
        try {
            val myMemoryResult = callMyMemoryTranslation(clean, targetLanguage)
            if (!myMemoryResult.isNullOrBlank()) {
                val unescaped = unescapeHtml(myMemoryResult).trim()
                if (unescaped.isNotBlank()) {
                    textBlockCache[cacheKey] = unescaped
                    return@withContext unescaped
                }
            }
        } catch (_: Exception) {}

        // 3. Retorno padrão limpo
        clean
    }

    fun getQuickTextBlockTranslation(
        text: String,
        targetLanguage: StudyLanguage
    ): String {
        if (targetLanguage == StudyLanguage.PORTUGUESE || text.isBlank()) {
            return text
        }
        val cacheKey = "block_${text.trim().hashCode()}_${targetLanguage.code}"
        return textBlockCache[cacheKey] ?: text
    }

    /**
     * Retorna tradução instantânea em 0ms (síncrona) para atualização imediata na UI
     * enquanto a requisição neural em background processa.
     */
    fun getQuickTranslation(
        termText: String,
        definitionText: String,
        targetLanguage: StudyLanguage
    ): Pair<String, String> {
        if (targetLanguage == StudyLanguage.PORTUGUESE) {
            return termText to definitionText
        }
        val cacheKey = "quick_${termText.hashCode()}_${definitionText.hashCode()}_${targetLanguage.code}"
        cache[cacheKey]?.let { return it }

        val res = getLocalMultilingualTranslation(termText, definitionText, targetLanguage)
        cache[cacheKey] = res
        return res
    }

    val termNameCache = ConcurrentHashMap<String, String>()

    /**
     * Retorna o nome do termo traduzido para qualquer idioma com precisão instantânea.
     */
    fun getMultiLanguageTermName(termText: String, lang: StudyLanguage, optId: String = ""): String {
        val clean = termText.trim()
        val lower = clean.lowercase()
        if (clean.isBlank()) return clean
        if (optId == "pt_br" || (optId.isEmpty() && lang == StudyLanguage.PORTUGUESE)) return clean

        val cacheKey = "${clean}_${lang.code}_$optId"
        termNameCache[cacheKey]?.let { return it }

        // Termo conflito e derivados com todas as traduções solicitadas
        if (lower.contains("conflito")) {
            val res = when {
                optId == "pt_pt" -> "Conflito"
                optId == "en_us" || optId == "en_gb" -> "Conflict"
                lang == StudyLanguage.PORTUGUESE -> "Conflito"
                lang == StudyLanguage.ENGLISH -> "Conflict"
                lang == StudyLanguage.SPANISH -> "Conflicto"
                lang == StudyLanguage.FRENCH -> "Conflit"
                lang == StudyLanguage.GERMAN -> "Konflikt"
                lang == StudyLanguage.ITALIAN -> "Conflitto"
                lang == StudyLanguage.RUSSIAN -> "Конфликт"
                lang == StudyLanguage.CHINESE -> "冲突"
                lang == StudyLanguage.JAPANESE -> "対立"
                lang == StudyLanguage.KOREAN -> "갈등"
                lang == StudyLanguage.ARABIC -> "صراع"
                lang == StudyLanguage.TURKISH -> "Çatışma"
                lang == StudyLanguage.DUTCH -> "Conflict"
                lang == StudyLanguage.SWEDISH -> "Konflikt"
                lang == StudyLanguage.POLISH -> "Konflikt"
                lang == StudyLanguage.GREEK -> "Σύγκρουση"
                lang == StudyLanguage.HEBREW -> "סכסוך"
                lang == StudyLanguage.HINDI -> "संघर्ष"
                lang == StudyLanguage.UKRAINIAN -> "Конфлікт"
                lang == StudyLanguage.ROMANIAN -> "Conflict"
                lang == StudyLanguage.INDONESIAN -> "Konflik"
                lang == StudyLanguage.VIETNAMESE -> "Xung đột"
                lang == StudyLanguage.NORWEGIAN -> "Konflikt"
                lang == StudyLanguage.DANISH -> "Konflikt"
                lang == StudyLanguage.FINNISH -> "Konflikti"
                lang == StudyLanguage.CZECH -> "Konflikt"
                else -> "Konflikt"
            }
            termNameCache[cacheKey] = res
            return res
        }

        val academic = getAcademicLexiconTerm(lower, lang, optId)
        if (academic != null) {
            termNameCache[cacheKey] = academic
            return academic
        }

        val quick = getQuickTranslation(clean, "", lang).first
        if (quick.isNotBlank() && quick != clean) {
            termNameCache[cacheKey] = quick
            return quick
        }

        val gen = translateGeneralLexicon(clean, lang.code)
        termNameCache[cacheKey] = gen
        return gen
    }

    private fun getAcademicLexiconTerm(lower: String, lang: StudyLanguage, optId: String): String? {
        return when {
            lower.contains("preconceito") -> when (lang) {
                StudyLanguage.PORTUGUESE -> "Preconceito"
                StudyLanguage.ENGLISH -> "Prejudice"
                StudyLanguage.SPANISH -> "Prejuicio"
                StudyLanguage.FRENCH -> "Préjugé"
                StudyLanguage.GERMAN -> "Vorurteil"
                StudyLanguage.ITALIAN -> "Pregiudizio"
                StudyLanguage.RUSSIAN -> "Предвзятость"
                StudyLanguage.CHINESE -> "偏见"
                StudyLanguage.JAPANESE -> "偏見"
                StudyLanguage.KOREAN -> "편견"
                StudyLanguage.ARABIC -> "تحيز"
                StudyLanguage.TURKISH -> "Önyargı"
                StudyLanguage.DUTCH -> "Vooroordeel"
                StudyLanguage.SWEDISH -> "Fördom"
                StudyLanguage.POLISH -> "Uprzedzenie"
                StudyLanguage.GREEK -> "Προκατάληψη"
                StudyLanguage.HEBREW -> "דעה קדומה"
                StudyLanguage.HINDI -> "पूर्वाग्रह"
                StudyLanguage.UKRAINIAN -> "Упередження"
                StudyLanguage.ROMANIAN -> "Prejudecată"
                StudyLanguage.INDONESIAN -> "Prasangka"
                StudyLanguage.VIETNAMESE -> "Định kiến"
                StudyLanguage.NORWEGIAN -> "Fordom"
                StudyLanguage.DANISH -> "Fordom"
                StudyLanguage.FINNISH -> "Ennakkoluulo"
                StudyLanguage.CZECH -> "Předsudek"
            }
            lower.contains("discriminação") -> when (lang) {
                StudyLanguage.PORTUGUESE -> "Discriminação"
                StudyLanguage.ENGLISH -> "Discrimination"
                StudyLanguage.SPANISH -> "Discriminación"
                StudyLanguage.FRENCH -> "Discrimination"
                StudyLanguage.GERMAN -> "Diskriminierung"
                StudyLanguage.ITALIAN -> "Discriminazione"
                StudyLanguage.RUSSIAN -> "Дискриминация"
                StudyLanguage.CHINESE -> "歧视"
                StudyLanguage.JAPANESE -> "差別"
                StudyLanguage.KOREAN -> "차별"
                StudyLanguage.ARABIC -> "تمييز"
                StudyLanguage.TURKISH -> "Ayrımcılık"
                StudyLanguage.DUTCH -> "Discriminatie"
                StudyLanguage.SWEDISH -> "Diskriminering"
                StudyLanguage.POLISH -> "Dyskryminacja"
                StudyLanguage.GREEK -> "Διάκριση"
                StudyLanguage.HEBREW -> "אפליה"
                StudyLanguage.HINDI -> "भेदभाव"
                StudyLanguage.UKRAINIAN -> "Дискримінація"
                StudyLanguage.ROMANIAN -> "Discriminare"
                StudyLanguage.INDONESIAN -> "Diskriminasi"
                StudyLanguage.VIETNAMESE -> "Phân biệt đối xử"
                StudyLanguage.NORWEGIAN -> "Diskriminering"
                StudyLanguage.DANISH -> "Diskrimination"
                StudyLanguage.FINNISH -> "Syrjintä"
                StudyLanguage.CZECH -> "Diskriminace"
            }
            lower.contains("constituição") || lower.contains("constitucional") -> when (lang) {
                StudyLanguage.PORTUGUESE -> "Constituição"
                StudyLanguage.ENGLISH -> "Constitution"
                StudyLanguage.SPANISH -> "Constitución"
                StudyLanguage.FRENCH -> "Constitution"
                StudyLanguage.GERMAN -> "Verfassung"
                StudyLanguage.ITALIAN -> "Costituzione"
                StudyLanguage.RUSSIAN -> "Конституция"
                StudyLanguage.CHINESE -> "宪法"
                StudyLanguage.JAPANESE -> "憲法"
                StudyLanguage.KOREAN -> "헌법"
                StudyLanguage.ARABIC -> "دستور"
                StudyLanguage.TURKISH -> "Anayasa"
                StudyLanguage.DUTCH -> "Grondwet"
                StudyLanguage.SWEDISH -> "Grundlag"
                StudyLanguage.POLISH -> "Konstytucja"
                StudyLanguage.GREEK -> "Σύνταγμα"
                StudyLanguage.HEBREW -> "חוקה"
                StudyLanguage.HINDI -> "संविधान"
                StudyLanguage.UKRAINIAN -> "Конституція"
                StudyLanguage.ROMANIAN -> "Constituție"
                StudyLanguage.INDONESIAN -> "Konstitusi"
                StudyLanguage.VIETNAMESE -> "Hiến pháp"
                StudyLanguage.NORWEGIAN -> "Grunnlov"
                StudyLanguage.DANISH -> "Grundlov"
                StudyLanguage.FINNISH -> "Perustuslaki"
                StudyLanguage.CZECH -> "Ústava"
            }
            lower.contains("direito") -> when (lang) {
                StudyLanguage.PORTUGUESE -> "Direito"
                StudyLanguage.ENGLISH -> "Law / Right"
                StudyLanguage.SPANISH -> "Derecho"
                StudyLanguage.FRENCH -> "Droit"
                StudyLanguage.GERMAN -> "Recht"
                StudyLanguage.ITALIAN -> "Diritto"
                StudyLanguage.RUSSIAN -> "Право"
                StudyLanguage.CHINESE -> "法律"
                StudyLanguage.JAPANESE -> "法"
                StudyLanguage.KOREAN -> "법"
                StudyLanguage.ARABIC -> "قانون"
                StudyLanguage.TURKISH -> "Hukuk"
                StudyLanguage.DUTCH -> "Recht"
                StudyLanguage.SWEDISH -> "Rätt"
                StudyLanguage.POLISH -> "Prawo"
                StudyLanguage.GREEK -> "Δίκαιο"
                StudyLanguage.HEBREW -> "משפט"
                StudyLanguage.HINDI -> "कानून"
                StudyLanguage.UKRAINIAN -> "Право"
                StudyLanguage.ROMANIAN -> "Drept"
                StudyLanguage.INDONESIAN -> "Hukum"
                StudyLanguage.VIETNAMESE -> "Pháp luật"
                StudyLanguage.NORWEGIAN -> "Rett"
                StudyLanguage.DANISH -> "Ret"
                StudyLanguage.FINNISH -> "Oikeus"
                StudyLanguage.CZECH -> "Právo"
            }
            lower.contains("justiça") -> when (lang) {
                StudyLanguage.PORTUGUESE -> "Justiça"
                StudyLanguage.ENGLISH -> "Justice"
                StudyLanguage.SPANISH -> "Justicia"
                StudyLanguage.FRENCH -> "Justice"
                StudyLanguage.GERMAN -> "Gerechtigkeit"
                StudyLanguage.ITALIAN -> "Giustizia"
                StudyLanguage.RUSSIAN -> "Справедливость"
                StudyLanguage.CHINESE -> "正义"
                StudyLanguage.JAPANESE -> "正義"
                StudyLanguage.KOREAN -> "정의"
                StudyLanguage.ARABIC -> "عدالة"
                StudyLanguage.TURKISH -> "Adalet"
                StudyLanguage.DUTCH -> "Gerechtigheid"
                StudyLanguage.SWEDISH -> "Rättvisa"
                StudyLanguage.POLISH -> "Sprawiedliwość"
                StudyLanguage.GREEK -> "Δικαιοσύνη"
                StudyLanguage.HEBREW -> "צדק"
                StudyLanguage.HINDI -> "न्याय"
                StudyLanguage.UKRAINIAN -> "Справедливість"
                StudyLanguage.ROMANIAN -> "Justiție"
                StudyLanguage.INDONESIAN -> "Keadilan"
                StudyLanguage.VIETNAMESE -> "Công lý"
                StudyLanguage.NORWEGIAN -> "Rettferdighet"
                StudyLanguage.DANISH -> "Retfærdighed"
                StudyLanguage.FINNISH -> "Oikeudenmukaisuus"
                StudyLanguage.CZECH -> "Spravedlnost"
            }
            else -> null
        }
    }

    /**
     * Resolve chave da API OpenAI a partir de parâmetros, SharedPreferences ou BuildConfig
     */
    fun resolveOpenAiApiKey(customApiKey: String? = null, context: Context? = null): String {
        val custom = customApiKey?.trim()
        if (!custom.isNullOrBlank()) return custom

        if (context != null) {
            val saved = com.example.data.openai.OpenAiService.getSavedApiKey(context)
            if (saved.isNotBlank()) return saved
        }

        val buildKey = BuildConfig.OPENAI_API_KEY.trim()
        if (buildKey.isNotBlank()) return buildKey

        val envKey = (System.getenv("OPENAI_API_KEY") ?: "").trim()
        if (envKey.isNotBlank()) return envKey

        return ""
    }

    private suspend fun translateTextPair(
        termText: String,
        definitionText: String,
        targetLanguage: StudyLanguage,
        customApiKey: String?,
        context: Context? = null
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        if (targetLanguage == StudyLanguage.PORTUGUESE) {
            return@withContext termText to definitionText
        }

        // 1. PRIORIDADE MÁXIMA MANDATÓRIA: API da OpenAI (gpt-4o-mini) para todos os idiomas
        val openAiKey = resolveOpenAiApiKey(customApiKey, context)
        if (openAiKey.isNotBlank()) {
            val openAiResult = callOpenAiTranslation(termText, definitionText, targetLanguage, openAiKey)
            if (openAiResult != null) return@withContext openAiResult
        }

        // 2. Se OpenAI indisponível no momento, fallback para MyMemory Neural Machine Translation
        try {
            val myMemoryTerm = callMyMemoryTranslation(termText, targetLanguage)
            val myMemoryDef = if (definitionText.isNotBlank()) {
                callMyMemoryTranslation(definitionText, targetLanguage)
            } else {
                myMemoryTerm
            }

            if (!myMemoryTerm.isNullOrBlank() && !myMemoryDef.isNullOrBlank()) {
                val cleanTerm = unescapeHtml(myMemoryTerm).trim()
                val cleanDef = unescapeHtml(myMemoryDef).trim()
                if (cleanTerm.isNotBlank() && cleanDef.isNotBlank()) {
                    return@withContext cleanTerm to cleanDef
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback Gemini Flash se chave disponível
        val geminiKey = BuildConfig.GEMINI_API_KEY
        if (geminiKey.isNotBlank() && geminiKey != "MY_GEMINI_API_KEY") {
            val geminiResult = callGeminiTranslation(termText, definitionText, targetLanguage, geminiKey)
            if (geminiResult != null) return@withContext geminiResult
        }

        // 4. Fallback léxico robusto offline de alta fidelidade
        getLocalMultilingualTranslation(termText, definitionText, targetLanguage)
    }

    /**
     * Divide texto em pedaços seguros menores que maxChars respeitando limites da API
     */
    private fun splitIntoSafeChunks(text: String, maxChars: Int = 300): List<String> {
        val clean = text.trim()
        if (clean.length <= maxChars) return listOf(clean)

        val result = mutableListOf<String>()
        val sentenceRegex = Regex("(?<=[.!?])\\s+")
        val sentences = clean.split(sentenceRegex)

        val currentChunk = StringBuilder()
        for (sentence in sentences) {
            if (sentence.length > maxChars) {
                if (currentChunk.isNotEmpty()) {
                    result.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }
                val commaParts = sentence.split(Regex("(?<=[,;:])\\s+"))
                for (part in commaParts) {
                    if (part.length > maxChars) {
                        val words = part.split(Regex("\\s+"))
                        val wordChunk = StringBuilder()
                        for (w in words) {
                            if (wordChunk.length + w.length + 1 > maxChars) {
                                if (wordChunk.isNotEmpty()) result.add(wordChunk.toString().trim())
                                wordChunk.clear()
                            }
                            if (wordChunk.isNotEmpty()) wordChunk.append(" ")
                            wordChunk.append(w)
                        }
                        if (wordChunk.isNotEmpty()) result.add(wordChunk.toString().trim())
                    } else {
                        if (currentChunk.length + part.length + 1 > maxChars) {
                            result.add(currentChunk.toString().trim())
                            currentChunk.clear()
                        }
                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                        currentChunk.append(part)
                    }
                }
            } else {
                if (currentChunk.length + sentence.length + 1 > maxChars) {
                    result.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }
                if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                currentChunk.append(sentence)
            }
        }
        if (currentChunk.isNotEmpty()) {
            result.add(currentChunk.toString().trim())
        }
        return result.filter { it.isNotBlank() }
    }

    /**
     * Chamada à API MyMemory Neural Machine Translation com suporte a todos os idiomas
     */
    private fun callMyMemoryTranslation(text: String, targetLanguage: StudyLanguage): String? {
        val clean = text.trim()
        if (clean.isBlank()) return ""

        val langPairCode = when (targetLanguage) {
            StudyLanguage.PORTUGUESE -> return clean
            StudyLanguage.ENGLISH -> "en"
            StudyLanguage.SPANISH -> "es"
            StudyLanguage.FRENCH -> "fr"
            StudyLanguage.GERMAN -> "de"
            StudyLanguage.ITALIAN -> "it"
            StudyLanguage.RUSSIAN -> "ru"
            StudyLanguage.CHINESE -> "zh-CN"
            StudyLanguage.JAPANESE -> "ja"
            StudyLanguage.KOREAN -> "ko"
            StudyLanguage.ARABIC -> "ar"
            StudyLanguage.TURKISH -> "tr"
            StudyLanguage.DUTCH -> "nl"
            StudyLanguage.SWEDISH -> "sv"
            StudyLanguage.POLISH -> "pl"
            StudyLanguage.GREEK -> "el"
            StudyLanguage.HEBREW -> "he"
            StudyLanguage.HINDI -> "hi"
            StudyLanguage.UKRAINIAN -> "uk"
            StudyLanguage.ROMANIAN -> "ro"
            StudyLanguage.INDONESIAN -> "id"
            StudyLanguage.VIETNAMESE -> "vi"
            StudyLanguage.NORWEGIAN -> "no"
            StudyLanguage.DANISH -> "da"
            StudyLanguage.FINNISH -> "fi"
            StudyLanguage.CZECH -> "cs"
        }

        val chunks = splitIntoSafeChunks(clean, 300)
        if (chunks.size <= 1) {
            return callSingleMyMemoryChunk(clean, langPairCode)
        }
        val translatedChunks = mutableListOf<String>()
        for (chunk in chunks) {
            val res = callSingleMyMemoryChunk(chunk, langPairCode) ?: return null
            translatedChunks.add(res)
        }
        return translatedChunks.joinToString(" ")
    }

    private fun callSingleMyMemoryChunk(chunk: String, targetLangCode: String): String? {
        return try {
            val encoded = URLEncoder.encode(chunk, "UTF-8")
            val urlStr = "https://api.mymemory.translated.net/get?q=$encoded&langpair=pt|$targetLangCode"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "AndroidStudyApp/2.0")

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val responseData = root.optJSONObject("responseData")
                val translatedText = responseData?.optString("translatedText", "") ?: ""
                if (translatedText.isNotBlank()) translatedText else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Chamada à API Gemini Flash para tradução contextual refinada
     */
    private fun callGeminiTranslation(
        termText: String,
        definitionText: String,
        targetLanguage: StudyLanguage,
        apiKey: String
    ): Pair<String, String>? {
        return try {
            val prompt = """
                Você é um tradutor acadêmico e jurídico multilíngue de alta precisão.
                Traduza o seguinte termo e sua definição de português para o idioma ${targetLanguage.displayName} (${targetLanguage.nativeName}).
                Retorne EXCLUSIVAMENTE um objeto JSON com as chaves "translatedTerm" e "translatedDefinition":
                {"translatedTerm": "...", "translatedDefinition": "..."}

                Termo: $termText
                Definição: $definitionText
            """.trimIndent()

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 6000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
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
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 1024)
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val candidates = root.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

                val cleanJson = rawText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val obj = JSONObject(cleanJson)
                val tTerm = obj.optString("translatedTerm", termText)
                val tDef = obj.optString("translatedDefinition", definitionText)
                tTerm to tDef
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Chamada à API OpenAI se fornecida chave
     */
    private fun callOpenAiTranslation(
        termText: String,
        definitionText: String,
        targetLanguage: StudyLanguage,
        apiKey: String
    ): Pair<String, String>? {
        return try {
            val prompt = """
                Translate the following dictionary/study term and its definition from Portuguese into ${targetLanguage.displayName} (${targetLanguage.nativeName}).
                Format response STRICTLY as a JSON object:
                {"translatedTerm": "...", "translatedDefinition": "..."}
                
                Term: $termText
                Definition: $definitionText
            """.trimIndent()

            val url = URL("https://api.openai.com/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.connectTimeout = 6000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.2)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val choices = root.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                val json = JSONObject(content)
                val tTerm = json.optString("translatedTerm", termText)
                val tDef = json.optString("translatedDefinition", definitionText)
                tTerm to tDef
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun callOpenAiTextBlockTranslation(
        text: String,
        targetLanguage: StudyLanguage,
        apiKey: String
    ): String? {
        return try {
            val prompt = """
                Translate the following text block from Portuguese into ${targetLanguage.displayName} (${targetLanguage.nativeName}).
                Preserve all technical, literary, legal, philosophical and grammatical nuances with maximum fidelity.
                Return ONLY the direct translated text. Do NOT include markdown code blocks, do NOT include greetings or notes.

                Text to translate:
                $text
            """.trimIndent()

            val url = URL("https://api.openai.com/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.connectTimeout = 7000
            conn.readTimeout = 12000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.2)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val choices = root.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                val cleanResult = content.trim().removeSurrounding("\"").trim()
                cleanResult.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Léxico multilíngue offline abrangente para todos os 13 idiomas
     */
    fun getLocalMultilingualTranslation(
        termText: String,
        definitionText: String,
        targetLanguage: StudyLanguage
    ): Pair<String, String> {
        val lower = termText.trim().lowercase()

        // 1. Termo CONFLITO e derivados (suporte direto e completo para todos os idiomas)
        if (lower.contains("conflito")) {
            val t = getMultiLanguageTermName(termText, targetLanguage)
            val d = when (targetLanguage) {
                StudyLanguage.PORTUGUESE -> "Embates, divergências ou lutas entre partes com interesses, ideias ou valores opostos."
                StudyLanguage.ENGLISH -> "A struggle, disagreement, or collision between opposing forces, ideas, or interests."
                StudyLanguage.SPANISH -> "Lucha, desacuerdo o colisión entre fuerzas, ideas o intereses opuestos."
                StudyLanguage.FRENCH -> "Lutte, désaccord ou affrontement entre forces, idées ou intérêts opposés."
                StudyLanguage.GERMAN -> "Auseinandersetzung, Meinungsverschiedenheit oder Zusammenstoß zwischen gegensätzlichen Kräften oder Interessen."
                StudyLanguage.ITALIAN -> "Lotta, disaccordo o scontro tra forze, idee o interessi contrastanti."
                StudyLanguage.RUSSIAN -> "Столкновение противоположных интересов, взглядов, сил или серьезное разногласие."
                StudyLanguage.CHINESE -> "对立力量、观点或利益之间的斗争、分歧或冲突。"
                StudyLanguage.JAPANESE -> "対立する勢力、意見、または利害の間の争い、不一致、または衝突。"
                StudyLanguage.KOREAN -> "서로 대립하는 힘, 의견 또는 이해관계 사이의 다툼이나 충돌."
                StudyLanguage.ARABIC -> "نزاع أو خلاف أو صدام بين قوى أو آراء أو مصالح متعارضة."
                StudyLanguage.TURKISH -> "Karşıt güçler, fikirler veya çıkarlar arasındaki çatışma, uyuşmazlık veya mücadele."
                StudyLanguage.DUTCH -> "Een strijd, meningsverschil of botsing tussen tegengestelde krachten, ideeën of belangen."
                StudyLanguage.SWEDISH -> "En strid, oenighet eller kollision mellan motstridiga krafter, idéer eller intressen."
                StudyLanguage.POLISH -> "Spór, sprzeczność interesów lub starcie przeciwstawnych sił, idei lub wartości."
                StudyLanguage.GREEK -> "Σύγκρουση, διαφωνία ή αντιπαράθεση μεταξύ αντίθετων δυνάμεων, ιδεών ή συμφερόντων."
                StudyLanguage.HEBREW -> "עימות, חילוקי דעות או התנגשות בין כוחות, רעיונות או אינטרסים מנוגדים."
                StudyLanguage.HINDI -> "विरोधी ताकतों, विचारों या हितों के बीच संघर्ष, असहमति या टकराव।"
                StudyLanguage.UKRAINIAN -> "Зіткнення протилежних інтересів, поглядів, сил або серйозна розбіжність."
                StudyLanguage.ROMANIAN -> "O luptă, dezacord sau ciocnire între forțe, idei sau interese opuse."
                StudyLanguage.INDONESIAN -> "Perselisihan, pertentangan, atau benturan antara kekuatan, ide, atau kepentingan yang berlawanan."
                StudyLanguage.VIETNAMESE -> "Sự đấu tranh, bất đồng hoặc va chạm giữa các lực lượng, ý kiến hoặc lợi ích đối lập."
                StudyLanguage.NORWEGIAN -> "En strid, uenighet eller kollisjon mellom motstridende krefter, ideer eller interesser."
                StudyLanguage.DANISH -> "En strid, uoverensstemmelse eller sammenstød mellem modstridende kræfter, idéer eller interesser."
                StudyLanguage.FINNISH -> "Vastakkaisten voimien, ajatusten tai intressien välinen ristiriita, erimielisyys tai yhteenotto."
                StudyLanguage.CZECH -> "Střet, spor nebo neshoda mezi protichůdnými silami, myšlenkami nebo zájmy."
            }
            return t to d
        }

        val (localTerm, localDef) = when (targetLanguage) {
            StudyLanguage.PORTUGUESE -> {
                termText to definitionText
            }
            StudyLanguage.GERMAN -> {
                val t = when {
                    lower.contains("preconceito") -> "Vorurteil"
                    lower.contains("discriminação") -> "Diskriminierung"
                    lower.contains("estereótipo") -> "Stereotyp"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Verfassung"
                    lower.contains("direito penal") -> "Strafrecht"
                    lower.contains("direito civil") -> "Zivilrecht"
                    lower.contains("direito administrativo") -> "Verwaltungsrecht"
                    lower.contains("direito constitucional") -> "Verfassungsrecht"
                    lower.contains("direito") -> "Recht / Gesetz"
                    lower.contains("legalidade") -> "Gesetzmäßigkeit"
                    lower.contains("ampla defesa") -> "Umfassende Verteidigung"
                    lower.contains("contraditório") -> "Rechtliches Gehör"
                    lower.contains("devido processo") -> "Faires Verfahren"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Gleichheit vor dem Gesetz"
                    lower.contains("dignidade") -> "Menschenwürde"
                    lower.contains("estado de direito") -> "Rechtsstaat"
                    lower.contains("cidadania") -> "Staatsbürgerschaft"
                    lower.contains("democracia") -> "Demokratie"
                    lower.contains("poder judiciário") -> "Judikative / Justiz"
                    lower.contains("poder executivo") -> "Exekutive"
                    lower.contains("poder legislativo") -> "Legislative"
                    lower.contains("ministério público") -> "Staatsanwaltschaft"
                    lower.contains("lei") || lower.contains("legislação") -> "Gesetz / Gesetzgebung"
                    lower.contains("processo") -> "Gerichtsverfahren"
                    lower.contains("recurso") -> "Rechtsmittel / Berufung"
                    lower.contains("jurisprudência") -> "Rechtsprechung"
                    lower.contains("sentença") -> "Gerichtsurteil"
                    lower.contains("acórdão") -> "Gerichtsentscheidung"
                    lower.contains("competência") -> "Zuständigkeit"
                    lower.contains("responsabilidade") -> "Verantwortung / Haftung"
                    lower.contains("administração") -> "Verwaltung"
                    lower.contains("servidor público") -> "Beamter / Öffentlicher Dienst"
                    lower.contains("segurança jurídica") -> "Rechtssicherheit"
                    lower.contains("habeas corpus") -> "Habeas Corpus"
                    lower.contains("tutela") -> "Rechtsschutz / Vormundschaft"
                    lower.contains("eficácia") -> "Wirksamkeit"
                    lower.contains("norma") -> "Rechtsnorm"
                    lower.contains("princípio") -> "Rechtsgrundsatz"
                    else -> translateGeneralLexicon(termText, "de")
                }
                val d = when {
                    definitionText.isBlank() -> "Rechtlicher Fachbegriff und juristische Definition auf Deutsch."
                    lower.contains("legalidade") -> "Grundsatz, nach dem die Verwaltung nur auf der Grundlage des Gesetzes handeln darf."
                    lower.contains("constituição") -> "Oberstes Grundgesetz des Staates, das die Grundrechte und Staatsorganisation regelt."
                    lower.contains("ampla defesa") -> "Verfassungsrechtlicher Grundsatz, der jedem Bürger das Recht auf umfassende Verteidigung garantiert."
                    lower.contains("devido processo") -> "Verfahrensgarantie, die ein gerechtes, faires und gesetzmäßiges Verfahren vorschreibt."
                    lower.contains("dignidade") -> "Grundlegendes Verfassungsprinzip zum Schutz der unantastbaren Würde des Menschen."
                    lower.contains("processo") -> "Rechtlich geregeltes Verfahren zur gerichtlichen Entscheidung über Streitigkeiten."
                    else -> "Rechtlicher Begriff ($t): Fachliche juristische Bedeutung im Kontext der Rechtsordnung."
                }
                t to d
            }
            StudyLanguage.RUSSIAN -> {
                val t = when {
                    lower.contains("preconceito") -> "Предвзятость / Предрассудок"
                    lower.contains("discriminação") -> "Дискриминация"
                    lower.contains("estereótipo") -> "Стереотип"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Конституция"
                    lower.contains("direito penal") -> "Уголовное право"
                    lower.contains("direito civil") -> "Гражданское право"
                    lower.contains("direito administrativo") -> "Административное право"
                    lower.contains("direito constitucional") -> "Конституционное право"
                    lower.contains("direito") -> "Право / Закон"
                    lower.contains("legalidade") -> "Законность"
                    lower.contains("ampla defesa") -> "Право на полную защиту"
                    lower.contains("contraditório") -> "Принцип состязательности"
                    lower.contains("devido processo") -> "Надлежащая правовая процедура"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Равноправие перед законом"
                    lower.contains("dignidade") -> "Человеческое достоинство"
                    lower.contains("estado de direito") -> "Правовое государство"
                    lower.contains("cidadania") -> "Гражданство"
                    lower.contains("democracia") -> "Демократия"
                    lower.contains("poder judiciário") -> "Судебная власть"
                    lower.contains("poder executivo") -> "Исполнительная власть"
                    lower.contains("poder legislativo") -> "Законодательная власть"
                    lower.contains("ministério público") -> "Прокуратура"
                    lower.contains("lei") || lower.contains("legislação") -> "Закон / Законодательство"
                    lower.contains("processo") -> "Судебный процесс"
                    lower.contains("recurso") -> "Апелляция / Обжалование"
                    lower.contains("jurisprudência") -> "Судебная практика"
                    lower.contains("sentença") -> "Судебное решение / Приговор"
                    lower.contains("acórdão") -> "Постановление суда"
                    lower.contains("competência") -> "Компетенция / Юрисдикция"
                    lower.contains("responsabilidade") -> "Юридическая ответственность"
                    lower.contains("administração") -> "Государственное управление"
                    lower.contains("servidor público") -> "Государственный служащий"
                    lower.contains("segurança jurídica") -> "Правовая определённость"
                    lower.contains("habeas corpus") -> "Хабеас корпус"
                    lower.contains("tutela") -> "Судебная защита / Опека"
                    lower.contains("eficácia") -> "Юридическая сила / Эффективность"
                    lower.contains("norma") -> "Правовая норма"
                    lower.contains("princípio") -> "Правовой принцип"
                    else -> translateGeneralLexicon(termText, "ru")
                }
                val d = when {
                    definitionText.isBlank() -> "Юридический термин и правовое толкование на русском языке."
                    lower.contains("legalidade") -> "Основополагающий принцип, требующий строгого соблюдения законов всеми органами власти."
                    lower.contains("constituição") -> "Высший нормативный правовой акт государства, закрепляющий основы строя и права граждан."
                    lower.contains("ampla defesa") -> "Конституционная гарантия права лица использовать все законные средства для защиты прав."
                    lower.contains("devido processo") -> "Гарантия справедливого, беспристрастного и законного судебного разбирательства."
                    lower.contains("dignidade") -> "Высшая ценность правового государства, гарантирующая неприкосновенность личности."
                    lower.contains("processo") -> "Установленный законом порядок осуществления правосудия и разрешения судебных споров."
                    else -> "Юридическое понятие ($t): Правовое значение и толкование в контексте правопорядка."
                }
                t to d
            }
            StudyLanguage.ITALIAN -> {
                val t = when {
                    lower.contains("preconceito") -> "Pregiudizio"
                    lower.contains("discriminação") -> "Discriminazione"
                    lower.contains("estereótipo") -> "Stereotipo"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Costituzione"
                    lower.contains("direito penal") -> "Diritto penale"
                    lower.contains("direito civil") -> "Diritto civile"
                    lower.contains("direito administrativo") -> "Diritto amministrativo"
                    lower.contains("direito constitucional") -> "Diritto costituzionale"
                    lower.contains("direito") -> "Diritto / Legge"
                    lower.contains("legalidade") -> "Principio di legalità"
                    lower.contains("ampla defesa") -> "Diritto di difesa"
                    lower.contains("contraditório") -> "Principio del contraddittorio"
                    lower.contains("devido processo") -> "Giusto processo"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Uguaglianza dinanzi alla legge"
                    lower.contains("dignidade") -> "Dignità della persona umana"
                    lower.contains("estado de direito") -> "Stato di diritto"
                    lower.contains("cidadania") -> "Cittadinanza"
                    lower.contains("democracia") -> "Democrazia"
                    lower.contains("poder judiciário") -> "Potere giudiziario"
                    lower.contains("poder executivo") -> "Potere esecutivo"
                    lower.contains("poder legislativo") -> "Potere legislativo"
                    lower.contains("ministério público") -> "Pubblico ministero"
                    lower.contains("lei") || lower.contains("legislação") -> "Legge / Legislazione"
                    lower.contains("processo") -> "Processo giudiziario"
                    lower.contains("recurso") -> "Ricorso / Appello"
                    lower.contains("jurisprudência") -> "Giurisprudenza"
                    lower.contains("sentença") -> "Sentenza"
                    lower.contains("acórdão") -> "Decisione collegiale"
                    lower.contains("competência") -> "Competenza giurisdizionale"
                    lower.contains("responsabilidade") -> "Responsabilità giuridica"
                    lower.contains("administração") -> "Pubblica amministrazione"
                    lower.contains("servidor público") -> "Pubblico ufficiale"
                    lower.contains("segurança jurídica") -> "Certezza del diritto"
                    lower.contains("habeas corpus") -> "Habeas corpus"
                    lower.contains("tutela") -> "Tutela giurisdizionale"
                    lower.contains("eficácia") -> "Efficacia giuridica"
                    lower.contains("norma") -> "Norma giuridica"
                    lower.contains("princípio") -> "Principio generale del diritto"
                    else -> translateGeneralLexicon(termText, "it")
                }
                val d = when {
                    definitionText.isBlank() -> "Termine giuridico e definizione normativa in italiano."
                    lower.contains("legalidade") -> "Principio fondamentale secondo cui l'azione pubblica deve fondarsi sempre sulla legge."
                    lower.contains("constituição") -> "Legge fondamentale dello Stato che definisce i diritti inviolabili e i poteri pubblici."
                    lower.contains("ampla defesa") -> "Garanzia costituzionale che riconosce a ogni persona il diritto a una difesa effettiva."
                    lower.contains("devido processo") -> "Diritto a un processo equo, pubblico, tempestivo e svolto davanti a un giudice terzo."
                    lower.contains("dignidade") -> "Valore supremo e principio fondante dell'ordinamento a tutela dell'essere umano."
                    lower.contains("processo") -> "Sequenza regolata di atti giuridici volta alla risoluzione di una controversia giudiziaria."
                    else -> "Concetto giuridico ($t): Significato normativo e dottrinale nel quadro dell'ordinamento."
                }
                t to d
            }
            StudyLanguage.CHINESE -> {
                val t = when {
                    lower.contains("preconceito") -> "偏见"
                    lower.contains("discriminação") -> "歧视"
                    lower.contains("estereótipo") -> "刻板印象"
                    lower.contains("constituição") || lower.contains("constitucional") -> "宪法"
                    lower.contains("direito penal") -> "刑法"
                    lower.contains("direito civil") -> "民法"
                    lower.contains("direito administrativo") -> "行政法"
                    lower.contains("direito constitucional") -> "宪法学"
                    lower.contains("direito") -> "法律 / 权利"
                    lower.contains("legalidade") -> "合法性原则"
                    lower.contains("ampla defesa") -> "全面辩护权"
                    lower.contains("contraditório") -> "对抗原则 / 辩论原则"
                    lower.contains("devido processo") -> "正当法律程序"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "法律面前人人平等"
                    lower.contains("dignidade") -> "人的尊严"
                    lower.contains("estado de direito") -> "法治国"
                    lower.contains("cidadania") -> "公民身份 / 公民权"
                    lower.contains("democracia") -> "民主制度"
                    lower.contains("poder judiciário") -> "司法权 / 法院系统"
                    lower.contains("poder executivo") -> "行政权"
                    lower.contains("poder legislativo") -> "立法权"
                    lower.contains("ministério público") -> "检察机关"
                    lower.contains("lei") || lower.contains("legislação") -> "法律 / 法令"
                    lower.contains("processo") -> "诉讼程序 / 审判程序"
                    lower.contains("recurso") -> "上诉 / 申诉"
                    lower.contains("jurisprudência") -> "司法判例"
                    lower.contains("sentença") -> "法院判决"
                    lower.contains("acórdão") -> "合议庭判决"
                    lower.contains("competência") -> "管辖权"
                    lower.contains("responsabilidade") -> "法律责任"
                    lower.contains("administração") -> "公共行政"
                    lower.contains("servidor público") -> "公务员"
                    lower.contains("segurança jurídica") -> "法律安定性"
                    lower.contains("habeas corpus") -> "人身保护令"
                    lower.contains("tutela") -> "司法救济 / 监护"
                    lower.contains("eficácia") -> "法律效力"
                    lower.contains("norma") -> "法律规范"
                    lower.contains("princípio") -> "法律原则"
                    else -> translateGeneralLexicon(termText, "zh")
                }
                val d = when {
                    definitionText.isBlank() -> "中文法律术语与概念释义。"
                    lower.contains("legalidade") -> "法治国家的基本原则，要求行政行为必须具有明确的法律依据。"
                    lower.contains("constituição") -> "国家的根本大法，规定国家基本制度、政权组织以及公民基本权利。"
                    lower.contains("ampla defesa") -> "宪法保障权利，确保诉讼各方享有充分陈述事实与抗辩的权利。"
                    lower.contains("devido processo") -> "保障程序公正的基本原则，要求司法审判遵循正当法定的程序。"
                    lower.contains("dignidade") -> "法治秩序的核心价值，保障人作为独立主体的人格尊严不受侵犯。"
                    lower.contains("processo") -> "依法定程序开展的司法裁判与争议解决过程。"
                    else -> "法律概念（$t）：在现代法治体系中的规范含义与应用价值。"
                }
                t to d
            }
            StudyLanguage.JAPANESE -> {
                val t = when {
                    lower.contains("preconceito") -> "偏見"
                    lower.contains("discriminação") -> "差別"
                    lower.contains("estereótipo") -> "固定観念"
                    lower.contains("constituição") || lower.contains("constitucional") -> "憲法"
                    lower.contains("direito penal") -> "刑法"
                    lower.contains("direito civil") -> "民法"
                    lower.contains("direito administrativo") -> "行政法"
                    lower.contains("direito constitucional") -> "憲法学"
                    lower.contains("direito") -> "法 / 権利"
                    lower.contains("legalidade") -> "罪刑法定主義 / 法律の留保"
                    lower.contains("ampla defesa") -> "十分な防御権"
                    lower.contains("contraditório") -> "対審原則 / 双方審尋"
                    lower.contains("devido processo") -> "適正手続 (デュー・プロセス)"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "法の下の平等"
                    lower.contains("dignidade") -> "個人の尊厳"
                    lower.contains("estado de direito") -> "法の支配 / 法治国家"
                    lower.contains("cidadania") -> "市民権"
                    lower.contains("democracia") -> "民主主義"
                    lower.contains("poder judiciário") -> "司法権 / 裁判所"
                    lower.contains("poder executivo") -> "行政権"
                    lower.contains("poder legislativo") -> "立法権"
                    lower.contains("ministério público") -> "検察庁"
                    lower.contains("lei") || lower.contains("legislação") -> "法律 / 法令"
                    lower.contains("processo") -> "訴訟手続"
                    lower.contains("recurso") -> "上訴 / 控訴"
                    lower.contains("jurisprudência") -> "判例 / 裁判例"
                    lower.contains("sentença") -> "判決"
                    lower.contains("acórdão") -> "合議体判決"
                    lower.contains("competência") -> "管轄権"
                    lower.contains("responsabilidade") -> "法的責任"
                    lower.contains("administração") -> "公行政"
                    lower.contains("servidor público") -> "公務員"
                    lower.contains("segurança jurídica") -> "法的安定性"
                    lower.contains("habeas corpus") -> "人身保護令"
                    lower.contains("tutela") -> "司法的救済 / 後見"
                    lower.contains("eficácia") -> "法的効力"
                    lower.contains("norma") -> "法規範"
                    lower.contains("princípio") -> "法の一般原則"
                    else -> translateGeneralLexicon(termText, "ja")
                }
                val d = when {
                    definitionText.isBlank() -> "日本語による法律専門用語と概念の解説。"
                    lower.contains("legalidade") -> "国家権力の行使は常に法に基づかなければならないという基本原則。"
                    lower.contains("constituição") -> "国の最高法規であり、国家の統治機構と国民の基本的人権を保障する。"
                    lower.contains("ampla defesa") -> "当事者が法的手続において十分な反論と立証を行う権利の保障。"
                    lower.contains("devido processo") -> "公正かつ適正な法定手続を踏まなければ権利の制限を受けない原則。"
                    lower.contains("dignidade") -> "法秩序の究極の根拠であり、個人のかけがえのない価値を尊重する根本原則。"
                    lower.contains("processo") -> "紛争解決のために法によって規定された一連の裁判手続。"
                    else -> "法律概念（$t）：法秩序における法的意義および解釈基準。"
                }
                t to d
            }
            StudyLanguage.UKRAINIAN -> {
                val t = when {
                    lower.contains("preconceito") -> "Упередження"
                    lower.contains("discriminação") -> "Дискримінація"
                    lower.contains("estereótipo") -> "Стереотип"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Конституція"
                    lower.contains("direito penal") -> "Кримінальне право"
                    lower.contains("direito civil") -> "Цивільне право"
                    lower.contains("direito administrativo") -> "Адміністративне право"
                    lower.contains("direito constitucional") -> "Конституційне право"
                    lower.contains("direito") -> "Право / Закон"
                    lower.contains("legalidade") -> "Принцип законності"
                    lower.contains("ampla defesa") -> "Право на повний захист"
                    lower.contains("contraditório") -> "Змагальність сторін"
                    lower.contains("devido processo") -> "Належна правова процедура"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Рівність перед законом"
                    lower.contains("dignidade") -> "Гідність людини"
                    lower.contains("estado de direito") -> "Правова держава"
                    lower.contains("cidadania") -> "Громадянство"
                    lower.contains("democracia") -> "Демократія"
                    lower.contains("poder judiciário") -> "Судова влада"
                    lower.contains("poder executivo") -> "Виконавча влада"
                    lower.contains("poder legislativo") -> "Законодавча влада"
                    lower.contains("ministério público") -> "Прокуратура"
                    lower.contains("lei") || lower.contains("legislação") -> "Закон / Законодавство"
                    lower.contains("processo") -> "Судовий процес"
                    lower.contains("recurso") -> "Апеляція / Оскарження"
                    lower.contains("jurisprudência") -> "Судова практика"
                    lower.contains("sentença") -> "Судове рішення / Вирок"
                    lower.contains("acórdão") -> "Постанова суду"
                    lower.contains("competência") -> "Підсудність / Компетенція"
                    lower.contains("responsabilidade") -> "Юридична відповідальність"
                    lower.contains("administração") -> "Публічна адміністрація"
                    lower.contains("servidor público") -> "Державний службовець"
                    lower.contains("segurança jurídica") -> "Правова визначеність"
                    lower.contains("habeas corpus") -> "Хабеас корпус"
                    lower.contains("tutela") -> "Судовий захист / Опіка"
                    lower.contains("eficácia") -> "Юридична чинність"
                    lower.contains("norma") -> "Правова норма"
                    lower.contains("princípio") -> "Правовий принцип"
                    else -> translateGeneralLexicon(termText, "uk")
                }
                val d = when {
                    definitionText.isBlank() -> "Юридичний термін і правове тлумачення українською мовою."
                    lower.contains("legalidade") -> "Фундаментальний принцип, який вимагає дій органів влади виключно на підставі закону."
                    lower.contains("constituição") -> "Основний Закон держави, що визначає основи суспільного ладу та права громадян."
                    lower.contains("ampla defesa") -> "Конституційне право особи на повний і всебічний захист своїх законних інтересів."
                    lower.contains("devido processo") -> "Гарантія справедливого судового розгляду компетентним і неупередженим судом."
                    lower.contains("dignidade") -> "Найвища соціальна цінність, закріплена в Конституції для захисту прав кожної людини."
                    lower.contains("processo") -> "Встановлений законом порядок розгляду справ та вирішення судових спорів."
                    else -> "Правове поняття ($t): Юридичний зміст та застосування у правовій системі."
                }
                t to d
            }
            StudyLanguage.GREEK -> {
                val t = when {
                    lower.contains("preconceito") -> "Προκατάληψη"
                    lower.contains("discriminação") -> "Διάκριση"
                    lower.contains("estereótipo") -> "Στερεότυπο"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Σύνταγμα"
                    lower.contains("direito penal") -> "Ποινικό δίκαιο"
                    lower.contains("direito civil") -> "Αστικό δίκαιο"
                    lower.contains("direito administrativo") -> "Διοικητικό δίκαιο"
                    lower.contains("direito constitucional") -> "Συνταγματικό δίκαιο"
                    lower.contains("direito") -> "Δίκαιο / Δικαίωμα"
                    lower.contains("legalidade") -> "Αρχή της νομιμότητας"
                    lower.contains("ampla defesa") -> "Δικαίωμα πλήρους άμυνας"
                    lower.contains("contraditório") -> "Αρχή της εκατέρωθεν ακροάσεως"
                    lower.contains("devido processo") -> "Δίκαιη δίκη"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Ισονομία / Ισότητα ενώπιον του νόμου"
                    lower.contains("dignidade") -> "Αξία του ανθρώπου"
                    lower.contains("estado de direito") -> "Κράτος δικαίου"
                    lower.contains("cidadania") -> "Ιθαγένεια / Πολιτειότητα"
                    lower.contains("democracia") -> "Δημοκρατία"
                    lower.contains("poder judiciário") -> "Δικαστική εξουσία"
                    lower.contains("poder executivo") -> "Εκτελεστική εξουσία"
                    lower.contains("poder legislativo") -> "Νομοθετική εξουσία"
                    lower.contains("ministério público") -> "Εισαγγελία"
                    lower.contains("lei") || lower.contains("legislação") -> "Νόμος / Νομοθεσία"
                    lower.contains("processo") -> "Δικαστική διαδικασία"
                    lower.contains("recurso") -> "Έφεση / Ένδικο μέσο"
                    lower.contains("jurisprudência") -> "Νομολογία"
                    lower.contains("sentença") -> "Δικαστική απόφαση"
                    lower.contains("acórdão") -> "Απόφαση συλλογικού δικαστηρίου"
                    lower.contains("competência") -> "Δικαιοδοσία / Αρμοδιότητα"
                    lower.contains("responsabilidade") -> "Νομική ευθύνη"
                    lower.contains("administração") -> "Δημόσια διοίκηση"
                    lower.contains("servidor público") -> "Δημόσιος υπάλληλος"
                    lower.contains("segurança jurídica") -> "Ασφάλεια δικαίου"
                    lower.contains("habeas corpus") -> "Habeas corpus"
                    lower.contains("tutela") -> "Έννομη προστασία / Επιτροπεία"
                    lower.contains("eficácia") -> "Νομική ισχύς"
                    lower.contains("norma") -> "Κανόνας δικαίου"
                    lower.contains("princípio") -> "Γενική αρχή του δικαίου"
                    else -> translateGeneralLexicon(termText, "el")
                }
                val d = when {
                    definitionText.isBlank() -> "Νομικός όρος και ερμηνεία στην ελληνική γλώσσα."
                    lower.contains("legalidade") -> "Θεμελιώδης αρχή σύμφωνα με την οποία η διοίκηση δρα αποκλειστικά βάσει του νόμου."
                    lower.contains("constituição") -> "Ο ανώτατος νόμος του κράτους που κατοχυρώνει τα θεμελιώδη ατομικά δικαιώματα."
                    lower.contains("ampla defesa") -> "Συνταγματικό δικαίωμα του πολίτη να υπερασπιστεί πλήρως τα έννομα συμφέροντά του."
                    lower.contains("devido processo") -> "Δικαίωμα σε δίκαιη, αμερόληπτη και νόμιμη δικαστική διαδικασία."
                    lower.contains("dignidade") -> "Υπέρτατη αξία του δημοκρατικού πολιτεύματος που προστατεύει τον άνθρωπο."
                    lower.contains("processo") -> "Δικονομικά ρυθμισμένη διαδικασία για την επίλυση διαφορών ενώπιον των δικαστηρίων."
                    else -> "Νομική έννοια ($t): Ερμηνεία και εφαρμογή στο πλαίσιο της έννομης τάξης."
                }
                t to d
            }
            StudyLanguage.ARABIC -> {
                val t = when {
                    lower.contains("preconceito") -> "تحيز / حكم مسبق"
                    lower.contains("discriminação") -> "تمييز"
                    lower.contains("estereótipo") -> "صورة نمطية"
                    lower.contains("constituição") || lower.contains("constitucional") -> "الدستور"
                    lower.contains("direito penal") -> "القانون الجنائي"
                    lower.contains("direito civil") -> "القانون المدني"
                    lower.contains("direito administrativo") -> "القانون الإداري"
                    lower.contains("direito constitucional") -> "القانون الدستوري"
                    lower.contains("direito") -> "قانون / حق"
                    lower.contains("legalidade") -> "مبدأ الشرعية"
                    lower.contains("ampla defesa") -> "حق الدفاع الكامل"
                    lower.contains("contraditório") -> "مبدأ الوجاهية / التقاضي التواجهي"
                    lower.contains("devido processo") -> "الإجراءات القانونية الواجبة"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "المساواة أمام القانون"
                    lower.contains("dignidade") -> "كرامة الإنسان"
                    lower.contains("estado de direito") -> "دولة القانون"
                    lower.contains("cidadania") -> "المواطنة"
                    lower.contains("democracia") -> "الديمقراطية"
                    lower.contains("poder judiciário") -> "السلطة القضائية"
                    lower.contains("poder executivo") -> "السلطة التنفيذية"
                    lower.contains("poder legislativo") -> "السلطة التشريعية"
                    lower.contains("ministério público") -> "النيابة العامة"
                    lower.contains("lei") || lower.contains("legislação") -> "قانون / تشريع"
                    lower.contains("processo") -> "إجراءات قانونية / دعوى قضائية"
                    lower.contains("recurso") -> "استئناف / طعن"
                    lower.contains("jurisprudência") -> "اجتهاد قضائي"
                    lower.contains("sentença") -> "حكم قضائي"
                    lower.contains("acórdão") -> "قرار قضائي جماعي"
                    lower.contains("competência") -> "اختصاص قضائي"
                    lower.contains("responsabilidade") -> "مسؤولية قانونية"
                    lower.contains("administração") -> "الإدارة العامة"
                    lower.contains("servidor público") -> "موظف عام"
                    lower.contains("segurança jurídica") -> "الأمن القانوني"
                    lower.contains("habeas corpus") -> "أمر الإحضار أمام المحكمة"
                    lower.contains("tutela") -> "حماية قضائية / وصاية"
                    lower.contains("eficácia") -> "نفاذ قانوني / فاعلية"
                    lower.contains("norma") -> "قاعدة قانونية"
                    lower.contains("princípio") -> "مبدأ قانوني عام"
                    else -> translateGeneralLexicon(termText, "ar")
                }
                val d = when {
                    definitionText.isBlank() -> "مصطلح قانوني وشرح دقيق باللغة العربية."
                    lower.contains("legalidade") -> "مبدأ أساسي يقضي بأن جميع تصرفات السلطة العامة يجب أن تستند إلى نصوص القانون."
                    lower.contains("constituição") -> "القانون الأسمى للدولة الذي يحدد شكل نظام الحكم ويضمن الحقوق والحريات الأساسية."
                    lower.contains("ampla defesa") -> "ضمانة دستورية تكفل للشخص ممارسة كافة الوسائل المشروعة للدفاع عن حقوقه."
                    lower.contains("devido processo") -> "مبدأ يضمن محاكمة عادلة وعلنية أمام قضاء مستقل ومحايد."
                    lower.contains("dignidade") -> "القيمة العليا التي يرتكز عليها النظام القانوني لحماية كرامة الإنسان وحقوقه."
                    lower.contains("processo") -> "سلسلة الإجراءات القانونية المتبعة للفصل في الخصومات أمام المحاكم المختصة."
                    else -> "مفهوم قانوني ($t): المعنى الفقهي والتطبيقي في إطار النظام القضائي."
                }
                t to d
            }
            StudyLanguage.HEBREW -> {
                val t = when {
                    lower.contains("preconceito") -> "דעה קדומה"
                    lower.contains("discriminação") -> "אפליה"
                    lower.contains("estereótipo") -> "סטריאוטיפ"
                    lower.contains("constituição") || lower.contains("constitucional") -> "חוקה"
                    lower.contains("direito penal") -> "משפט פלילי"
                    lower.contains("direito civil") -> "משפט אזרחי"
                    lower.contains("direito administrativo") -> "משפט מנהלי"
                    lower.contains("direito constitucional") -> "משפט חוקתי"
                    lower.contains("direito") -> "משפט / זכות"
                    lower.contains("legalidade") -> "עקרון החוקיות"
                    lower.contains("ampla defesa") -> "זכות הטיעון וההגנה"
                    lower.contains("contraditório") -> "זכות העימות הדיוני"
                    lower.contains("devido processo") -> "הליך הוגן"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "שוויון בפני החוק"
                    lower.contains("dignidade") -> "כבוד האדם"
                    lower.contains("estado de direito") -> "שלטון החוק"
                    lower.contains("cidadania") -> "אזרחות"
                    lower.contains("democracia") -> "דמוקרטיה"
                    lower.contains("poder judiciário") -> "הרשות השופטת"
                    lower.contains("poder executivo") -> "הרשות המבצעת"
                    lower.contains("poder legislativo") -> "הרשות המחוקקת"
                    lower.contains("ministério público") -> "פרקליטות המדינה"
                    lower.contains("lei") || lower.contains("legislação") -> "חוק / חקיקה"
                    lower.contains("processo") -> "הליך משפטי"
                    lower.contains("recurso") -> "ערעור"
                    lower.contains("jurisprudência") -> "פסיקה"
                    lower.contains("sentença") -> "פסק דין"
                    lower.contains("acórdão") -> "החלטת מותב שופטים"
                    lower.contains("competência") -> "סמכות שיפוט"
                    lower.contains("responsabilidade") -> "אחריות משפטית"
                    lower.contains("administração") -> "מנהל ציבורי"
                    lower.contains("servidor público") -> "עובד ציבור"
                    lower.contains("segurança jurídica") -> "ודאות משפטית"
                    lower.contains("habeas corpus") -> "הביאס קורפוס"
                    lower.contains("tutela") -> "הגנה משפטית / אפוטרופסות"
                    lower.contains("eficácia") -> "תוקף משפטי"
                    lower.contains("norma") -> "נורמה משפטית"
                    lower.contains("princípio") -> "עקרון יסוד משפטי"
                    else -> translateGeneralLexicon(termText, "he")
                }
                val d = when {
                    definitionText.isBlank() -> "מונח משפטי והגדרה מדויקת בעברית."
                    lower.contains("legalidade") -> "עקרון יסוד לפיו הרשות השלטונית רשאית לפעול אך ורק מכוח הסמכה מפורשת בחוק."
                    lower.contains("constituição") -> "חוק היסוד העליון של המדינה הקובע את סדרי השלטון ומגן על זכויות היסוד."
                    lower.contains("ampla defesa") -> "זכות חוקתית המבטיחה לכל אדם את מלוא האמצעים המשפטיים להגנה על עניינו."
                    lower.contains("devido processo") -> "הבטחה לניהול משפט הוגן, פומבי וחסר פניות בפני בית משפט מוסמך."
                    lower.contains("dignidade") -> "ערך יסוד עליון במדינה דמוקרטית המבטיח את כבודו וחירותו של כל אדם."
                    lower.contains("processo") -> "סדר פעולות משפטי הקבוע בדין לשם הכרעה בסכסוכים בפני הרשות השופטת."
                    else -> "מושג משפטי ($t): משמעות נורמטיבית ויישום במסגרת שיטת המשפט."
                }
                t to d
            }
            StudyLanguage.ENGLISH -> {
                val t = when {
                    lower.contains("preconceito") -> "Prejudice / Bias"
                    lower.contains("discriminação") -> "Discrimination"
                    lower.contains("estereótipo") -> "Stereotype"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Constitution"
                    lower.contains("direito penal") -> "Criminal Law"
                    lower.contains("direito civil") -> "Civil Law"
                    lower.contains("direito administrativo") -> "Administrative Law"
                    lower.contains("direito constitucional") -> "Constitutional Law"
                    lower.contains("direito") -> "Law / Right"
                    lower.contains("legalidade") -> "Principle of Legality"
                    lower.contains("ampla defesa") -> "Right to a Full Defense"
                    lower.contains("contraditório") -> "Adversarial Principle"
                    lower.contains("devido processo") -> "Due Process of Law"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Equality Before the Law"
                    lower.contains("dignidade") -> "Human Dignity"
                    lower.contains("estado de direito") -> "Rule of Law"
                    lower.contains("cidadania") -> "Citizenship"
                    lower.contains("democracia") -> "Democracy"
                    lower.contains("poder judiciário") -> "Judiciary / Judicial Branch"
                    lower.contains("poder executivo") -> "Executive Branch"
                    lower.contains("poder legislativo") -> "Legislative Branch"
                    lower.contains("ministério público") -> "Public Prosecutor's Office"
                    lower.contains("lei") || lower.contains("legislação") -> "Statute / Law"
                    lower.contains("processo") -> "Legal Process / Lawsuit"
                    lower.contains("recurso") -> "Appeal / Legal Remedy"
                    lower.contains("jurisprudência") -> "Jurisprudence / Case Law"
                    lower.contains("sentença") -> "Judicial Ruling / Judgment"
                    lower.contains("acórdão") -> "Appellate Decision"
                    lower.contains("competência") -> "Jurisdiction / Competence"
                    lower.contains("responsabilidade") -> "Legal Liability"
                    lower.contains("administração") -> "Public Administration"
                    lower.contains("servidor público") -> "Civil Servant"
                    lower.contains("segurança jurídica") -> "Legal Certainty"
                    lower.contains("habeas corpus") -> "Habeas Corpus"
                    lower.contains("tutela") -> "Judicial Relief / Guardianship"
                    lower.contains("eficácia") -> "Legal Efficacy"
                    lower.contains("norma") -> "Legal Rule / Norm"
                    lower.contains("princípio") -> "General Legal Principle"
                    else -> translateGeneralLexicon(termText, "en")
                }
                val d = when {
                    definitionText.isBlank() -> "Legal concept and doctrinal explanation in English."
                    lower.contains("legalidade") -> "Fundamental principle establishing that the government may act only as authorized by law."
                    lower.contains("constituição") -> "The supreme law of the state establishing the structure of government and civil rights."
                    lower.contains("ampla defesa") -> "Constitutional guarantee ensuring the right to present a comprehensive defense."
                    lower.contains("devido processo") -> "Constitutional requirement that legal proceedings adhere to established rules and fairness."
                    lower.contains("dignidade") -> "Core constitutional value protecting the inherent worth and rights of every human being."
                    lower.contains("processo") -> "Legally regulated sequence of acts to resolve disputes before a judicial court."
                    else -> "Legal concept ($t): Meaning and doctrinal application within the legal system."
                }
                t to d
            }
            StudyLanguage.SPANISH -> {
                val t = when {
                    lower.contains("preconceito") -> "Prejuicio"
                    lower.contains("discriminação") -> "Discriminación"
                    lower.contains("estereótipo") -> "Estereotipo"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Constitución"
                    lower.contains("direito penal") -> "Derecho penal"
                    lower.contains("direito civil") -> "Derecho civil"
                    lower.contains("direito administrativo") -> "Derecho administrativo"
                    lower.contains("direito constitucional") -> "Derecho constitucional"
                    lower.contains("direito") -> "Derecho / Ley"
                    lower.contains("legalidade") -> "Principio de legalidad"
                    lower.contains("ampla defesa") -> "Derecho a la defensa"
                    lower.contains("contraditório") -> "Principio de contradicción"
                    lower.contains("devido processo") -> "Debido proceso legal"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Igualdad ante la ley"
                    lower.contains("dignidade") -> "Dignidad de la persona humana"
                    lower.contains("estado de direito") -> "Estado de derecho"
                    lower.contains("cidadania") -> "Ciudadanía"
                    lower.contains("democracia") -> "Democracia"
                    lower.contains("poder judiciário") -> "Poder judicial"
                    lower.contains("poder executivo") -> "Poder ejecutivo"
                    lower.contains("poder legislativo") -> "Poder legislativo"
                    lower.contains("ministério público") -> "Ministerio público / Fiscalía"
                    lower.contains("lei") || lower.contains("legislação") -> "Ley / Legislación"
                    lower.contains("processo") -> "Proceso judicial"
                    lower.contains("recurso") -> "Recurso / Apelación"
                    lower.contains("jurisprudência") -> "Jurisprudencia"
                    lower.contains("sentença") -> "Sentencia judicial"
                    lower.contains("acórdão") -> "Fallo colegiado"
                    lower.contains("competência") -> "Competencia judicial"
                    lower.contains("responsabilidade") -> "Responsabilidad legal"
                    lower.contains("administração") -> "Administración pública"
                    lower.contains("servidor público") -> "Funcionario público"
                    lower.contains("segurança jurídica") -> "Seguridad jurídica"
                    lower.contains("habeas corpus") -> "Habeas corpus"
                    lower.contains("tutela") -> "Tutela judicial / Guarda"
                    lower.contains("eficácia") -> "Eficacia jurídica"
                    lower.contains("norma") -> "Norma jurídica"
                    lower.contains("princípio") -> "Principio general del derecho"
                    else -> translateGeneralLexicon(termText, "es")
                }
                val d = when {
                    definitionText.isBlank() -> "Término jurídico y definición doctrinal en español."
                    lower.contains("legalidade") -> "Principio fundamental según el cual la administración pública solo puede actuar conforme a la ley."
                    lower.contains("constituição") -> "Norma suprema del Estado que establece la organización de los poderes y los derechos fundamentales."
                    lower.contains("ampla defesa") -> "Garantía constitucional que concede a toda persona el derecho a defenderse ampliamente en juicio."
                    lower.contains("devido processo") -> "Garantía que exige el cumplimiento de todas las fases legales y formales del procedimiento."
                    lower.contains("dignidade") -> "Principio rector del ordenamiento que consagra el valor inviolable del ser humano."
                    lower.contains("processo") -> "Conjunto ordenado de actos jurídicos mediante el cual los tribunales resuelven litigios."
                    else -> "Concepto jurídico ($t): Significado normativo y aplicación dentro del ordenamiento."
                }
                t to d
            }
            StudyLanguage.FRENCH -> {
                val t = when {
                    lower.contains("preconceito") -> "Préjugé"
                    lower.contains("discriminação") -> "Discrimination"
                    lower.contains("estereótipo") -> "Stéréotype"
                    lower.contains("constituição") || lower.contains("constitucional") -> "Constitution"
                    lower.contains("direito penal") -> "Droit pénal"
                    lower.contains("direito civil") -> "Droit civil"
                    lower.contains("direito administrativo") -> "Droit administratif"
                    lower.contains("direito constitucional") -> "Droit constitutionnel"
                    lower.contains("direito") -> "Droit / Loi"
                    lower.contains("legalidade") -> "Principe de légalité"
                    lower.contains("ampla defesa") -> "Droits de la défense"
                    lower.contains("contraditório") -> "Principe du contradictoire"
                    lower.contains("devido processo") -> "Procès équitable"
                    lower.contains("isonomia") || lower.contains("igualdade") -> "Égalité devant la loi"
                    lower.contains("dignidade") -> "Dignité de la personne humaine"
                    lower.contains("estado de direito") -> "État de droit"
                    lower.contains("cidadania") -> "Citoyenneté"
                    lower.contains("democracia") -> "Démocratie"
                    lower.contains("poder judiciário") -> "Pouvoir judiciaire"
                    lower.contains("poder executivo") -> "Pouvoir exécutif"
                    lower.contains("poder legislativo") -> "Pouvoir législatif"
                    lower.contains("ministério público") -> "Ministère public / Parquet"
                    lower.contains("lei") || lower.contains("legislação") -> "Loi / Législation"
                    lower.contains("processo") -> "Procès / Procédure judiciaire"
                    lower.contains("recurso") -> "Recours / Appel"
                    lower.contains("jurisprudência") -> "Jurisprudence"
                    lower.contains("sentença") -> "Jugement / Décision"
                    lower.contains("acórdão") -> "Arrêt"
                    lower.contains("competência") -> "Compétence juridictionnelle"
                    lower.contains("responsabilidade") -> "Responsabilité juridique"
                    lower.contains("administração") -> "Administration publique"
                    lower.contains("servidor público") -> "Fonctionnaire public"
                    lower.contains("segurança jurídica") -> "Sécurité juridique"
                    lower.contains("habeas corpus") -> "Habeas corpus"
                    lower.contains("tutela") -> "Protection judiciaire / Tutelle"
                    lower.contains("eficácia") -> "Efficacité juridique"
                    lower.contains("norma") -> "Règle de droit / Norme"
                    lower.contains("princípio") -> "Principe général du droit"
                    else -> translateGeneralLexicon(termText, "fr")
                }
                val d = when {
                    definitionText.isBlank() -> "Terme juridique et définition doctrinale en français."
                    lower.contains("legalidade") -> "Principe selon lequel l'action administrative doit être conforme à la loi."
                    lower.contains("constituição") -> "Loi suprême de l'État organisant les institutions et garantissant les libertés publiques."
                    lower.contains("ampla defesa") -> "Garantie constitutionnelle assurant le plein exercice des droits de la défense."
                    lower.contains("devido processo") -> "Droit fondamental garantissant un procès équitable devant un tribunal impartial."
                    lower.contains("dignidade") -> "Principe fondamental affirmant la valeur inaliénable de chaque être humain."
                    lower.contains("processo") -> "Ensemble des actes juridiques accomplis pour trancher un litige en justice."
                    else -> "Concept juridique ($t): Portée normative et doctrinale dans l'ordre juridique."
                }
                t to d
            }
            StudyLanguage.KOREAN -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.KOREAN, "") ?: translateGeneralLexicon(termText, "ko")
                val d = when {
                    definitionText.isBlank() -> "한국어로 된 학술 및 법률 용어 정의."
                    lower.contains("legalidade") -> "행정 작용이 반드시 법률에 근거하여 이루어져야 한다는 법치주의 원칙."
                    lower.contains("constituição") -> "국가의 기본 조직과 통치 원리를 정하고 국민의 기본권을 보장하는 최고 규범."
                    lower.contains("ampla defesa") -> "모든 당사자가 재판에서 불이익 없이 정당하게 자신을 방어할 수 있는 헌법상 보장."
                    lower.contains("devido processo") -> "적법한 절차와 공정한 재판을 보장하는 적법절차의 원칙."
                    lower.contains("dignidade") -> "모든 인간의 침해할 수 없는 절대적 가치인 인간의 존엄성."
                    lower.contains("processo") -> "분쟁을 법적으로 공정하게 해결하기 위한 일련의 소송 절차."
                    else -> "학술 용어 ($t): 연구 및 제도적 맥락에서 다루어지는 법적·이론적 개념."
                }
                t to d
            }
            StudyLanguage.TURKISH -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.TURKISH, "") ?: translateGeneralLexicon(termText, "tr")
                val d = when {
                    definitionText.isBlank() -> "Türkçe akademik ve hukuki terim tanımı."
                    lower.contains("legalidade") -> "İdarenin ve kamu gücünün kanunlara uygun hareket etmesi gerektiğini öngören kanunilik ilkesi."
                    lower.contains("constituição") -> "Devletin temel yapısını ve bireylerin temel haklarını düzenleyen en üst hukuk normu."
                    lower.contains("ampla defesa") -> "Kişinin haklarını etkili bir şekilde savunabilmesini teminat altına alan savunma hakkı."
                    lower.contains("devido processo") -> "Adil, tarafsız ve kanunlara uygun bir yargılama yapılmasını güvence altına alan adil yargılanma hakkı."
                    lower.contains("dignidade") -> "İnsan onurunun dokunulmazlığını ve temel değerini vurgulayan temel ilke."
                    lower.contains("processo") -> "Uyuşmazlıkların adli yoldan çözülmesi için izlenen usul ve yargılama süreci."
                    else -> "Kavram ($t): Hukuki ve akademik bağlamda ele alınan temel ilke ve tanım."
                }
                t to d
            }
            StudyLanguage.DUTCH -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.DUTCH, "") ?: translateGeneralLexicon(termText, "nl")
                val d = when {
                    definitionText.isBlank() -> "Juridische en academische termdefinitie in het Nederlands."
                    lower.contains("legalidade") -> "Het legaliteitsbeginsel dat voorschrijft dat overheidsoptreden altijd op de wet gebaseerd moet zijn."
                    lower.contains("constituição") -> "De hoogste wet van de staat die de staatsinrichting en grondrechten regelt."
                    lower.contains("ampla defesa") -> "Het fundamentele recht op een effectieve verdediging in rechte."
                    lower.contains("devido processo") -> "Het recht op een eerlijk proces voor een onpartijdige rechter."
                    lower.contains("dignidade") -> "Het fundamentele principe van de menselijke waardigheid als grondslag van de rechtsstaat."
                    lower.contains("processo") -> "De wettelijk vastgelegde gerechtelijke procedure ter beslechting van geschillen."
                    else -> "Juridisch begrip ($t): Normatieve en inhoudelijke betekenis in de rechtsorde."
                }
                t to d
            }
            StudyLanguage.SWEDISH -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.SWEDISH, "") ?: translateGeneralLexicon(termText, "sv")
                val d = when {
                    definitionText.isBlank() -> "Juridisk och akademisk termdefinition på svenska."
                    lower.contains("legalidade") -> "Legalitetsprincipen som kräver att all myndighetsutövning har stöd i lag."
                    lower.contains("constituição") -> "Statens grundlag som fastställer statsskicket och skyddar grundläggande fri- och rättigheter."
                    lower.contains("ampla defesa") -> "Rätten till ett fullgott och effektivt försvar i rättsliga sammanhang."
                    lower.contains("devido processo") -> "Rätten till en rättvis och lagenlig rättegång inför en opartisk domstol."
                    lower.contains("dignidade") -> "Principen om alla människors okränkbara värdighet."
                    lower.contains("processo") -> "Det lagreglerade rättsliga förfarandet för att lösa tvister."
                    else -> "Begrepp ($t): Teoretisk och normativ betydelse inom rättsordningen."
                }
                t to d
            }
            StudyLanguage.POLISH -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.POLISH, "") ?: translateGeneralLexicon(termText, "pl")
                val d = when {
                    definitionText.isBlank() -> "Definicja terminu prawnego i naukowego w języku polskim."
                    lower.contains("legalidade") -> "Zasada praworządności wymagająca działania organów władzy na podstawie i w granicach prawa."
                    lower.contains("constituição") -> "Najwyższe prawo państwa określające ustrój oraz podstawowe prawa i wolności obywateli."
                    lower.contains("ampla defesa") -> "Konstytucyjne prawo do rzetelnej i skutecznej obrony we wszelkich stadiach postępowania."
                    lower.contains("devido processo") -> "Gwarancja rzetelnego, sprawiedliwego i jawnego procesu przed bezstronnym sądem."
                    lower.contains("dignidade") -> "Przyrodzona i niezbywalna godność człowieka stanowiąca źródło wolności i praw."
                    lower.contains("processo") -> "Uregulowany przepisami prawnymi ciąg czynności sądowych zmierzający do rozstrzygnięcia sprawy."
                    else -> "Pojęcie prawne ($t): Znaczenie doktrynalne i normatywne w porządku prawnym."
                }
                t to d
            }
            StudyLanguage.HINDI -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.HINDI, "") ?: translateGeneralLexicon(termText, "hi")
                val d = when {
                    definitionText.isBlank() -> "हिंदी में कानूनी और शैक्षणिक शब्द की परिभाषा।"
                    lower.contains("legalidade") -> "कानून के शासन का सिद्धांत जिसके अनुसार प्रशासन को केवल कानून के तहत कार्य करना चाहिए।"
                    lower.contains("constituição") -> "राष्ट्र का सर्वोच्च कानून जो राज्य के अंगों और मौलिक अधिकारों को निर्धारित करता है।"
                    lower.contains("ampla defesa") -> "न्याय प्रणाली में स्वयं का निष्पक्ष और पूर्ण बचाव करने का मौलिक अधिकार।"
                    lower.contains("devido processo") -> "निष्पक्ष, पारदर्शी और वैध न्यायिक प्रक्रिया की संवैधानिक गारंटी।"
                    lower.contains("dignidade") -> "मानव गरिमा की अविभाज्य और मौलिक अवधारणा।"
                    lower.contains("processo") -> "न्यायिक विवादों के समाधान के लिए स्थापित कानूनी प्रक्रिया।"
                    else -> "कानूनी अवधारणा ($t): कानूनी व्यवस्था के संदर्भ में प्रासंगिक अर्थ और परिभाषा।"
                }
                t to d
            }
            StudyLanguage.ROMANIAN -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.ROMANIAN, "") ?: translateGeneralLexicon(termText, "ro")
                val d = when {
                    definitionText.isBlank() -> "Termen juridic și definiție academică în limba română."
                    lower.contains("legalidade") -> "Principiul legalității conform căruia autoritățile trebuie să acționeze numai pe baza legii."
                    lower.contains("constituição") -> "Legea fundamentală a statului care garantează drepturile și organizează puterile publice."
                    lower.contains("ampla defesa") -> "Garanția constituțională care asigură dreptul la o apărare deplină și efectivă."
                    lower.contains("devido processo") -> "Dreptul fundamental la un proces echitabil în fața unei instanțe imparțiale."
                    lower.contains("dignidade") -> "Principiul demnității umane inviolabile ca fundament al ordinii de drept."
                    lower.contains("processo") -> "Procedura legală judiciară instituită pentru soluționarea litigiilor."
                    else -> "Concept juridic ($t): Semnificație normativă și doctrinară în ordinea juridică."
                }
                t to d
            }
            StudyLanguage.INDONESIAN -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.INDONESIAN, "") ?: translateGeneralLexicon(termText, "id")
                val d = when {
                    definitionText.isBlank() -> "Istilah hukum dan definisi akademik dalam bahasa Indonesia."
                    lower.contains("legalidade") -> "Asas legalitas yang menuntut setiap tindakan pemerintah harus berdasarkan hukum yang berlaku."
                    lower.contains("constituição") -> "Hukum dasar tertinggi negara yang mengatur ketatanegaraan dan hak asasi warga negara."
                    lower.contains("ampla defesa") -> "Hak konstitusional setiap orang untuk memperoleh pembelaan yang efektif dalam peradilan."
                    lower.contains("devido processo") -> "Prinsip peradilan yang adil dan jujur sesuai dengan prosedur hukum yang sah."
                    lower.contains("dignidade") -> "Prinsip martabat manusia yang hakiki dan tidak dapat dicabut."
                    lower.contains("processo") -> "Rangkaian prosedur hukum di pengadilan untuk menyelesaikan sengketa hukum."
                    else -> "Konsep hukum ($t): Makna normatif dan doktrinal dalam kerangka sistem hukum."
                }
                t to d
            }
            StudyLanguage.VIETNAMESE -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.VIETNAMESE, "") ?: translateGeneralLexicon(termText, "vi")
                val d = when {
                    definitionText.isBlank() -> "Thuật ngữ pháp lý và định nghĩa học thuật bằng tiếng Việt."
                    lower.contains("legalidade") -> "Nguyên tắc pháp chế đòi hỏi mọi hoạt động của cơ quan công quyền phải dựa trên luật pháp."
                    lower.contains("constituição") -> "Đạo luật cơ bản cao nhất của nhà nước quy định chế độ chính trị và quyền công dân."
                    lower.contains("ampla defesa") -> "Quyền bào chữa đầy đủ và bình đẳng trước cơ quan tài phán."
                    lower.contains("devido processo") -> "Nguyên tắc thủ tục tố tụng công bằng và hợp pháp."
                    lower.contains("dignidade") -> "Nguyên tắc tôn trọng và bảo vệ phẩm giá con người."
                    lower.contains("processo") -> "Trình tự thủ tục tố tụng tư pháp nhằm giải quyết các tranh chấp theo luật định."
                    else -> "Khái niệm ($t): Ý nghĩa pháp lý và học thuật trong hệ thống pháp luật."
                }
                t to d
            }
            StudyLanguage.NORWEGIAN -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.NORWEGIAN, "") ?: translateGeneralLexicon(termText, "no")
                val d = when {
                    definitionText.isBlank() -> "Juridisk og akademisk termdefinisjon på norsk."
                    lower.contains("legalidade") -> "Legalitetsprinsippet som krever at offentlige inngrep må ha hjemmel i lov."
                    lower.contains("constituição") -> "Grunnloven som statens øverste rettskilde og vern om grunnleggende rettigheter."
                    lower.contains("ampla defesa") -> "Retten til et fullgodt og effektivt forsvar i rettsprosesser."
                    lower.contains("devido processo") -> "Retten til en rettferdig og upartisk rettergang."
                    lower.contains("dignidade") -> "Prinsippet om menneskets ukrenkelige verdighet."
                    lower.contains("processo") -> "Den rettslige fremgangsmåten for å avgjøre tvister i domstolene."
                    else -> "Begrep ($t): Betydning og anvendelse i rettsordenen."
                }
                t to d
            }
            StudyLanguage.DANISH -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.DANISH, "") ?: translateGeneralLexicon(termText, "da")
                val d = when {
                    definitionText.isBlank() -> "Juridisk og akademisk termdefinition på dansk."
                    lower.contains("legalidade") -> "Legalitetsprincippet der foreskriver at forvaltningen skal handle med hjemmel i lov."
                    lower.contains("constituição") -> "Grundloven som den højeste retskilde der sikrer frihedsrettigheder og magtens tredeling."
                    lower.contains("ampla defesa") -> "Retten til et effektivt og fyldestgørende forsvar."
                    lower.contains("devido processo") -> "Garantien for en retfærdig og lovlig rettergang for en uafhængig domstol."
                    lower.contains("dignidade") -> "Princippet om menneskets ukrænkelige værdighed."
                    lower.contains("processo") -> "Den retlige proces til afgørelse af tvister ved domstolene."
                    else -> "Begreb ($t): Normativ betydning i retssystemet."
                }
                t to d
            }
            StudyLanguage.FINNISH -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.FINNISH, "") ?: translateGeneralLexicon(termText, "fi")
                val d = when {
                    definitionText.isBlank() -> "Oikeudellinen ja akateeminen käsitemääritelmä suomeksi."
                    lower.contains("legalidade") -> "Laillisuusperiaate, jonka mukaan julkisen vallan käytön tulee aina perustua lakiin."
                    lower.contains("constituição") -> "Perustuslaki valtion ylimpänä säädöksenä, joka turvaa perusoikeudet."
                    lower.contains("ampla defesa") -> "Oikeus täysimääräiseen ja tehokkaaseen puolustukseen oikeudenkäynnissä."
                    lower.contains("devido processo") -> "Oikeus oikeudenmukaiseen ja asianmukaiseen oikeudenkäyntiin."
                    lower.contains("dignidade") -> "Ihmisarvon loukkaamattomuuden periaate."
                    lower.contains("processo") -> "Lainmukainen oikeudenkäyntimenettely riitojen ratkaisemiseksi."
                    else -> "Käsite ($t): Oikeudellinen merkitys ja soveltaminen oikeusjärjestyksessä."
                }
                t to d
            }
            StudyLanguage.CZECH -> {
                val t = getAcademicLexiconTerm(lower, StudyLanguage.CZECH, "") ?: translateGeneralLexicon(termText, "cs")
                val d = when {
                    definitionText.isBlank() -> "Právní a akademická definice pojmu v češtině."
                    lower.contains("legalidade") -> "Zásada legality požadující, aby státní moc byla uplatňována jen na základě zákona."
                    lower.contains("constituição") -> "Ústava jako základní a nejvyšší zákon státu chránící základní lidská práva."
                    lower.contains("ampla defesa") -> "Ústavní právo na plnou a účinnou obhajobu před soudem."
                    lower.contains("devido processo") -> "Právo na spravedlivý a řádný proces před nezávislým soudem."
                    lower.contains("dignidade") -> "Zásada nedotknutelnosti lidské důstojnosti."
                    lower.contains("processo") -> "Zákonem stanovený soudní proces k rozhodování o právech a sporech."
                    else -> "Pojem ($t): Právní a věcný význam v rámci právního řádu."
                }
                t to d
            }
            else -> {
                val t = translateGeneralLexicon(termText, targetLanguage.code)
                val d = if (definitionText.isNotBlank()) "[$t] ${targetLanguage.displayName}: $definitionText" else t
                t to d
            }
        }

        return localTerm to localDef
    }

    /**
     * Tradução morfológica inteligente para vocabulário geral quando não há regra estrita
     */
    private fun translateGeneralLexicon(termText: String, langCode: String): String {
        val t = termText.trim()
        val lower = t.lowercase()

        return when (langCode) {
            "de" -> when {
                lower.endsWith("ção") -> t.removeSuffix("ção").removeSuffix("cao") + "tion"
                lower.endsWith("dade") -> t.removeSuffix("dade") + "tät"
                lower.endsWith("ismo") -> t.removeSuffix("ismo") + "ismus"
                lower.endsWith("ista") -> t.removeSuffix("ista") + "ist"
                else -> t
            }
            "it" -> when {
                lower.endsWith("ção") -> t.removeSuffix("ção").removeSuffix("cao") + "zione"
                lower.endsWith("dade") -> t.removeSuffix("dade") + "tà"
                lower.endsWith("ismo") -> t.removeSuffix("ismo") + "ismo"
                lower.endsWith("ista") -> t.removeSuffix("ista") + "ista"
                else -> t
            }
            "es" -> when {
                lower.endsWith("ção") -> t.removeSuffix("ção").removeSuffix("cao") + "ción"
                lower.endsWith("dade") -> t.removeSuffix("dade") + "dad"
                lower.endsWith("ismo") -> t.removeSuffix("ismo") + "ismo"
                lower.endsWith("ista") -> t.removeSuffix("ista") + "ista"
                else -> t
            }
            "fr" -> when {
                lower.endsWith("ção") -> t.removeSuffix("ção").removeSuffix("cao") + "tion"
                lower.endsWith("dade") -> t.removeSuffix("dade") + "té"
                lower.endsWith("ismo") -> t.removeSuffix("ismo") + "isme"
                lower.endsWith("ista") -> t.removeSuffix("ista") + "iste"
                else -> t
            }
            "en" -> when {
                lower.endsWith("ção") -> t.removeSuffix("ção").removeSuffix("cao") + "tion"
                lower.endsWith("dade") -> t.removeSuffix("dade") + "ty"
                lower.endsWith("ismo") -> t.removeSuffix("ismo") + "ism"
                lower.endsWith("ista") -> t.removeSuffix("ista") + "ist"
                else -> t
            }
            else -> t
        }
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
    }
}
