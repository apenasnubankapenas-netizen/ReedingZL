package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComparativeLanguageTerm
import com.example.data.model.GeminiNote
import com.example.data.model.OpenAiPageSummary
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.ComparativeParagraph
import com.example.data.model.BookHighlight
import com.example.data.model.PageMultiTranslation
import com.example.data.model.SegmentChatMessage
import com.example.data.model.PdfBook
import com.example.data.model.ReadingLanguage
import com.example.data.model.PdfViewMode
import com.example.ui.components.FontSizeDialog
import com.example.ui.components.HighlightsNotebookDialog
import com.example.ui.components.SegmentAiChatCard
import com.example.data.model.BookQuestion
import com.example.data.model.BookPageRecommendation
import com.example.data.pdf.BookParagraph
import com.example.data.pdf.PdfRendererHelper
import com.example.data.pdf.PdfTocItem
import com.example.data.speech.PdfSpeechManager
import com.example.data.speech.TtsVoiceInfo
import com.example.ui.components.BigPageNumberOverlay
import com.example.ui.components.ComparativeLanguageDialog
import com.example.ui.components.GeminiExplanationDialog
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.HelpOutline
import com.example.data.model.BookCategoryTaxonomy
import com.example.ui.components.ComparativeKaraokeLine
import com.example.ui.components.BookCoverCategoryTopBadge
import com.example.ui.components.BookCategoryDetailsDialog
import com.example.ui.components.InteractiveKaraokeParagraph
import com.example.ui.components.KaraokeDefinitionText
import com.example.ui.components.AllLanguagesDialog
import com.example.ui.components.InteractiveKaraokeSentence
import com.example.ui.components.InteractiveQuestionCard
import com.example.ui.components.BookQuestionsDialog
import com.example.ui.components.GlobalQuestionsReelDialog
import com.example.ui.components.InteractiveTocDialog
import com.example.ui.components.OpenAiDictionaryDialog
import com.example.ui.components.OpenAiStudyFeaturesBottomSheet
import com.example.ui.components.OpenAiSummaryDialog
import com.example.ui.components.PageMultiTranslationDialog
import com.example.ui.components.PageRestructureDialog
import com.example.ui.components.ScientificReconstructionDialog
import com.example.ui.components.VoiceSelectorBottomSheet
import com.example.ui.components.OfflinePageReaderDialog
import com.example.ui.components.PlanaltoLegislationDialog
import com.example.ui.components.EnglishWordAnalysisDialog
import com.example.ui.components.EnglishPageVocabularyDialog
import com.example.ui.components.PolyglotWordAnalysisDialog
import com.example.ui.components.PolyglotPageVocabularyDialog
import com.example.ui.components.SentenceGrammarAnalysisDialog
import com.example.ui.components.MultilingualTermItem
import com.example.ui.components.SavedSentencesStudySheet
import com.example.data.model.SavedSentenceAnalysis
import com.example.data.model.StudyLanguage
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import com.example.ui.components.AuthorCollectionDialog
import com.example.ui.components.AuthorReelsSlide
import com.example.ui.components.ManageAuthorBooksDialog
import com.example.ui.components.BookAndAuthorPickerDialog
import com.example.data.model.AuthorCollection
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import com.example.data.model.SubjectDiscipline
import com.example.data.model.PageSegmentExplanation
import com.example.ui.components.SubjectReelsSlide
import com.example.ui.components.SubjectDisciplineDialog
import com.example.ui.components.ManageSubjectContentDialog
import com.example.ui.components.BooksGridReelsSlide
import androidx.compose.material.icons.filled.GridView
import com.example.data.model.PublicOrganization
import com.example.ui.components.PublicOrganizationsGridReelsSlide
import com.example.ui.components.PublicOrganizationDetailDialog
import com.example.ui.components.AddEditPublicOrganizationDialog
import com.example.ui.components.StudyAlarmSettingsDialog
import com.example.ui.components.StudyAlarmAlertOverlay
import com.example.ui.viewmodel.MainViewModel
import com.example.data.translator.TermsNotesTranslator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ReelsFilterMode {
    ALL,
    BOOKS,
    AUTHORS,
    SUBJECTS,
    ORGANIZATIONS
}

enum class ReelsBooksLayoutMode {
    GRID_4,
    SINGLE
}

sealed class ReelsFeedItem(val key: String) {
    data class BookItem(val book: PdfBook) : ReelsFeedItem("book_${book.id}")
    data class BooksGridItem(val books: List<PdfBook>, val pageIndex: Int) : ReelsFeedItem("books_grid_${pageIndex}_${books.firstOrNull()?.id ?: 0}")
    data class AuthorItem(val author: AuthorCollection, val authorBooks: List<PdfBook>) : ReelsFeedItem("author_${author.id}")
    data class SubjectItem(val subject: SubjectDiscipline, val subjectBooks: List<PdfBook>, val subjectAuthors: List<AuthorCollection>) : ReelsFeedItem("subject_${subject.id}")
    data class OrganizationsGridItem(val organizations: List<PublicOrganization>, val pageIndex: Int) : ReelsFeedItem("orgs_grid_${pageIndex}_${organizations.firstOrNull()?.id ?: 0}")
}

