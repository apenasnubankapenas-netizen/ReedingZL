package com.example.data.epub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.data.model.PdfBook
import com.example.data.pdf.PdfRendererHelper
import com.example.data.pdf.PdfTocItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class EpubMetadata(
    val title: String,
    val author: String,
    val language: String = "pt",
    val coverEntryPath: String? = null
)

data class EpubPage(
    val pageNumber: Int,
    val chapterTitle: String,
    val text: String,
    val chapterIndex: Int
)

data class EpubBookInfo(
    val metadata: EpubMetadata,
    val pages: List<EpubPage>,
    val toc: List<PdfTocItem>,
    val coverBitmap: Bitmap? = null
)

object EpubReaderHelper {

    private const val TAG = "EpubReaderHelper"
    private val bookCache = ConcurrentHashMap<String, EpubBookInfo>()

    fun isEpub(uriOrPath: String?): Boolean {
        if (uriOrPath.isNullOrBlank()) return false
        val lower = uriOrPath.lowercase()
        return lower.endsWith(".epub") || lower.contains(".epub?") || lower.contains("epub_")
    }

    fun isEpub(book: PdfBook): Boolean {
        if (isEpub(book.fileUri)) return true
        if (book.sampleContentJson?.contains("\"format\":\"epub\"") == true) return true
        return false
    }

