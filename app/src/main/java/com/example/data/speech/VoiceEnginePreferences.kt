package com.example.data.speech

import android.content.Context
import com.example.ui.components.VoiceEngineChoice

/**
 * Gerenciador de preferências para escolha do motor de voz por idioma:
 * Voz Nativa do Telefone (PHONE) ou Voz Neural OpenAI (OPENAI).
 */
object VoiceEnginePreferences {
    private const val PREFS_NAME = "voice_engine_prefs"
    private const val KEY_PREFIX = "engine_choice_"

    fun getPreferredEngine(context: Context, langCode: String): VoiceEngineChoice? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString("$KEY_PREFIX${langCode.lowercase()}", null) ?: return null
        return try {
            VoiceEngineChoice.valueOf(value)
        } catch (_: Exception) {
            null
        }
    }

    fun setPreferredEngine(context: Context, langCode: String, choice: VoiceEngineChoice) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_PREFIX${langCode.lowercase()}", choice.name).apply()
    }

    fun clearPreference(context: Context, langCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_PREFIX${langCode.lowercase()}").apply()
    }
}