private data class HudThemeColors(
    val surfaceBg: Color,
    val borderColor: Color,
    val accentColor: Color,
    val textColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsBookPagerScreen(
    books: List<PdfBook>,
    initialBookIndex: Int = 0,
    readingTheme: String,
    speechManager: PdfSpeechManager,
    isSpeaking: Boolean,
    activeSentenceIndex: Int,
    speechSpeed: Float,
    starredPages: Set<Int>,
    geminiNotes: List<GeminiNote>,
    openAiSummaries: List<OpenAiPageSummary> = emptyList(),
    dictionaryTerms: List<PageDictionaryTerm> = emptyList(),
    comparativeTerms: List<ComparativeLanguageTerm> = emptyList(),
    verbPhrasalTerms: List<com.example.data.model.VerbPhrasalVerbStudyTerm> = emptyList(),
    onSaveVerbPhrasalTerms: (List<com.example.data.model.VerbPhrasalVerbStudyTerm>) -> Unit = {},
    onToggleFavoriteVerb: (com.example.data.model.VerbPhrasalVerbStudyTerm) -> Unit = {},
    pageTranslations: List<PageMultiTranslation> = emptyList(),
    segmentExplanations: List<PageSegmentExplanation> = emptyList(),
    isGeneratingSegmentExplanation: Boolean = false,
    segmentExplanationError: String? = null,
    bookQuestions: List<BookQuestion> = emptyList(),
    isGeneratingQuestions: Boolean = false,
    questionsGenerationError: String? = null,
    bookRecommendations: List<BookPageRecommendation> = emptyList(),
    isGeneratingBookRecommendations: Boolean = false,
    bookRecommendationsError: String? = null,
    isClassifyingBook: Boolean = false,
    onCloseReels: () -> Unit,
    onPageChanged: (book: PdfBook, page: Int) -> Unit,
    onToggleSpeech: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onThemeChange: (String) -> Unit,
    onToggleStarPage: (page: Int) -> Unit,
    onSaveGeminiNote: (cueIndex: Int, term: String, context: String, responseText: String, targetBookId: Int?) -> Unit = { _, _, _, _, _ -> },
    onDeleteGeminiNoteForCue: (cueIndex: Int, targetBookId: Int?) -> Unit = { _, _ -> },
    onSaveOpenAiSummary: (OpenAiPageSummary) -> Unit = {},
    onDeleteOpenAiSummary: (OpenAiPageSummary) -> Unit = {},
    onSaveDictionaryTerms: (List<PageDictionaryTerm>) -> Unit = {},
    onReplaceDictionaryTermsForPage: (bookId: Int, page: Int, List<PageDictionaryTerm>) -> Unit = { _, _, _ -> },
    onUpdateDictionaryTerm: (PageDictionaryTerm) -> Unit = {},
    onDeleteDictionaryTerm: (PageDictionaryTerm) -> Unit = {},
    onDeleteDictionaryTermsForPage: (bookId: Int, page: Int) -> Unit = { _, _ -> },
    onSaveComparativeTerms: (List<ComparativeLanguageTerm>) -> Unit = {},
    onReplaceComparativeTermsForPage: (bookId: Int, page: Int, List<ComparativeLanguageTerm>) -> Unit = { _, _, _ -> },
    onUpdateComparativeTerm: (ComparativeLanguageTerm) -> Unit = {},
    onDeleteComparativeTerm: (ComparativeLanguageTerm) -> Unit = {},
    onDeleteComparativeTermsForPage: (bookId: Int, page: Int) -> Unit = { _, _ -> },
    onSavePageTranslation: (PageMultiTranslation) -> Unit = {},
    onDeletePageTranslation: (PageMultiTranslation) -> Unit = {},
    onDeletePageTranslationsForPage: (bookId: Int, page: Int) -> Unit = { _, _ -> },
    onRequestSegmentExplanation: (book: PdfBook, pageNumber: Int, segmentIndex: Int, segmentText: String, forceRegenerate: Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteSegmentExplanation: (PageSegmentExplanation) -> Unit = {},
    onAnswerQuestion: (questionId: Long, selectedIndex: Int, isCorrect: Boolean) -> Unit = { _, _, _ -> },
    onResetQuestionAnswer: (questionId: Long) -> Unit = {},
    onResetAllAnswers: (bookId: Int) -> Unit = {},
    onResetAllQuestionsGlobal: () -> Unit = {},
    onDeleteQuestion: (questionId: Long) -> Unit = {},
    onDeleteQuestionsForPage: (bookId: Int, page: Int) -> Unit = { _, _ -> },
    onDeleteAllQuestionsForBook: (bookId: Int) -> Unit = {},
    onGenerateQuestions: (book: PdfBook, startPage: Int, endPage: Int, institution: String, difficulty: String, count: Int, model: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onGenerateBookRecommendations: (book: PdfBook, pageNumber: Int, pageText: String, forceRegenerate: Boolean) -> Unit = { _, _, _, _ -> },
    onDeleteBookRecommendationsForPage: (bookId: Int, pageNumber: Int) -> Unit = { _, _ -> },
    onReclassifyCategory: (book: PdfBook) -> Unit = {},
    onSelectCategory: (bookId: Int, category: String, subcategory: String?, colorHex: String) -> Unit = { _, _, _, _ -> },
    onWordClick: (book: PdfBook, sentenceIndex: Int, wordIndex: Int, sentences: List<String>) -> Unit = { _, _, _, _ -> },
    authors: List<AuthorCollection> = emptyList(),
    onSaveAuthor: (AuthorCollection, List<Int>) -> Unit = { _, _ -> },
    onDeleteAuthor: (AuthorCollection) -> Unit = {},
    onAssignBooksToAuthor: (authorId: Int, bookIds: List<Int>) -> Unit = { _, _ -> },
    subjects: List<SubjectDiscipline> = emptyList(),
    onSaveSubject: (SubjectDiscipline, List<Int>, List<Int>) -> Unit = { _, _, _ -> },
    onDeleteSubject: (SubjectDiscipline) -> Unit = {},
    onUpdateSubjectAssociations: (subjectId: Int, authorIds: List<Int>, directBookIds: List<Int>) -> Unit = { _, _, _ -> },
    onAddDirectBooksToSubject: (subjectId: Int, List<MainViewModel.MultiPdfBookItem>) -> Unit = { _, _ -> },
    highlights: List<BookHighlight> = emptyList(),
    onSaveHighlight: (BookHighlight) -> Unit = {},
    onDeleteHighlight: (BookHighlight) -> Unit = {},
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val effectiveSegmentExplanations: List<PageSegmentExplanation> = if (segmentExplanations.isNotEmpty() || viewModel == null) {
        segmentExplanations
    } else {
        viewModel.allSegmentExplanations.collectAsState(initial = emptyList<PageSegmentExplanation>()).value
    }
    val effectiveIsGeneratingExplanation = if (isGeneratingSegmentExplanation || viewModel == null) {
        isGeneratingSegmentExplanation
    } else {
        viewModel.isGeneratingSegmentExplanation.collectAsState().value
    }
    val effectiveExplanationError = segmentExplanationError ?: viewModel?.segmentExplanationError?.collectAsState()?.value

    val effectiveOnRequestExplanation: (PdfBook, Int, Int, String, Boolean) -> Unit = { book, page, segIdx, text, force ->
        if (viewModel != null) {
            viewModel.requestSegmentExplanation(book, page, segIdx, text, force)
        } else {
            onRequestSegmentExplanation(book, page, segIdx, text, force)
        }
    }
    val effectiveOnDeleteExplanation: (PageSegmentExplanation) -> Unit = { expl ->
        if (viewModel != null) {
            viewModel.deleteSegmentExplanation(expl)
        } else {
            onDeleteSegmentExplanation(expl)
        }
    }

    val effectiveSegmentChatMessages: List<SegmentChatMessage> = if (viewModel != null) {
        viewModel.allSegmentChatMessages.collectAsState(initial = emptyList<SegmentChatMessage>()).value
    } else {
        emptyList()
    }
    val effectiveSegmentChatLoadingMap = viewModel?.segmentChatLoadingMap?.collectAsState()?.value ?: emptyMap()
    val effectiveSegmentChatErrorMap = viewModel?.segmentChatErrorMap?.collectAsState()?.value ?: emptyMap()

    val effectiveOnSendChatMessage: (PdfBook, Int, Int, String, String, Boolean) -> Unit = { book, page, segIdx, text, question, isAudio ->
        viewModel?.sendSegmentChatMessage(book, page, segIdx, text, question, isAudio)
    }
    val effectiveOnClearSegmentChat: (Int, Int, Int) -> Unit = { bookId, page, segIdx ->
        viewModel?.clearSegmentChatMessages(bookId, page, segIdx)
    }
    val effectiveOnDeleteChatMessage: (SegmentChatMessage) -> Unit = { msg ->
        viewModel?.deleteSegmentChatMessage(msg)
    }

    var selectedAuthorCollection by remember { mutableStateOf<AuthorCollection?>(null) }
    var authorToEdit by remember { mutableStateOf<AuthorCollection?>(null) }
    var showAuthorDialog by remember { mutableStateOf(false) }
    var authorToManageBooks by remember { mutableStateOf<AuthorCollection?>(null) }

    var selectedSubjectDiscipline by remember { mutableStateOf<SubjectDiscipline?>(null) }
    var subjectToEdit by remember { mutableStateOf<SubjectDiscipline?>(null) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var subjectToManageContent by remember { mutableStateOf<SubjectDiscipline?>(null) }

    var feedFilterMode by remember { mutableStateOf(ReelsFilterMode.ALL) }
    var booksLayoutMode by remember { mutableStateOf(ReelsBooksLayoutMode.GRID_4) }
    var activeReadingBookId by remember { mutableStateOf<Int?>(null) }
    var showBookPickerSheet by remember { mutableStateOf(false) }
    var showHighlightsNotebook by remember { mutableStateOf(false) }
    var showLegislationDialog by remember { mutableStateOf(false) }
    var showPageChatDialog by remember { mutableStateOf(false) }
    var selectedEnglishWordForAnalysis by remember { mutableStateOf<String?>(null) }
    var selectedEnglishSentenceContext by remember { mutableStateOf("") }
    var showEnglishPageVocabDialog by remember { mutableStateOf(false) }

    // Suporte Poliglota C2: Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico e Inglês
    var selectedPolyglotWordForAnalysis by remember { mutableStateOf<String?>(null) }
    var selectedPolyglotLanguage by remember { mutableStateOf<StudyLanguage?>(null) }
    var selectedPolyglotSentenceContext by remember { mutableStateOf("") }
    var showPolyglotPageVocabDialog by remember { mutableStateOf(false) }

    // Análise de Sintaxe & Gramática de Frases Complexas (8 Idiomas + Persistência Local)
    var selectedSentenceForGrammarAnalysis by remember { mutableStateOf<String?>(null) }
    var selectedGrammarLanguage by remember { mutableStateOf<StudyLanguage?>(null) }
    var showSavedSentencesStudySheet by remember { mutableStateOf(false) }
    val savedSentenceAnalyses = viewModel?.allSavedSentenceAnalyses?.collectAsState(initial = emptyList())?.value ?: emptyList()

    val organizations by (viewModel?.allOrganizations ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
    val studyAlarmSettings by (viewModel?.studyAlarmSettings ?: kotlinx.coroutines.flow.flowOf(com.example.data.alarm.StudyAlarmSettings())).collectAsState(initial = com.example.data.alarm.StudyAlarmSettings())
    val activeAlarmEvent by (viewModel?.activeAlarmEvent ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)

    var showStudyAlarmDialog by remember { mutableStateOf(false) }
    var selectedOrgForDetail by remember { mutableStateOf<PublicOrganization?>(null) }
    var showAddEditOrgDialog by remember { mutableStateOf(false) }
    var orgToEdit by remember { mutableStateOf<PublicOrganization?>(null) }

    // Itens dinâmicos do Feed:
    // Permite alternar entre todos os livros, autores, matérias, órgãos públicos ou modo misto.
    // No modo GRID_4 (padrão), exibe de 4 em 4 livros ou órgãos por slide na tela.
    val currentFeedItems: List<ReelsFeedItem> = remember(books, authors, subjects, organizations, selectedAuthorCollection, selectedSubjectDiscipline, feedFilterMode, booksLayoutMode) {
        val selectedSubj = selectedSubjectDiscipline
        val selectedAuth = selectedAuthorCollection
        val orgChunks = organizations.chunked(4)
        val orgSlides = if (orgChunks.isEmpty()) {
            listOf(ReelsFeedItem.OrganizationsGridItem(emptyList(), 0))
        } else {
            orgChunks.mapIndexed { idx, chunk -> ReelsFeedItem.OrganizationsGridItem(chunk, idx) }
        }

        if (selectedSubj != null) {
            val subAuthorIds = selectedSubj.getAuthorIds()
            val subDirectBookIds = selectedSubj.getDirectBookIds()
            val subBooks = books.filter { b ->
                subDirectBookIds.contains(b.id) || (b.authorCollectionId != null && subAuthorIds.contains(b.authorCollectionId))
            }
            val subAuthors = authors.filter { subAuthorIds.contains(it.id) }
            val feed = mutableListOf<ReelsFeedItem>()
            // Capa da matéria como slide inicial
            feed.add(ReelsFeedItem.SubjectItem(selectedSubj, subBooks, subAuthors))
            // Livros pertencentes à matéria (de 4 em 4 no modo GRID_4)
            if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) {
                subBooks.chunked(4).forEachIndexed { idx, chunk ->
                    feed.add(ReelsFeedItem.BooksGridItem(chunk, idx))
                }
            } else {
                feed.addAll(subBooks.map { ReelsFeedItem.BookItem(it) })
            }
            feed
        } else if (selectedAuth != null) {
            val authorBooks = books.filter { it.authorCollectionId == selectedAuth.id }
            if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) {
                authorBooks.chunked(4).mapIndexed { idx, chunk -> ReelsFeedItem.BooksGridItem(chunk, idx) }
            } else {
                authorBooks.map { ReelsFeedItem.BookItem(it) }
            }
        } else {
            val authorSlides = authors.map { author ->
                ReelsFeedItem.AuthorItem(
                    author = author,
                    authorBooks = books.filter { it.authorCollectionId == author.id }
                )
            }
            val subjectSlides = subjects.map { subject ->
                val subAuthorIds = subject.getAuthorIds()
                val subDirectBookIds = subject.getDirectBookIds()
                val subBooks = books.filter { b ->
                    subDirectBookIds.contains(b.id) || (b.authorCollectionId != null && subAuthorIds.contains(b.authorCollectionId))
                }
                val subAuthors = authors.filter { subAuthorIds.contains(it.id) }
                ReelsFeedItem.SubjectItem(
                    subject = subject,
                    subjectBooks = subBooks,
                    subjectAuthors = subAuthors
                )
            }

            if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) {
                val bookChunks = books.chunked(4)
                when (feedFilterMode) {
                    ReelsFilterMode.BOOKS -> {
                        bookChunks.mapIndexed { idx, chunk -> ReelsFeedItem.BooksGridItem(chunk, idx) }
                    }
                    ReelsFilterMode.AUTHORS -> {
                        if (authorSlides.isNotEmpty()) authorSlides else bookChunks.mapIndexed { idx, chunk -> ReelsFeedItem.BooksGridItem(chunk, idx) }
                    }
                    ReelsFilterMode.SUBJECTS -> {
                        if (subjectSlides.isNotEmpty()) subjectSlides else bookChunks.mapIndexed { idx, chunk -> ReelsFeedItem.BooksGridItem(chunk, idx) }
                    }
                    ReelsFilterMode.ORGANIZATIONS -> {
                        orgSlides
                    }
                    ReelsFilterMode.ALL -> {
                        if (authorSlides.isEmpty() && subjectSlides.isEmpty() && orgSlides.isEmpty()) {
                            bookChunks.mapIndexed { idx, chunk -> ReelsFeedItem.BooksGridItem(chunk, idx) }
                        } else {
                            val feed = mutableListOf<ReelsFeedItem>()
                            var authorIndex = 0
                            var subjectIndex = 0
                            var orgIndex = 0
                            for (i in bookChunks.indices) {
                                feed.add(ReelsFeedItem.BooksGridItem(bookChunks[i], i))
                                if ((i + 1) % 2 == 0) {
                                    if (subjectIndex < subjectSlides.size) {
                                        feed.add(subjectSlides[subjectIndex])
                                        subjectIndex++
                                    } else if (orgIndex < orgSlides.size) {
                                        feed.add(orgSlides[orgIndex])
                                        orgIndex++
                                    } else if (authorIndex < authorSlides.size) {
                                        feed.add(authorSlides[authorIndex])
                                        authorIndex++
                                    }
                                } else if ((i + 1) % 3 == 0) {
                                    if (orgIndex < orgSlides.size) {
                                        feed.add(orgSlides[orgIndex])
                                        orgIndex++
                                    } else if (authorIndex < authorSlides.size) {
                                        feed.add(authorSlides[authorIndex])
                                        authorIndex++
                                    }
                                }
                            }
                            while (subjectIndex < subjectSlides.size) {
                                feed.add(subjectSlides[subjectIndex])
                                subjectIndex++
                            }
                            while (orgIndex < orgSlides.size) {
                                feed.add(orgSlides[orgIndex])
                                orgIndex++
                            }
                            while (authorIndex < authorSlides.size) {
                                feed.add(authorSlides[authorIndex])
                                authorIndex++
                            }
                            feed
                        }
                    }
                }
            } else {
                when (feedFilterMode) {
                    ReelsFilterMode.BOOKS -> {
                        books.map { ReelsFeedItem.BookItem(it) }
                    }
                    ReelsFilterMode.AUTHORS -> {
                        if (authorSlides.isNotEmpty()) authorSlides else books.map { ReelsFeedItem.BookItem(it) }
                    }
                    ReelsFilterMode.SUBJECTS -> {
                        if (subjectSlides.isNotEmpty()) subjectSlides else books.map { ReelsFeedItem.BookItem(it) }
                    }
                    ReelsFilterMode.ORGANIZATIONS -> {
                        orgSlides
                    }
                    ReelsFilterMode.ALL -> {
                        if (authorSlides.isEmpty() && subjectSlides.isEmpty() && orgSlides.isEmpty()) {
                            books.map { ReelsFeedItem.BookItem(it) }
                        } else {
                            val feed = mutableListOf<ReelsFeedItem>()
                            var authorIndex = 0
                            var subjectIndex = 0
                            var orgIndex = 0
                            for (i in books.indices) {
                                feed.add(ReelsFeedItem.BookItem(books[i]))
                                if ((i + 1) % 2 == 0) {
                                    if (subjectIndex < subjectSlides.size) {
                                        feed.add(subjectSlides[subjectIndex])
                                        subjectIndex++
                                    } else if (orgIndex < orgSlides.size) {
                                        feed.add(orgSlides[orgIndex])
                                        orgIndex++
                                    } else if (authorIndex < authorSlides.size) {
                                        feed.add(authorSlides[authorIndex])
                                        authorIndex++
                                    }
                                } else if ((i + 1) % 3 == 0) {
                                    if (orgIndex < orgSlides.size) {
                                        feed.add(orgSlides[orgIndex])
                                        orgIndex++
                                    } else if (authorIndex < authorSlides.size) {
                                        feed.add(authorSlides[authorIndex])
                                        authorIndex++
                                    }
                                }
                            }
                            while (subjectIndex < subjectSlides.size) {
                                feed.add(subjectSlides[subjectIndex])
                                subjectIndex++
                            }
                            while (orgIndex < orgSlides.size) {
                                feed.add(orgSlides[orgIndex])
                                orgIndex++
                            }
                            while (authorIndex < authorSlides.size) {
                                feed.add(authorSlides[authorIndex])
                                authorIndex++
                            }
                            feed
                        }
                    }
                }
            }
        }
    }

    val initialPage = remember(initialBookIndex, booksLayoutMode, currentFeedItems.size) {
        if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) {
            (initialBookIndex / 4).coerceIn(0, (currentFeedItems.size - 1).coerceAtLeast(0))
        } else {
            initialBookIndex.coerceIn(0, (currentFeedItems.size - 1).coerceAtLeast(0))
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { currentFeedItems.size })

    LaunchedEffect(currentFeedItems.size) {
        if (pagerState.currentPage >= currentFeedItems.size && currentFeedItems.isNotEmpty()) {
            pagerState.scrollToPage(currentFeedItems.size - 1)
        }
    }

    LaunchedEffect(selectedAuthorCollection?.id) {
        if (currentFeedItems.isNotEmpty() && pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    LaunchedEffect(selectedSubjectDiscipline?.id) {
        if (currentFeedItems.isNotEmpty() && pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    val currentFeedItem = currentFeedItems.getOrNull(pagerState.currentPage)
    val currentFeedBook = when (currentFeedItem) {
        is ReelsFeedItem.BookItem -> currentFeedItem.book
        is ReelsFeedItem.BooksGridItem -> currentFeedItem.books.firstOrNull()
        else -> null
    }
    val activeReadingBook = books.firstOrNull { it.id == activeReadingBookId }
    val currentBook = activeReadingBook ?: currentFeedBook
    val isSpeakingCategory by speechManager.isSpeakingCategory.collectAsState()

    // OpenAI Summary, Dictionary, & Trilateral Page Translation state
    var showOpenAiSummaryDialog by remember { mutableStateOf(false) }
    var showOpenAiAdvancedStudySheet by remember { mutableStateOf(false) }
    var showDictionaryDialog by remember { mutableStateOf(false) }
    var showComparativeDialog by remember { mutableStateOf(false) }
    var showPageMultiTranslationDialog by remember { mutableStateOf(false) }
    var showScientificDialog by remember { mutableStateOf(false) }
    var showBooksDialog by remember { mutableStateOf(false) }
    var activeReadingLanguages by remember { mutableStateOf(mapOf<String, ReadingLanguage>()) }
    var comparativeModePages by remember { mutableStateOf(setOf<String>()) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showBookQuestionsDialog by remember { mutableStateOf(false) }
    var showGlobalQuestionsReelDialog by remember { mutableStateOf(false) }
    var focusedDictionaryTerm by remember { mutableStateOf<PageDictionaryTerm?>(null) }
    var openAiTargetPageText by remember { mutableStateOf("") }
    var showPageRestructureDialog by remember { mutableStateOf(false) }
    var pageRestructureTargetText by remember { mutableStateOf("") }
    var pageContentRevision by remember { mutableStateOf(0L) }
    var showOfflinePageDialog by remember { mutableStateOf(false) }
    var offlineTargetPageText by remember { mutableStateOf("") }
    var offlineTargetRawSentences by remember { mutableStateOf<List<String>>(emptyList()) }

    // Mudança automática de voz para o idioma de leitura:
    // Se a página possuir tradução em EN/FR/ES, a voz correspondente é selecionada automaticamente.
    // Ao virar a página para uma sem tradução (português), retorna à voz em português.
    // Ao voltar para a página traduzida, a voz no idioma escolhido é restaurada instantaneamente.
    LaunchedEffect(currentBook?.id, currentBook?.currentPage, pageTranslations, activeReadingLanguages) {
        val b = currentBook ?: return@LaunchedEffect
        val pageKey = "${b.id}_${b.currentPage}"
        val pageTrans = pageTranslations.firstOrNull {
            it.bookId == b.id && it.pageNumber == b.currentPage
        }
        val targetLang = if (pageTrans != null) {
            activeReadingLanguages[pageKey] ?: ReadingLanguage.PORTUGUESE
        } else {
            ReadingLanguage.PORTUGUESE
        }
        speechManager.setReadingLanguage(targetLang)
    }

    // Double-tap star pop animation
    var showStarAnimation by remember { mutableStateOf(false) }

    // Big page number overlay state
    var showBigPageOverlay by remember { mutableStateOf(false) }
    var lastOverlayPage by remember { mutableStateOf(currentBook?.currentPage ?: 1) }

    // Table of contents, voice selector and theme states
    var showTocSheet by remember { mutableStateOf(false) }
    var showVoiceSelectorSheet by remember { mutableStateOf(false) }
    var showJumpPageDialog by remember { mutableStateOf(false) }
    var tocSearchQuery by remember { mutableStateOf("") }
    var tocQuickPageInput by remember { mutableStateOf("") }
    val bookTocMap = remember { mutableStateMapOf<Int, List<PdfTocItem>>() }
    var reelsViewMode by remember { mutableStateOf(PdfViewMode.FOREGROUND_TEXT) }
    var openedBookIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Helper para garantir que o sumário daquele livro específico seja carregado sem misturar com outros
    fun ensureTocLoaded(targetBook: PdfBook) {
        val existing = bookTocMap[targetBook.id]
        if (existing.isNullOrEmpty()) {
            scope.launch {
                val toc = PdfRendererHelper.generateTableOfContents(context, targetBook)
                bookTocMap[targetBook.id] = toc
            }
        }
    }

    // O sumário é carregado sob demanda (ao abrir o sumário ou ao entrar no livro) para máxima velocidade
    val availableVoices by speechManager.availableVoices.collectAsState()
    val selectedVoice by speechManager.selectedVoice.collectAsState()
    val activeWordIndex by speechManager.activeWordIndex.collectAsState()
    val activeComparativeLineId by speechManager.activeComparativeLineId.collectAsState()

    var isFullscreen by remember { mutableStateOf(false) }
    var geminiSelectedTerm by remember { mutableStateOf<String?>(null) }
    var geminiSelectedContext by remember { mutableStateOf<String?>(null) }
    var showThemeMenu by remember { mutableStateOf(false) }
    val fontPrefs = remember { context.getSharedPreferences("reading_font_prefs", android.content.Context.MODE_PRIVATE) }
    var reelsFontSizeSp by androidx.compose.runtime.saveable.rememberSaveable {
        mutableFloatStateOf(fontPrefs.getFloat("global_reels_font_size_sp", 14f))
    }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    // Trigger big page overlay when page changes
    fun triggerBigPageOverlay(page: Int) {
        lastOverlayPage = page
        showBigPageOverlay = true
        scope.launch {
            delay(850)
            showBigPageOverlay = false
        }
    }

    // Notify page change when user swipes to a new book
    LaunchedEffect(pagerState) {
        var previousSettledIndex: Int? = null
        snapshotFlow { pagerState.settledPage }.collect { index ->
            if (previousSettledIndex != null && previousSettledIndex != index) {
                // Ao deslizar para outro item do feed, fecha o modo de leitura e interrompe fala anterior
                activeReadingBookId = null
                openedBookIds = emptySet()
                isFullscreen = false
                speechManager.stop()
            }
            previousSettledIndex = index
            val feedItem = currentFeedItems.getOrNull(index)
            val book = when (feedItem) {
                is ReelsFeedItem.BookItem -> feedItem.book
                is ReelsFeedItem.BooksGridItem -> feedItem.books.firstOrNull()
                else -> null
            }
            if (book != null) {
                onPageChanged(book, book.currentPage)
            }
        }
    }

    // Leitura por voz em inglês nativa das categorias ao passar pelas capas dos livros
    LaunchedEffect(pagerState.currentPage, currentBook?.id, currentBook?.category, openedBookIds, activeReadingBookId) {
        val book = currentBook ?: return@LaunchedEffect
        val isReadingMode = activeReadingBookId != null || openedBookIds.contains(book.id)

        // Se o usuário está na capa do livro e o livro possui categoria classificada:
        if (!isReadingMode && !book.category.isNullOrBlank()) {
            speechManager.stopCategorySpeech()
            delay(280)
            speechManager.speakCategoryInEnglish(book.category!!, book.subcategory)
        } else {
            speechManager.stopCategorySpeech()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.stopCategorySpeech()
        }
    }

    val isCurrentBookStarred = currentBook != null && starredPages.contains(currentBook.currentPage)

    // Phase-by-phase Back Navigation
    BackHandler(enabled = true) {
        when {
            isFullscreen -> {
                isFullscreen = false
            }
            geminiSelectedTerm != null -> {
                geminiSelectedTerm = null
                geminiSelectedContext = null
            }
            showTocSheet -> {
                showTocSheet = false
            }
            showVoiceSelectorSheet -> {
                showVoiceSelectorSheet = false
            }
            showJumpPageDialog -> {
                showJumpPageDialog = false
            }
            showThemeMenu -> {
                showThemeMenu = false
            }
            showOpenAiSummaryDialog -> {
                showOpenAiSummaryDialog = false
            }
            showOpenAiAdvancedStudySheet -> {
                showOpenAiAdvancedStudySheet = false
            }
            showDictionaryDialog -> {
                showDictionaryDialog = false
                focusedDictionaryTerm = null
            }
            showComparativeDialog -> {
                showComparativeDialog = false
            }
            showCategoryDialog -> {
                showCategoryDialog = false
            }
            showAuthorDialog -> {
                showAuthorDialog = false
                authorToEdit = null
            }
            showBookPickerSheet -> {
                showBookPickerSheet = false
            }
            showLegislationDialog -> {
                showLegislationDialog = false
            }
            selectedEnglishWordForAnalysis != null -> {
                selectedEnglishWordForAnalysis = null
            }
            showEnglishPageVocabDialog -> {
                showEnglishPageVocabDialog = false
            }
            selectedPolyglotWordForAnalysis != null -> {
                selectedPolyglotWordForAnalysis = null
            }
            showPolyglotPageVocabDialog -> {
                showPolyglotPageVocabDialog = false
            }
            selectedSentenceForGrammarAnalysis != null -> {
                selectedSentenceForGrammarAnalysis = null
            }
            showSavedSentencesStudySheet -> {
                showSavedSentencesStudySheet = false
            }
            authorToManageBooks != null -> {
                authorToManageBooks = null
            }
            activeReadingBookId != null -> {
                activeReadingBookId = null
                openedBookIds = emptySet()
                speechManager.stop()
            }
            currentBook != null && openedBookIds.contains(currentBook.id) -> {
                openedBookIds = openedBookIds - currentBook.id
                activeReadingBookId = null
                speechManager.stop()
            }
            selectedAuthorCollection != null -> {
                selectedAuthorCollection = null
            }
            feedFilterMode != ReelsFilterMode.ALL -> {
                feedFilterMode = ReelsFilterMode.ALL
            }
            reelsViewMode != PdfViewMode.FOREGROUND_TEXT -> {
                reelsViewMode = PdfViewMode.FOREGROUND_TEXT
            }
            else -> {
                onCloseReels()
            }
        }
    }

    var activeTheme by remember(readingTheme) { mutableStateOf(readingTheme) }
    LaunchedEffect(readingTheme) {
        activeTheme = readingTheme
    }

    val bgColor = when (activeTheme) {
        "CATEGORY_COLOR" -> {
            val pal = BookCategoryTaxonomy.getPaletteForCategory(currentBook?.category, currentBook?.categoryColor)
            pal.surfaceColor
        }
        "MONOCHROME_BW" -> Color(0xFF0F0F0F)
        "PAPER_SEPIA" -> Color(0xFF231D15)
        "DARK_OLED" -> Color(0xFF000000)
        "CINEMA_YELLOW" -> Color(0xFF070707)
        "CYBERPUNK_NEON" -> Color(0xFF070A14)
        "STREAMING_BOX" -> Color(0xFF101115)
        "TERMINAL_GREEN" -> Color(0xFF020703)
        else -> Color(0xFF0D0D0D)
    }

    val hudPalette = remember(activeTheme, currentBook?.category, currentBook?.categoryColor) {
        when (activeTheme) {
            "CATEGORY_COLOR" -> {
                val pal = BookCategoryTaxonomy.getPaletteForCategory(currentBook?.category, currentBook?.categoryColor)
                HudThemeColors(
                    surfaceBg = pal.surfaceColor.copy(alpha = 0.88f),
                    borderColor = pal.accentColor.copy(alpha = 0.5f),
                    accentColor = pal.accentColor,
                    textColor = Color.White
                )
            }
            "PAPER_SEPIA" -> HudThemeColors(
                surfaceBg = Color(0xFF2E2419).copy(alpha = 0.90f),
                borderColor = Color(0xFFD4AF37).copy(alpha = 0.45f),
                accentColor = Color(0xFFE8C374),
                textColor = Color(0xFFF5EACB)
            )
            "CYBERPUNK_NEON" -> HudThemeColors(
                surfaceBg = Color(0xFF0D1127).copy(alpha = 0.92f),
                borderColor = Color(0xFF00FFCC).copy(alpha = 0.55f),
                accentColor = Color(0xFF00FFCC),
                textColor = Color(0xFF00FFCC)
            )
            "TERMINAL_GREEN" -> HudThemeColors(
                surfaceBg = Color(0xFF021204).copy(alpha = 0.92f),
                borderColor = Color(0xFF00FF66).copy(alpha = 0.5f),
                accentColor = Color(0xFF00FF66),
                textColor = Color(0xFF80FF9F)
            )
            "CINEMA_YELLOW" -> HudThemeColors(
                surfaceBg = Color(0xFF141414).copy(alpha = 0.90f),
                borderColor = Color(0xFFFFD700).copy(alpha = 0.5f),
                accentColor = Color(0xFFFFD700),
                textColor = Color(0xFFFFEB3B)
            )
            "STREAMING_BOX" -> HudThemeColors(
                surfaceBg = Color(0xFF16192E).copy(alpha = 0.88f),
                borderColor = Color(0xFF818CF8).copy(alpha = 0.45f),
                accentColor = Color(0xFF818CF8),
                textColor = Color.White
            )
            "DARK_OLED" -> HudThemeColors(
                surfaceBg = Color(0xFF0A0A0A).copy(alpha = 0.94f),
                borderColor = Color.White.copy(alpha = 0.28f),
                accentColor = Color(0xFFE2E8F0),
                textColor = Color.White
            )
            else -> HudThemeColors(
                surfaceBg = Color.Black.copy(alpha = 0.70f),
                borderColor = Color(0xFFC084FC).copy(alpha = 0.35f),
                accentColor = Color(0xFFC084FC),
                textColor = Color.White
            )
        }
    }

    val isReadingAnyBook = activeReadingBook != null || (currentBook != null && openedBookIds.contains(currentBook.id))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (currentFeedItems.isEmpty() && selectedAuthorCollection != null) {
            val emptyAuthor = selectedAuthorCollection!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.2f),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Coleção de ${emptyAuthor.name}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta coleção ainda não possui livros adicionados.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { authorToManageBooks = emptyAuthor },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Livros a esta Coleção", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { selectedAuthorCollection = null }) {
                        Text("← Voltar para todos os livros", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        } else if (activeReadingBook != null) {
            val book = activeReadingBook
            val pageSummary = openAiSummaries.firstOrNull {
                it.bookId == book.id && it.pageNumber == book.currentPage
            }
            val pageDictionaryTerms = dictionaryTerms.filter {
                it.bookId == book.id && it.pageNumber == book.currentPage
            }
            val pageTranslation = pageTranslations.firstOrNull {
                it.bookId == book.id && it.pageNumber == book.currentPage
            }
            val pageKey = "${book.id}_${book.currentPage}"
            val activeReadingLanguage = activeReadingLanguages[pageKey] ?: ReadingLanguage.PORTUGUESE
            val isComparativeMode = comparativeModePages.contains(pageKey)
            ReelBookPageCard(
                book = book,
                readingTheme = activeTheme,
                isFullscreen = isFullscreen,
                viewMode = reelsViewMode,
                contentRevision = pageContentRevision,
                isShowingCoverFirst = false,
                isSpeaking = isSpeaking,
                isSpeakingCategory = false,
                onSpeakCategory = null,
                activeSentenceIndex = activeSentenceIndex,
                activeWordIndex = activeWordIndex,
                activeComparativeLineId = activeComparativeLineId,
                isClassifyingCategory = isClassifyingBook && (currentBook?.id == book.id),
                onOpenCategoryDetails = { showCategoryDialog = true },
                pageSummary = pageSummary,
                pageDictionaryTerms = pageDictionaryTerms,
                onSaveDictionaryTerms = onSaveDictionaryTerms,
                pageTranslation = pageTranslation,
                activeReadingLanguage = activeReadingLanguage,
                isComparativeMode = isComparativeMode,
                onToggleComparativeMode = {
                    comparativeModePages = if (isComparativeMode) {
                        comparativeModePages - pageKey
                    } else {
                        comparativeModePages + pageKey
                    }
                },
                speechManager = speechManager,
                onToggleSpeech = onToggleSpeech,
                onSelectReadingLanguage = { lang ->
                    activeReadingLanguages = activeReadingLanguages + (pageKey to lang)
                    speechManager.setReadingLanguage(lang)
                },
                onOpenMultiTranslation = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showPageMultiTranslationDialog = true
                    }
                },
                geminiNotes = geminiNotes.filter { it.projectId == book.id },
                pageSegmentExplanations = effectiveSegmentExplanations.filter { it.bookId == book.id && it.pageNumber == book.currentPage },
                isGeneratingSegmentExplanation = effectiveIsGeneratingExplanation,
                segmentExplanationError = effectiveExplanationError,
                onRequestSegmentExplanation = effectiveOnRequestExplanation,
                onDeleteSegmentExplanation = effectiveOnDeleteExplanation,
                segmentChatMessages = effectiveSegmentChatMessages.filter { it.bookId == book.id && it.pageNumber == book.currentPage },
                segmentChatLoadingMap = effectiveSegmentChatLoadingMap,
                segmentChatErrorMap = effectiveSegmentChatErrorMap,
                onSendSegmentChatMessage = effectiveOnSendChatMessage,
                onClearSegmentChat = effectiveOnClearSegmentChat,
                onDeleteSegmentChatMessage = effectiveOnDeleteChatMessage,
                pageQuestions = bookQuestions.filter { it.bookId == book.id && it.startPage <= book.currentPage && it.endPage >= book.currentPage },
                onAnswerQuestion = onAnswerQuestion,
                onResetQuestionAnswer = onResetQuestionAnswer,
                onDeleteQuestion = onDeleteQuestion,
                onOpenQuestionsDialog = {
                    showBookQuestionsDialog = true
                },
                onOpenSummary = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showOpenAiSummaryDialog = true
                    }
                },
                onOpenOpenAiStudy = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showOpenAiAdvancedStudySheet = true
                    }
                },
                onOpenDictionary = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        focusedDictionaryTerm = null
                        showDictionaryDialog = true
                    }
                },
                onOpenDictionaryTerm = { term ->
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        focusedDictionaryTerm = term
                        showDictionaryDialog = true
                    }
                },
                onWordClick = { sIdx, wIdx, sents ->
                    onWordClick(book, sIdx, wIdx, sents)
                },
                onWordLongPress = { clickedWord, sentence ->
                    val detected = StudyLanguage.detectFromText(clickedWord.ifBlank { sentence })
                    selectedPolyglotWordForAnalysis = clickedWord
                    selectedPolyglotLanguage = detected
                    selectedPolyglotSentenceContext = sentence
                },
                onAnalyzeSentence = { sentence ->
                    val detected = StudyLanguage.detectFromText(sentence)
                    selectedSentenceForGrammarAnalysis = sentence
                    selectedGrammarLanguage = detected
                },
                onOpenJumpDialog = {
                    showJumpPageDialog = true
                },
                onOpenToc = {
                    ensureTocLoaded(book)
                    showTocSheet = true
                },
                onEnterBook = {
                    activeReadingBookId = book.id
                    openedBookIds = openedBookIds + book.id
                    onPageChanged(book, book.currentPage)
                    ensureTocLoaded(book)
                },
                onReturnToCover = {
                    activeReadingBookId = null
                    openedBookIds = openedBookIds - book.id
                    speechManager.stop()
                },
                onSwitchViewMode = { newMode ->
                    reelsViewMode = newMode
                },
                textFontSizeSp = reelsFontSizeSp,
                onDoubleTap = {
                    showStarAnimation = true
                    onToggleStarPage(book.currentPage)
                    scope.launch {
                        delay(900)
                        showStarAnimation = false
                    }
                },
                onSingleTap = {},
                onSwipeLeft = {
                    if (book.currentPage < book.totalPages) {
                        val next = book.currentPage + 1
                        onPageChanged(book, next)
                        triggerBigPageOverlay(next)
                    }
                },
                onSwipeRight = {
                    if (book.currentPage > 1) {
                        val prev = book.currentPage - 1
                        onPageChanged(book, prev)
                        triggerBigPageOverlay(prev)
                    }
                },
                onSentenceGeminiClick = { term, sentence ->
                    geminiSelectedTerm = term
                    geminiSelectedContext = sentence
                },
                onOpenOfflineTools = {
                    scope.launch {
                        val (_, extracted) = PdfRendererHelper.extractParagraphsAndSentencesFromPage(
                            context, book, book.currentPage
                        )
                        offlineTargetRawSentences = extracted
                        offlineTargetPageText = extracted.joinToString(" ").trim().ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showOfflinePageDialog = true
                    }
                }
            )
        } else {
            VerticalPager(
                state = pagerState,
                key = { pageIdx -> currentFeedItems.getOrNull(pageIdx)?.key ?: pageIdx.toString() },
                userScrollEnabled = !isReadingAnyBook,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                when (val feedItem = currentFeedItems.getOrNull(pageIndex)) {
                    is ReelsFeedItem.BooksGridItem -> {
                        BooksGridReelsSlide(
                            books = feedItem.books,
                            readingTheme = activeTheme,
                            onEnterBook = { selectedBook ->
                                activeReadingBookId = selectedBook.id
                                openedBookIds = openedBookIds + selectedBook.id
                                onPageChanged(selectedBook, selectedBook.currentPage)
                                ensureTocLoaded(selectedBook)
                            }
                        )
                    }
                    is ReelsFeedItem.AuthorItem -> {
                        AuthorReelsSlide(
                            author = feedItem.author,
                            authorBooks = feedItem.authorBooks,
                            readingTheme = activeTheme,
                            onOpenCollection = { auth ->
                                selectedAuthorCollection = auth
                            },
                            onManageBooks = { auth ->
                                authorToManageBooks = auth
                            },
                            onEditAuthor = { auth ->
                                authorToEdit = auth
                                showAuthorDialog = true
                            }
                        )
                    }
                    is ReelsFeedItem.SubjectItem -> {
                        SubjectReelsSlide(
                            subject = feedItem.subject,
                            subjectBooks = feedItem.subjectBooks,
                            subjectAuthors = feedItem.subjectAuthors,
                            readingTheme = activeTheme,
                            onOpenDiscipline = { subj ->
                                selectedSubjectDiscipline = subj
                            },
                            onManageContent = { subj ->
                                subjectToManageContent = subj
                            },
                            onAddDirectBook = { subj ->
                                subjectToManageContent = subj
                            },
                            onEditSubject = { subj ->
                                subjectToEdit = subj
                                showSubjectDialog = true
                            }
                        )
                    }
                    is ReelsFeedItem.OrganizationsGridItem -> {
                        PublicOrganizationsGridReelsSlide(
                            organizations = feedItem.organizations,
                            readingTheme = activeTheme,
                            onSelectOrganization = { org ->
                                selectedOrgForDetail = org
                            },
                            onAddNewOrganization = {
                                orgToEdit = null
                                showAddEditOrgDialog = true
                            }
                        )
                    }
                    is ReelsFeedItem.BookItem -> {
                        val book = feedItem.book
            val pageSummary = openAiSummaries.firstOrNull {
                it.bookId == book.id && it.pageNumber == book.currentPage
            }
            val pageDictionaryTerms = dictionaryTerms.filter {
                it.bookId == book.id && it.pageNumber == book.currentPage
            }
            val pageTranslation = pageTranslations.firstOrNull {
                it.bookId == book.id && it.pageNumber == book.currentPage
            }
            val pageKey = "${book.id}_${book.currentPage}"
            val activeReadingLanguage = activeReadingLanguages[pageKey] ?: ReadingLanguage.PORTUGUESE
            val isComparativeMode = comparativeModePages.contains(pageKey)
            val isBookInReadingMode = openedBookIds.contains(book.id)
            val isCurrentPage = pagerState.currentPage == pageIndex
            ReelBookPageCard(
                book = book,
                readingTheme = activeTheme,
                isFullscreen = isFullscreen,
                viewMode = reelsViewMode,
                contentRevision = pageContentRevision,
                isShowingCoverFirst = !isBookInReadingMode,
                isSpeaking = isSpeaking && isCurrentPage,
                isSpeakingCategory = isSpeakingCategory && isCurrentPage,
                onSpeakCategory = {
                    if (!book.category.isNullOrBlank()) {
                        speechManager.speakCategoryInEnglish(book.category!!, book.subcategory)
                    }
                },
                activeSentenceIndex = activeSentenceIndex,
                activeWordIndex = activeWordIndex,
                activeComparativeLineId = activeComparativeLineId,
                isClassifyingCategory = isClassifyingBook && (currentBook?.id == book.id),
                onOpenCategoryDetails = { showCategoryDialog = true },
                pageSummary = pageSummary,
                pageDictionaryTerms = pageDictionaryTerms,
                onSaveDictionaryTerms = onSaveDictionaryTerms,
                pageTranslation = pageTranslation,
                activeReadingLanguage = activeReadingLanguage,
                isComparativeMode = isComparativeMode,
                onToggleComparativeMode = {
                    comparativeModePages = if (isComparativeMode) {
                        comparativeModePages - pageKey
                    } else {
                        comparativeModePages + pageKey
                    }
                },
                speechManager = speechManager,
                onToggleSpeech = onToggleSpeech,
                onSelectReadingLanguage = { lang ->
                    activeReadingLanguages = activeReadingLanguages + (pageKey to lang)
                    speechManager.setReadingLanguage(lang)
                },
                onOpenMultiTranslation = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showPageMultiTranslationDialog = true
                    }
                },
                geminiNotes = geminiNotes.filter { it.projectId == book.id },
                pageSegmentExplanations = effectiveSegmentExplanations.filter { it.bookId == book.id && it.pageNumber == book.currentPage },
                isGeneratingSegmentExplanation = effectiveIsGeneratingExplanation,
                segmentExplanationError = effectiveExplanationError,
                onRequestSegmentExplanation = effectiveOnRequestExplanation,
                onDeleteSegmentExplanation = effectiveOnDeleteExplanation,
                segmentChatMessages = effectiveSegmentChatMessages.filter { it.bookId == book.id && it.pageNumber == book.currentPage },
                segmentChatLoadingMap = effectiveSegmentChatLoadingMap,
                segmentChatErrorMap = effectiveSegmentChatErrorMap,
                onSendSegmentChatMessage = effectiveOnSendChatMessage,
                onClearSegmentChat = effectiveOnClearSegmentChat,
                onDeleteSegmentChatMessage = effectiveOnDeleteChatMessage,
                pageQuestions = bookQuestions.filter { it.bookId == book.id && it.startPage <= book.currentPage && it.endPage >= book.currentPage },
                onAnswerQuestion = onAnswerQuestion,
                onResetQuestionAnswer = onResetQuestionAnswer,
                onDeleteQuestion = onDeleteQuestion,
                onOpenQuestionsDialog = {
                    showBookQuestionsDialog = true
                },
                onOpenSummary = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showOpenAiSummaryDialog = true
                    }
                },
                onOpenOpenAiStudy = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showOpenAiAdvancedStudySheet = true
                    }
                },
                onOpenDictionary = {
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        focusedDictionaryTerm = null
                        showDictionaryDialog = true
                    }
                },
                onOpenDictionaryTerm = { term ->
                    scope.launch {
                        val text = PdfRendererHelper.extractTextFromPage(context, book, book.currentPage).joinToString("\n\n")
                        openAiTargetPageText = text.ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        focusedDictionaryTerm = term
                        showDictionaryDialog = true
                    }
                },
                onWordClick = { sIdx, wIdx, sents ->
                    onWordClick(book, sIdx, wIdx, sents)
                },
                onWordLongPress = { clickedWord, sentence ->
                    val detected = StudyLanguage.detectFromText(clickedWord.ifBlank { sentence })
                    selectedPolyglotWordForAnalysis = clickedWord
                    selectedPolyglotLanguage = detected
                    selectedPolyglotSentenceContext = sentence
                },
                onAnalyzeSentence = { sentence ->
                    val detected = StudyLanguage.detectFromText(sentence)
                    selectedSentenceForGrammarAnalysis = sentence
                    selectedGrammarLanguage = detected
                },
                onOpenJumpDialog = {
                    showJumpPageDialog = true
                },
                onOpenToc = {
                    ensureTocLoaded(book)
                    showTocSheet = true
                },
                onEnterBook = {
                    activeReadingBookId = book.id
                    openedBookIds = openedBookIds + book.id
                    onPageChanged(book, book.currentPage)
                    ensureTocLoaded(book)
                },
                onReturnToCover = {
                    activeReadingBookId = null
                    openedBookIds = openedBookIds - book.id
                    speechManager.stop()
                },
                onSwitchViewMode = { newMode ->
                    reelsViewMode = newMode
                },
                textFontSizeSp = reelsFontSizeSp,
                onDoubleTap = {
                    showStarAnimation = true
                    onToggleStarPage(book.currentPage)
                    scope.launch {
                        delay(900)
                        showStarAnimation = false
                    }
                },
                onSingleTap = {
                    if (!isBookInReadingMode) {
                        onToggleSpeech()
                    }
                    // No modo leitura, o toque simples na tela NÃO alterna para tela cheia.
                    // A entrada no modo tela cheia só ocorre exclusivamente pelo botão "Cheia".
                },
                onSwipeLeft = {
                    // Arrastar para a esquerda: avança a página do livro atual
                    if (book.currentPage < book.totalPages) {
                        val next = book.currentPage + 1
                        onPageChanged(book, next)
                        triggerBigPageOverlay(next)
                    }
                },
                onSwipeRight = {
                    // Arrastar para a direita: volta a página do livro atual
                    if (book.currentPage > 1) {
                        val prev = book.currentPage - 1
                        onPageChanged(book, prev)
                        triggerBigPageOverlay(prev)
                    }
                },
                onSentenceGeminiClick = { term, sentence ->
                    geminiSelectedTerm = term
                    geminiSelectedContext = sentence
                },
                onOpenOfflineTools = {
                    scope.launch {
                        val (_, extracted) = PdfRendererHelper.extractParagraphsAndSentencesFromPage(
                            context, book, book.currentPage
                        )
                        offlineTargetRawSentences = extracted
                        offlineTargetPageText = extracted.joinToString(" ").trim().ifBlank {
                            PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(book.currentPage - 1) ?: book.title
                        }
                        showOfflinePageDialog = true
                    }
                }
            )
                    }
                    null -> Unit
                }
            }
        }

        // Top Reels Header: Todos os botões integrados na barra superior sem títulos de livros/autores e sem botões flutuando sobre as capas
        val isReading = activeReadingBook != null || (currentBook != null && openedBookIds.contains(currentBook.id))
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            // Botão Voltar / Fechar e indicador de voz (sempre à esquerda)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isReading) {
                                activeReadingBookId = null
                                openedBookIds = emptySet()
                                speechManager.stop()
                            } else if (selectedSubjectDiscipline != null) {
                                selectedSubjectDiscipline = null
                            } else if (selectedAuthorCollection != null) {
                                selectedAuthorCollection = null
                            } else if (feedFilterMode != ReelsFilterMode.ALL) {
                                feedFilterMode = ReelsFilterMode.ALL
                            } else {
                                onCloseReels()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isReading || selectedAuthorCollection != null || selectedSubjectDiscipline != null || feedFilterMode != ReelsFilterMode.ALL) Icons.Default.ArrowBack else Icons.Default.Close,
                            contentDescription = if (isReading) "Voltar para a Capa" else if (selectedSubjectDiscipline != null) "Voltar para todas as matérias" else if (selectedAuthorCollection != null) "Voltar para todos os livros" else if (feedFilterMode != ReelsFilterMode.ALL) "Voltar para todos" else "Fechar Reels",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (isSpeaking) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF8E2DE2),
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Voz ativa",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // Barra de controles no topo
            if (!isReading) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
                ) {
                    // 🏛️ Legislação do Planalto & Vade Mecum (Minimalista & Discreto)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFCD34D).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showLegislationDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Portal de Legislação",
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "⚖️ Leis",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    // ⏰ Alarme de Estudo Periódico
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (studyAlarmSettings.isEnabled) Color(0xFFFF2E97).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (studyAlarmSettings.isEnabled) Color(0xFFFF2CF1) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showStudyAlarmDialog = true }
                            .testTag("study_alarm_top_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Alarme de Estudo",
                                tint = if (studyAlarmSettings.isEnabled) Color.White else Color(0xFFFF2E97),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (studyAlarmSettings.isEnabled) "⏰ ${studyAlarmSettings.intervalMinutes}m" else "⏰ Alarme",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Toggle de Visualização: 4 Livros (Grade 2x2) vs 1 Livro (Capa Cheia)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) Color(0xFF7C3AED) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                booksLayoutMode = if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) {
                                    ReelsBooksLayoutMode.SINGLE
                                } else {
                                    ReelsBooksLayoutMode.GRID_4
                                }
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) Icons.Default.GridView else Icons.Default.ViewAgenda,
                                contentDescription = "Modo de Exibição",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (booksLayoutMode == ReelsBooksLayoutMode.GRID_4) "4 Livros" else "1 Livro",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 1. Todos
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (feedFilterMode == ReelsFilterMode.ALL && selectedAuthorCollection == null) Color(0xFF6366F1) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (feedFilterMode == ReelsFilterMode.ALL && selectedAuthorCollection == null) Color(0xFF818CF8) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                feedFilterMode = ReelsFilterMode.ALL
                                selectedAuthorCollection = null
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                    ) {
                        Text(
                            text = "Todos",
                            fontSize = 11.sp,
                            fontWeight = if (feedFilterMode == ReelsFilterMode.ALL && selectedAuthorCollection == null) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // 2. Livros
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (feedFilterMode == ReelsFilterMode.BOOKS && selectedAuthorCollection == null) Color(0xFF6366F1) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (feedFilterMode == ReelsFilterMode.BOOKS && selectedAuthorCollection == null) Color(0xFF818CF8) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                feedFilterMode = ReelsFilterMode.BOOKS
                                selectedAuthorCollection = null
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                    ) {
                        Text(
                            text = "Livros",
                            fontSize = 11.sp,
                            fontWeight = if (feedFilterMode == ReelsFilterMode.BOOKS && selectedAuthorCollection == null) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // 3. Autores
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (feedFilterMode == ReelsFilterMode.AUTHORS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) Color(0xFF6366F1) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (feedFilterMode == ReelsFilterMode.AUTHORS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) Color(0xFF818CF8) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                feedFilterMode = ReelsFilterMode.AUTHORS
                                selectedAuthorCollection = null
                                selectedSubjectDiscipline = null
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                    ) {
                        Text(
                            text = "Autores",
                            fontSize = 11.sp,
                            fontWeight = if (feedFilterMode == ReelsFilterMode.AUTHORS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // 4. Matérias
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (feedFilterMode == ReelsFilterMode.SUBJECTS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) Color(0xFF1E3A8A) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (feedFilterMode == ReelsFilterMode.SUBJECTS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                feedFilterMode = ReelsFilterMode.SUBJECTS
                                selectedAuthorCollection = null
                                selectedSubjectDiscipline = null
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                    ) {
                        Text(
                            text = "Matérias",
                            fontSize = 11.sp,
                            fontWeight = if (feedFilterMode == ReelsFilterMode.SUBJECTS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // 5. Órgãos / Editais
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (feedFilterMode == ReelsFilterMode.ORGANIZATIONS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) Color(0xFF0098DF) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (feedFilterMode == ReelsFilterMode.ORGANIZATIONS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) Color(0xFF0EF3FF) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                feedFilterMode = ReelsFilterMode.ORGANIZATIONS
                                selectedAuthorCollection = null
                                selectedSubjectDiscipline = null
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                            .testTag("filter_organizations_chip")
                    ) {
                        Text(
                            text = "Órgãos",
                            fontSize = 11.sp,
                            fontWeight = if (feedFilterMode == ReelsFilterMode.ORGANIZATIONS && selectedAuthorCollection == null && selectedSubjectDiscipline == null) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // 6. Escolher
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showBookPickerSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Escolher",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Escolher",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 6. Matéria ativa ou Botão + Matéria
                    if (selectedSubjectDiscipline != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    subjectToManageContent = selectedSubjectDiscipline
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Gerenciar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    subjectToEdit = null
                                    showSubjectDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Nova Matéria",
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "+ Matéria",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 7. + Autor (ou + Livros se em coleção de autor)
                    if (selectedAuthorCollection != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    authorToManageBooks = selectedAuthorCollection
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "+ Livros",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4F46E5).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    authorToEdit = null
                                    showAuthorDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Novo Autor",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "+ Autor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 6. Citações & Marcações (Highlights Notebook)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showHighlightsNotebook = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Caderno de Citações e Marcações",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Citações",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Modo leitura ativa: controles sem título do livro
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End)
                ) {
                    // 1. Pular Página
                    if (currentBook != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showJumpPageDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Pular Página",
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "p.${currentBook.currentPage}/${currentBook.totalPages}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 2. Modo de Leitura
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF8E2DE2).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                reelsViewMode = when (reelsViewMode) {
                                    PdfViewMode.FOREGROUND_TEXT -> PdfViewMode.COVER_ESSAY
                                    PdfViewMode.COVER_ESSAY -> PdfViewMode.FULL_PAGE_SHEET
                                    PdfViewMode.FULL_PAGE_SHEET -> PdfViewMode.FOREGROUND_TEXT
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = when (reelsViewMode) {
                                    PdfViewMode.COVER_ESSAY -> Icons.Default.Visibility
                                    PdfViewMode.FULL_PAGE_SHEET -> Icons.Default.MenuBook
                                    PdfViewMode.FOREGROUND_TEXT -> Icons.Default.Subject
                                },
                                contentDescription = "Modo de Leitura",
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = when (reelsViewMode) {
                                    PdfViewMode.COVER_ESSAY -> "Ensaio"
                                    PdfViewMode.FULL_PAGE_SHEET -> "Folha"
                                    PdfViewMode.FOREGROUND_TEXT -> "Texto"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 3. Tamanho da Fonte
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4F46E5).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showFontSizeDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Tamanho da Fonte",
                                tint = Color(0xFFC7D2FE),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${reelsFontSizeSp.toInt()}sp",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 🏛️ Legislação do Planalto & Vade Mecum
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showLegislationDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Legislação Planalto",
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Legislação",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 4. Reestruturar / Restaurar Página com OpenAI (OCR, caracteres, pontuação com fidelidade 100%)
                    if (currentBook != null) {
                        val isPageCustomized = remember(currentBook.id, currentBook.currentPage, pageContentRevision) {
                            PdfRendererHelper.hasCustomPageText(context, currentBook.id, currentBook.currentPage)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPageCustomized) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF10A37F).copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPageCustomized) Color(0xFF34D399) else Color(0xFF10A37F)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        pageRestructureTargetText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showPageRestructureDialog = true
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPageCustomized) Icons.Default.Check else Icons.Default.AutoAwesome,
                                    contentDescription = "Restaurar Página com IA",
                                    tint = if (isPageCustomized) Color(0xFF34D399) else Color(0xFFA7F3D0),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isPageCustomized) "Restaurado" else "Restaurar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 5. Citações & Marcações (Highlights Notebook)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showHighlightsNotebook = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Citações",
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Citações",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 6. Categoria e Cores
                    if (currentBook != null) {
                        val catPalette = BookCategoryTaxonomy.getPaletteForCategory(currentBook.category, currentBook.categoryColor)
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, catPalette.accentColor),
                            modifier = Modifier.size(30.dp)
                        ) {
                            IconButton(onClick = { showCategoryDialog = true }, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Categoria & Cores do Livro",
                                    tint = catPalette.accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // 5. Troca de Tema
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.size(30.dp)
                        ) {
                            IconButton(onClick = { showThemeMenu = true }, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = Icons.Default.FormatColorFill,
                                    contentDescription = "Tema",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            val catPalette = BookCategoryTaxonomy.getPaletteForCategory(currentBook?.category, currentBook?.categoryColor)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = catPalette.accentColor,
                                            modifier = Modifier.size(14.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "🎨 Modo de Cores da Categoria",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = currentBook?.category ?: "Classificação IA",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "CATEGORY_COLOR"
                                    onThemeChange("CATEGORY_COLOR")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚪ Preto e Branco Alto Contraste") },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "MONOCHROME_BW"
                                    onThemeChange("MONOCHROME_BW")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📜 Papel Sépiado E-Ink") },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "PAPER_SEPIA"
                                    onThemeChange("PAPER_SEPIA")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚫ Dark OLED Profundo") },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "DARK_OLED"
                                    onThemeChange("DARK_OLED")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🎬 Legenda Cinema Amarelo (SRT)") },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "CINEMA_YELLOW"
                                    onThemeChange("CINEMA_YELLOW")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚡ Cyberpunk Neon Subtitle") },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "CYBERPUNK_NEON"
                                    onThemeChange("CYBERPUNK_NEON")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📺 Closed Captions Translúcido") },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "STREAMING_BOX"
                                    onThemeChange("STREAMING_BOX")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📟 Terminal Matrix Retro") },
                                onClick = {
                                    showThemeMenu = false
                                    activeTheme = "TERMINAL_GREEN"
                                    onThemeChange("TERMINAL_GREEN")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

        // Right Side Floating Action HUD (Modern Compact Dock: Play, Page, Voice, Mode, Gemini, Speed, Star)
        // Aparece SOMENTE quando o usuário entra na leitura do livro após "Toque para Ler" e não está em tela cheia
        AnimatedVisibility(
            visible = isReading && !isFullscreen,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .statusBarsPadding()
                .padding(top = 58.dp)
                .navigationBarsPadding()
                .padding(bottom = 10.dp, end = 3.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = hudPalette.surfaceBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, hudPalette.borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 2.5.dp, vertical = 4.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                // Play / Pause Voice Action
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSpeaking) Color(0xFF8E2DE2) else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { onToggleSpeech() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Ouvir",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isSpeaking) "Ouvindo" else "Ouvir",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Voice Selector Action
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { showVoiceSelectorSheet = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Trocar Voz",
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Voz",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Mode Toggle Action (1º Plano vs Capa & Ensaio vs Folha PDF)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF8E2DE2).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                reelsViewMode = when (reelsViewMode) {
                                    PdfViewMode.FOREGROUND_TEXT -> PdfViewMode.COVER_ESSAY
                                    PdfViewMode.COVER_ESSAY -> PdfViewMode.FULL_PAGE_SHEET
                                    PdfViewMode.FULL_PAGE_SHEET -> PdfViewMode.FOREGROUND_TEXT
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (reelsViewMode) {
                                    PdfViewMode.COVER_ESSAY -> Icons.Default.Visibility
                                    PdfViewMode.FULL_PAGE_SHEET -> Icons.Default.MenuBook
                                    PdfViewMode.FOREGROUND_TEXT -> Icons.Default.Subject
                                },
                                contentDescription = "Alternar Modo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = when (reelsViewMode) {
                            PdfViewMode.COVER_ESSAY -> "Ensaio"
                            PdfViewMode.FULL_PAGE_SHEET -> "Folha"
                            PdfViewMode.FOREGROUND_TEXT -> "Texto"
                        },
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // FONT SIZE Action (Ajustar tamanho da fonte na leitura)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4F46E5).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { showFontSizeDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Tamanho da Fonte",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Fonte",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // OPEN AI Deep Summary & Voice Action (with real-time summary badge indicator)
                val currentSummary = currentBook?.let { b ->
                    openAiSummaries.firstOrNull { it.bookId == b.id && it.pageNumber == b.currentPage }
                }
                val hasCurrentSummary = currentSummary != null

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasCurrentSummary) Color(0xFF10A37F) else Color.White.copy(alpha = 0.15f),
                        border = if (hasCurrentSummary) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00DC82)) else null,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        openAiTargetPageText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showOpenAiSummaryDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = if (hasCurrentSummary) "Resumo Salvo" else "Resumo IA",
                                tint = if (hasCurrentSummary) Color.Black else Color(0xFF00DC82),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "RESUMO",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCurrentSummary) Color(0xFF00DC82) else Color.White
                    )
                }

                // UNIVERSAL DICTIONARY Action (OpenAI powered: difficult/legal/compound terms, ES/FR/EN versions with examples)
                val currentBookPageTerms = currentBook?.let { b ->
                    dictionaryTerms.filter { it.bookId == b.id && it.pageNumber == b.currentPage }
                } ?: emptyList()
                val hasPageTerms = currentBookPageTerms.isNotEmpty()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasPageTerms) Color(0xFF0284C7) else Color.White.copy(alpha = 0.15f),
                        border = if (hasPageTerms) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        openAiTargetPageText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        focusedDictionaryTerm = null
                                        showDictionaryDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = if (hasPageTerms) "Dicionário (${currentBookPageTerms.size})" else "Dicionário",
                                tint = if (hasPageTerms) Color.White else Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = if (hasPageTerms) "${currentBookPageTerms.size} Ter." else "Dicionário",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPageTerms) Color(0xFF38BDF8) else Color.White
                    )
                }

                // 🧠 10 RECURSOS AVANÇADOS OPENAI (Quiz, Podcast, Pronúncia, Audiograma, Frases de Contexto, Falsos Cognatos, Mapa Semântico)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10A37F).copy(alpha = 0.30f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10A37F)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        openAiTargetPageText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showOpenAiAdvancedStudySheet = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Estudo IA (10 Melhorias OpenAI)",
                                tint = Color(0xFF10A37F),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = "ESTUDO IA",
                        fontSize = 6.6.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10A37F)
                    )
                }

                // IDIOMAS / COMPARATIVO Action (Disponível para qualquer livro e página: Inglês, Francês, Espanhol, IPA, 16 tempos verbais)
                val currentBookComparativeTerms = currentBook?.let { b ->
                    comparativeTerms.filter { it.bookId == b.id && it.pageNumber == b.currentPage }
                } ?: emptyList()
                val hasComparativeTerms = currentBookComparativeTerms.isNotEmpty()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasComparativeTerms) Color(0xFF7C3AED) else Color.White.copy(alpha = 0.15f),
                        border = if (hasComparativeTerms) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA78BFA)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        openAiTargetPageText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showComparativeDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = if (hasComparativeTerms) "Idiomas (${currentBookComparativeTerms.size})" else "Idiomas",
                                tint = if (hasComparativeTerms) Color.White else Color(0xFFA78BFA),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = if (hasComparativeTerms) "${currentBookComparativeTerms.size} Idio." else "Idiomas",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasComparativeTerms) Color(0xFFA78BFA) else Color.White
                    )
                }

                // 🌍 POLIGLOTA C2 ACTION (Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico, Inglês)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFA5B4FC)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        openAiTargetPageText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showPolyglotPageVocabDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Spellcheck,
                                contentDescription = "Poliglota C2",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Poliglota",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC)
                    )
                }

                // 🌲 SINTAXE & GRAMÁTICA POLIGLOTA ACTION (Caderno de Frases Salvas e Estudo de Regras)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.30f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF34D399)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                showSavedSentencesStudySheet = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = "Caderno de Sintaxe & Gramática",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Sintaxe",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }

                // TRADUÇÃO TRILATERAL Action (Traduz a página para Inglês, Francês e Espanhol e salva 100% offline)
                val currentBookPageTranslation = currentBook?.let { b ->
                    pageTranslations.firstOrNull { it.bookId == b.id && it.pageNumber == b.currentPage }
                }
                val hasPageTranslation = currentBookPageTranslation != null

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasPageTranslation) Color(0xFF059669) else Color.White.copy(alpha = 0.15f),
                        border = if (hasPageTranslation) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF34D399)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        openAiTargetPageText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showPageMultiTranslationDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = if (hasPageTranslation) "Tradução Salva (3 Idiomas)" else "Traduzir Página",
                                tint = if (hasPageTranslation) Color.White else Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = if (hasPageTranslation) "3 Idiomas" else "Traduzir",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPageTranslation) Color(0xFF34D399) else Color.White
                    )
                }

                // QUESTION ACTION (FGV, CEBRASPE, FCC, IADES, 100% Offline)
                val currentBookQuestions = currentBook?.let { b ->
                    bookQuestions.filter { it.bookId == b.id }
                } ?: emptyList()
                val currentBookPageQuestions = currentBook?.let { b ->
                    currentBookQuestions.filter { it.startPage <= b.currentPage && it.endPage >= b.currentPage }
                } ?: emptyList()
                val hasPageQuestions = currentBookPageQuestions.isNotEmpty()
                val hasAnyBookQuestions = currentBookQuestions.isNotEmpty()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasPageQuestions) Color(0xFFD97706) else if (hasAnyBookQuestions) Color(0xFFD97706).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                        border = if (hasPageQuestions) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                showBookQuestionsDialog = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = if (hasPageQuestions) "Question (${currentBookPageQuestions.size})" else "Question",
                                tint = if (hasPageQuestions) Color.White else Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = if (hasPageQuestions) "${currentBookPageQuestions.size} Qs" else "Question",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPageQuestions) Color(0xFFFBBF24) else Color.White
                    )
                }

                // BANCO GLOBAL DE QUESTÕES • REELS (Renomeado exclusivamente para "Quiz", super compacto)
                val totalGlobalQuestions = bookQuestions.size
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (totalGlobalQuestions > 0) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (totalGlobalQuestions > 0) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                showGlobalQuestionsReelDialog = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Quiz",
                                tint = if (totalGlobalQuestions > 0) Color(0xFFDDD6FE) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Quiz",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalGlobalQuestions > 0) Color(0xFFDDD6FE) else Color.White
                    )
                }

                // 💬 CHAT IA / TUTOR INTELIGENTE DA PÁGINA (Com Voz e Texto)
                val currentPageChatMsgs = currentBook?.let { b ->
                    effectiveSegmentChatMessages.filter { it.bookId == b.id && it.pageNumber == b.currentPage }
                } ?: emptyList()
                val hasPageChat = currentPageChatMsgs.isNotEmpty()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasPageChat) Color(0xFF10A37F) else Color.White.copy(alpha = 0.15f),
                        border = if (hasPageChat) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00DC82)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00DC82).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        if (openAiTargetPageText.isBlank()) {
                                            val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                            openAiTargetPageText = text.ifBlank {
                                                PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                            }
                                        }
                                        showPageChatDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = if (hasPageChat) "Chat IA (${currentPageChatMsgs.size})" else "Chat IA",
                                tint = if (hasPageChat) Color.White else Color(0xFF00DC82),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = if (hasPageChat) "${currentPageChatMsgs.size} Chat" else "Chat IA",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPageChat) Color(0xFF00DC82) else Color.White
                    )
                }

                // SCIENTIFIC RECONSTRUCTION ACTION (Fórmulas, Química, Citologia, Matemática, Física)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                scope.launch {
                                    if (currentBook != null) {
                                        val text = PdfRendererHelper.extractTextFromPage(context, currentBook, currentBook.currentPage).joinToString("\n\n")
                                        openAiTargetPageText = text.ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showScientificDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🔬", fontSize = 13.sp)
                        }
                    }
                    Text(
                        text = "Ciência",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Speed Control Action (0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x)
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                val currentSpeedIdx = speeds.indexOfFirst { kotlin.math.abs(it - speechSpeed) < 0.05f }
                val nextSpeedIndex = if (currentSpeedIdx in speeds.indices) {
                    (currentSpeedIdx + 1) % speeds.size
                } else {
                    speeds.indexOfFirst { it >= speechSpeed }.takeIf { it != -1 } ?: 0
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                onSpeedChange(speeds[nextSpeedIndex])
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val formattedSpeed = if (speechSpeed % 1.0f == 0f) "${speechSpeed.toInt()}x" else "${speechSpeed}x"
                            Text(
                                text = formattedSpeed,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = "Veloc.",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // BOOKS Action (Substitui o botão Favorito mantendo toda a integridade da UI)
                val currentBookRecommendations = remember(bookRecommendations, currentBook?.id, currentBook?.currentPage) {
                    if (currentBook != null) {
                        bookRecommendations.filter { it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage }
                    } else emptyList()
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (currentBookRecommendations.isNotEmpty()) Color(0xFF2563EB).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f),
                        border = if (currentBookRecommendations.isNotEmpty()) BorderStroke(1.dp, Color(0xFF60A5FA)) else null,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                if (currentBook != null) {
                                    if (openAiTargetPageText.isBlank()) {
                                        scope.launch {
                                            val (_, extracted) = PdfRendererHelper.extractParagraphsAndSentencesFromPage(
                                                context, currentBook, currentBook.currentPage
                                            )
                                            openAiTargetPageText = extracted.joinToString(" ").trim()
                                            showBooksDialog = true
                                        }
                                    } else {
                                        showBooksDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "BOOKS",
                                tint = if (currentBookRecommendations.isNotEmpty()) Color(0xFF93C5FD) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "BOOKS",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBookRecommendations.isNotEmpty()) Color(0xFF93C5FD) else Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                // ⚖️ PORTAL DE LEGISLAÇÃO & VADE MECUM PLANALTO
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD97706),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFCD34D)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                showLegislationDialog = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Legislação Planalto",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Leis",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFCD34D)
                    )
                }

                // ⚡ OFFLINE READING SUITE (Métricas, Cloze Test, Dicionário, Notas e TTS Offline)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF059669).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                if (currentBook != null) {
                                    scope.launch {
                                        val (_, extracted) = PdfRendererHelper.extractParagraphsAndSentencesFromPage(
                                            context, currentBook, currentBook.currentPage
                                        )
                                        offlineTargetRawSentences = extracted
                                        offlineTargetPageText = extracted.joinToString(" ").trim().ifBlank {
                                            PdfRendererHelper.parseSamplePages(currentBook.sampleContentJson).getOrNull(currentBook.currentPage - 1) ?: currentBook.title
                                        }
                                        showOfflinePageDialog = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Offline Suite",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Offline",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }

                // Caderno de Citações e Marcações Action (Highlights)
                val bookHighlightsCount = remember(highlights, currentBook?.id) {
                    if (currentBook != null) {
                        highlights.count { it.bookId == currentBook.id }
                    } else highlights.size
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (bookHighlightsCount > 0) Color(0xFFD97706) else Color.White.copy(alpha = 0.15f),
                        border = if (bookHighlightsCount > 0) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24)) else null,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                showHighlightsNotebook = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Caderno de Citações ($bookHighlightsCount)",
                                tint = if (bookHighlightsCount > 0) Color.White else Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = if (bookHighlightsCount > 0) "$bookHighlightsCount Citaç." else "Citações",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bookHighlightsCount > 0) Color(0xFFFBBF24) else Color.White
                    )
                }

                // Table of Contents / Sumário (Visível apenas após clicar para ler o livro e estar nas páginas)
                if (currentBook != null && openedBookIds.contains(currentBook.id)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF8E2DE2),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFC084FC)),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    val b = currentBook ?: return@clickable
                                    scope.launch {
                                        val existing = bookTocMap[b.id]
                                        if (existing.isNullOrEmpty()) {
                                            val toc = PdfRendererHelper.generateTableOfContents(context, b)
                                            bookTocMap[b.id] = toc
                                        }
                                        showTocSheet = true
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = "Sumário",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "Sumário",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC084FC)
                        )
                    }

                    // Ação de Tela Cheia
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { isFullscreen = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Tela Cheia",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "Cheia",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

        // Floating Exit Fullscreen Button
        AnimatedVisibility(
            visible = isReading && isFullscreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(38.dp)
                    .clickable { isFullscreen = false }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Sair da Tela Cheia",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Double-Tap Star Burst Pop-up Animation
        AnimatedVisibility(
            visible = showStarAnimation,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 1.3f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // Huge Page Number Overlay with Fade-Out
        BigPageNumberOverlay(
            visible = showBigPageOverlay,
            pageNumber = lastOverlayPage,
            totalPages = currentBook?.totalPages ?: 1
        )
    }

    // Voice Selector Modal Bottom Sheet
    if (showVoiceSelectorSheet) {
        VoiceSelectorBottomSheet(
            speechManager = speechManager,
            availableVoices = availableVoices,
            selectedVoice = selectedVoice,
            readingTheme = activeTheme,
            onVoiceSelected = { voice ->
                speechManager.selectVoice(voice)
            },
            onDismiss = { showVoiceSelectorSheet = false }
        )
    }

    // Table of Contents Dialog (Formatado em árvore hierárquica idêntico à imagem de referência HOJE.PNG)
    if (showTocSheet && currentBook != null) {
        InteractiveTocDialog(
            book = currentBook,
            currentPage = currentBook.currentPage,
            tocItems = bookTocMap[currentBook.id] ?: emptyList(),
            readingTheme = activeTheme,
            onDismiss = { showTocSheet = false },
            onSelectPage = { targetPage ->
                onPageChanged(currentBook, targetPage)
                triggerBigPageOverlay(targetPage)
                openedBookIds = openedBookIds + currentBook.id
                showTocSheet = false
            }
        )
    }

    // Direct Jump To Page Dialog in Reels
    if (showJumpPageDialog && currentBook != null) {
        var pageInputText by remember(showJumpPageDialog, currentBook.id, currentBook.currentPage) { 
            mutableStateOf(currentBook.currentPage.toString()) 
        }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showJumpPageDialog = false },
            title = {
                Text(
                    text = "Ir para a página",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Digite o número da página (1 a ${currentBook.totalPages}):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pageInputText,
                        onValueChange = {
                            pageInputText = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Número da página") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val target = pageInputText.trim().toIntOrNull()
                                if (target != null && target in 1..currentBook.totalPages) {
                                    onPageChanged(currentBook, target)
                                    triggerBigPageOverlay(target)
                                    openedBookIds = openedBookIds + currentBook.id
                                    showJumpPageDialog = false
                                } else {
                                    errorMessage = "Insira um valor entre 1 e ${currentBook.totalPages}"
                                }
                            }
                        ),
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Início (1)" to 1,
                            "25%" to (currentBook.totalPages * 0.25f).toInt().coerceAtLeast(1),
                            "50%" to (currentBook.totalPages * 0.50f).toInt().coerceAtLeast(1),
                            "Fim (${currentBook.totalPages})" to currentBook.totalPages
                        ).forEach { (label, pg) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onPageChanged(currentBook, pg)
                                        triggerBigPageOverlay(pg)
                                        openedBookIds = openedBookIds + currentBook.id
                                        showJumpPageDialog = false
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pageInputText.trim().toIntOrNull()
                        if (target != null && target in 1..currentBook.totalPages) {
                            onPageChanged(currentBook, target)
                            triggerBigPageOverlay(target)
                            openedBookIds = openedBookIds + currentBook.id
                            showJumpPageDialog = false
                        } else {
                            errorMessage = "Insira um valor entre 1 e ${currentBook.totalPages}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2DE2))
                ) {
                    Text("Ir Direto", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpPageDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Gemini Explanation Dialog in Reels
    if (geminiSelectedTerm != null) {
        val term = geminiSelectedTerm!!
        val fullCtx = geminiSelectedContext ?: term
        val savedNote = geminiNotes.firstOrNull { it.projectId == currentBook?.id && it.termOrSentence.equals(term, ignoreCase = true) }

        GeminiExplanationDialog(
            termOrSentence = term,
            fullContext = fullCtx,
            translationContext = null,
            cueIndex = activeSentenceIndex + 1,
            existingNote = savedNote,
            isStarred = isCurrentBookStarred,
            readingTheme = activeTheme,
            category = currentBook?.category,
            categoryColor = currentBook?.categoryColor,
            onToggleStar = {
                if (currentBook != null) {
                    onToggleStarPage(currentBook.currentPage)
                }
            },
            onSaveNote = { responseText ->
                onSaveGeminiNote(activeSentenceIndex + 1, term, fullCtx, responseText, currentBook?.id)
            },
            onDeleteNote = {
                onDeleteGeminiNoteForCue(activeSentenceIndex + 1, currentBook?.id)
            },
            onDismiss = {
                geminiSelectedTerm = null
                geminiSelectedContext = null
            }
        )
    }

    // OpenAI Summary & Voice Dialog in Reels
    if (showOpenAiSummaryDialog && currentBook != null) {
        val pageSummary = openAiSummaries.firstOrNull {
            it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage
        }
        OpenAiSummaryDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageText = openAiTargetPageText,
            existingSummary = pageSummary,
            readingTheme = activeTheme,
            onSaveSummary = { newSummary ->
                onSaveOpenAiSummary(newSummary)
            },
            onDeleteSummary = { summaryToDelete ->
                onDeleteOpenAiSummary(summaryToDelete)
            },
            onDismiss = {
                showOpenAiSummaryDialog = false
            }
        )
    }

    // Universal Dictionary Dialog in Reels
    if (showDictionaryDialog && currentBook != null) {
        val currentPageTerms = dictionaryTerms.filter {
            it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage
        }
        val currentBookTerms = dictionaryTerms.filter {
            it.bookId == currentBook.id
        }
        OpenAiDictionaryDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageText = openAiTargetPageText,
            pageTerms = currentPageTerms,
            bookTerms = currentBookTerms,
            allTerms = dictionaryTerms,
            initialFocusedTerm = focusedDictionaryTerm,
            readingTheme = activeTheme,
            verbsList = verbPhrasalTerms,
            onSaveVerbs = onSaveVerbPhrasalTerms,
            onToggleFavoriteVerb = onToggleFavoriteVerb,
            onSaveTerms = { terms ->
                onReplaceDictionaryTermsForPage(currentBook.id, currentBook.currentPage, terms)
            },
            onUpdateTerm = { updated ->
                onUpdateDictionaryTerm(updated)
            },
            onDeleteTerm = { term ->
                onDeleteDictionaryTerm(term)
            },
            onDeleteTermsForPage = { bookId, page ->
                onDeleteDictionaryTermsForPage(bookId, page)
            },
            onDismiss = {
                showDictionaryDialog = false
                focusedDictionaryTerm = null
            }
        )
    }

    // Comparative Language Dialog in Reels (Exclusivo para Dicionários)
    if (showComparativeDialog && currentBook != null) {
        val currentPageComparativeTerms = comparativeTerms.filter {
            it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage
        }
        val currentBookComparativeTerms = comparativeTerms.filter {
            it.bookId == currentBook.id
        }
        ComparativeLanguageDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageText = openAiTargetPageText,
            pageTerms = currentPageComparativeTerms,
            bookTerms = currentBookComparativeTerms,
            allTerms = comparativeTerms,
            readingTheme = activeTheme,
            onDismiss = {
                showComparativeDialog = false
            },
            onSaveTerms = { terms ->
                onReplaceComparativeTermsForPage(currentBook.id, currentBook.currentPage, terms)
            },
            onUpdateTerm = { updated ->
                onUpdateComparativeTerm(updated)
            },
            onDeleteTerm = { term ->
                onDeleteComparativeTerm(term)
            },
            onDeleteTermsForPage = { bookId, page ->
                onDeleteComparativeTermsForPage(bookId, page)
            }
        )
    }

    // Page Multi-Translation Dialog (Tradução Trilateral Completa: EN, FR, ES + Comparativo Offline)
    if (showPageMultiTranslationDialog && currentBook != null) {
        val currentPageTranslation = pageTranslations.firstOrNull {
            it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage
        }
        PageMultiTranslationDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageOriginalText = openAiTargetPageText,
            savedTranslation = currentPageTranslation,
            speechManager = speechManager,
            readingTheme = activeTheme,
            onDismiss = { showPageMultiTranslationDialog = false },
            onSaveTranslation = { translation ->
                onSavePageTranslation(translation)
            },
            onDeleteTranslation = { translation ->
                onDeletePageTranslation(translation)
            },
            onApplyReadingLanguageToReader = { lang ->
                val pageKey = "${currentBook.id}_${currentBook.currentPage}"
                activeReadingLanguages = activeReadingLanguages + (pageKey to lang)
                speechManager.setReadingLanguage(lang)
            }
        )
    }

    // 🧠 10 Recursos de Estudo Avançado OpenAI (Quiz, Flashcards, Podcast 2 Vozes, Pronúncia Whisper, Audiograma, Frases Contexto, Falsos Amigos, Mapa Semântico)
    if (showOpenAiAdvancedStudySheet && currentBook != null) {
        val currentPageTerms = dictionaryTerms.filter {
            it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage
        }
        OpenAiStudyFeaturesBottomSheet(
            pageNumber = currentBook.currentPage,
            pageText = openAiTargetPageText,
            pageTerms = currentPageTerms,
            accentColor = Color(0xFF10A37F),
            onDismissRequest = { showOpenAiAdvancedStudySheet = false }
        )
    }

    // Scientific Reconstruction Dialog (Fórmulas, Química, Citologia, Matemática, Física)
    if (showScientificDialog && currentBook != null && viewModel != null) {
        ScientificReconstructionDialog(
            viewModel = viewModel,
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageRawText = openAiTargetPageText,
            speechManager = speechManager,
            onDismiss = { showScientificDialog = false }
        )
    }

    // 💬 CHAT IA / TUTOR INTELIGENTE DA PÁGINA EM REELS (Com Áudio e Texto)
    if (showPageChatDialog && currentBook != null) {
        val currentPageChatMsgs = remember(effectiveSegmentChatMessages, currentBook.id, currentBook.currentPage) {
            effectiveSegmentChatMessages.filter { it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage }
        }
        val isChatLoading = effectiveSegmentChatLoadingMap["${currentBook.id}_${currentBook.currentPage}_0"] == true ||
                effectiveSegmentChatLoadingMap.any { it.key.startsWith("${currentBook.id}_${currentBook.currentPage}_") && it.value }
        val chatError = effectiveSegmentChatErrorMap["${currentBook.id}_${currentBook.currentPage}_0"]
        val chatDialogPalette = remember(activeTheme, currentBook.category, currentBook.categoryColor) {
            com.example.ui.theme.ReadingThemeManager.getPalette(activeTheme, currentBook.category, currentBook.categoryColor)
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPageChatDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = chatDialogPalette.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, chatDialogPalette.cardBorder.takeIf { it != Color.Transparent } ?: chatDialogPalette.accent.copy(alpha = 0.5f)),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.88f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header do Diálogo de Chat da Página
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = chatDialogPalette.accent.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = chatDialogPalette.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Chat IA • Página ${currentBook.currentPage}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chatDialogPalette.textPrimary
                                )
                                Text(
                                    text = currentBook.title,
                                    fontSize = 11.sp,
                                    color = chatDialogPalette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { showPageChatDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar Chat",
                                tint = chatDialogPalette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Componente de Chat Inteligente com Áudio e Texto
                    SegmentAiChatCard(
                        segmentIndex = 0,
                        segmentText = openAiTargetPageText.ifBlank { currentBook.title },
                        messages = currentPageChatMsgs,
                        isLoading = isChatLoading,
                        errorMessage = chatError,
                        isMinimized = false,
                        readingTheme = activeTheme,
                        category = currentBook.category,
                        categoryColor = currentBook.categoryColor,
                        modifier = Modifier.fillMaxSize(),
                        onToggleMinimize = {},
                        onSendMessage = { question: String, isAudio: Boolean ->
                            effectiveOnSendChatMessage(
                                currentBook,
                                currentBook.currentPage,
                                0,
                                openAiTargetPageText.ifBlank { currentBook.title },
                                question,
                                isAudio
                            )
                        },
                        onClearChat = {
                            effectiveOnClearSegmentChat(currentBook.id, currentBook.currentPage, 0)
                        },
                        onDeleteMessage = effectiveOnDeleteChatMessage,
                        onDismiss = { showPageChatDialog = false },
                        onSpeakTextInLang = { langCode: String, text: String ->
                            speechManager.speakTextImmediately(text)
                        },
                        onStopSpeaking = {
                            speechManager.stop()
                        }
                    )
                }
            }
        }
    }

    // Page Restructure Dialog (Correção de letras faltantes, OCR bugado, pontuação e símbolos para voz)
    if (showPageRestructureDialog && currentBook != null) {
        PageRestructureDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            currentRawText = pageRestructureTargetText,
            speechManager = speechManager,
            viewModel = viewModel,
            onDismiss = { showPageRestructureDialog = false },
            onApplyRestructuredText = { newText ->
                scope.launch {
                    PdfRendererHelper.saveCustomPageText(context, currentBook.id, currentBook.currentPage, newText)
                    pageContentRevision = System.currentTimeMillis()
                    viewModel?.applyScientificTextToReader(currentBook.id, currentBook.currentPage, newText)
                }
            },
            onResetToOriginal = {
                scope.launch {
                    PdfRendererHelper.deleteCustomPageText(context, currentBook.id, currentBook.currentPage)
                    pageContentRevision = System.currentTimeMillis()
                    viewModel?.applyScientificTextToReader(currentBook.id, currentBook.currentPage, "")
                }
            }
        )
    }

    // Category Details & OpenAI Reclassification Dialog
    if (showCategoryDialog && currentBook != null) {
        BookCategoryDetailsDialog(
            book = currentBook,
            isClassifying = isClassifyingBook,
            currentReadingTheme = activeTheme,
            onDismiss = { showCategoryDialog = false },
            onApplyCategoryColorTheme = {
                activeTheme = "CATEGORY_COLOR"
                onThemeChange("CATEGORY_COLOR")
            },
            onReclassifyWithOpenAi = {
                onReclassifyCategory(currentBook)
            },
            onSelectCategory = { cat, sub, col ->
                onSelectCategory(currentBook.id, cat, sub, col)
            }
        )
    }

    // Book Questions Dialog (FGV, CEBRASPE, FCC, IADES, 100% Offline)
    if (showBookQuestionsDialog && currentBook != null) {
        val currentBookQuestions = bookQuestions.filter { it.bookId == currentBook.id }
        BookQuestionsDialog(
            book = currentBook,
            currentPage = currentBook.currentPage,
            questions = currentBookQuestions,
            allGlobalQuestions = bookQuestions,
            allBooks = books,
            isGenerating = isGeneratingQuestions,
            generationError = questionsGenerationError,
            readingTheme = activeTheme,
            onDismiss = { showBookQuestionsDialog = false },
            onAnswerSelected = onAnswerQuestion,
            onResetAnswer = onResetQuestionAnswer,
            onResetAllAnswers = { onResetAllAnswers(currentBook.id) },
            onDeleteQuestion = onDeleteQuestion,
            onDeleteQuestionsForPage = { page -> onDeleteQuestionsForPage(currentBook.id, page) },
            onDeleteAllQuestionsForBook = { onDeleteAllQuestionsForBook(currentBook.id) },
            onGenerateQuestions = { startPage, endPage, institution, difficulty, count, model ->
                onGenerateQuestions(currentBook, startPage, endPage, institution, difficulty, count, model)
            },
            onSpeakText = { text ->
                speechManager.speakTextImmediately(text)
            },
            onOpenGlobalReels = {
                showGlobalQuestionsReelDialog = true
            }
        )
    }

    // Global Questions Reel Dialog (Banco Geral de Questões de Todos os Livros)
    if (showGlobalQuestionsReelDialog) {
        GlobalQuestionsReelDialog(
            allQuestions = bookQuestions,
            allBooks = books,
            onAnswerSelected = onAnswerQuestion,
            onResetAnswer = onResetQuestionAnswer,
            onResetAllQuestionsGlobal = onResetAllQuestionsGlobal,
            onDeleteQuestion = onDeleteQuestion,
            onDismiss = { showGlobalQuestionsReelDialog = false },
            onSpeakText = { text ->
                speechManager.speakTextImmediately(text)
            },
            onOpenBookAtPage = { targetBook, targetPage ->
                showGlobalQuestionsReelDialog = false
                onPageChanged(targetBook, targetPage)
            }
        )
    }

    // BOOKS: Recomendações de Livros Reais via OpenAI e Análise Temática/Termos entre Vírgulas
    if (showBooksDialog && currentBook != null) {
        val currentPageRecommendations = bookRecommendations.filter {
            it.bookId == currentBook.id && it.pageNumber == currentBook.currentPage
        }
        BooksRecommendationsDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageText = openAiTargetPageText,
            recommendations = currentPageRecommendations,
            isGenerating = isGeneratingBookRecommendations,
            errorMessage = bookRecommendationsError,
            readingTheme = activeTheme,
            onDismiss = { showBooksDialog = false },
            onGenerate = { forceRegenerate ->
                onGenerateBookRecommendations(
                    currentBook,
                    currentBook.currentPage,
                    openAiTargetPageText,
                    forceRegenerate
                )
            },
            onDeleteRecommendations = {
                onDeleteBookRecommendationsForPage(currentBook.id, currentBook.currentPage)
            },
            onSpeakText = { text ->
                speechManager.speakTextImmediately(text)
            }
        )
    }

    // AUTHOR COLLECTION: Diálogo de Criação e Edição de Autor (com foto e seleção de livros)
    if (showAuthorDialog) {
        AuthorCollectionDialog(
            author = authorToEdit,
            allBooks = books,
            onDismiss = {
                showAuthorDialog = false
                authorToEdit = null
            },
            onSaveAuthor = { savedAuthor, bookIds, directBooks ->
                if (directBooks.isNotEmpty() && viewModel != null) {
                    viewModel.createAuthorWithBooks(savedAuthor, bookIds, directBooks)
                } else {
                    onSaveAuthor(savedAuthor, bookIds)
                }
                showAuthorDialog = false
                authorToEdit = null
            },
            onDeleteAuthor = { authorToDelete ->
                if (selectedAuthorCollection?.id == authorToDelete.id) {
                    selectedAuthorCollection = null
                }
                onDeleteAuthor(authorToDelete)
                showAuthorDialog = false
                authorToEdit = null
            },
            onAddDirectBooks = { items ->
                viewModel?.createMultiplePdfBooks(items)
            }
        )
    }

    // AUTHOR COLLECTION: Diálogo para gerenciar quais livros estão dentro da coleção
    if (authorToManageBooks != null) {
        ManageAuthorBooksDialog(
            author = authorToManageBooks!!,
            allBooks = books,
            onDismiss = { authorToManageBooks = null },
            onSaveBooks = { bookIds ->
                onAssignBooksToAuthor(authorToManageBooks!!.id, bookIds)
                authorToManageBooks = null
            },
            onAddDirectBooks = { items ->
                viewModel?.createMultiplePdfBooks(items)
            }
        )
    }

    // FONT SIZE: Diálogo de Ajuste do Tamanho da Fonte
    if (showFontSizeDialog) {
        FontSizeDialog(
            fontSizeSp = reelsFontSizeSp,
            onFontSizeChange = {
                reelsFontSizeSp = it
                fontPrefs.edit().putFloat("global_reels_font_size_sp", it).apply()
            },
            onDismissRequest = { showFontSizeDialog = false }
        )
    }

    // PICKER: Diálogo para Escolher Livros, Autores e Matérias
    if (showBookPickerSheet) {
        BookAndAuthorPickerDialog(
            books = books,
            authors = authors,
            subjects = subjects,
            onSelectBook = { selectedBook ->
                selectedAuthorCollection = null
                selectedSubjectDiscipline = null
                if (feedFilterMode == ReelsFilterMode.AUTHORS || feedFilterMode == ReelsFilterMode.SUBJECTS) {
                    feedFilterMode = ReelsFilterMode.BOOKS
                }
                scope.launch {
                    val targetIdx = currentFeedItems.indexOfFirst {
                        (it as? ReelsFeedItem.BookItem)?.book?.id == selectedBook.id ||
                        (it as? ReelsFeedItem.BooksGridItem)?.books?.any { b -> b.id == selectedBook.id } == true
                    }
                    if (targetIdx >= 0) {
                        pagerState.scrollToPage(targetIdx)
                    }
                }
                activeReadingBookId = selectedBook.id
                openedBookIds = openedBookIds + selectedBook.id
                onPageChanged(selectedBook, selectedBook.currentPage)
                ensureTocLoaded(selectedBook)
            },
            onSelectAuthor = { author ->
                selectedSubjectDiscipline = null
                selectedAuthorCollection = null
                feedFilterMode = ReelsFilterMode.AUTHORS
                scope.launch {
                    val targetIdx = currentFeedItems.indexOfFirst {
                        (it as? ReelsFeedItem.AuthorItem)?.author?.id == author.id
                    }
                    if (targetIdx >= 0) {
                        pagerState.scrollToPage(targetIdx)
                    }
                }
            },
            onSelectSubject = { subject ->
                selectedAuthorCollection = null
                selectedSubjectDiscipline = subject
                feedFilterMode = ReelsFilterMode.ALL
                scope.launch {
                    pagerState.scrollToPage(0)
                }
            },
            onDismissRequest = { showBookPickerSheet = false }
        )
    }

    // SUBJECT / DISCIPLINE: Diálogo de Criação e Edição de Matéria / Disciplina
    if (showSubjectDialog) {
        SubjectDisciplineDialog(
            subject = subjectToEdit,
            allAuthors = authors,
            allBooks = books,
            onDismiss = {
                showSubjectDialog = false
                subjectToEdit = null
            },
            onSaveSubject = { savedSubj, authorIds, directBookIds ->
                onSaveSubject(savedSubj, authorIds, directBookIds)
                showSubjectDialog = false
                subjectToEdit = null
            },
            onDeleteSubject = { subjToDelete ->
                if (selectedSubjectDiscipline?.id == subjToDelete.id) {
                    selectedSubjectDiscipline = null
                }
                onDeleteSubject(subjToDelete)
                showSubjectDialog = false
                subjectToEdit = null
            },
            onAddDirectBooks = { subjectId, items ->
                onAddDirectBooksToSubject(subjectId, items)
            }
        )
    }

    // SUBJECT / DISCIPLINE: Diálogo para Gerenciar Autores e Livros Diretos da Matéria
    if (subjectToManageContent != null) {
        ManageSubjectContentDialog(
            subject = subjectToManageContent!!,
            allAuthors = authors,
            allBooks = books,
            onDismiss = { subjectToManageContent = null },
            onSaveAssociations = { authorIds, directBookIds ->
                onUpdateSubjectAssociations(subjectToManageContent!!.id, authorIds, directBookIds)
                subjectToManageContent = null
            },
            onAddDirectBooks = { subjectId, items ->
                onAddDirectBooksToSubject(subjectId, items)
            }
        )
    }

    // CADERNO DE CITAÇÕES E MARCAÇÕES (HIGHLIGHTS)
    if (showHighlightsNotebook) {
        HighlightsNotebookDialog(
            currentBook = currentBook,
            highlights = highlights,
            onDismiss = { showHighlightsNotebook = false },
            onNavigateToPage = { targetBookId, targetPage ->
                showHighlightsNotebook = false
                val targetBook = books.firstOrNull { it.id == targetBookId }
                if (targetBook != null) {
                    val targetIdx = currentFeedItems.indexOfFirst {
                        (it as? ReelsFeedItem.BookItem)?.book?.id == targetBookId ||
                        (it as? ReelsFeedItem.BooksGridItem)?.books?.any { b -> b.id == targetBookId } == true
                    }
                    if (targetIdx >= 0) {
                        scope.launch { pagerState.scrollToPage(targetIdx) }
                    }
                    activeReadingBookId = targetBookId
                    openedBookIds = openedBookIds + targetBookId
                    onPageChanged(targetBook, targetPage)
                    ensureTocLoaded(targetBook)
                }
            },
            onSpeakText = { textToSpeak ->
                speechManager.speakTextImmediately(textToSpeak)
            },
            onSaveHighlight = { highlight ->
                onSaveHighlight(highlight)
            },
            onDeleteHighlight = { highlight ->
                onDeleteHighlight(highlight)
            },
            speechManager = speechManager
        )
    }

    // ALARME DE ESTUDO: Overlay de Notificação/Alerta em Tela Cheia ao Disparar
    if (activeAlarmEvent != null) {
        StudyAlarmAlertOverlay(
            alarmEvent = activeAlarmEvent!!,
            onDismiss = {
                viewModel?.dismissActiveStudyAlarm()
            },
            onOpenBookNow = { bookToOpen ->
                viewModel?.dismissActiveStudyAlarm()
                activeReadingBookId = bookToOpen.id
                openedBookIds = openedBookIds + bookToOpen.id
                val targetIdx = currentFeedItems.indexOfFirst {
                    (it as? ReelsFeedItem.BookItem)?.book?.id == bookToOpen.id ||
                    (it as? ReelsFeedItem.BooksGridItem)?.books?.any { b -> b.id == bookToOpen.id } == true
                }
                if (targetIdx >= 0) {
                    scope.launch { pagerState.scrollToPage(targetIdx) }
                }
                onPageChanged(bookToOpen, bookToOpen.currentPage)
                ensureTocLoaded(bookToOpen)
            }
        )
    }

    // ALARME DE ESTUDO: Diálogo de Configurações (Intervalo em minutos, som, vibração, teste)
    if (showStudyAlarmDialog) {
        StudyAlarmSettingsDialog(
            currentSettings = studyAlarmSettings,
            onDismiss = { showStudyAlarmDialog = false },
            onSaveSettings = { enabled, intervalMinutes, soundEnabled, vibrationEnabled, customAudioUri, customAudioName ->
                viewModel?.updateStudyAlarmSettings(
                    enabled = enabled,
                    intervalMinutes = intervalMinutes,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    customAudioUri = customAudioUri,
                    customAudioName = customAudioName
                )
            },
            onTestAlarmNow = {
                viewModel?.testTriggerStudyAlarmNow()
            }
        )
    }

    // ÓRGÃOS PÚBLICOS E AUTARQUIAS: Detalhes do Órgão e Editais em PDF
    if (selectedOrgForDetail != null) {
        PublicOrganizationDetailDialog(
            organization = selectedOrgForDetail!!,
            allBooks = books,
            onDismiss = { selectedOrgForDetail = null },
            onOpenBook = { editalBook ->
                selectedOrgForDetail = null
                speechManager.stop()
                booksLayoutMode = ReelsBooksLayoutMode.SINGLE
                selectedAuthorCollection = null
                selectedSubjectDiscipline = null
                feedFilterMode = ReelsFilterMode.BOOKS
                activeReadingBookId = editalBook.id
                openedBookIds = openedBookIds + editalBook.id
                onPageChanged(editalBook, editalBook.currentPage)
                ensureTocLoaded(editalBook)
                scope.launch {
                    val targetIdx = currentFeedItems.indexOfFirst {
                        (it as? ReelsFeedItem.BookItem)?.book?.id == editalBook.id ||
                        (it as? ReelsFeedItem.BooksGridItem)?.books?.any { b -> b.id == editalBook.id } == true
                    }
                    if (targetIdx >= 0) {
                        pagerState.scrollToPage(targetIdx)
                    }
                }
            },
            onAddBookToOrg = { bookId ->
                val org = selectedOrgForDetail ?: return@PublicOrganizationDetailDialog
                viewModel?.addPdfToOrganization(org.id, bookId)
                selectedOrgForDetail = org.withAddedPdfBookId(bookId)
            },
            onRemoveBookFromOrg = { bookId ->
                val org = selectedOrgForDetail ?: return@PublicOrganizationDetailDialog
                viewModel?.removePdfFromOrganization(org.id, bookId)
                selectedOrgForDetail = org.withRemovedPdfBookId(bookId)
            },
            onEditOrg = {
                orgToEdit = selectedOrgForDetail
                showAddEditOrgDialog = true
            },
            onDeleteOrg = {
                val org = selectedOrgForDetail ?: return@PublicOrganizationDetailDialog
                viewModel?.deletePublicOrganization(org)
                selectedOrgForDetail = null
            }
        )
    }

    // ÓRGÃOS PÚBLICOS: Diálogo de Adicionar / Editar Órgão
    if (showAddEditOrgDialog) {
        AddEditPublicOrganizationDialog(
            initialOrganization = orgToEdit,
            onDismiss = {
                showAddEditOrgDialog = false
                orgToEdit = null
            },
            onSave = { savedOrg ->
                viewModel?.savePublicOrganization(savedOrg)
                if (selectedOrgForDetail?.id == savedOrg.id) {
                    selectedOrgForDetail = savedOrg
                }
                showAddEditOrgDialog = false
                orgToEdit = null
            }
        )
    }

    // OFFLINE PAGE READER SUITE DIALOG
    if (showOfflinePageDialog && currentBook != null) {
        val currentPageNotes = geminiNotes.filter { it.projectId == currentBook.id && it.cueIndex == currentBook.currentPage }

        OfflinePageReaderDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageText = offlineTargetPageText,
            rawSentences = offlineTargetRawSentences,
            speechManager = speechManager,
            isSpeaking = isSpeaking,
            pageNotes = currentPageNotes,
            pageDictionaryTerms = dictionaryTerms,
            onSaveNote = { term, noteText ->
                onSaveGeminiNote(currentBook.currentPage, term, noteText, noteText, currentBook.id)
            },
            onDeleteNote = { note ->
                onDeleteGeminiNoteForCue(note.cueIndex, note.projectId)
            },
            onSaveDictionaryTerm = { term ->
                if (viewModel != null) {
                    viewModel.saveDictionaryTerm(term)
                } else {
                    onSaveDictionaryTerms(listOf(term))
                }
            },
            onSaveDictionaryTerms = { terms ->
                if (viewModel != null) {
                    viewModel.saveDictionaryTerms(terms)
                } else {
                    onSaveDictionaryTerms(terms)
                }
            },
            onDeleteDictionaryTerm = { term ->
                onDeleteDictionaryTerm(term)
            },
            onDismiss = { showOfflinePageDialog = false }
        )
    }

    // PORTAL DE LEGISLAÇÃO DO PLANALTO & VADE MECUM DIGITAL INTERATIVO
    if (showLegislationDialog) {
        PlanaltoLegislationDialog(
            savedLawBooks = books,
            speechManager = speechManager,
            onSaveQuestions = { questions ->
                viewModel?.saveQuestions(questions)
            },
            onRecordQuestionAnswer = { qId, sIdx, isCorr ->
                viewModel?.recordQuestionAnswer(qId, sIdx, isCorr)
            },
            onResetQuestionAnswer = { qId ->
                viewModel?.resetQuestionAnswer(qId)
            },
            onSaveDictionaryTerms = { terms ->
                if (viewModel != null) {
                    viewModel.saveDictionaryTerms(terms)
                } else {
                    onSaveDictionaryTerms(terms)
                }
            },
            existingDictionaryTerms = dictionaryTerms,
            onOpenBook = { selectedLawBook ->
                activeReadingBookId = selectedLawBook.id
                openedBookIds = openedBookIds + selectedLawBook.id
                onPageChanged(selectedLawBook, selectedLawBook.currentPage)
                ensureTocLoaded(selectedLawBook)
                showLegislationDialog = false
            },
            onSyncLawAsBook = { title, author, sampleJson, totalPages, category, subcategory, categoryColor, sourceUrl ->
                viewModel?.syncLawBookToDatabase(
                    title = title,
                    author = author,
                    sampleJson = sampleJson,
                    totalPages = totalPages,
                    category = category,
                    subcategory = subcategory,
                    color = categoryColor,
                    sourceUrl = sourceUrl
                )
            },
            onCreateAndOpenLawBook = { title, author, sampleJson, totalPages ->
                showLegislationDialog = false
                viewModel?.createPdfBook(title, author, null, totalPages, sampleJson)
            },
            onDismiss = { showLegislationDialog = false }
        )
    }

    // RAIO-X POLIGLOTA C2: Russo, Alemão, Grego, Chinês, Japonês, Ucraniano, Árabe, Hebraico e Inglês
    if (selectedPolyglotWordForAnalysis != null && currentBook != null) {
        PolyglotWordAnalysisDialog(
            word = selectedPolyglotWordForAnalysis!!,
            initialLanguage = selectedPolyglotLanguage,
            sentenceContext = selectedPolyglotSentenceContext,
            bookTitle = currentBook.title,
            bookId = currentBook.id,
            pageNumber = currentBook.currentPage,
            readingTheme = activeTheme,
            category = currentBook.category,
            categoryColor = currentBook.categoryColor,
            speechManager = speechManager,
            onSaveToDictionary = { term ->
                if (viewModel != null) {
                    viewModel.saveDictionaryTerm(term)
                } else {
                    onSaveDictionaryTerms(listOf(term))
                }
            },
            onAnalyzeSentenceGrammar = { sent, lang ->
                selectedSentenceForGrammarAnalysis = sent
                selectedGrammarLanguage = lang
            },
            onDismiss = { selectedPolyglotWordForAnalysis = null }
        )
    }

    // SCANNER DE VOCABULÁRIO POLIGLOTA DA PÁGINA INTEIRA
    if (showPolyglotPageVocabDialog && currentBook != null) {
        PolyglotPageVocabularyDialog(
            book = currentBook,
            pageNumber = currentBook.currentPage,
            pageText = openAiTargetPageText,
            initialLanguage = selectedPolyglotLanguage,
            readingTheme = activeTheme,
            speechManager = speechManager,
            onSaveDictionaryTerms = { terms ->
                if (viewModel != null) {
                    viewModel.saveDictionaryTerms(terms)
                } else {
                    onSaveDictionaryTerms(terms)
                }
            },
            onOpenSingleWordAnalysis = { wordToAnalyze, lang, ctxSentence ->
                selectedPolyglotWordForAnalysis = wordToAnalyze
                selectedPolyglotLanguage = lang
                selectedPolyglotSentenceContext = ctxSentence
            },
            onDismiss = { showPolyglotPageVocabDialog = false }
        )
    }

    // ANÁLISE DE SINTAXE E GRAMÁTICA DE FRASES COMPLEXAS (8 IDIOMAS)
    if (selectedSentenceForGrammarAnalysis != null && currentBook != null) {
        SentenceGrammarAnalysisDialog(
            sentence = selectedSentenceForGrammarAnalysis!!,
            initialLanguage = selectedGrammarLanguage,
            bookTitle = currentBook.title,
            bookId = currentBook.id,
            pageNumber = currentBook.currentPage,
            readingTheme = activeTheme,
            category = currentBook.category,
            categoryColor = currentBook.categoryColor,
            speechManager = speechManager,
            onSaveAnalysis = { analysisToSave ->
                viewModel?.saveSentenceAnalysis(analysisToSave)
            },
            onDismiss = { selectedSentenceForGrammarAnalysis = null }
        )
    }

    // CADERNO DE ESTUDOS DE SINTAXE & GRAMÁTICA (FRASES SALVAS LOCALMENTE)
    if (showSavedSentencesStudySheet) {
        SavedSentencesStudySheet(
            savedAnalyses = savedSentenceAnalyses,
            readingTheme = activeTheme,
            category = currentBook?.category,
            categoryColor = currentBook?.categoryColor,
            speechManager = speechManager,
            onToggleFavorite = { analysis ->
                viewModel?.toggleFavoriteSentenceAnalysis(analysis)
            },
            onDeleteAnalysis = { id ->
                viewModel?.deleteSentenceAnalysis(id)
            },
            onUpdateNotes = { updated ->
                viewModel?.updateSentenceAnalysis(updated)
            },
            onDismiss = { showSavedSentencesStudySheet = false }
        )
    }
}

