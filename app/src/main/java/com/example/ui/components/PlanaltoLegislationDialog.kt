package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.legislation.CachedLawDocument
import com.example.data.legislation.LawArticleItem
import com.example.data.legislation.LawOfflineCacheManager
import com.example.data.legislation.PlanaltoLegislationHelper
import com.example.data.legislation.StructuredLawResult
import com.example.data.model.BookQuestion
import com.example.data.model.PageSegmentExplanation
import com.example.data.model.PdfBook
import com.example.data.model.SegmentChatMessage
import com.example.data.openai.OpenAiService
import com.example.data.speech.PdfSpeechManager
import kotlinx.coroutines.launch

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import com.example.data.model.PageDictionaryTerm

enum class LegislationTab {
    NATIVE_READER,
    VADE_MECUM,
    SAVED_LAWS,
    BROWSER
}

enum class LegislationWebviewTheme(val label: String, val shortName: String, val iconText: String) {
    DARK_OLED("Tema Escuro OLED", "OLED", "🌙"),
    NEGATIVE_FILTER("Filtro Negativo", "Negativo", "🔄"),
    MONOCHROME_BW("Preto & Branco", "P&B", "⚪⚫"),
    ORIGINAL_LIGHT("Claro Original", "Claro", "☀️")
}

private fun loadSavedPlanaltoChatMessages(ctx: Context): Map<String, List<SegmentChatMessage>> {
    val prefs = ctx.getSharedPreferences("planalto_chat_cache", Context.MODE_PRIVATE)
    val map = mutableMapOf<String, List<SegmentChatMessage>>()
    prefs.all.forEach { (k, v) ->
        if (v is String && v.isNotBlank()) {
            try {
                val arr = org.json.JSONArray(v)
                val list = mutableListOf<SegmentChatMessage>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        SegmentChatMessage(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            bookId = obj.optInt("bookId", 0),
                            pageNumber = obj.optInt("pageNumber", 1),
                            segmentIndex = obj.optInt("segmentIndex", 0),
                            sender = obj.optString("sender", "USER"),
                            messageText = obj.optString("messageText", ""),
                            isAudioInput = obj.optBoolean("isAudioInput", false),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                map[k] = list
            } catch (_: Exception) {}
        }
    }
    return map
}

private fun savePlanaltoChatMessagesToDisk(ctx: Context, articleId: String, messages: List<SegmentChatMessage>) {
    try {
        val prefs = ctx.getSharedPreferences("planalto_chat_cache", Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        messages.forEach { msg ->
            val obj = org.json.JSONObject().apply {
                put("id", msg.id)
                put("bookId", msg.bookId)
                put("pageNumber", msg.pageNumber)
                put("segmentIndex", msg.segmentIndex)
                put("sender", msg.sender)
                put("messageText", msg.messageText)
                put("isAudioInput", msg.isAudioInput)
                put("timestamp", msg.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(articleId, arr.toString()).apply()
    } catch (_: Exception) {}
}

private fun loadSavedPlanaltoExplanations(ctx: Context): Map<String, PageSegmentExplanation> {
    val prefs = ctx.getSharedPreferences("planalto_explanations_cache", Context.MODE_PRIVATE)
    val map = mutableMapOf<String, PageSegmentExplanation>()
    prefs.all.forEach { (k, v) ->
        if (v is String && v.isNotBlank()) {
            try {
                val obj = org.json.JSONObject(v)
                val exp = PageSegmentExplanation(
                    id = obj.optLong("id", 0),
                    bookId = obj.optInt("bookId", 0),
                    pageNumber = obj.optInt("pageNumber", 1),
                    segmentIndex = obj.optInt("segmentIndex", 0),
                    segmentText = obj.optString("segmentText", ""),
                    explanationPortuguese = obj.optString("explanationPortuguese", ""),
                    explanationSpanish = obj.optString("explanationSpanish", ""),
                    explanationEnglish = obj.optString("explanationEnglish", ""),
                    explanationFrench = obj.optString("explanationFrench", ""),
                    modelUsed = obj.optString("modelUsed", "gpt-4o-mini")
                )
                map[k] = exp
            } catch (_: Exception) {}
        }
    }
    return map
}

private fun savePlanaltoExplanationToDisk(ctx: Context, articleId: String, exp: PageSegmentExplanation) {
    try {
        val prefs = ctx.getSharedPreferences("planalto_explanations_cache", Context.MODE_PRIVATE)
        val obj = org.json.JSONObject().apply {
            put("id", exp.id)
            put("bookId", exp.bookId)
            put("pageNumber", exp.pageNumber)
            put("segmentIndex", exp.segmentIndex)
            put("segmentText", exp.segmentText)
            put("explanationPortuguese", exp.explanationPortuguese)
            put("explanationSpanish", exp.explanationSpanish)
            put("explanationEnglish", exp.explanationEnglish)
            put("explanationFrench", exp.explanationFrench)
            put("modelUsed", exp.modelUsed)
        }
        prefs.edit().putString(articleId, obj.toString()).apply()
    } catch (_: Exception) {}
}

private const val PLANALTO_SESSION_PREFS = "planalto_session_prefs"

private fun getSavedPlanaltoUrl(ctx: Context, defaultUrl: String): String {
    val prefs = ctx.getSharedPreferences(PLANALTO_SESSION_PREFS, Context.MODE_PRIVATE)
    return prefs.getString("last_url", null) ?: defaultUrl
}

private fun getSavedPlanaltoHistory(ctx: Context, initialUrl: String): List<String> {
    val prefs = ctx.getSharedPreferences(PLANALTO_SESSION_PREFS, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("history_stack", null) ?: return listOf(initialUrl)
    return try {
        val arr = org.json.JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val u = arr.getString(i)
            if (u.isNotBlank()) list.add(u)
        }
        if (list.isEmpty()) listOf(initialUrl) else list
    } catch (_: Exception) {
        listOf(initialUrl)
    }
}

private fun getSavedPlanaltoScrollIndex(ctx: Context): Int {
    val prefs = ctx.getSharedPreferences(PLANALTO_SESSION_PREFS, Context.MODE_PRIVATE)
    return prefs.getInt("last_scroll_index", 0)
}

private fun savePlanaltoSession(ctx: Context, url: String, history: List<String>, scrollIndex: Int = 0) {
    try {
        val prefs = ctx.getSharedPreferences(PLANALTO_SESSION_PREFS, Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        history.forEach { arr.put(it) }
        prefs.edit()
            .putString("last_url", url)
            .putString("history_stack", arr.toString())
            .putInt("last_scroll_index", scrollIndex)
            .apply()
    } catch (_: Exception) {}
}

private fun savePlanaltoScrollIndexOnly(ctx: Context, scrollIndex: Int) {
    try {
        val prefs = ctx.getSharedPreferences(PLANALTO_SESSION_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt("last_scroll_index", scrollIndex).apply()
    } catch (_: Exception) {}
}

private fun resetPlanaltoSessionToHome(ctx: Context) {
    try {
        val prefs = ctx.getSharedPreferences(PLANALTO_SESSION_PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    } catch (_: Exception) {}
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlanaltoLegislationDialog(
    initialUrl: String = PlanaltoLegislationHelper.PLANALTO_BASE_URL,
    savedLawBooks: List<PdfBook> = emptyList(),
    speechManager: PdfSpeechManager? = null,
    onSaveQuestions: ((List<BookQuestion>) -> Unit)? = null,
    onRecordQuestionAnswer: ((questionId: Long, selectedIndex: Int, isCorrect: Boolean) -> Unit)? = null,
    onResetQuestionAnswer: ((questionId: Long) -> Unit)? = null,
    onSaveDictionaryTerms: ((List<PageDictionaryTerm>) -> Unit)? = null,
    existingDictionaryTerms: List<PageDictionaryTerm> = emptyList(),
    onSyncLawAsBook: ((title: String, author: String, sampleJson: String, totalPages: Int, category: String, subcategory: String, color: String, sourceUrl: String) -> Unit)? = null,
    onOpenBook: (PdfBook) -> Unit,
    onCreateAndOpenLawBook: (title: String, author: String, sampleJson: String, totalPages: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localSpeechManager = remember { speechManager ?: PdfSpeechManager(context) }

    val savedUrl = remember { getSavedPlanaltoUrl(context, initialUrl) }
    val savedHistory = remember { getSavedPlanaltoHistory(context, initialUrl) }
    val savedScrollIdx = remember { getSavedPlanaltoScrollIndex(context) }

    var activeTab by remember { mutableStateOf(LegislationTab.NATIVE_READER) }
    var webviewTheme by remember { mutableStateOf(LegislationWebviewTheme.DARK_OLED) }
    var currentUrl by remember { mutableStateOf(savedUrl) }
    var urlInput by remember { mutableStateOf(savedUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBackWeb by remember { mutableStateOf(false) }
    var canGoForwardWeb by remember { mutableStateOf(false) }
    var isPageLoadingWeb by remember { mutableStateOf(false) }

    // Pesquisa dentro do WebView
    var webSearchQuery by remember { mutableStateOf("") }

    // Pilha de histórico nativo de navegação entre leis (persiste em sequência)
    var lawHistoryStack by remember { mutableStateOf(savedHistory) }

    // Estado do Leitor Nativo Estruturado
    var currentStructuredLaw by remember { mutableStateOf<StructuredLawResult.Success?>(null) }
    var isLoadingStructuredLaw by remember { mutableStateOf(true) }
    var structuredLawError by remember { mutableStateOf<String?>(null) }

    // Controles de Visualização & Leitura
    var fontSizeSp by remember { mutableFloatStateOf(14f) }
    var showRevokedArticles by remember { mutableStateOf(true) }
    var articleSearchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Reprodução TTS & Karaokê Sincronizado
    val isSpeaking by localSpeechManager.isSpeaking.collectAsState()
    val activeWordIndex by localSpeechManager.activeWordIndex.collectAsState()
    val activeTargetId by localSpeechManager.activeTargetId.collectAsState()
    var activeSpeakingArticleId by remember { mutableStateOf<String?>(null) }
    var speechSpeed by remember { mutableFloatStateOf(1.0f) }

    // ==========================================
    // REELS INTEGRATION: CHAT OPENAI COM ÁUDIO
    // ==========================================
    var activeChatArticle by remember { mutableStateOf<LawArticleItem?>(null) }
    var chatMessagesMap by remember { mutableStateOf(loadSavedPlanaltoChatMessages(context)) }
    var isChatLoading by remember { mutableStateOf(false) }
    var chatErrorMessage by remember { mutableStateOf<String?>(null) }
    var isChatMinimized by remember { mutableStateOf(false) }

    // ==========================================
    // REELS INTEGRATION: EXPLICAÇÃO COM IA (4 IDIOMAS & CONCURSO)
    // ==========================================
    var activeExplanationArticle by remember { mutableStateOf<LawArticleItem?>(null) }
    var articleExplanationsMap by remember { mutableStateOf(loadSavedPlanaltoExplanations(context)) }
    var generatingExplanationArticleId by remember { mutableStateOf<String?>(null) }

    // ==========================================
    // REELS INTEGRATION: QUESTÕES / QUIZ DE PROVAS
    // ==========================================
    var activeQuizArticle by remember { mutableStateOf<LawArticleItem?>(null) }
    var articleQuestionsMap by remember { mutableStateOf(mapOf<String, List<BookQuestion>>()) }
    var isGeneratingQuiz by remember { mutableStateOf(false) }
    var quizErrorMessage by remember { mutableStateOf<String?>(null) }

    // ==========================================
    // REELS INTEGRATION: DICIONÁRIO JURÍDICO
    // ==========================================
    var activeDictionaryTerm by remember { mutableStateOf<String?>(null) }
    var dictionaryDefinition by remember { mutableStateOf<String?>(null) }
    var isLoadingDictionary by remember { mutableStateOf(false) }

    // Vade Mecum e Offline Cache
    var searchLawQuery by remember { mutableStateOf("") }
    var cachedOfflineLawsList by remember { mutableStateOf<List<CachedLawDocument>>(emptyList()) }

    fun applyWebviewTheme(wv: WebView?, theme: LegislationWebviewTheme) {
        val js = when (theme) {
            LegislationWebviewTheme.DARK_OLED -> PlanaltoLegislationHelper.DARK_MODE_INJECTION_JS
            LegislationWebviewTheme.NEGATIVE_FILTER -> PlanaltoLegislationHelper.NEGATIVE_FILTER_JS
            LegislationWebviewTheme.MONOCHROME_BW -> PlanaltoLegislationHelper.HIGH_CONTRAST_BW_JS
            LegislationWebviewTheme.ORIGINAL_LIGHT -> PlanaltoLegislationHelper.RESET_THEME_JS
        }
        wv?.evaluateJavascript(js, null)
    }

    fun loadLawStructured(targetUrl: String, addToHistory: Boolean = true, forceRefresh: Boolean = false) {
        isLoadingStructuredLaw = true
        structuredLawError = null
        currentUrl = targetUrl
        urlInput = targetUrl
        activeTab = LegislationTab.NATIVE_READER

        if (addToHistory && (lawHistoryStack.isEmpty() || lawHistoryStack.last() != targetUrl)) {
            lawHistoryStack = lawHistoryStack + targetUrl
        }
        savePlanaltoSession(context, targetUrl, lawHistoryStack, 0)

        scope.launch {
            val result = PlanaltoLegislationHelper.fetchStructuredLawWithCache(context, targetUrl, forceRefresh)
            when (result) {
                is StructuredLawResult.Success -> {
                    currentStructuredLaw = result
                    isLoadingStructuredLaw = false
                    structuredLawError = null
                    // Atualiza lista de salvos offline no banco local
                    cachedOfflineLawsList = LawOfflineCacheManager.getAllCachedLawsList(context)

                    // Sincroniza automaticamente a lei como livro para o modo Reels
                    val classification = PlanaltoLegislationHelper.classifyLawDocument(result.title, targetUrl)
                    onSyncLawAsBook?.invoke(
                        result.title,
                        "Presidência da República / Planalto",
                        result.sampleContentJson,
                        result.pages.size,
                        classification.category,
                        classification.subcategory,
                        classification.categoryColor,
                        targetUrl
                    )
                }
                is StructuredLawResult.Error -> {
                    isLoadingStructuredLaw = false
                    structuredLawError = result.message
                }
            }
        }
    }

    // Inicialização direta onde o usuário parou ou na página civil_03
    LaunchedEffect(Unit) {
        loadLawStructured(savedUrl, addToHistory = false)
        cachedOfflineLawsList = LawOfflineCacheManager.getAllCachedLawsList(context)
    }

    // Restaura a posição onde o usuário parou na leitura da lei
    LaunchedEffect(currentStructuredLaw) {
        if (savedScrollIdx > 0 && currentUrl == savedUrl) {
            val count = currentStructuredLaw?.articles?.size ?: 0
            if (savedScrollIdx < count) {
                listState.scrollToItem(savedScrollIdx)
            }
        }
    }

    // Grava a rolagem em tempo real para lembrar onde parou
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex > 0) {
            savePlanaltoScrollIndexOnly(context, listState.firstVisibleItemIndex)
        }
    }

    val filteredPresets = remember(searchLawQuery) {
        if (searchLawQuery.isBlank()) {
            PlanaltoLegislationHelper.POPULAR_LAWS_CATALOG
        } else {
            PlanaltoLegislationHelper.POPULAR_LAWS_CATALOG.filter {
                it.title.contains(searchLawQuery, ignoreCase = true) ||
                it.shortName.contains(searchLawQuery, ignoreCase = true) ||
                it.description.contains(searchLawQuery, ignoreCase = true) ||
                it.category.contains(searchLawQuery, ignoreCase = true)
            }
        }
    }

    val displayedArticles = remember(currentStructuredLaw, showRevokedArticles, articleSearchQuery) {
        val all = currentStructuredLaw?.articles ?: emptyList()
        val cleanQ = articleSearchQuery.trim()
        if (cleanQ.isBlank()) {
            all.filter { showRevokedArticles || !it.isRevoked }
        } else {
            val qDigits = cleanQ.filter { it.isDigit() }
            all.filter { art ->
                val matchesRevoked = showRevokedArticles || !art.isRevoked
                if (!matchesRevoked) return@filter false

                val headerLower = art.header.lowercase()
                val contentLower = art.content.lowercase()
                val noteLower = art.revisionNote?.lowercase() ?: ""

                // 1. Busca direta (case insensitive)
                if (headerLower.contains(cleanQ.lowercase()) ||
                    contentLower.contains(cleanQ.lowercase()) ||
                    noteLower.contains(cleanQ.lowercase())
                ) {
                    return@filter true
                }

                // 2. Busca por número de artigo (ex: "5", "art. 5", "artigo 5", "121", "1021", etc.)
                if (qDigits.isNotEmpty()) {
                    val headerDigits = art.header.filter { it.isDigit() }
                    if (headerDigits == qDigits) return@filter true

                    val regexArt = Regex("(?i)\\b(art(?:igo)?\\.?\\s*0*$qDigits\\b)")
                    if (regexArt.containsMatchIn(art.header) || regexArt.containsMatchIn(art.content.take(80))) {
                        return@filter true
                    }

                    val regexNum = Regex("\\b0*$qDigits[º°ªo\\.]?\\b")
                    if (regexNum.containsMatchIn(art.header)) {
                        return@filter true
                    }
                }

                // 3. Busca por múltiplas palavras
                val words = cleanQ.lowercase().split("\\s+".toRegex()).filter { it.length > 1 }
                if (words.size > 1) {
                    val combined = "$headerLower $contentLower $noteLower"
                    if (words.all { combined.contains(it) }) {
                        return@filter true
                    }
                }

                false
            }
        }
    }

    fun playTtsForArticle(article: LawArticleItem) {
        val targetId = "art_${article.id}"
        if (isSpeaking && activeTargetId == targetId) {
            localSpeechManager.stop()
            activeSpeakingArticleId = null
        } else {
            activeSpeakingArticleId = article.id
            localSpeechManager.setSpeed(speechSpeed)
            val textToSpeak = "${article.header}. ${article.content}".replace("~~", "").trim()
            localSpeechManager.startReadingTarget(targetId, textToSpeak)
        }
    }

    fun seekWordForArticle(article: LawArticleItem, wordIndex: Int) {
        val targetId = "art_${article.id}"
        activeSpeakingArticleId = article.id
        val textToSpeak = "${article.header}. ${article.content}".replace("~~", "").trim()
        localSpeechManager.setSpeed(speechSpeed)
        localSpeechManager.speakFromWordInTarget(targetId, textToSpeak, wordIndex)
    }

    fun stopTts() {
        localSpeechManager.stop()
        activeSpeakingArticleId = null
    }

    // Ação: Explicação com IA com 3 Exemplos Práticos de Personagens (Vilela, Janinho, Márcia, Parroca, Lorenzzo)
    fun requestExplanationForArticle(article: LawArticleItem) {
        if (articleExplanationsMap.containsKey(article.id)) return

        generatingExplanationArticleId = article.id
        scope.launch {
            val title = currentStructuredLaw?.title ?: "Legislação Planalto"
            val key = OpenAiService.getSavedApiKey(context)
            val res = OpenAiService.generateLawExplanationWithCharacters(
                apiKey = key,
                model = "gpt-4o-mini",
                lawTitle = title,
                articleHeader = article.header,
                articleText = article.content.replace("~~", "")
            )
            generatingExplanationArticleId = null
            res.onSuccess { exp ->
                articleExplanationsMap = articleExplanationsMap + (article.id to exp)
                savePlanaltoExplanationToDisk(context, article.id, exp)
            }.onFailure { err ->
                Toast.makeText(context, "Erro ao gerar explicação: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Ação: Chat OpenAI com suporte nativo a gravação de áudio e voz
    fun openOpenAiChatForArticle(article: LawArticleItem) {
        activeChatArticle = article
        isChatMinimized = false
        if (!chatMessagesMap.containsKey(article.id)) {
            val introMsg = SegmentChatMessage(
                id = System.currentTimeMillis(),
                bookId = 0,
                pageNumber = 1,
                segmentIndex = 0,
                sender = "ASSISTANT",
                messageText = "Olá! Sou seu tutor jurídico de IA conectado diretamente à API da OpenAI. Você pode digitar ou enviar áudios com sua voz para tirar dúvidas sobre o ${article.header}!",
                isAudioInput = false,
                timestamp = System.currentTimeMillis()
            )
            val initialList = listOf(introMsg)
            chatMessagesMap = chatMessagesMap + (article.id to initialList)
            savePlanaltoChatMessagesToDisk(context, article.id, initialList)
        }
    }

    fun handleSendOpenAiChatMessage(question: String, isAudio: Boolean) {
        val art = activeChatArticle ?: return
        val currentList = chatMessagesMap[art.id] ?: emptyList()
        val userMsg = SegmentChatMessage(
            id = System.currentTimeMillis(),
            bookId = 0,
            pageNumber = 1,
            segmentIndex = 0,
            sender = "USER",
            messageText = question,
            isAudioInput = isAudio,
            timestamp = System.currentTimeMillis()
        )
        val updatedWithUser = currentList + userMsg
        chatMessagesMap = chatMessagesMap + (art.id to updatedWithUser)
        savePlanaltoChatMessagesToDisk(context, art.id, updatedWithUser)
        isChatLoading = true
        chatErrorMessage = null

        scope.launch {
            val title = currentStructuredLaw?.title ?: "Legislação"
            val key = OpenAiService.getSavedApiKey(context)
            val historyPairs = updatedWithUser.map { (if (it.sender == "USER") "user" else "assistant") to it.messageText }
            val result = OpenAiService.sendSegmentChatMessage(
                apiKey = key,
                bookTitle = title,
                bookAuthor = "Presidência da República / Planalto",
                pageNumber = 1,
                segmentText = "${art.header}: ${art.content.replace("~~", "")}",
                history = historyPairs,
                userQuestion = question
            )
            isChatLoading = false
            result.onSuccess { aiText ->
                val assistantMsg = SegmentChatMessage(
                    id = System.currentTimeMillis(),
                    bookId = 0,
                    pageNumber = 1,
                    segmentIndex = 0,
                    sender = "ASSISTANT",
                    messageText = aiText,
                    isAudioInput = false,
                    timestamp = System.currentTimeMillis()
                )
                val finalList = updatedWithUser + assistantMsg
                chatMessagesMap = chatMessagesMap + (art.id to finalList)
                savePlanaltoChatMessagesToDisk(context, art.id, finalList)
            }.onFailure { err ->
                chatErrorMessage = err.localizedMessage ?: "Erro ao consultar OpenAI"
            }
        }
    }

    // Ação: Questões / Quiz de Concurso com OpenAI e Sincronização Global com Reels
    fun requestQuizForArticle(article: LawArticleItem) {
        activeQuizArticle = article
        if (articleQuestionsMap.containsKey(article.id)) return

        isGeneratingQuiz = true
        quizErrorMessage = null
        scope.launch {
            val title = currentStructuredLaw?.title ?: "Legislação"
            val key = OpenAiService.getSavedApiKey(context)
            val result = OpenAiService.generateBookQuestions(
                apiKey = key,
                bookId = 0,
                bookTitle = title,
                startPage = 1,
                endPage = 1,
                contentSnippet = "${article.header}: ${article.content.replace("~~", "")}",
                institution = "FGV / CESPE / VUNESP",
                difficulty = "MEDIUM",
                questionCount = 3
            )
            isGeneratingQuiz = false
            result.onSuccess { questions ->
                if (questions.isNotEmpty()) {
                    articleQuestionsMap = articleQuestionsMap + (article.id to questions)
                    onSaveQuestions?.invoke(questions)
                    Toast.makeText(context, "🎯 ${questions.size} questões sincronizadas com o banco dos Reels!", Toast.LENGTH_SHORT).show()
                } else {
                    quizErrorMessage = "Não foi possível gerar questões para este artigo."
                }
            }.onFailure { err ->
                quizErrorMessage = err.localizedMessage ?: "Erro ao gerar questões com OpenAI."
            }
        }
    }

    // Ação: Dicionário Jurídico - Extração de Termos para Dicionário Offline sem duplicatas
    fun extractAndSaveDictionaryTermsForArticle(article: LawArticleItem) {
        isLoadingDictionary = true
        scope.launch {
            val key = OpenAiService.getSavedApiKey(context)
            val title = currentStructuredLaw?.title ?: "Legislação"
            val snippet = "${article.header}: ${article.content.replace("~~", "")}"
            val result = OpenAiService.extractUniversalDictionaryTerms(
                apiKey = key,
                model = "gpt-4o-mini",
                bookId = 0,
                pageNumber = 1,
                bookTitle = title,
                pageText = snippet
            )
            isLoadingDictionary = false
            result.onSuccess { terms ->
                val existingSet = existingDictionaryTerms.map { it.term.trim().lowercase() }.toSet()
                val uniqueNewTerms = terms.filter { it.term.trim().lowercase() !in existingSet }
                if (uniqueNewTerms.isNotEmpty()) {
                    onSaveDictionaryTerms?.invoke(uniqueNewTerms)
                    Toast.makeText(context, "📖 ${uniqueNewTerms.size} novos termos jurídicos adicionados ao Dicionário Offline!", Toast.LENGTH_LONG).show()
                } else if (terms.isNotEmpty()) {
                    Toast.makeText(context, "📖 Termos jurídicos já catalogados no Dicionário Offline.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nenhum termo técnico inédito nesta parte.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { err ->
                Toast.makeText(context, "Erro no dicionário: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Ação: Consulta Dicionário Individual
    fun lookupDictionaryTerm(term: String, contextSentence: String) {
        activeDictionaryTerm = term
        isLoadingDictionary = true
        dictionaryDefinition = null
        scope.launch {
            val key = OpenAiService.getSavedApiKey(context)
            val res = OpenAiService.explainTermOrSentence(
                context = context,
                termOrSentence = term,
                fullContext = contextSentence.replace("~~", ""),
                customApiKey = key
            )
            isLoadingDictionary = false
            dictionaryDefinition = res.getOrElse { "Definição jurídica: Termo relevante da doutrina e hermenêutica processual." }
        }
    }

    // Navegação Voltar Nativa
    BackHandler {
        when {
            activeDictionaryTerm != null -> activeDictionaryTerm = null
            activeQuizArticle != null -> activeQuizArticle = null
            activeExplanationArticle != null -> activeExplanationArticle = null
            activeChatArticle != null -> activeChatArticle = null
            activeTab == LegislationTab.BROWSER && webViewInstance?.canGoBack() == true -> webViewInstance?.goBack()
            activeTab != LegislationTab.NATIVE_READER -> activeTab = LegislationTab.NATIVE_READER
            lawHistoryStack.size > 1 -> {
                val newStack = lawHistoryStack.dropLast(1)
                lawHistoryStack = newStack
                loadLawStructured(newStack.last(), addToHistory = false)
            }
            else -> {
                stopTts()
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            stopTts()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color(0xFF09090B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // ==========================================
                // CABEÇALHO NATIVO ELEGANTE (ZERO CARA DE NAVEGADOR)
                // ==========================================
                Surface(
                    color = Color(0xFF121215),
                    border = BorderStroke(0.8.dp, Color(0xFF27272A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (lawHistoryStack.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val newStack = lawHistoryStack.dropLast(1)
                                            lawHistoryStack = newStack
                                            loadLawStructured(newStack.last(), addToHistory = false)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Voltar norma anterior",
                                            tint = Color(0xFFF59E0B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFD97706).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("⚖️", fontSize = 15.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentStructuredLaw?.title?.take(38) ?: "Legislação Planalto (Civil 03)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (currentStructuredLaw?.isFromCache == true) Color(0xFF059669).copy(alpha = 0.2f) else Color(0xFFD97706).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (currentStructuredLaw?.isFromCache == true) "⚡ Offline Salvo" else "🟢 Conectado Planalto",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentStructuredLaw?.isFromCache == true) Color(0xFF34D399) else Color(0xFFF59E0B),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${currentStructuredLaw?.totalArticles ?: 0} Artigos",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFFA1A1AA)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Botão Início Civil 03
                                IconButton(
                                    onClick = {
                                        resetPlanaltoSessionToHome(context)
                                        lawHistoryStack = listOf(PlanaltoLegislationHelper.PLANALTO_BASE_URL)
                                        loadLawStructured(PlanaltoLegislationHelper.PLANALTO_BASE_URL, addToHistory = false)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = "Ir para Início Planalto",
                                        tint = Color(0xFFF59E0B)
                                    )
                                }

                                // Botão Salvar como Livro no App
                                if (currentStructuredLaw != null) {
                                    IconButton(
                                        onClick = {
                                            val cur = currentStructuredLaw ?: return@IconButton
                                            onCreateAndOpenLawBook(
                                                cur.title,
                                                "Presidência da República / Planalto",
                                                cur.sampleContentJson,
                                                cur.pages.size
                                            )
                                            Toast.makeText(context, "📖 Lei importada como livro nos seus Reels com ${cur.pages.size} páginas!", Toast.LENGTH_LONG).show()
                                            stopTts()
                                            onDismiss()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.BookmarkAdd,
                                            contentDescription = "Salvar como Livro nos Reels",
                                            tint = Color(0xFFFCD34D)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        stopTts()
                                        onDismiss()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ABAS NATIVAS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentUrl.trimEnd('/') == PlanaltoLegislationHelper.PLANALTO_BASE_URL.trimEnd('/')) Color(0xFFD97706) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                modifier = Modifier.clickable {
                                    loadLawStructured(PlanaltoLegislationHelper.PLANALTO_BASE_URL, addToHistory = true)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        tint = if (currentUrl.trimEnd('/') == PlanaltoLegislationHelper.PLANALTO_BASE_URL.trimEnd('/')) Color.Black else Color(0xFFFCD34D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🏛️ Civil 03 (Início)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (currentUrl.trimEnd('/') == PlanaltoLegislationHelper.PLANALTO_BASE_URL.trimEnd('/')) Color.Black else Color(0xFFFCD34D)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTab == LegislationTab.NATIVE_READER && currentUrl.trimEnd('/') != PlanaltoLegislationHelper.PLANALTO_BASE_URL.trimEnd('/')) Color(0xFFD97706) else Color(0xFF27272A),
                                modifier = Modifier.clickable { activeTab = LegislationTab.NATIVE_READER }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = if (activeTab == LegislationTab.NATIVE_READER && currentUrl.trimEnd('/') != PlanaltoLegislationHelper.PLANALTO_BASE_URL.trimEnd('/')) Color.Black else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "📖 Leitor Nativo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTab == LegislationTab.NATIVE_READER && currentUrl.trimEnd('/') != PlanaltoLegislationHelper.PLANALTO_BASE_URL.trimEnd('/')) Color.Black else Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTab == LegislationTab.VADE_MECUM) Color(0xFFD97706) else Color(0xFF27272A),
                                modifier = Modifier.clickable { activeTab = LegislationTab.VADE_MECUM }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (activeTab == LegislationTab.VADE_MECUM) Color.Black else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "⚡ Vade Mecum (${PlanaltoLegislationHelper.POPULAR_LAWS_CATALOG.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTab == LegislationTab.VADE_MECUM) Color.Black else Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTab == LegislationTab.SAVED_LAWS) Color(0xFFD97706) else Color(0xFF27272A),
                                modifier = Modifier.clickable { activeTab = LegislationTab.SAVED_LAWS }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = if (activeTab == LegislationTab.SAVED_LAWS) Color.Black else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "💾 Salvos Offline (${cachedOfflineLawsList.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTab == LegislationTab.SAVED_LAWS) Color.Black else Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTab == LegislationTab.BROWSER) Color(0xFFD97706) else Color(0xFF27272A),
                                modifier = Modifier.clickable { activeTab = LegislationTab.BROWSER }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (activeTab == LegislationTab.BROWSER) Color.Black else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🌐 Web Original",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTab == LegislationTab.BROWSER) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // CONTEÚDO PRINCIPAL
                // ==========================================
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        LegislationTab.NATIVE_READER -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // BARRA SUPERIOR DE CONTROLES NATIVOS (ZOOM, FONTE, REVOGADOS, BUSCA)
                                Surface(
                                    color = Color(0xFF18181B),
                                    border = BorderStroke(0.6.dp, Color(0xFF27272A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // CONTROLE DE FONTE / ZOOM A- e A+
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF27272A),
                                                    border = BorderStroke(0.8.dp, Color(0xFF3F3F46)),
                                                    modifier = Modifier.clickable {
                                                        fontSizeSp = (fontSizeSp - 1f).coerceAtLeast(10f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = "A-",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF3F3F46)
                                                ) {
                                                    Text(
                                                        text = "${fontSizeSp.toInt()}sp",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFFCD34D),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF27272A),
                                                    border = BorderStroke(0.8.dp, Color(0xFF3F3F46)),
                                                    modifier = Modifier.clickable {
                                                        fontSizeSp = (fontSizeSp + 1f).coerceAtMost(36f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = "A+",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            // FILTRO: OCULTAR / MOSTRAR REVOGADOS
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (showRevokedArticles) Color(0xFFB45309).copy(alpha = 0.25f) else Color(0xFF059669).copy(alpha = 0.25f),
                                                border = BorderStroke(0.8.dp, if (showRevokedArticles) Color(0xFFF59E0B) else Color(0xFF34D399)),
                                                modifier = Modifier.clickable {
                                                    showRevokedArticles = !showRevokedArticles
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        if (showRevokedArticles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = if (showRevokedArticles) Color(0xFFFCD34D) else Color(0xFF34D399),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (showRevokedArticles) "Revogados Visíveis" else "Só Vigentes (Limpo)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (showRevokedArticles) Color(0xFFFCD34D) else Color(0xFF34D399)
                                                    )
                                                }
                                            }

                                            // BOTÃO RECARREGAR ONLINE
                                            IconButton(
                                                onClick = { loadLawStructured(currentUrl, addToHistory = false, forceRefresh = true) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Atualizar Online", tint = Color.Gray, modifier = Modifier.size(15.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // CAMPO DE BUSCA DE ARTIGO NA LEI
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF09090B),
                                            border = BorderStroke(1.2.dp, if (articleSearchQuery.isNotBlank()) Color(0xFFF59E0B) else Color(0xFF3F3F46)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = if (articleSearchQuery.isNotBlank()) Color(0xFFF59E0B) else Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(19.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(modifier = Modifier.weight(1f)) {
                                                    if (articleSearchQuery.isEmpty()) {
                                                        Text(
                                                            text = "Pesquisar por artigo, número ou tema (ex: Art. 5º, 121, homicídio)...",
                                                            fontSize = 12.5.sp,
                                                            color = Color(0xFF71717A)
                                                        )
                                                    }
                                                    BasicTextField(
                                                        value = articleSearchQuery,
                                                        onValueChange = { articleSearchQuery = it },
                                                        singleLine = true,
                                                        textStyle = TextStyle(
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.White
                                                        ),
                                                        cursorBrush = SolidColor(Color(0xFFF59E0B)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                if (articleSearchQuery.isNotBlank()) {
                                                    IconButton(
                                                        onClick = { articleSearchQuery = "" },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Clear,
                                                            contentDescription = "Limpar busca",
                                                            tint = Color(0xFFF59E0B),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (articleSearchQuery.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (displayedArticles.isNotEmpty()) "🔍 ${displayedArticles.size} artigo(s) encontrado(s)" else "⚠️ Nenhum artigo encontrado para \"$articleSearchQuery\"",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (displayedArticles.isNotEmpty()) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                                )
                                                Text(
                                                    text = "Limpar",
                                                    fontSize = 10.sp,
                                                    color = Color.LightGray,
                                                    modifier = Modifier.clickable { articleSearchQuery = "" }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isLoadingStructuredLaw) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text("Baixando e estruturando artigos no telefone...", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Salvando em banco offline e separando normas revogadas", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                } else if (structuredLawError != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(structuredLawError ?: "Erro ao carregar", color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = { loadLawStructured(currentUrl, addToHistory = false, forceRefresh = true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                            ) {
                                                Text("Tentar Novamente", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    // LISTA DE ARTIGOS NATIVOS E ESTRUTURADOS COM TODAS AS FUNÇÕES REELS
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        items(displayedArticles, key = { it.id }) { article ->
                                            val isSpeakingThis = isSpeaking && activeTargetId == "art_${article.id}"
                                            val explanation = articleExplanationsMap[article.id]
                                            val isGenThis = generatingExplanationArticleId == article.id

                                            NativeLawArticleCardWithReelsTools(
                                                article = article,
                                                fontSizeSp = fontSizeSp,
                                                isSpeaking = isSpeakingThis,
                                                activeWordIndex = activeWordIndex,
                                                activeTargetId = activeTargetId,
                                                dictionaryTerms = existingDictionaryTerms,
                                                explanation = explanation,
                                                isGeneratingExplanation = isGenThis,
                                                onPlayTts = {
                                                    if (isSpeakingThis) {
                                                        stopTts()
                                                    } else {
                                                        playTtsForArticle(article)
                                                    }
                                                },
                                                onWordClick = { wordIndex ->
                                                    seekWordForArticle(article, wordIndex)
                                                },
                                                onDictionaryTermClick = { term ->
                                                    lookupDictionaryTerm(term.term, article.content)
                                                },
                                                onExplainWithAi = { requestExplanationForArticle(article) },
                                                onChatWithAi = { openOpenAiChatForArticle(article) },
                                                onQuizWithAi = { requestQuizForArticle(article) },
                                                onExtractDictionary = { extractAndSaveDictionaryTermsForArticle(article) },
                                                onLookupSingleTerm = { term -> lookupDictionaryTerm(term, article.content) },
                                                onNavigateToLink = { targetUrl -> loadLawStructured(targetUrl, addToHistory = true) },
                                                onSpeakExplanation = { lang, text ->
                                                    localSpeechManager.speakFromWordInTarget("expl_${article.id}", text, 0)
                                                },
                                                onSpeakExplanationWord = { lang, fullText, wordOffset ->
                                                    localSpeechManager.speakFromWordInTarget("expl_${article.id}", fullText, wordOffset)
                                                },
                                                onStopExplanationSpeaking = {
                                                    localSpeechManager.stopTarget("expl_${article.id}")
                                                }
                                            )
                                        }
                                    }
                                }

                                // PLAYER DE ÁUDIO TTS FLUTUANTE INFERIOR
                                if (isSpeaking || activeSpeakingArticleId != null) {
                                    Surface(
                                        color = Color(0xFF18181B),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Voz Neural Lendo Legislação",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "Velocidade: ${speechSpeed}x",
                                                        fontSize = 9.5.sp,
                                                        color = Color(0xFFFCD34D)
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF27272A),
                                                    modifier = Modifier.clickable {
                                                        speechSpeed = when (speechSpeed) {
                                                            1.0f -> 1.25f
                                                            1.25f -> 1.5f
                                                            1.5f -> 1.75f
                                                            1.75f -> 2.0f
                                                            else -> 1.0f
                                                        }
                                                        localSpeechManager.setSpeed(speechSpeed)
                                                    }
                                                ) {
                                                    Text(
                                                        text = "${speechSpeed}x",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                    )
                                                }

                                                IconButton(onClick = { stopTts() }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Stop, contentDescription = "Parar", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LegislationTab.VADE_MECUM -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchLawQuery,
                                    onValueChange = { searchLawQuery = it },
                                    placeholder = { Text("Buscar norma (ex: CF/88, Penal, 8.112, CLT, Licitações)...", fontSize = 12.sp, color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (searchLawQuery.isNotBlank()) {
                                            IconButton(onClick = { searchLawQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF27272A),
                                        focusedContainerColor = Color(0xFF18181B),
                                        unfocusedContainerColor = Color(0xFF18181B),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "VADE MECUM INTERATIVO (${filteredPresets.size} NORMAS FUNDAMENTAIS)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredPresets, key = { it.id }) { law ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF18181B),
                                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Text(law.emoji, fontSize = 20.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = law.shortName,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                            Text(
                                                                text = law.category,
                                                                fontSize = 10.5.sp,
                                                                color = Color(0xFFF59E0B)
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFD97706).copy(alpha = 0.15f),
                                                        border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                                                    ) {
                                                        Text(
                                                            text = law.importanceTag,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFCD34D),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = law.title,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFE4E4E7),
                                                    lineHeight = 15.sp
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = law.description,
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFFA1A1AA),
                                                    lineHeight = 14.sp
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = { loadLawStructured(law.planaltoUrl, addToHistory = true) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFD97706),
                                                            contentColor = Color.Black
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text("Abrir no Leitor Nativo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LegislationTab.SAVED_LAWS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "LEIS BAIXADAS E SALVAS NO TELEFONE (${cachedOfflineLawsList.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (cachedOfflineLawsList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.OfflinePin, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("Nenhuma lei salva offline ainda.", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Toda lei que você abre é salva automaticamente para ler sem internet.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(cachedOfflineLawsList, key = { it.url }) { doc ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF18181B),
                                                border = BorderStroke(1.dp, Color(0xFF27272A)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(doc.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Text("⚡ Disponível 100% Offline", fontSize = 10.sp, color = Color(0xFF34D399))
                                                    }

                                                    Button(
                                                        onClick = { loadLawStructured(doc.url, addToHistory = true) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.Black),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Ler", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LegislationTab.BROWSER -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    color = Color(0xFF18181B),
                                    border = BorderStroke(0.6.dp, Color(0xFF27272A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { webViewInstance?.goBack() },
                                            enabled = canGoBackWeb,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = if (canGoBackWeb) Color.White else Color.Gray)
                                        }
                                        IconButton(
                                            onClick = { webViewInstance?.goForward() },
                                            enabled = canGoForwardWeb,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avançar", tint = if (canGoForwardWeb) Color.White else Color.Gray)
                                        }
                                        IconButton(
                                            onClick = { webViewInstance?.reload() },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = Color.White)
                                        }

                                        OutlinedTextField(
                                            value = urlInput,
                                            onValueChange = { urlInput = it },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFFF59E0B),
                                                unfocusedBorderColor = Color(0xFF3F3F46),
                                                focusedContainerColor = Color(0xFF09090B),
                                                unfocusedContainerColor = Color(0xFF09090B)
                                            ),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                            keyboardActions = KeyboardActions(onGo = {
                                                val target = urlInput.trim()
                                                val finalUrl = if (target.startsWith("http://") || target.startsWith("https://")) target else "https://www.planalto.gov.br/ccivil_03/$target"
                                                currentUrl = finalUrl
                                                webViewInstance?.loadUrl(finalUrl)
                                            })
                                        )
                                    }

                                    // BARRA DE PESQUISA EM PÁGINA (WEBVIEW)
                                    Surface(
                                        color = Color(0xFF18181B),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(0.6.dp, Color(0xFF27272A))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            OutlinedTextField(
                                                value = webSearchQuery,
                                                onValueChange = { newQ ->
                                                    webSearchQuery = newQ
                                                    if (newQ.isNotBlank()) {
                                                        webViewInstance?.findAllAsync(newQ)
                                                    } else {
                                                        webViewInstance?.clearMatches()
                                                    }
                                                },
                                                placeholder = { Text("Pesquisar na página web (ex: Art. 5º, pena)...", color = Color.Gray, fontSize = 11.sp) },
                                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 11.5.sp),
                                                modifier = Modifier.weight(1f).height(46.dp),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent,
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent
                                                )
                                            )
                                            if (webSearchQuery.isNotBlank()) {
                                                IconButton(
                                                    onClick = { webViewInstance?.findNext(false) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Anterior", tint = Color.LightGray, modifier = Modifier.size(15.dp))
                                                }
                                                IconButton(
                                                    onClick = { webViewInstance?.findNext(true) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Próximo", tint = Color.LightGray, modifier = Modifier.size(15.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        webSearchQuery = ""
                                                        webViewInstance?.clearMatches()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isPageLoadingWeb) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF59E0B), trackColor = Color(0xFF27272A))
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                                setBackgroundColor(android.graphics.Color.BLACK)
                                                settings.apply {
                                                    javaScriptEnabled = true
                                                    domStorageEnabled = true
                                                    loadWithOverviewMode = true
                                                    useWideViewPort = true
                                                    builtInZoomControls = true
                                                    displayZoomControls = false
                                                    cacheMode = WebSettings.LOAD_DEFAULT
                                                    userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                                                }
                                                webViewClient = object : WebViewClient() {
                                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                        super.onPageStarted(view, url, favicon)
                                                        isPageLoadingWeb = true
                                                        canGoBackWeb = view?.canGoBack() == true
                                                        canGoForwardWeb = view?.canGoForward() == true
                                                        applyWebviewTheme(view, webviewTheme)
                                                    }
                                                    override fun onPageFinished(view: WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        isPageLoadingWeb = false
                                                        canGoBackWeb = view?.canGoBack() == true
                                                        canGoForwardWeb = view?.canGoForward() == true
                                                        applyWebviewTheme(view, webviewTheme)
                                                    }
                                                }
                                                webChromeClient = object : WebChromeClient() {
                                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                        super.onProgressChanged(view, newProgress)
                                                        if (newProgress > 15) applyWebviewTheme(view, webviewTheme)
                                                    }
                                                }
                                                loadUrl(currentUrl)
                                                webViewInstance = this
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // REELS: CHAT IA OPENAI COM SUPORTE A ÁUDIO / VOZ
        // ==========================================
        if (activeChatArticle != null) {
            val art = activeChatArticle!!
            val currentChatMessages = chatMessagesMap[art.id] ?: emptyList()

            Dialog(
                onDismissRequest = { activeChatArticle = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF121215),
                    border = BorderStroke(1.dp, Color(0xFF6366F1)),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.88f)
                ) {
                    SegmentAiChatCard(
                        segmentIndex = 0,
                        segmentText = "${art.header}: ${art.content}",
                        messages = currentChatMessages,
                        isLoading = isChatLoading,
                        errorMessage = chatErrorMessage,
                        isMinimized = isChatMinimized,
                        isDialogMode = true,
                        readingTheme = "DARK_OLED",
                        category = "Legislação Planalto",
                        categoryColor = "#D97706",
                        modifier = Modifier.fillMaxSize(),
                        onToggleMinimize = { isChatMinimized = !isChatMinimized },
                        onSendMessage = { question, isAudio ->
                            handleSendOpenAiChatMessage(question, isAudio)
                        },
                        onClearChat = {
                            chatMessagesMap = chatMessagesMap + (art.id to emptyList())
                        },
                        onDismiss = { activeChatArticle = null },
                        onSpeakTextInLang = { lang, text ->
                            localSpeechManager.speakTextInLanguage(text, lang)
                        },
                        onStopSpeaking = { localSpeechManager.stop() }
                    )
                }
            }
        }

        // ==========================================
        // REELS: MODAL DE EXPLICAÇÃO COM IA (TRILATERAL)
        // ==========================================
        if (activeExplanationArticle != null) {
            val art = activeExplanationArticle!!
            val explanation = articleExplanationsMap[art.id]

            Dialog(
                onDismissRequest = { activeExplanationArticle = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Explicação Didática: ${art.header}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { activeExplanationArticle = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF27272A), modifier = Modifier.padding(vertical = 6.dp))

                        if (generatingExplanationArticleId == art.id) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFFF59E0B))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Analisando dispositivo legal com a OpenAI...", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        } else if (explanation != null) {
                            val isSpeakingExplanation = isSpeaking && activeTargetId == "expl_${art.id}"
                            SegmentAiExplanationCard(
                                explanation = explanation,
                                isLoading = generatingExplanationArticleId == art.id,
                                readingTheme = "DARK_OLED",
                                category = "Legislação Planalto",
                                categoryColor = "#D97706",
                                isSpeaking = isSpeakingExplanation,
                                activeComparativeLineId = "expl_${art.id}",
                                activeWordIndex = activeWordIndex,
                                modifier = Modifier.fillMaxWidth(),
                                onLanguageChangeAndSpeak = { lang, txt ->
                                    localSpeechManager.speakFromWordInTarget("expl_${art.id}", txt, 0)
                                },
                                onSpeakExplanation = { lang, txt ->
                                    localSpeechManager.speakFromWordInTarget("expl_${art.id}", txt, 0)
                                },
                                onSpeakExplanationWord = { lang, fullText, wordOffset ->
                                    localSpeechManager.speakFromWordInTarget("expl_${art.id}", fullText, wordOffset)
                                },
                                onStopSpeaking = { localSpeechManager.stopTarget("expl_${art.id}") },
                                onRegenerate = { requestExplanationForArticle(art) },
                                onDelete = { articleExplanationsMap = articleExplanationsMap - art.id },
                                onDismiss = { activeExplanationArticle = null }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhuma explicação disponível.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // REELS: QUIZ E QUESTÕES DE PROVA COM IA
        // ==========================================
        if (activeQuizArticle != null) {
            val art = activeQuizArticle!!
            val questions = articleQuestionsMap[art.id] ?: emptyList()

            Dialog(
                onDismissRequest = { activeQuizArticle = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.85f)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Quiz & Questões: ${art.header}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { activeQuizArticle = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF27272A), modifier = Modifier.padding(vertical = 6.dp))

                        if (isGeneratingQuiz) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Elaborando questões inéditas de concurso com IA...", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        } else if (questions.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(questions, key = { it.id }) { q ->
                                    InteractiveQuestionCard(
                                        question = q,
                                        onAnswerSelected = { selIdx, isCorrect ->
                                            val updated = questions.map {
                                                if (it.id == q.id) it.copy(userAnswerIndex = selIdx, isAnswered = true, isCorrect = isCorrect) else it
                                            }
                                            articleQuestionsMap = articleQuestionsMap + (art.id to updated)
                                            onRecordQuestionAnswer?.invoke(q.id, selIdx, isCorrect)
                                        },
                                        onResetAnswer = {
                                            val updated = questions.map {
                                                if (it.id == q.id) it.copy(userAnswerIndex = null, isAnswered = false, isCorrect = null) else it
                                            }
                                            articleQuestionsMap = articleQuestionsMap + (art.id to updated)
                                            onResetQuestionAnswer?.invoke(q.id)
                                        },
                                        onDeleteQuestion = {
                                            articleQuestionsMap = articleQuestionsMap + (art.id to questions.filter { it.id != q.id })
                                        },
                                        onSpeakText = { txt -> localSpeechManager.speakTextInLanguage(txt, "pt") }
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(quizErrorMessage ?: "Nenhuma questão disponível.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // REELS: DICIONÁRIO JURÍDICO INSTANTÂNEO
        // ==========================================
        if (activeDictionaryTerm != null) {
            val term = activeDictionaryTerm!!
            Dialog(
                onDismissRequest = { activeDictionaryTerm = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📖", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dicionário: \"$term\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { activeDictionaryTerm = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF27272A), modifier = Modifier.padding(vertical = 8.dp))

                        if (isLoadingDictionary) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Text(
                                text = dictionaryDefinition ?: "Sem definição.",
                                fontSize = 12.5.sp,
                                color = Color(0xFFE0F2FE),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NativeLawArticleCardWithReelsTools(
    article: LawArticleItem,
    fontSizeSp: Float,
    isSpeaking: Boolean,
    activeWordIndex: Int = 0,
    activeTargetId: String? = null,
    dictionaryTerms: List<PageDictionaryTerm> = emptyList(),
    explanation: PageSegmentExplanation?,
    isGeneratingExplanation: Boolean,
    onPlayTts: () -> Unit,
    onWordClick: (wordIndex: Int) -> Unit = {},
    onDictionaryTermClick: ((PageDictionaryTerm) -> Unit)? = null,
    onExplainWithAi: () -> Unit,
    onChatWithAi: () -> Unit,
    onQuizWithAi: () -> Unit,
    onExtractDictionary: () -> Unit,
    onLookupSingleTerm: (String) -> Unit,
    onNavigateToLink: (String) -> Unit,
    onSpeakExplanation: (langCode: String, text: String) -> Unit = { _, _ -> },
    onSpeakExplanationWord: (langCode: String, fullText: String, wordOffset: Int) -> Unit = { _, _, _ -> },
    onStopExplanationSpeaking: () -> Unit = {}
) {
    val annotatedHeader = remember(article.header) {
        buildAnnotatedString {
            val text = article.header
            var currentIndex = 0
            val pattern = java.util.regex.Pattern.compile("~~(.*?)~~", java.util.regex.Pattern.DOTALL)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (start > currentIndex) {
                    append(text.substring(currentIndex, start))
                }
                val revokedPhrase = matcher.group(1) ?: ""
                withStyle(
                    SpanStyle(
                        color = Color(0xFF71717A),
                        textDecoration = TextDecoration.LineThrough
                    )
                ) {
                    append(revokedPhrase)
                }
                currentIndex = end
            }
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }

    val isDirectoryLinkCard = article.links.size == 1 && article.content.isBlank()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSpeaking) Color(0xFF1E1B4B) else if (article.isRevoked) Color(0xFF141416) else Color(0xFF18181B),
        border = BorderStroke(
            1.dp,
            if (isSpeaking) Color(0xFFF59E0B) else if (article.isRevoked) Color(0xFF3F3F46).copy(alpha = 0.5f) else Color(0xFF27272A)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDirectoryLinkCard) Modifier.clickable { onNavigateToLink(article.links[0].targetUrl) } else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==========================================
            // ÁREA PRINCIPAL DO TEXTO DA LEI
            // ==========================================
            Column(modifier = Modifier.weight(1f)) {
                // CABEÇALHO DO ARTIGO / CAPÍTULO
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = annotatedHeader,
                        fontSize = (fontSizeSp + 0.5f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSpeaking) Color(0xFFFCD34D) else if (article.isRevoked) Color(0xFFA1A1AA) else Color(0xFFFFFFFF)
                    )
                    if (article.isRevoked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.18f),
                            border = BorderStroke(0.6.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "⚠️ ARTIGO REVOGADO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (article.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    InteractiveUniversalKaraokeText(
                        text = article.content,
                        isSpeaking = isSpeaking,
                        activeWordIndex = activeWordIndex,
                        fontSizeSp = fontSizeSp,
                        textColor = if (article.isRevoked) Color(0xFF71717A) else Color(0xFFF4F4F5),
                        activeWordColor = Color(0xFFFCD34D),
                        activeWordBgColor = Color(0x66F59E0B),
                        pastWordColor = Color(0xFFCBD5E1),
                        dictionaryTerms = dictionaryTerms,
                        onDictionaryTermClick = onDictionaryTermClick,
                        onWordClick = onWordClick
                    )
                }

                // NOTA DE REVISÃO HISTÓRICA (ex: Redação dada pela Lei nº ...)
                if (article.revisionNote != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.revisionNote,
                        fontSize = (fontSizeSp - 3.5f).coerceAtLeast(8.5f).sp,
                        color = Color(0xFF9CA3AF),
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    )
                }

                // LINKS INTERNOS CLICÁVEIS PARA DIRETÓRIOS E DEMAIS LEIS
                if (article.links.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        article.links.forEach { link ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { onNavigateToLink(link.targetUrl) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = link.text,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE0F2FE)
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // EXPLICAÇÃO COM IA ACOPLADA DIRETAMENTE NO BLOCO (IN-PLACE)
                // ==========================================
                if (isGeneratingExplanation) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF27272A).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Elaborando explicação com 3 exemplos práticos (Vilela, Janinho, Márcia, Parroca, Lorenzzo)...",
                                fontSize = 10.5.sp,
                                color = Color(0xFFFCD34D),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (explanation != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isSpeakingThisExpl = isSpeaking && activeTargetId == "expl_${article.id}"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SegmentAiExplanationCard(
                            explanation = explanation,
                            isLoading = false,
                            readingTheme = "DARK_OLED",
                            category = "Legislação Planalto",
                            categoryColor = "#D97706",
                            isSpeaking = isSpeakingThisExpl,
                            activeComparativeLineId = "expl_${article.id}",
                            activeWordIndex = activeWordIndex,
                            modifier = Modifier.fillMaxWidth(),
                            onLanguageChangeAndSpeak = { _, txt -> onSpeakExplanation("PT", txt) },
                            onSpeakExplanation = onSpeakExplanation,
                            onSpeakExplanationWord = onSpeakExplanationWord,
                            onStopSpeaking = onStopExplanationSpeaking,
                            onRegenerate = onExplainWithAi,
                            onDelete = { },
                            onDismiss = { }
                        )
                    }
                }
            }

            // ==========================================
            // BOTÕES DE FUNÇÃO DO MODO REELS À DIREITA
            // ==========================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Áudio / TTS
                Surface(
                    shape = CircleShape,
                    color = if (isSpeaking) Color(0xFFD97706) else Color(0xFF27272A),
                    border = BorderStroke(0.6.dp, if (isSpeaking) Color(0xFFFBBF24) else Color(0xFF3F3F46)),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onPlayTts() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isSpeaking) Icons.Default.PauseCircle else Icons.Default.VolumeUp,
                            contentDescription = "Ler Artigo com Voz",
                            tint = if (isSpeaking) Color.Black else Color(0xFF93C5FD),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // 2. Explicação com IA
                Surface(
                    shape = CircleShape,
                    color = if (explanation != null) Color(0xFFD97706).copy(alpha = 0.35f) else Color(0xFF27272A),
                    border = BorderStroke(0.6.dp, if (explanation != null) Color(0xFFF59E0B) else Color(0xFF3F3F46)),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onExplainWithAi() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "Explicar Artigo com IA (3 Exemplos)",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // 3. Chat OpenAI (com envio de áudio e voz)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF27272A),
                    border = BorderStroke(0.6.dp, Color(0xFF3F3F46)),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onChatWithAi() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chat OpenAI com voz",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // 4. Questões & Quiz de Concurso
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF27272A),
                    border = BorderStroke(0.6.dp, Color(0xFF3F3F46)),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onQuizWithAi() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Quiz e Questões de Concurso",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // 5. Vasculhar e Enviar Termos ao Dicionário Offline
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF27272A),
                    border = BorderStroke(0.6.dp, Color(0xFF3F3F46)),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onExtractDictionary() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Vasculhar Termos para Dicionário Offline",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
