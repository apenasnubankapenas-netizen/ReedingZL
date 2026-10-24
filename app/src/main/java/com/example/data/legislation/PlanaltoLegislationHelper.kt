package com.example.data.legislation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LawPreset(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val category: String,
    val planaltoUrl: String,
    val emoji: String,
    val importanceTag: String = "Essencial para Provas"
)

data class LawClassification(
    val category: String,
    val subcategory: String,
    val categoryColor: String
)

object PlanaltoLegislationHelper {
    private const val TAG = "PlanaltoLegislation"

    fun classifyLawDocument(title: String, url: String): LawClassification {
        val t = title.lowercase()
        val u = url.lowercase()
        return when {
            t.contains("constituiç") || u.contains("constituicao") ->
                LawClassification("Direito Constitucional", "Constituição Federal", "#1D4ED8")
            t.contains("código penal") || u.contains("del2848") ->
                LawClassification("Direito Penal", "Código Penal", "#B91C1C")
            t.contains("processo penal") || u.contains("del3689") ->
                LawClassification("Processo Penal", "Código Processual", "#C2410C")
            t.contains("código civil") || u.contains("l10406") ->
                LawClassification("Direito Civil", "Código Civil", "#047857")
            t.contains("processo civil") || u.contains("l13105") ->
                LawClassification("Processo Civil", "Código Processual", "#4338CA")
            t.contains("trabalho") || t.contains("clt") || u.contains("del5452") ->
                LawClassification("Direito do Trabalho", "CLT", "#0F766E")
            t.contains("tributário") || u.contains("l5172") ->
                LawClassification("Direito Tributário", "Código Tributário", "#854D0E")
            t.contains("consumidor") || u.contains("l8078") ->
                LawClassification("Direito do Consumidor", "CDC", "#0284C7")
            t.contains("trânsito") || u.contains("l9503") ->
                LawClassification("Legislação de Trânsito", "CTB", "#4B5563")
            t.contains("eleitoral") || u.contains("l4737") ->
                LawClassification("Direito Eleitoral", "Código Eleitoral", "#4F46E5")
            t.contains("decreto-lei") || u.contains("del") ->
                LawClassification("Legislação Federal", "Decreto-Lei", "#B45309")
            t.contains("decreto") || u.contains("decreto") ->
                LawClassification("Legislação Federal", "Decreto Executivo", "#D97706")
            t.contains("complementar") || u.contains("lcp") ->
                LawClassification("Legislação Federal", "Lei Complementar", "#7C3AED")
            t.contains("provisória") || u.contains("mpv") ->
                LawClassification("Legislação Federal", "Medida Provisória", "#BE185D")
            t.contains("lei nº") || t.contains("lei n") || u.contains("/lei/") || u.contains("/leis/") ->
                LawClassification("Legislação Federal", "Lei Ordinária", "#059669")
            else ->
                LawClassification("Legislação & Direito", "Legislação Planalto", "#0D9488")
        }
    }

    const val PLANALTO_BASE_URL = "https://www.planalto.gov.br/ccivil_03/"

    // Injeção de CSS Dark Mode OLED / Preto e Branco de alto contraste para WebView do Planalto
    const val DARK_MODE_INJECTION_JS = """
        (function() {
            var css = `
                html, body, div, table, tbody, tr, td, th, center, blockquote, font, p, span, form, ul, li {
                    background-color: #000000 !important;
                    background-image: none !important;
                    color: #F4F4F5 !important;
                    border-color: #27272A !important;
                }
                html, body {
                    background-color: #000000 !important;
                    color: #F4F4F5 !important;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif !important;
                    line-height: 1.7 !important;
                    padding: 10px !important;
                }
                * {
                    box-shadow: none !important;
                    text-shadow: none !important;
                }
                a, a *, a font, a span {
                    color: #93C5FD !important;
                    text-decoration: underline !important;
                }
                a:visited, a:visited * {
                    color: #C084FC !important;
                }
                strike, del, s, font[color='#808080'], font[color='gray'], .revogado {
                    color: #71717A !important;
                    text-decoration: line-through !important;
                    opacity: 0.65 !important;
                }
                table {
                    border-collapse: collapse !important;
                    border: 1px solid #3F3F46 !important;
                    width: 100% !important;
                    background-color: #09090B !important;
                }
                td, th {
                    border: 1px solid #27272A !important;
                    padding: 8px !important;
                    color: #E4E4E7 !important;
                    background-color: #09090B !important;
                }
                p {
                    margin-bottom: 14px !important;
                    text-align: justify !important;
                    color: #F4F4F5 !important;
                }
                b, strong, h1, h2, h3, h4 {
                    color: #FFFFFF !important;
                    font-weight: 700 !important;
                }
                font[color='red'], font[color='#ff0000'] {
                    color: #F87171 !important;
                }
                font[color='blue'], font[color='#0000ff'] {
                    color: #60A5FA !important;
                }
                img {
                    filter: invert(0.9) hue-rotate(180deg) !important;
                    max-width: 100% !important;
                    border-radius: 6px !important;
                }
                input, select, textarea, button {
                    background-color: #18181B !important;
                    color: #FFFFFF !important;
                    border: 1px solid #3F3F46 !important;
                    border-radius: 6px !important;
                    padding: 6px !important;
                }
            `;
            var style = document.getElementById('legislation-custom-theme-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'legislation-custom-theme-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                (document.head || document.documentElement).appendChild(style);
            } else {
                style.textContent = css;
            }

            document.querySelectorAll('[bgcolor], [background]').forEach(function(el) {
                el.removeAttribute('bgcolor');
                el.removeAttribute('background');
            });
        })();
    """

