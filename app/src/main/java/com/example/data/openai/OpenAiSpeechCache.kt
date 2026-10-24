package com.example.data.openai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

object OpenAiSpeechCache {
    private const val TAG = "OpenAiSpeechCache"

    // Vozes da OpenAI: mescladas entre masculinas e femininas para escolha aleatória natural
    // Femininas: nova, shimmer, alloy
    // Masculinas: onyx, echo, fable
    val OPENAI_VOICES = listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer")

    fun getRandomVoice(): String {
        return OPENAI_VOICES.random()
    }

    /**
     * Recupera do cache em disco local ou gera via OpenAI TTS com voz aleatória,
     * persistindo o arquivo de áudio permanentemente para execuções offline subsequentes.
     */
    suspend fun getOrGenerateAudio(
        context: Context,
        text: String,
        langCode: String,
        blockId: String,
        customApiKey: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isBlank()) {
            return@withContext Result.failure(Exception("Texto para narração em áudio está vazio."))
        }

        val cacheDir = File(context.cacheDir, "openai_block_tts")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val textHash = abs(clean.hashCode())
        val safeBlockId = blockId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val prefix = "${safeBlockId}_${langCode}_${textHash}"

        // Verifica se já existe em cache local permanente
        val cachedFiles = cacheDir.listFiles { _, name -> name.startsWith(prefix) && name.endsWith(".mp3") }
        val existingFile = cachedFiles?.firstOrNull { it.length() > 0L }
        if (existingFile != null) {
            Log.d(TAG, "Audio em cache encontrado: ${existingFile.name}")
            return@withContext Result.success(existingFile)
        }

        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else OpenAiService.getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Configure sua chave da API da OpenAI no aplicativo para gerar narração com voz de IA."))
        }

        // Escolhe de maneira estritamente aleatória entre vozes femininas e masculinas da OpenAI
        val chosenVoice = getRandomVoice()
        val targetFile = File(cacheDir, "${prefix}_${chosenVoice}.mp3")

        try {
            val res = OpenAiService.generateSpeechAudio(
                apiKey = apiKey,
                text = clean,
                voice = chosenVoice,
                cacheKey = "${blockId}_${langCode}"
            )

            if (res.isSuccess) {
                val tempFile = res.getOrThrow()
                tempFile.copyTo(targetFile, overwrite = true)
                try { tempFile.delete() } catch (_: Exception) {}
                Result.success(targetFile)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Falha ao sintetizar voz com OpenAI."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro gerando áudio OpenAI: ${e.message}", e)
            Result.failure(e)
        }
    }
}
