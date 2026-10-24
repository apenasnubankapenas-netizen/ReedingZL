package com.example.data.legislation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class CachedLawDocument(
    val url: String,
    val title: String,
    val rawHtml: String,
    val articles: List<LawArticleItem>,
    val timestamp: Long = System.currentTimeMillis()
)

data class LawArticleItem(
    val id: String,
    val header: String, // e.g. "Art. 121", "CAPÍTULO I", "TÍTULO II", "§ 1º", "Parágrafo único"
    val content: String,
    val isRevoked: Boolean = false,
    val revisionNote: String? = null, // e.g. "(Redação dada pela Lei nº 13.964, de 2019)"
    val links: List<LawInternalLink> = emptyList()
)

data class LawInternalLink(
    val text: String,
    val targetUrl: String
)

object LawOfflineCacheManager {
    private const val TAG = "LawOfflineCache"
    private const val CACHE_DIR = "legislation_cache"

    private fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun hashUrl(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun getCachedLaw(context: Context, url: String): CachedLawDocument? = withContext(Dispatchers.IO) {
        try {
            val key = hashUrl(url)
            val file = File(getCacheDir(context), "$key.json")
            if (!file.exists()) return@withContext null

            val jsonStr = file.readText()
            val obj = JSONObject(jsonStr)

            val title = obj.optString("title", "Legislação")
            val rawHtml = obj.optString("rawHtml", "")
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

            val articlesArr = obj.optJSONArray("articles") ?: JSONArray()
            val articles = mutableListOf<LawArticleItem>()

            for (i in 0 until articlesArr.length()) {
                val artObj = articlesArr.getJSONObject(i)
                val id = artObj.optString("id", "art_$i")
                val header = artObj.optString("header", "")
                val content = artObj.optString("content", "")
                val isRevoked = artObj.optBoolean("isRevoked", false)
                val revisionNote = if (artObj.has("revisionNote")) artObj.optString("revisionNote") else null

                val linksArr = artObj.optJSONArray("links") ?: JSONArray()
                val links = mutableListOf<LawInternalLink>()
                for (j in 0 until linksArr.length()) {
                    val lObj = linksArr.getJSONObject(j)
                    links.add(LawInternalLink(lObj.getString("text"), lObj.getString("targetUrl")))
                }

                articles.add(
                    LawArticleItem(
                        id = id,
                        header = header,
                        content = content,
                        isRevoked = isRevoked,
                        revisionNote = revisionNote,
                        links = links
                    )
                )
            }

            CachedLawDocument(
                url = url,
                title = title,
                rawHtml = rawHtml,
                articles = articles,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao carregar cache da lei $url: ${e.message}")
            null
        }
    }

    suspend fun saveCachedLaw(
        context: Context,
        url: String,
        title: String,
        rawHtml: String,
        articles: List<LawArticleItem>
    ) = withContext(Dispatchers.IO) {
        try {
            val key = hashUrl(url)
            val file = File(getCacheDir(context), "$key.json")

            val obj = JSONObject()
            obj.put("url", url)
            obj.put("title", title)
            obj.put("rawHtml", rawHtml)
            obj.put("timestamp", System.currentTimeMillis())

            val articlesArr = JSONArray()
            for (art in articles) {
                val artObj = JSONObject()
                artObj.put("id", art.id)
                artObj.put("header", art.header)
                artObj.put("content", art.content)
                artObj.put("isRevoked", art.isRevoked)
                if (art.revisionNote != null) {
                    artObj.put("revisionNote", art.revisionNote)
                }

                val linksArr = JSONArray()
                for (l in art.links) {
                    val lObj = JSONObject()
                    lObj.put("text", l.text)
                    lObj.put("targetUrl", l.targetUrl)
                    linksArr.put(lObj)
                }
                artObj.put("links", linksArr)

                articlesArr.put(artObj)
            }
            obj.put("articles", articlesArr)

            file.writeText(obj.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar cache da lei $url: ${e.message}", e)
        }
    }

    suspend fun getAllCachedLawsList(context: Context): List<CachedLawDocument> = withContext(Dispatchers.IO) {
        try {
            val dir = getCacheDir(context)
            val files = dir.listFiles { f -> f.extension == "json" } ?: return@withContext emptyList()
            files.mapNotNull { f ->
                try {
                    val jsonStr = f.readText()
                    val obj = JSONObject(jsonStr)
                    val url = obj.optString("url", "")
                    val title = obj.optString("title", "Legislação Salva")
                    val timestamp = obj.optLong("timestamp", 0L)
                    CachedLawDocument(url, title, "", emptyList(), timestamp)
                } catch (_: Exception) {
                    null
                }
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