    // Injeção de Filtro Negativo Invertido (Color Invert Filter)
    const val NEGATIVE_FILTER_JS = """
        (function() {
            var css = `
                html {
                    filter: invert(1) hue-rotate(180deg) !important;
                    background-color: #000000 !important;
                }
                body {
                    background-color: #FFFFFF !important;
                }
                img, video, iframe {
                    filter: invert(1) hue-rotate(180deg) !important;
                }
            `;
            var style = document.getElementById('legislation-custom-theme-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'legislation-custom-theme-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                (document.head || document.documentElement).appendChild(style);
            } else {
                style.textContent = css;
            }
        })();
    """

    // Injeção de Preto e Branco Alto Contraste (Monochrome Pure)
    const val HIGH_CONTRAST_BW_JS = """
        (function() {
            var css = `
                html, body, div, table, tr, td, p, span, font, a, center {
                    background-color: #000000 !important;
                    color: #FFFFFF !important;
                    border-color: #444444 !important;
                }
                a, a font, a span {
                    color: #FFFFFF !important;
                    text-decoration: underline !important;
                }
                strike, del, s {
                    color: #888888 !important;
                    text-decoration: line-through !important;
                }
                img {
                    filter: grayscale(1) invert(1) !important;
                }
            `;
            var style = document.getElementById('legislation-custom-theme-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'legislation-custom-theme-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                (document.head || document.documentElement).appendChild(style);
            } else {
                style.textContent = css;
            }
        })();
    """

    // Limpa filtros e volta ao tema original
    const val RESET_THEME_JS = """
        (function() {
            var style = document.getElementById('legislation-custom-theme-style');
            if (style) {
                style.remove();
            }
        })();
    """

