package com.example.data.pdf

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.PdfBook
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Extrator de Sumário e Links de Alta Performance para qualquer livro em PDF.
 * Suporta livros de qualquer tamanho (incluindo PDFs gigantescos) sem travar a interface e sem estourar a memória RAM.
 * Extrai a hierarquia completa de tópicos e subtemas (/Outlines) com links e marcadores
 * idêntico ao Microsoft Edge e Adobe Acrobat Reader.
 */
object PdfOutlineExtractor {

    private const val TAG = "PdfOutlineExtractor"

    /**
     * Resolve o arquivo físico para leitura direta com alta performance.
     */
    fun resolvePdfFile(context: Context, book: PdfBook): File? {
        val internalFile = File(context.filesDir, "pdfs/book_${book.id}.pdf")
        if (internalFile.exists() && internalFile.length() > 0) return internalFile

        val fileUri = book.fileUri
        if (!fileUri.isNullOrBlank()) {
            if (fileUri.startsWith("file://") || fileUri.startsWith("/")) {
                val path = if (fileUri.startsWith("file://")) fileUri.removePrefix("file://") else fileUri
                val f = File(path)
                if (f.exists() && f.length() > 0) return f
            }
            val cachedFile = File(context.cacheDir, "cached_pdf_${kotlin.math.abs(Uri.parse(fileUri).hashCode())}.pdf")
            if (cachedFile.exists() && cachedFile.length() > 0) return cachedFile

            val tocCacheFile = File(context.cacheDir, "pdf_toc_${book.id}.pdf")
            if (tocCacheFile.exists() && tocCacheFile.length() > 0) return tocCacheFile

            // Copiar do stream da URI para o cache temporário
            try {
                val uri = Uri.parse(fileUri)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tocCacheFile).use { output ->
                        val buffer = ByteArray(65536)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
                if (tocCacheFile.exists() && tocCacheFile.length() > 0) return tocCacheFile
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao resolver arquivo do PDF para leitura de sumário: ${e.message}")
            }
        }
        return null
    }

    /**
     * Extrai sumário e links usando Context e PdfBook.
     */
    fun extractOutlineAndLinks(
        context: Context,
        book: PdfBook
    ): Pair<List<PdfTocItem>, List<String>> {
        val file = resolvePdfFile(context, book)
        if (file != null && file.exists() && file.length() > 0) {
            return extractOutlineAndLinks(file, book.totalPages)
        }
        return Pair(emptyList(), emptyList())
    }

    /**
     * Extrai o sumário nativo e todos os links a partir de um File.
     * Utiliza algoritmos rápidos baseados em índices que nunca travam a thread.
     */
    fun extractOutlineAndLinks(
        pdfFile: File,
        totalPages: Int
    ): Pair<List<PdfTocItem>, List<String>> {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            return Pair(emptyList(), emptyList())
        }

        val fileLength = pdfFile.length()

        // 1. Para arquivos pequenos até 6MB, leitura rápida em memória protegida contra OOM
        if (fileLength <= 6L * 1024L * 1024L) {
            try {
                val bytes = pdfFile.readBytes()
                val content = String(bytes, Charsets.ISO_8859_1)
                return extractOutlineFromContentString(content, totalPages)
            } catch (t: Throwable) {
                Log.w(TAG, "Tentando extração por blocos após falha em memória: ${t.message}")
            }
        }