    /**
     * Extrai metadados preliminares (título, autor, capa) rapidamente de um arquivo EPUB
     */
    suspend fun extractEpubMetadataQuickly(context: Context, fileUri: String): EpubMetadata = withContext(Dispatchers.IO) {
        try {
            val file = resolveFileFromUri(context, fileUri) ?: return@withContext EpubMetadata("Livro EPUB", "Autor Desconhecido")
            ZipFile(file).use { zip ->
                val opfPath = getOpfPath(zip) ?: return@withContext EpubMetadata(file.nameWithoutExtension, "Autor Desconhecido")
                val opfEntry = zip.getEntry(opfPath) ?: return@withContext EpubMetadata(file.nameWithoutExtension, "Autor Desconhecido")
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                return@withContext parseMetadataFromOpf(opfContent, opfPath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair metadados rápidos do EPUB: ${e.message}", e)
            EpubMetadata("Livro EPUB", "Autor Desconhecido")
        }
    }

    suspend fun getEpubBookInfo(context: Context, fileUri: String?, book: PdfBook? = null): EpubBookInfo = withContext(Dispatchers.IO) {
        val key = fileUri ?: book?.id?.toString() ?: "unknown"
        val cached = bookCache[key]
        if (cached != null && cached.pages.isNotEmpty()) {
            return@withContext cached
        }

        // Se for um livro de amostra embutido em JSON com formato epub
        if (book?.sampleContentJson != null && book.sampleContentJson.contains("\"format\":\"epub\"")) {
            val samplePages = PdfRendererHelper.parseSamplePages(book.sampleContentJson)
            val pages = samplePages.mapIndexed { idx, txt ->
                EpubPage(
                    pageNumber = idx + 1,
                    chapterTitle = "Capítulo ${idx + 1}",
                    text = txt,
                    chapterIndex = idx
                )
            }
            val toc = pages.map {
                PdfTocItem(
                    title = it.chapterTitle,
                    pageNumber = it.pageNumber,
                    snippet = it.text.take(80).replace("\n", " ").trim() + "..."
                )
            }
            val info = EpubBookInfo(
                metadata = EpubMetadata(book.title, book.author),
                pages = pages,
                toc = toc
            )
            bookCache[key] = info
            return@withContext info
        }

        if (fileUri.isNullOrBlank()) {
            return@withContext createFallbackEpubInfo(book?.title ?: "Livro EPUB", book?.author ?: "Autor Desconhecido")
        }

        val file = resolveFileFromUri(context, fileUri)
        if (file == null || !file.exists() || file.length() == 0L) {
            return@withContext createFallbackEpubInfo(book?.title ?: "Livro EPUB", book?.author ?: "Autor Desconhecido")
        }

        try {
            ZipFile(file).use { zip ->
                val opfPath = getOpfPath(zip) ?: return@withContext createFallbackEpubInfo(file.nameWithoutExtension, "Autor Desconhecido")
                val opfEntry = zip.getEntry(opfPath) ?: return@withContext createFallbackEpubInfo(file.nameWithoutExtension, "Autor Desconhecido")
                val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }

                val metadata = parseMetadataFromOpf(opfContent, opfPath)
                val manifest = parseManifest(opfContent, opfDir)
                val spine = parseSpine(opfContent)

                // Extrai capa se disponível
                var coverBitmap: Bitmap? = null
                val coverPath = metadata.coverEntryPath ?: findCoverEntryPath(zip, manifest)
                if (!coverPath.isNullOrBlank()) {
                    val coverEntry = zip.getEntry(coverPath)
                    if (coverEntry != null) {
                        try {
                            coverBitmap = zip.getInputStream(coverEntry).use { input ->
                                BitmapFactory.decodeStream(input)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Não foi possível decodificar capa: ${e.message}")
                        }
                    }
                }

                // Processa os capítulos na ordem do spine e pagina o texto de forma confortável
                val allPages = mutableListOf<EpubPage>()
                val tocItems = mutableListOf<PdfTocItem>()
                var globalPageNumber = 1

                for ((chapterIdx, idref) in spine.withIndex()) {
                    val itemHref = manifest[idref] ?: continue
                    val entry = zip.getEntry(itemHref) ?: continue
                    val rawHtml = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                    val chapterTitle = extractChapterTitle(rawHtml) ?: "Capítulo ${chapterIdx + 1}"
                    val cleanText = htmlToPlainText(rawHtml)

                    if (cleanText.isBlank()) continue

                    val chapterStartPage = globalPageNumber
                    val paginatedPages = paginateText(cleanText, chapterTitle, chapterIdx, globalPageNumber)

                    for (p in paginatedPages) {
                        allPages.add(p)
                        globalPageNumber++
                    }

                    tocItems.add(
                        PdfTocItem(
                            title = chapterTitle,
                            pageNumber = chapterStartPage,
                            snippet = cleanText.take(100).replace("\n", " ").trim() + "..."
                        )
                    )
                }

                if (allPages.isEmpty()) {
                    allPages.add(
                        EpubPage(
                            pageNumber = 1,
                            chapterTitle = "Página Inicial",
                            text = "Este livro EPUB não contém capítulos de texto legíveis ou está vazio.",
                            chapterIndex = 0
                        )
                    )
                }

                val finalInfo = EpubBookInfo(
                    metadata = metadata,
                    pages = allPages,
                    toc = tocItems,
                    coverBitmap = coverBitmap
                )
                bookCache[key] = finalInfo
                return@withContext finalInfo
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao processar EPUB completo: ${e.message}", e)
            return@withContext createFallbackEpubInfo(book?.title ?: "Livro EPUB", book?.author ?: "Autor Desconhecido")
        }
    }

    suspend fun getEpubPageCount(context: Context, fileUri: String): Int {
        val info = getEpubBookInfo(context, fileUri)
        return info.pages.size.coerceAtLeast(1)
    }

    suspend fun getEpubPageText(context: Context, book: PdfBook, pageNumber: Int): String {
        val info = getEpubBookInfo(context, book.fileUri, book)
        val clamped = (pageNumber - 1).coerceIn(0, (info.pages.size - 1).coerceAtLeast(0))
        return info.pages.getOrNull(clamped)?.text ?: ""
    }

    suspend fun getEpubTableOfContents(context: Context, book: PdfBook): List<PdfTocItem> {
        val info = getEpubBookInfo(context, book.fileUri, book)
        return info.toc
    }

    suspend fun getEpubCoverBitmap(context: Context, book: PdfBook, destWidth: Int = 850): Bitmap? {
        val info = getEpubBookInfo(context, book.fileUri, book)
        if (info.coverBitmap != null && !info.coverBitmap.isRecycled) {
            return info.coverBitmap
        }
        // Gera capa tipográfica elegante se não tiver imagem interna
        return renderEpubCoverTypography(book.title, book.author, destWidth)
    }

    /**
     * Renderiza a página do EPUB como um Bitmap em alta definição para leitura visual nos Reels,
     * respeitando o tema (P&B, Sépia, Dark OLED, etc.) e alinhando com a tipografia do livro.
     */
    suspend fun renderEpubPageBitmap(
        context: Context,
        book: PdfBook,
        pageIndexZeroBased: Int,
        destWidth: Int = 1080,
        readingTheme: String = "MONOCHROME_BW"
    ): Bitmap? = withContext(Dispatchers.Default) {
        val info = getEpubBookInfo(context, book.fileUri, book)
        val pageNumber = pageIndexZeroBased + 1
        val page = info.pages.getOrNull(pageIndexZeroBased) ?: info.pages.lastOrNull() ?: return@withContext null

        val targetHeight = (destWidth * 1.55f).toInt().coerceIn(1200, 2400)
        val bitmap = try {
            Bitmap.createBitmap(destWidth, targetHeight, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            return@withContext null
        }

        val canvas = Canvas(bitmap)

        // Paleta de cores por tema
        val (bgColor, textColor, headerFooterColor, borderColor) = when (readingTheme) {
            "PAPER_SEPIA" -> Quad(Color.parseColor("#F9F4EB"), Color.parseColor("#2C2219"), Color.parseColor("#8C7B6C"), Color.parseColor("#E5DAC7"))
            "DARK_OLED" -> Quad(Color.parseColor("#000000"), Color.parseColor("#E2E8F0"), Color.parseColor("#64748B"), Color.parseColor("#1E293B"))
            "CINEMA_YELLOW" -> Quad(Color.parseColor("#121214"), Color.parseColor("#FDE047"), Color.parseColor("#A1A1AA"), Color.parseColor("#27272A"))
            "TERMINAL_GREEN" -> Quad(Color.parseColor("#0D1117"), Color.parseColor("#39D353"), Color.parseColor("#238636"), Color.parseColor("#161B22"))
            "CYBERPUNK_NEON" -> Quad(Color.parseColor("#0B0F19"), Color.parseColor("#38BDF8"), Color.parseColor("#818CF8"), Color.parseColor("#1E293B"))
            else -> Quad(Color.parseColor("#FFFFFF"), Color.parseColor("#111827"), Color.parseColor("#6B7280"), Color.parseColor("#E5E7EB"))
        }

        // 1. Fundo
        canvas.drawColor(bgColor)

        val marginHorizontal = (destWidth * 0.08f).toInt()
        val marginTop = (targetHeight * 0.065f).toInt()
        val marginBottom = (targetHeight * 0.065f).toInt()
        val contentWidth = destWidth - (marginHorizontal * 2)
        val maxContentHeight = targetHeight - marginTop - marginBottom

        // 2. Cabeçalho (Título do Capítulo / Livro)
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = headerFooterColor
            textSize = destWidth * 0.026f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val headerText = page.chapterTitle.uppercase().take(42)
        canvas.drawText(headerText, marginHorizontal.toFloat(), marginTop * 0.7f, headerPaint)

        // Linha sutil separadora abaixo do cabeçalho
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            strokeWidth = 1.5f
        }
        canvas.drawLine(
            marginHorizontal.toFloat(),
            marginTop * 0.82f,
            (destWidth - marginHorizontal).toFloat(),
            marginTop * 0.82f,
            linePaint
        )

        // 3. Corpo do Texto com StaticLayout
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = destWidth * 0.040f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(page.text, 0, page.text.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(destWidth * 0.012f, 1.45f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                page.text,
                bodyPaint,
                contentWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.45f,
                destWidth * 0.012f,
                true
            )
        }

        canvas.save()
        canvas.translate(marginHorizontal.toFloat(), marginTop.toFloat())
        staticLayout.draw(canvas)
        canvas.restore()

        // 4. Rodapé (Número de Página)
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = headerFooterColor
            textSize = destWidth * 0.027f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val footerText = "— ${page.pageNumber} de ${info.pages.size} —"
        canvas.drawText(
            footerText,
            destWidth / 2f,
            targetHeight - (marginBottom * 0.45f),
            footerPaint
        )

        bitmap
    }

    private fun renderEpubCoverTypography(title: String, author: String, destWidth: Int): Bitmap {
        val targetHeight = (destWidth * 1.5f).toInt()
        val bitmap = Bitmap.createBitmap(destWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fundo em gradiente suave escuro
        canvas.drawColor(Color.parseColor("#181824"))

        // Borda decorativa dourada fina
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val rect = Rect((destWidth * 0.06f).toInt(), (targetHeight * 0.05f).toInt(), (destWidth * 0.94f).toInt(), (targetHeight * 0.95f).toInt())
        canvas.drawRect(rect, borderPaint)

        // Badge EPUB
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            textSize = destWidth * 0.035f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("EDIÇÃO EPUB DIGITAL", destWidth / 2f, targetHeight * 0.18f, badgePaint)

        // Título
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = destWidth * 0.065f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Quebra o título em linhas
        val words = title.split(" ")
        val lines = mutableListOf<String>()
        var cur = ""
        for (w in words) {
            if (cur.length + w.length > 20) {
                lines.add(cur.trim())
                cur = "$w "
            } else {
                cur += "$w "
            }
        }
        if (cur.isNotBlank()) lines.add(cur.trim())

        var y = targetHeight * 0.42f
        for (line in lines) {
            canvas.drawText(line, destWidth / 2f, y, titlePaint)
            y += destWidth * 0.08f
        }

        // Autor
        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = destWidth * 0.042f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(author, destWidth / 2f, targetHeight * 0.82f, authorPaint)

        return bitmap
    }

    // ==========================================
    // Utilitários de Parsing EPUB
    // ==========================================

    private fun getOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        val xml = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
        val match = Regex("""full-path\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(xml)
        return match?.groupValues?.getOrNull(1)
    }

    private fun parseMetadataFromOpf(opfXml: String, opfPath: String): EpubMetadata {
        val titleMatch = Regex("""<dc:title[^>]*>([^<]+)</dc:title>""", RegexOption.IGNORE_CASE).find(opfXml)
        val authorMatch = Regex("""<dc:creator[^>]*>([^<]+)</dc:creator>""", RegexOption.IGNORE_CASE).find(opfXml)
        val langMatch = Regex("""<dc:language[^>]*>([^<]+)</dc:language>""", RegexOption.IGNORE_CASE).find(opfXml)

        val title = titleMatch?.groupValues?.getOrNull(1)?.trim() ?: "Livro EPUB"
        val author = authorMatch?.groupValues?.getOrNull(1)?.trim() ?: "Autor Desconhecido"
        val lang = langMatch?.groupValues?.getOrNull(1)?.trim() ?: "pt"

        // Busca cover id ou href
        var coverHref: String? = null
        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

        val coverMetaMatch = Regex("""<meta[^>]+name\s*=\s*["']cover["'][^>]+content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(opfXml)
            ?: Regex("""<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+name\s*=\s*["']cover["']""", RegexOption.IGNORE_CASE).find(opfXml)
        val coverId = coverMetaMatch?.groupValues?.getOrNull(1)

        if (!coverId.isNullOrBlank()) {
            val itemMatch = Regex("""<item[^>]+id\s*=\s*["']${Regex.escape(coverId)}["'][^>]+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(opfXml)
            val href = itemMatch?.groupValues?.getOrNull(1)
            if (!href.isNullOrBlank()) {
                coverHref = opfDir + href
            }
        }

        if (coverHref.isNullOrBlank()) {
            val coverPropMatch = Regex("""<item[^>]+properties\s*=\s*["'][^"']*cover-image[^"']*["'][^>]+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(opfXml)
            val href = coverPropMatch?.groupValues?.getOrNull(1)
            if (!href.isNullOrBlank()) {
                coverHref = opfDir + href
            }
        }

        return EpubMetadata(
            title = title,
            author = author,
            language = lang,
            coverEntryPath = coverHref
        )
    }

    private fun parseManifest(opfXml: String, opfDir: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val itemRegex = Regex("""<item\s+([^>]+)/>|<item\s+([^>]+)>""", RegexOption.IGNORE_CASE)
        for (match in itemRegex.findAll(opfXml)) {
            val attrs = (match.groupValues[1].ifBlank { match.groupValues[2] })
            val idMatch = Regex("""id\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
            val hrefMatch = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
            if (idMatch != null && hrefMatch != null) {
                val id = idMatch.groupValues[1]
                val href = hrefMatch.groupValues[1]
                map[id] = opfDir + href
            }
        }
        return map
    }

    private fun parseSpine(opfXml: String): List<String> {
        val list = mutableListOf<String>()
        val spineRegex = Regex("""<itemref[^>]+idref\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        for (match in spineRegex.findAll(opfXml)) {
            list.add(match.groupValues[1])
        }
        return list
    }

    private fun findCoverEntryPath(zip: ZipFile, manifest: Map<String, String>): String? {
        for ((id, path) in manifest) {
            val lower = id.lowercase()
            if (lower.contains("cover") && (path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) || path.endsWith(".png", true))) {
                if (zip.getEntry(path) != null) return path
            }
        }
        // Fallback: varre entradas do zip
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val lower = entry.name.lowercase()
            if (lower.contains("cover") && (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
                return entry.name
            }
        }
        return null
    }

    private fun extractChapterTitle(html: String): String? {
        val h1 = Regex("""<h1[^>]*>([^<]+)</h1>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)
        if (!h1.isNullOrBlank()) return cleanTitle(h1)

        val h2 = Regex("""<h2[^>]*>([^<]+)</h2>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)
        if (!h2.isNullOrBlank()) return cleanTitle(h2)

        val title = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)
        if (!title.isNullOrBlank() && !title.equals("unknown", true) && !title.endsWith(".xhtml", true)) {
            return cleanTitle(title)
        }
        return null
    }

    private fun cleanTitle(str: String): String {
        return Html.fromHtml(str, Html.FROM_HTML_MODE_COMPACT).toString().trim().replace("\n", " ")
    }

    fun htmlToPlainText(html: String): String {
        var processed = html
        // Remove scripts e estilos
        processed = processed.replace(Regex("""<script[^>]*>.*?</script>""", RegexOption.DOT_MATCHES_ALL), "")
        processed = processed.replace(Regex("""<style[^>]*>.*?</style>""", RegexOption.DOT_MATCHES_ALL), "")
        // Converte quebras de bloco em parágrafos duplos
        processed = processed.replace(Regex("""<(p|div|h1|h2|h3|h4|h5|h6|li|blockquote)[^>]*>""", RegexOption.IGNORE_CASE), "\n\n")
        processed = processed.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")

        val spanned = Html.fromHtml(processed, Html.FROM_HTML_MODE_COMPACT)
        val text = spanned.toString()
        return PdfRendererHelper.cleanAndReorganizePdfText(text)
    }

    /**
     * Pagina o texto do capítulo em blocos confortáveis de ~1200 a 1600 caracteres,
     * respeitando fronteiras de parágrafos e frases.
     */
    private fun paginateText(
        text: String,
        chapterTitle: String,
        chapterIndex: Int,
        startPageNumber: Int
    ): List<EpubPage> {
        val paragraphs = text.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) {
            return listOf(
                EpubPage(
                    pageNumber = startPageNumber,
                    chapterTitle = chapterTitle,
                    text = text,
                    chapterIndex = chapterIndex
                )
            )
        }

        val pages = mutableListOf<EpubPage>()
        var currentChunk = StringBuilder()
        var curPage = startPageNumber

        for (para in paragraphs) {
            // Se o parágrafo atual sozinho for gigante (> 1800 chars), quebra por sentenças
            if (para.length > 1800) {
                val sentences = PdfRendererHelper.splitTextIntoSentences(para)
                for (s in sentences) {
                    if (currentChunk.length + s.length > 1500 && currentChunk.isNotBlank()) {
                        pages.add(
                            EpubPage(
                                pageNumber = curPage++,
                                chapterTitle = chapterTitle,
                                text = currentChunk.toString().trim(),
                                chapterIndex = chapterIndex
                            )
                        )
                        currentChunk = StringBuilder()
                    }
                    if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                    currentChunk.append(s)
                }
                continue
            }

            if (currentChunk.length + para.length > 1500 && currentChunk.isNotBlank()) {
                pages.add(
                    EpubPage(
                        pageNumber = curPage++,
                        chapterTitle = chapterTitle,
                        text = currentChunk.toString().trim(),
                        chapterIndex = chapterIndex
                    )
                )
                currentChunk = StringBuilder()
            }

            if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
            currentChunk.append(para)
        }

        if (currentChunk.isNotBlank()) {
            pages.add(
                EpubPage(
                    pageNumber = curPage,
                    chapterTitle = chapterTitle,
                    text = currentChunk.toString().trim(),
                    chapterIndex = chapterIndex
                )
            )
        }

        return pages
    }

    private fun resolveFileFromUri(context: Context, uriString: String): File? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                File(uri.path ?: uriString)
            } else {
                val cached = File(context.filesDir, "epubs/" + uriString.hashCode() + ".epub")
                if (cached.exists() && cached.length() > 0) cached else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun createFallbackEpubInfo(title: String, author: String): EpubBookInfo {
        val sample = listOf(
            EpubPage(
                pageNumber = 1,
                chapterTitle = "Capítulo 1",
                text = "Bem-vindo à leitura de \"$title\" por $author em formato EPUB. O aplicativo sincroniza cada frase com áudio neural, suporte a citações, marcações, dicionário e IA em tempo real.",
                chapterIndex = 0
            )
        )
        return EpubBookInfo(
            metadata = EpubMetadata(title, author),
            pages = sample,
            toc = listOf(PdfTocItem(title = "Capítulo 1", pageNumber = 1, snippet = "Início da obra em EPUB"))
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
