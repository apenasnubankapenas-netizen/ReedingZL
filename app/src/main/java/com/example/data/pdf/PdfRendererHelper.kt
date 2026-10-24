package com.example.data.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.PdfBook
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

data class PdfTocItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val pageNumber: Int,
    val level: Int = 0,
    val sectionNumber: String = "",
    val snippet: String = "",
    val links: List<String> = emptyList(),
    val children: List<PdfTocItem> = emptyList()
)

object PdfRendererHelper {

    private const val TAG = "PdfRendererHelper"
    private val textCache = ConcurrentHashMap<String, String>()
    private val maxMemoryBytes = Runtime.getRuntime().maxMemory().toInt()
    private val cacheSizeBytes = (maxMemoryBytes / 8).coerceIn(16 * 1024 * 1024, 48 * 1024 * 1024)
    private val bitmapCache = object : android.util.LruCache<String, Bitmap>(cacheSizeBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }
    private val tocCache = ConcurrentHashMap<Int, List<PdfTocItem>>()
    private val pdfRenderMutex = Mutex()
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Permite injetar diretamente texto didático/científico reestruturado para a página,
     * substituindo texto bugado com fórmulas quebradas.
     */
    fun setCustomPageText(bookId: Int, pageNumber: Int, text: String) {
        if (text.isNotBlank()) {
            val cacheKey = "book_${bookId}_page_$pageNumber"
            textCache[cacheKey] = text
            textCache["${bookId}_$pageNumber"] = text
        }
    }

