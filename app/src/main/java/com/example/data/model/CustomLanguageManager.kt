package com.example.data.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class CustomLanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false,
    val badgeColorHex: Long = 0xFF6366F1
) {
    val ttsLocale: Locale get() = Locale(code)
    val badgeColor: Color get() = Color(badgeColorHex)
}

object CustomLanguageManager {
    private const val PREFS_NAME = "custom_study_languages_prefs"
    private const val KEY_CUSTOM_LANGS = "custom_langs_json"

    // Catálogo inicial de idiomas mundiais adicionais para inclusão rápida
    val POPULAR_WORLD_LANGUAGES = listOf(
        CustomLanguageItem("ko", "Coreano", "한국어", "🇰🇷", false, 0xFF3B82F6),
        CustomLanguageItem("hi", "Hindi", "हिन्दी", "🇮🇳", false, 0xFFF97316),
        CustomLanguageItem("nl", "Holandês", "Nederlands", "🇳🇱", false, 0xFF06B6D4),
        CustomLanguageItem("pl", "Polonês", "Polski", "🇵🇱", false, 0xFFEC4899),
        CustomLanguageItem("tr", "Turco", "Türkçe", "🇹🇷", false, 0xFFEF4444),
        CustomLanguageItem("sv", "Sueco", "Svenska", "🇸🇪", false, 0xFFEAB308),
        CustomLanguageItem("la", "Latim", "Latina", "🏛️", false, 0xFF8B5CF6),
        CustomLanguageItem("da", "Dinamarquês", "Dansk", "🇩🇰", false, 0xFFDC2626),
        CustomLanguageItem("no", "Norueguês", "Norsk", "🇳🇴", false, 0xFF2563EB),
        CustomLanguageItem("fi", "Finlandês", "Suomi", "🇫🇮", false, 0xFF0284C7),
        CustomLanguageItem("cs", "Tcheco", "Čeština", "🇨🇿", false, 0xFF14B8A6),
        CustomLanguageItem("ro", "Romeno", "Română", "🇷🇴", false, 0xFFF59E0B),
        CustomLanguageItem("hu", "Húngaro", "Magyar", "🇭🇺", false, 0xFF10B981),
        CustomLanguageItem("th", "Tailandês", "ไทย", "🇹🇭", false, 0xFF6366F1),
        CustomLanguageItem("vi", "Vietnamita", "Tiếng Việt", "🇻🇳", false, 0xFFEA580C),
        CustomLanguageItem("id", "Indonésio", "Bahasa Indonesia", "🇮🇩", false, 0xFF0D9488)
    )

    fun getCustomLanguages(context: Context): List<CustomLanguageItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_LANGS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CustomLanguageItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CustomLanguageItem(
                        code = obj.optString("code", ""),
                        displayName = obj.optString("displayName", ""),
                        nativeName = obj.optString("nativeName", ""),
                        flag = obj.optString("flag", "🌐"),
                        isRtl = obj.optBoolean("isRtl", false),
                        badgeColorHex = obj.optLong("badgeColorHex", 0xFF6366F1)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomLanguage(context: Context, item: CustomLanguageItem) {
        val current = getCustomLanguages(context).toMutableList()
        if (current.none { it.code.equals(item.code, ignoreCase = true) }) {
            current.add(item)
            saveList(context, current)
        }
    }

    fun removeCustomLanguage(context: Context, code: String) {
        val current = getCustomLanguages(context).toMutableList()
        current.removeAll { it.code.equals(code, ignoreCase = true) }
        saveList(context, current)
    }

    private fun saveList(context: Context, list: List<CustomLanguageItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("code", item.code)
                put("displayName", item.displayName)
                put("nativeName", item.nativeName)
                put("flag", item.flag)
                put("isRtl", item.isRtl)
                put("badgeColorHex", item.badgeColorHex)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_LANGS, array.toString()).apply()
    }
}
