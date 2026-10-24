package com.example.data.repository

import com.example.data.local.AuthorCollectionDao
import com.example.data.local.BookHighlightDao
import com.example.data.local.BookPageRecommendationDao
import com.example.data.local.BookQuestionDao
import com.example.data.local.ComparativeLanguageTermDao
import com.example.data.local.FolderDao
import com.example.data.local.GeminiNoteDao
import com.example.data.local.OpenAiPageSummaryDao
import com.example.data.local.PageDictionaryTermDao
import com.example.data.local.PageMultiTranslationDao
import com.example.data.local.PageSegmentExplanationDao
import com.example.data.local.PdfBookDao
import com.example.data.local.ProjectDao
import com.example.data.local.ScientificPageReconstructionDao
import com.example.data.local.SegmentChatMessageDao
import com.example.data.local.SubjectDisciplineDao
import com.example.data.local.SavedSentenceAnalysisDao
import com.example.data.local.VerbPhrasalVerbDao
import com.example.data.local.PublicOrganizationDao
import com.example.data.model.AuthorCollection
import com.example.data.model.BookHighlight
import com.example.data.model.BookQuestion
import com.example.data.model.ComparativeLanguageTerm
import com.example.data.model.Folder
import com.example.data.model.GeminiNote
import com.example.data.model.OpenAiPageSummary
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PageMultiTranslation
import com.example.data.model.PageSegmentExplanation
import com.example.data.model.PdfBook
import com.example.data.model.Project
import com.example.data.model.ScientificPageReconstruction
import com.example.data.model.SegmentChatMessage
import com.example.data.model.SubjectDiscipline
import com.example.data.model.SavedSentenceAnalysis
import com.example.data.model.VerbPhrasalVerbStudyTerm
import com.example.data.model.PublicOrganization
import com.example.data.sample.SampleProjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val folderDao: FolderDao,
    private val geminiNoteDao: GeminiNoteDao,
    private val pdfBookDao: PdfBookDao,
    private val openAiPageSummaryDao: OpenAiPageSummaryDao,
    private val pageDictionaryTermDao: PageDictionaryTermDao,
    private val comparativeLanguageTermDao: ComparativeLanguageTermDao,
    private val pageMultiTranslationDao: PageMultiTranslationDao,
    private val bookQuestionDao: BookQuestionDao,
    private val bookPageRecommendationDao: BookPageRecommendationDao,
    private val scientificPageReconstructionDao: ScientificPageReconstructionDao,
    private val authorCollectionDao: AuthorCollectionDao,
    private val bookHighlightDao: BookHighlightDao,
    private val subjectDisciplineDao: SubjectDisciplineDao,
    private val pageSegmentExplanationDao: PageSegmentExplanationDao,
    private val segmentChatMessageDao: SegmentChatMessageDao,
    private val savedSentenceAnalysisDao: SavedSentenceAnalysisDao? = null,
    private val verbPhrasalVerbDao: VerbPhrasalVerbDao? = null,
    private val publicOrganizationDao: PublicOrganizationDao? = null
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    val allGeminiNotes: Flow<List<GeminiNote>> = geminiNoteDao.getAllNotes()
    val allPdfBooks: Flow<List<PdfBook>> = pdfBookDao.getAllPdfBooks()
    val allOpenAiSummaries: Flow<List<OpenAiPageSummary>> = openAiPageSummaryDao.getAllSummaries()
    val allDictionaryTerms: Flow<List<PageDictionaryTerm>> = pageDictionaryTermDao.getAllTerms()
    val allComparativeTerms: Flow<List<ComparativeLanguageTerm>> = comparativeLanguageTermDao.getAllTerms()
    val allPageTranslations: Flow<List<PageMultiTranslation>> = pageMultiTranslationDao.getAllTranslations()
    val allBookQuestions: Flow<List<BookQuestion>> = bookQuestionDao.getAllQuestions()
    val allBookRecommendations: Flow<List<com.example.data.model.BookPageRecommendation>> = bookPageRecommendationDao.getAllRecommendations()
    val allAuthors: Flow<List<AuthorCollection>> = authorCollectionDao.getAllAuthors()
    val allHighlights: Flow<List<BookHighlight>> = bookHighlightDao.getAllHighlights()
    val allSubjects: Flow<List<SubjectDiscipline>> = subjectDisciplineDao.getAllSubjects()
    val allSegmentExplanations: Flow<List<PageSegmentExplanation>> = pageSegmentExplanationDao.getAllExplanations()
    val allSegmentChatMessages: Flow<List<SegmentChatMessage>> = segmentChatMessageDao.getAllMessages()
    val allSavedSentenceAnalyses: Flow<List<SavedSentenceAnalysis>> =
        savedSentenceAnalysisDao?.getAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allVerbPhrasalTerms: Flow<List<VerbPhrasalVerbStudyTerm>> =
        verbPhrasalVerbDao?.getAllTerms() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allOrganizations: Flow<List<PublicOrganization>> =
        publicOrganizationDao?.getAllOrganizations() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun checkAndSeedInitialData() {
        val existingFolders = allFolders.first()
        if (existingFolders.isEmpty()) {
            for (f in SampleProjects.defaultFolders) {
                folderDao.insertFolder(f)
            }
            for (p in SampleProjects.defaultProjects) {
                projectDao.insertProject(p)
            }
        }
        val existingBooks = allPdfBooks.first()
        for (b in SampleProjects.defaultPdfBooks) {
            if (existingBooks.none { it.id == b.id || it.title == b.title }) {
                pdfBookDao.insertOrUpdate(b)
            }
        }

        val existingAuthors = allAuthors.first()
        val defaultAuthors = listOf(
            AuthorCollection(
                id = 1,
                name = "Machado de Assis",
                bio = "Um dos maiores gênios da literatura mundial. Autor de Dom Casmurro e fundador da Academia Brasileira de Letras.",
                photoUri = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Machado_de_Assis_real.jpg/800px-Machado_de_Assis_real.jpg",
                bannerColorHex = "#7C3AED"
            ),
            AuthorCollection(
                id = 2,
                name = "Language & Mind Institute",
                bio = "Centro de pesquisas em aquisição acelerada de idiomas, retenção cognitiva e neurociência da leitura imersiva.",
                photoUri = "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=400",
                bannerColorHex = "#10B981"
            ),
            AuthorCollection(
                id = 3,
                name = "Sêneca & Epicteto",
                bio = "Mestres do estoicismo helenístico e romano, dedicados à arte da serenidade, clareza mental e autodomínio.",
                photoUri = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400",
                bannerColorHex = "#D97706"
            ),
            AuthorCollection(
                id = 4,
                name = "Python Documentation Team",
                bio = "Equipe oficial de autores e engenheiros da Python Software Foundation, desenvolvedores do ecossistema Python.",
                photoUri = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=400",
                bannerColorHex = "#06B6D4"
            )
        )

        for (auth in defaultAuthors) {
            val existing = existingAuthors.firstOrNull { it.id == auth.id || it.name == auth.name }
            val authorId = if (existing != null) {
                existing.id
            } else {
                authorCollectionDao.insertOrUpdate(auth).toInt()
            }
            // Link corresponding book if book exists
            pdfBookDao.setBookAuthorCollection(auth.id, authorId)
        }
    }

    fun getProjectsByFolder(folderId: Int): Flow<List<Project>> {
        return projectDao.getProjectsByFolder(folderId)
    }

    suspend fun getProjectById(id: Int): Project? {
        return projectDao.getProjectById(id)
    }

    suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project)
    }

    suspend fun insertFolder(folder: Folder): Long {
        return folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        projectDao.removeFolderFromProjects(folder.id)
        folderDao.deleteFolder(folder)
    }

    fun getNotesForProject(projectId: Int): Flow<List<GeminiNote>> {
        return geminiNoteDao.getNotesForProject(projectId)
    }

    suspend fun getNoteForCue(projectId: Int, cueIndex: Int): GeminiNote? {
        return geminiNoteDao.getNoteForCue(projectId, cueIndex)
    }

    suspend fun getNoteByTerm(termOrSentence: String): GeminiNote? {
        return geminiNoteDao.getNoteByTerm(termOrSentence)
    }

    suspend fun getNoteByTermAndProject(projectId: Int, termOrSentence: String): GeminiNote? {
        return geminiNoteDao.getNoteByTermAndProject(projectId, termOrSentence)
    }

    suspend fun saveGeminiNote(note: GeminiNote): Long {
        return geminiNoteDao.insertOrUpdateNote(note)
    }

    suspend fun deleteGeminiNote(note: GeminiNote) {
        geminiNoteDao.deleteNote(note)
    }

    suspend fun deleteGeminiNoteForCue(projectId: Int, cueIndex: Int) {
        geminiNoteDao.deleteNoteForCue(projectId, cueIndex)
    }

    suspend fun insertPdfBook(book: PdfBook): Long {
        return pdfBookDao.insertOrUpdate(book)
    }

    suspend fun updatePdfBook(book: PdfBook) {
        pdfBookDao.update(book)
    }

    suspend fun updatePdfProgress(bookId: Int, page: Int) {
        pdfBookDao.updateProgress(bookId, page)
    }

    suspend fun updatePdfSpeechSpeed(bookId: Int, speed: Float) {
        pdfBookDao.updateSpeechSpeed(bookId, speed)
    }

    suspend fun updatePdfReadingTheme(bookId: Int, theme: String) {
        pdfBookDao.updateReadingTheme(bookId, theme)
    }

    suspend fun updateAllPdfReadingThemes(theme: String) {
        pdfBookDao.updateAllReadingThemes(theme)
    }

    suspend fun updatePdfStarredPages(bookId: Int, starredJson: String) {
        pdfBookDao.updateStarredPages(bookId, starredJson)
    }

    suspend fun updatePdfCategory(bookId: Int, category: String?, subcategory: String?, color: String?) {
        pdfBookDao.updateCategory(bookId, category, subcategory, color)
    }

    suspend fun deletePdfBook(book: PdfBook) {
        pdfBookDao.delete(book)
    }

    suspend fun deletePdfBookById(id: Int) {
        pdfBookDao.deleteById(id)
    }

    fun getSummaryForPage(bookId: Int, pageNumber: Int): Flow<OpenAiPageSummary?> {
        return openAiPageSummaryDao.getSummaryForPage(bookId, pageNumber)
    }

    suspend fun getSummaryForPageSync(bookId: Int, pageNumber: Int): OpenAiPageSummary? {
        return openAiPageSummaryDao.getSummaryForPageSync(bookId, pageNumber)
    }

    fun getAllSummariesForBook(bookId: Int): Flow<List<OpenAiPageSummary>> {
        return openAiPageSummaryDao.getAllSummariesForBook(bookId)
    }

    suspend fun saveOpenAiSummary(summary: OpenAiPageSummary): Long {
        return openAiPageSummaryDao.insertOrUpdate(summary)
    }

    suspend fun deleteOpenAiSummary(summary: OpenAiPageSummary) {
        openAiPageSummaryDao.delete(summary)
    }

    suspend fun deleteOpenAiSummaryForPage(bookId: Int, pageNumber: Int) {
        openAiPageSummaryDao.deleteByBookAndPage(bookId, pageNumber)
    }

    // --- Page & Universal Dictionary Operations ---

    fun getDictionaryTermsForPage(bookId: Int, pageNumber: Int): Flow<List<PageDictionaryTerm>> {
        return pageDictionaryTermDao.getTermsForPage(bookId, pageNumber)
    }

    suspend fun getDictionaryTermsForPageSync(bookId: Int, pageNumber: Int): List<PageDictionaryTerm> {
        return pageDictionaryTermDao.getTermsForPageSync(bookId, pageNumber)
    }

    fun getDictionaryTermsForBook(bookId: Int): Flow<List<PageDictionaryTerm>> {
        return pageDictionaryTermDao.getTermsForBook(bookId)
    }

    fun searchDictionaryTerms(query: String): Flow<List<PageDictionaryTerm>> {
        return pageDictionaryTermDao.searchTerms(query)
    }

    suspend fun replaceTermsForPage(bookId: Int, pageNumber: Int, terms: List<PageDictionaryTerm>) {
        pageDictionaryTermDao.deleteTermsForPage(bookId, pageNumber)
        if (terms.isNotEmpty()) {
            pageDictionaryTermDao.insertTerms(terms)
        }
    }

    suspend fun saveDictionaryTerms(terms: List<PageDictionaryTerm>) {
        if (terms.isNotEmpty()) {
            val existing = pageDictionaryTermDao.getAllTermsSync()
                .map { it.term.trim().lowercase() }
                .toSet()
            val uniqueTerms = terms
                .distinctBy { it.term.trim().lowercase() }
                .filter { it.term.trim().lowercase() !in existing && it.term.isNotBlank() }
            if (uniqueTerms.isNotEmpty()) {
                pageDictionaryTermDao.insertTerms(uniqueTerms)
            }
        }
    }

    suspend fun saveDictionaryTerm(term: PageDictionaryTerm): Long {
        if (term.term.isBlank()) return -1L
        val existing = pageDictionaryTermDao.findTermByName(term.term.trim())
        if (existing != null) {
            // Termo já existente no dicionário - previne duplicatas
            return existing.id.toLong()
        }
        return pageDictionaryTermDao.insertOrUpdate(term)
    }

    suspend fun deleteDictionaryTerm(term: PageDictionaryTerm) {
        pageDictionaryTermDao.delete(term)
    }

    suspend fun deleteDictionaryTermsForPage(bookId: Int, pageNumber: Int) {
        pageDictionaryTermDao.deleteTermsForPage(bookId, pageNumber)
    }

    suspend fun deleteDictionaryTermsForBook(bookId: Int) {
        pageDictionaryTermDao.deleteTermsForBook(bookId)
    }

    // --- Comparative Language Terms ---

    fun getComparativeTermsForPage(bookId: Int, pageNumber: Int): Flow<List<ComparativeLanguageTerm>> {
        return comparativeLanguageTermDao.getTermsForPage(bookId, pageNumber)
    }

    suspend fun getComparativeTermsForPageSync(bookId: Int, pageNumber: Int): List<ComparativeLanguageTerm> {
        return comparativeLanguageTermDao.getTermsForPageSync(bookId, pageNumber)
    }

    fun getComparativeTermsForBook(bookId: Int): Flow<List<ComparativeLanguageTerm>> {
        return comparativeLanguageTermDao.getTermsForBook(bookId)
    }

    fun searchComparativeTerms(query: String): Flow<List<ComparativeLanguageTerm>> {
        return comparativeLanguageTermDao.searchTerms(query)
    }

    suspend fun replaceComparativeTermsForPage(bookId: Int, pageNumber: Int, terms: List<ComparativeLanguageTerm>) {
        comparativeLanguageTermDao.deleteTermsForPage(bookId, pageNumber)
        if (terms.isNotEmpty()) {
            comparativeLanguageTermDao.insertTerms(terms)
        }
    }

    suspend fun saveComparativeTerms(terms: List<ComparativeLanguageTerm>) {
        if (terms.isNotEmpty()) {
            comparativeLanguageTermDao.insertTerms(terms)
        }
    }

    suspend fun saveComparativeTerm(term: ComparativeLanguageTerm): Long {
        return comparativeLanguageTermDao.insertOrUpdate(term)
    }

    suspend fun deleteComparativeTerm(term: ComparativeLanguageTerm) {
        comparativeLanguageTermDao.delete(term)
    }

    suspend fun deleteComparativeTermsForPage(bookId: Int, pageNumber: Int) {
        comparativeLanguageTermDao.deleteTermsForPage(bookId, pageNumber)
    }

    suspend fun deleteComparativeTermsForBook(bookId: Int) {
        comparativeLanguageTermDao.deleteTermsForBook(bookId)
    }

    // --- Page Multi-Translation (Offline Trilateral Translations) ---

    fun getTranslationForPage(bookId: Int, pageNumber: Int): Flow<PageMultiTranslation?> {
        return pageMultiTranslationDao.getTranslationForPage(bookId, pageNumber)
    }

    suspend fun getTranslationForPageSync(bookId: Int, pageNumber: Int): PageMultiTranslation? {
        return pageMultiTranslationDao.getTranslationForPageSync(bookId, pageNumber)
    }

    fun getTranslationsForBook(bookId: Int): Flow<List<PageMultiTranslation>> {
        return pageMultiTranslationDao.getTranslationsForBook(bookId)
    }

    suspend fun savePageTranslation(translation: PageMultiTranslation): Long {
        return pageMultiTranslationDao.insertOrUpdate(translation)
    }

    suspend fun deletePageTranslation(translation: PageMultiTranslation) {
        pageMultiTranslationDao.delete(translation)
    }

    suspend fun deletePageTranslationForPage(bookId: Int, pageNumber: Int) {
        pageMultiTranslationDao.deleteForPage(bookId, pageNumber)
    }

    suspend fun deleteAllPageTranslationsForBook(bookId: Int) {
        pageMultiTranslationDao.deleteAllForBook(bookId)
    }

    // --- Book Questions (FGV, CEBRASPE, FCC, IADES, Offline) ---

    fun getQuestionsForBook(bookId: Int): Flow<List<BookQuestion>> {
        return bookQuestionDao.getQuestionsForBook(bookId)
    }

    fun getQuestionsForPage(bookId: Int, pageNumber: Int): Flow<List<BookQuestion>> {
        return bookQuestionDao.getQuestionsForPage(bookId, pageNumber)
    }

    suspend fun getQuestionsForPageSync(bookId: Int, pageNumber: Int): List<BookQuestion> {
        return bookQuestionDao.getQuestionsForPageSync(bookId, pageNumber)
    }

    suspend fun saveQuestions(questions: List<BookQuestion>): List<Long> {
        return bookQuestionDao.insertQuestions(questions)
    }

    suspend fun saveQuestion(question: BookQuestion): Long {
        return bookQuestionDao.insertQuestion(question)
    }

    suspend fun updateQuestion(question: BookQuestion) {
        bookQuestionDao.updateQuestion(question)
    }

    suspend fun recordQuestionAnswer(id: Long, userAnswerIndex: Int, isCorrect: Boolean) {
        bookQuestionDao.recordAnswer(id, userAnswerIndex, isCorrect)
    }

    suspend fun resetQuestionAnswer(id: Long) {
        bookQuestionDao.resetAnswer(id)
    }

    suspend fun resetAllAnswersForBook(bookId: Int) {
        bookQuestionDao.resetAllAnswersForBook(bookId)
    }

    suspend fun resetAllQuestionsGlobal() {
        bookQuestionDao.resetAllAnswersGlobal()
    }

    suspend fun deleteQuestion(id: Long) {
        bookQuestionDao.deleteQuestionById(id)
    }

    suspend fun deleteQuestionsForPage(bookId: Int, page: Int) {
        bookQuestionDao.deleteQuestionsForPage(bookId, page)
    }

    suspend fun deleteQuestionsForBook(bookId: Int) {
        bookQuestionDao.deleteQuestionsForBook(bookId)
    }

    // -------------------------------------------------------------
    // BOOKS: Recomendações Bibliográficas por Página e Termos
    // -------------------------------------------------------------
    fun getBookRecommendationsForPage(bookId: Int, pageNumber: Int): Flow<List<com.example.data.model.BookPageRecommendation>> {
        return bookPageRecommendationDao.getRecommendationsForPage(bookId, pageNumber)
    }

    suspend fun getBookRecommendationsForPageSync(bookId: Int, pageNumber: Int): List<com.example.data.model.BookPageRecommendation> {
        return bookPageRecommendationDao.getRecommendationsForPageSync(bookId, pageNumber)
    }

    fun getAllRecommendationsForBook(bookId: Int): Flow<List<com.example.data.model.BookPageRecommendation>> {
        return bookPageRecommendationDao.getAllRecommendationsForBook(bookId)
    }

    suspend fun saveBookRecommendations(recommendations: List<com.example.data.model.BookPageRecommendation>): List<Long> {
        return bookPageRecommendationDao.insertAll(recommendations)
    }

    suspend fun replaceBookRecommendationsForPage(bookId: Int, pageNumber: Int, recommendations: List<com.example.data.model.BookPageRecommendation>) {
        bookPageRecommendationDao.deleteForPage(bookId, pageNumber)
        if (recommendations.isNotEmpty()) {
            bookPageRecommendationDao.insertAll(recommendations)
        }
    }

    suspend fun deleteBookRecommendationsForPage(bookId: Int, pageNumber: Int) {
        bookPageRecommendationDao.deleteForPage(bookId, pageNumber)
    }

    // -------------------------------------------------------------
    // BOOKS: Reconstrução Científica da Página (Matemática, Química, Citologia, Biologia, Lógica)
    // -------------------------------------------------------------
    fun getScientificReconstructionForPage(bookId: Int, pageNumber: Int): Flow<ScientificPageReconstruction?> {
        return scientificPageReconstructionDao.getReconstructionForPage(bookId, pageNumber)
    }

    suspend fun getScientificReconstructionForPageSync(bookId: Int, pageNumber: Int): ScientificPageReconstruction? {
        return scientificPageReconstructionDao.getReconstructionForPageSync(bookId, pageNumber)
    }

    suspend fun saveScientificReconstruction(reconstruction: ScientificPageReconstruction): Long {
        return scientificPageReconstructionDao.insertOrUpdate(reconstruction)
    }

    suspend fun deleteScientificReconstruction(bookId: Int, pageNumber: Int) {
        scientificPageReconstructionDao.deleteForPage(bookId, pageNumber)
    }

    // -------------------------------------------------------------
    // AUTHORS & COLLECTIONS
    // -------------------------------------------------------------
    fun getAuthorById(id: Int): Flow<AuthorCollection?> {
        return authorCollectionDao.getAuthorById(id)
    }

    suspend fun getAuthorByIdSync(id: Int): AuthorCollection? {
        return authorCollectionDao.getAuthorByIdSync(id)
    }

    suspend fun saveAuthor(author: AuthorCollection): Long {
        return authorCollectionDao.insertOrUpdate(author)
    }

    suspend fun deleteAuthor(author: AuthorCollection) {
        pdfBookDao.clearAuthorFromBooks(author.id)
        authorCollectionDao.delete(author)
    }

    suspend fun setBookAuthorCollection(bookId: Int, authorId: Int?) {
        pdfBookDao.setBookAuthorCollection(bookId, authorId)
    }

    suspend fun assignBooksToAuthor(authorId: Int, bookIds: List<Int>) {
        // Clear old ones if not in list
        val allBooks = allPdfBooks.first()
        for (book in allBooks) {
            if (book.authorCollectionId == authorId && !bookIds.contains(book.id)) {
                pdfBookDao.setBookAuthorCollection(book.id, null)
            } else if (bookIds.contains(book.id) && book.authorCollectionId != authorId) {
                pdfBookDao.setBookAuthorCollection(book.id, authorId)
            }
        }
    }

    suspend fun removeBookFromAuthor(bookId: Int) {
        pdfBookDao.setBookAuthorCollection(bookId, null)
    }

    fun getBooksByAuthorId(authorId: Int): Flow<List<PdfBook>> {
        return pdfBookDao.getBooksByAuthorId(authorId)
    }

    fun getStandaloneBooks(): Flow<List<PdfBook>> {
        return pdfBookDao.getStandaloneBooks()
    }

    fun getHighlightsForBook(bookId: Int): Flow<List<BookHighlight>> {
        return bookHighlightDao.getHighlightsForBook(bookId)
    }

    fun getHighlightsForPage(bookId: Int, pageNumber: Int): Flow<List<BookHighlight>> {
        return bookHighlightDao.getHighlightsForPage(bookId, pageNumber)
    }

    suspend fun insertHighlight(highlight: BookHighlight): Long {
        return bookHighlightDao.insertHighlight(highlight)
    }

    suspend fun updateHighlight(highlight: BookHighlight) {
        bookHighlightDao.updateHighlight(highlight)
    }

    suspend fun deleteHighlight(highlight: BookHighlight) {
        bookHighlightDao.deleteHighlight(highlight)
    }

    suspend fun deleteHighlightById(id: Int) {
        bookHighlightDao.deleteById(id)
    }

    // -------------------------------------------------------------
    // SUBJECTS / DISCIPLINES
    // -------------------------------------------------------------
    suspend fun saveSubject(subject: SubjectDiscipline): Long {
        return subjectDisciplineDao.insertOrUpdate(subject)
    }

    suspend fun deleteSubject(subject: SubjectDiscipline) {
        subjectDisciplineDao.delete(subject)
    }

    suspend fun updateSubjectAssociations(subjectId: Int, authorIds: List<Int>, directBookIds: List<Int>) {
        val subject = subjectDisciplineDao.getSubjectByIdSync(subjectId) ?: return
        val updated = subject.copy(
            authorIdsJson = SubjectDiscipline.listToJson(authorIds),
            directBookIdsJson = SubjectDiscipline.listToJson(directBookIds)
        )
        subjectDisciplineDao.update(updated)
    }

    suspend fun addDirectBookToSubject(subjectId: Int, bookId: Int) {
        val subject = subjectDisciplineDao.getSubjectByIdSync(subjectId) ?: return
        val currentDirectBooks = subject.getDirectBookIds().toMutableList()
        if (!currentDirectBooks.contains(bookId)) {
            currentDirectBooks.add(bookId)
            val updated = subject.copy(
                directBookIdsJson = SubjectDiscipline.listToJson(currentDirectBooks)
            )
            subjectDisciplineDao.update(updated)
        }
    }

    // -------------------------------------------------------------
    // PUBLIC ORGANIZATIONS & EDITAIS
    // -------------------------------------------------------------
    suspend fun saveOrganization(org: PublicOrganization): Long {
        return publicOrganizationDao?.insertOrUpdate(org) ?: 0L
    }

    suspend fun deleteOrganization(org: PublicOrganization) {
        publicOrganizationDao?.delete(org)
    }

    suspend fun addPdfBookToOrganization(orgId: Int, bookId: Int) {
        val dao = publicOrganizationDao ?: return
        val org = dao.getOrganizationByIdSync(orgId) ?: return
        val updated = org.withAddedPdfBookId(bookId)
        dao.update(updated)
    }

    suspend fun removePdfBookFromOrganization(orgId: Int, bookId: Int) {
        val dao = publicOrganizationDao ?: return
        val org = dao.getOrganizationByIdSync(orgId) ?: return
        val updated = org.withRemovedPdfBookId(bookId)
        dao.update(updated)
    }

    suspend fun checkAndSeedInitialOrganizations() {
        val dao = publicOrganizationDao ?: return
        val existing = allOrganizations.firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                PublicOrganization(
                    name = "Receita Federal do Brasil",
                    organizationType = "Órgão de Governo",
                    description = "Editais para Auditor-Fiscal e Analista Tributário",
                    iconPreset = "RECEITA",
                    accentColorHex = "#0284C7"
                ),
                PublicOrganization(
                    name = "Polícia Federal (DPF)",
                    organizationType = "Órgão de Governo",
                    description = "Editais de Agente, Escrivão, Papiloscopista e Delegado",
                    iconPreset = "POLICIA_FEDERAL",
                    accentColorHex = "#1E293B"
                ),
                PublicOrganization(
                    name = "INSS - Previdência Social",
                    organizationType = "Autarquia Federal",
                    description = "Editais e normas para Técnico e Analista do Seguro Social",
                    iconPreset = "INSS",
                    accentColorHex = "#059669"
                ),
                PublicOrganization(
                    name = "Tribunal de Contas da União (TCU)",
                    organizationType = "Órgão de Governo",
                    description = "Editais de Auditor Federal de Controle Externo",
                    iconPreset = "TCU",
                    accentColorHex = "#D97706"
                )
            )
            defaults.forEach { dao.insertOrUpdate(it) }
        }
    }

    // -------------------------------------------------------------
    // PAGE SEGMENT EXPLANATIONS (Trilateral In-Place Explanations)
    // -------------------------------------------------------------
    fun getSegmentExplanationsForPage(bookId: Int, pageNumber: Int): Flow<List<PageSegmentExplanation>> {
        return pageSegmentExplanationDao.getExplanationsForPage(bookId, pageNumber)
    }

    suspend fun getSegmentExplanationsForPageSync(bookId: Int, pageNumber: Int): List<PageSegmentExplanation> {
        return pageSegmentExplanationDao.getExplanationsForPageSync(bookId, pageNumber)
    }

    fun getAllSegmentExplanationsForBook(bookId: Int): Flow<List<PageSegmentExplanation>> {
        return pageSegmentExplanationDao.getAllExplanationsForBook(bookId)
    }

    suspend fun getSegmentExplanation(bookId: Int, pageNumber: Int, segmentIndex: Int): PageSegmentExplanation? {
        return pageSegmentExplanationDao.getExplanationForSegment(bookId, pageNumber, segmentIndex)
    }

    suspend fun saveSegmentExplanation(explanation: PageSegmentExplanation): Long {
        return pageSegmentExplanationDao.insertOrUpdate(explanation)
    }

    suspend fun saveSegmentExplanations(explanations: List<PageSegmentExplanation>) {
        pageSegmentExplanationDao.insertOrUpdateAll(explanations)
    }

    suspend fun deleteSegmentExplanationById(id: Long) {
        pageSegmentExplanationDao.deleteById(id)
    }

    suspend fun deleteSegmentExplanation(bookId: Int, pageNumber: Int, segmentIndex: Int) {
        pageSegmentExplanationDao.deleteForSegment(bookId, pageNumber, segmentIndex)
    }

    suspend fun deleteSegmentExplanationsForPage(bookId: Int, pageNumber: Int) {
        pageSegmentExplanationDao.deleteForPage(bookId, pageNumber)
    }

    suspend fun deleteAllSegmentExplanationsForBook(bookId: Int) {
        pageSegmentExplanationDao.deleteAllForBook(bookId)
    }

    fun getSegmentChatMessages(bookId: Int, pageNumber: Int, segmentIndex: Int): Flow<List<SegmentChatMessage>> {
        return segmentChatMessageDao.getMessagesForSegment(bookId, pageNumber, segmentIndex)
    }

    suspend fun saveSegmentChatMessage(message: SegmentChatMessage): Long {
        return segmentChatMessageDao.insertMessage(message)
    }

    suspend fun deleteSegmentChatMessage(message: SegmentChatMessage) {
        segmentChatMessageDao.deleteMessage(message)
    }

    suspend fun clearSegmentChat(bookId: Int, pageNumber: Int, segmentIndex: Int) {
        segmentChatMessageDao.clearMessagesForSegment(bookId, pageNumber, segmentIndex)
    }

    fun getSegmentChatMessageCount(bookId: Int, pageNumber: Int, segmentIndex: Int): Flow<Int> {
        return segmentChatMessageDao.getMessageCountForSegment(bookId, pageNumber, segmentIndex)
    }

    // =========================================================================
    // CADERNO DE ESTUDOS DE SINTAXE E GRAMÁTICA POLIGLOTA (PERSISTÊNCIA LOCAL)
    // =========================================================================
    fun getSavedSentenceAnalysesForBook(bookId: Int): Flow<List<SavedSentenceAnalysis>> {
        return savedSentenceAnalysisDao?.getByBook(bookId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    fun getSavedSentenceAnalysesByLanguage(langCode: String): Flow<List<SavedSentenceAnalysis>> {
        return savedSentenceAnalysisDao?.getByLanguage(langCode) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    fun getFavoriteSentenceAnalyses(): Flow<List<SavedSentenceAnalysis>> {
        return savedSentenceAnalysisDao?.getFavorites() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveSentenceAnalysis(analysis: SavedSentenceAnalysis): Long {
        return savedSentenceAnalysisDao?.insert(analysis) ?: -1L
    }

    suspend fun updateSentenceAnalysis(analysis: SavedSentenceAnalysis) {
        savedSentenceAnalysisDao?.update(analysis)
    }

    suspend fun deleteSentenceAnalysis(id: Long) {
        savedSentenceAnalysisDao?.deleteById(id)
    }

    suspend fun toggleFavoriteSentenceAnalysis(analysis: SavedSentenceAnalysis) {
        savedSentenceAnalysisDao?.update(analysis.copy(isFavorite = !analysis.isFavorite))
    }

    // =========================================================================
    // ESTUDO DE VERBOS & PHRASAL VERBS (PREPOSIÇÕES & 16 EXEMPLOS QUESTION WORDS)
    // =========================================================================
    fun getVerbPhrasalTermsForPage(bookId: Int, pageNumber: Int): Flow<List<VerbPhrasalVerbStudyTerm>> {
        return verbPhrasalVerbDao?.getTermsForPage(bookId, pageNumber) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    fun getVerbPhrasalTermsForBook(bookId: Int): Flow<List<VerbPhrasalVerbStudyTerm>> {
        return verbPhrasalVerbDao?.getTermsForBook(bookId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    fun getVerbPhrasalTermsByLanguage(langCode: String): Flow<List<VerbPhrasalVerbStudyTerm>> {
        return verbPhrasalVerbDao?.getTermsByLanguage(langCode) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    fun searchVerbPhrasalTerms(query: String): Flow<List<VerbPhrasalVerbStudyTerm>> {
        return verbPhrasalVerbDao?.searchTerms(query) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveVerbPhrasalTerms(terms: List<VerbPhrasalVerbStudyTerm>) {
        verbPhrasalVerbDao?.insertTerms(terms)
    }

    suspend fun insertOrUpdateVerbPhrasalTerm(term: VerbPhrasalVerbStudyTerm): Long {
        return verbPhrasalVerbDao?.insertOrUpdate(term) ?: -1L
    }

    suspend fun deleteVerbPhrasalTerm(term: VerbPhrasalVerbStudyTerm) {
        verbPhrasalVerbDao?.delete(term)
    }

    suspend fun deleteVerbPhrasalTermsForPage(bookId: Int, pageNumber: Int) {
        verbPhrasalVerbDao?.deleteTermsForPage(bookId, pageNumber)
    }

    suspend fun toggleFavoriteVerbPhrasalTerm(term: VerbPhrasalVerbStudyTerm) {
        verbPhrasalVerbDao?.updateFavorite(term.id, !term.isFavorite)
    }
}
