package com.example.data.translator

import com.example.data.model.SubtitleCue
import com.example.data.parser.SrtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OpenAiTranslator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions"

    suspend fun translateSrt(
        apiKey: String,
        model: String = "gpt-4o-mini",
        targetLanguage: String,
        srtContent: String,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("Chave da API da OpenAI não informada."))
        }

        val originalCues = SrtParser.parseSrt(srtContent)
        if (originalCues.isEmpty()) {
            return@withContext Result.failure(Exception("Nenhuma legenda válida encontrada no arquivo SRT."))
        }

        val totalCues = originalCues.size
        val translatedCuesMap = mutableMapOf<Int, String>()

        // Batch in sizes of 25 cues per request
        val batchSize = 25
        val batches = originalCues.chunked(batchSize)
        var currentCompleted = 0

        try {
            for (batch in batches) {
                val batchResult = translateBatch(
                    apiKey = cleanKey,
                    model = model.ifBlank { "gpt-4o-mini" },
                    targetLanguage = targetLanguage,
                    batch = batch
                )

                if (batchResult.isFailure) {
                    return@withContext Result.failure(
                        batchResult.exceptionOrNull() ?: Exception("Falha ao traduzir lote.")
                    )
                }

                val translations = batchResult.getOrNull() ?: emptyMap()
                translatedCuesMap.putAll(translations)

                currentCompleted += batch.size
                onProgress(currentCompleted.coerceAtMost(totalCues), totalCues)
            }

            // Map back to original cues preserving exact timing
            val finalCues = originalCues.map { cue ->
                val translatedText = translatedCuesMap[cue.index] ?: cue.text
                val wordTimings = SrtParser.generateWordTimings(
                    text = translatedText,
                    startMs = cue.startTimeMs,
                    endMs = cue.endTimeMs
                )
                cue.copy(
                    text = translatedText,
                    words = wordTimings
                )
            }

            val translatedSrt = SrtParser.exportSrt(finalCues)
            Result.success(translatedSrt)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun translateBatch(
        apiKey: String,
        model: String,
        targetLanguage: String,
        batch: List<SubtitleCue>
    ): Result<Map<Int, String>> {
        return try {
            val itemsArray = JSONArray()
            batch.forEach { cue ->
                val item = JSONObject()
                item.put("id", cue.index)
                item.put("text", cue.text)
                itemsArray.put(item)
            }

            val systemPrompt = """
                Você é um tradutor especialista em legendas de filmes e vídeos SRT.
                Sua tarefa é traduzir o texto de cada legenda para o idioma: $targetLanguage.
                
                REGRAS OBRIGATÓRIAS:
                1. Mantenha o tom natural, conciso e contextualizado.
                2. Retorne APENAS um objeto JSON válido no formato:
                   {"translations": [{"id": 1, "text": "texto traduzido"}, ...]}
                3. O ID de cada objeto no resultado deve corresponder EXATAMENTE ao ID de entrada.
                4. Não remova, não adicione e não ignore nenhum ID.
                5. Não inclua NENHUM texto fora da estrutura JSON.
            """.trimIndent()

            val userContent = itemsArray.toString()

            val payload = JSONObject()
            payload.put("model", model)

            val messagesArray = JSONArray()

            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", systemPrompt)
            messagesArray.put(systemMsg)

            val userMsg = JSONObject()
            userMsg.put("role", "user")
            userMsg.put("content", userContent)
            messagesArray.put(userMsg)

            payload.put("messages", messagesArray)
            payload.put("temperature", 0.3)

            // Force JSON response if supported
            val responseFormat = JSONObject()
            responseFormat.put("type", "json_object")
            payload.put("response_format", responseFormat)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(DEFAULT_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
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
                    return Result.failure(Exception(errorMsg))
                }

                val resObj = JSONObject(bodyStr)
                val choices = resObj.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return Result.failure(Exception("Resposta da OpenAI sem opções."))
                }

                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                val cleanContent = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                val jsonContent = JSONObject(cleanContent)
                val translationsArray = jsonContent.optJSONArray("translations") ?: JSONArray()

                val map = mutableMapOf<Int, String>()
                for (i in 0 until translationsArray.length()) {
                    val obj = translationsArray.getJSONObject(i)
                    val id = obj.getInt("id")
                    val text = obj.getString("text")
                    map[id] = text
                }

                Result.success(map)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