@Composable
fun ReelBookPageCard(
    book: PdfBook,
    readingTheme: String,
    isFullscreen: Boolean = false,
    viewMode: PdfViewMode = PdfViewMode.FOREGROUND_TEXT,
    textFontSizeSp: Float = 14f,
    contentRevision: Long = 0L,
    isShowingCoverFirst: Boolean = true,
    isSpeaking: Boolean,
    isSpeakingCategory: Boolean = false,
    onSpeakCategory: (() -> Unit)? = null,
    activeSentenceIndex: Int,
    activeWordIndex: Int = 0,
    activeComparativeLineId: String? = null,
    isClassifyingCategory: Boolean = false,
    onOpenCategoryDetails: () -> Unit = {},
    pageSummary: OpenAiPageSummary? = null,
    pageDictionaryTerms: List<PageDictionaryTerm> = emptyList(),
    pageTranslation: PageMultiTranslation? = null,
    activeReadingLanguage: ReadingLanguage = ReadingLanguage.PORTUGUESE,
    isComparativeMode: Boolean = false,
    onToggleComparativeMode: () -> Unit = {},
    speechManager: PdfSpeechManager? = null,
    onSelectReadingLanguage: (ReadingLanguage) -> Unit = {},
    onOpenMultiTranslation: () -> Unit = {},
    geminiNotes: List<GeminiNote> = emptyList(),
    pageSegmentExplanations: List<PageSegmentExplanation> = emptyList(),
    isGeneratingSegmentExplanation: Boolean = false,
    segmentExplanationError: String? = null,
    onRequestSegmentExplanation: (book: PdfBook, pageNumber: Int, segmentIndex: Int, segmentText: String, forceRegenerate: Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteSegmentExplanation: (PageSegmentExplanation) -> Unit = {},
    segmentChatMessages: List<SegmentChatMessage> = emptyList(),
    segmentChatLoadingMap: Map<String, Boolean> = emptyMap(),
    segmentChatErrorMap: Map<String, String?> = emptyMap(),
    onSendSegmentChatMessage: (book: PdfBook, pageNumber: Int, segmentIndex: Int, segmentText: String, question: String, isAudio: Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onClearSegmentChat: (bookId: Int, pageNumber: Int, segmentIndex: Int) -> Unit = { _, _, _ -> },
    onDeleteSegmentChatMessage: (SegmentChatMessage) -> Unit = {},
    pageQuestions: List<BookQuestion> = emptyList(),
    onAnswerQuestion: (questionId: Long, selectedIndex: Int, isCorrect: Boolean) -> Unit = { _, _, _ -> },
    onResetQuestionAnswer: (questionId: Long) -> Unit = {},
    onDeleteQuestion: (questionId: Long) -> Unit = {},
    onOpenQuestionsDialog: () -> Unit = {},
    onOpenSummary: () -> Unit = {},
    onOpenOpenAiStudy: () -> Unit = {},
    onOpenDictionary: () -> Unit = {},
    onOpenDictionaryTerm: (PageDictionaryTerm) -> Unit = {},
    onWordClick: (sentenceIndex: Int, wordIndex: Int, sentences: List<String>) -> Unit = { _, _, _ -> },
    onWordLongPress: (word: String, sentence: String) -> Unit = { _, _ -> },
    onAnalyzeSentence: (sentence: String) -> Unit = {},
    onOpenJumpDialog: () -> Unit = {},
    onOpenToc: () -> Unit = {},
    onEnterBook: () -> Unit = {},
    onReturnToCover: () -> Unit = {},
    onSwitchViewMode: (PdfViewMode) -> Unit = {},
    onDoubleTap: () -> Unit,
    onSingleTap: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSentenceGeminiClick: (term: String, sentence: String) -> Unit,
    onOpenOfflineTools: (() -> Unit)? = null,
    onSaveDictionaryTerms: (List<PageDictionaryTerm>) -> Unit = {},
    onToggleSpeech: () -> Unit = {}
) {
    val context = LocalContext.current
    var activeExpandedSegmentIndex by remember { mutableStateOf<Int?>(null) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedSentences by remember { mutableStateOf<List<String>>(emptyList()) }
    var readingParagraphs by remember { mutableStateOf<List<BookParagraph>>(emptyList()) }
    var isLoadingContent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var selectedTermLanguages by remember { mutableStateOf(mapOf<Int, StudyLanguage>()) }
    var termTranslationsCache by remember { mutableStateOf(mapOf<String, Pair<String, String>>()) }
    var selectedNoteLanguages by remember { mutableStateOf(mapOf<Int, StudyLanguage>()) }
    var noteTranslationsCache by remember { mutableStateOf(mapOf<String, Pair<String, String>>()) }
    var activeAllLanguagesDialogForTermId by remember { mutableStateOf<Int?>(null) }
    var activeAllLanguagesDialogForNoteId by remember { mutableStateOf<Int?>(null) }

    var dragTotalX by remember { mutableFloatStateOf(0f) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
        if (zoomScale > 1f) {
            val maxOffsetX = (zoomScale - 1f) * 400f
            val maxOffsetY = (zoomScale - 1f) * 600f
            panOffset = Offset(
                x = (panOffset.x + panChange.x * zoomScale).coerceIn(-maxOffsetX, maxOffsetX),
                y = (panOffset.y + panChange.y * zoomScale).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            panOffset = Offset.Zero
        }
    }

    // Carregar capa (página 0) apenas uma vez por livro, de forma leve e com cores autênticas
    LaunchedEffect(book.id) {
        if (!book.fileUri.isNullOrBlank() && coverBitmap == null) {
            try {
                coverBitmap = PdfRendererHelper.renderPdfPageBitmap(
                    context = context,
                    fileUri = book.fileUri,
                    pageIndexZeroBased = 0,
                    destWidth = 850,
                    readingTheme = "ORIGINAL"
                )
            } catch (_: Throwable) {}
        }
    }

    // Extrair texto (nativo / OCR / sample) e renderizar bitmap da página atual
    // SOMENTE quando o usuário estiver no modo de leitura (!isShowingCoverFirst)
    LaunchedEffect(book.id, book.currentPage, readingTheme, activeReadingLanguage, pageTranslation?.translatedAt, isShowingCoverFirst, contentRevision) {
        activeExpandedSegmentIndex = null
        if (isShowingCoverFirst) {
            // No modo review / navegação de capas: não aloca memória para páginas internas nem OCR
            return@LaunchedEffect
        }

        isLoadingContent = true
        zoomScale = 1f
        panOffset = Offset.Zero

        if (!book.fileUri.isNullOrBlank()) {
            try {
                pageBitmap = PdfRendererHelper.renderPdfPageBitmap(
                    context = context,
                    fileUri = book.fileUri,
                    pageIndexZeroBased = (book.currentPage - 1).coerceAtLeast(0),
                    destWidth = 1080,
                    readingTheme = readingTheme
                )
            } catch (_: Throwable) {}
        }

        try {
            val (paras, sentences) = when (activeReadingLanguage) {
                ReadingLanguage.PORTUGUESE -> {
                    val customPt = PdfRendererHelper.getSavedCustomPageLanguageText(context, book.id, book.currentPage, "pt")
                        ?: PdfRendererHelper.getSavedCustomPageText(context, book.id, book.currentPage)
                    if (!customPt.isNullOrBlank()) {
                        PdfRendererHelper.extractParagraphsAndSentences(customPt)
                    } else {
                        PdfRendererHelper.extractParagraphsAndSentencesFromPage(context, book, book.currentPage)
                    }
                }
                ReadingLanguage.ENGLISH -> {
                    val customEn = PdfRendererHelper.getSavedCustomPageLanguageText(context, book.id, book.currentPage, "en")
                    if (!customEn.isNullOrBlank()) {
                        PdfRendererHelper.extractParagraphsAndSentences(customEn)
                    } else if (!pageTranslation?.englishText.isNullOrBlank()) {
                        PdfRendererHelper.extractParagraphsAndSentences(pageTranslation!!.englishText)
                    } else {
                        PdfRendererHelper.extractParagraphsAndSentencesFromPage(context, book, book.currentPage)
                    }
                }
                ReadingLanguage.FRENCH -> {
                    val customFr = PdfRendererHelper.getSavedCustomPageLanguageText(context, book.id, book.currentPage, "fr")
                    if (!customFr.isNullOrBlank()) {
                        PdfRendererHelper.extractParagraphsAndSentences(customFr)
                    } else if (!pageTranslation?.frenchText.isNullOrBlank()) {
                        PdfRendererHelper.extractParagraphsAndSentences(pageTranslation!!.frenchText)
                    } else {
                        PdfRendererHelper.extractParagraphsAndSentencesFromPage(context, book, book.currentPage)
                    }
                }
                ReadingLanguage.SPANISH -> {
                    val customEs = PdfRendererHelper.getSavedCustomPageLanguageText(context, book.id, book.currentPage, "es")
                    if (!customEs.isNullOrBlank()) {
                        PdfRendererHelper.extractParagraphsAndSentences(customEs)
                    } else if (!pageTranslation?.spanishText.isNullOrBlank()) {
                        PdfRendererHelper.extractParagraphsAndSentences(pageTranslation!!.spanishText)
                    } else {
                        PdfRendererHelper.extractParagraphsAndSentencesFromPage(context, book, book.currentPage)
                    }
                }
            }

            extractedSentences = sentences
            readingParagraphs = paras
        } catch (_: Throwable) {
            readingParagraphs = listOf(BookParagraph(0, listOf("Página ${book.currentPage}"), 0))
            extractedSentences = listOf("Página ${book.currentPage}")
        }
        isLoadingContent = false
    }

    val displayParagraphs = remember(readingParagraphs, extractedSentences) {
        if (readingParagraphs.isNotEmpty()) {
            readingParagraphs
        } else if (extractedSentences.isNotEmpty()) {
            listOf(BookParagraph(0, extractedSentences, 0))
        } else {
            emptyList()
        }
    }

    val categoryPalette = BookCategoryTaxonomy.getPaletteForCategory(book.category, book.categoryColor)

    val cardBg = when (readingTheme) {
        "CATEGORY_COLOR" -> categoryPalette.surfaceColor
        "CYBER_NEON_NIGHT" -> Color(0xFF030D22)
        "MONOCHROME_BW" -> Color(0xFF141414)
        "PAPER_SEPIA" -> Color(0xFFF9EED4)
        "DARK_OLED" -> Color(0xFF050505)
        "CINEMA_YELLOW" -> Color(0xFF0C0C0C)
        "CYBERPUNK_NEON" -> Color(0xFF0B1220)
        "STREAMING_BOX" -> Color(0xFF16171E)
        "TERMINAL_GREEN" -> Color(0xFF050F07)
        else -> Color(0xFF1A1A1A)
    }

    val textCol = when (readingTheme) {
        "CATEGORY_COLOR" -> categoryPalette.textColor
        "CYBER_NEON_NIGHT" -> Color(0xFFFDFEFF)
        "MONOCHROME_BW" -> Color(0xFFE5E5E5)
        "PAPER_SEPIA" -> Color(0xFF261D12)
        "DARK_OLED" -> Color(0xFFFFFFFF)
        "CINEMA_YELLOW" -> Color(0xFFFFE600)
        "CYBERPUNK_NEON" -> Color(0xFF00F0FF)
        "STREAMING_BOX" -> Color(0xFFF3F4F6)
        "TERMINAL_GREEN" -> Color(0xFF00FF66)
        else -> Color.White
    }

    val activeSentenceBg = when (readingTheme) {
        "CATEGORY_COLOR" -> categoryPalette.activeSentenceBg
        "CYBER_NEON_NIGHT" -> Color(0xFF1C1347)
        "PAPER_SEPIA" -> Color(0xFFDECBA1)
        "CINEMA_YELLOW" -> Color(0xFF2E2500)
        "CYBERPUNK_NEON" -> Color(0xFF260D2E)
        "STREAMING_BOX" -> Color(0xFF242634)
        "TERMINAL_GREEN" -> Color(0xFF00290F)
        else -> Color(0xFF3B1E63)
    }

    val gestureModifier = if (!isShowingCoverFirst) {
        Modifier
            .pointerInput(book.id, book.currentPage, viewMode) {
                detectTapGestures(
                    onDoubleTap = {
                        if (viewMode == PdfViewMode.FULL_PAGE_SHEET) {
                            if (zoomScale > 1.2f) {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            } else {
                                zoomScale = 2.2f
                            }
                        } else {
                            onDoubleTap()
                        }
                    },
                    onTap = {
                        onSingleTap()
                    }
                )
            }
            .pointerInput(book.id, book.currentPage, zoomScale) {
                if (zoomScale <= 1.05f) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragTotalX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (kotlin.math.abs(dragAmount) > 2f) {
                                change.consume()
                                dragTotalX += dragAmount
                            }
                        },
                        onDragEnd = {
                            if (kotlin.math.abs(dragTotalX) > 60f) {
                                if (dragTotalX < 0) {
                                    onSwipeLeft()
                                } else {
                                    onSwipeRight()
                                }
                            }
                            dragTotalX = 0f
                        },
                        onDragCancel = {
                            dragTotalX = 0f
                        }
                    )
                }
            }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = if (isFullscreen) 4.dp else 52.dp)
            .navigationBarsPadding()
            .padding(start = 6.dp, end = 6.dp, bottom = 2.dp)
            .then(gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                when (readingTheme) {
                    "CATEGORY_COLOR" -> categoryPalette.accentColor.copy(alpha = 0.55f)
                    "PAPER_SEPIA" -> Color(0x668B6F47)
                    "DARK_OLED" -> Color(0x33FFFFFF)
                    "CINEMA_YELLOW" -> Color(0x88FFE600)
                    "CYBERPUNK_NEON" -> Color(0x8800F0FF)
                    "STREAMING_BOX" -> Color(0x66818CF8)
                    "TERMINAL_GREEN" -> Color(0x8800FF66)
                    else -> Color(0x33FFFFFF)
                }
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isShowingCoverFirst) {
                // Book Cover First Hero Screen - Maximized Edge-to-Edge Display
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Giant Cover Graphic stretching towards borders
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            when (readingTheme) {
                                "CATEGORY_COLOR" -> categoryPalette.accentColor.copy(alpha = 0.45f)
                                "PAPER_SEPIA" -> Color(0x558B6F47)
                                "CINEMA_YELLOW" -> Color(0x55FFE600)
                                "CYBERPUNK_NEON" -> Color(0x5500F0FF)
                                "STREAMING_BOX" -> Color(0x55818CF8)
                                "TERMINAL_GREEN" -> Color(0x5500FF66)
                                else -> Color.White.copy(alpha = 0.25f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onEnterBook() }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverBitmap != null) {
                                Image(
                                    bitmap = coverBitmap!!.asImageBitmap(),
                                    contentDescription = "Capa de ${book.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                val placeholderCoverGradient = when (readingTheme) {
                                    "CATEGORY_COLOR" -> listOf(categoryPalette.surfaceColor, categoryPalette.accentColor.copy(alpha = 0.45f))
                                    "PAPER_SEPIA" -> listOf(Color(0xFFEADBBE), Color(0xFFCDB896))
                                    "DARK_OLED" -> listOf(Color(0xFF1E1E1E), Color(0xFF000000))
                                    "CINEMA_YELLOW" -> listOf(Color(0xFF2B2500), Color(0xFF0F0E00))
                                    "CYBERPUNK_NEON" -> listOf(Color(0xFF260D2E), Color(0xFF070A14))
                                    "STREAMING_BOX" -> listOf(Color(0xFF212538), Color(0xFF101115))
                                    "TERMINAL_GREEN" -> listOf(Color(0xFF002B11), Color(0xFF020703))
                                    else -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(placeholderCoverGradient)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(68.dp)
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = book.author,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.85f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Book Info (Title, Author, Category Tag & Reading Pill)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textCol,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${book.author} • ${book.totalPages} páginas",
                            style = MaterialTheme.typography.bodySmall,
                            color = textCol.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtle Category Subtitle with palette dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenCategoryDetails() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = categoryPalette.accentColor,
                                modifier = Modifier.size(7.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (book.category ?: "Classificando categoria..."),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = categoryPalette.accentColor
                            )
                            if (!book.subcategory.isNullOrBlank()) {
                                Text(
                                    text = " • ${book.subcategory}",
                                    fontSize = 10.sp,
                                    color = textCol.copy(alpha = 0.65f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        val pillAccent = when (readingTheme) {
                            "CATEGORY_COLOR" -> categoryPalette.accentColor
                            "CYBER_NEON_NIGHT" -> Color(0xFF0EF3FF) // Ciano neon
                            "PAPER_SEPIA" -> Color(0xFF8B5E34)
                            "DARK_OLED" -> Color(0xFFE0E0E0)
                            "CINEMA_YELLOW" -> Color(0xFFFFE600)
                            "CYBERPUNK_NEON" -> Color(0xFF00F0FF)
                            "STREAMING_BOX" -> Color(0xFF818CF8)
                            "TERMINAL_GREEN" -> Color(0xFF00FF66)
                            else -> Color(0xFFC084FC)
                        }

                        // Slim Elegant Action Pill (não obstrui a capa nem a tela)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = pillAccent.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                pillAccent.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onEnterBook() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = pillAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (book.currentPage > 1) {
                                        "Continuar da Pág. ${book.currentPage} • Toque para Ler"
                                    } else {
                                        "Toque para Ler • Pág. 1"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textCol
                                )
                            }
                        }
                    }
                }
            }

            if (!isShowingCoverFirst) {
                if (viewMode == PdfViewMode.FULL_PAGE_SHEET) {
                // Large Immersive PDF Sheet View (Maximized Space)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 4.dp, end = if (isFullscreen) 4.dp else 52.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    if (isLoadingContent) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF8E2DE2))
                        }
                    } else if (pageBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .transformable(transformState),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = pageBitmap!!.asImageBitmap(),
                                contentDescription = "Folha do Livro Ampliada",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale,
                                        translationX = panOffset.x,
                                        translationY = panOffset.y
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // If zoomed in, discreet Reset Zoom pill at bottom start
                        if (zoomScale > 1.05f) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC)),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clickable {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${(zoomScale * 100).toInt()}% (Redefinir)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Typography Reader Layout (FOREGROUND_TEXT or COVER_ESSAY) with generous margins from the right HUD
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = if (isFullscreen) 12.dp else 40.dp, top = 8.dp, bottom = 4.dp)
                ) {
                    if (isLoadingContent) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF8E2DE2))
                        }
                    } else if (viewMode == PdfViewMode.COVER_ESSAY) {
                        // Mode: Capa + Campo de Ensaio Rápido de 1 Hora + Texto Justificado
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            item {
                                // Cover Preview Hero Card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (readingTheme == "PAPER_SEPIA") Color(0xFFEADBBE) else Color.White.copy(alpha = 0.08f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (coverBitmap != null) {
                                        Image(
                                            bitmap = coverBitmap!!.asImageBitmap(),
                                            contentDescription = "Capa do Livro",
                                            modifier = Modifier
                                                .width(64.dp)
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .width(64.dp)
                                                .height(90.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF8E2DE2).copy(alpha = 0.3f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MenuBook,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF8E2DE2)
                                        ) {
                                            Text(
                                                text = "ENSAIO RÁPIDO",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${book.author} • ${book.totalPages} págs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textCol.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Barra de Métricas Cognitivas e Ferramentas Offline da Página
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable { onOpenOfflineTools?.invoke() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(5.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.25f),
                                                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF10B981))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FlashOn,
                                                        contentDescription = null,
                                                        tint = Color(0xFF34D399),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "OFFLINE",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFD1FAE5)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            val wordCount = extractedSentences.sumOf { it.split(Regex("\\s+")).size }
                                            val estMin = (wordCount / 185).coerceAtLeast(1)
                                            Text(
                                                text = "⏱ ~$estMin min • ${extractedSentences.size} frases",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Text(
                                            text = "Resumo & Cloze ↗",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }

                                // Banner minimalista se já houver resumo salvo para esta página
                                if (pageSummary != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10A37F).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF00DC82).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .clickable { onOpenSummary() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "resumo",
                                                tint = Color(0xFF00DC82),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "resumo",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF00DC82)
                                            )
                                        }
                                    }
                                }

                                // Banner minimalista de Termos do Dicionário para esta página
                                if (pageDictionaryTerms.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .clickable { onOpenDictionary() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = "termos disponíveis",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "termos disponíveis",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }

                                // Banner minimalista de Questões da Página
                                if (pageQuestions.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFD97706).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .clickable { onOpenQuestionsDialog() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = "questão",
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "questão",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFFBBF24)
                                            )
                                        }
                                    }
                                }

                                // Campo de ensaio explicativo com texto justificado
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (readingTheme == "PAPER_SEPIA") Color(0xFFE4D3B4) else Color(0xFF241433)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "📖 Visão Geral da Obra",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (readingTheme == "PAPER_SEPIA") Color(0xFF261D12) else Color(0xFFE9D5FF)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Explore a introdução imediata desta obra. Você pode saltar diretamente para qualquer página ou ouvir cada parágrafo com as vozes neurais.",
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            textAlign = TextAlign.Justify,
                                            color = textCol.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Texto da Página ${book.currentPage}:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = textCol.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            items(displayParagraphs, key = { it.paragraphIndex }) { paragraph ->
                                InteractiveKaraokeParagraph(
                                    paragraph = paragraph,
                                    activeSentenceIndex = activeSentenceIndex,
                                    activeWordIndex = activeWordIndex,
                                    isSpeaking = isSpeaking,
                                    activeComparativeLineId = activeComparativeLineId,
                                    textFontSizeSp = textFontSizeSp.coerceAtLeast(9f),
                                    fontFamily = FontFamily.Serif,
                                    textColor = textCol,
                                    readingTheme = readingTheme,
                                    category = book.category,
                                    categoryColor = book.categoryColor,
                                    dictionaryTerms = pageDictionaryTerms,
                                    pageSegmentExplanations = pageSegmentExplanations,
                                    activeExpandedSegmentIndex = activeExpandedSegmentIndex,
                                    isGeneratingExplanation = isGeneratingSegmentExplanation,
                                    explanationError = segmentExplanationError,
                                    modifier = Modifier.fillMaxWidth(),
                                    onWordClick = { sIdx, wIdx ->
                                        onWordClick(sIdx, wIdx, extractedSentences)
                                    },
                                    onWordLongPress = onWordLongPress,
                                    onDictionaryTermClick = { term ->
                                        onOpenDictionaryTerm(term)
                                    },
                                    onExtractBlockTerms = { _, blockText ->
                                        scope.launch {
                                            val apiKey = com.example.data.openai.OpenAiService.getApiKey(context)
                                            val extractedTerms = if (apiKey.isNotBlank()) {
                                                val result = com.example.data.openai.OpenAiService.extractUniversalDictionaryTerms(
                                                    apiKey = apiKey,
                                                    model = "gpt-4o-mini",
                                                    bookId = book.id,
                                                    pageNumber = book.currentPage,
                                                    bookTitle = book.title,
                                                    pageText = blockText
                                                )
                                                result.getOrNull()?.takeIf { it.isNotEmpty() } ?: com.example.data.offline.OfflineReadingEngine.extractLocalTerms(
                                                    bookId = book.id,
                                                    pageNumber = book.currentPage,
                                                    bookTitle = book.title,
                                                    text = blockText
                                                )
                                            } else {
                                                com.example.data.offline.OfflineReadingEngine.extractLocalTerms(
                                                    bookId = book.id,
                                                    pageNumber = book.currentPage,
                                                    bookTitle = book.title,
                                                    text = blockText
                                                )
                                            }
                                            val existing = pageDictionaryTerms.map { it.term.trim().lowercase() }.toSet()
                                            val newTerms = extractedTerms.filter { it.term.trim().lowercase() !in existing }
                                            if (newTerms.isNotEmpty()) {
                                                onSaveDictionaryTerms(newTerms)
                                                android.widget.Toast.makeText(context, "✨ ${newTerms.size} termos com significados adicionados às notas!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Termos já catalogados nas notas da página.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onSegmentClickForExplanation = { sIdx, sText ->
                                        if (activeExpandedSegmentIndex == sIdx) {
                                            activeExpandedSegmentIndex = null
                                        } else {
                                            activeExpandedSegmentIndex = sIdx
                                            onRequestSegmentExplanation(book, book.currentPage, sIdx, sText, false)
                                        }
                                    },
                                    onLanguageChangeAndSpeak = { langCode, textToSpeak ->
                                        val readLang = when (langCode.lowercase()) {
                                            "es", "spanish", "espanhol" -> ReadingLanguage.SPANISH
                                            "en", "english", "inglês" -> ReadingLanguage.ENGLISH
                                            "fr", "french", "francês" -> ReadingLanguage.FRENCH
                                            else -> ReadingLanguage.PORTUGUESE
                                        }
                                        speechManager?.setReadingLanguage(readLang)
                                        if (textToSpeak.isNotBlank()) {
                                            speechManager?.speakTextInLanguage(textToSpeak, readLang)
                                        }
                                    },
                                    onSpeakExplanation = { langCode, text ->
                                        val readLang = when (langCode.lowercase()) {
                                            "es", "spanish", "espanhol" -> ReadingLanguage.SPANISH
                                            "en", "english", "inglês" -> ReadingLanguage.ENGLISH
                                            "fr", "french", "francês" -> ReadingLanguage.FRENCH
                                            else -> ReadingLanguage.PORTUGUESE
                                        }
                                        speechManager?.setReadingLanguage(readLang)
                                        if (text.isNotBlank()) {
                                            speechManager?.speakTextInLanguage(text, readLang)
                                        }
                                    },
                                     onSpeakExplanationWord = { langCode, fullText, wordOffset ->
                                        val readLang = when (langCode.lowercase()) {
                                            "es", "spanish", "espanhol" -> ReadingLanguage.SPANISH
                                            "en", "english", "inglês" -> ReadingLanguage.ENGLISH
                                            "fr", "french", "francês" -> ReadingLanguage.FRENCH
                                            else -> ReadingLanguage.PORTUGUESE
                                        }
                                        val targetExplanationId = pageSegmentExplanations.firstOrNull { it.segmentIndex == activeExpandedSegmentIndex }?.id ?: 0
                                        val lineId = "expl_${targetExplanationId}_${langCode.uppercase()}"
                                        speechManager?.speakComparativeLine(
                                            text = fullText,
                                            language = readLang,
                                            lineId = lineId,
                                            startWordOffset = wordOffset
                                        )
                                    },
                                    onSpeakTranslatedBlock = { lang, text, wordOffset, lineId ->
                                        speechManager?.speakStudyLine(
                                            text = text,
                                            language = lang,
                                            lineId = lineId,
                                            startWordOffset = wordOffset
                                        )
                                    },
                                    onPlayOpenAiAudio = { audioFile, lineId, text, wordOffset ->
                                        speechManager?.playOpenAiAudio(
                                            audioFile = audioFile,
                                            lineId = lineId,
                                            text = text,
                                            startWordOffset = wordOffset
                                        )
                                    },
                                    isLanguageAvailableOnDevice = { loc ->
                                        speechManager?.isLanguageAvailableOnDevice(loc) ?: false
                                    },
                                    onStopSpeaking = {
                                        speechManager?.pause()
                                    },
                                    onToggleSpeech = onToggleSpeech,
                                    onRegenerateExplanation = { sIdx, sText ->
                                        onRequestSegmentExplanation(book, book.currentPage, sIdx, sText, true)
                                    },
                                    onDeleteExplanation = { expl ->
                                        onDeleteSegmentExplanation(expl)
                                        if (activeExpandedSegmentIndex == expl.segmentIndex) {
                                            activeExpandedSegmentIndex = null
                                        }
                                    },
                                    onDismissExplanation = {
                                        activeExpandedSegmentIndex = null
                                    },
                                    segmentChatMessages = segmentChatMessages.filter {
                                        it.segmentIndex == paragraph.globalStartSentenceIndex
                                    },
                                    isSegmentChatLoading = segmentChatLoadingMap["${book.id}-${book.currentPage}-${paragraph.globalStartSentenceIndex}"] == true,
                                    segmentChatError = segmentChatErrorMap["${book.id}-${book.currentPage}-${paragraph.globalStartSentenceIndex}"],
                                    onSendChatMessage = { sIdx, sText, q, isAudio ->
                                        onSendSegmentChatMessage(book, book.currentPage, sIdx, sText, q, isAudio)
                                    },
                                    onClearSegmentChat = { sIdx ->
                                        onClearSegmentChat(book.id, book.currentPage, sIdx)
                                    },
                                    onDeleteChatMessage = onDeleteSegmentChatMessage,
                                    onAnalyzeSentence = onAnalyzeSentence,
                                    speechManager = speechManager
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Seção de Fechamento da Página (Simulado / Questões)
                            if (pageQuestions.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFD97706).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenQuestionsDialog() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "🎯 ${pageQuestions.size} Questões Desta Página",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFBBF24)
                                            ) {
                                                Text(
                                                    text = "PRATICAR ▶",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    } else {
                        val hasSavedPageLangs = remember(book.id, book.currentPage, contentRevision) {
                            PdfRendererHelper.hasAnySavedCustomPageLanguage(context, book.id, book.currentPage)
                        }
                        // Reorganized & 100% Justified Foreground Typography Reader
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Barra de Métricas Cognitivas e Ferramentas Offline da Página
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clickable { onOpenOfflineTools?.invoke() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(5.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.25f),
                                                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF10B981))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FlashOn,
                                                        contentDescription = null,
                                                        tint = Color(0xFF34D399),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "OFFLINE",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFD1FAE5)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            val wordCount = extractedSentences.sumOf { it.split(Regex("\\s+")).size }
                                            val estMin = (wordCount / 185).coerceAtLeast(1)
                                            Text(
                                                text = "⏱ ~$estMin min • ${extractedSentences.size} frases",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Text(
                                            text = "Resumo & Cloze ↗",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }

                            if (pageTranslation != null || hasSavedPageLangs) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF161028).copy(alpha = 0.90f),
                                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF8E2DE2).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Compact Language switch pills (PT, EN, FR, ES)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                ReadingLanguage.values().forEach { lang ->
                                                    val isSelected = activeReadingLanguage == lang
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isSelected) Color(0xFF8E2DE2) else Color.White.copy(alpha = 0.08f),
                                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFC084FC)) else null,
                                                        modifier = Modifier.clickable { onSelectReadingLanguage(lang) }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(text = lang.flag, fontSize = 10.5.sp)
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = lang.shortLabel,
                                                                fontSize = 10.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Right buttons: Line-by-line Comparative mode toggle & Open Dialog
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                // Toggle Comparative Mode button
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isComparativeMode) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, if (isComparativeMode) Color(0xFFEF4444) else Color(0xFF10B981)),
                                                    modifier = Modifier.clickable { onToggleComparativeMode() }
                                                ) {
                                                    Text(
                                                        text = if (isComparativeMode) "✕ Normal" else "⚖️ Comparar",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isComparativeMode) Color(0xFFFCA5A5) else Color(0xFFD1FAE5),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                    )
                                                }

                                                // Open full dialog / options button
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    modifier = Modifier.clickable { onOpenMultiTranslation() }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.OpenInNew,
                                                        contentDescription = "Configurações da Tradução",
                                                        tint = Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier.padding(3.dp).size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (pageSummary != null) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10A37F).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF00DC82).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .clickable { onOpenSummary() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "resumo",
                                                tint = Color(0xFF00DC82),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "resumo",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF00DC82)
                                            )
                                        }
                                    }
                                }
                            }
                            if (pageDictionaryTerms.isNotEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .clickable { onOpenDictionary() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = "termos disponíveis",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "termos disponíveis",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }
                            }

                            // Banner minimalista de Questões da Página
                            if (pageQuestions.isNotEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFD97706).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .clickable { onOpenQuestionsDialog() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = "questão",
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "questão",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFFBBF24)
                                            )
                                        }
                                    }
                                }
                            }
                            if (isComparativeMode && pageTranslation != null) {
                                val paragraphs = pageTranslation.getParagraphs()
                                val targetLang = if (activeReadingLanguage != ReadingLanguage.PORTUGUESE) {
                                    activeReadingLanguage
                                } else {
                                    ReadingLanguage.ENGLISH
                                }

                                items(items = paragraphs, key = { it.paragraphIndex }) { p: ComparativeParagraph ->
                                    val ptSentences = PdfRendererHelper.extractParagraphsAndSentences(p.portuguese).second
                                    val targetText = when (targetLang) {
                                        ReadingLanguage.ENGLISH -> p.english
                                        ReadingLanguage.FRENCH -> p.french
                                        ReadingLanguage.SPANISH -> p.spanish
                                        ReadingLanguage.PORTUGUESE -> p.english
                                    }
                                    val targetSentences = PdfRendererHelper.extractParagraphsAndSentences(targetText).second
                                    val lineCount = maxOf(ptSentences.size, targetSentences.size).coerceAtLeast(1)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp)
                                    ) {
                                        for (i in 0 until lineCount) {
                                            val tLine = targetSentences.getOrNull(i) ?: ""
                                            val pLine = ptSentences.getOrNull(i) ?: ""

                                            val foreignLineId = "t_${p.paragraphIndex}_$i"
                                            val ptLineId = "p_${p.paragraphIndex}_$i"

                                            // 1. Foreign language line with real-time word-by-word Karaoke and native voice
                                            if (tLine.isNotBlank()) {
                                                ComparativeKaraokeLine(
                                                    text = tLine,
                                                    language = targetLang,
                                                    lineId = foreignLineId,
                                                    isSpeaking = isSpeaking,
                                                    activeComparativeLineId = activeComparativeLineId,
                                                    activeWordIndex = activeWordIndex,
                                                    textColor = textCol,
                                                    onSpeakLine = { wordOffset ->
                                                        speechManager?.speakComparativeLine(
                                                            text = tLine,
                                                            language = targetLang,
                                                            lineId = foreignLineId,
                                                            startWordOffset = wordOffset
                                                        )
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                            }

                                            // 2. Portuguese line with real-time word-by-word Karaoke
                                            if (pLine.isNotBlank()) {
                                                ComparativeKaraokeLine(
                                                    text = pLine,
                                                    language = ReadingLanguage.PORTUGUESE,
                                                    lineId = ptLineId,
                                                    isSpeaking = isSpeaking,
                                                    activeComparativeLineId = activeComparativeLineId,
                                                    activeWordIndex = activeWordIndex,
                                                    textColor = textCol.copy(alpha = 0.92f),
                                                    onSpeakLine = { wordOffset ->
                                                        speechManager?.speakComparativeLine(
                                                            text = pLine,
                                                            language = ReadingLanguage.PORTUGUESE,
                                                            lineId = ptLineId,
                                                            startWordOffset = wordOffset
                                                        )
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(7.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(displayParagraphs, key = { it.paragraphIndex }) { paragraph ->
                                    InteractiveKaraokeParagraph(
                                        paragraph = paragraph,
                                        activeSentenceIndex = activeSentenceIndex,
                                        activeWordIndex = activeWordIndex,
                                        isSpeaking = isSpeaking,
                                        activeComparativeLineId = activeComparativeLineId,
                                        textFontSizeSp = textFontSizeSp.coerceAtLeast(9f),
                                        fontFamily = FontFamily.Serif,
                                        textColor = textCol,
                                        readingTheme = readingTheme,
                                        category = book.category,
                                        categoryColor = book.categoryColor,
                                        dictionaryTerms = pageDictionaryTerms,
                                        pageSegmentExplanations = pageSegmentExplanations,
                                        activeExpandedSegmentIndex = activeExpandedSegmentIndex,
                                        isGeneratingExplanation = isGeneratingSegmentExplanation,
                                        explanationError = segmentExplanationError,
                                        modifier = Modifier.fillMaxWidth(),
                                        onWordClick = { sIdx, wIdx ->
                                            onWordClick(sIdx, wIdx, extractedSentences)
                                        },
                                        onWordLongPress = onWordLongPress,
                                        onDictionaryTermClick = { term ->
                                            onOpenDictionaryTerm(term)
                                        },
                                        onExtractBlockTerms = { _, blockText ->
                                            scope.launch {
                                                val apiKey = com.example.data.openai.OpenAiService.getApiKey(context)
                                                val extractedTerms = if (apiKey.isNotBlank()) {
                                                    val result = com.example.data.openai.OpenAiService.extractUniversalDictionaryTerms(
                                                        apiKey = apiKey,
                                                        model = "gpt-4o-mini",
                                                        bookId = book.id,
                                                        pageNumber = book.currentPage,
                                                        bookTitle = book.title,
                                                        pageText = blockText
                                                    )
                                                    result.getOrNull()?.takeIf { it.isNotEmpty() } ?: com.example.data.offline.OfflineReadingEngine.extractLocalTerms(
                                                        bookId = book.id,
                                                        pageNumber = book.currentPage,
                                                        bookTitle = book.title,
                                                        text = blockText
                                                    )
                                                } else {
                                                    com.example.data.offline.OfflineReadingEngine.extractLocalTerms(
                                                        bookId = book.id,
                                                        pageNumber = book.currentPage,
                                                        bookTitle = book.title,
                                                        text = blockText
                                                    )
                                                }
                                                val existing = pageDictionaryTerms.map { it.term.trim().lowercase() }.toSet()
                                                val newTerms = extractedTerms.filter { it.term.trim().lowercase() !in existing }
                                                if (newTerms.isNotEmpty()) {
                                                    onSaveDictionaryTerms(newTerms)
                                                    android.widget.Toast.makeText(context, "✨ ${newTerms.size} termos com significados adicionados às notas!", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Termos já catalogados nas notas da página.", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onSegmentClickForExplanation = { sIdx, sText ->
                                            if (activeExpandedSegmentIndex == sIdx) {
                                                activeExpandedSegmentIndex = null
                                            } else {
                                                activeExpandedSegmentIndex = sIdx
                                                onRequestSegmentExplanation(book, book.currentPage, sIdx, sText, false)
                                            }
                                        },
                                        onLanguageChangeAndSpeak = { langCode, textToSpeak ->
                                            val readLang = when (langCode.lowercase()) {
                                                "es", "spanish", "espanhol" -> ReadingLanguage.SPANISH
                                                "en", "english", "inglês" -> ReadingLanguage.ENGLISH
                                                "fr", "french", "francês" -> ReadingLanguage.FRENCH
                                                else -> ReadingLanguage.PORTUGUESE
                                            }
                                            speechManager?.setReadingLanguage(readLang)
                                            if (textToSpeak.isNotBlank()) {
                                                speechManager?.speakTextInLanguage(textToSpeak, readLang)
                                            }
                                        },
                                        onSpeakExplanation = { langCode, text ->
                                            val readLang = when (langCode.lowercase()) {
                                                "es", "spanish", "espanhol" -> ReadingLanguage.SPANISH
                                                "en", "english", "inglês" -> ReadingLanguage.ENGLISH
                                                "fr", "french", "francês" -> ReadingLanguage.FRENCH
                                                else -> ReadingLanguage.PORTUGUESE
                                            }
                                            speechManager?.setReadingLanguage(readLang)
                                            if (text.isNotBlank()) {
                                                speechManager?.speakTextInLanguage(text, readLang)
                                            }
                                        },
                                         onSpeakExplanationWord = { langCode, fullText, wordOffset ->
                                            val readLang = when (langCode.lowercase()) {
                                                "es", "spanish", "espanhol" -> ReadingLanguage.SPANISH
                                                "en", "english", "inglês" -> ReadingLanguage.ENGLISH
                                                "fr", "french", "francês" -> ReadingLanguage.FRENCH
                                                else -> ReadingLanguage.PORTUGUESE
                                            }
                                            val targetExplanationId = pageSegmentExplanations.firstOrNull { it.segmentIndex == activeExpandedSegmentIndex }?.id ?: 0
                                            val lineId = "expl_${targetExplanationId}_${langCode.uppercase()}"
                                            speechManager?.speakComparativeLine(
                                                text = fullText,
                                                language = readLang,
                                                lineId = lineId,
                                                startWordOffset = wordOffset
                                            )
                                        },
                                        onSpeakTranslatedBlock = { lang, text, wordOffset, lineId ->
                                            speechManager?.speakStudyLine(
                                                text = text,
                                                language = lang,
                                                lineId = lineId,
                                                startWordOffset = wordOffset
                                            )
                                        },
                                        onPlayOpenAiAudio = { audioFile, lineId, text, wordOffset ->
                                            speechManager?.playOpenAiAudio(
                                                audioFile = audioFile,
                                                lineId = lineId,
                                                text = text,
                                                startWordOffset = wordOffset
                                            )
                                        },
                                        isLanguageAvailableOnDevice = { loc ->
                                            speechManager?.isLanguageAvailableOnDevice(loc) ?: false
                                        },
                                        onStopSpeaking = {
                                            speechManager?.pause()
                                        },
                                        onToggleSpeech = onToggleSpeech,
                                        onRegenerateExplanation = { sIdx, sText ->
                                            onRequestSegmentExplanation(book, book.currentPage, sIdx, sText, true)
                                        },
                                        onDeleteExplanation = { expl ->
                                            onDeleteSegmentExplanation(expl)
                                            if (activeExpandedSegmentIndex == expl.segmentIndex) {
                                                activeExpandedSegmentIndex = null
                                            }
                                        },
                                        onDismissExplanation = {
                                            activeExpandedSegmentIndex = null
                                        },
                                        segmentChatMessages = segmentChatMessages.filter {
                                            it.segmentIndex == paragraph.globalStartSentenceIndex
                                        },
                                        isSegmentChatLoading = segmentChatLoadingMap["${book.id}-${book.currentPage}-${paragraph.globalStartSentenceIndex}"] == true,
                                        segmentChatError = segmentChatErrorMap["${book.id}-${book.currentPage}-${paragraph.globalStartSentenceIndex}"],
                                        onSendChatMessage = { sIdx, sText, q, isAudio ->
                                            onSendSegmentChatMessage(book, book.currentPage, sIdx, sText, q, isAudio)
                                        },
                                        onClearSegmentChat = { sIdx ->
                                            onClearSegmentChat(book.id, book.currentPage, sIdx)
                                        },
                                        onDeleteChatMessage = onDeleteSegmentChatMessage,
                                        onAnalyzeSentence = onAnalyzeSentence,
                                        speechManager = speechManager
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            // 📝 NOTAS & TERMOS DA PÁGINA (Sincronizado com Fonte, Tema e Tradução Multilíngue para Todos os Idiomas)
                            val currentPageNotes = geminiNotes.filter { it.cueIndex == book.currentPage }
                            if (pageDictionaryTerms.isNotEmpty() || currentPageNotes.isNotEmpty()) {
                                val noteCardBg = when (readingTheme) {
                                    "PAPER_SEPIA" -> Color(0xFFF3E7CD)
                                    "TERMINAL_GREEN" -> Color(0xFF031407)
                                    "CYBERPUNK_NEON" -> Color(0xFF130926)
                                    "CINEMA_YELLOW" -> Color(0xFF1A1603)
                                    "DARK_OLED" -> Color(0xFF0D0D0D)
                                    "LIGHT" -> Color(0xFFF8FAFC)
                                    "MONOCHROME_BW" -> Color(0xFF1F1F1F)
                                    "CYBER_NEON_NIGHT" -> Color(0xFF081432)
                                    else -> cardBg.copy(alpha = 0.94f)
                                }

                                val noteBorderColor = when (readingTheme) {
                                    "PAPER_SEPIA" -> Color(0xFF8C6239).copy(alpha = 0.45f)
                                    "TERMINAL_GREEN" -> Color(0xFF00FF66).copy(alpha = 0.45f)
                                    "CYBERPUNK_NEON" -> Color(0xFFFF0080).copy(alpha = 0.5f)
                                    "CINEMA_YELLOW" -> Color(0xFFFFCC00).copy(alpha = 0.5f)
                                    "DARK_OLED" -> Color.White.copy(alpha = 0.22f)
                                    "LIGHT" -> Color(0xFF0284C7).copy(alpha = 0.35f)
                                    "MONOCHROME_BW" -> Color.White.copy(alpha = 0.3f)
                                    else -> Color(0xFF38BDF8).copy(alpha = 0.4f)
                                }

                                val noteAccentColor = when (readingTheme) {
                                    "PAPER_SEPIA" -> Color(0xFF9E5A1C)
                                    "TERMINAL_GREEN" -> Color(0xFF00FF66)
                                    "CYBERPUNK_NEON" -> Color(0xFF0EF3FF)
                                    "CINEMA_YELLOW" -> Color(0xFFFFCC00)
                                    "DARK_OLED" -> Color(0xFF60A5FA)
                                    "LIGHT" -> Color(0xFF0284C7)
                                    "MONOCHROME_BW" -> Color(0xFFCCCCCC)
                                    else -> Color(0xFF38BDF8)
                                }

                                val noteTextColor = when (readingTheme) {
                                    "PAPER_SEPIA" -> Color(0xFF2B2013)
                                    "LIGHT" -> Color(0xFF1E293B)
                                    "TERMINAL_GREEN" -> Color(0xFF99FFB8)
                                    "CINEMA_YELLOW" -> Color(0xFFFFF7A1)
                                    else -> textCol
                                }

                                val noteHighlightBg = when (readingTheme) {
                                    "PAPER_SEPIA" -> Color(0xFFE5A93C).copy(alpha = 0.6f)
                                    "TERMINAL_GREEN" -> Color(0xFF00FF66).copy(alpha = 0.5f)
                                    "CYBERPUNK_NEON" -> Color(0xFFFF007F).copy(alpha = 0.6f)
                                    "CINEMA_YELLOW" -> Color(0xFFFFCC00).copy(alpha = 0.6f)
                                    "LIGHT" -> Color(0xFF38BDF8).copy(alpha = 0.5f)
                                    else -> Color(0xFFFBBF24).copy(alpha = 0.6f)
                                }

                                val noteHighlightTextColor = when (readingTheme) {
                                    "TERMINAL_GREEN" -> Color.Black
                                    "PAPER_SEPIA" -> Color(0xFF261D12)
                                    "CINEMA_YELLOW" -> Color.Black
                                    else -> Color.Black
                                }

                                val noteTitleFontSize = (textFontSizeSp * 0.92f).coerceIn(12f, 28f).sp
                                val noteBodyFontSize = (textFontSizeSp * 0.82f).coerceIn(11f, 25f).sp
                                val noteBodyLineHeight = (textFontSizeSp * 1.30f).coerceIn(15f, 36f).sp

                                val allSupportedLanguages = StudyLanguage.allSupportedLanguages

                                item {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmarks,
                                                contentDescription = "Notas e Termos",
                                                tint = noteAccentColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Notas & Termos da Página (${pageDictionaryTerms.size + currentPageNotes.size})",
                                                fontSize = (textFontSizeSp * 0.84f).coerceIn(11f, 22f).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = noteAccentColor
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF10A37F).copy(alpha = 0.20f),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF10A37F).copy(alpha = 0.60f)),
                                                modifier = Modifier.clickable { onOpenOpenAiStudy() }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = Color(0xFF10A37F),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Estudo IA",
                                                        fontSize = (textFontSizeSp * 0.72f).coerceIn(10f, 18f).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10A37F)
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = noteAccentColor.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, noteAccentColor.copy(alpha = 0.4f)),
                                                modifier = Modifier.clickable { onOpenDictionary() }
                                            ) {
                                                Text(
                                                    text = "Ver",
                                                    fontSize = (textFontSizeSp * 0.72f).coerceIn(10f, 18f).sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = noteAccentColor,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Cada Termo do Dicionário com Karaokê e Tradução multilíngue com bandeiras e reprodução OpenAI
                                items(pageDictionaryTerms, key = { "dict_term_${it.id}_${it.term}" }) { term ->
                                    MultilingualTermItem(
                                        term = term,
                                        speechManager = speechManager,
                                        accentColor = noteAccentColor,
                                        textColor = noteTextColor,
                                        titleFontSize = noteTitleFontSize,
                                        bodyFontSize = noteBodyFontSize,
                                        bodyLineHeight = noteBodyLineHeight,
                                        highlightBgColor = noteHighlightBg,
                                        highlightTextColor = noteHighlightTextColor,
                                        cardBgColor = noteCardBg,
                                        cardBorderColor = null,
                                        showDetailsButton = true,
                                        onDetailsClick = { onOpenDictionaryTerm(term) },
                                        onTermClick = { onOpenDictionaryTerm(term) },
                                        scopeId = "page_note_${term.id}",
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                // Cada Anotação Gemini da página com Karaokê e Tradução
                                items(
                                    items = currentPageNotes,
                                    key = { note: com.example.data.model.GeminiNote -> "gemini_note_${note.id}" }
                                ) { note ->
                                    val currentLang = selectedNoteLanguages[note.id] ?: StudyLanguage.PORTUGUESE
                                    val originalText = note.geminiResponse.ifBlank { note.fullContext.ifBlank { note.termOrSentence } }
                                    val cacheKey = "note_${note.id}_${note.termOrSentence.hashCode()}_${currentLang.code}"
                                    val translationPair = noteTranslationsCache[cacheKey]

                                    val displayNoteTitle = translationPair?.first ?: if (currentLang == StudyLanguage.PORTUGUESE) {
                                        note.termOrSentence
                                    } else {
                                        TermsNotesTranslator.getQuickTranslation(note.termOrSentence, "", currentLang).first
                                    }
                                    val displayNoteText = translationPair?.second ?: if (currentLang == StudyLanguage.PORTUGUESE) {
                                        originalText
                                    } else {
                                        TermsNotesTranslator.getQuickTranslation(note.termOrSentence, originalText, currentLang).second
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = noteCardBg,
                                        border = androidx.compose.foundation.BorderStroke(0.9.dp, noteBorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(11.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Nota: $displayNoteTitle",
                                                    fontSize = noteTitleFontSize,
                                                    fontWeight = FontWeight.Bold,
                                                    color = noteAccentColor
                                                )
                                            }

                                            // Seletor de Idiomas para a Nota
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                allSupportedLanguages.forEach { lang ->
                                                    val isSelected = currentLang == lang
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isSelected) noteAccentColor.copy(alpha = 0.3f) else Color.Transparent,
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            0.7.dp,
                                                            if (isSelected) noteAccentColor else noteTextColor.copy(alpha = 0.25f)
                                                        ),
                                                        modifier = Modifier.clickable {
                                                            selectedNoteLanguages = selectedNoteLanguages + (note.id to lang)
                                                            val targetNoteKey = "note_${note.id}_${note.termOrSentence.hashCode()}_${lang.code}"
                                                            if (noteTranslationsCache[targetNoteKey] == null) {
                                                                scope.launch {
                                                                    val openAiKey = com.example.data.openai.OpenAiService.getApiKey(context)
                                                                    val res = TermsNotesTranslator.translateNoteText(
                                                                        noteTitle = note.termOrSentence,
                                                                        noteContent = originalText,
                                                                        targetLanguage = lang,
                                                                        customApiKey = openAiKey,
                                                                        context = context
                                                                    )
                                                                    noteTranslationsCache = noteTranslationsCache + (targetNoteKey to res)
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = lang.flag,
                                                                fontSize = (noteBodyFontSize.value * 0.82f).sp
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = lang.code.uppercase(),
                                                                fontSize = (noteBodyFontSize.value * 0.75f).sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) noteAccentColor else noteTextColor.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Botão + MAIS Idiomas
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = noteAccentColor.copy(alpha = 0.15f),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.7.dp,
                                                        noteAccentColor.copy(alpha = 0.4f)
                                                    ),
                                                    modifier = Modifier.clickable {
                                                        activeAllLanguagesDialogForNoteId = note.id
                                                    }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Mais idiomas",
                                                            tint = noteAccentColor,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "MAIS",
                                                            fontSize = (noteBodyFontSize.value * 0.72f).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = noteAccentColor
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            KaraokeDefinitionText(
                                                targetId = "page_gemini_note_${note.id}_${currentLang.code}",
                                                text = displayNoteText,
                                                speechManager = speechManager,
                                                accentColor = noteAccentColor,
                                                textColor = noteTextColor,
                                                fontSize = noteBodyFontSize,
                                                lineHeight = noteBodyLineHeight,
                                                highlightBgColor = noteHighlightBg,
                                                highlightTextColor = noteHighlightTextColor,
                                                showListenButton = true,
                                                language = currentLang
                                            )
                                        }
                                    }
                                }
                            }

                            // 💡 Síntese e Ideia Central da Página (Page-End Key Takeaway)
                            item {
                                Spacer(modifier = Modifier.height(14.dp))
                                val summaryContent = pageSummary?.summaryText?.trim()
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (summaryContent != null) Color(0xFF1E1B4B).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (summaryContent != null) Color(0xFF818CF8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenSummary() }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = "Síntese da Página",
                                                    tint = Color(0xFFA5B4FC),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "💡 Síntese da Página ${book.currentPage}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFA5B4FC)
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (summaryContent != null && speechManager != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFF6366F1).copy(alpha = 0.3f),
                                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF818CF8)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                speechManager.speakTextInLanguage(summaryContent, activeReadingLanguage)
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.VolumeUp,
                                                                contentDescription = "Ouvir Síntese",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "Ouvir",
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = if (summaryContent != null) "Ver completo ▶" else "Gerar síntese ▶",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        if (summaryContent != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = summaryContent,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                color = Color.White.copy(alpha = 0.92f),
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Toque para gerar os pontos-chave, termos e resumo didático com áudio desta página.",
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = Color.White.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Seção de Fechamento da Página (Simulado / Questões)
                            if (pageQuestions.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFD97706).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenQuestionsDialog() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${pageQuestions.size} Questões Desta Página",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFBBF24)
                                            ) {
                                                Text(
                                                    text = "questão",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Footer hints inside Reel Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "◀ Arraste p/ trocar pág ▶",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textCol.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "▶ Toque p/ ouvir",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

    // Diálogo para escolha de qualquer idioma para Termos da Página
    if (activeAllLanguagesDialogForTermId != null) {
        val termId = activeAllLanguagesDialogForTermId!!
        val term = pageDictionaryTerms.firstOrNull { it.id == termId }
        val currentLang = selectedTermLanguages[termId] ?: StudyLanguage.PORTUGUESE
        AllLanguagesDialog(
            selectedLanguage = currentLang,
            onSelectLanguage = { lang ->
                activeAllLanguagesDialogForTermId = null
                selectedTermLanguages = selectedTermLanguages + (termId to lang)
                if (term != null) {
                    val targetKey = "term_${term.id}_${term.term}_${lang.code}"
                    if (termTranslationsCache[targetKey] == null) {
                        scope.launch {
                            val openAiKey = com.example.data.openai.OpenAiService.getApiKey(context)
                            val res = TermsNotesTranslator.translateDictionaryTerm(
                                term = term,
                                targetLanguage = lang,
                                customApiKey = openAiKey,
                                context = context
                            )
                            termTranslationsCache = termTranslationsCache + (targetKey to res)
                        }
                    }
                }
            },
            onDismissRequest = { activeAllLanguagesDialogForTermId = null }
        )
    }

    // Diálogo para escolha de qualquer idioma para Notas da Página
    if (activeAllLanguagesDialogForNoteId != null) {
        val noteId = activeAllLanguagesDialogForNoteId!!
        val note = geminiNotes.firstOrNull { it.id == noteId }
        val currentLang = selectedNoteLanguages[noteId] ?: StudyLanguage.PORTUGUESE
        AllLanguagesDialog(
            selectedLanguage = currentLang,
            onSelectLanguage = { lang ->
                activeAllLanguagesDialogForNoteId = null
                selectedNoteLanguages = selectedNoteLanguages + (noteId to lang)
                if (note != null) {
                    val originalText = note.geminiResponse.ifBlank { note.fullContext.ifBlank { note.termOrSentence } }
                    val targetNoteKey = "note_${note.id}_${note.termOrSentence.hashCode()}_${lang.code}"
                    if (noteTranslationsCache[targetNoteKey] == null) {
                        scope.launch {
                            val openAiKey = com.example.data.openai.OpenAiService.getApiKey(context)
                            val res = TermsNotesTranslator.translateNoteText(
                                noteTitle = note.termOrSentence,
                                noteContent = originalText,
                                targetLanguage = lang,
                                customApiKey = openAiKey,
                                context = context
                            )
                            noteTranslationsCache = noteTranslationsCache + (targetNoteKey to res)
                        }
                    }
                }
            },
            onDismissRequest = { activeAllLanguagesDialogForNoteId = null }
        )
    }
}
}
