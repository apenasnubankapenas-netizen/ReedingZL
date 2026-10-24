package com.example.data.openai

import android.content.Context
import android.util.Log
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.StudyLanguage
import com.example.data.translator.TermsNotesTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// ==========================================
// DATA MODELS PARA AS 10 MELHORIAS DA OPENAI
// ==========================================

// 1. Sincronização Perfeita de Karaokê (Whisper Word Timestamps)
data class WordTimestamp(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

// 2. Quizzes e Flashcards Ativos por Página
data class PageQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class PageFlashcard(
    val front: String,
    val back: String,
    val keyConcept: String
)

data class PageActiveStudyKit(
    val questions: List<PageQuizQuestion>,
    val flashcards: List<PageFlashcard>
)

// 3. Modo Podcast / Diálogo Didático (Duas Vozes)
data class PodcastLine(
    val speaker: String, // "Alex (Apresentador)" ou "Dra. Sofia (Especialista)"
    val voice: String,   // "onyx" (masculina grave) ou "nova" (feminina expressiva)
    val text: String,
    var audioFile: File? = null
)

data class PodcastEpisode(
    val title: String,
    val summary: String,
    val lines: List<PodcastLine>
)

// 4. Simplificação de Linguagem em Níveis
data class SimplifiedLevels(
    val original: String,
    val didactic: String,
    val bulletPoints: List<String>
)

// 5. Treinador de Pronúncia e Leitura em Voz Alta
data class PronunciationResult(
    val recognizedText: String,
    val expectedText: String,
    val scorePercent: Int,
    val feedback: String,
    val phoneticTips: String
)

// 6. Frases de Contexto Real com Áudio Próprio
data class TermContextExample(
    val sentenceOriginal: String,
    val sentencePortuguese: String,
    val contextType: String, // ex: "Profissional", "Cotidiano", "Acadêmico"
    var audioFile: File? = null
)

// 7. Alerta de Falsos Amigos (Falsos Cognatos) e Nuances Culturais
data class FalseFriendsAlert(
    val term: String,
    val targetLanguage: String,
    val isFalseFriend: Boolean,
    val warningTitle: String,
    val explanation: String,
    val correctEquivalent: String
)

// 8. Audiograma de 30 Segundos
data class PageAudiogram(
    val pageNumber: Int,
    val bulletPoints: List<String>,
    val narratorScript: String,
    var audioFile: File? = null
)

// 9. Relação Semântica e Conexões entre os Termos
data class SemanticRelation(
    val sourceTerm: String,
    val relationType: String, // "Gera", "Depende de", "Soluciona", "Opõe-se a"
    val targetTerm: String,
    val explanation: String
)

/**
 * Serviço centralizado das 10 melhorias avançadas integradas com a API da OpenAI.
 */
object OpenAiAdvancedStudyService {

    private const val TAG = "OpenAiAdvancedStudy"
    private const val CHAT_URL = "https://api.openai.com/v1/chat/completions"
    private const val TRANSCRIPTION_URL = "https://api.openai.com/v1/audio/transcriptions"
    private const val SPEECH_URL = "https://api.openai.com/v1/audio/speech"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Cache local em memória de resultados para resposta instantânea
    private val studyKitCache = ConcurrentHashMap<String, PageActiveStudyKit>()
    private val podcastCache = ConcurrentHashMap<String, PodcastEpisode>()
    private val simplifiedCache = ConcurrentHashMap<String, SimplifiedLevels>()
    private val examplesCache = ConcurrentHashMap<String, List<TermContextExample>>()
    private val falseFriendsCache = ConcurrentHashMap<String, FalseFriendsAlert>()
    private val audiogramCache = ConcurrentHashMap<String, PageAudiogram>()
    private val semanticCache = ConcurrentHashMap<String, List<SemanticRelation>>()

    private fun resolveApiKey(context: Context): String {
        return OpenAiService.getApiKey(context).ifBlank {
            TermsNotesTranslator.resolveOpenAiApiKey(null, context)
        }
    }

    // =========================================================================
    // 1. SINCRONIZAÇÃO PERFEITA DE KARAOKÊ COM WHISPER (Word Timestamps)
    // =========================================================================
    suspend fun fetchWhisperWordTimestamps(
        audioFile: File,
        languageCode: String? = null,
        context: Context
    ): Result<List<WordTimestamp>> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave OpenAI ausente."))
        }
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(Exception("Arquivo de áudio não encontrado."))
        }

        try {
            val fileBody = audioFile.asRequestBody("audio/mpeg".toMediaType())
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.name, fileBody)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("timestamp_granularities[]", "word")

            if (!languageCode.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("language", languageCode.take(2).lowercase())
            }

            val request = Request.Builder()
                .url(TRANSCRIPTION_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(multipartBuilder.build())
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Whisper HTTP ${response.code}"))
            }

            val bodyStr = response.body?.string() ?: ""
            val json = JSONObject(bodyStr)
            val wordsArray = json.optJSONArray("words") ?: JSONArray()
            val list = mutableListOf<WordTimestamp>()

            for (i in 0 until wordsArray.length()) {
                val item = wordsArray.optJSONObject(i) ?: continue
                val word = item.optString("word", "").trim()
                val start = (item.optDouble("start", 0.0) * 1000).toLong()
                val end = (item.optDouble("end", 0.0) * 1000).toLong()
                if (word.isNotBlank()) {
                    list.add(WordTimestamp(word, start, end))
                }
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Erro Whisper word alignment: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. QUIZZES E FLASHCARDS ATIVOS POR PÁGINA (gpt-4o-mini)
    // =========================================================================
    suspend fun generatePageActiveStudyKit(
        pageText: String,
        context: Context
    ): Result<PageActiveStudyKit> = withContext(Dispatchers.IO) {
        val clean = pageText.trim()
        if (clean.isBlank()) return@withContext Result.failure(Exception("Página vazia."))

        val cacheKey = "study_kit_${abs(clean.hashCode())}"
        studyKitCache[cacheKey]?.let { return@withContext Result.success(it) }

        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val prompt = """
            Você é um professor e elaborador de questões pedagógicas.
            Com base no seguinte texto da página de um livro:
            \"\"\"$clean\"\"\"

            Gere um kit de estudo ativo no formato JSON rigoroso:
            {
              "questions": [
                {
                  "question": "Pergunta 1 objetiva sobre o ponto central",
                  "options": ["Opção A", "Opção B", "Opção C", "Opção D"],
                  "correctIndex": 0,
                  "explanation": "Explicação sucinta de por que a resposta está certa."
                },
                {
                  "question": "Pergunta 2 de aplicação prática ou interpretação",
                  "options": ["Opção A", "Opção B", "Opção C", "Opção D"],
                  "correctIndex": 1,
                  "explanation": "Explicação pedagógica."
                }
              ],
              "flashcards": [
                {
                  "front": "Conceito ou pergunta chave",
                  "back": "Definição direta e fácil de lembrar",
                  "keyConcept": "Palavra-chave"
                },
                {
                  "front": "Segundo conceito da página",
                  "back": "Definição direta",
                  "keyConcept": "Palavra-chave 2"
                }
              ]
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.3)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("OpenAI HTTP ${response.code}"))

            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            val qArray = json.optJSONArray("questions") ?: JSONArray()
            val questions = mutableListOf<PageQuizQuestion>()
            for (i in 0 until qArray.length()) {
                val qObj = qArray.optJSONObject(i) ?: continue
                val opts = mutableListOf<String>()
                val optArr = qObj.optJSONArray("options") ?: JSONArray()
                for (j in 0 until optArr.length()) opts.add(optArr.optString(j))
                questions.add(
                    PageQuizQuestion(
                        question = qObj.optString("question", "Pergunta"),
                        options = opts,
                        correctIndex = qObj.optInt("correctIndex", 0),
                        explanation = qObj.optString("explanation", "")
                    )
                )
            }

            val fArray = json.optJSONArray("flashcards") ?: JSONArray()
            val flashcards = mutableListOf<PageFlashcard>()
            for (i in 0 until fArray.length()) {
                val fObj = fArray.optJSONObject(i) ?: continue
                flashcards.add(
                    PageFlashcard(
                        front = fObj.optString("front", ""),
                        back = fObj.optString("back", ""),
                        keyConcept = fObj.optString("keyConcept", "")
                    )
                )
            }

            val kit = PageActiveStudyKit(questions, flashcards)
            studyKitCache[cacheKey] = kit
            Result.success(kit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro gerando Study Kit: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. MODO PODCAST / DIÁLOGO ENTRE DUAS VOZES (TTS Multi-Voice)
    // =========================================================================
    suspend fun generatePodcastDialogue(
        pageText: String,
        context: Context
    ): Result<PodcastEpisode> = withContext(Dispatchers.IO) {
        val clean = pageText.trim()
        val cacheKey = "podcast_${abs(clean.hashCode())}"
        podcastCache[cacheKey]?.let { return@withContext Result.success(it) }

        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val prompt = """
            Você é um roteirista de podcasts educativos dinâmicos e inteligentes.
            Crie um diálogo curto e cativante de 45 a 60 segundos entre dois apresentadores sobre este texto:
            \"\"\"$clean\"\"\"

            Personagens:
            - "Alex" (curioso, faz perguntas perspicazes e provocações práticas) -> use voz "onyx"
            - "Sofia" (especialista, explica de forma didática com exemplos) -> use voz "nova"

            Formato JSON estrito:
            {
              "title": "Título atraente do episódio",
              "summary": "Resumo de 1 frase",
              "lines": [
                {"speaker": "Alex", "voice": "onyx", "text": "Falas curtas e conversacionais..."},
                {"speaker": "Sofia", "voice": "nova", "text": "Resposta direta e esclarecedora..."},
                {"speaker": "Alex", "voice": "onyx", "text": "Segunda pergunta instigante..."},
                {"speaker": "Sofia", "voice": "nova", "text": "Conclusão memorável..."}
              ]
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.4)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("OpenAI HTTP ${response.code}"))

            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            val linesArray = json.optJSONArray("lines") ?: JSONArray()
            val lines = mutableListOf<PodcastLine>()
            for (i in 0 until linesArray.length()) {
                val lineObj = linesArray.optJSONObject(i) ?: continue
                lines.add(
                    PodcastLine(
                        speaker = lineObj.optString("speaker", "Narrador"),
                        voice = lineObj.optString("voice", "nova"),
                        text = lineObj.optString("text", "")
                    )
                )
            }

            val ep = PodcastEpisode(
                title = json.optString("title", "Diálogo da Página"),
                summary = json.optString("summary", ""),
                lines = lines
            )
            podcastCache[cacheKey] = ep
            Result.success(ep)
        } catch (e: Exception) {
            Log.e(TAG, "Erro gerando Podcast: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun synthesizeVoiceLine(
        text: String,
        voice: String,
        context: Context
    ): Result<File> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val cacheDir = File(context.cacheDir, "openai_podcast_tts")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val textHash = abs(text.trim().hashCode())
        val targetFile = File(cacheDir, "line_${voice}_${textHash}.mp3")
        if (targetFile.exists() && targetFile.length() > 0L) {
            return@withContext Result.success(targetFile)
        }

        try {
            val payload = JSONObject().apply {
                put("model", "tts-1")
                put("input", text.trim())
                put("voice", voice.lowercase())
                put("response_format", "mp3")
            }

            val request = Request.Builder()
                .url(SPEECH_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("TTS HTTP ${response.code}"))

            val bytes = response.body?.bytes() ?: return@withContext Result.failure(Exception("Corpo de áudio vazio."))
            targetFile.writeBytes(bytes)
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Erro sintetizando voz $voice: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. SIMPLIFICAÇÃO DE LINGUAGEM EM NÍVEIS (Didático / Essencial)
    // =========================================================================
    suspend fun simplifyTextLevels(
        text: String,
        context: Context
    ): Result<SimplifiedLevels> = withContext(Dispatchers.IO) {
        val clean = text.trim()
        val cacheKey = "levels_${abs(clean.hashCode())}"
        simplifiedCache[cacheKey]?.let { return@withContext Result.success(it) }

        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val prompt = """
            Você é um especialista em comunicação clara e simplificação pedagógica.
            Reescreva o seguinte trecho em 2 versões simplificadas:
            \"\"\"$clean\"\"\"

            1. "didactic": versão fluida, sem jargões desnecessários, com metáforas didáticas se oportuno.
            2. "bulletPoints": lista com 3 a 5 conclusões essenciais e diretas.

            JSON esperado:
            {
              "didactic": "...",
              "bulletPoints": ["Ponto 1", "Ponto 2", "Ponto 3"]
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.3)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("OpenAI HTTP ${response.code}"))

            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            val bullets = mutableListOf<String>()
            val bArray = json.optJSONArray("bulletPoints") ?: JSONArray()
            for (i in 0 until bArray.length()) bullets.add(bArray.optString(i))

            val levels = SimplifiedLevels(
                original = clean,
                didactic = json.optString("didactic", clean),
                bulletPoints = bullets
            )
            simplifiedCache[cacheKey] = levels
            Result.success(levels)
        } catch (e: Exception) {
            Log.e(TAG, "Erro simplificando texto: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. TREINADOR DE PRONÚNCIA E LEITURA EM VOZ ALTA (Whisper + GPT)
    // =========================================================================
    suspend fun evaluatePronunciation(
        audioRecordFile: File,
        expectedText: String,
        targetLanguage: StudyLanguage,
        context: Context
    ): Result<PronunciationResult> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        // Passo 1: Transcrever com Whisper
        val transcriptionRes = fetchWhisperWordTimestamps(audioRecordFile, targetLanguage.code, context)
        val recognized = if (transcriptionRes.isSuccess) {
            transcriptionRes.getOrNull()?.joinToString(" ") { it.word } ?: ""
        } else {
            // Tenta transcrição simples
            try {
                val fileBody = audioRecordFile.asRequestBody("audio/mp4".toMediaType())
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioRecordFile.name, fileBody)
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("language", targetLanguage.code.take(2).lowercase())
                    .build()
                val req = Request.Builder().url(TRANSCRIPTION_URL).addHeader("Authorization", "Bearer $apiKey").post(multipart).build()
                val resp = httpClient.newCall(req).execute()
                JSONObject(resp.body?.string() ?: "").optString("text", "")
            } catch (_: Exception) { "" }
        }

        // Passo 2: Avaliar com GPT-4o-mini
        val prompt = """
            Você é um professor fonoaudiólogo de línguas estrangeiras (${targetLanguage.displayName} - ${targetLanguage.nativeName}).
            O aluno deveria ler/pronunciar: "$expectedText"
            O sistema reconheceu o áudio como: "$recognized"

            Analise a clareza, correspondência e dê uma pontuação de 0 a 100%.
            Formato JSON:
            {
              "scorePercent": 92,
              "feedback": "Excelente entonação! Pronunciou perfeitamente as consoantes.",
              "phoneticTips": "Atenção especial na tonicidade da segunda sílaba."
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.2)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            Result.success(
                PronunciationResult(
                    recognizedText = recognized.ifBlank { "(Áudio não captado claramente)" },
                    expectedText = expectedText,
                    scorePercent = json.optInt("scorePercent", 80),
                    feedback = json.optString("feedback", "Boa tentativa de pronúncia!"),
                    phoneticTips = json.optString("phoneticTips", "Mantenha a prática regular.")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro avaliando pronúncia: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 6. FRASES DE CONTEXTO REAL COM ÁUDIO PRÓPRIO
    // =========================================================================
    suspend fun fetchRealWorldExamples(
        term: String,
        language: StudyLanguage,
        context: Context
    ): Result<List<TermContextExample>> = withContext(Dispatchers.IO) {
        val cacheKey = "examples_${term.lowercase()}_${language.code}"
        examplesCache[cacheKey]?.let { return@withContext Result.success(it) }

        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val prompt = """
            Você é um lexicógrafo nativo em ${language.displayName}.
            Para o termo de estudo: "$term"
            Gere 3 frases curtas e autênticas de uso no mundo real em ${language.displayName} (${language.nativeName}) com sua tradução em Português.

            JSON esperado:
            {
              "examples": [
                {
                  "sentenceOriginal": "Frase no idioma ${language.displayName}...",
                  "sentencePortuguese": "Tradução natural em português...",
                  "contextType": "Cotidiano / Profissional / Acadêmico"
                }
              ]
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.3)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            val array = json.optJSONArray("examples") ?: JSONArray()
            val list = mutableListOf<TermContextExample>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                list.add(
                    TermContextExample(
                        sentenceOriginal = item.optString("sentenceOriginal", ""),
                        sentencePortuguese = item.optString("sentencePortuguese", ""),
                        contextType = item.optString("contextType", "Prático")
                    )
                )
            }

            examplesCache[cacheKey] = list
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Erro gerando exemplos de contexto: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 7. ALERTA DE FALSOS AMIGOS (Falsos Cognatos) E NUANCES CULTURAIS
    // =========================================================================
    suspend fun detectFalseFriendsAndNuances(
        term: String,
        targetLanguage: StudyLanguage,
        context: Context
    ): Result<FalseFriendsAlert> = withContext(Dispatchers.IO) {
        val cacheKey = "falsefriend_${term.lowercase()}_${targetLanguage.code}"
        falseFriendsCache[cacheKey]?.let { return@withContext Result.success(it) }

        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val prompt = """
            Você é um linguista comparativo experiente.
            Analise o termo "$term" em relação ao idioma ${targetLanguage.displayName} (${targetLanguage.nativeName}).
            Verifique se ele possui:
            - Um falso cognato ("falso amigo") perigoso;
            - Ou uma nuance cultural/jurídica que frequentemente confunde falantes de português.

            JSON esperado:
            {
              "isFalseFriend": true ou false,
              "warningTitle": "Cuidado com o Falso Amigo" ou "Nuance Importante",
              "explanation": "Explicação objetiva de 1 a 2 linhas...",
              "correctEquivalent": "Palavra correta equivalente se aplicável"
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.2)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            val alert = FalseFriendsAlert(
                term = term,
                targetLanguage = targetLanguage.displayName,
                isFalseFriend = json.optBoolean("isFalseFriend", false),
                warningTitle = json.optString("warningTitle", "Dica de Vocabulário"),
                explanation = json.optString("explanation", ""),
                correctEquivalent = json.optString("correctEquivalent", "")
            )
            falseFriendsCache[cacheKey] = alert
            Result.success(alert)
        } catch (e: Exception) {
            Log.e(TAG, "Erro checando falsos amigos: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 8. AUDIOGRAMA DE 30 SEGUNDOS (Resumo Rápido da Página)
    // =========================================================================
    suspend fun generatePageAudiogram(
        pageNumber: Int,
        pageText: String,
        context: Context
    ): Result<PageAudiogram> = withContext(Dispatchers.IO) {
        val clean = pageText.trim()
        val cacheKey = "audiogram_${pageNumber}_${abs(clean.hashCode())}"
        audiogramCache[cacheKey]?.let { return@withContext Result.success(it) }

        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val prompt = """
            Você é um locutor e sintetizador de resumos executivos.
            Crie um audiograma narrado de exatamente 30 segundos sobre esta página $pageNumber:
            \"\"\"$clean\"\"\"

            Gere um roteiro narrativo fluido e conciso (máximo 65 palavras) e 3 pontos-chave.
            JSON:
            {
              "narratorScript": "Texto exato falado pelo narrador...",
              "bulletPoints": ["Destaque 1", "Destaque 2", "Destaque 3"]
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.3)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            val bullets = mutableListOf<String>()
            val bArray = json.optJSONArray("bulletPoints") ?: JSONArray()
            for (i in 0 until bArray.length()) bullets.add(bArray.optString(i))

            val script = json.optString("narratorScript", clean.take(200))
            val audioRes = OpenAiSpeechCache.getOrGenerateAudio(
                context = context,
                text = script,
                langCode = "pt",
                blockId = "audiogram_p${pageNumber}"
            )

            val audiogram = PageAudiogram(
                pageNumber = pageNumber,
                bulletPoints = bullets,
                narratorScript = script,
                audioFile = audioRes.getOrNull()
            )
            audiogramCache[cacheKey] = audiogram
            Result.success(audiogram)
        } catch (e: Exception) {
            Log.e(TAG, "Erro gerando Audiograma: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 9. RELAÇÃO SEMÂNTICA E CONEXÕES ENTRE OS TERMOS DA PÁGINA
    // =========================================================================
    suspend fun generateSemanticRelationships(
        pageText: String,
        terms: List<String>,
        context: Context
    ): Result<List<SemanticRelation>> = withContext(Dispatchers.IO) {
        if (terms.isEmpty()) return@withContext Result.success(emptyList())

        val termsKey = terms.sorted().joinToString(",")
        val cacheKey = "semantic_${abs(termsKey.hashCode())}"
        semanticCache[cacheKey]?.let { return@withContext Result.success(it) }

        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Chave OpenAI necessária."))

        val prompt = """
            Você é um especialista em modelagem conceitual e ontologia do conhecimento.
            Analise os seguintes termos presentes nesta página:
            Termos: ${terms.joinToString(", ")}
            Contexto: \"\"\"${pageText.take(1000)}\"\"\"

            Identifique como esses termos se conectam em cadeia de raciocínio.
            JSON esperado:
            {
              "relations": [
                {
                  "sourceTerm": "Termo A",
                  "relationType": "origina / fundamenta / soluciona / opõe-se a",
                  "targetTerm": "Termo B",
                  "explanation": "Explicação curta da conexão causal ou lógica."
                }
              ]
            }
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.2)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            }

            val request = Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val root = JSONObject(response.body?.string() ?: "")
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            val json = JSONObject(content)

            val array = json.optJSONArray("relations") ?: JSONArray()
            val list = mutableListOf<SemanticRelation>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                list.add(
                    SemanticRelation(
                        sourceTerm = item.optString("sourceTerm", ""),
                        relationType = item.optString("relationType", "relaciona-se com"),
                        targetTerm = item.optString("targetTerm", ""),
                        explanation = item.optString("explanation", "")
                    )
                )
            }

            semanticCache[cacheKey] = list
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Erro gerando relações semânticas: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 10. PRÉ-CACHE PREDITIVO DE ÁUDIO EM BACKGROUND
    // =========================================================================
    fun preloadNextPageTerms(
        terms: List<PageDictionaryTerm>,
        languages: List<StudyLanguage> = listOf(StudyLanguage.ENGLISH, StudyLanguage.SPANISH, StudyLanguage.FRENCH),
        context: Context
    ) {
        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank() || terms.isEmpty()) return

        scope.launch {
            try {
                // Seleciona os 3 primeiros termos da página seguinte
                terms.take(3).forEach { term ->
                    languages.forEach { lang ->
                        val translated = TermsNotesTranslator.translateDictionaryTerm(
                            term = term,
                            targetLanguage = lang,
                            customApiKey = apiKey,
                            context = context
                        )
                        val textToNarrate = "${translated.first}: ${translated.second}"
                        if (textToNarrate.isNotBlank()) {
                            OpenAiSpeechCache.getOrGenerateAudio(
                                context = context,
                                text = textToNarrate,
                                langCode = lang.code,
                                blockId = "pred_${term.id}",
                                customApiKey = apiKey
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Pré-cache background concluído ou interrompido: ${e.message}")
            }
        }
    }
}