    val POPULAR_LAWS_CATALOG = listOf(
        LawPreset(
            id = "cf88",
            title = "Constituição da República Federativa do Brasil de 1988",
            shortName = "CF/88",
            description = "Lei Maior do ordenamento jurídico brasileiro. Direitos e Garantias Fundamentais, Organização do Estado e Poderes.",
            category = "Direito Constitucional",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/constituicao/constituicao.htm",
            emoji = "🏛️",
            importanceTag = "Mais Cobrada de Todas"
        ),
        LawPreset(
            id = "cp",
            title = "Código Penal Brasileiro (Decreto-Lei nº 2.848/1940)",
            shortName = "Código Penal",
            description = "Parte Geral (crime, imputabilidade, penas) e Parte Especial (crimes contra a pessoa, patrimônio e administração pública).",
            category = "Direito Penal",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/decreto-lei/del2848compilado.htm",
            emoji = "⚖️",
            importanceTag = "Essencial Carreiras Policiais e Jurídicas"
        ),
        LawPreset(
            id = "cc",
            title = "Código Civil Brasileiro (Lei nº 10.406/2002)",
            shortName = "Código Civil",
            description = "Pessoas, Bens, Fatos Jurídicos, Obrigações, Contratos, Responsabilidade Civil, Direitos Reais e Família.",
            category = "Direito Civil",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/leis/2002/l10406compilada.htm",
            emoji = "📜",
            importanceTag = "Base do Direito Privado"
        ),
        LawPreset(
            id = "cpp",
            title = "Código de Processo Penal (Decreto-Lei nº 3.689/1941)",
            shortName = "CPP",
            description = "Inquérito policial, ação penal, provas, prisões, medidas cautelares, recursos e Tribunal do Júri.",
            category = "Direito Processual Penal",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/decreto-lei/del3689compilado.htm",
            emoji = "🛡️",
            importanceTag = "Imprescindível para Concursos"
        ),
        LawPreset(
            id = "cpc",
            title = "Código de Processo Civil (Lei nº 13.105/2015)",
            shortName = "Novo CPC",
            description = "Normas fundamentais, tutela provisória, petição inicial, resposta do réu, provas, sentença e execução.",
            category = "Direito Processual Civil",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2015/lei/l13105.htm",
            emoji = "🏛️",
            importanceTag = "Processual Geral"
        ),
        LawPreset(
            id = "lei8112",
            title = "Regime Jurídico dos Servidores Públicos Civis da União (Lei nº 8.112/1990)",
            shortName = "Lei 8.112/90",
            description = "Provimento, vacância, direitos, vantagens, regime disciplinar e processo administrativo disciplinar (PAD).",
            category = "Direito Administrativo",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/leis/l8112cons.htm",
            emoji = "👔",
            importanceTag = "100% Presente em Editais Federais"
        ),
        LawPreset(
            id = "clt",
            title = "Consolidação das Leis do Trabalho (Decreto-Lei nº 5.452/1943)",
            shortName = "CLT",
            description = "Contrato individual de trabalho, jornada, férias, rescisão, segurança do trabalho e direito coletivo.",
            category = "Direito do Trabalho",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/decreto-lei/del5452.htm",
            emoji = "💼",
            importanceTag = "TRT e OAB"
        ),
        LawPreset(
            id = "lei14133",
            title = "Nova Lei de Licitações e Contratos Administrativos (Lei nº 14.133/2021)",
            shortName = "Nova Lei de Licitações",
            description = "Modalidades licitatórias, contratação direta (dispensa e inexigibilidade), critérios de julgamento e contratos.",
            category = "Direito Administrativo",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/_ato2019-2022/2021/lei/l14133.htm",
            emoji = "📑",
            importanceTag = "Tema de Destaque Atual"
        ),
        LawPreset(
            id = "lei8429",
            title = "Lei de Improbidade Administrativa (Lei nº 8.429/1992 atualizada)",
            shortName = "Improbidade Administrativa",
            description = "Atos que importam em enriquecimento ilícito, prejuízo ao erário e atentam contra os princípios da administração.",
            category = "Direito Administrativo",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/leis/l8429.htm",
            emoji = "🚔",
            importanceTag = "Fundamental Administrativo"
        ),
        LawPreset(
            id = "lei13869",
            title = "Lei de Abuso de Autoridade (Lei nº 13.869/2019)",
            shortName = "Abuso de Autoridade",
            description = "Crimes cometidos por agentes públicos, servidores ou não, no exercício ou a pretexto de exercer funções.",
            category = "Legislação Penal Especial",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/_ato2019-2022/2019/lei/l13869.htm",
            emoji = "🚨",
            importanceTag = "Penal Especial"
        ),
        LawPreset(
            id = "ctb",
            title = "Código de Trânsito Brasileiro (Lei nº 9.503/1997)",
            shortName = "CTB",
            description = "Sistema Nacional de Trânsito, normas de circulação e conduta, infrações, penalidades e crimes de trânsito.",
            category = "Direito Administrativo / Trânsito",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm",
            emoji = "🚗",
            importanceTag = "PRF e Detrans"
        ),
        LawPreset(
            id = "eca",
            title = "Estatuto da Criança e do Adolescente (Lei nº 8.069/1990)",
            shortName = "ECA",
            description = "Proteção integral, direitos fundamentais, medidas de proteção, atos infracionais e medidas socioeducativas.",
            category = "Direito da Criança e do Adolescente",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/leis/l8069.htm",
            emoji = "👶",
            importanceTag = "Magistratura, MP e Defensoria"
        ),
        LawPreset(
            id = "cdc",
            title = "Código de Defesa do Consumidor (Lei nº 8.078/1990)",
            shortName = "CDC",
            description = "Direitos básicos do consumidor, responsabilidade pelo fato e vício do produto/serviço, práticas abusivas.",
            category = "Direito do Consumidor",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/leis/l8078compilado.htm",
            emoji = "⚖️",
            importanceTag = "Consumidor e Cível"
        ),
        LawPreset(
            id = "lei11343",
            title = "Lei de Drogas (Lei nº 11.343/2006)",
            shortName = "Lei de Drogas",
            description = "Prevenção, tratamento de usuários, crimes de tráfico e procedimentos da investigação policial.",
            category = "Legislação Penal Especial",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/_ato2004-2006/2006/lei/l11343.htm",
            emoji = "💊",
            importanceTag = "Carreiras Policiais e MP"
        ),
        LawPreset(
            id = "lgpd",
            title = "Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018)",
            shortName = "LGPD",
            description = "Tratamento de dados pessoais, inclusive nos meios digitais, por pessoa natural ou jurídica.",
            category = "Direito Digital",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm",
            emoji = "🔒",
            importanceTag = "Direito Contemporâneo"
        ),
        LawPreset(
            id = "lei11340",
            title = "Lei Maria da Penha (Lei nº 11.340/2006)",
            shortName = "Maria da Penha",
            description = "Mecanismos para coibir a violência doméstica e familiar contra a mulher e medidas protetivas de urgência.",
            category = "Legislação Especial",
            planaltoUrl = "https://www.planalto.gov.br/ccivil_03/_ato2004-2006/2006/lei/l11340.htm",
            emoji = "💜",
            importanceTag = "Direitos Humanos e Penal"
        )
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Faz download do conteúdo HTML da página do Planalto ou recupera do cache offline,
     * identifica o charset (ex: Windows-1252 / ISO-8859-1) e converte em artigos estruturados nativos.
     */
    suspend fun fetchStructuredLawWithCache(
        context: Context,
        url: String,
        forceRefresh: Boolean = false
    ): StructuredLawResult = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.substringBefore("#").trim()
            if (!forceRefresh) {
                val cached = LawOfflineCacheManager.getCachedLaw(context, cleanUrl) ?: LawOfflineCacheManager.getCachedLaw(context, url)
                if (cached != null && cached.articles.isNotEmpty()) {
                    val cleanText = cached.articles.joinToString("\n\n") { "${it.header} ${it.content}".trim() }
                    val pages = paginateLawContent(cleanText)
                    val jsonArray = JSONArray()
                    pages.forEach { jsonArray.put(it) }

                    return@withContext StructuredLawResult.Success(
                        title = cached.title,
                        articles = cached.articles,
                        pages = pages,
                        sampleContentJson = jsonArray.toString(),
                        totalArticles = cached.articles.count { it.header.startsWith("Art", ignoreCase = true) },
                        sourceUrl = cleanUrl,
                        isFromCache = true
                    )
                }
            }

            val candidateUrls = linkedSetOf<String>()
            candidateUrls.add(cleanUrl)
            if (cleanUrl.startsWith("http://", ignoreCase = true)) {
                candidateUrls.add("https://" + cleanUrl.substring(7))
            }
            if (!cleanUrl.contains("/ccivil_03/") && cleanUrl.contains("planalto.gov.br")) {
                val rel = cleanUrl.substringAfter("planalto.gov.br/").removePrefix("ccivil_03/")
                candidateUrls.add("https://www.planalto.gov.br/ccivil_03/$rel")
            }
            if (cleanUrl.endsWith(".htm", ignoreCase = true)) {
                candidateUrls.add(cleanUrl.dropLast(4) + ".HTM")
                candidateUrls.add(cleanUrl.dropLast(4) + ".htm")
            }

            var successfulResponse: okhttp3.Response? = null
            var finalUrlUsed = cleanUrl
            var lastErrorCode = 0

            for (targetCandidate in candidateUrls) {
                try {
                    val request = Request.Builder()
                        .url(targetCandidate)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8")
                        .header("Connection", "keep-alive")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        successfulResponse = response
                        finalUrlUsed = targetCandidate
                        break
                    } else {
                        lastErrorCode = response.code
                        response.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao tentar URL candidata $targetCandidate: ${e.message}")
                }
            }

            if (successfulResponse == null) {
                // Tenta fallback para cache mesmo expirado se houver
                val cached = LawOfflineCacheManager.getCachedLaw(context, cleanUrl) ?: LawOfflineCacheManager.getCachedLaw(context, url)
                if (cached != null && cached.articles.isNotEmpty()) {
                    val cleanText = cached.articles.joinToString("\n\n") { "${it.header} ${it.content}".trim() }
                    val pages = paginateLawContent(cleanText)
                    val jsonArray = JSONArray()
                    pages.forEach { jsonArray.put(it) }

                    return@withContext StructuredLawResult.Success(
                        title = cached.title,
                        articles = cached.articles,
                        pages = pages,
                        sampleContentJson = jsonArray.toString(),
                        totalArticles = cached.articles.count { it.header.startsWith("Art", ignoreCase = true) },
                        sourceUrl = cleanUrl,
                        isFromCache = true
                    )
                }
                return@withContext StructuredLawResult.Error("Erro ao acessar página do Planalto: HTTP ${if (lastErrorCode > 0) lastErrorCode else 404}")
            }

            val responseBody = successfulResponse.body ?: return@withContext StructuredLawResult.Error("Corpo da página vazio.")
            val bytes = responseBody.bytes()

            val charset = detectCharset(bytes) ?: Charset.forName("windows-1252")
            val rawHtml = String(bytes, charset)

            val (title, structuredArticles) = parsePlanaltoHtmlToArticles(rawHtml, finalUrlUsed)
            if (structuredArticles.isEmpty()) {
                return@withContext StructuredLawResult.Error("Não foi possível extrair os artigos da norma.")
            }

            // Salva no banco/cache local offline
            LawOfflineCacheManager.saveCachedLaw(
                context = context,
                url = finalUrlUsed,
                title = title,
                rawHtml = rawHtml,
                articles = structuredArticles
            )

            val cleanText = structuredArticles.joinToString("\n\n") { "${it.header} ${it.content}".trim() }
            val pages = paginateLawContent(cleanText)
            val jsonArray = JSONArray()
            pages.forEach { jsonArray.put(it) }

            StructuredLawResult.Success(
                title = title,
                articles = structuredArticles,
                pages = pages,
                sampleContentJson = jsonArray.toString(),
                totalArticles = structuredArticles.count { it.header.startsWith("Art", ignoreCase = true) },
                sourceUrl = url,
                isFromCache = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar legislação estruturada: ${e.message}", e)
            val cached = LawOfflineCacheManager.getCachedLaw(context, url)
            if (cached != null && cached.articles.isNotEmpty()) {
                val cleanText = cached.articles.joinToString("\n\n") { "${it.header} ${it.content}".trim() }
                val pages = paginateLawContent(cleanText)
                val jsonArray = JSONArray()
                pages.forEach { jsonArray.put(it) }

                return@withContext StructuredLawResult.Success(
                    title = cached.title,
                    articles = cached.articles,
                    pages = pages,
                    sampleContentJson = jsonArray.toString(),
                    totalArticles = cached.articles.count { it.header.startsWith("Art", ignoreCase = true) },
                    sourceUrl = url,
                    isFromCache = true
                )
            }
            StructuredLawResult.Error("Falha na conexão: ${e.localizedMessage ?: "Sem conexão offline"}")
        }
    }