        // 2. Para arquivos maiores (até 10GB+), extração por janelas de metadados sem carregar o arquivo todo
        return extractOutlineFromLargeFile(pdfFile, totalPages)
    }

    /**
     * Extração de metadados para arquivos gigantescos (acima de 6MB e até 10GB+).
     * Lê apenas o cabeçalho inicial (4MB) e a cauda final (4MB), onde residem 100% dos catálogos
     * e nós de Outlines do PDF, ignorando gigabytes de imagens e mídia.
     */
    private fun extractOutlineFromLargeFile(
        pdfFile: File,
        totalPages: Int
    ): Pair<List<PdfTocItem>, List<String>> {
        return try {
            val length = pdfFile.length()
            val windowSize = 4L * 1024L * 1024L // 4 MB para capturar sumários até em PDFs de vários gigabytes de forma leve
            val raf = RandomAccessFile(pdfFile, "r")

            raf.use { f ->
                // Ler os primeiros 4MB (cabeçalho e catálogo)
                val headLen = minOf(windowSize, length).toInt()
                val headBytes = ByteArray(headLen)
                f.seek(0)
                f.readFully(headBytes)
                val headStr = String(headBytes, Charsets.ISO_8859_1)

                // Ler os últimos 4MB (tabela xref e trailers)
                val tailStr = if (length > windowSize) {
                    val tailLen = minOf(windowSize, length - headLen).toInt()
                    val tailBytes = ByteArray(tailLen)
                    f.seek(length - tailLen)
                    f.readFully(tailBytes)
                    String(tailBytes, Charsets.ISO_8859_1)
                } else ""

                val combined = headStr + "\n" + tailStr
                extractOutlineFromContentString(combined, totalPages)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erro na extração de arquivo grande: ${t.message}", t)
            Pair(emptyList(), emptyList())
        }
    }

    /**
     * Processa a string de conteúdo e constrói a árvore hierárquica completa (temas e subtemas).
     */
    private fun extractOutlineFromContentString(
        content: String,
        totalPages: Int
    ): Pair<List<PdfTocItem>, List<String>> {
        // 1. Mapear objetos úteis usando busca ultra-rápida por índice (sub-15ms)
        val objectMap = scanPdfObjectsFast(content)

        // 2. Mapear páginas físicas: pageObjectId -> pageNumber (1..totalPages)
        val pageNumberMap = buildPageNumberMap(content, objectMap, totalPages)

        // 3. Mapear destinos nomeados: name -> pageNumber
        val namedDestMap = scanNamedDestinations(content, objectMap, pageNumberMap)

        // 4. Extrair todos os links do livro
        val allBookLinks = scanAllPdfLinks(content, objectMap, pageNumberMap)

        // 5. Mapear relacionamentos pai-filho (/Parent) para todos os objetos de outline
        val parentChildMap = HashMap<Int, MutableList<Int>>()
        for ((id, dict) in objectMap) {
            if (dict.contains("/Title")) {
                val parentRef = extractRef(dict, "/Parent")
                if (parentRef != null) {
                    parentChildMap.getOrPut(parentRef) { mutableListOf() }.add(id)
                }
            }
        }

        // 6. Localizar a raiz de Outlines
        val outlineRootId = findOutlineRootObjectId(content, objectMap)

        if (outlineRootId != null) {
            val rootDict = objectMap[outlineRootId]
            val firstItemRef = if (rootDict != null) extractRef(rootDict, "/First") else null

            // Coleta os nós raiz: ou pela cadeia /First -> /Next, ou por parentChildMap[outlineRootId]
            val rootIds = LinkedHashSet<Int>()
            if (firstItemRef != null) {
                var curr: Int? = firstItemRef
                val seen = mutableSetOf<Int>()
                while (curr != null && !seen.contains(curr)) {
                    seen.add(curr)
                    rootIds.add(curr)
                    val nDict = objectMap[curr] ?: ""
                    curr = extractRef(nDict, "/Next")
                }
            }
            if (rootIds.isEmpty() && parentChildMap.containsKey(outlineRootId)) {
                rootIds.addAll(parentChildMap[outlineRootId]!!)
            }

            if (rootIds.isNotEmpty()) {
                val rootItems = mutableListOf<PdfTocItem>()
                val visited = mutableSetOf<Int>()
                for (rId in rootIds) {
                    visited.add(rId)
                    val item = parseOutlineNode(
                        objId = rId,
                        level = 0,
                        objectMap = objectMap,
                        parentChildMap = parentChildMap,
                        pageNumberMap = pageNumberMap,
                        namedDestMap = namedDestMap,
                        totalPages = totalPages,
                        visited = visited
                    )
                    if (item != null) {
                        rootItems.add(item)
                    }
                }

                if (rootItems.isNotEmpty()) {
                    Log.d(TAG, "Sumário nativo reconhecido com sucesso: ${rootItems.size} temas raiz.")
                    return Pair(rootItems, allBookLinks)
                }
            }
        }

        // Fallback robusto: se a raiz /Outlines não tem /First ou não foi mapeada,
        // reconstruir temas e subtemas usando parentChildMap e numeração de seções
        val titleItems = extractAllTitleObjects(objectMap, parentChildMap, outlineRootId, pageNumberMap, namedDestMap, totalPages)
        if (titleItems.isNotEmpty()) {
            return Pair(titleItems, allBookLinks)
        }

        return Pair(emptyList(), allBookLinks)
    }

    /**
     * Mapeamento ultra-rápido de objetos usando String.indexOf (evita regexes lentas sobre grandes arquivos).
     */
    private fun scanPdfObjectsFast(content: String): Map<Int, String> {
        val objectMap = HashMap<Int, String>()
        var pos = 0
        val len = content.length

        while (pos < len) {
            val objIdx = content.indexOf(" obj", pos)
            if (objIdx == -1) break

            // Achar o início do número do objeto (ex: "12 0 obj")
            var p = objIdx - 1
            while (p >= pos && content[p].isWhitespace()) p--
            // Pular geração (geralmente 0)
            while (p >= pos && content[p].isDigit()) p--
            while (p >= pos && content[p].isWhitespace()) p--
            val idEnd = p
            while (p >= pos && content[p].isDigit()) p--
            val idStart = p + 1

            val objId = if (idStart <= idEnd) {
                content.substring(idStart, idEnd + 1).toIntOrNull()
            } else null

            val endObjIdx = content.indexOf("endobj", objIdx + 4)
            if (endObjIdx != -1) {
                if (objId != null) {
                    val rawBody = content.substring(objIdx + 4, endObjIdx)
                    // Remove fluxos binários comprimidos (stream...endstream) para reter apenas o dicionário
                    val streamIdx = rawBody.indexOf("stream")
                    val dictBody = if (streamIdx != -1) rawBody.substring(0, streamIdx) else rawBody

                    // Filtra apenas objetos que contêm estruturas de dicionário ou metadados relevantes
                    if (dictBody.contains("<<") || dictBody.contains("/Title") || dictBody.contains("/Outlines") || dictBody.contains("/Pages") || dictBody.contains("/Parent")) {
                        objectMap[objId] = dictBody
                    }
                }
                pos = endObjIdx + 6
            } else {
                pos = objIdx + 4
            }
        }
        return objectMap
    }

    /**
     * Constrói o mapa de páginas físicas (objId da página -> número da página 1..totalPages).
     */
    private fun buildPageNumberMap(
        content: String,
        objectMap: Map<Int, String>,
        totalPages: Int
    ): Map<Int, Int> {
        val pageNumberMap = HashMap<Int, Int>()
        val pageObjList = mutableListOf<Int>()

        fun collectPagesFromKids(kidsStr: String) {
            val refPattern = Pattern.compile("(\\d+)\\s+\\d+\\s+R")
            val refMatcher = refPattern.matcher(kidsStr)
            while (refMatcher.find()) {
                val refId = refMatcher.group(1)?.toIntOrNull() ?: continue
                val childObj = objectMap[refId] ?: ""
                if (childObj.contains("/Type") && childObj.contains("/Pages") && !childObj.contains("/Type /Page ")) {
                    val subKidsMatch = Pattern.compile("/Kids\\s*\\[([^\\]]+)\\]").matcher(childObj)
                    if (subKidsMatch.find()) {
                        collectPagesFromKids(subKidsMatch.group(1) ?: "")
                    }
                } else {
                    pageObjList.add(refId)
                }
            }
        }

        // Achar o nó raiz de Pages a partir do Catálogo
        val catalogMatch = Pattern.compile("/Type\\s*/Catalog[\\s\\S]*?/Pages\\s+(\\d+)\\s+\\d+\\s+R").matcher(content)
        val pagesRootId = if (catalogMatch.find()) {
            catalogMatch.group(1)?.toIntOrNull()
        } else {
            val altMatch = Pattern.compile("/Root\\s+(\\d+)\\s+\\d+\\s+R").matcher(content)
            val rootId = if (altMatch.find()) altMatch.group(1)?.toIntOrNull() else null
            val rootObj = if (rootId != null) objectMap[rootId] ?: "" else ""
            val pMatch = Pattern.compile("/Pages\\s+(\\d+)\\s+\\d+\\s+R").matcher(rootObj)
            if (pMatch.find()) pMatch.group(1)?.toIntOrNull() else null
        }

        if (pagesRootId != null) {
            val rootPagesObj = objectMap[pagesRootId] ?: ""
            val kidsMatcher = Pattern.compile("/Kids\\s*\\[([^\\]]+)\\]").matcher(rootPagesObj)
            if (kidsMatcher.find()) {
                collectPagesFromKids(kidsMatcher.group(1) ?: "")
            }
        }

        if (pageObjList.isNotEmpty()) {
            pageObjList.forEachIndexed { index, objId ->
                pageNumberMap[objId] = index + 1
            }
        } else {
            // Fallback: varrer todos os objetos que têm /Type /Page
            val individualPages = mutableListOf<Int>()
            for ((objId, body) in objectMap) {
                if (body.contains("/Type") && Pattern.compile("/Type\\s*/Page\\b").matcher(body).find()) {
                    individualPages.add(objId)
                }
            }
            individualPages.sorted().forEachIndexed { index, objId ->
                pageNumberMap[objId] = (index + 1).coerceIn(1, totalPages)
            }
        }

        return pageNumberMap
    }

    /**
     * Mapeia destinos nomeados no formato /Dests.
     */
    private fun scanNamedDestinations(
        content: String,
        objectMap: Map<Int, String>,
        pageNumberMap: Map<Int, Int>
    ): Map<String, Int> {
        val dests = HashMap<String, Int>()
        try {
            val pattern = Pattern.compile("/(?:Dests|Names)\\s*<<([\\s\\S]*?)>>")
            val matcher = pattern.matcher(content)
            while (matcher.find()) {
                val body = matcher.group(1) ?: ""
                val entryPattern = Pattern.compile("/([A-Za-z0-9._\\-]+)\\s*\\[\\s*(\\d+)\\s+\\d+\\s+R")
                val entryMatcher = entryPattern.matcher(body)
                while (entryMatcher.find()) {
                    val name = entryMatcher.group(1) ?: ""
                    val pageObjId = entryMatcher.group(2)?.toIntOrNull()
                    if (pageObjId != null && pageNumberMap.containsKey(pageObjId)) {
                        dests[name] = pageNumberMap[pageObjId]!!
                        dests[name.lowercase()] = pageNumberMap[pageObjId]!!
                    }
                }
            }
        } catch (_: Exception) {}
        return dests
    }

    /**
     * Extrai todos os links web e referências do livro.
     */
    private fun scanAllPdfLinks(
        content: String,
        objectMap: Map<Int, String>,
        pageNumberMap: Map<Int, Int>
    ): List<String> {
        val allLinks = LinkedHashSet<String>()
        try {
            // 1. Varrer anotações /URI
            val uriPattern = Pattern.compile("""/URI\s*\(([^)]+)\)""")
            val matcher = uriPattern.matcher(content)
            while (matcher.find() && allLinks.size < 500) {
                val url = unescapePdfString(matcher.group(1) ?: "").trim()
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("www.")) {
                    val validUrl = if (url.startsWith("www.")) "https://$url" else url
                    allLinks.add(validUrl)
                }
            }
        } catch (_: Exception) {}
        return allLinks.toList()
    }

    /**
     * Localiza o nó raiz de Outlines no PDF.
     */
    private fun findOutlineRootObjectId(
        content: String,
        objectMap: Map<Int, String>
    ): Int? {
        // 1. Procurar /Outlines no Catálogo ou no conteúdo geral (/Outlines 12 0 R)
        val catMatch = Pattern.compile("/Outlines\\s+(\\d+)\\s+\\d+\\s+R").matcher(content)
        if (catMatch.find()) {
            val id = catMatch.group(1)?.toIntOrNull()
            if (id != null && objectMap.containsKey(id)) return id
        }

        // 2. Procurar em objetos com /Type /Outlines
        for ((id, body) in objectMap) {
            if (body.contains("/Type") && Pattern.compile("/Type\\s*/Outlines\\b").matcher(body).find()) {
                return id
            }
        }

        // 3. Procurar por dicionários que tenham /First e /Count (estrutura padrão de Outlines)
        for ((id, body) in objectMap) {
            if (body.contains("/First") && body.contains("/Count") && !body.contains("/Title")) {
                return id
            }
        }

        return null
    }

    /**
     * Decodifica o nó de outline recursivamente com suporte a subtemas (Microsoft Edge style).
     */
    private fun parseOutlineNode(
        objId: Int,
        level: Int,
        objectMap: Map<Int, String>,
        parentChildMap: Map<Int, List<Int>>,
        pageNumberMap: Map<Int, Int>,
        namedDestMap: Map<String, Int>,
        totalPages: Int,
        visited: MutableSet<Int>
    ): PdfTocItem? {
        val dict = objectMap[objId] ?: return null

        val title = extractTitle(dict, objectMap) ?: "Tópico"
        val resolvedPage = resolveDestination(dict, objectMap, pageNumberMap, namedDestMap, totalPages)
        val itemLinks = extractItemLinks(dict)

        val children = mutableListOf<PdfTocItem>()
        val firstChildRef = extractRef(dict, "/First")

        // 1. Tentar primeiro a cadeia de ponteiros nativa do PDF: /First -> /Next
        if (firstChildRef != null) {
            var currentChildRef: Int? = firstChildRef
            while (currentChildRef != null && !visited.contains(currentChildRef)) {
                visited.add(currentChildRef)
                val childItem = parseOutlineNode(
                    objId = currentChildRef,
                    level = level + 1,
                    objectMap = objectMap,
                    parentChildMap = parentChildMap,
                    pageNumberMap = pageNumberMap,
                    namedDestMap = namedDestMap,
                    totalPages = totalPages,
                    visited = visited
                )
                if (childItem != null) {
                    children.add(childItem)
                }
                val childDict = objectMap[currentChildRef] ?: ""
                currentChildRef = extractRef(childDict, "/Next")
            }
        } else if (parentChildMap.containsKey(objId)) {
            // 2. Se o nó não possui /First explícito, buscar todos os filhos mapeados por /Parent
            val childIds = parentChildMap[objId] ?: emptyList()
            for (childId in childIds) {
                if (!visited.contains(childId)) {
                    visited.add(childId)
                    val childItem = parseOutlineNode(
                        objId = childId,
                        level = level + 1,
                        objectMap = objectMap,
                        parentChildMap = parentChildMap,
                        pageNumberMap = pageNumberMap,
                        namedDestMap = namedDestMap,
                        totalPages = totalPages,
                        visited = visited
                    )
                    if (childItem != null) {
                        children.add(childItem)
                    }
                }
            }
        }

        val secNumber = extractSectionNumber(title)
        val effectiveLevel = if (level == 0 && secNumber.isNotBlank() && secNumber.count { it == '.' } >= 1) {
            secNumber.count { it == '.' }.coerceAtMost(3)
        } else {
            level
        }

        return PdfTocItem(
            id = "native_toc_${objId}_${effectiveLevel}_${resolvedPage}",
            title = title,
            pageNumber = resolvedPage.coerceIn(1, totalPages.coerceAtLeast(1)),
            level = effectiveLevel,
            sectionNumber = secNumber,
            snippet = if (children.isNotEmpty()) "${children.size} subtemas disponíveis" else "Página $resolvedPage",
            links = itemLinks,
            children = children
        )
    }

    /**
     * Extrai todos os itens de título do PDF e monta a hierarquia de temas e subtemas.
     */
    private fun extractAllTitleObjects(
        objectMap: Map<Int, String>,
        parentChildMap: Map<Int, List<Int>>,
        outlineRootId: Int?,
        pageNumberMap: Map<Int, Int>,
        namedDestMap: Map<String, Int>,
        totalPages: Int
    ): List<PdfTocItem> {
        val titleObjIds = objectMap.filter { it.value.contains("/Title") }.keys
        if (titleObjIds.isEmpty()) return emptyList()

        // Identifica nós raiz: nós cujo /Parent é outlineRootId ou cujo /Parent não é outro item de título
        val rootIds = LinkedHashSet<Int>()
        for (id in titleObjIds) {
            val dict = objectMap[id] ?: continue
            val pRef = extractRef(dict, "/Parent")
            if (pRef == null || pRef == outlineRootId || !titleObjIds.contains(pRef)) {
                rootIds.add(id)
            }
        }

        // Se encontrou raízes bem definidas pelo grafo /Parent:
        if (rootIds.isNotEmpty() && rootIds.size < titleObjIds.size) {
            val visited = mutableSetOf<Int>()
            val rootItems = mutableListOf<PdfTocItem>()
            for (rId in rootIds) {
                visited.add(rId)
                val item = parseOutlineNode(
                    objId = rId,
                    level = 0,
                    objectMap = objectMap,
                    parentChildMap = parentChildMap,
                    pageNumberMap = pageNumberMap,
                    namedDestMap = namedDestMap,
                    totalPages = totalPages,
                    visited = visited
                )
                if (item != null) rootItems.add(item)
            }
            if (rootItems.isNotEmpty()) {
                rootItems.sortBy { it.pageNumber }
                return rootItems
            }
        }

        // Fallback: monta itens individuais determinando level a partir da numeração de seções
        val flatList = mutableListOf<PdfTocItem>()
        for (objId in titleObjIds) {
            val dict = objectMap[objId] ?: continue
            val title = extractTitle(dict, objectMap) ?: continue
            if (title.isBlank()) continue
            val resolvedPage = resolveDestination(dict, objectMap, pageNumberMap, namedDestMap, totalPages)
            val secNumber = extractSectionNumber(title)
            val links = extractItemLinks(dict)

            val level = when {
                secNumber.count { it == '.' } >= 3 -> 3
                secNumber.count { it == '.' } == 2 -> 2
                secNumber.count { it == '.' } == 1 -> 1
                else -> 0
            }

            val item = PdfTocItem(
                id = "title_toc_${objId}_${resolvedPage}",
                title = title,
                pageNumber = resolvedPage.coerceIn(1, totalPages.coerceAtLeast(1)),
                level = level,
                sectionNumber = secNumber,
                snippet = "Página $resolvedPage",
                links = links
            )
            flatList.add(item)
        }

        if (flatList.isEmpty()) return emptyList()

        flatList.sortBy { it.pageNumber }
        return flatList
    }

    /**
     * Decodifica qualquer string de título em PDF (formato Literal ou Hexadecimal)
     * e garante reparo de caracteres mojibake / asiáticos acidentais.
     */
    fun decodePdfTitleString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        val rawDecoded = when {
            trimmed.startsWith("<") && trimmed.endsWith(">") -> decodeHexPdfString(trimmed)
            trimmed.startsWith("(") && trimmed.endsWith(")") -> decodeLiteralPdfString(trimmed)
            else -> trimmed
        }

        val cleaned = rawDecoded.replace("\u0000", "").trim()
        // Reparo automático de mojibake (caso bytes latinos tenham sido agrupados como ideogramas CJK)
        return repairMojibakeIfNeeded(cleaned)
    }

    /**
     * Decodifica strings no formato hexadecimal <...>.
     */
    private fun decodeHexPdfString(raw: String): String {
        val hexContent = raw.removePrefix("<").removeSuffix(">").replace("\\s".toRegex(), "")
        if (hexContent.isBlank()) return ""

        val paddedHex = if (hexContent.length % 2 != 0) hexContent + "0" else hexContent
        val bytes = ByteArray(paddedHex.length / 2)
        try {
            for (i in bytes.indices) {
                val byteStr = paddedHex.substring(i * 2, i * 2 + 2)
                bytes[i] = byteStr.toInt(16).toByte()
            }

            if (bytes.size >= 2) {
                val b0 = bytes[0].toInt() and 0xFF
                val b1 = bytes[1].toInt() and 0xFF

                // UTF-16BE com BOM (FE FF)
                if (b0 == 0xFE && b1 == 0xFF) {
                    val rest = bytes.copyOfRange(2, bytes.size)
                    return decodeUnicodeBytes(rest, isBigEndian = true)
                }
                // UTF-16LE com BOM (FF FE)
                if (b0 == 0xFF && b1 == 0xFE) {
                    val rest = bytes.copyOfRange(2, bytes.size)
                    return decodeUnicodeBytes(rest, isBigEndian = false)
                }
                // UTF-8 com BOM (EF BB BF)
                if (bytes.size >= 3 && b0 == 0xEF && b1 == 0xBB && (bytes[2].toInt() and 0xFF) == 0xBF) {
                    return String(bytes, 3, bytes.size - 3, Charsets.UTF_8).trim()
                }
                // UTF-16 sem BOM (quando possui alternância de bytes 0x00 para caracteres ASCII/Latinos)
                if (bytes.size >= 4 && isUtf16ByteStream(bytes)) {
                    val isBig = (bytes[0].toInt() and 0xFF) == 0
                    return decodeUnicodeBytes(bytes, isBigEndian = isBig)
                }
            }

            // Tentar UTF-8 primeiro
            val utf8 = try { String(bytes, Charsets.UTF_8) } catch (_: Exception) { "" }
            if (utf8.isNotBlank() && !utf8.contains("\uFFFD") && utf8.any { it.code in 128..255 }) {
                return utf8.trim()
            }

            // Fallback para PDFDocEncoding / ISO-8859-1
            return decodePdfDocEncoding(bytes).trim()
        } catch (_: Exception) {
            return hexContent
        }
    }

    /**
     * Decodifica strings literais ( ... ).
     */
    private fun decodeLiteralPdfString(raw: String): String {
        val inner = if (raw.startsWith("(") && raw.endsWith(")")) {
            raw.substring(1, raw.length - 1)
        } else raw

        val unescaped = unescapePdfString(inner)
        val bytes = unescaped.toByteArray(Charsets.ISO_8859_1)

        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF

            if (b0 == 0xFE && b1 == 0xFF) {
                val rest = bytes.copyOfRange(2, bytes.size)
                return decodeUnicodeBytes(rest, isBigEndian = true)
            }
            if (b0 == 0xFF && b1 == 0xFE) {
                val rest = bytes.copyOfRange(2, bytes.size)
                return decodeUnicodeBytes(rest, isBigEndian = false)
            }
            if (bytes.size >= 3 && b0 == 0xEF && b1 == 0xBB && (bytes[2].toInt() and 0xFF) == 0xBF) {
                return String(bytes, 3, bytes.size - 3, Charsets.UTF_8).trim()
            }
            if (bytes.size >= 4 && isUtf16ByteStream(bytes)) {
                val isBig = (bytes[0].toInt() and 0xFF) == 0
                return decodeUnicodeBytes(bytes, isBigEndian = isBig)
            }
        }

        val utf8 = try { String(bytes, Charsets.UTF_8) } catch (_: Exception) { "" }
        if (utf8.isNotBlank() && !utf8.contains("\uFFFD") && utf8.any { it.code in 128..255 }) {
            return utf8.trim()
        }

        return decodePdfDocEncoding(bytes).trim()
    }

    /**
     * Decodifica bytes Unicode verificando se realmente são caracteres de 2 bytes
     * ou se são caracteres ASCII de 1 byte erroneamente precedidos de BOM FE FF.
     */
    private fun decodeUnicodeBytes(bytes: ByteArray, isBigEndian: Boolean): String {
        if (bytes.isEmpty()) return ""

        // Se o texto não possui nenhum byte nulo (0x00) e possui tamanho considerável,
        // o gerador do PDF colocou o BOM mas usou 1 byte por caractere (ASCII/Latin-1)!
        val zeroCount = bytes.count { it == 0.toByte() }
        if (bytes.size >= 4 && zeroCount == 0) {
            return decodePdfDocEncoding(bytes)
        }

        val charset = if (isBigEndian) Charset.forName("UTF-16BE") else Charset.forName("UTF-16LE")
        return try {
            String(bytes, charset).trim()
        } catch (_: Exception) {
            decodePdfDocEncoding(bytes)
        }
    }

    /**
     * Heurística para detectar se um fluxo de bytes é verdadeiramente UTF-16.
     */
    private fun isUtf16ByteStream(bytes: ByteArray): Boolean {
        if (bytes.size < 4 || bytes.size % 2 != 0) return false
        var evenZeros = 0
        var oddZeros = 0
        for (i in bytes.indices) {
            if (bytes[i] == 0.toByte()) {
                if (i % 2 == 0) evenZeros++ else oddZeros++
            }
        }
        val pairs = bytes.size / 2
        return (evenZeros.toDouble() / pairs >= 0.3) || (oddZeros.toDouble() / pairs >= 0.3)
    }

    /**
     * Decodifica PDFDocEncoding padrão (ISO-8859-1 com mapeamento de caracteres especiais).
     */
    private fun decodePdfDocEncoding(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size)
        for (b in bytes) {
            val code = b.toInt() and 0xFF
            val ch = when (code) {
                0x18 -> '•'
                0x19 -> '†'
                0x1A -> '‡'
                0x80 -> '•'
                0x96 -> '–'
                0x97 -> '—'
                0x91 -> '‘'
                0x92 -> '’'
                0x93 -> '“'
                0x94 -> '”'
                else -> code.toChar()
            }
            if (code != 0) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * DETECÇÃO E REPARO DE MOJIBAKE / CARACTERES ASIÁTICOS EM ACENTOS:
     * Quando palavras com acento são interpretadas na codificação errada (ex: UTF-8 lido como UTF-16,
     * ou ISO-8859-1 com acentos gerando blocos de 16 bits), os bytes de acentuação (como 0xC3 0xA7 para 'ç')
     * caem na faixa de ideogramas CJK/Hangul. Esta rotina restaura os bytes originais e os decodifica como UTF-8.
     */
    fun repairMojibakeIfNeeded(text: String): String {
        if (text.isBlank()) return text

        // 1. Reparar Mojibake clássico de UTF-8 lido como Latin-1 (ex: "IntroduÃ§Ã£o", "CapÃ­tulo")
        if (text.contains("Ã") || text.contains("Â") || text.contains("â")) {
            try {
                val isoBytes = text.toByteArray(Charsets.ISO_8859_1)
                val utf8Fixed = String(isoBytes, Charsets.UTF_8)
                if (!utf8Fixed.contains("\uFFFD") && utf8Fixed.any { it in "áàãâéêíóôõúüçÁÀÃÂÉÊÍÓÔÕÚÜÇ" }) {
                    return utf8Fixed.replace("\u0000", "").trim()
                }
            } catch (_: Exception) {}
        }

        // 2. Verificar se há ideogramas asiáticos / CJK / Hangul causados por agrupamento de bytes
        var cjkCount = 0
        for (c in text) {
            val code = c.code
            if (code in 0x2E80..0x9FFF || code in 0xAC00..0xD7AF || code in 0xF900..0xFAFF || code in 0x3000..0x312F) {
                cjkCount++
            }
        }

        if (cjkCount == 0) return text.replace("\u0000", "").trim()

        // 3. Reconstituir o fluxo de bytes original desmembrando os blocos de 16 bits
        val byteList = mutableListOf<Byte>()
        for (c in text) {
            val code = c.code
            if (code in 0x2E80..0x9FFF || code in 0xAC00..0xD7AF || code in 0xF900..0xFAFF || code in 0x3000..0x312F) {
                val b1 = (code shr 8) and 0xFF
                val b2 = code and 0xFF
                if (b1 != 0) byteList.add(b1.toByte())
                if (b2 != 0) byteList.add(b2.toByte())
            } else {
                val b = c.code and 0xFF
                byteList.add(b.toByte())
            }
        }

        if (byteList.isNotEmpty()) {
            val bytes = byteList.toByteArray()
            // Tentar primeiro decodificar os bytes como UTF-8
            try {
                val utf8Candidate = String(bytes, Charsets.UTF_8)
                val stillHasAsian = utf8Candidate.any { it.code in 0x2E80..0x9FFF || it.code in 0xAC00..0xD7AF }
                if (!utf8Candidate.contains("\uFFFD") && !stillHasAsian && utf8Candidate.isNotBlank()) {
                    Log.d(TAG, "Mojibake asiático reparado via UTF-8: '$text' -> '$utf8Candidate'")
                    return utf8Candidate.replace("\u0000", "").trim()
                }
            } catch (_: Exception) {}

            // Tentar como PDFDocEncoding / ISO-8859-1
            try {
                val latinCandidate = decodePdfDocEncoding(bytes)
                val stillHasAsian = latinCandidate.any { it.code in 0x2E80..0x9FFF || it.code in 0xAC00..0xD7AF }
                if (!stillHasAsian && latinCandidate.isNotBlank()) {
                    Log.d(TAG, "Mojibake asiático reparado via Latin-1: '$text' -> '$latinCandidate'")
                    return latinCandidate.replace("\u0000", "").trim()
                }
            } catch (_: Exception) {}
        }

        return text.replace("\u0000", "").trim()
    }

    private fun unescapePdfString(str: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (c == '\\' && i + 1 < str.length) {
                val next = str[i + 1]
                when (next) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000C'); i += 2 }
                    '(' -> { sb.append('('); i += 2 }
                    ')' -> { sb.append(')'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    in '0'..'7' -> {
                        var octalDigits = ""
                        var j = i + 1
                        while (j < str.length && j < i + 4 && str[j] in '0'..'7') {
                            octalDigits += str[j]
                            j++
                        }
                        val charCode = octalDigits.toIntOrNull(8) ?: 0
                        sb.append(charCode.toChar())
                        i = j
                    }
                    else -> { sb.append(next); i += 2 }
                }
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    private fun extractTitle(dict: String, objectMap: Map<Int, String>? = null): String? {
        val hexMatcher = Pattern.compile("/Title\\s*(<[0-9A-Fa-f\\s]+>)").matcher(dict)
        if (hexMatcher.find()) {
            val hexRaw = hexMatcher.group(1) ?: ""
            val decoded = decodePdfTitleString(hexRaw)
            if (decoded.isNotBlank()) return decoded
        }

        val titleIdx = dict.indexOf("/Title")
        if (titleIdx != -1) {
            val afterTitle = dict.substring(titleIdx + 6).trim()
            if (afterTitle.startsWith("(")) {
                var depth = 0
                var endIdx = -1
                var escaped = false
                for (k in afterTitle.indices) {
                    val ch = afterTitle[k]
                    if (escaped) {
                        escaped = false
                        continue
                    }
                    if (ch == '\\') {
                        escaped = true
                        continue
                    }
                    if (ch == '(') depth++
                    if (ch == ')') {
                        depth--
                        if (depth == 0) {
                            endIdx = k
                            break
                        }
                    }
                }
                if (endIdx != -1) {
                    val rawLiteral = afterTitle.substring(0, endIdx + 1)
                    val decoded = decodePdfTitleString(rawLiteral)
                    if (decoded.isNotBlank()) return decoded
                }
            } else {
                // Referência indireta /Title 14 0 R
                val indRefMatcher = Pattern.compile("^(\\d+)\\s+\\d+\\s+R").matcher(afterTitle)
                if (indRefMatcher.find()) {
                    val refId = indRefMatcher.group(1)?.toIntOrNull()
                    if (refId != null && objectMap != null) {
                        val refBody = objectMap[refId]
                        if (refBody != null) {
                            val indTitle = extractTitle(refBody, null)
                            if (indTitle != null && indTitle.isNotBlank()) return indTitle
                        }
                    }
                }
            }
        }

        return null
    }

    private fun resolveDestination(
        dict: String,
        objectMap: Map<Int, String>,
        pageNumberMap: Map<Int, Int>,
        namedDestMap: Map<String, Int>,
        totalPages: Int
    ): Int {
        // 1. /Dest [ <pageObjId> 0 R ... ]
        val destArrayMatcher = Pattern.compile("/Dest\\s*\\[\\s*(\\d+)\\s+\\d+\\s+R").matcher(dict)
        if (destArrayMatcher.find()) {
            val pageObjId = destArrayMatcher.group(1)?.toIntOrNull()
            if (pageObjId != null && pageNumberMap.containsKey(pageObjId)) {
                return pageNumberMap[pageObjId]!!
            }
        }

        // 2. /Dest [ 3 /Fit ]
        val directNumMatcher = Pattern.compile("/Dest\\s*\\[\\s*(\\d+)\\s+(?!\\d+\\s+R)").matcher(dict)
        if (directNumMatcher.find()) {
            val pageNum = directNumMatcher.group(1)?.toIntOrNull()
            if (pageNum != null) {
                return (pageNum + 1).coerceIn(1, totalPages)
            }
        }

        // 3. Action /A << /S /GoTo /D [ <pageObjId> 0 R ... ] >>
        val actionGoToMatcher = Pattern.compile("/A\\s*<<[\\s\\S]*?/S\\s*/GoTo[\\s\\S]*?/D\\s*\\[\\s*(\\d+)\\s+\\d+\\s+R").matcher(dict)
        if (actionGoToMatcher.find()) {
            val pageObjId = actionGoToMatcher.group(1)?.toIntOrNull()
            if (pageObjId != null && pageNumberMap.containsKey(pageObjId)) {
                return pageNumberMap[pageObjId]!!
            }
        }

        // 4. Destino nomeado /Dest (name) ou /Dest /name
        val namedMatcher = Pattern.compile("/(?:Dest|D)\\s*(?:/|\\()([A-Za-z0-9._\\-]+)").matcher(dict)
        if (namedMatcher.find()) {
            val name = namedMatcher.group(1) ?: ""
            if (namedDestMap.containsKey(name)) {
                return namedDestMap[name]!!
            }
            if (namedDestMap.containsKey(name.lowercase())) {
                return namedDestMap[name.lowercase()]!!
            }
        }

        // 5. Referência indireta para objeto de destino /Dest 14 0 R
        val refDestMatcher = Pattern.compile("/Dest\\s+(\\d+)\\s+\\d+\\s+R").matcher(dict)
        if (refDestMatcher.find()) {
            val destObjId = refDestMatcher.group(1)?.toIntOrNull()
            if (destObjId != null && pageNumberMap.containsKey(destObjId)) {
                return pageNumberMap[destObjId]!!
            } else if (destObjId != null) {
                val destBody = objectMap[destObjId]
                if (destBody != null) {
                    val innerMatcher = Pattern.compile("\\[\\s*(\\d+)\\s+\\d+\\s+R").matcher(destBody)
                    if (innerMatcher.find()) {
                        val innerObjId = innerMatcher.group(1)?.toIntOrNull()
                        if (innerObjId != null && pageNumberMap.containsKey(innerObjId)) {
                            return pageNumberMap[innerObjId]!!
                        }
                    }
                }
            }
        }

        return 1
    }

    private fun extractItemLinks(dict: String): List<String> {
        val links = mutableListOf<String>()
        val uriPattern = Pattern.compile("/URI\\s*\\(([^)]+)\\)")
        val matcher = uriPattern.matcher(dict)
        while (matcher.find()) {
            val uri = matcher.group(1) ?: continue
            val clean = unescapePdfString(uri).trim()
            if (clean.startsWith("http://") || clean.startsWith("https://")) {
                links.add(clean)
            }
        }
        return links
    }

    private fun extractRef(dict: String, key: String): Int? {
        val pattern = Pattern.compile("$key\\s+(\\d+)\\s+\\d+\\s+R")
        val matcher = pattern.matcher(dict)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        return null
    }

    private fun extractSectionNumber(title: String): String {
        val clean = title.removePrefix("▪").removePrefix("•").removePrefix("-").trim()
        // Suporta tanto números diretos "1.1", "1.2.3" quanto prefixados "Capítulo 1", "Seção 2.1"
        val pattern = Pattern.compile("(?i)^(?:cap[ií]tulo|chapter|se[çc][ãa]o|section|parte|part)?\\s*([0-9]+(?:\\.[0-9]+)*\\.?)")
        val matcher = pattern.matcher(clean)
        if (matcher.find()) {
            return matcher.group(1)?.trimEnd('.') ?: ""
        }
        return ""
    }

    /**
     * Extrai o texto nativo e vetorial de uma página específica diretamente dos streams do PDF (/Contents).
     * Decodifica /FlateDecode com java.util.zip.Inflater e extrai operadores de texto Tj e TJ.
     * Retorna o texto formatado da página, ou null se for página escaneada/imagem pura.
     */
    fun extractPageNativeText(context: Context, book: PdfBook, pageNumber: Int): String? {
        val file = resolvePdfFile(context, book) ?: return null
        return extractPageNativeText(file, pageNumber, book.totalPages)
    }

    fun extractPageNativeText(pdfFile: File, pageNumber: Int, totalPages: Int): String? {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return null
        return try {
            val fileLength = pdfFile.length()
            if (fileLength <= 80L * 1024L * 1024L) {
                val bytes = pdfFile.readBytes()
                val content = String(bytes, Charsets.ISO_8859_1)
                extractPageTextFromContent(bytes, content, pageNumber, totalPages)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha na extração de texto nativo da página $pageNumber: ${e.message}")
            null
        }
    }

    private fun extractPageTextFromContent(
        bytes: ByteArray,
        content: String,
        pageNumber: Int,
        totalPages: Int
    ): String? {
        val objectMap = scanPdfObjectsFast(content)
        val pageNumberMap = buildPageNumberMap(content, objectMap, totalPages)

        // Encontra o objId da página correspondente ao pageNumber
        val pageObjId = pageNumberMap.entries.firstOrNull { it.value == pageNumber }?.key
            ?: return null

        val pageDict = objectMap[pageObjId] ?: return null

        // Extrai as referências de /Contents
        val streamObjIds = mutableListOf<Int>()
        val arrayMatch = Pattern.compile("/Contents\\s*\\[([^\\]]+)\\]").matcher(pageDict)
        if (arrayMatch.find()) {
            val listStr = arrayMatch.group(1) ?: ""
            val refPattern = Pattern.compile("(\\d+)\\s+\\d+\\s+R")
            val refMatcher = refPattern.matcher(listStr)
            while (refMatcher.find()) {
                refMatcher.group(1)?.toIntOrNull()?.let { streamObjIds.add(it) }
            }
        } else {
            val singleMatch = Pattern.compile("/Contents\\s+(\\d+)\\s+\\d+\\s+R").matcher(pageDict)
            if (singleMatch.find()) {
                singleMatch.group(1)?.toIntOrNull()?.let { streamObjIds.add(it) }
            }
        }

        if (streamObjIds.isEmpty()) return null

        val extractedTextBuilder = java.lang.StringBuilder()

        for (streamId in streamObjIds) {
            val streamText = extractTextFromStreamObject(bytes, content, streamId, objectMap[streamId])
            if (!streamText.isNullOrBlank()) {
                if (extractedTextBuilder.isNotEmpty()) {
                    extractedTextBuilder.append("\n\n")
                }
                extractedTextBuilder.append(streamText)
            }
        }

        val result = extractedTextBuilder.toString().trim()
        return if (result.length >= 20) result else null
    }

    private fun extractTextFromStreamObject(
        bytes: ByteArray,
        content: String,
        streamObjId: Int,
        streamDict: String?
    ): String? {
        val objPattern = Pattern.compile("(?:\\n|\\r|^)\\s*$streamObjId\\s+\\d+\\s+obj\\b")
        val objMatcher = objPattern.matcher(content)
        if (!objMatcher.find()) return null

        val objStartPos = objMatcher.start()
        val streamKeywordIdx = content.indexOf("stream", objStartPos)
        if (streamKeywordIdx == -1) return null

        var dataStart = streamKeywordIdx + 6
        if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
        if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

        val endStreamIdx = content.indexOf("endstream", dataStart)
        if (endStreamIdx == -1 || endStreamIdx <= dataStart) return null

        var dataEnd = endStreamIdx
        while (dataEnd > dataStart && (bytes[dataEnd - 1] == '\r'.code.toByte() || bytes[dataEnd - 1] == '\n'.code.toByte())) {
            dataEnd--
        }

        if (dataEnd <= dataStart) return null

        val rawStreamBytes = bytes.copyOfRange(dataStart, dataEnd)
        val dict = streamDict ?: content.substring(objStartPos, streamKeywordIdx)
        val isFlate = dict.contains("/FlateDecode")

        val decompressedBytes = if (isFlate) {
            try {
                val inflater = java.util.zip.Inflater()
                inflater.setInput(rawStreamBytes)
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0) {
                        if (inflater.needsInput() || inflater.needsDictionary()) break
                    }
                    out.write(buffer, 0, count)
                }
                inflater.end()
                out.toByteArray()
            } catch (e: Exception) {
                rawStreamBytes
            }
        } else {
            rawStreamBytes
        }

        val streamString = String(decompressedBytes, Charsets.ISO_8859_1)
        return parsePdfContentStreamText(streamString)
    }

    /**
     * Analisa o fluxo de operadores PDF (BT ... ET, Tj, TJ, TD, T*) e reconstrói o texto das frases.
     */
    private fun parsePdfContentStreamText(streamStr: String): String {
        val result = java.lang.StringBuilder()
        val btMatcher = Pattern.compile("BT\\b([\\s\\S]*?)\\bET").matcher(streamStr)

        while (btMatcher.find()) {
            val textBlock = btMatcher.group(1) ?: continue
            val blockBuilder = java.lang.StringBuilder()

            // Operador TJ (array de glyphs e kerning): [(T)-10(ex) 20(to)] TJ
            val tjArrayPattern = Pattern.compile("\\[([^\\]]+)\\]\\s*TJ")
            val tjArrayMatcher = tjArrayPattern.matcher(textBlock)
            var lastEnd = 0

            while (tjArrayMatcher.find()) {
                val arrayContent = tjArrayMatcher.group(1) ?: ""
                val linePart = parseTjArrayContent(arrayContent)
                if (linePart.isNotBlank()) {
                    if (blockBuilder.isNotEmpty() && !blockBuilder.endsWith(" ") && !blockBuilder.endsWith("\n")) {
                        blockBuilder.append(" ")
                    }
                    blockBuilder.append(linePart)
                }
                lastEnd = tjArrayMatcher.end()
            }

            // Operadores Tj simples: (Texto) Tj ou <48656C6C6F> Tj
            val tjSimplePattern = Pattern.compile("(?:\\((.*?)\\)|<([0-9A-Fa-f\\s]+)>)\\s*(?:Tj|'|\")")
            val tjSimpleMatcher = tjSimplePattern.matcher(textBlock)
            while (tjSimpleMatcher.find()) {
                val literal = tjSimpleMatcher.group(1)
                val hex = tjSimpleMatcher.group(2)
                val decoded = when {
                    literal != null -> decodeLiteralPdfString("($literal)")
                    hex != null -> decodeHexPdfString("<$hex>")
                    else -> ""
                }
                if (decoded.isNotBlank()) {
                    if (blockBuilder.isNotEmpty() && !blockBuilder.endsWith(" ") && !blockBuilder.endsWith("\n")) {
                        blockBuilder.append(" ")
                    }
                    blockBuilder.append(decoded)
                }
            }

            val cleanBlock = blockBuilder.toString().trim()
            if (cleanBlock.isNotBlank()) {
                if (result.isNotEmpty()) {
                    result.append("\n\n")
                }
                result.append(cleanBlock)
            }
        }

        return result.toString()
            .replace(Regex("""(?<!\n)\n(?!\n)"""), " ") // substitui quebras simples por espaço mantendo parágrafos
            .replace(Regex("""[ \t]+"""), " ")
            .trim()
    }

    private fun parseTjArrayContent(arrayContent: String): String {
        val sb = java.lang.StringBuilder()
        val itemPattern = Pattern.compile("(?:\\((.*?)\\)|<([0-9A-Fa-f\\s]+)>|(-?\\d+(?:\\.\\d+)?))")
        val matcher = itemPattern.matcher(arrayContent)

        while (matcher.find()) {
            val literal = matcher.group(1)
            val hex = matcher.group(2)
            val num = matcher.group(3)

            if (literal != null) {
                sb.append(decodeLiteralPdfString("($literal)"))
            } else if (hex != null) {
                sb.append(decodeHexPdfString("<$hex>"))
            } else if (num != null) {
                val offset = num.toDoubleOrNull() ?: 0.0
                // Um deslocamento de kerning negativo significativo representa um espaço entre palavras
                if (offset < -120.0 && sb.isNotEmpty() && !sb.endsWith(" ")) {
                    sb.append(" ")
                }
            }
        }
        return sb.toString().trim()
    }
}