    private fun getCustomPageTextFile(context: Context, bookId: Int, pageNumber: Int): File {
        val dir = File(context.filesDir, "custom_page_texts")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "book_${bookId}_page_${pageNumber}.txt")
    }

    fun getCustomPageLanguageTextFile(context: Context, bookId: Int, pageNumber: Int, langCode: String): File {
        val dir = File(context.filesDir, "custom_page_texts")
        if (!dir.exists()) dir.mkdirs()
        val cleanCode = langCode.lowercase().trim()
        return if (cleanCode == "pt" || cleanCode == "portuguese") {
            File(dir, "book_${bookId}_page_${pageNumber}.txt")
        } else {
            File(dir, "book_${bookId}_page_${pageNumber}_lang_${cleanCode}.txt")
        }
    }

    fun hasCustomPageText(context: Context, bookId: Int, pageNumber: Int): Boolean {
        val file = getCustomPageTextFile(context, bookId, pageNumber)
        return file.exists() && file.length() > 0
    }

    fun hasCustomPageLanguageText(context: Context, bookId: Int, pageNumber: Int, langCode: String): Boolean {
        val file = getCustomPageLanguageTextFile(context, bookId, pageNumber, langCode)
        return file.exists() && file.length() > 0
    }

    fun hasAnySavedCustomPageLanguage(context: Context, bookId: Int, pageNumber: Int): Boolean {
        if (hasCustomPageText(context, bookId, pageNumber)) return true
        val dir = File(context.filesDir, "custom_page_texts")
        if (!dir.exists()) return false
        val prefix = "book_${bookId}_page_${pageNumber}_lang_"
        return dir.listFiles()?.any { it.name.startsWith(prefix) && it.length() > 0 } == true
    }

    fun getSavedCustomPageText(context: Context, bookId: Int, pageNumber: Int): String? {
        val cacheKey = "book_${bookId}_page_$pageNumber"
        val inMemory = textCache[cacheKey]
        if (!inMemory.isNullOrBlank() && hasCustomPageText(context, bookId, pageNumber)) {
            return inMemory
        }
        val file = getCustomPageTextFile(context, bookId, pageNumber)
        if (file.exists() && file.length() > 0) {
            val text = try { file.readText().trim() } catch (_: Exception) { null }
            if (!text.isNullOrBlank()) {
                textCache[cacheKey] = text
                textCache["${bookId}_$pageNumber"] = text
                return text
            }
        }
        return null
    }

    fun getSavedCustomPageLanguageText(context: Context, bookId: Int, pageNumber: Int, langCode: String): String? {
        val cleanCode = langCode.lowercase().trim()
        if (cleanCode == "pt" || cleanCode == "portuguese") {
            return getSavedCustomPageText(context, bookId, pageNumber)
        }
        val cacheKey = "book_${bookId}_page_${pageNumber}_lang_${cleanCode}"
        val inMemory = textCache[cacheKey]
        if (!inMemory.isNullOrBlank() && hasCustomPageLanguageText(context, bookId, pageNumber, cleanCode)) {
            return inMemory
        }
        val file = getCustomPageLanguageTextFile(context, bookId, pageNumber, cleanCode)
        if (file.exists() && file.length() > 0) {
            val text = try { file.readText().trim() } catch (_: Exception) { null }
            if (!text.isNullOrBlank()) {
                textCache[cacheKey] = text
                return text
            }
        }
        return null
    }

    fun saveCustomPageText(context: Context, bookId: Int, pageNumber: Int, text: String) {
        val trimmed = text.trim()
        val cacheKey = "book_${bookId}_page_$pageNumber"
        if (trimmed.isNotBlank()) {
            textCache[cacheKey] = trimmed
            textCache["${bookId}_$pageNumber"] = trimmed
            try {
                val file = getCustomPageTextFile(context, bookId, pageNumber)
                file.writeText(trimmed)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing custom page text to disk: ${e.message}", e)
            }
        }
    }

    fun saveCustomPageLanguageText(context: Context, bookId: Int, pageNumber: Int, langCode: String, text: String) {
        val cleanCode = langCode.lowercase().trim()
        if (cleanCode == "pt" || cleanCode == "portuguese") {
            saveCustomPageText(context, bookId, pageNumber, text)
            return
        }
        val trimmed = text.trim()
        val cacheKey = "book_${bookId}_page_${pageNumber}_lang_${cleanCode}"
        if (trimmed.isNotBlank()) {
            textCache[cacheKey] = trimmed
            try {
                val file = getCustomPageLanguageTextFile(context, bookId, pageNumber, cleanCode)
                file.writeText(trimmed)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing custom page language text to disk: ${e.message}", e)
            }
        }
    }

    fun deleteCustomPageText(context: Context, bookId: Int, pageNumber: Int) {
        val cacheKey = "book_${bookId}_page_$pageNumber"
        textCache.remove(cacheKey)
        textCache.remove("${bookId}_$pageNumber")
        try {
            val file = getCustomPageTextFile(context, bookId, pageNumber)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting custom page text: ${e.message}", e)
        }
    }

    private fun isFallbackText(text: String?, book: PdfBook): Boolean {
        if (text.isNullOrBlank()) return true
        val trimmed = text.trim()
        if (trimmed.startsWith("Página") && trimmed.contains("do livro \"${book.title}\"") && trimmed.length < 150) {
            return true
        }
        if (trimmed.startsWith("Página ") && trimmed.contains(" do livro ") && trimmed.length < 90) {
            return true
        }
        return false
    }

    /**
     * Valida rigorosamente se uma string extraída é texto legível de leitura humana.
     * Rejeita fragmentos desconexos de fontes/glifos ("sopa de letrinhas do alfabeto")
     * e sequências de letras isoladas separadas por espaços geradas por operadores Tj corrompidos.
     */
    fun isReadableNaturalText(text: String?): Boolean {
        if (text.isNullOrBlank() || text.length < 15) return false
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size < 3) return false

        // Se mais de 20% das palavras forem letras avulsas isoladas (ex: "a b c d e f"), é lixo de fonte/Tj desfragmentado
        val singleLetterWords = words.count { it.length == 1 && it[0].isLetter() }
        if (singleLetterWords.toFloat() / words.size.toFloat() > 0.20f) {
            return false
        }

        // Média de tamanho das palavras: em português e inglês é tipicamente entre 3.0 e 15.0
        val avgLength = words.map { it.length }.average()
        if (avgLength < 2.4 || avgLength > 22.0) {
            return false
        }

        // Deve conter uma proporção saudável de caracteres alfanuméricos e espaços normais
        val normalChars = text.count { it.isLetterOrDigit() || it.isWhitespace() }
        if (normalChars.toFloat() / text.length.toFloat() < 0.60f) {
            return false
        }

        return true
    }

    fun clearTextCache() {
        textCache.clear()
    }

    fun clearTocCache(bookId: Int? = null) {
        if (bookId != null) {
            tocCache.remove(bookId)
        } else {
            tocCache.clear()
        }
    }

    /**
     * Copies any URI (content://, file://, etc.) to internal app storage safely with buffered streams,
     * allowing zero-permission restrictions, persistent fast access, and instant PDF rendering.
     */
    suspend fun copyUriToInternalStorage(context: Context, uri: Uri, targetFileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDir = File(context.filesDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val destinationFile = File(pdfDir, targetFileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }
            if (destinationFile.exists() && destinationFile.length() > 0) {
                destinationFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to internal storage: ${e.message}", e)
            null
        }
    }

    /**
     * Resolves a ParcelFileDescriptor for any URI type, with automatic persistent fallback caching.
     */
    fun openPdfParcelFileDescriptor(context: Context, fileUri: String?): ParcelFileDescriptor? {
        if (fileUri.isNullOrBlank()) return null
        try {
            val uri = Uri.parse(fileUri)
            
            // 1. Direct file path or file:// URI
            if (fileUri.startsWith("file://") || fileUri.startsWith("/")) {
                val filePath = if (fileUri.startsWith("file://")) fileUri.removePrefix("file://") else fileUri
                val file = File(filePath)
                if (file.exists() && file.canRead()) {
                    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                }
            }

            // 2. content:// URI - try take persistable permission first
            if (fileUri.startsWith("content://")) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val pfd = try {
                    context.contentResolver.openFileDescriptor(uri, "r")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed direct openFileDescriptor for $fileUri: ${e.message}")
                    null
                }

                if (pfd != null) {
                    return pfd
                }

                // 3. Fallback: check cached copy in files/cache dir
                val cachedFile = File(context.cacheDir, "cached_pdf_${kotlin.math.abs(uri.hashCode())}.pdf")
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                }

                // Copy stream to cache file ONLY if reasonably sized (<100MB)
                val statSize = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                } catch (_: Exception) { -1L }

                if (statSize in 1L..(100L * 1024L * 1024L)) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(cachedFile).use { output ->
                            val buffer = ByteArray(65536)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }

                    if (cachedFile.exists() && cachedFile.length() > 0) {
                        return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF ParcelFileDescriptor: ${e.message}", e)
        }
        return null
    }

    suspend fun getPdfPageCount(context: Context, fileUri: String?): Int = withContext(Dispatchers.IO) {
        if (fileUri.isNullOrBlank()) return@withContext 1
        if (com.example.data.epub.EpubReaderHelper.isEpub(fileUri)) {
            return@withContext com.example.data.epub.EpubReaderHelper.getEpubPageCount(context, fileUri)
        }
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = openPdfParcelFileDescriptor(context, fileUri)
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                return@withContext count.coerceAtLeast(1)
            }
            1
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF page count: ${e.message}", e)
            1
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    suspend fun renderPdfPageBitmap(
        context: Context,
        fileUri: String?,
        pageIndexZeroBased: Int,
        destWidth: Int = 1080,
        readingTheme: String = "MONOCHROME_BW"
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (fileUri.isNullOrBlank()) return@withContext null
        val cacheKey = "${fileUri}_${pageIndexZeroBased}_${destWidth}_${readingTheme}"
        val cached = bitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        if (com.example.data.epub.EpubReaderHelper.isEpub(fileUri)) {
            val dummyBook = PdfBook(id = fileUri.hashCode(), title = "Livro EPUB", fileUri = fileUri)
            val epubBitmap = com.example.data.epub.EpubReaderHelper.renderEpubPageBitmap(
                context = context,
                book = dummyBook,
                pageIndexZeroBased = pageIndexZeroBased,
                destWidth = destWidth,
                readingTheme = readingTheme
            )
            if (epubBitmap != null) {
                bitmapCache.put(cacheKey, epubBitmap)
                return@withContext epubBitmap
            }
        }

        pdfRenderMutex.withLock {
            // Checar cache novamente após obter o lock para evitar re-renderização redundante
            val cachedUnderLock = bitmapCache.get(cacheKey)
            if (cachedUnderLock != null && !cachedUnderLock.isRecycled) {
                return@withContext cachedUnderLock
            }

            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null

            try {
                pfd = openPdfParcelFileDescriptor(context, fileUri) ?: return@withContext null
                renderer = PdfRenderer(pfd)
                
                if (pageIndexZeroBased < 0 || pageIndexZeroBased >= renderer.pageCount) {
                    return@withContext null
                }

                page = renderer.openPage(pageIndexZeroBased)
                val pageRatio = page.height.toFloat() / page.width.toFloat()
                val targetHeight = (destWidth * pageRatio).toInt().coerceIn(100, 3600)

                val originalBitmap = try {
                    Bitmap.createBitmap(destWidth, targetHeight, Bitmap.Config.ARGB_8888)
                } catch (oom: OutOfMemoryError) {
                    Log.w(TAG, "Memória baixa ao alocar $destWidth x $targetHeight. Esvaziando cache e reduzindo...", oom)
                    bitmapCache.evictAll()
                    System.gc()
                    val reducedW = (destWidth * 3 / 4).coerceAtLeast(360)
                    val reducedH = (targetHeight * 3 / 4).coerceAtLeast(480)
                    Bitmap.createBitmap(reducedW, reducedH, Bitmap.Config.RGB_565)
                }

                // CRITICAL: erase to pure white so transparent PDF pages do not render black-on-black
                originalBitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(originalBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val finalBitmap = if (readingTheme == "DEFAULT" || readingTheme == "ORIGINAL") {
                    originalBitmap
                } else {
                    val filtered = applyColorFilter(originalBitmap, readingTheme)
                    if (filtered != originalBitmap) {
                        try { originalBitmap.recycle() } catch (_: Throwable) {}
                    }
                    filtered
                }

                bitmapCache.put(cacheKey, finalBitmap)
                finalBitmap
            } catch (oom: OutOfMemoryError) {
                Log.e(TAG, "OutOfMemoryError em renderPdfPageBitmap: ${oom.message}", oom)
                bitmapCache.evictAll()
                System.gc()
                null
            } catch (t: Throwable) {
                Log.e(TAG, "Error rendering PDF page bitmap: ${t.message}", t)
                null
            } finally {
                try { page?.close() } catch (_: Throwable) {}
                try { renderer?.close() } catch (_: Throwable) {}
                try { pfd?.close() } catch (_: Throwable) {}
            }
        }
    }

    suspend fun extractParagraphsAndSentencesFromPage(
        context: Context,
        book: PdfBook,
        pageNumber: Int
    ): Pair<List<BookParagraph>, List<String>> = withContext(Dispatchers.IO) {
        val cacheKey = "book_${book.id}_page_$pageNumber"

        // 0. Verifica primeiro se há texto personalizado / reestruturado salvo permanentemente no dispositivo
        val customSavedText = getSavedCustomPageText(context, book.id, pageNumber)
        if (!customSavedText.isNullOrBlank()) {
            return@withContext extractParagraphsAndSentences(customSavedText)
        }

        val cached = textCache[cacheKey]
        // Só reutiliza o cache se o texto for legível de verdade e não for fallback
        if (!cached.isNullOrBlank() && isReadableNaturalText(cached) && !isFallbackText(cached, book)) {
            return@withContext extractParagraphsAndSentences(cached)
        }

        // EPUB: extração de texto perfeita diretamente dos capítulos estruturados
        if (com.example.data.epub.EpubReaderHelper.isEpub(book)) {
            val epubText = com.example.data.epub.EpubReaderHelper.getEpubPageText(context, book, pageNumber)
            if (epubText.isNotBlank()) {
                textCache[cacheKey] = epubText
                textCache["${book.id}_$pageNumber"] = epubText
                return@withContext extractParagraphsAndSentences(epubText)
            }
        }

        // 1. Se sampleContentJson estiver presente (livros modelo / pré-instalados):
        val samplePages = parseSamplePages(book.sampleContentJson)
        if (samplePages.isNotEmpty()) {
            val pageIdx = (pageNumber - 1).coerceIn(0, samplePages.size - 1)
            val text = samplePages[pageIdx]
            if (text.isNotBlank() && isReadableNaturalText(text) && !isFallbackText(text, book)) {
                textCache[cacheKey] = text
                textCache["${book.id}_$pageNumber"] = text
                return@withContext extractParagraphsAndSentences(text)
            }
        }

        // 2. Para arquivos PDF reais com fileUri: ML Kit OCR visual de alta resolução
        // O OCR visual é 100% imune a encodings de fontes CMap/CID e lê as palavras reais impressas
        if (!book.fileUri.isNullOrBlank()) {
            try {
                val pageBitmap = renderPdfPageBitmap(
                    context = context,
                    fileUri = book.fileUri,
                    pageIndexZeroBased = (pageNumber - 1).coerceAtLeast(0),
                    destWidth = 1100,
                    readingTheme = "ORIGINAL"
                )

                if (pageBitmap != null) {
                    val inputImage = InputImage.fromBitmap(pageBitmap, 0)
                    val visionText = recognizer.process(inputImage).await()
                    
                    // Ordena os blocos de leitura no sentido ocidental: de cima para baixo, depois esquerda para a direita
                    val sortedBlocks = visionText.textBlocks.sortedWith(
                        compareBy<com.google.mlkit.vision.text.Text.TextBlock> { (it.boundingBox?.top ?: 0) / 25 }
                            .thenBy { it.boundingBox?.left ?: 0 }
                    )
                    
                    var fullText = if (sortedBlocks.isNotEmpty()) {
                        val fullTextBuilder = StringBuilder()
                        for (i in sortedBlocks.indices) {
                            val block = sortedBlocks[i]
                            val blockText = block.lines
                                .sortedBy { it.boundingBox?.top ?: 0 }
                                .joinToString(" ") { it.text.trim() }
                                .trim()
                            if (blockText.isBlank()) continue

                            if (fullTextBuilder.isNotEmpty()) {
                                val lastChar = fullTextBuilder.trimEnd().lastOrNull()
                                val firstChar = blockText.firstOrNull()
                                val isSentenceContinuation = (lastChar != null && lastChar !in listOf('.', '!', '?', '…', ':')) ||
                                        (firstChar != null && (firstChar.isLowerCase() || firstChar in listOf(',', ';', ')', ']')))
                                if (isSentenceContinuation) {
                                    fullTextBuilder.append(" ")
                                } else {
                                    fullTextBuilder.append("\n\n")
                                }
                            }
                            fullTextBuilder.append(blockText)
                        }
                        fullTextBuilder.toString().trim()
                    } else {
                        visionText.text.trim()
                    }

                    // Se fullText estiver vazio, tenta com realce de contraste monocromático
                    if (fullText.isBlank()) {
                        val enhanced = applyColorFilter(pageBitmap, "MONOCHROME_BW")
                        val enhancedImage = InputImage.fromBitmap(enhanced, 0)
                        val enhancedVision = recognizer.process(enhancedImage).await()
                        fullText = enhancedVision.text.trim()
                    }

                    if (fullText.isNotBlank()) {
                        val cleaned = cleanAndReorganizePdfText(fullText)
                        if (isReadableNaturalText(cleaned) && !isFallbackText(cleaned, book)) {
                            textCache[cacheKey] = cleaned
                            textCache["${book.id}_$pageNumber"] = cleaned
                            return@withContext extractParagraphsAndSentences(cleaned)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OCR extraction error for page $pageNumber: ${e.message}", e)
            }
        }

        // 3. Fallback para extração nativa de streams vetoriais, COM filtro estrito de legibilidade
        try {
            val nativeText = PdfOutlineExtractor.extractPageNativeText(context, book, pageNumber)
            if (!nativeText.isNullOrBlank() && isReadableNaturalText(nativeText)) {
                val cleaned = cleanAndReorganizePdfText(nativeText)
                if (isReadableNaturalText(cleaned) && !isFallbackText(cleaned, book)) {
                    textCache[cacheKey] = cleaned
                    textCache["${book.id}_$pageNumber"] = cleaned
                    return@withContext extractParagraphsAndSentences(cleaned)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha na extração de texto nativo para pág $pageNumber: ${e.message}")
        }

        // 4. Se a página for puramente visual (ilustração artística, capa gráfica ou diagrama):
        val tocItem = tocCache[book.id]?.find { it.pageNumber == pageNumber }
        val fallback = if (tocItem != null && tocItem.title.isNotBlank()) {
            "${tocItem.title}\n\n[Página $pageNumber contém ilustração gráfica da obra \"${book.title}\"]"
        } else {
            "Página $pageNumber • Conteúdo visual de \"${book.title}\""
        }
        // NUNCA armazenar fallback no cache
        extractParagraphsAndSentences(fallback)
    }

    suspend fun extractTextFromPage(
        context: Context,
        book: PdfBook,
        pageNumber: Int
    ): List<String> = withContext(Dispatchers.IO) {
        extractParagraphsAndSentencesFromPage(context, book, pageNumber).second
    }

    suspend fun extractRawOriginalTextFromPage(
        context: Context,
        book: PdfBook,
        pageNumber: Int
    ): String = withContext(Dispatchers.IO) {
        if (com.example.data.epub.EpubReaderHelper.isEpub(book)) {
            val epubText = com.example.data.epub.EpubReaderHelper.getEpubPageText(context, book, pageNumber)
            if (epubText.isNotBlank()) return@withContext epubText
        }

        // Extrai o conteúdo original puro do PDF ou amostra ignorando o custom text
        val samplePages = parseSamplePages(book.sampleContentJson)
        if (samplePages.isNotEmpty()) {
            val pageIdx = (pageNumber - 1).coerceIn(0, samplePages.size - 1)
            val text = samplePages[pageIdx]
            if (text.isNotBlank() && isReadableNaturalText(text) && !isFallbackText(text, book)) {
                return@withContext text
            }
        }

        if (!book.fileUri.isNullOrBlank()) {
            try {
                val pageBitmap = renderPdfPageBitmap(
                    context = context,
                    fileUri = book.fileUri,
                    pageIndexZeroBased = (pageNumber - 1).coerceAtLeast(0),
                    destWidth = 1100,
                    readingTheme = "ORIGINAL"
                )
                if (pageBitmap != null) {
                    val inputImage = InputImage.fromBitmap(pageBitmap, 0)
                    val visionText = recognizer.process(inputImage).await()
                    val sortedBlocks = visionText.textBlocks.sortedWith(
                        compareBy<com.google.mlkit.vision.text.Text.TextBlock> { (it.boundingBox?.top ?: 0) / 25 }
                            .thenBy { it.boundingBox?.left ?: 0 }
                    )
                    if (sortedBlocks.isNotEmpty()) {
                        val fullTextBuilder = StringBuilder()
                        for (i in sortedBlocks.indices) {
                            val blockText = sortedBlocks[i].lines
                                .sortedBy { it.boundingBox?.top ?: 0 }
                                .joinToString(" ") { it.text.trim() }
                                .trim()
                            if (blockText.isBlank()) continue
                            if (fullTextBuilder.isNotEmpty()) fullTextBuilder.append("\n\n")
                            fullTextBuilder.append(blockText)
                        }
                        val result = cleanAndReorganizePdfText(fullTextBuilder.toString().trim())
                        if (result.isNotBlank()) return@withContext result
                    }
                }
            } catch (_: Exception) {}
        }
        return@withContext ""
    }

    fun extractLinksFromText(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val urlRegex = Regex("""(https?://[^\s<>"]+|www\.[^\s<>"]+)""", RegexOption.IGNORE_CASE)
        return urlRegex.findAll(text).map { match ->
            val url = match.value.trimEnd('.', ',', ';', ':', ')', ']')
            if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        }.distinct().toList()
    }

    suspend fun generateTableOfContents(
        context: Context,
        book: PdfBook,
        forceRefresh: Boolean = false
    ): List<PdfTocItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = tocCache[book.id]
            if (!cached.isNullOrEmpty()) {
                return@withContext cached
            }
        }

        // EPUB: extração de sumário a partir do TOC / capítulos do EPUB
        if (com.example.data.epub.EpubReaderHelper.isEpub(book)) {
            val epubToc = com.example.data.epub.EpubReaderHelper.getEpubTableOfContents(context, book)
            if (epubToc.isNotEmpty()) {
                val tree = buildTocTree(epubToc)
                tocCache[book.id] = tree
                return@withContext tree
            }
        }

        val samplePages = parseSamplePages(book.sampleContentJson)
        val items = mutableListOf<PdfTocItem>()

        // 1. Extração nativa de alta fidelidade (ISO 32000-1) igual ao Microsoft Edge / Adobe
        try {
            val (nativeBookmarks, _) = PdfOutlineExtractor.extractOutlineAndLinks(context, book)
            if (nativeBookmarks.isNotEmpty()) {
                Log.d(TAG, "Sumário nativo reconhecido: ${nativeBookmarks.size} tópicos raiz encontrados para o livro ${book.id} (${book.title}).")
                val tree = buildTocTree(nativeBookmarks)
                tocCache[book.id] = tree
                return@withContext tree
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tentativa de extração de outline nativo: ${e.message}")
        }

        // 2. Verificar se o livro contém páginas de amostra ou texto em cache
        val allPagesText = if (samplePages.isNotEmpty()) {
            samplePages
        } else {
            val total = book.totalPages.coerceAtLeast(1)
            (1..total).map { p ->
                textCache["${book.id}_$p"] ?: textCache["book_${book.id}_page_$p"] ?: ""
            }
        }

        // 3. Procurar se alguma das primeiras páginas é explicitamente um "Table of Contents" / "Sumário" / "Índice"
        val maxSearchPages = allPagesText.size.coerceAtMost(8)
        val tocPageIndex = allPagesText.take(maxSearchPages).indexOfFirst { pageText ->
            val lower = pageText.lowercase()
            (lower.contains("table of contents") || lower.contains("sumário") || lower.contains("sumario") || lower.contains("índice") || lower.contains("contents")) &&
                    (lower.contains("1.") || lower.contains("2.") || lower.contains("4.") || lower.contains("chapter") || lower.contains("capítulo") || pageText.lines().size >= 4)
        }

        if (tocPageIndex != -1) {
            val tocPageText = allPagesText[tocPageIndex]
            val lines = tocPageText.lines()
            val parsedToc = parseExplicitTocLines(lines, allPagesText, book.totalPages)
            if (parsedToc.isNotEmpty()) {
                val tree = buildTocTree(parsedToc)
                tocCache[book.id] = tree
                return@withContext tree
            }
        }

        // 4. Se não há página dedicada de sumário, extrair cabeçalhos hierárquicos ao longo das páginas
        var foundHierarchicalHeadings = false
        allPagesText.forEachIndexed { pageIndex, pageText ->
            val pageNum = pageIndex + 1
            val lines = pageText.lines().map { it.trim() }.filter { it.isNotBlank() }
            for (line in lines) {
                val parsedItem = detectSectionHeading(line, pageNum, pageText)
                if (parsedItem != null) {
                    items.add(parsedItem)
                    foundHierarchicalHeadings = true
                }
            }
        }

        if (foundHierarchicalHeadings && items.isNotEmpty()) {
            val tree = buildTocTree(items)
            tocCache[book.id] = tree
            return@withContext tree
        }

        // 5. Fallback estruturado página por página
        items.clear()
        val total = book.totalPages.coerceAtLeast(1)
        for (p in 1..total) {
            val text = allPagesText.getOrNull(p - 1) ?: ""
            val foundLinks = if (text.isNotBlank()) extractLinksFromText(text) else emptyList()
            val firstLine = text.lines().firstOrNull { it.isNotBlank() } ?: "Página $p"
            val displayTitle = if (firstLine.startsWith("Página") || firstLine.startsWith("Capítulo") || firstLine.startsWith("Chapter")) {
                firstLine.take(45)
            } else {
                "Pág $p: ${if (firstLine.length > 40) firstLine.take(37) + "..." else firstLine}"
            }
            val displaySnippet = if (text.isNotBlank()) text.take(90).replace("\n", " ").trim() + "..." else "Toque para saltar diretamente para a página $p"

            val level = when {
                firstLine.matches(Regex("""^(\d+\.\d+\.\d+).*""")) -> 2
                firstLine.matches(Regex("""^(\d+\.\d+).*""")) -> 1
                else -> 0
            }

            items.add(
                PdfTocItem(
                    title = displayTitle,
                    pageNumber = p,
                    level = level,
                    sectionNumber = "",
                    snippet = displaySnippet,
                    links = foundLinks
                )
            )
        }
        val tree = buildTocTree(items)
        tocCache[book.id] = tree
        tree
    }

    /**
     * Organiza uma lista plana de tópicos em uma estrutura de árvore hierárquica (Tree Node),
     * similar ao VS Code / Microsoft Edge Outline, onde itens de nível superior contêm seus subtópicos.
     */
    fun buildTocTree(flatItems: List<PdfTocItem>): List<PdfTocItem> {
        if (flatItems.isEmpty()) return emptyList()
        if (flatItems.any { it.children.isNotEmpty() }) return flatItems

        class MutableNode(
            val item: PdfTocItem,
            val children: MutableList<MutableNode> = mutableListOf()
        )

        val roots = mutableListOf<MutableNode>()
        val stack = ArrayDeque<MutableNode>()

        for (item in flatItems) {
            val node = MutableNode(item)
            while (stack.isNotEmpty() && stack.last().item.level >= item.level) {
                stack.removeLast()
            }
            if (stack.isEmpty()) {
                roots.add(node)
            } else {
                stack.last().children.add(node)
            }
            stack.addLast(node)
        }

        fun toImmutable(node: MutableNode): PdfTocItem {
            val childItems = node.children.map { toImmutable(it) }
            val snippet = if (childItems.isNotEmpty()) {
                "${childItems.size} subtemas disponíveis"
            } else {
                node.item.snippet
            }
            return node.item.copy(
                snippet = snippet,
                children = childItems
            )
        }

        return roots.map { toImmutable(it) }
    }

    private fun detectSectionHeading(line: String, pageNum: Int, pageText: String): PdfTocItem? {
        val clean = line.removePrefix("▪").removePrefix("•").removePrefix("-").trim()
        val regexL3 = Regex("""^(\d+\.\d+\.\d+\.\d+)\.?\s*(.*)""")
        val regexL2 = Regex("""^(\d+\.\d+\.\d+)\.?\s*(.*)""")
        val regexL1 = Regex("""^(\d+\.\d+)\.?\s*(.*)""")
        val regexL0 = Regex("""^(\d+)\.\s*(.*)""")
        val regexChapter = Regex("""^(Capítulo|Chapter|Seção|Section|Parte|Módulo)\s+([0-9IVXLCDM]+)[\s:.\-]+(.*)""", RegexOption.IGNORE_CASE)

        return when {
            regexL3.matches(clean) -> {
                val match = regexL3.find(clean)!!
                val secNum = match.groupValues[1]
                val title = match.groupValues[2].ifBlank { secNum }
                PdfTocItem(title = "$secNum $title", pageNumber = pageNum, level = 3, sectionNumber = secNum, snippet = pageText.take(100))
            }
            regexL2.matches(clean) -> {
                val match = regexL2.find(clean)!!
                val secNum = match.groupValues[1]
                val title = match.groupValues[2].ifBlank { secNum }
                PdfTocItem(title = "$secNum $title", pageNumber = pageNum, level = 2, sectionNumber = secNum, snippet = pageText.take(100))
            }
            regexL1.matches(clean) -> {
                val match = regexL1.find(clean)!!
                val secNum = match.groupValues[1]
                val title = match.groupValues[2].ifBlank { secNum }
                PdfTocItem(title = "$secNum $title", pageNumber = pageNum, level = 1, sectionNumber = secNum, snippet = pageText.take(100))
            }
            regexL0.matches(clean) -> {
                val match = regexL0.find(clean)!!
                val secNum = match.groupValues[1] + "."
                val title = match.groupValues[2].ifBlank { secNum }
                // Ignorar números soltos ou datas
                if (title.length >= 3) {
                    PdfTocItem(title = "$secNum $title", pageNumber = pageNum, level = 0, sectionNumber = secNum, snippet = pageText.take(100))
                } else null
            }
            regexChapter.matches(clean) -> {
                PdfTocItem(title = clean.take(50), pageNumber = pageNum, level = 0, sectionNumber = "", snippet = pageText.take(100))
            }
            else -> null
        }
    }

    private fun parseExplicitTocLines(
        lines: List<String>,
        allPagesText: List<String>,
        totalPages: Int
    ): List<PdfTocItem> {
        val results = mutableListOf<PdfTocItem>()
        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) continue
            val lower = trimmed.lowercase()
            if (lower == "table of contents" || lower == "sumário" || lower == "sumario" || lower == "índice" || lower == "contents") {
                continue
            }

            // Detectar nível por marcadores ▪ e contagem de pontos ou recuo
            val isBullet = rawLine.contains("▪") || rawLine.contains("•")
            val clean = trimmed.removePrefix("▪").removePrefix("•").removePrefix("-").trim()

            // Extrair número de página no final da linha se houver (ex: "... 4" ou "12")
            val pageMatch = Regex("""[\s.·…]+(\d+)$""").find(clean)
            val explicitPage = pageMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            val textWithoutPage = if (explicitPage != null) {
                clean.substring(0, pageMatch.range.first).trim()
            } else clean

            // Determinar level hierárquico
            val level = when {
                textWithoutPage.matches(Regex("""^(\d+\.\d+\.\d+\.\d+).*""")) -> 3
                textWithoutPage.matches(Regex("""^(\d+\.\d+\.\d+).*""")) -> 2
                textWithoutPage.matches(Regex("""^(\d+\.\d+).*""")) -> 1
                textWithoutPage.matches(Regex("""^(\d+)\..*""")) -> 0
                isBullet && rawLine.indexOf("▪") >= 4 -> 2
                isBullet -> 1
                else -> 0
            }

            // Extrair seção se houver
            val secMatch = Regex("""^(\d+(\.\d+)*\.?)\s*(.*)""").find(textWithoutPage)
            val secNum = secMatch?.groupValues?.getOrNull(1) ?: ""
            val titleText = textWithoutPage.ifBlank { "Tópico" }

            // Determinar página de destino (se não explícita na linha, buscar no corpo das páginas)
            val targetPage = if (explicitPage != null && explicitPage in 1..totalPages) {
                explicitPage
            } else {
                // Procurar no texto das páginas de conteúdo (pulando a primeira página se for o próprio sumário)
                val keyword = (secNum.ifBlank { titleText }).take(16).trim()
                val searchPages = if (allPagesText.size > 1) allPagesText.drop(1) else allPagesText
                val foundOffset = searchPages.indexOfFirst { it.contains(keyword, ignoreCase = true) }
                if (foundOffset != -1) {
                    (foundOffset + 2).coerceIn(1, totalPages)
                } else {
                    val fallbackIndex = allPagesText.indexOfFirst { it.contains(keyword, ignoreCase = true) }
                    if (fallbackIndex != -1) fallbackIndex + 1 else 1
                }
            }

            results.add(
                PdfTocItem(
                    title = titleText,
                    pageNumber = targetPage.coerceIn(1, totalPages.coerceAtLeast(1)),
                    level = level,
                    sectionNumber = secNum,
                    snippet = "Seção $secNum - Pág $targetPage",
                    links = extractLinksFromText(titleText)
                )
            )
        }
        return results
    }

    private fun extractNativePdfBookmarks(file: File, totalPages: Int): List<PdfTocItem> {
        return try {
            val (bookmarks, _) = PdfOutlineExtractor.extractOutlineAndLinks(file, totalPages)
            bookmarks
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair bookmarks nativos: ${e.message}", e)
            emptyList()
        }
    }

    fun applyColorFilter(src: Bitmap, theme: String): Bitmap {
        val result = try {
            Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            Log.w(TAG, "OOM in applyColorFilter, falling back to original bitmap")
            return src
        }
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (theme) {
            "MONOCHROME_BW" -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val contrast = 1.35f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(contrastMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
            "DARK_OLED" -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val invertMatrix = ColorMatrix(floatArrayOf(
                    -1f,  0f,  0f, 0f, 255f,
                     0f, -1f,  0f, 0f, 255f,
                     0f,  0f, -1f, 0f, 255f,
                     0f,  0f,  0f, 1f,   0f
                ))
                matrix.postConcat(invertMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
            "CINEMA_YELLOW" -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val yellowMatrix = ColorMatrix(floatArrayOf(
                    -1f,  0f,  0f, 0f, 255f,
                     0f, -1f,  0f, 0f, 230f,
                     0f,  0f, -1f, 0f, 40f,
                     0f,  0f,  0f, 1f, 0f
                ))
                matrix.postConcat(yellowMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
            "CYBERPUNK_NEON" -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val cyanMatrix = ColorMatrix(floatArrayOf(
                    -1f,  0f,  0f, 0f, 20f,
                     0f, -1f,  0f, 0f, 240f,
                     0f,  0f, -1f, 0f, 255f,
                     0f,  0f,  0f, 1f, 0f
                ))
                matrix.postConcat(cyanMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
            "TERMINAL_GREEN" -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val greenMatrix = ColorMatrix(floatArrayOf(
                    -1f,  0f,  0f, 0f, 10f,
                     0f, -1f,  0f, 0f, 255f,
                     0f,  0f, -1f, 0f, 70f,
                     0f,  0f,  0f, 1f, 0f
                ))
                matrix.postConcat(greenMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
            "STREAMING_BOX" -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val invertMatrix = ColorMatrix(floatArrayOf(
                    -0.9f,  0f,    0f,    0f, 240f,
                     0f,   -0.9f,  0f,    0f, 240f,
                     0f,    0f,   -0.9f,  0f, 240f,
                     0f,    0f,    0f,    1f, 0f
                ))
                matrix.postConcat(invertMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
            "PAPER_SEPIA" -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0.2f)
                val sepiaMatrix = ColorMatrix(floatArrayOf(
                    0.95f, 0f, 0f, 0f, 15f,
                    0f, 0.90f, 0f, 0f, 10f,
                    0f, 0f, 0.80f, 0f, 0f,
                    0f, 0f, 0.80f, 1f, 0f
                ))
                matrix.postConcat(sepiaMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
            else -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
            }
        }

        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    fun parseSamplePages(sampleContentJson: String?): List<String> {
        if (sampleContentJson.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(sampleContentJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            listOf(sampleContentJson)
        }
    }

    fun applyTypographyRules(text: String): String {
        if (text.isBlank()) return text
        var result = text

        // 1. Single-letter words in Portuguese/Spanish/English (a, o, e, é, à, ó, i)
        // Bind with non-breaking space (\u00A0) so they NEVER sit alone at line ends or wrap isolated
        result = result.replace(Regex("(?<=[\\s\"'«“(]|^)([aoeéàóiAOEÉÀÓI])\\s+(?=[\\p{L}\\d])"), "$1\u00A0")

        // 2. Common short prepositions/articles/conjunctions (de, do, da, em, no, na, um, se, ou, os, as, ao, às)
        result = result.replace(Regex("(?<=[\\s\"'«“(]|^)(de|do|da|em|no|na|um|se|ou|os|as|ao|às|nos|nas|dos|das|por|pra|pro|De|Do|Da|Em|No|Na|Um|Se|Ou|Os|As|Ao|Às)\\s+(?=[\\p{L}\\d])"), "$1\u00A0")

        // 3. Numbers, ordinals and symbols followed by words (e.g. "1º dia", "10 páginas", "R$ 50", "Cap. 2", "pág. 5", "art. 1º")
        result = result.replace(Regex("(?<=[\\s\"'«“(]|^)(\\d{1,4}[ºª°]?|R\\$|\\$|§|[Nn]º|[Cc]ap\\.?|[Pp]ág\\.?|[Aa]rt\\.?)\\s+(?=[\\p{L}\\d])"), "$1\u00A0")

        return result
    }

    fun cleanAndReorganizePdfText(rawText: String): String {
        if (rawText.isBlank()) return ""

        var cleaned = rawText.replace("\r\n", "\n").replace("\r", "\n")

        // 1. Remove hyphenation breaks at line ends (including soft hyphens): "exem-\nplo" -> "exemplo"
        cleaned = cleaned.replace(Regex("([\\p{L}\\d]+)[-\\u00AD\\u2010\\u2011\\u2013]\\s*\\n+\\s*([\\p{L}\\d]+)"), "$1$2")
        cleaned = cleaned.replace("\u00AD", "")

        // 2. Remove standalone page numbers or Roman numerals
        cleaned = cleaned.replace(Regex("(?m)^\\s*(\\d{1,4}|[IVXLCDM]{1,6})\\s*$"), "")

        // 3. Unify mid-sentence newlines into continuous space: lines that don't end in terminal punctuation
        cleaned = cleaned.replace(Regex("([^.!?…:\\n])\\s*\\n\\s*([\\p{L}\\d\"'«“(\\[])"), "$1 $2")

        // 4. Any remaining single newlines -> space
        cleaned = cleaned.replace(Regex("(?<!\\n)\\n(?!\\n)"), " ")

        // 5. Clean excessive blank lines (max 2 newlines between paragraphs)
        cleaned = cleaned.replace(Regex("\\n{3,}"), "\n\n")

        // 6. Fix spacing before punctuation (e.g. "palavra , outra" -> "palavra, outra")
        cleaned = cleaned.replace(Regex("\\s+([,.:;!?%])"), "$1")

        // 7. Ensure space after punctuation if immediately followed by a letter or number
        cleaned = cleaned.replace(Regex("([,.:;!?])([\\p{L}\\d])"), "$1 $2")

        // 8. Normalize repeated whitespace/tabs to single space
        cleaned = cleaned.replace(Regex("[ \\t\\x0B\\f]+"), " ")

        return cleaned.trim()
    }

    fun extractParagraphsAndSentences(rawText: String): Pair<List<BookParagraph>, List<String>> {
        val organized = cleanAndReorganizePdfText(rawText)
        if (organized.isBlank()) return Pair(emptyList(), emptyList())

        val rawParagraphs = organized.split(Regex("\\n{2,}"))
        val allSentences = mutableListOf<String>()
        val paragraphsList = mutableListOf<BookParagraph>()

        for (rawPara in rawParagraphs) {
            val trimmedPara = rawPara.trim()
            if (trimmedPara.isBlank()) continue

            // Split paragraph into distinct sentences strictly closed by a period (.)
            val sentenceSplitter = Regex("(?<=\\.)\\s+(?=[\\p{L}\\d\"'«“(\\[])")
            val rawSentences = trimmedPara.split(sentenceSplitter)
            val paraSentences = mutableListOf<String>()

            for (raw in rawSentences) {
                val s = raw.trim()
                if (s.isBlank()) continue

                if (paraSentences.isNotEmpty()) {
                    val prev = paraSentences.last()
                    val prevLower = prev.lowercase().trim()
                    val isPrevAbbrev = prevLower.endsWith("dr.") || prevLower.endsWith("dra.") ||
                            prevLower.endsWith("sr.") || prevLower.endsWith("sra.") ||
                            prevLower.endsWith("prof.") || prevLower.endsWith("profa.") ||
                            prevLower.endsWith("art.") || prevLower.endsWith("pág.") ||
                            prevLower.endsWith("págs.") || prevLower.endsWith("cap.") ||
                            prevLower.endsWith("vol.") || prevLower.endsWith("ex.") ||
                            prevLower.endsWith("etc.") || prevLower.endsWith("fig.") ||
                            prevLower.endsWith("nº.") || prevLower.endsWith("n°.") ||
                            prevLower.endsWith("i.e.") || prevLower.endsWith("e.g.") ||
                            prevLower.matches(Regex(".*\\b\\d{1,3}\\.$")) ||
                            prevLower.matches(Regex(".*\\b[a-zA-Z]\\.$")) ||
                            prev.length <= 4 ||
                            !prev.endsWith(".")
                    if (isPrevAbbrev) {
                        paraSentences[paraSentences.size - 1] = applyTypographyRules("$prev $s")
                        continue
                    }
                }
                paraSentences.add(applyTypographyRules(s))
            }

            // Ensure every oração is strictly closed by a period (.)
            val finalizedSentences = paraSentences.map { sentence ->
                val trimmed = sentence.trim()
                if (!trimmed.endsWith(".")) "$trimmed." else trimmed
            }

            if (finalizedSentences.isNotEmpty()) {
                val startIdx = allSentences.size
                allSentences.addAll(finalizedSentences)
                paragraphsList.add(
                    BookParagraph(
                        paragraphIndex = paragraphsList.size,
                        sentences = finalizedSentences,
                        globalStartSentenceIndex = startIdx
                    )
                )
            }
        }

        return Pair(paragraphsList, allSentences)
    }

    fun splitTextIntoSentences(text: String): List<String> {
        return extractParagraphsAndSentences(text).second
    }

    fun extractParagraphs(text: String): List<BookParagraph> {
        return extractParagraphsAndSentences(text).first
    }
}

data class BookParagraph(
    val paragraphIndex: Int,
    val sentences: List<String>,
    val globalStartSentenceIndex: Int
)