    /**
     * Faz download do conteúdo HTML da página do Planalto, identifica o charset (ex: Windows-1252 / ISO-8859-1)
     * e converte em texto limpo estruturado.
     */
    suspend fun fetchAndExtractLaw(url: String): ExtractedLawResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ExtractedLawResult.Error("Erro ao acessar página do Planalto: HTTP ${response.code}")
            }

            val responseBody = response.body ?: return@withContext ExtractedLawResult.Error("Corpo da página vazio.")
            val bytes = responseBody.bytes()

            // O Planalto frequentemente utiliza charset ISO-8859-1 / Windows-1252
            val charset = detectCharset(bytes) ?: Charset.forName("windows-1252")
            val rawHtml = String(bytes, charset)

            val (title, cleanText) = parsePlanaltoHtml(rawHtml, url)
            if (cleanText.isBlank()) {
                return@withContext ExtractedLawResult.Error("Não foi possível extrair os artigos da norma.")
            }

            // Pagina o texto da lei em fatias didáticas
            val pages = paginateLawContent(cleanText)
            val jsonArray = JSONArray()
            pages.forEach { jsonArray.put(it) }

            ExtractedLawResult.Success(
                title = title,
                pages = pages,
                sampleContentJson = jsonArray.toString(),
                totalArticles = countArticles(cleanText),
                sourceUrl = url
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar legislação: ${e.message}", e)
            ExtractedLawResult.Error("Falha na conexão com o Planalto: ${e.localizedMessage ?: "Erro desconhecido"}")
        }
    }

    fun resolvePlanaltoUrl(baseUrl: String, href: String): String {
        var cleanHref = href.trim().replace("\\", "/")
        if (cleanHref.isBlank() || cleanHref.startsWith("#") || cleanHref.startsWith("javascript:", ignoreCase = true) || cleanHref.startsWith("mailto:", ignoreCase = true)) {
            return baseUrl
        }

        // Se já é URL absoluta HTTP/HTTPS
        if (cleanHref.startsWith("http://", ignoreCase = true) || cleanHref.startsWith("https://", ignoreCase = true)) {
            if (cleanHref.startsWith("http://www.planalto.gov.br", ignoreCase = true)) {
                cleanHref = "https://" + cleanHref.substring(7)
            }
            if (cleanHref.startsWith("https://planalto.gov.br", ignoreCase = true)) {
                cleanHref = "https://www.planalto.gov.br" + cleanHref.removePrefix("https://planalto.gov.br")
            }
            if (cleanHref.startsWith("https://www.planalto.gov.br/") && !cleanHref.contains("/ccivil_03/", ignoreCase = true)) {
                val path = cleanHref.removePrefix("https://www.planalto.gov.br/")
                cleanHref = "https://www.planalto.gov.br/ccivil_03/$path"
            }
            return cleanHref
        }

        if (cleanHref.startsWith("/")) {
            val withCcivil = if (!cleanHref.startsWith("/ccivil_03", ignoreCase = true)) {
                "/ccivil_03$cleanHref"
            } else {
                cleanHref
            }
            return "https://www.planalto.gov.br$withCcivil"
        }

        if (cleanHref.startsWith("ccivil_03/", ignoreCase = true)) {
            return "https://www.planalto.gov.br/$cleanHref"
        }

        val effectiveBase = if (!baseUrl.endsWith("/") && !baseUrl.substringAfterLast("/").contains(".")) {
            "$baseUrl/"
        } else {
            baseUrl
        }

        return try {
            val baseUri = java.net.URI(effectiveBase)
            val resolved = baseUri.resolve(cleanHref).toString()
            var fixed = if (resolved.startsWith("https://www.planalto.gov.br/") && !resolved.contains("/ccivil_03/", ignoreCase = true)) {
                val path = resolved.removePrefix("https://www.planalto.gov.br/")
                "https://www.planalto.gov.br/ccivil_03/$path"
            } else if (resolved.startsWith("http://www.planalto.gov.br/")) {
                "https://" + resolved.substring(7)
            } else {
                resolved
            }
            if (!fixed.startsWith("http://") && !fixed.startsWith("https://")) {
                val baseDir = effectiveBase.substringBeforeLast("/")
                fixed = "$baseDir/$cleanHref"
            }
            fixed
        } catch (e: Exception) {
            val baseDir = effectiveBase.substringBeforeLast("/")
            "$baseDir/$cleanHref"
        }
    }

    /**
     * Faz parser profundo do HTML preservando estrutura de Artigos, Incisos, Parágrafos,
     * identificação de termos e trechos pontualmente REVOGADOS (com marcação de strikethrough ~~)
     * e links internos para todas as subpáginas e diretórios do Planalto sem misturar links entre blocos.
     */
    fun parsePlanaltoHtmlToArticles(html: String, baseUrl: String): Pair<String, List<LawArticleItem>> {
        val (title, _) = parsePlanaltoHtml(html, baseUrl)

        // Limpa scripts e estilos
        val cleanedHtml = html
            .replace(Regex("(?is)<head.*?</head>"), "")
            .replace(Regex("(?is)<script.*?</script>"), "")
            .replace(Regex("(?is)<style.*?</style>"), "")
            .replace(Regex("(?is)<!--.*?-->"), "")

        val articles = mutableListOf<LawArticleItem>()
        var articleIndex = 0

        val hasTableRows = cleanedHtml.contains("<tr", ignoreCase = true)
        val isDirectoryOrIndex = baseUrl.endsWith("ccivil_03/") ||
                baseUrl.endsWith("ccivil_03/index.htm", ignoreCase = true) ||
                baseUrl.contains("quadro_codigo", ignoreCase = true) ||
                baseUrl.contains("/codigos/", ignoreCase = true) ||
                baseUrl.contains("codigos.htm", ignoreCase = true) ||
                baseUrl.contains("/leis/leis", ignoreCase = true) ||
                baseUrl.contains("decretos.htm", ignoreCase = true)

        // 1. Tratamento específico para tabelas de diretórios/índices do Planalto (ex: quadro_codigo.htm, leis.htm)
        if (hasTableRows && isDirectoryOrIndex) {
            val trRegex = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            val trMatcher = trRegex.matcher(cleanedHtml)
            while (trMatcher.find()) {
                val rowHtml = trMatcher.group(1) ?: continue
                val linkMatcher = Pattern.compile("<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(rowHtml)
                val rowLinks = mutableListOf<LawInternalLink>()
                while (linkMatcher.find()) {
                    val href = linkMatcher.group(1)?.trim() ?: ""
                    val linkText = cleanHtmlEntities(stripHtmlTags(linkMatcher.group(2) ?: "")).trim()
                    if (href.isNotBlank() && linkText.isNotBlank() && !href.startsWith("javascript:", ignoreCase = true) && !href.startsWith("#")) {
                        val fullUrl = resolvePlanaltoUrl(baseUrl, href)
                        rowLinks.add(LawInternalLink(linkText, fullUrl))
                    }
                }

                val rowText = cleanHtmlEntities(stripHtmlTags(rowHtml)).trim()
                if (rowText.isBlank() ||
                    rowText.contains("Presidência da República", ignoreCase = true) ||
                    rowText.contains("Subchefia para Assuntos Jurídicos", ignoreCase = true) ||
                    rowText.contains("Casa Civil", ignoreCase = true) ||
                    rowText.contains("Este texto não substitui o publicado no DOU", ignoreCase = true)
                ) {
                    continue
                }

                if (rowLinks.isNotEmpty()) {
                    val primaryLink = rowLinks.first()
                    val header = primaryLink.text.ifBlank { rowText.take(40) }
                    val content = if (rowText.startsWith(header, ignoreCase = true)) {
                        rowText.substring(header.length).trim().removePrefix("-").removePrefix("–").removePrefix(":").trim()
                    } else {
                        rowText
                    }

                    articles.add(
                        LawArticleItem(
                            id = "art_${articleIndex++}",
                            header = header,
                            content = content,
                            isRevoked = false,
                            revisionNote = null,
                            links = rowLinks
                        )
                    )
                } else if (rowText.length > 5) {
                    articles.add(
                        LawArticleItem(
                            id = "art_${articleIndex++}",
                            header = rowText.take(50),
                            content = rowText,
                            isRevoked = false,
                            revisionNote = null,
                            links = emptyList()
                        )
                    )
                }
            }

            if (articles.isNotEmpty()) {
                return Pair(title.ifBlank { "Legislação Planalto" }, articles)
            }
        }

        // 2. Parser padrão de artigos e dispositivos legais (<p>, <div>, <blockquote>)
        val paragraphRegex = Pattern.compile("<(?:p|div|tr|td|blockquote)[^>]*>(.*?)</(?:p|div|tr|td|blockquote)>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = paragraphRegex.matcher(cleanedHtml)

        var currentArticleHeader = "Preâmbulo"
        var currentArticleContent = StringBuilder()
        var currentArticleRevoked = false
        var currentRevisionNote: String? = null
        var currentLinks = mutableListOf<LawInternalLink>()

        fun commitCurrentArticle() {
            val content = currentArticleContent.toString().trim()
            if (content.isNotBlank() || currentArticleHeader.isNotBlank() || currentLinks.isNotEmpty()) {
                articles.add(
                    LawArticleItem(
                        id = "art_${articleIndex++}",
                        header = currentArticleHeader,
                        content = content,
                        isRevoked = currentArticleRevoked,
                        revisionNote = currentRevisionNote,
                        links = currentLinks.toList()
                    )
                )
            }
            currentArticleContent.clear()
            currentLinks.clear()
            currentArticleRevoked = false
            currentRevisionNote = null
        }

        val artHeaderRegex = Regex("(?is)^(Art(?:igo)?\\.?\\s*\\d+(?:\\.\\d+)*[º°ªo]?(?:-[A-Za-z0-9]+)?(?:\\.)?)\\s*(?:[-\\–—:]\\s*)?(.*)")

        while (matcher.find()) {
            var rawBlock = matcher.group(1) ?: continue

            // 1. Converte APENAS tags reais de texto revogado pontuais (<strike>, <s>, <del>, style line-through) em markdown ~~texto~~
            rawBlock = rawBlock
                .replace(Regex("(?is)<strike[^>]*>(.*?)</strike>"), "~~$1~~")
                .replace(Regex("(?is)<s\\b[^>]*>(.*?)</s>"), "~~$1~~")
                .replace(Regex("(?is)<del[^>]*>(.*?)</del>"), "~~$1~~")
                .replace(Regex("(?is)<span[^>]*style=[\"'][^\"']*line-through[^\"']*[\"'][^>]*>(.*?)</span>"), "~~$1~~")

            // 2. Extrai links <a href="..."> resolvendo URLs relativas e subdiretórios
            val linkMatcher = Pattern.compile("<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(rawBlock)
            val extractedLinks = mutableListOf<LawInternalLink>()
            while (linkMatcher.find()) {
                val href = linkMatcher.group(1)?.trim() ?: ""
                val linkText = cleanHtmlEntities(stripHtmlTags(linkMatcher.group(2) ?: "")).trim()
                if (href.isNotBlank() && linkText.isNotBlank() && !href.startsWith("javascript:", ignoreCase = true) && !href.startsWith("#")) {
                    val fullUrl = resolvePlanaltoUrl(baseUrl, href)
                    extractedLinks.add(LawInternalLink(linkText, fullUrl))
                }
            }

            val text = cleanHtmlEntities(stripHtmlTags(rawBlock)).trim()
            if (text.isBlank() ||
                text.contains("Presidência da República", ignoreCase = true) ||
                text.contains("Subchefia para Assuntos Jurídicos", ignoreCase = true) ||
                text.contains("Casa Civil", ignoreCase = true) ||
                text.contains("Este texto não substitui o publicado no DOU", ignoreCase = true)
            ) {
                continue
            }

            // Identifica se é um cabeçalho de artigo, capítulo, seção, livro ou título
            val isHeader = text.startsWith("Art.", ignoreCase = true) ||
                    text.startsWith("Artigo ", ignoreCase = true) ||
                    text.startsWith("CAPÍTULO", ignoreCase = true) ||
                    text.startsWith("TÍTULO", ignoreCase = true) ||
                    text.startsWith("SEÇÃO", ignoreCase = true) ||
                    text.startsWith("SUBSEÇÃO", ignoreCase = true) ||
                    text.startsWith("LIVRO", ignoreCase = true) ||
                    text.startsWith("PARTE", ignoreCase = true) ||
                    text.startsWith("ANEXO", ignoreCase = true) ||
                    text.startsWith("EMENDA", ignoreCase = true)

            // Extrai notas de revisão tipo "(Redação dada pela Lei nº ...)"
            val revNoteMatcher = Pattern.compile("\\((?:Redação|Incluído|Vide|Revogado|Acrescido|Restaurado)[^)]+\\)", Pattern.CASE_INSENSITIVE).matcher(text)
            var revNote: String? = null
            if (revNoteMatcher.find()) {
                revNote = revNoteMatcher.group(0)
            }

            val isCaputRevoked = isHeader && (
                text.contains("~~") && text.take(40).contains("~~") ||
                text.contains("(Revogado pela", ignoreCase = true) ||
                text.contains("(Revogado pelo", ignoreCase = true) ||
                text.contains("(Revogado)", ignoreCase = true) ||
                text.contains("Revogado.", ignoreCase = true) ||
                text.contains("Suprimido.", ignoreCase = true)
            )

            if (isHeader) {
                commitCurrentArticle()

                val artMatch = artHeaderRegex.find(text)
                if (artMatch != null) {
                    currentArticleHeader = artMatch.groupValues[1].trim()
                    val remainder = artMatch.groupValues[2].trim()
                    if (remainder.isNotBlank()) {
                        currentArticleContent.append(remainder)
                    }
                } else {
                    val dotIndex = text.indexOf(" - ")
                    if (dotIndex in 1..30) {
                        currentArticleHeader = text.substring(0, dotIndex).trim()
                        val body = text.substring(dotIndex + 3).trim()
                        if (body.isNotBlank()) {
                            currentArticleContent.append(body)
                        }
                    } else {
                        currentArticleHeader = text
                    }
                }

                currentArticleRevoked = isCaputRevoked
                currentRevisionNote = revNote
                currentLinks.addAll(extractedLinks)
            } else {
                if (currentArticleContent.isNotEmpty()) {
                    currentArticleContent.append("\n\n")
                }
                currentArticleContent.append(text)
                if (revNote != null && currentRevisionNote == null) {
                    currentRevisionNote = revNote
                }
                currentLinks.addAll(extractedLinks)
            }
        }

        commitCurrentArticle()

        // Fallback por linhas se vazio
        if (articles.isEmpty()) {
            val lines = parsePlanaltoHtml(html, baseUrl).second.lines().map { it.trim() }.filter { it.isNotBlank() }
            for (line in lines) {
                val isHeader = line.startsWith("Art.", ignoreCase = true) || line.startsWith("CAPÍTULO", ignoreCase = true)
                if (isHeader) {
                    articles.add(LawArticleItem("art_${articleIndex++}", line, "", false))
                } else {
                    val last = articles.lastOrNull()
                    if (last != null) {
                        articles[articles.size - 1] = last.copy(content = "${last.content}\n\n$line".trim())
                    } else {
                        articles.add(LawArticleItem("art_${articleIndex++}", "Início", line, false))
                    }
                }
            }
        }

        return Pair(title.ifBlank { "Legislação Planalto" }, articles)
    }

    private fun detectCharset(bytes: ByteArray): Charset? {
        val sample = String(bytes.take(2048).toByteArray(), Charsets.US_ASCII)
        val matcher = Pattern.compile("charset=([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE).matcher(sample)
        if (matcher.find()) {
            val name = matcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                try {
                    return Charset.forName(name)
                } catch (_: Exception) {}
            }
        }
        return null
    }

    /**
     * Limpa o HTML do Planalto preservando artigos, parágrafos, incisos e títulos.
     */
    fun parsePlanaltoHtml(html: String, fallbackUrl: String): Pair<String, String> {
        // 1. Extração do título da lei
        val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
        var title = if (titleMatcher.find()) {
            cleanHtmlEntities(titleMatcher.group(1) ?: "")
                .replace("Presidência da República", "")
                .replace("Casa Civil", "")
                .replace("Subchefia para Assuntos Jurídicos", "")
                .trim()
        } else ""

        if (title.isBlank() || title.length < 5) {
            // Tenta achar cabeçalho H1, H2, H3 ou P com classe
            val headMatcher = Pattern.compile("<(?:h[1-4]|p)[^>]*>(LEI|DECRETO|CONSTITUIÇÃO|EMENDA|MEDIDA PROVISÓRIA)[^<]*</(?:h[1-4]|p)>", Pattern.CASE_INSENSITIVE).matcher(html)
            title = if (headMatcher.find()) {
                cleanHtmlEntities(stripHtmlTags(headMatcher.group(0) ?: "")).trim()
            } else {
                POPULAR_LAWS_CATALOG.firstOrNull { fallbackUrl.contains(it.id, ignoreCase = true) || it.planaltoUrl.equals(fallbackUrl, ignoreCase = true) }?.title
                    ?: "Legislação do Planalto (${fallbackUrl.substringAfterLast("/").substringBefore(".").uppercase()})"
            }
        }

        // 2. Remove tags script, style, comments, head
        var cleaned = html
            .replace(Regex("(?is)<head.*?</head>"), "")
            .replace(Regex("(?is)<script.*?</script>"), "")
            .replace(Regex("(?is)<style.*?</style>"), "")
            .replace(Regex("(?is)<!--.*?-->"), "")

        // 3. Substitui quebras HTML por newlines estruturadas
        cleaned = cleaned
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)</tr>"), "\n")
            .replace(Regex("(?i)</li>"), "\n")
            .replace(Regex("(?i)</blockquote>"), "\n\n")

        // 4. Remove todas as demais tags HTML
        cleaned = stripHtmlTags(cleaned)
        cleaned = cleanHtmlEntities(cleaned)

        // 5. Normaliza quebras de linha e espaçamentos
        val lines = cleaned.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                !line.contains("Presidência da República", ignoreCase = true) &&
                !line.contains("Subchefia para Assuntos Jurídicos", ignoreCase = true) &&
                !line.contains("Casa Civil", ignoreCase = true) &&
                !line.contains("Este texto não substitui o publicado no DOU", ignoreCase = true) &&
                !line.contains("Retificação", ignoreCase = true) &&
                !line.contains("Brasília,", ignoreCase = true) &&
                !line.contains("Palácio do Planalto,", ignoreCase = true)
            }

        val text = lines.joinToString("\n\n").trim()
        return Pair(title.ifBlank { "Legislação Planalto" }, text)
    }

    private fun stripHtmlTags(html: String): String {
        return html.replace(Regex("<[^>]+>"), " ")
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&ordm;", "º")
            .replace("&ordf;", "ª")
            .replace("&sect;", "§")
            .replace("&ccedil;", "ç")
            .replace("&Ccedil;", "Ç")
            .replace("&atilde;", "ã")
            .replace("&Atilde;", "Ã")
            .replace("&otilde;", "õ")
            .replace("&Otilde;", "Õ")
            .replace("&aacute;", "á")
            .replace("&Aacute;", "Á")
            .replace("&eacute;", "é")
            .replace("&Eacute;", "É")
            .replace("&iacute;", "í")
            .replace("&Iacute;", "Í")
            .replace("&oacute;", "ó")
            .replace("&Oacute;", "Ó")
            .replace("&uacute;", "ú")
            .replace("&Uacute;", "Ú")
            .replace("&acirc;", "â")
            .replace("&Acirc;", "Â")
            .replace("&ecirc;", "ê")
            .replace("&Ecirc;", "Ê")
            .replace("&ocirc;", "ô")
            .replace("&Ocirc;", "Ô")
            .replace("&agrave;", "à")
            .replace("&Agrave;", "À")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("&#(\\d+);")) { match ->
                val code = match.groupValues[1].toIntOrNull()
                if (code != null) code.toChar().toString() else match.value
            }
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Divide o texto legal em páginas didáticas ideais para leitura nos Reels,
     * respeitando limites de artigos (sem cortar um artigo no meio).
     */
    fun paginateLawContent(fullText: String, targetCharsPerPage: Int = 1800): List<String> {
        val paragraphs = fullText.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) return listOf("Sem conteúdo disponível.")

        val pages = mutableListOf<String>()
        val currentPage = StringBuilder()

        for (p in paragraphs) {
            val isArticleStart = p.startsWith("Art.", ignoreCase = true) ||
                                p.startsWith("CAPÍTULO", ignoreCase = true) ||
                                p.startsWith("TÍTULO", ignoreCase = true) ||
                                p.startsWith("SEÇÃO", ignoreCase = true) ||
                                p.startsWith("LIVRO", ignoreCase = true)

            // Se atingiu o limite de caracteres e estamos no início de um novo artigo ou seção, fecha a página
            if (currentPage.length >= targetCharsPerPage && isArticleStart) {
                pages.add(currentPage.toString().trim())
                currentPage.clear()
            }

            if (currentPage.isNotEmpty()) {
                currentPage.append("\n\n")
            }
            currentPage.append(p)
        }

        if (currentPage.isNotBlank()) {
            pages.add(currentPage.toString().trim())
        }

        return if (pages.isEmpty()) listOf(fullText) else pages
    }

    private fun countArticles(text: String): Int {
        val matcher = Pattern.compile("Art\\.\\s*\\d+", Pattern.CASE_INSENSITIVE).matcher(text)
        var count = 0
        while (matcher.find()) {
            count++
        }
        return count
    }
}

sealed class ExtractedLawResult {
    data class Success(
        val title: String,
        val pages: List<String>,
        val sampleContentJson: String,
        val totalArticles: Int,
        val sourceUrl: String
    ) : ExtractedLawResult()

    data class Error(val message: String) : ExtractedLawResult()
}

sealed class StructuredLawResult {
    data class Success(
        val title: String,
        val articles: List<LawArticleItem>,
        val pages: List<String>,
        val sampleContentJson: String,
        val totalArticles: Int,
        val sourceUrl: String,
        val isFromCache: Boolean = false
    ) : StructuredLawResult()

    data class Error(val message: String) : StructuredLawResult()
}
